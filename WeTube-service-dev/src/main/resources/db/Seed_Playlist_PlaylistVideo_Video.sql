-- =========================
-- PLAYLISTS FOR USER 1
-- =========================
INSERT INTO playlists (id, title, playlist_type, user_id, created_at, updated_at)
VALUES
    ('6f5a2e3a-3dca-4d3e-9f06-6b2f8bc09d4e', 'Liked Videos', 'LIKED', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa', NOW(), NOW()),
    ('e9b7f1c3-042f-4b57-bd71-9f20de8eeed3', 'Watch Later', 'WATCH_LATER', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa', NOW(), NOW()),
    ('4a5bd3c7-64d9-4c98-8de6-9cc0eb732097', 'History', 'HISTORY', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa', NOW(), NOW()),
    ('0e51e4c5-59a0-4ed9-9dcf-f28808a9428d', 'Chill Vibes', 'USER_PLAYLIST', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa', NOW(), NOW()),
    ('0e5124c4-59a0-4ed9-9dcf-f28808a9428d', 'Mr Beast Videos', 'USER_PLAYLIST', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa', NOW(), NOW()),
    ('f7121e2d-8d4e-4c92-a223-f9202d598a0a', 'Uploaded Videos', 'USER_UPLOADED', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa', NOW(), NOW());

-- =========================
-- PLAYLISTS FOR USER 2
-- =========================
INSERT INTO playlists (id, title, playlist_type, user_id, created_at, updated_at)
VALUES
    ('b7d3b32d-75f4-4b58-b71a-32f30f787c21', 'Liked Videos', 'LIKED', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaab', NOW(), NOW()),
    ('2ec04a16-3f3b-4a1d-8bb7-22a86276e927', 'Watch Later', 'WATCH_LATER', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaab', NOW(), NOW()),
    ('36b1561c-87c7-42c3-b89e-6a157860e0e9', 'History', 'HISTORY', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaab', NOW(), NOW()),
    ('5c39c1f7-4a3b-4e68-9527-c50a1aef1453', 'My Study Music', 'USER_PLAYLIST', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaab', NOW(), NOW()),
    ('7a0e77fa-6a32-4ca9-9f5b-5b0f25936020', 'Uploaded Videos', 'USER_UPLOADED', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaab', NOW(), NOW());

-- =========================
-- PLAYLISTS FOR USER 3
-- =========================
INSERT INTO playlists (id, title, playlist_type, user_id, created_at, updated_at)
VALUES
    ('39f0e9d1-cd0f-476a-b9f0-ef197e1e0a7f', 'Liked Videos', 'LIKED', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaac', NOW(), NOW()),
    ('9677ee7f-36d1-4ea4-8c10-bf7ecbfe85e6', 'Watch Later', 'WATCH_LATER', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaac', NOW(), NOW()),
    ('17b044e1-66a8-44d6-a404-f06b198bb65e', 'History', 'HISTORY', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaac', NOW(), NOW()),
    ('ecfb788e-2c97-4f06-ae32-471f199e66c3', 'Workout Motivation', 'USER_PLAYLIST', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaac', NOW(), NOW()),
    ('12f498e9-3b66-4e5f-9453-c56362b9c19a', 'Uploaded Videos', 'USER_UPLOADED', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaac', NOW(), NOW());

-- =========================
-- PLAYLISTS FOR USER Alice
-- =========================
INSERT INTO playlists (id, title, playlist_type, user_id, created_at, updated_at)
VALUES
    ('6f5a5h3d-3dca-4d3e-9f06-6b2f8bc09d4e', 'Liked Videos', 'LIKED', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaad', NOW(), NOW()),
    ('e9b7d5a7-042f-4b57-bd71-9f20de8eeed3', 'Watch Later', 'WATCH_LATER', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaad', NOW(), NOW()),
    ('4a5be1h6-64d9-4c98-8de6-9cc0eb732097', 'History', 'HISTORY', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaad', NOW(), NOW()),
    ('0e51h6f8-59a0-4ed9-9dcf-f28808a9428d', 'Alice Wonder', 'USER_PLAYLIST', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaad', NOW(), NOW()),
    ('0e5151c5-59a0-4ed9-9dcf-f28808a9428d', 'Bob Monster', 'USER_PLAYLIST', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaad', NOW(), NOW()),
    ('f7125b5c-8d4e-4c92-a223-f9202d598a0a', 'Uploaded Videos', 'USER_UPLOADED', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaad', NOW(), NOW());



-- =========================================
-- VIDEOS FOR USER 1
-- =========================================
INSERT INTO videos (id, users_id, title, description, thumbnail_url, video_url, videos_status, total_view, duration, created_at, updated_at)
VALUES
    ('91bc4b46-91f9-433a-b928-6b524b13f2b9', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'How To Use CSS Dev Tools Like a Senior Developer',
     'The dev tools built into the browser have tons of amazing features built in, but most developers never even use 10% of the available features',
     'https://i.ytimg.com/an_webp/Qf_5zmxrxzE/mqdefault_6s.webp?du=3000&sqp=CJSbnMYG&rs=AOn4CLBiSicTu4jwtH7FtJQXVPcHjY-eYw',
     'https://res.cloudinary.com/dx3cioett/video/upload/v1757878507/How_To_Use_CSS_Dev_Tools_Like_a_Senior_Developer_m9usss.mp4',
     'ACTIVE', 1200, 1190, NOW(), NOW()),

    ('38c75418-5df5-4e31-8a67-91f9c893bff1', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'NEW Next.js TypeScript Features',
     'Next.js 15 added a ton of type safety improvements over previous versions, but the newest additions to Next.js really improve the TypeScript experience and bring it on par with many alternative frameworks, such as Tanstack Start',
     'https://i.ytimg.com/an_webp/rVdR0_Ujgq4/mqdefault_6s.webp?du=3000&sqp=CM6TnMYG&rs=AOn4CLBGyJmc3nCKGK0tgOR9pj7w57bCBQ',
     'https://res.cloudinary.com/dx3cioett/video/upload/v1757878502/NEW_Next.js_TypeScript_Features_afsucu.mp4',
     'ACTIVE', 4500, 429, NOW(), NOW()),

    ('d5f6db34-4a0a-4184-a83c-95f4242a4f5e', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Now Is The Best Time To Learn To Code',
     'It may seem like now is the worst time to learn to code with the fear of AI taking your job and overall uncertainty in the market, but that couldn’t be further from the truth.',
     'https://i.ytimg.com/an_webp/9xzqqZMXjjg/mqdefault_6s.webp?du=3000&sqp=CPicnMYG&rs=AOn4CLBpV6qpTc8oufLW3sBE1jA1WwInJQ',
     'https://res.cloudinary.com/dx3cioett/video/upload/v1757878505/Now_Is_The_Best_Time_To_Learn_To_Code_bczysf.mp4',
     'ACTIVE', 980, 1119, NOW(), NOW());

-- =========================================
-- VIDEOS FOR USER 2
-- =========================================
INSERT INTO videos (id, users_id, title, description, thumbnail_url, video_url, videos_status, total_view, duration, created_at, updated_at)
VALUES
    ('7a1a7181-43be-49a6-9ab7-991a81f3e1b7', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaab', 'MONTAGEM TRALALERO TRALALA - SHX4 x WUYS',
     'Song Name : MONTAGEM TRALALERO TRALALA Artist : SHX4 x WUYS Release By : Hypertunes Records',
     'https://i.ytimg.com/vi/dPwIfzXwE2c/hqdefault.jpg',
     'https://res.cloudinary.com/dx3cioett/video/upload/v1757879522/MONTAGEM_TRALALERO_TRALALA_-_SHX4_x_WUYS_qvjapy.mp4',
     'ACTIVE', 1200, 84, NOW(), NOW()),
    ('1cd08f89-82b7-4f2c-94fa-3993c3dfde8a', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaab', 'LIRILÌ LARILÀ FUNK - SXYGX x SHX4',
     'Song Name : LIRILÌ LARILÀ FUNK - SXYGX, SHX4 Artist : SXYGX, SHX4 Release By : Hypertunes Records',
     'https://i.ytimg.com/vi/xQrKqIVtj0A/hqdefault.jpg',
     'https://res.cloudinary.com/dx3cioett/video/upload/v1757879523/LIRIL%C3%8C_LARIL%C3%80_FUNK_-_SXYGX_x_SHX4_slgfh8.mp4',
     'ACTIVE', 4500, 98, NOW(), NOW()),
    ('25b3a8d5-bb94-4f8f-b97b-370d7e2a1e8c', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaab', 'CAPPUCCINO ASSASSINO FUNK - SHX4 x SXYGX',
     'Song name : CAPPUCCINO ASSASSINO FUNK Artist : SHX4, SXYGX Release by : Hypertunes Records',
     'https://i.ytimg.com/vi/DjlnilW0ObM/hqdefault.jpg',
     'https://res.cloudinary.com/dx3cioett/video/upload/v1757879527/CAPPUCCINO_ASSASSINO_FUNK_-_SHX4_x_SXYGX_wqcogf.mp4',
     'ACTIVE', 980, 117, NOW(), NOW());

-- =========================================
-- VIDEOS FOR USER 3
-- =========================================
INSERT INTO videos (id, users_id, title, description, thumbnail_url, video_url, videos_status, total_view, duration, created_at, updated_at)
VALUES
    ('a6de7a3c-c20a-4e15-9c36-b75c94c07388', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaac', '2,000,000 People Get Clean Water For The First Time!',
     'Donation: https://www.beastphilanthropy.org/campaign/water-is-life',
     'https://i.ytimg.com/an_webp/Z4hVGCWH1Kc/mqdefault_6s.webp?du=3000&sqp=CMKfnMYG&rs=AOn4CLC20X71udkHVHlcmCYLybOhzKLGVw',
     'https://res.cloudinary.com/dx3cioett/video/upload/v1757880390/In_10_Minutes_This_Room_Will_Explode_d5crn8.mp4',
     'ACTIVE', 1200, 599, NOW(), NOW()),
    ('c5a4e07e-3b17-4d38-b60b-07a019f5eec3', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaac', 'I Built 100 Wells In Africa',
     'Donation: https://www.beastphilanthropy.org/campaign/water-is-life',
     'https://i.ytimg.com/an_webp/mwKJfNYwvm8/mqdefault_6s.webp?du=3000&sqp=COqunMYG&rs=AOn4CLDRiR87kzL1XWgA2G_vtn6wbZJuXg',
     'https://res.cloudinary.com/dx3cioett/video/upload/v1757880399/I_Built_100_Wells_In_Africa_gbczru.mp4',
     'ACTIVE', 4500, 629, NOW(), NOW()),
    ('0b5c8c6a-1a68-4f2a-93c4-48f67cb8f6d7', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaac', 'In 10 Minutes This Room Will Explode!',
     'Donation: https://www.beastphilanthropy.org/campaign/water-is-life',
     'https://i.ytimg.com/an_webp/Pv0iVoSZzN8/mqdefault_6s.webp?du=3000&sqp=COS0nMYG&rs=AOn4CLB7rJccbobhaMUUpwDHwBgqdtIP0Q',
     'https://res.cloudinary.com/dx3cioett/video/upload/v1757880390/In_10_Minutes_This_Room_Will_Explode_d5crn8.mp4',
     'ACTIVE', 980, 599, NOW(), NOW());
     
-- =========================================
-- VIDEOS FOR USER ALICE
-- =========================================
INSERT INTO videos (id, users_id, title, description, thumbnail_url, video_url, videos_status, total_view, duration, created_at, updated_at)
VALUES
    ('66666666-6666-6666-6666-666666666660', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaad', 'Alice AMV 2!',
     'Another epic Naruto AMV By Alice',
     'https://i.ytimg.com/an_webp/Z4hVGCWH1Kc/mqdefault_6s.webp?du=3000&sqp=CMKfnMYG&rs=AOn4CLC20X71udkHVHlcmCYLybOhzKLGVw',
     'https://res.cloudinary.com/dx3cioett/video/upload/v1757880390/In_10_Minutes_This_Room_Will_Explode_d5crn8.mp4',
     'ACTIVE', 1200, 599, NOW(), NOW());
     
     
-- =========================================
-- PLAYLIST_VIDEOS FOR USER 1 (USER_UPLOADED) - 3 videos
-- =========================================
INSERT INTO playlist_videos (id, playlist_id, video_id, history_duration)
VALUES
    (UUID(), 'f7121e2d-8d4e-4c92-a223-f9202d598a0a', '91bc4b46-91f9-433a-b928-6b524b13f2b9', 0),
    (UUID(), 'f7121e2d-8d4e-4c92-a223-f9202d598a0a', '38c75418-5df5-4e31-8a67-91f9c893bff1', 12.5),
    (UUID(), 'f7121e2d-8d4e-4c92-a223-f9202d598a0a', 'd5f6db34-4a0a-4184-a83c-95f4242a4f5e', 0);

-- =========================================
-- PLAYLIST_VIDEOS FOR USER 2 (USER_UPLOADED) - 3 videos
-- =========================================
INSERT INTO playlist_videos (id, playlist_id, video_id, history_duration)
VALUES
    (UUID(), '7a0e77fa-6a32-4ca9-9f5b-5b0f25936020', '7a1a7181-43be-49a6-9ab7-991a81f3e1b7', 0),
    (UUID(), '7a0e77fa-6a32-4ca9-9f5b-5b0f25936020', '1cd08f89-82b7-4f2c-94fa-3993c3dfde8a', 0),
    (UUID(), '7a0e77fa-6a32-4ca9-9f5b-5b0f25936020', '25b3a8d5-bb94-4f8f-b97b-370d7e2a1e8c', 0);

-- =========================================
-- PLAYLIST_VIDEOS FOR USER 3 (USER_UPLOADED) - 3 videos
-- =========================================
INSERT INTO playlist_videos (id, playlist_id, video_id, history_duration)
VALUES
    (UUID(), '12f498e9-3b66-4e5f-9453-c56362b9c19a', 'c5a4e07e-3b17-4d38-b60b-07a019f5eec3', 0),
    (UUID(), '12f498e9-3b66-4e5f-9453-c56362b9c19a', 'a6de7a3c-c20a-4e15-9c36-b75c94c07388', 0),
    (UUID(), '12f498e9-3b66-4e5f-9453-c56362b9c19a', '0b5c8c6a-1a68-4f2a-93c4-48f67cb8f6d7', 0);

-- =========================================
-- PLAYLIST_VIDEOS FOR USER ALICE (USER_UPLOADED) - 1 videos
-- =========================================
INSERT INTO playlist_videos (id, playlist_id, video_id, history_duration)
VALUES
    (UUID(), '0e51h6f8-59a0-4ed9-9dcf-f28808a9428d', '66666666-6666-6666-6666-666666666660', 	0);
    


-- =========================================
-- ADD VIDEOS TO USER 1 PLAYLIST: "Chill Vibes"
-- (Dùng video của USER 2 và USER 3)
-- =========================================
INSERT INTO playlist_videos (id, playlist_id, video_id, history_duration)
VALUES
    (UUID(), '0e51e4c5-59a0-4ed9-9dcf-f28808a9428d', '7a1a7181-43be-49a6-9ab7-991a81f3e1b7', 0), -- từ user 2
    (UUID(), '0e51e4c5-59a0-4ed9-9dcf-f28808a9428d', 'a6de7a3c-c20a-4e15-9c36-b75c94c07388', 0); -- từ user 3

-- =========================================
-- ADD VIDEOS TO USER 1 PLAYLIST: "Mr Beast Videos"
-- (Dùng video của USER 3)
-- =========================================
INSERT INTO playlist_videos (id, playlist_id, video_id, history_duration)
VALUES
    (UUID(), '0e5124c4-59a0-4ed9-9dcf-f28808a9428d', 'a6de7a3c-c20a-4e15-9c36-b75c94c07388', 0),
    (UUID(), '0e5124c4-59a0-4ed9-9dcf-f28808a9428d', '0b5c8c6a-1a68-4f2a-93c4-48f67cb8f6d7', 0),
    (UUID(), '0e5124c4-59a0-4ed9-9dcf-f28808a9428d', 'c5a4e07e-3b17-4d38-b60b-07a019f5eec3', 0);

-- =========================================
-- ADD VIDEOS TO USER 2 PLAYLIST: "My Study Music"
-- (Dùng video của USER 1 và USER 3)
-- =========================================
INSERT INTO playlist_videos (id, playlist_id, video_id, history_duration)
VALUES
    (UUID(), '5c39c1f7-4a3b-4e68-9527-c50a1aef1453', '91bc4b46-91f9-433a-b928-6b524b13f2b9', 0), -- từ user 1
    (UUID(), '5c39c1f7-4a3b-4e68-9527-c50a1aef1453', '38c75418-5df5-4e31-8a67-91f9c893bff1', 0), -- từ user 1
    (UUID(), '5c39c1f7-4a3b-4e68-9527-c50a1aef1453', 'c5a4e07e-3b17-4d38-b60b-07a019f5eec3', 0); -- từ user 3


-- =========================================
-- ADD VIDEOS TO USER 3 PLAYLIST: "Workout Motivation"
-- (Dùng video của USER 1 và USER 2)
-- =========================================
INSERT INTO playlist_videos (id, playlist_id, video_id, history_duration)
VALUES
    (UUID(), 'ecfb788e-2c97-4f06-ae32-471f199e66c3', 'd5f6db34-4a0a-4184-a83c-95f4242a4f5e', 0), -- từ user 1
    (UUID(), 'ecfb788e-2c97-4f06-ae32-471f199e66c3', '25b3a8d5-bb94-4f8f-b97b-370d7e2a1e8c', 0), -- từ user 2
    (UUID(), 'ecfb788e-2c97-4f06-ae32-471f199e66c3', '1cd08f89-82b7-4f2c-94fa-3993c3dfde8a', 0); -- từ user 2
