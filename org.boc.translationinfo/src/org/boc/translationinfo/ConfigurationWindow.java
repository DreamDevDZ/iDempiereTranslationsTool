package org.boc.translationinfo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WStringEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.event.WTableModelEvent;
import org.adempiere.webui.event.WTableModelListener;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.minigrid.ColumnInfo;
import org.compiere.minigrid.IDColumn;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.North;
import org.zkoss.zul.South;
import org.zkoss.zul.West;
import org.zkoss.zul.East;
import org.zkoss.zul.Listitem;

public class ConfigurationWindow extends Window implements EventListener<Event>, WTableModelListener 
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3270839781945849310L;

	/**
	 * 	Constructor.
	 * 	Called from InfoTranslation, confirmPanel button
	 *	@param parent
	 */
	public ConfigurationWindow(Window parent)
	{
		super();
		setTitle(Msg.getMsg(Env.getCtx(), "Select Tables and Columns"));
		ZKUpdateUtil.setWidth(this, "500px");
		ZKUpdateUtil.setHeight(this, "600px");
		setSclass("popup-dialog");
		
		try	{
			jbInit();
			dynInit();
		}
		catch (Exception e)	{
			log.log(Level.SEVERE, "ConfigurationWindow", e);
		}
		
		AEnv.showCenterWindow(parent, this);
	}	//	ConfigurationWindow

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(ConfigurationWindow.class);

	private ConfirmPanel confirmPanel = new ConfirmPanel(true);
	
	private WListbox listboxTable = new WListbox();
	private WListbox listboxColumn = new WListbox();
	
	private String table_sql;
	private String column_sql;	
	private WStringEditor fieldTable = new WStringEditor();//textbox
	private WStringEditor fieldColumn = new WStringEditor();

	/**
	 * 	Static Init
	 *	@throws Exception
	 */
	private void jbInit() throws Exception
	{
		Borderlayout mainLayout = new Borderlayout();
		ZKUpdateUtil.setHflex(mainLayout, "1");
		ZKUpdateUtil.setVflex(mainLayout, "1");
        this.appendChild(mainLayout);
		
        North north = new North();
        mainLayout.appendChild(north);
        
        Grid parameterGrid = GridFactory.newGridLayout();
        parameterGrid.setStyle("width: 95%; margin: auto !important;");
        
        Columns columns = new Columns();
		parameterGrid.appendChild(columns);
		Column col1 = new Column();
		Column col2 = new Column();
		columns.appendChild(col1);
		columns.appendChild(col2);
		
		Rows rows = new Rows();
		parameterGrid.appendChild(rows);
		Row row = new Row();
		rows.appendChild(row);
		
		col1.appendChild(fieldTable.getComponent());
		ZKUpdateUtil.setHflex((Textbox) (fieldTable.getComponent()), "1");
		
		col2.appendChild(fieldColumn.getComponent());
		ZKUpdateUtil.setHflex((Textbox) (fieldColumn.getComponent()), "1");
		
		north.appendChild(parameterGrid);
		//
        West west = new West();
        west.setSclass("dialog-content");
        west.setAutoscroll(false);
        ZKUpdateUtil.setWidth(west, "50%");
        mainLayout.appendChild(west);
        
        listboxTable.autoSize();
		west.appendChild(listboxTable);
		ZKUpdateUtil.setVflex(listboxTable, "1");
		ZKUpdateUtil.setHflex(listboxTable, "1");
		//
		East east = new East();
        east.setSclass("dialog-content");
        east.setAutoscroll(false);
        ZKUpdateUtil.setWidth(east, "50%");
        mainLayout.appendChild(east);
        
        listboxColumn.autoSize();
        east.appendChild(listboxColumn);
		ZKUpdateUtil.setVflex(listboxColumn, "1");
		ZKUpdateUtil.setHflex(listboxColumn, "1");
		//
		South south = new South();
		south.setSclass("dialog-footer");
		mainLayout.appendChild(south);
		south.appendChild(confirmPanel);
		
		//	ConfirmPanel
		confirmPanel.addActionListener(this);	
		
	}	//	jbInit
	
	private static ColumnInfo[] table_layout = new ColumnInfo[] 
	{
		new ColumnInfo(" ", "DISTINCT 0", IDColumn.class),
		new ColumnInfo(Msg.translate(Env.getCtx(), "tablename"), "tablename", String.class), 
		new ColumnInfo("in", "tableInTranslationIW", String.class),				
	};
	/**	From Clause							*/
	private static String table_sqlFrom = CreateView.table_column+" a";
	/** Where Clause						*/
	private static String table_sqlWhere = "UPPER(tablename) LIKE ?";
	//
	private static ColumnInfo[] column_layout = new ColumnInfo[] 
	{
		new ColumnInfo(" ", "DISTINCT 0", IDColumn.class),
		new ColumnInfo(Msg.translate(Env.getCtx(), "columnname"), "columnname", String.class),
		new ColumnInfo("in", "columnInTranslationIW", String.class)
	};
	/**	From Clause							*/
	private static String column_sqlFrom = CreateView.table_column+" a";
	/** Where Clause						*/
	private static String column_sqlWhere = "UPPER(columnname) LIKE ?";

	/**
	 * 	Dynamic Init of the Center Panel
	 */
	private void dynInit()
	{	
		fieldTable.addValueChangeListener(new ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent evt) {
				try {
					refreshTable((String)fieldTable.getValue());
				} catch (Exception e) {
					log.log(Level.SEVERE, fieldTable.getGridField().getColumnName(), e);
				}			
			}
        });
		
		fieldColumn.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent evt) {
				try {
					refreshColumn((String)fieldColumn.getValue());
				} catch (Exception e) {
					log.log(Level.SEVERE, fieldColumn.getGridField().getColumnName(), e);
				}
			}
		});
		
		table_sql = listboxTable.prepareTable (table_layout, table_sqlFrom, 
					table_sqlWhere, true, "a")
				+ " ORDER BY tablename";	
		
		listboxTable.getModel().addTableModelListener(this);
		
		column_sql = listboxColumn.prepareTable (column_layout, column_sqlFrom, 
				column_sqlWhere, true, "a")
			+ " ORDER BY columnname";	
		
		listboxColumn.getModel().addTableModelListener(this);

		refreshTable("");
		refreshColumn("");
		
		//
		Set<Listitem> list = new HashSet<>();
		for(int row=0; row < listboxTable.getRowCount(); ++row) {
		if(listboxTable.getValueAt(row, 2).equals("Y")) {
			list.add(listboxTable.getItemAtIndex(row));
		}
		}
		listboxTable.setSelectedItems(list);
		
		Set<Listitem> list2 = new HashSet<>();
		for(int row=0; row < listboxColumn.getRowCount(); ++row) {
		if(listboxColumn.getValueAt(row, 2).equals("Y")) {
			list2.add(listboxColumn.getItemAtIndex(row));
		}
		}
		listboxColumn.setSelectedItems(list2);
		
	}	//	dynInit

	/**
	 * 	Refresh Query
	 */
	private void refreshTable(String table)
	{
		String sql = table_sql;
		//
		log.finest(sql);
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			
			StringBuilder value = new StringBuilder(table.toUpperCase());
            if (!value.toString().endsWith("%"))
                value.append("%");
			pstmt.setString(1, value.toString());
			
			rs = pstmt.executeQuery();
			listboxTable.loadTable(rs);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally {
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
	}	//	refresh
	
	private void refreshColumn(String column) {
		String sql = column_sql;
		//
		log.finest(sql);
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			
			StringBuilder value = new StringBuilder(column.toUpperCase());
            if (!value.toString().endsWith("%"))
                value.append("%");
			pstmt.setString(1, value.toString());
			
			rs = pstmt.executeQuery();
			listboxColumn.loadTable(rs);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally {
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
	}
	
	/**
	 * 	Action Listener
	 *	@param e event 
	 */
	public void onEvent(Event e) throws Exception 
	{
		if (e.getTarget().getId().equals(ConfirmPanel.A_OK))
		{			
			getListTable();
			getListColumn();
			
			if(tableChanged && tables != null && columns != null) {
				updateTables();
				CreateView.createTrlView();
			}
			
			dispose();
		}
		else if (e.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			dispose();
		}
	}
	
	private void updateTables() { 
		
		Statement stmt = null;
		String sql1 = "update ad_table "
				+ "set intranslationiw = 'Y' "
				+ "where tablename in ('"+tables.toString()+"')";
		String sql2 = "update ad_table "
				+ "set intranslationiw = 'N' "
				+ "where tablename not in ('"+tables.toString()+"')";
		String sql3 = "update ad_column "
				+ "set intranslationiw = 'Y' "
				+ "where columnname in ('"+columns.toString()+"')";
		String sql4 = "update ad_column "
				+ "set intranslationiw = 'N' "
				+ "where columnname not in ('"+columns.toString()+"')";
		
		try
		{
			stmt = DB.createStatement();
			stmt.addBatch(sql1);
			stmt.addBatch(sql2);
			stmt.addBatch(sql3);
			stmt.addBatch(sql4);
			stmt.executeBatch();
			//int[] result = stmt.executeBatch();
			
		} 
		catch (SQLException ex)
		{
			log.log(Level.SEVERE, stmt.toString(), ex);
		}
		finally 
		{
			DB.close(stmt);
			stmt = null;	
		}		
	}
	
	private boolean tableChanged = false;
	
	/**
	 * 	Table selection changed
	 *	@param e event
	 */
	public void tableChanged(WTableModelEvent e) 
	{
		enableButtons();
		tableChanged = true;
	}

	/**
	 * 	Enable Buttons
	 */
	private void enableButtons()
	{
		confirmPanel.getOKButton().setEnabled(true);
	}	//	enableButtons

	
	private StringBuilder tables = null;
	private StringBuilder columns = null;
	
	private void getListTable() {
		tables = new StringBuilder();
		
		int i = 0;
	 	for(Listitem item:listboxTable.getSelectedItems()) { 
	 		int row = item.getIndex();
	 		if(i > 0)
	 			tables.append("', '");
	 		tables.append(listboxTable.getValueAt(row, 1));
	 		++i;
	 	}			
	}
	
	private void getListColumn() {
		columns = new StringBuilder();
		
		int i = 0;
	 	for(Listitem item:listboxColumn.getSelectedItems()) {
	 		int row = item.getIndex();
	 		if(i > 0)
	 			columns.append("', '");
	 		columns.append(listboxColumn.getValueAt(row, 1));
	 		++i;
	 	}		
	}
	
}
