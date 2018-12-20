CREATE DATABASE  IF NOT EXISTS `arrowhead`;
USE `arrowhead`;

CREATE TABLE IF NOT EXISTS `service_registry` (
  `id` bigint(20) NOT NULL,
  `end_of_validity` datetime(6) DEFAULT NULL,
  `metadata` varchar(255) DEFAULT NULL,
  `service_uri` varchar(255) DEFAULT NULL,
  `udp` char(1) DEFAULT NULL,
  `version` int(11) DEFAULT NULL,
  `arrowhead_service_id` bigint(20) NOT NULL,
  `provider_system_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3q3tqiu7f92u946p33plj5fxq` (`arrowhead_service_id`,`provider_system_id`),
  KEY `FK4lc944mp4x24pr09wuxbb08ky` (`provider_system_id`),
  CONSTRAINT `FK4lc944mp4x24pr09wuxbb08ky` FOREIGN KEY (`provider_system_id`) REFERENCES `arrowhead_system` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKr0x7pvbi16w5b6ao6q43t606p` FOREIGN KEY (`arrowhead_service_id`) REFERENCES `arrowhead_service` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
