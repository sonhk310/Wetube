-- 1. Users
INSERT INTO users (id, email, name, picture, created_at, updated_at, password)
VALUES
-- (UUID(), 'alice@example.com', 'Alice', 'https://picsum.photos/200?1', NOW(), NOW(), 'pass'),
(UUID(), 'bob@example.com',   'Bob',   'https://picsum.photos/200?2', NOW(), NOW(), 'pass'),
(UUID(), 'carol@example.com', 'Carol', 'https://picsum.photos/200?3', NOW(), NOW(), 'pass');

-- 2. Tags
INSERT INTO tags (id, name, created_at, count)
VALUES
(UUID(), 'Sports',  NOW(), 0),
(UUID(), 'Music',   NOW(), 0),
(UUID(), 'Anime',   NOW(), 0),
(UUID(), 'Action',  NOW(), 0),
(UUID(), 'Comedy',  NOW(), 0);

-- 3. Videos
-- Giả định có enum ActiveStatus { PUBLIC, PRIVATE, DRAFT }
INSERT INTO videos (id, users_id, title, description, thumbnail_url, video_Url, videos_Status, total_View, duration, created_At, updated_At)
VALUES
(UUID(), (SELECT id FROM users LIMIT 1), 'Naruto AMV', 'Anime music video', 'https://picsum.photos/300?11', 'http://videos.com/naruto.mp4', 'PUBLIC', 1200, 180, NOW(), NOW()),
(UUID(), (SELECT id FROM users LIMIT 1 OFFSET 1), 'Football Highlights', 'Match recap', 'https://picsum.photos/300?12', 'http://videos.com/football.mp4', 'PUBLIC', 900, 300, NOW(), NOW()),
(UUID(), (SELECT id FROM users LIMIT 1 OFFSET 2), 'Stand-up Comedy', 'Funny jokes', 'https://picsum.photos/300?13', 'http://videos.com/comedy.mp4', 'PUBLIC', 500, 200, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='alice@example.com'), 'One Piece Opening', 'Anime opening theme', 'https://picsum.photos/300?21', 'http://videos.com/onepiece.mp4', 'PUBLIC', 2200, 210, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='alice@example.com'), 'Top 10 Anime Fights', 'Compilation of best anime battles', 'https://picsum.photos/300?22', 'http://videos.com/animefights.mp4', 'PUBLIC', 1800, 400, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='bob@example.com'), 'Basketball Tricks', 'Amazing basketball skills', 'https://picsum.photos/300?23', 'http://videos.com/basketball.mp4', 'PUBLIC', 750, 180, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='bob@example.com'), 'Funny Football Moments', 'Comedy + Sports mix', 'https://picsum.photos/300?24', 'http://videos.com/funnyfootball.mp4', 'PUBLIC', 600, 240, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='carol@example.com'), 'Stand-up Special 2025', 'Full comedy show', 'https://picsum.photos/300?25', 'http://videos.com/standup.mp4', 'PUBLIC', 1300, 3600, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='alice@example.com'), 'Dragon Ball Z Battles', 'Epic DBZ fights', 'https://picsum.photos/300?31', 'http://videos.com/dbz.mp4', 'PUBLIC', 1500, 420, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='alice@example.com'), 'Anime OST Mix', 'Best anime soundtracks', 'https://picsum.photos/300?32', 'http://videos.com/animeost.mp4', 'PUBLIC', 980, 360, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='bob@example.com'), 'World Cup Goals', 'Best football goals', 'https://picsum.photos/300?33', 'http://videos.com/worldcup.mp4', 'PUBLIC', 3100, 480, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='bob@example.com'), 'NBA Highlights', 'Basketball top plays', 'https://picsum.photos/300?34', 'http://videos.com/nba.mp4', 'PUBLIC', 2750, 600, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='carol@example.com'), 'Comedy Skits Compilation', 'Funny short videos', 'https://picsum.photos/300?35', 'http://videos.com/comedyskits.mp4', 'PUBLIC', 2100, 540, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='alice@example.com'), 'Attack on Titan Trailer', 'AoT epic scenes', 'https://picsum.photos/300?36', 'http://videos.com/aot.mp4', 'PUBLIC', 1900, 180, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='bob@example.com'), 'Extreme Sports Fails', 'Funny and dangerous sports fails', 'https://picsum.photos/300?37', 'http://videos.com/extremefails.mp4', 'PUBLIC', 1250, 240, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='carol@example.com'), 'Stand-up 2024 Highlights', 'Comedy event recap', 'https://picsum.photos/300?38', 'http://videos.com/comedy2024.mp4', 'PUBLIC', 850, 3600, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='alice@example.com'), 'Full Metal Alchemist OST', 'Emotional anime soundtrack', 'https://picsum.photos/300?39', 'http://videos.com/fmaost.mp4', 'PUBLIC', 1400, 300, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='bob@example.com'), 'Funny Basketball Bloopers', 'Basketball comedy moments', 'https://picsum.photos/300?40', 'http://videos.com/basketballfunny.mp4', 'PUBLIC', 950, 200, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='carol@example.com'), 'Comedy Night Show', 'Stand-up and jokes', 'https://picsum.photos/300?41', 'http://videos.com/comedynight.mp4', 'PUBLIC', 2200, 4000, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='alice@example.com'), 'Naruto Shippuden OST', 'Naruto soundtrack collection', 'https://picsum.photos/300?42', 'http://videos.com/narutoost.mp4', 'PUBLIC', 1050, 250, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='bob@example.com'), 'Skateboarding Tricks', 'Awesome skateboard skills', 'https://picsum.photos/300?43', 'http://videos.com/skateboard.mp4', 'PUBLIC', 1650, 300, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='carol@example.com'), 'Funny Pranks Compilation', 'Best pranks of the year', 'https://picsum.photos/300?44', 'http://videos.com/pranks.mp4', 'PUBLIC', 3100, 600, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='alice@example.com'), 'Bleach Anime Trailer', 'Bleach new arc', 'https://picsum.photos/300?45', 'http://videos.com/bleach.mp4', 'PUBLIC', 2000, 220, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='bob@example.com'), 'Tennis Grand Slam', 'Highlights from tennis match', 'https://picsum.photos/300?46', 'http://videos.com/tennis.mp4', 'PUBLIC', 850, 500, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='carol@example.com'), 'Comedy Stand-up Mix', 'Mixed stand-up jokes', 'https://picsum.photos/300?47', 'http://videos.com/comedy_mix.mp4', 'PUBLIC', 1750, 1800, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='alice@example.com'), 'Anime Funny Moments', 'Funny clips from anime', 'https://picsum.photos/300?48', 'http://videos.com/animefunny.mp4', 'PUBLIC', 2300, 350, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='bob@example.com'), 'Olympics Highlights', 'Summer Olympics best moments', 'https://picsum.photos/300?49', 'http://videos.com/olympics.mp4', 'PUBLIC', 2700, 800, NOW(), NOW()),
(UUID(), (SELECT id FROM users WHERE email='carol@example.com'), 'Roast Comedy Show', 'Roasting event funny clips', 'https://picsum.photos/300?50', 'http://videos.com/roast.mp4', 'PUBLIC', 1850, 4200, NOW(), NOW());

SELECT * FROM tags;

-- 4. Video_Tags (liên kết video với tag)
INSERT INTO video_tags (id, video_id, tag_id)
VALUES
(UUID(), (SELECT id FROM videos WHERE title='Naruto AMV'), (SELECT id FROM tags WHERE name='Anime')),
(UUID(), (SELECT id FROM videos WHERE title='Naruto AMV'), (SELECT id FROM tags WHERE name='Music')),
(UUID(), (SELECT id FROM videos WHERE title='Football Highlights'), (SELECT id FROM tags WHERE name='Sports')),
(UUID(), (SELECT id FROM videos WHERE title='Stand-up Comedy'), (SELECT id FROM tags WHERE name='Comedy')),
-- One Piece Opening: Anime + Music
(UUID(), (SELECT id FROM videos WHERE title='One Piece Opening'), (SELECT id FROM tags WHERE name='Anime')),
(UUID(), (SELECT id FROM videos WHERE title='One Piece Opening'), (SELECT id FROM tags WHERE name='Music')),

-- Top 10 Anime Fights: Anime + Action
(UUID(), (SELECT id FROM videos WHERE title='Top 10 Anime Fights'), (SELECT id FROM tags WHERE name='Anime')),
(UUID(), (SELECT id FROM videos WHERE title='Top 10 Anime Fights'), (SELECT id FROM tags WHERE name='Action')),

-- Basketball Tricks: Sports
(UUID(), (SELECT id FROM videos WHERE title='Basketball Tricks'), (SELECT id FROM tags WHERE name='Sports')),

-- Funny Football Moments: Sports + Comedy
(UUID(), (SELECT id FROM videos WHERE title='Funny Football Moments'), (SELECT id FROM tags WHERE name='Sports')),
(UUID(), (SELECT id FROM videos WHERE title='Funny Football Moments'), (SELECT id FROM tags WHERE name='Comedy')),

-- Stand-up Special 2025: Comedy
(UUID(), (SELECT id FROM videos WHERE title='Stand-up Special 2025'), (SELECT id FROM tags WHERE name='Comedy')),
-- Dragon Ball Z Battles: Anime + Action
(UUID(), (SELECT id FROM videos WHERE title='Dragon Ball Z Battles'), (SELECT id FROM tags WHERE name='Anime')),
(UUID(), (SELECT id FROM videos WHERE title='Dragon Ball Z Battles'), (SELECT id FROM tags WHERE name='Action')),

-- Anime OST Mix: Anime + Music
(UUID(), (SELECT id FROM videos WHERE title='Anime OST Mix'), (SELECT id FROM tags WHERE name='Anime')),
(UUID(), (SELECT id FROM videos WHERE title='Anime OST Mix'), (SELECT id FROM tags WHERE name='Music')),

-- World Cup Goals: Sports
(UUID(), (SELECT id FROM videos WHERE title='World Cup Goals'), (SELECT id FROM tags WHERE name='Sports')),

-- NBA Highlights: Sports + Action
(UUID(), (SELECT id FROM videos WHERE title='NBA Highlights'), (SELECT id FROM tags WHERE name='Sports')),
(UUID(), (SELECT id FROM videos WHERE title='NBA Highlights'), (SELECT id FROM tags WHERE name='Action')),

-- Comedy Skits Compilation: Comedy
(UUID(), (SELECT id FROM videos WHERE title='Comedy Skits Compilation'), (SELECT id FROM tags WHERE name='Comedy')),

-- Attack on Titan Trailer: Anime + Action
(UUID(), (SELECT id FROM videos WHERE title='Attack on Titan Trailer'), (SELECT id FROM tags WHERE name='Anime')),
(UUID(), (SELECT id FROM videos WHERE title='Attack on Titan Trailer'), (SELECT id FROM tags WHERE name='Action')),

-- Extreme Sports Fails: Sports + Comedy
(UUID(), (SELECT id FROM videos WHERE title='Extreme Sports Fails'), (SELECT id FROM tags WHERE name='Sports')),
(UUID(), (SELECT id FROM videos WHERE title='Extreme Sports Fails'), (SELECT id FROM tags WHERE name='Comedy')),

-- Stand-up 2024 Highlights: Comedy
(UUID(), (SELECT id FROM videos WHERE title='Stand-up 2024 Highlights'), (SELECT id FROM tags WHERE name='Comedy')),

-- Full Metal Alchemist OST: Anime + Music
(UUID(), (SELECT id FROM videos WHERE title='Full Metal Alchemist OST'), (SELECT id FROM tags WHERE name='Anime')),
(UUID(), (SELECT id FROM videos WHERE title='Full Metal Alchemist OST'), (SELECT id FROM tags WHERE name='Music')),

-- Funny Basketball Bloopers: Sports + Comedy
(UUID(), (SELECT id FROM videos WHERE title='Funny Basketball Bloopers'), (SELECT id FROM tags WHERE name='Sports')),
(UUID(), (SELECT id FROM videos WHERE title='Funny Basketball Bloopers'), (SELECT id FROM tags WHERE name='Comedy')),

-- Comedy Night Show: Comedy
(UUID(), (SELECT id FROM videos WHERE title='Comedy Night Show'), (SELECT id FROM tags WHERE name='Comedy')),

-- Naruto Shippuden OST: Anime + Music
(UUID(), (SELECT id FROM videos WHERE title='Naruto Shippuden OST'), (SELECT id FROM tags WHERE name='Anime')),
(UUID(), (SELECT id FROM videos WHERE title='Naruto Shippuden OST'), (SELECT id FROM tags WHERE name='Music')),

-- Skateboarding Tricks: Sports
(UUID(), (SELECT id FROM videos WHERE title='Skateboarding Tricks'), (SELECT id FROM tags WHERE name='Sports')),

-- Funny Pranks Compilation: Comedy
(UUID(), (SELECT id FROM videos WHERE title='Funny Pranks Compilation'), (SELECT id FROM tags WHERE name='Comedy')),

-- Bleach Anime Trailer: Anime + Action
(UUID(), (SELECT id FROM videos WHERE title='Bleach Anime Trailer'), (SELECT id FROM tags WHERE name='Anime')),
(UUID(), (SELECT id FROM videos WHERE title='Bleach Anime Trailer'), (SELECT id FROM tags WHERE name='Action')),

-- Tennis Grand Slam: Sports
(UUID(), (SELECT id FROM videos WHERE title='Tennis Grand Slam'), (SELECT id FROM tags WHERE name='Sports')),

-- Comedy Stand-up Mix: Comedy
(UUID(), (SELECT id FROM videos WHERE title='Comedy Stand-up Mix'), (SELECT id FROM tags WHERE name='Comedy')),

-- Anime Funny Moments: Anime + Comedy
(UUID(), (SELECT id FROM videos WHERE title='Anime Funny Moments'), (SELECT id FROM tags WHERE name='Anime')),
(UUID(), (SELECT id FROM videos WHERE title='Anime Funny Moments'), (SELECT id FROM tags WHERE name='Comedy')),

-- Olympics Highlights: Sports + Action
(UUID(), (SELECT id FROM videos WHERE title='Olympics Highlights'), (SELECT id FROM tags WHERE name='Sports')),
(UUID(), (SELECT id FROM videos WHERE title='Olympics Highlights'), (SELECT id FROM tags WHERE name='Action')),

-- Roast Comedy Show: Comedy
(UUID(), (SELECT id FROM videos WHERE title='Roast Comedy Show'), (SELECT id FROM tags WHERE name='Comedy')),

(UUID(), (SELECT id FROM videos WHERE title = 'Alice AMV 2!'), (SELECT id FROM tags WHERE name = 'Anime')),
(UUID(), (SELECT id FROM videos WHERE title = 'Alice AMV 2!'), (SELECT id FROM tags WHERE name = 'Music'));

SELECT * FROM wetube_local_db.user_tag;
-- 5. User_Tag (user quan tâm tag nào, điểm bao nhiêu)
INSERT INTO user_tag (user_Id, tag_Id, point, created_at)
VALUES
((SELECT id FROM users WHERE email='alice@example.com'), (SELECT id FROM tags WHERE name='Anime'), 0.9, NOW()),
((SELECT id FROM users WHERE email='alice@example.com'), (SELECT id FROM tags WHERE name='Music'), 0.7, NOW()),
((SELECT id FROM users WHERE email='bob@example.com'),   (SELECT id FROM tags WHERE name='Sports'), 0.8, NOW()),
((SELECT id FROM users WHERE email='carol@example.com'), (SELECT id FROM tags WHERE name='Comedy'), 0.95, NOW());
   

