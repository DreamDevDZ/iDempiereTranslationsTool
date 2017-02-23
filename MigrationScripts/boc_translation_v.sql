-- View: adempiere.boc_translation_v

-- DROP VIEW adempiere.boc_translation_v;

CREATE OR REPLACE VIEW adempiere.boc_translation_v AS 
 SELECT 0 AS boc_translation_v_id,
    o.ad_desktop_id AS term_id,
    o.description::text AS original,
    t.description::text AS translation,
    'Description'::text AS columnname,
    'AD_Desktop_Trl'::text AS tablename,
    t.ad_language,
    t.istranslated,
    t.isactive,
    t.ad_client_id,
    t.ad_org_id,
    460 AS ad_table_id,
    6280 AS ad_column_id,
    l.ad_language_id
   FROM adempiere.ad_desktop o
     JOIN adempiere.ad_desktop_trl t USING (ad_desktop_id)
     LEFT JOIN adempiere.ad_language l ON t.ad_language::text = l.ad_language::text
UNION
 SELECT 0 AS boc_translation_v_id,
    o.ad_desktop_id AS term_id,
    o.help::text AS original,
    t.help::text AS translation,
    'Help'::text AS columnname,
    'AD_Desktop_Trl'::text AS tablename,
    t.ad_language,
    t.istranslated,
    t.isactive,
    t.ad_client_id,
    t.ad_org_id,
    460 AS ad_table_id,
    6281 AS ad_column_id,
    l.ad_language_id
   FROM adempiere.ad_desktop o
     JOIN adempiere.ad_desktop_trl t USING (ad_desktop_id)
     LEFT JOIN adempiere.ad_language l ON t.ad_language::text = l.ad_language::text
UNION
 SELECT 0 AS boc_translation_v_id,
    o.ad_desktop_id AS term_id,
    o.name::text AS original,
    t.name::text AS translation,
    'Name'::text AS columnname,
    'AD_Desktop_Trl'::text AS tablename,
    t.ad_language,
    t.istranslated,
    t.isactive,
    t.ad_client_id,
    t.ad_org_id,
    460 AS ad_table_id,
    6279 AS ad_column_id,
    l.ad_language_id
   FROM adempiere.ad_desktop o
     JOIN adempiere.ad_desktop_trl t USING (ad_desktop_id)
     LEFT JOIN adempiere.ad_language l ON t.ad_language::text = l.ad_language::text
  ORDER BY 3, 4;

ALTER TABLE adempiere.boc_translation_v
  OWNER TO adempiere;
