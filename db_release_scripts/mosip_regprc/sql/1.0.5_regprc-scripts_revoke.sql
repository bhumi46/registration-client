-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_regprc
-- Release Version 	: 1.0.5
-- Purpose    		: Revoking Database Alter deployement done for release in Registration ProcessorDB.       
-- Create By   		: Sadanandegowda DM
-- Created Date		: 03-Jan-2020
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------

\c mosip_regprc sysadmin

ALTER TABLE regprc.individual_demographic_dedup DROP COLUMN mobile_number;

ALTER TABLE regprc.individual_demographic_dedup DROP COLUMN email;

ALTER TABLE regprc.individual_demographic_dedup DROP COLUMN pincode;

----------------------------------------------------------------------------------------------------