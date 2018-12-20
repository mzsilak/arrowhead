CREATE DATABASE  IF NOT EXISTS `arrowhead`;
USE `arrowhead`;

CREATE TABLE  IF NOT EXISTS `hibernate_sequence` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

LOCK TABLES `hibernate_sequence` WRITE, `hibernate_sequence` AS hs READ;
INSERT INTO `hibernate_sequence` SELECT (1) FROM DUAL WHERE NOT EXISTS (SELECT * FROM `hibernate_sequence` AS hs);
UNLOCK TABLES;
