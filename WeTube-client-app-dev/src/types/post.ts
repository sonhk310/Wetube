export interface Author {
    id: string;
    name: string;
    avatarUrl: string;
}

export interface Post {
    id: string;
    content: string;
    imageUrl?: string;
    createdAt: string;
    videoLink?: string;
    likeCount: number;
    commentCount: number;
    author?: Author;
    poll?: Poll | null;
}

export interface PollOption {
    optionId: string;
    optionText: string;
    voteCount: number;
    percentage?: number;
}

export interface Poll {
    id: string;
    title?: string;
    options: PollOption[];
    expiresAt?: string;
    totalVotes?: number;
    userVotedOptionId?: string;
}
