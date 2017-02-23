-- View: adempiere.boc_translation_txc_v

-- 
-- DROP VIEW adempiere.boc_translation_txc_v;

--
CREATE OR REPLACE VIEW adempiere.boc_translation_txc_v AS 
 SELECT t.ad_table_id,
    t.tablename,
    t.intranslationiw AS tableintranslationiw,
    c.ad_column_id,
    c.columnname,
    c.intranslationiw AS columnintranslationiw,
    t.isactive,
    t.ad_client_id,
    t.ad_org_id
   
FROM information_schema.columns
     
JOIN adempiere.ad_table t ON columns.table_name::text = lower(t.tablename::text)
     
JOIN adempiere.ad_column c ON columns.column_name::text = lower(c.columnname::text) AND t.ad_table_id = c.ad_table_id
  WHERE columns.table_schema::text = 'adempiere'::text 
AND columns.table_name::text ~~ '%_trl'::text 
AND columns.data_type::text = 'character varying'::text 
AND columns.column_name::text !~~ '%_uu'::text 
AND columns.column_name::text !~~ 'ad_language'::text 
AND t.isactive = 'Y'::bpchar 
AND c.isactive = 'Y'::bpchar
  
ORDER BY t.tablename, c.columnname;

ALTER TABLE adempiere.boc_translation_txc_v
  OWNER TO adempiere;
