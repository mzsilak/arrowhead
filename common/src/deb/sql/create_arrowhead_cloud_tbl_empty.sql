CREATE DATABASE  IF NOT EXISTS `arrowhead`;
USE `arrowhead`;

CREATE TABLE IF NOT EXISTS `arrowhead_cloud` (
  `id` bigint(20) NOT NULL,
  `address` varchar(255) NOT NULL,
  `authentication_info` varchar(2047) DEFAULT NULL,
  `cloud_name` varchar(255) NOT NULL,
  `gatekeeper_service_uri` varchar(255) NOT NULL,
  `operator` varchar(255) NOT NULL,
  `port` int(11) NOT NULL,
  `is_secure` char(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9cjou6d7x3w0pvnnb27bc4c4d` (`operator`,`cloud_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;