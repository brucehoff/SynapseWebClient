CREATE TABLE `JDODATEANNOTATION` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ATTRIBUTE` varchar(256) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL,
  `OWNER_ID` bigint(20) NOT NULL,
  `VALUE` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `JDODATEANNOTATION_N49` (`OWNER_ID`),
  CONSTRAINT `DATE_ANNON_OWNER_FK` FOREIGN KEY (`OWNER_ID`) REFERENCES `JDONODE` (`ID`) ON DELETE CASCADE
)