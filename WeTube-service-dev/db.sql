create database Wetube_service_db;
use Wetube_service_db;

drop TABLE users;
CREATE TABLE users (
    id VARCHAR(36)  PRIMARY KEY,
    user_name VARCHAR(100),
    email VARCHAR(100) UNIQUE,
    password VARCHAR(255) NOT NULL,
    user_status ENUM('pending', 'confirmed', 'completed', 'cancelled') DEFAULT 'pending',
    subsriber_status ENUM('pending', 'subsriber', 'unsubsriber') DEFAULT 'pending',
    is_Premium BOOLEAN DEFAULT FALSE,
    total_views INT DEFAULT 0,
    uploaded_videos INT DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE videos (
    id VARCHAR(36) PRIMARY KEY,
    users_id VARCHAR(36),
    title VARCHAR(255),
    hashtag VARCHAR(100),
    thumbnail_url VARCHAR(255),
    video_url VARCHAR(255),
    videos_status ENUM('pending', 'confirmed', 'completed', 'cancelled') DEFAULT 'pending',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (users_id) REFERENCES users(id)
);

CREATE TABLE comments (
    id VARCHAR(36) PRIMARY KEY,
    content TEXT NOT NULL,
    target_type ENUM('POST', 'VIDEO') NOT NULL,
    target_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    parent_comment_id VARCHAR(36),

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (target_id) REFERENCES videos(id),
    FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE CASCADE
);

CREATE TABLE posts (
    id VARCHAR(36) PRIMARY KEY,
    content TEXT,
    image_url VARCHAR(255),
    like_count INT,
    user_id VARCHAR(36),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE likes (
    id VARCHAR(36) PRIMARY KEY,
    target_id VARCHAR(36),
    target_type ENUM('POST', 'COMMENT', 'VIDEO'),
    user_id VARCHAR(36),
    status BOOLEAN DEFAULT TRUE
);

CREATE TABLE poll_options (
    id VARCHAR(36) PRIMARY KEY,
    post_id VARCHAR(36),
    option_text TEXT,
    created_at TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE TABLE votes (
    id VARCHAR(36) PRIMARY KEY,
    poll_option_id VARCHAR(36),
    user_id VARCHAR(36),
    created_at TIMESTAMP,
    FOREIGN KEY (poll_option_id) REFERENCES poll_options(id) ON DELETE CASCADE
);


CREATE TABLE playlists (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    playlist_type ENUM('None', 'Liked', 'WatchLater', 'UserPlaylist', 'UserUploaded', 'History') DEFAULT 'UserPlaylist',
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE playlist_video (
    playlist_id VARCHAR(36),
    video_id VARCHAR(36),
    history_duration DECIMAL(10,2) DEFAULT 0.00,
    PRIMARY KEY (playlist_id, video_id),
    FOREIGN KEY (playlist_id) REFERENCES playlists(id),
    FOREIGN KEY (video_id) REFERENCES videos(id)
);

CREATE TABLE video_tags (
    video_id VARCHAR(36),
    tag_id VARCHAR(36),
    PRIMARY KEY (video_id, tag_id),
    FOREIGN KEY (video_id) REFERENCES videos(id),
    FOREIGN KEY (tag_id) REFERENCES tags(id)
);

CREATE TABLE tags (
    id VARCHAR(36) PRIMARY KEY,
    content VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE user_tags
DROP COLUMN relevance;
CREATE TABLE user_tags (
    user_id VARCHAR(36),
    tag_id VARCHAR(36),
    relevance DECIMAL(10,2) DEFAULT 0.00,
created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, tag_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (tag_id) REFERENCES tags(id)
);

CREATE TABLE categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,              
    description TEXT,                          
    slug VARCHAR(100) UNIQUE,                  
    parent_id INT DEFAULT NULL,                
    display_order INT DEFAULT 0,               
    is_active BOOLEAN DEFAULT TRUE,            
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (parent_id) REFERENCES categories(id)
);


CREATE TABLE video_categories (
    video_id VARCHAR(36),
    category_id INT,
    PRIMARY KEY (video_id, category_id),
    FOREIGN KEY (video_id) REFERENCES videos(id),
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE user_categories (
    users_id VARCHAR(36),
    category_id INT,
    relevance DECIMAL(10,2) DEFAULT 0.00,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (users_id, category_id),
    FOREIGN KEY (users_id) REFERENCES users(id),
     FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY,
    sender_id VARCHAR(36),
    receiver_id VARCHAR(36),
    title VARCHAR(255),
    content TEXT,
    type ENUM(
        'Subscribed', 'LikeVideo', 'CommentVideo', 'NewPost',
        'LikePost', 'CommentPost', 'NewVideoFromSubscribed', 'Report'
    ) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id)
);









