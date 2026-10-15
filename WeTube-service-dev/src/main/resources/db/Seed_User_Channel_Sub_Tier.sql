use Wetube_local_db;

INSERT INTO channels (id, name, picture, background_img_url, description, country_code, status,
                      revenue, total_subscribers, total_videos, total_views, created_at, updated_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'Web Dev Simplified', 
     'https://yt3.googleusercontent.com/ytc/AIdro_nO3F7DfVXaf6wsHPS_hF327ggeWUCwZSELb5DCWBL1aw=s160-c-k-c0x00ffffff-no-rj',
     'https://yt3.googleusercontent.com/lITiXZFL1niq2Uwm1yHmChui9RM50N2O3XdjUwbI6oa3_KhYdHBWfoLriW8wXbubnKwDel9_1A=w1707-fcrop64=1,00005a57ffffa5a8-k-c0xffffffff-no-nd-rj',
     'Web Dev Simplified is all about teaching web development skills and techniques in an efficient and practical manner',
     'US', 'ACTIVE', 150.5, 1700000, 45, 502000, NOW(), NOW()),

    ('11111111-1111-1111-1111-111111111112', 'MRioX', 
     'https://yt3.googleusercontent.com/ytc/AIdro_lzXs22krjqYJ2Kuh8cYbYHD_qpdo01j1RjSX5G2PrJ1w=s160-c-k-c0x00ffffff-no-rj',
     'https://yt3.googleusercontent.com/bhst8ESGUCHoPMC-0hE3lW9p12IaUL3l_OQZfErsBmAO-Y3i6v6lz7V5peoKidfjqfYNG0Yy=w1707-fcrop64=1,00005a57ffffa5a8-k-c0xffffffff-no-nd-rj',
     'RioX Playlist https://riox.lnk.to/rioxspotify

For release under our Record Label, please send your demo (Unreleased) ',
     'CA', 'ACTIVE', 350.75, 5400, 120, 245000, NOW(), NOW()),

    ('11111111-1111-1111-1111-111111111113', 'MrBeast', 
     'https://yt3.googleusercontent.com/nxYrc_1_2f77DoBadyxMTmv7ZpRZapHR5jbuYe7PlPd5cIRJxtNNEYyOC0ZsxaDyJJzXrnJiuDE=s160-c-k-c0x00ffffff-no-rj',
     'https://yt3.googleusercontent.com/5KWiriZZ_KEoEdSMFTJKj2M6vR_XSiRZeQ-ix0cvG3TGZuGoi8sfAjrSiZAP0GzXBkmF8ZGytw=w1707-fcrop64=1,00005a57ffffa5a8-k-c0xffffffff-no-nd-rj',
     'ĐĂNG KÝ LIỀN TAY, BIẾT ĐÂU GẶP MAY!',
     'VN', 'ACTIVE', 220.3, 3200, 88, 156000, NOW(), NOW()),
     
	('11111111-1111-1111-1111-111111111114', 'Alice Channel', 
     'https://yt3.googleusercontent.com/nxYrc_1_2f77DoBadyxMTmv7ZpRZapHR5jbuYe7PlPd5cIRJxtNNEYyOC0ZsxaDyJJzXrnJiuDE=s160-c-k-c0x00ffffff-no-rj',
     'https://yt3.googleusercontent.com/5KWiriZZ_KEoEdSMFTJKj2M6vR_XSiRZeQ-ix0cvG3TGZuGoi8sfAjrSiZAP0GzXBkmF8ZGytw=w1707-fcrop64=1,00005a57ffffa5a8-k-c0xffffffff-no-nd-rj',
     'Alice official channel',
     'US', 'ACTIVE', 220.3, 3200, 88, 156000, NOW(), NOW());
  
  -- Select * from channels;
  
  -- Insert Users (tham chiếu channel_id)
INSERT INTO users (id, email, name, picture, created_at, updated_at, password, channel_id)
VALUES
    ('aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'WebDev@wetube.com', 'Dao Nguyen Dev',
     'https://yt3.googleusercontent.com/ytc/AIdro_nO3F7DfVXaf6wsHPS_hF327ggeWUCwZSELb5DCWBL1aw=s160-c-k-c0x00ffffff-no-rj',
     NOW(), NOW(), 'hashed_password_123', '11111111-1111-1111-1111-111111111111'),

    ('aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaab', 'Riox@wetube.com', 'Mr Rio X',
     'https://yt3.googleusercontent.com/ytc/AIdro_lzXs22krjqYJ2Kuh8cYbYHD_qpdo01j1RjSX5G2PrJ1w=s160-c-k-c0x00ffffff-no-rj',
     NOW(), NOW(), 'hashed_password_456', '11111111-1111-1111-1111-111111111112'),

    ('aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaac', 'MrBeast@wetube.com', 'Mr Beast',
     'https://yt3.googleusercontent.com/nxYrc_1_2f77DoBadyxMTmv7ZpRZapHR5jbuYe7PlPd5cIRJxtNNEYyOC0ZsxaDyJJzXrnJiuDE=s160-c-k-c0x00ffffff-no-rj',
     NOW(), NOW(), 'hashed_password_789', '11111111-1111-1111-1111-111111111113'),
     
	('aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaad', 'alice@example.com','Alice', 
    'https://picsum.photos/200',
    NOW(), NOW(), 'hashed_password_789', '11111111-1111-1111-1111-111111111114');
  -- Select * from users;
  
-- ========================================
-- 2. SEED BẢNG TIERS (MembershipTier)
-- Mỗi channel 2 tier: Basic + Premium
-- ========================================
INSERT INTO tiers (id, channel_id, title, price, description, is_default)
VALUES
-- Channel 1 tiers
('11111111-aaaa-bbbb-cccc-000000000001', '11111111-1111-1111-1111-111111111111', 'Default Tier', 0, 'Default tier', TRUE),
('11111111-aaaa-bbbb-cccc-000000000002', '11111111-1111-1111-1111-111111111111', 'Basic', 2.99, 'Basic membership - badges & emojis', FALSE),
('11111111-aaaa-bbbb-cccc-000000000003', '11111111-1111-1111-1111-111111111111', 'Premium', 4.99, 'Premium membership - extra perks', FALSE),

-- Channel 2 tiers
('11111111-aaaa-bbbb-cccc-000000000004', '11111111-1111-1111-1111-111111111112', 'Default Tier', 0, 'Default tier', TRUE),

-- Channel 3 tiers
('11111111-aaaa-bbbb-cccc-000000000005', '11111111-1111-1111-1111-111111111113', 'Default Tier', 0, 'Default tier', TRUE),
('11111111-aaaa-bbbb-cccc-000000000006', '11111111-1111-1111-1111-111111111113', 'Basic', 2.99, 'Basic membership - badges & emojis', FALSE),
('11111111-aaaa-bbbb-cccc-000000000007', '11111111-1111-1111-1111-111111111113', 'Premium', 4.99, 'Premium membership - extra perks', FALSE);

-- ========================================
-- 3. SEED BẢNG SUBSCRIPTIONS
-- User này đăng ký tier của CHANNEL KHÁC
-- ========================================

INSERT INTO subscriptions (user_id, tier_id, notification_mode, created_at)
VALUES
    -- Dao Nguyen Dev (Channel 1) đăng ký tier default của Channel 2
    ('aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-aaaa-bbbb-cccc-000000000004', 'ALL', NOW()),

    -- Mr Rio X (Channel 2) đăng ký tier default của Channel 3
    ('aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaab', '11111111-aaaa-bbbb-cccc-000000000005', 'PERSONALIZE', NOW()),

    -- Mr Beast (Channel 3) đăng ký tier default của Channel 1
    ('aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaac', '11111111-aaaa-bbbb-cccc-000000000001', 'NONE_NOTIFICATION', NOW());

