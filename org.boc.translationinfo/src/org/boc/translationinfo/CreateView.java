package org.boc.translationinfo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.DB;

public class CreateView {
	
	private static CLogger log = CLogger.getCLogger(CreateView.class);

	public final static String table_column = "boc_translation_txc_v";
	public final static String trl_view = "boc_translation_v";
	
	public String getTableColumnView() {
		return table_column;
	}
	
	public String getTranslationView() {
		return trl_view;
	}
	
	/**
	 * Creates/replaces view in database
	 */
	public static void createTCView() // rather alter ad_table and ad_colum and set infotranslation (menu, dashboard, view) access for system only
	{	
		log.log(Level.INFO, "Creating view: "+table_column);
		
		//String sql1 = "DROP VIEW adempiere.boc_translation_txc_v";
		String sql2 = "CREATE OR REPLACE VIEW "+ table_column +" AS "
				+ "SELECT t.ad_table_id, t.tablename, t.inTranslationIW AS tableInTranslationIW, "
				+ "c.ad_column_id, c.columnname, c.inTranslationIW AS columnInTranslationIW, t.isActive, t.ad_client_id, t.ad_org_id "				
				+ "FROM INFORMATION_SCHEMA.COLUMNS "
				+ "JOIN ad_table t ON (table_name = lower(t.tablename)) "
				+ "JOIN ad_column c ON (column_name = lower(c.columnname) AND t.ad_table_id = c.ad_table_id) "
				+ "WHERE table_schema = 'adempiere' "
				+ "AND table_name LIKE '%_trl' "
				+ "AND data_type = 'character varying' "
				+ "AND column_name NOT LIKE '%_uu' "
				+ "AND column_name NOT LIKE 'ad_language' "
				+ "AND t.isActive = 'Y' "
				+ "AND c.isActive = 'Y'"
				+ "ORDER BY tablename, columnname";
		
		/*String sql = "INSERT INTO BOC_table_column (ad_table_id, tablename, tableintranslationiw, ad_column_id, columnname, columnintranslationiw) "
		+ "SELECT t.ad_table_id, t.tablename, t.inTranslationIW AS tableInTranslationIW, "
		+ "c.ad_column_id, c.columnname, c.inTranslationIW AS columnInTranslationIW, c.isActive, c.ad_client_id, c.ad_org_id "				
		+ "FROM INFORMATION_SCHEMA.COLUMNS "
		+ "JOIN ad_table t ON (table_name = lower(t.tablename)) "
		+ "JOIN ad_column c ON (column_name = lower(c.columnname) AND t.ad_table_id = c.ad_table_id) "
		+ "WHERE table_schema = 'adempiere' "
		+ "AND table_name LIKE '%_trl' "
		+ "AND data_type = 'character varying' "
		+ "AND column_name NOT LIKE '%_uu' "
		+ "AND column_name NOT LIKE 'ad_language' "
		+ "AND t.isActive = 'Y' "
		+ "AND c.isActive = 'Y'"
		+ "ORDER BY tablename, columnname";*/
		
		PreparedStatement stmt = null;
		
		try
		{
			stmt = DB.prepareStatement(sql2, null);
			//stmt.addBatch(sql1);
			//stmt.addBatch(sql2);
			stmt.execute();
			//int[] result = stmt.executeBatch();
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, stmt.toString(), e);
		}
		finally 
		{
			DB.close(stmt);
			stmt = null;	
		}
	}
	
	/**
	 * Creates/replaces view in database
	 */
	public static void createTrlView ()
	{	
		log.log(Level.INFO, "Creating view: "+trl_view);
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		StringBuilder sb = new StringBuilder("CREATE OR REPLACE VIEW "+ trl_view +" AS ");
		String sql = "select * from "+table_column+" where tableintranslationiw = 'Y' and columnintranslationiw = 'Y'"; 
		
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			rs = pstmt.executeQuery();
			
			String table_name;
			String column_name;
			Integer table_id;
			Integer column_id;
		
			int count = 0;
			while(rs.next()) 
			{	
				count++;
				table_name = rs.getString("tablename");
				column_name = rs.getString("columnname");
				table_id = rs.getInt("ad_table_id");
				column_id = rs.getInt("ad_column_id");
				
				sb.append("SELECT 0 AS boc_translation_v_id, o."+table_name.substring(0, table_name.length()-3)+"id AS term_id, o."+column_name+"::text AS original, t."+column_name+"::text AS translation, '"+column_name+"'::text AS columnName, "
						+ "'"+table_name+"'::text AS tableName, t.ad_language AS ad_language, t.isTranslated, t.isActive, t.ad_client_id, t.ad_org_id, "+table_id+" AS ad_table_id, "+column_id+" AS ad_column_id, ad_language_id "
						+ "FROM "+table_name.substring(0, table_name.length()-4)+" o INNER JOIN "+table_name+" t USING ("+table_name.substring(0, table_name.length()-3)+"id) LEFT JOIN ad_language l ON (t.ad_language = l.ad_language) "
						+ "WHERE coalesce(trim(from regexp_replace(o."+column_name+", '[\n\r\t]+', '' )),'') <> '' "
						+ "UNION ");
			}
			
			if(count == 0) {
				log.log(Level.WARNING, "No result for query "+sb.toString());
				return;
			}				
				
			sb.delete(sb.lastIndexOf("UNION"), sb.length());
			sb.append("ORDER BY original, translation");
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally 
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;			
		}
		
		try
		{
			pstmt = DB.prepareStatement(sb.toString(), null);
			pstmt.execute();
		} 
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sb.toString(), e);
		}
		finally 
		{
			DB.close(pstmt);
			pstmt = null;	
		}
		
	}
	
}
