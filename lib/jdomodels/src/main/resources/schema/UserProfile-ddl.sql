CREATE TABLE `JDOUSERPROFILE` (
  `ID` bigint(20) NOT NULL,
  `ETAG` bigint(20) NOT NULL,
  `PROPERTIES` mediumblob,
  `ANNOTATIONS` mediumblob,
  `NAME` varchar(256) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `USERPROFILE_UNIQUE_NAME` (`NAME`)
)