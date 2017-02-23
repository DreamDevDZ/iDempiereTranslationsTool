-- Add new column which determines what tables/columns are used for Translation Info Window

-- Column: intranslationiw

-- 
ALTER TABLE adempiere.ad_table DROP COLUMN intranslationiw;


ALTER TABLE adempiere.ad_table ADD COLUMN intranslationiw character(1);

ALTER TABLE adempiere.ad_table ALTER COLUMN intranslationiw SET DEFAULT 'N'::bpchar;
ALTER TABLE adempiere.ad_table ALTER COLUMN intranslationiw SET NOT NULL;




-- Column: intranslationiw

-- 
ALTER TABLE adempiere.ad_column DROP COLUMN intranslationiw;


ALTER TABLE adempiere.ad_column ADD COLUMN intranslationiw character(1);
ALTER TABLE adempiere.ad_column ALTER COLUMN intranslationiw SET DEFAULT 'N'::bpchar;

ALTER TABLE adempiere.ad_column ALTER COLUMN intranslationiw SET NOT NULL;

