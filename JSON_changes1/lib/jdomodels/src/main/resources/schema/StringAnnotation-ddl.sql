CREATE TABLE `JDOSTRINGANNOTATION` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ATTRIBUTE` varchar(256) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL,
  `OWNER_ID` bigint(20) NOT NULL,
  `VALUE` varchar(500) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `VAL1_ATT2_INDEX` (`VALUE`,`ATTRIBUTE`),
  KEY `VAL1_OWN2_INDEX` (`VALUE`,`OWNER_ID`),
  KEY `OWN1_ATT2_INDEX` (`OWNER_ID`,`ATTRIBUTE`),
  KEY `OWN1_VAL2_INDEX` (`OWNER_ID`,`VALUE`),
  KEY `ATT1_VAL1_INDEX` (`ATTRIBUTE`,`VALUE`),
  KEY `JDOSTRINGANNOTATION_N49` (`OWNER_ID`),
  KEY `ATT1_OWN2_INDEX` (`ATTRIBUTE`,`OWNER_ID`),
  CONSTRAINT `STRING_ANNON_OWNER_FK` FOREIGN KEY (`OWNER_ID`) REFERENCES `JDONODE` (`ID`) ON DELETE CASCADE
)