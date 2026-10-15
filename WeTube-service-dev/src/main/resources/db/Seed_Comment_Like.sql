use wetube_local_db;

-- Comments (Bob comments on Alice's video)
INSERT INTO comments (id, target_id, parent_comment_id, content, like_count,
                      created_at, updated_at, target_type, user_id)
VALUES (
    UUID(),
    '66666666-6666-6666-6666-666666666660',
    NULL,
    'Awesome AMV! 🔥🔥',
    0,
    NOW(), NOW(),
    'VIDEO',
    'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
);

-- Likes on video (Bob likes Alice's video)
INSERT INTO likes (id, target_id, target_type, user_id, status)
VALUES (
    UUID(),
    '66666666-6666-6666-6666-666666666660',
    'VIDEO',
    'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    TRUE
);