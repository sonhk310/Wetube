-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: wetube_local_db
-- ------------------------------------------------------
-- Server version	8.0.43
use wetube_local_db;
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `category_videos`
--

DROP TABLE IF EXISTS `category_videos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category_videos` (
  `id` varchar(36) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `order_position` int NOT NULL,
  `category_id` varchar(36) NOT NULL,
  `video_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpylkmxixjle2vn15d33buhetr` (`category_id`),
  KEY `FKyud2ls22qj1fv0h1miao9rlo` (`video_id`),
  CONSTRAINT `FKpylkmxixjle2vn15d33buhetr` FOREIGN KEY (`category_id`) REFERENCES `channel_categories` (`id`),
  CONSTRAINT `FKyud2ls22qj1fv0h1miao9rlo` FOREIGN KEY (`video_id`) REFERENCES `videos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `channel_categories`
--

DROP TABLE IF EXISTS `channel_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `channel_categories` (
  `id` varchar(36) NOT NULL,
  `category_type` enum('FEATURED','FOR_YOU','LIVE','MEMBERSHIP','MEMBERSHIP_VIDEO','POPULAR','SHORT','VIDEO') NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_visible` bit(1) NOT NULL,
  `order_position` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `channel_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKfl6as5ees3lf46b2wj4q4tsei` (`channel_id`),
  CONSTRAINT `FKfl6as5ees3lf46b2wj4q4tsei` FOREIGN KEY (`channel_id`) REFERENCES `channels` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `channels`
--

DROP TABLE IF EXISTS `channels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `channels` (
  `id` varchar(36) NOT NULL,
  `background_img_url` varchar(255) DEFAULT NULL,
  `country_code` varchar(2) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `picture` varchar(255) DEFAULT NULL,
  `revenue` float NOT NULL,
  `status` enum('ACTIVE','INACTIVE','NONE','PENDING','PUBLIC') DEFAULT NULL,
  `total_subscribers` int NOT NULL,
  `total_videos` int NOT NULL,
  `total_views` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `comments`
--

DROP TABLE IF EXISTS `comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comments` (
  `id` varchar(36) NOT NULL,
  `content` text,
  `created_at` datetime(6) DEFAULT NULL,
  `like_count` int DEFAULT NULL,
  `parent_comment_id` binary(16) DEFAULT NULL,
  `target_id` varchar(36) NOT NULL,
  `target_type` enum('COMMENT','POST','VIDEO') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8omq0tc18jd43bu5tjh6jvraq` (`user_id`),
  CONSTRAINT `FK8omq0tc18jd43bu5tjh6jvraq` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `likes`
--

DROP TABLE IF EXISTS `likes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `likes` (
  `id` varchar(36) NOT NULL,
  `status` bit(1) DEFAULT NULL,
  `target_id` varchar(36) NOT NULL,
  `target_type` enum('COMMENT','POST','VIDEO') NOT NULL,
  `user_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oauth_accounts`
--

DROP TABLE IF EXISTS `oauth_accounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oauth_accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `picture` varchar(255) DEFAULT NULL,
  `provider` varchar(255) NOT NULL,
  `provider_user_id` varchar(255) NOT NULL,
  `raw_info` json DEFAULT NULL,
  `scopes` text,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKqattdvs18omb33um7ge1tb5ex` (`provider`,`provider_user_id`),
  KEY `FKn6j2b3ur1fcj3vkodrmqtdxmi` (`user_id`),
  CONSTRAINT `FKn6j2b3ur1fcj3vkodrmqtdxmi` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment_momo`
--

DROP TABLE IF EXISTS `payment_momo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_momo` (
  `id` varchar(36) NOT NULL,
  `amount` bigint DEFAULT NULL,
  `create_at` datetime(6) DEFAULT NULL,
  `event_type` varchar(255) DEFAULT NULL,
  `order_id` varchar(255) DEFAULT NULL,
  `payload` json DEFAULT NULL,
  `provider` varchar(255) DEFAULT NULL,
  `request_id` varchar(255) DEFAULT NULL,
  `status` enum('FAILED','PENDING','SUCCESS') DEFAULT NULL,
  `update_at` datetime(6) DEFAULT NULL,
  `premium_user_id` varchar(36) DEFAULT NULL,
  `sub_package_id` varchar(36) DEFAULT NULL,
  `user_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKlov3r756tdbr25ap1roks2hjn` (`premium_user_id`),
  KEY `FKlpot9pqkpa533a01haw4jaucm` (`sub_package_id`),
  KEY `FKoyrqtcpdglnp5ti6deijykiog` (`user_id`),
  CONSTRAINT `FKlov3r756tdbr25ap1roks2hjn` FOREIGN KEY (`premium_user_id`) REFERENCES `premium_user` (`id`),
  CONSTRAINT `FKlpot9pqkpa533a01haw4jaucm` FOREIGN KEY (`sub_package_id`) REFERENCES `sub_package` (`id`),
  CONSTRAINT `FKoyrqtcpdglnp5ti6deijykiog` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `playlist_videos`
--

DROP TABLE IF EXISTS `playlist_videos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `playlist_videos` (
  `id` varchar(36) NOT NULL,
  `history_duration` float NOT NULL,
  `playlist_id` varchar(36) NOT NULL,
  `video_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3ex8uu6wos3w18w1df8ruapgw` (`playlist_id`,`video_id`),
  KEY `FKagtsknljsxw3o4bsmt6svs18l` (`video_id`),
  CONSTRAINT `FK9k328sk1nnj1oihl99w8w0nuw` FOREIGN KEY (`playlist_id`) REFERENCES `playlists` (`id`),
  CONSTRAINT `FKagtsknljsxw3o4bsmt6svs18l` FOREIGN KEY (`video_id`) REFERENCES `videos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `playlists`
--

DROP TABLE IF EXISTS `playlists`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `playlists` (
  `id` varchar(36) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `playlist_type` enum('FOR_YOU','HISTORY','LIKED','MEMBERSHIP','NONE','POPULAR','SHORT','USER_PLAYLIST','USER_UPLOADED','WATCH_LATER') NOT NULL,
  `title` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKb12gqrjwapun82tja4djyyo8d` (`user_id`,`title`),
  CONSTRAINT `FKtgjwvfg23v990xk7k0idmqbrj` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `poll_options`
--

DROP TABLE IF EXISTS `poll_options`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `poll_options` (
  `id` binary(16) NOT NULL,
  `option_text` varchar(500) NOT NULL,
  `post_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_poll_option_post_id` (`post_id`),
  CONSTRAINT `FK3xibw3b09jcqvgkol8cb8ueyd` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `posts`
--

DROP TABLE IF EXISTS `posts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `posts` (
  `id` binary(16) NOT NULL,
  `comment_count` int DEFAULT NULL,
  `content` text,
  `created_at` datetime(6) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `like_count` int DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK5lidm6cqbc7u4xhqpxm898qme` (`user_id`),
  CONSTRAINT `FK5lidm6cqbc7u4xhqpxm898qme` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `premium_user`
--

DROP TABLE IF EXISTS `premium_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `premium_user` (
  `id` varchar(36) NOT NULL,
  `create_at` datetime(6) DEFAULT NULL,
  `end_date` date NOT NULL,
  `start_date` date NOT NULL,
  `status` enum('ACTIVE','INACTIVE','NONE','PENDING','PUBLIC') DEFAULT NULL,
  `update_at` datetime(6) DEFAULT NULL,
  `sub_package_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKamqf76b705cbx93cb11gj374p` (`sub_package_id`),
  KEY `FK4bf4mhbgdvvjl19r07q0fju8r` (`user_id`),
  CONSTRAINT `FK4bf4mhbgdvvjl19r07q0fju8r` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKamqf76b705cbx93cb11gj374p` FOREIGN KEY (`sub_package_id`) REFERENCES `sub_package` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `refresh_tokens`
--

DROP TABLE IF EXISTS `refresh_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refresh_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `parent_token_hash` varchar(255) DEFAULT NULL,
  `revoked` bit(1) NOT NULL,
  `session_id` varchar(36) NOT NULL,
  `token_hash` varchar(88) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_refresh_session` (`session_id`),
  UNIQUE KEY `UKo2mlirhldriil2y7krapq4frt` (`token_hash`),
  KEY `FK1lih5y2npsf8u5o3vhdb9y0os` (`user_id`),
  CONSTRAINT `FK1lih5y2npsf8u5o3vhdb9y0os` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKch1113horj4qr56f91omojv8` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rooms`
--

DROP TABLE IF EXISTS `rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `room_id` varchar(255) NOT NULL,
  `room_name` varchar(255) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK522j1g1kig0go3c1o8r8gvu7g` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sub_package`
--

DROP TABLE IF EXISTS `sub_package`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sub_package` (
  `id` varchar(36) NOT NULL,
  `create_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `duration_days` int NOT NULL,
  `name` varchar(255) NOT NULL,
  `price` decimal(38,2) NOT NULL,
  `update_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `subscriptions`
--

DROP TABLE IF EXISTS `subscriptions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscriptions` (
  `created_at` datetime(6) DEFAULT NULL,
  `notification_mode` enum('ALL','NONE_NOTIFICATION','PERSONALIZE') DEFAULT NULL,
  `user_id` varchar(36) NOT NULL,
  `tier_id` varchar(36) NOT NULL,
  PRIMARY KEY (`user_id`,`tier_id`),
  KEY `FK8egrl9vajmj9gi2sdiib4hg9t` (`tier_id`),
  CONSTRAINT `FK8egrl9vajmj9gi2sdiib4hg9t` FOREIGN KEY (`tier_id`) REFERENCES `tiers` (`id`),
  CONSTRAINT `FKhro52ohfqfbay9774bev0qinr` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tags`
--

DROP TABLE IF EXISTS `tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tags` (
  `id` varchar(36) NOT NULL,
  `count` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `name` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tiers`
--

DROP TABLE IF EXISTS `tiers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tiers` (
  `id` varchar(36) NOT NULL,
  `is_default` bit(1) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `price` float NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `channel_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_title_price` (`channel_id`,`title`,`price`),
  CONSTRAINT `FKr3jom8sxpg4jeydwa0vsnmyec` FOREIGN KEY (`channel_id`) REFERENCES `channels` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `transactions`
--

DROP TABLE IF EXISTS `transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transactions` (
  `id` varchar(36) NOT NULL,
  `amount` decimal(38,2) NOT NULL,
  `channel_revenue` decimal(38,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `wetube_fee` decimal(38,2) NOT NULL,
  `channel_id` varchar(36) NOT NULL,
  `membership_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK3npytqlngupfe3e1sgxpmymx8` (`channel_id`),
  KEY `FKq43s8gyapcrdhmo6rnor11haw` (`membership_id`),
  KEY `FKqwv7rmvc8va8rep7piikrojds` (`user_id`),
  CONSTRAINT `FK3npytqlngupfe3e1sgxpmymx8` FOREIGN KEY (`channel_id`) REFERENCES `channels` (`id`),
  CONSTRAINT `FKq43s8gyapcrdhmo6rnor11haw` FOREIGN KEY (`membership_id`) REFERENCES `tiers` (`id`),
  CONSTRAINT `FKqwv7rmvc8va8rep7piikrojds` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_tag`
--

DROP TABLE IF EXISTS `user_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_tag` (
  `tag_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `point` double NOT NULL,
  PRIMARY KEY (`tag_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` varchar(36) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `picture` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `channel_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UK7it5mrsltdf8f93rppibu1jpq` (`channel_id`),
  CONSTRAINT `FKifnkmsx2hj7nygwj9wxf99ah3` FOREIGN KEY (`channel_id`) REFERENCES `channels` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users_roles`
--

DROP TABLE IF EXISTS `users_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_roles` (
  `role_id` bigint NOT NULL,
  `user_id` varchar(36) NOT NULL,
  PRIMARY KEY (`role_id`,`user_id`),
  KEY `FK2o0jvgh89lemvvo17cbqvdxaa` (`user_id`),
  CONSTRAINT `FK2o0jvgh89lemvvo17cbqvdxaa` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKj6m8fwv7oqv74fcehir1a9ffy` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `video_tags`
--

DROP TABLE IF EXISTS `video_tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video_tags` (
  `id` varchar(36) NOT NULL,
  `tag_id` varchar(36) NOT NULL,
  `video_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_video_tag` (`video_id`,`tag_id`),
  KEY `FKqfnpe46owtsy6c6t5my7exjmq` (`tag_id`),
  CONSTRAINT `FKpr6ks7ia3ilx9ec2mmwb82lb6` FOREIGN KEY (`video_id`) REFERENCES `videos` (`id`),
  CONSTRAINT `FKqfnpe46owtsy6c6t5my7exjmq` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `videos`
--

DROP TABLE IF EXISTS `videos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `videos` (
  `id` varchar(36) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `duration` float NOT NULL,
  `thumbnail_url` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `total_view` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `video_url` varchar(255) DEFAULT NULL,
  `videos_status` enum('ACTIVE','INACTIVE','NONE','PENDING','PUBLIC') DEFAULT NULL,
  `users_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKonafuk8aki7wdhw3pr2fhfwk8` (`users_id`),
  CONSTRAINT `FKonafuk8aki7wdhw3pr2fhfwk8` FOREIGN KEY (`users_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `votes`
--

DROP TABLE IF EXISTS `votes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `votes` (
  `id` binary(16) NOT NULL,
  `poll_option_id` binary(16) NOT NULL,
  `post_id` binary(16) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_vote_post_user` (`user_id`,`post_id`),
  KEY `FK2ytse4emviescgg6gr9o91bro` (`poll_option_id`),
  KEY `FK1m2jqtro85c13ya5kv0kvkc97` (`post_id`),
  CONSTRAINT `FK1m2jqtro85c13ya5kv0kvkc97` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`),
  CONSTRAINT `FK2ytse4emviescgg6gr9o91bro` FOREIGN KEY (`poll_option_id`) REFERENCES `poll_options` (`id`),
  CONSTRAINT `FKli4uj3ic2vypf5pialchj925e` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

INSERT IGNORE INTO roles (id, code) VALUES (1, 'ROLE_USER');
INSERT IGNORE INTO roles (id, code) VALUES (2, 'ROLE_ADMIN');
-- Dump completed on 2025-09-19 15:54:00
