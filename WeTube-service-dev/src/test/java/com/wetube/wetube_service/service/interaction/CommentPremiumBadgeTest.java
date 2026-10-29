package com.wetube.wetube_service.service.interaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.wetube.wetube_service.dto.CommentDto.CommentResponseDto;
import com.wetube.wetube_service.dto.UserDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.interaction.Comment;
import com.wetube.wetube_service.mapper.UserMapper;
import com.wetube.wetube_service.mapper.interaction.CommentMapper;
import com.wetube.wetube_service.repository.interaction.CommentRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.interaction.impl.CommentServiceImpl;
import com.wetube.wetube_service.service.premium.PremiumUserService;

/** Huy hiệu Premium ở bình luận: tác giả (kể cả người trả lời) được đánh dấu bằng một truy vấn cho cả danh sách. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentPremiumBadgeTest {

    @Mock CommentRepository commentRepository;
    @Mock CommentMapper commentMapper;
    @Mock UserMapper userMapper;
    @Mock VideoRepository videoRepository;
    @Mock PremiumUserService premiumUserService;

    @InjectMocks CommentServiceImpl service;

    private final UUID videoId = UUID.randomUUID();
    private final UUID premiumAuthor = UUID.randomUUID();
    private final UUID plainAuthor = UUID.randomUUID();
    private final UUID thirdAuthor = UUID.randomUUID();

    private Comment comment(UUID author, UUID parent) {
        AppUser user = new AppUser();
        user.setId(author);
        Comment c = new Comment();
        c.setId(UUID.randomUUID());
        c.setUser(user);
        c.setTargetType(Comment.TargetType.VIDEO);
        c.setTargetId(videoId);
        c.setParentCommentId(parent);
        return c;
    }

    @BeforeEach
    void setUp() {
        when(commentMapper.toResponseDto(any())).thenAnswer(i -> new CommentResponseDto());
        when(userMapper.toBasicDto(any())).thenAnswer(i -> UserDto.builder().id(((AppUser) i.getArgument(0)).getId()).build());
        when(videoRepository.findById(videoId)).thenReturn(Optional.empty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void premiumAuthorsOfCommentsAndRepliesAreFlaggedWithASingleLookup() {
        Comment root1 = comment(premiumAuthor, null);
        Comment root2 = comment(plainAuthor, null);
        Comment reply = comment(thirdAuthor, root2.getId());
        Comment premiumReply = comment(premiumAuthor, root2.getId());
        when(commentRepository.findByTargetTypeAndTargetIdAndParentCommentIdIsNullOrderByCreatedAtDesc(Comment.TargetType.VIDEO, videoId))
                .thenReturn(List.of(root1, root2));
        when(commentRepository.findByParentCommentIdOrderByCreatedAtAsc(root2.getId())).thenReturn(List.of(reply, premiumReply));
        when(premiumUserService.premiumUserIds(any())).thenReturn(Set.of(premiumAuthor));

        List<CommentResponseDto> result = service.getCommentsByTargetId(videoId, Comment.TargetType.VIDEO, CommentService.Sort.NEWEST);

        ArgumentCaptor<java.util.Collection<UUID>> asked = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(premiumUserService, times(1)).premiumUserIds(asked.capture());
        assertThat(asked.getValue()).containsExactlyInAnyOrder(premiumAuthor, plainAuthor, thirdAuthor);
        CommentResponseDto first = result.stream().filter(c -> c.getUser().getId().equals(premiumAuthor)).findFirst().orElseThrow();
        CommentResponseDto second = result.stream().filter(c -> c.getUser().getId().equals(plainAuthor)).findFirst().orElseThrow();
        assertThat(first.getUser().isPremium()).isTrue();
        assertThat(second.getUser().isPremium()).isFalse();
        assertThat(second.getReplies()).extracting(r -> r.getUser().isPremium()).containsExactly(false, true);
    }

    @Test
    void noCommentsMeansNoPremiumQueryRows() {
        when(commentRepository.findByTargetTypeAndTargetIdAndParentCommentIdIsNullOrderByCreatedAtDesc(eq(Comment.TargetType.VIDEO), any()))
                .thenReturn(List.of());

        assertThat(service.getCommentsByTargetId(videoId, Comment.TargetType.VIDEO, CommentService.Sort.NEWEST)).isEmpty();
    }
}
