CREATE DATABASE  IF NOT EXISTS `arrowhead`;
USE `arrowhead`;

CREATE TABLE IF NOT EXISTS `arrowhead_service_interfaces` (
  `arrowhead_service_id` bigint(20) NOT NULL,
  `interfaces` varchar(255) DEFAULT NULL,
  KEY `FKsb09f6kft101e8rixhm5t53f3` (`arrowhead_service_id`),
  CONSTRAINT `FKsb09f6kft101e8rixhm5t53f3` FOREIGN KEY (`arrowhead_service_id`) REFERENCES `arrowhead_service` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
