CREATE DATABASE  IF NOT EXISTS `arrowhead`;
USE `arrowhead`;

CREATE TABLE IF NOT EXISTS `arrowhead_system` (
  `id` bigint(20) NOT NULL,
  `address` varchar(255) NOT NULL,
  `authentication_info` varchar(2047) DEFAULT NULL,
  `port` int(11) NOT NULL,
  `system_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjiab72gx1c0711gjfr39mhck9` (`system_name`,`address`,`port`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;