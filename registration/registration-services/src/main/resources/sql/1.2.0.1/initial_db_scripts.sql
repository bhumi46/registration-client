----- create new tables

CREATE TABLE "REG"."LOC_HIERARCHY_LIST"("HIERARCHY_LEVEL" INTEGER NOT NULL, "HIERARCHY_LEVEL_NAME" VARCHAR(36) NOT NULL, "LANG_CODE" VARCHAR(3) NOT NULL, "IS_ACTIVE" BOOLEAN NOT NULL, "CR_BY" VARCHAR(256) NOT NULL, "CR_DTIMES" TIMESTAMP NOT NULL, "UPD_BY" VARCHAR(256), "UPD_DTIMES" TIMESTAMP);
ALTER TABLE "REG"."LOC_HIERARCHY_LIST" ADD CONSTRAINT "PK_LOCHL_ID" PRIMARY KEY ("HIERARCHY_LEVEL", "HIERARCHY_LEVEL_NAME", "LANG_CODE" );

CREATE TABLE "REG"."PERMITTED_LOCAL_CONFIG" ("CODE" VARCHAR(128) NOT NULL, "NAME" VARCHAR(128) NOT NULL, "CONFIG_TYPE" VARCHAR(128) NOT NULL, "IS_ACTIVE" BOOLEAN NOT NULL, "CR_BY" VARCHAR(32) NOT NULL, "CR_DTIMES" TIMESTAMP NOT NULL, "UPD_BY" VARCHAR(32), "UPD_DTIMES" TIMESTAMP, "IS_DELETED" BOOLEAN, "DEL_DTIMES" TIMESTAMP);
ALTER TABLE "REG"."PERMITTED_LOCAL_CONFIG" ADD CONSTRAINT "PK_PERMCONFIG_CODE" PRIMARY KEY ("CODE");

CREATE TABLE "REG"."LOCAL_PREFERENCES" ("ID" VARCHAR(36) NOT NULL, "NAME" VARCHAR(128) NOT NULL, "VAL" VARCHAR(512), "CONFIG_TYPE" VARCHAR(128) NOT NULL, "MACHINE_NAME" VARCHAR(128) NOT NULL,"CR_BY" VARCHAR(32) NOT NULL, "CR_DTIMES" TIMESTAMP NOT NULL, "UPD_BY" VARCHAR(32), "UPD_DTIMES" TIMESTAMP, "IS_DELETED" BOOLEAN, "DEL_DTIMES" TIMESTAMP);
ALTER TABLE "REG"."LOCAL_PREFERENCES" ADD CONSTRAINT "PK_LOCPREF_ID" PRIMARY KEY ("ID");

CREATE TABLE "REG"."PROCESS_SPEC" ("ID" VARCHAR(100) NOT NULL, "TYPE" VARCHAR(100) NOT NULL, "ID_VERSION" VARCHAR(8), "ORDER_NUM" INTEGER NOT NULL, "IS_SUB_PROCESS" BOOLEAN  NOT NULL, "FLOW" VARCHAR(36) NOT NULL, "IS_ACTIVE" BOOLEAN NOT NULL);
ALTER TABLE "REG"."PROCESS_SPEC" ADD CONSTRAINT "PK_PROCSPEC_TYPE" PRIMARY KEY ("TYPE");
ALTER TABLE "REG"."PROCESS_SPEC" ADD CONSTRAINT "UK_PROCSPEC_ID" UNIQUE ("ID");

CREATE TABLE "REG"."FILE_SIGNATURE" ("FILE_NAME" VARCHAR(100) NOT NULL, "SIGNATURE" VARCHAR(2048) NOT NULL, "CONTENT_LENGTH" BIGINT, "ENCRYPTED" BOOLEAN  NOT NULL);

----- drop unwanted tables & constraint

DROP TABLE "REG"."AUDIT_LOG_CONTROL";
DROP TABLE "REG"."DEVICE_TYPE";
DROP TABLE "REG"."REG_CENTER_MACHINE_DEVICE";
DROP TABLE "REG"."MOSIP_DEVICE_SERVICE";
DROP TABLE "REG"."REG_CENTER_MACHINE";
DROP TABLE "REG"."GENDER";
DROP TABLE "REG"."FOUNDATIONAL_TRUST_PROVIDER";
DROP TABLE "REG"."INDIVIDUAL_TYPE";
DROP TABLE "REG"."DEVICE_SPEC";
DROP TABLE "REG"."DEVICE_MASTER";
DROP TABLE "REG"."REG_DEVICE_SUB_TYPE";
DROP TABLE "REG"."REG_CENTER_DEVICE";
DROP TABLE "REG"."TITLE";
DROP TABLE "REG"."APP_DETAIL";
DROP TABLE "REG"."REG_DEVICE_TYPE";
DROP TABLE "REG"."DEVICE_PROVIDER";
DROP TABLE "REG"."TEMPLATE_TYPE";
DROP TABLE "REG"."TEMPLATE_FILE_FORMAT";
DROP TABLE "REG"."REG_CENTER_USER";
DROP TABLE "REG"."VALID_DOCUMENT";

----- rename table

RENAME TABLE "REG"."BLACKLISTED_WORDS" TO "BLOCKLISTED_WORDS";

----- modify columns type & constraint

ALTER TABLE "REG"."MACHINE_SPEC" DROP CONSTRAINT "PK_MSPEC_CODE";
ALTER TABLE "REG"."MACHINE_SPEC" ADD CONSTRAINT "PK_MSPEC_CODE" PRIMARY KEY ("ID");

ALTER TABLE "REG"."MACHINE_TYPE" DROP CONSTRAINT "PK_MTYP_CODE";
ALTER TABLE "REG"."MACHINE_TYPE" ADD CONSTRAINT "PK_MTYP_CODE" PRIMARY KEY ("CODE");

ALTER TABLE "REG"."MACHINE_MASTER" DROP CONSTRAINT "PK_MACHM_ID";
ALTER TABLE "REG"."MACHINE_MASTER" ADD CONSTRAINT "PK_MACHM_ID" PRIMARY KEY ("ID");

ALTER TABLE "REG"."MACHINE_SPEC" ALTER COLUMN "LANG_CODE" NULL;
ALTER TABLE "REG"."USER_DETAIL" ALTER COLUMN "STATUS_CODE" NULL;
ALTER TABLE "REG"."MACHINE_MASTER" ALTER COLUMN "LANG_CODE" NULL;
ALTER TABLE "REG"."MACHINE_MASTER" ALTER COLUMN "MAC_ADDRESS" NULL;
ALTER TABLE "REG"."MACHINE_MASTER" ALTER COLUMN "SERIAL_NUM" NULL;
ALTER TABLE "REG"."MACHINE_MASTER" ALTER COLUMN "PUBLIC_KEY" NOT NULL;
ALTER TABLE "REG"."MACHINE_MASTER" ALTER COLUMN "KEY_INDEX" NOT NULL;
ALTER TABLE "REG"."MACHINE_TYPE" ALTER COLUMN "LANG_CODE" NULL;
ALTER TABLE "REG"."USER_DETAIL" ADD COLUMN "REG_CNTR_ID" VARCHAR(10);
UPDATE "REG"."USER_DETAIL" SET REG_CNTR_ID=(SELECT DISTINCT ID FROM "REG"."REGISTRATION_CENTER");
ALTER TABLE "REG"."USER_DETAIL" ALTER COLUMN "REG_CNTR_ID" NOT NULL;

ALTER TABLE "REG"."MACHINE_MASTER" ADD COLUMN "REG_CNTR_ID" VARCHAR(10);
UPDATE "REG"."MACHINE_MASTER" SET REG_CNTR_ID=(SELECT DISTINCT ID FROM "REG"."REGISTRATION_CENTER");
ALTER TABLE "REG"."MACHINE_MASTER" ALTER COLUMN "REG_CNTR_ID" NOT NULL;

ALTER TABLE "AUDIT"."APP_AUDIT_LOG" ALTER CR_BY SET DATA TYPE VARCHAR(256);
ALTER TABLE "REG"."USER_BIOMETRIC" ADD COLUMN QUALITY_SCORE_2 INTEGER;
UPDATE "REG"."USER_BIOMETRIC" SET QUALITY_SCORE_2=QUALITY_SCORE;
ALTER TABLE "REG"."USER_BIOMETRIC" DROP COLUMN QUALITY_SCORE;
RENAME COLUMN "REG"."USER_BIOMETRIC".QUALITY_SCORE_2 TO QUALITY_SCORE;

----- Changes in key-manager related tables

ALTER TABLE "REG"."CA_CERT_STORE" ADD CONSTRAINT "UK_CERT_THUMBPRINT" UNIQUE ("CERT_THUMBPRINT", "PARTNER_DOMAIN");
DELETE FROM "REG"."KEY_ALIAS";
ALTER TABLE "REG"."KEY_ALIAS" ADD COLUMN "UNI_IDENT" VARCHAR(50) DEFAULT '';
ALTER TABLE "REG"."KEY_ALIAS" ADD CONSTRAINT "UK_UNI_IDENT" UNIQUE ("UNI_IDENT");
ALTER TABLE "REG"."KEY_POLICY_DEF" ADD COLUMN "PRE_EXPIRE_DAYS" SMALLINT;
ALTER TABLE "REG"."KEY_POLICY_DEF" ADD COLUMN "ACCESS_ALLOWED" VARCHAR(1024);

UPDATE "REG"."KEY_POLICY_DEF" SET PRE_EXPIRE_DAYS=50, ACCESS_ALLOWED='NA';
INSERT INTO "REG"."KEY_POLICY_DEF" ("APP_ID", "KEY_VALIDITY_DURATION","PRE_EXPIRE_DAYS", "ACCESS_ALLOWED", "IS_ACTIVE", "CR_BY", "CR_DTIMES") VALUES('BASE', 730, 20, 'NA', true, 'mosipadmin', current timestamp);

----- Changes in registration and its related tables

ALTER TABLE "REG"."REGISTRATION" DROP CONSTRAINT "PK_REG_ID";
ALTER TABLE "REG"."REGISTRATION" ADD COLUMN "APP_ID" VARCHAR(128) NOT NULL default '';
ALTER TABLE "REG"."REGISTRATION" ADD COLUMN "PACKET_ID" VARCHAR(256) NOT NULL default '';
ALTER TABLE "REG"."REGISTRATION" ADD COLUMN "ADDITIONAL_INFO_REQ_ID" VARCHAR(128);
ALTER TABLE "REG"."REGISTRATION" ADD COLUMN "ACK_SIGNATURE" VARCHAR(350);
ALTER TABLE "REG"."REGISTRATION" ADD COLUMN "HAS_BWORDS" BOOLEAN;
UPDATE "REG"."REGISTRATION" SET PACKET_ID=ID;
ALTER TABLE "REG"."REGISTRATION" ADD CONSTRAINT "PK_REG_ID" PRIMARY KEY ("PACKET_ID");
ALTER TABLE "REG"."REGISTRATION" ALTER SERVER_STATUS_CODE SET DATA TYPE VARCHAR(256);

ALTER TABLE "REG"."REGISTRATION_TRANSACTION" ADD COLUMN "APP_ID" VARCHAR(39) NOT NULL default '';

----- insert / update statements

UPDATE "REG"."GLOBAL_PARAM" set VAL='DEVICE' WHERE CODE='mosip.registration.mdm.trust.domain.digitalId';
INSERT INTO "REG"."GLOBAL_PARAM" VALUES ('mosip.kernel.vid.restricted-numbers','mosip.kernel.vid.restricted-numbers','786,666','CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);
INSERT INTO "REG"."GLOBAL_PARAM" VALUES ('mosip.kernel.vid.not-start-with','mosip.kernel.vid.not-start-with','0,1','CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);
INSERT INTO "REG"."GLOBAL_PARAM" VALUES ('mosip.kernel.vid.length.repeating-limit','mosip.kernel.vid.length.repeating-limit','2','CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);
INSERT INTO "REG"."GLOBAL_PARAM" VALUES ('mosip.kernel.vid.length.repeating-block-limit','mosip.kernel.vid.length.repeating-block-limit','2','CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);
INSERT INTO "REG"."GLOBAL_PARAM" VALUES ('mosip.kernel.vid.length.sequence-limit','mosip.kernel.vid.length.sequence-limit','3','CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);
INSERT INTO "REG"."GLOBAL_PARAM" VALUES ('mosip.kernel.vid.length','mosip.kernel.vid.length','16','CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);
INSERT INTO "REG"."GLOBAL_PARAM" VALUES ('mosip.registration.audit_timestamp','mosip.registration.audit_timestamp',current timestamp,'CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);
