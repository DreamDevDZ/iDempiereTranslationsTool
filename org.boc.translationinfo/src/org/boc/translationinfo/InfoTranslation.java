package org.boc.translationinfo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;

import org.adempiere.model.MInfoRelated;
import org.adempiere.webui.AdempiereWebUI;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.EditorBox;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Tab;
import org.adempiere.webui.component.Tabbox;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.Tabpanels;
import org.adempiere.webui.component.Tabs;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.editor.WebEditorFactory;
import org.adempiere.webui.event.DialogEvents;
import org.adempiere.webui.info.IWhereClauseEditor;
import org.adempiere.webui.info.InfoWindow;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.minigrid.ColumnInfo;
import org.compiere.minigrid.EmbedWinInfo;
import org.compiere.minigrid.IDColumn;
import org.compiere.model.AccessSqlParser;
import org.compiere.model.GridField;
import org.compiere.model.GridFieldVO;
import org.compiere.model.MInfoColumn;
import org.compiere.model.MInfoWindow;
import org.compiere.model.MProcess;
import org.compiere.model.MQuery;
import org.compiere.model.MTable;
import org.compiere.model.AccessSqlParser.TableInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.compiere.util.ValueNamePair;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SwipeEvent;
import org.zkoss.zul.Center;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
//import org.boc.testui.ConfigurationWindow;

public class InfoTranslation extends InfoWindow{

	private static CLogger log = CLogger.getCLogger(InfoTranslation.class);
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6275528422926833555L;
	
	private final String mainInfoWindow = "Translation Info";
	private final String subInfoWindow = "Translation Info Related";
	// "Tranlsation Info Plus"
	
	private List<GridField> gridFields;
	private Checkbox checkAND;
	//
	private Checkbox checkTranslatedSub;
	private Checkbox checkTranslatedMain;
	
	@Override
	protected boolean loadInfoDefinition() {
		if (infoWindow != null) {
			String tableName = null;
				tableName = MTable.getTableName(Env.getCtx(), infoWindow.getAD_Table_ID());
			
			AccessSqlParser sqlParser = new AccessSqlParser("SELECT * FROM " + infoWindow.getFromClause());
			tableInfos = sqlParser.getTableInfo(0);
			if (tableInfos[0].getSynonym() != null && tableInfos[0].getSynonym().trim().length() > 0) {
				p_tableName = tableInfos[0].getSynonym().trim();
				if (p_whereClause != null && p_whereClause.trim().length() > 0) {
					p_whereClause = p_whereClause.replace(tableName+".", p_tableName+".");
				}					
			}
			
			infoColumns = infoWindow.getInfoColumns(tableInfos);
		
			gridFields = new ArrayList<GridField>();
			
			for(MInfoColumn infoColumn : infoColumns) {
				if (infoColumn.isKey())
					keyColumnOfView = infoColumn;
				String columnName = infoColumn.getColumnName();
				/*!m_lookup && infoColumn.isMandatory():apply Mandatory only case open as window and only for criteria field*/
				boolean isMandatory = !m_lookup && infoColumn.isMandatory() && infoColumn.isQueryCriteria();
				GridFieldVO vo = GridFieldVO.createParameter(infoContext, p_WindowNo, AEnv.getADWindowID(p_WindowNo), infoWindow.getAD_InfoWindow_ID(), 0,
						columnName, infoColumn.get_Translation("Name"), infoColumn.getAD_Reference_ID(), 
						infoColumn.getAD_Reference_Value_ID(), isMandatory, false);
				if (infoColumn.getAD_Val_Rule_ID() > 0) {
					vo.ValidationCode = infoColumn.getAD_Val_Rule().getCode();
					if (vo.lookupInfo != null) {
						vo.lookupInfo.ValidationCode = vo.ValidationCode;
						vo.lookupInfo.IsValidated = false;
					}
				}
				if (infoColumn.getDisplayLogic() != null)					
					vo.DisplayLogic =  infoColumn.getDisplayLogic();
				if (infoColumn.isQueryCriteria() && infoColumn.getDefaultValue() != null)
					vo.DefaultValue = infoColumn.getDefaultValue();
				String desc = infoColumn.get_Translation("Description");
				vo.Description = desc != null ? desc : "";
				String help = infoColumn.get_Translation("Help");
				vo.Help = help != null ? help : "";
				GridField gridField = new GridField(vo);
				gridFields.add(gridField);
				
				//
				if(infoColumn.getName().equalsIgnoreCase("not translated")
						&& infoColumn.getSelectClause().toLowerCase().contains("istranslated")
						&& infoColumn.getAD_Reference().getName().equalsIgnoreCase("yes-no"))
					createTranslatedMainCheckbox();
			}
			
			StringBuilder builder = new StringBuilder(p_whereClause != null ? p_whereClause.trim() : "");
			String infoWhereClause = infoWindow.getWhereClause();
			if (infoWhereClause != null && infoWhereClause.indexOf("@") >= 0) {
				infoWhereClause = Env.parseContext(Env.getCtx(), p_WindowNo, infoWhereClause, true, false);
				if (infoWhereClause.length() == 0)
					log.log(Level.SEVERE, "Cannot parse context= " + infoWindow.getWhereClause());
			}
			if (infoWhereClause != null && infoWhereClause.trim().length() > 0) {								
				if (builder.length() > 0) {
					builder.append(" AND ");
				}
				builder.append(infoWhereClause);
				p_whereClause = builder.toString();
			}
			
			return true;
		} else {
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.adempiere.webui.panel.InfoPanel#setParameters(java.sql.PreparedStatement, boolean)
	 */
	@Override
	protected void setParameters(PreparedStatement pstmt, boolean forCount)
			throws SQLException {
		
		// when query not by click requery button, reuse parameter value
		if (!isQueryByUser && prevParameterValues != null){
			for (int parameterIndex = 0; parameterIndex < prevParameterValues.size(); parameterIndex++){
				setParameter (pstmt, parameterIndex + 1, prevParameterValues.get(parameterIndex), prevQueryOperators.get(parameterIndex));
			}
			return;
		}
		
		// init collection to save current parameter value 
		if (prevParameterValues == null){
			prevParameterValues = new ArrayList<Object> ();
			prevQueryOperators = new ArrayList<String> ();
			prevRefParmeterEditor = new ArrayList<WEditor>(); 
		}else{
			prevParameterValues.clear();
			prevQueryOperators.clear();
			prevRefParmeterEditor.clear();
		}

		int parameterIndex = 0;
		for(WEditor editor : editors) {

			if (!editor.isVisible())
				continue;
			
			if (editor.getGridField() != null && editor.getValue() != null && editor.getValue().toString().trim().length() > 0) {
				MInfoColumn mInfoColumn = findInfoColumn(editor.getGridField());
				if (mInfoColumn == null || mInfoColumn.getSelectClause().equals("0")) {
					continue;
				}
				Object value = editor.getValue();
				parameterIndex++;
				prevParameterValues.add(value);
				prevQueryOperators.add(mInfoColumn.getQueryOperator());
				prevRefParmeterEditor.add(editor);
				setParameter (pstmt, parameterIndex, value, mInfoColumn.getQueryOperator());
			}
		}
		//
		if(checkTranslatedMain != null && checkTranslatedMain.isChecked()) {
			Object value = "N";
			parameterIndex++;
			prevParameterValues.add(value);
			prevQueryOperators.add("=");
			//prevRefParmeterEditor.add(editor);
			setParameter (pstmt, parameterIndex, value, "=");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.adempiere.webui.panel.InfoPanel#getSQLWhere()
	 */
	@Override
	protected String getSQLWhere() {
		/**
		 * when query not by click requery button, reuse prev where clause
		 * IDEMPIERE-1979  
		 */
		if (!isQueryByUser && prevWhereClause != null){
			return prevWhereClause;
		}
		
		StringBuilder builder = new StringBuilder();
		MTable table = MTable.get(Env.getCtx(), infoWindow.getAD_Table_ID());
		if (!hasIsActiveEditor1() && table.get_ColumnIndex("IsActive") >=0 ) {
			if (p_whereClause != null && p_whereClause.trim().length() > 0) {
				builder.append(" AND ");
			}
			builder.append(tableInfos[0].getSynonym()).append(".IsActive='Y'");
		}
		int count = 0;
		for(WEditor editor : editors) {
			if (!editor.isVisible())
				continue;
			
			if (editor instanceof IWhereClauseEditor) {
				String whereClause = ((IWhereClauseEditor) editor).getWhereClause();
				if (whereClause != null && whereClause.trim().length() > 0) {
					count++;
					if (count == 1) {
						if (builder.length() > 0) {
							builder.append(" AND ");
							if (!checkAND.isChecked()) builder.append(" ( ");
						} else if (p_whereClause != null && p_whereClause.trim().length() > 0) {
							builder.append(" AND ");
							if (!checkAND.isChecked()) builder.append(" ( ");
						}	
					} else {
						builder.append(checkAND.isChecked() ? " AND " : " OR ");
					}
					builder.append(whereClause);
				}
			} else if (editor.getGridField() != null && editor.getValue() != null && editor.getValue().toString().trim().length() > 0) {
				MInfoColumn mInfoColumn = findInfoColumn(editor.getGridField());
				if (mInfoColumn == null || mInfoColumn.getSelectClause().equals("0")) {
					continue;
				}
				String columnName = mInfoColumn.getSelectClause();
				int asIndex = columnName.toUpperCase().lastIndexOf(" AS ");
				if (asIndex > 0) {
					columnName = columnName.substring(0, asIndex);
				}
				
				count++;
				if (count == 1) {
					if (builder.length() > 0) {
						builder.append(" AND ");
						if (!checkAND.isChecked()) builder.append(" ( ");
					} else if (p_whereClause != null && p_whereClause.trim().length() > 0) {
						builder.append(" AND ");
						if (!checkAND.isChecked()) builder.append(" ( ");
					} else if (hasIsActiveEditor1() && !checkAND.isChecked()) {
						builder.append(" ( ");
					}
				} else {
					builder.append(checkAND.isChecked() ? " AND " : " OR ");
				}
				
				String columnClause = null;
				if (mInfoColumn.getQueryFunction() != null && mInfoColumn.getQueryFunction().trim().length() > 0) {
					String function = mInfoColumn.getQueryFunction();
					if (function.indexOf("@") >= 0) {
						String s = Env.parseContext(infoContext, p_WindowNo, function, true, false);
						if (s.length() == 0) {
							log.log(Level.SEVERE, "Failed to parse query function. " + function);
						} else {
							function = s;
						}
					}
					if (function.indexOf("?") >= 0) {
						columnClause = function.replaceFirst("[?]", columnName);
					} else {
						columnClause = function+"("+columnName+")";
					}
				} else {
					columnClause = columnName;
				}
				builder.append(columnClause)
					   .append(" ")
					   .append(mInfoColumn.getQueryOperator())
					   .append(" ?");				
			}
		}
		
		/*//
		count++;
		if (count == 1) {
			if (builder.length() > 0) {
				builder.append(" AND ");
				if (!checkAND.isChecked()) builder.append(" ( ");
			} else if (p_whereClause != null && p_whereClause.trim().length() > 0) {
				builder.append(" AND ");
				if (!checkAND.isChecked()) builder.append(" ( ");
			} else if (hasIsActiveEditor1() && !checkAND.isChecked()) {
				builder.append(" ( ");
			}
		} else {
			builder.append(checkAND.isChecked() ? " AND " : " OR ");
		}
		
		if(checkTranslatedMain != null && checkTranslatedMain.isChecked())
			builder.append(tableInfos[0].getSynonym()+".istranslated = ? ");
		//*/
		
		if (count > 0 && !checkAND.isChecked()) {
			builder.append(" ) ");
		}
		//
		if(checkTranslatedMain != null && checkTranslatedMain.isChecked())
			builder.append(" AND "+tableInfos[0].getSynonym()+".istranslated = ? ");
		//
		String sql = builder.toString();
		if (sql.indexOf("@") >= 0) {
			sql = Env.parseContext(infoContext, p_WindowNo, sql, true, true);
		}
		
		// IDEMPIERE-1979
		prevWhereClause = sql;
		
		return sql;
		
		/*StringBuilder builder = new StringBuilder(super.getSQLWhere());
		
		if(checkTranslatedMain != null && checkTranslatedMain.isChecked()) {
			if (builder.length() > 0)
				builder.append(" AND "+tableInfos[0].getSynonym()+".istranslated = ? ");
		
			builder.append(tableInfos[0].getSynonym()+".istranslated = ? ");
		}
		
		// IDEMPIERE-1979
		prevWhereClause = builder.toString();
		return prevWhereClause;*/
	}

	@Override
	protected MInfoColumn findInfoColumn(GridField gridField) {
		for(int i = 0; i < gridFields.size(); i++) {
			if (gridFields.get(i) == gridField) {
				return infoColumns[i];
			}
		}
		return null;
	}

	@Override
	protected void createParameterPanel() {
		parameterGrid = GridFactory.newGridLayout();
		parameterGrid.setWidgetAttribute(AdempiereWebUI.WIDGET_INSTANCE_NAME, "infoParameterPanel");
		parameterGrid.setStyle("width: 95%; margin: auto !important;");
		Columns columns = new Columns();
		parameterGrid.appendChild(columns);
		for(int i = 0; i < 6; i++)//
			columns.appendChild(new Column());
		
		Column column = new Column();
		ZKUpdateUtil.setWidth(column, "100px");
		column.setAlign("right");
		columns.appendChild(column);
		
		Rows rows = new Rows();
		parameterGrid.appendChild(rows);
		
		editors = new ArrayList<WEditor>();
		identifiers = new ArrayList<WEditor>();
		TreeMap<Integer, List<Object[]>> tree = new TreeMap<Integer, List<Object[]>>();
		for (int i = 0; i < infoColumns.length; i++)
		{
			if (infoColumns[i].isQueryCriteria()) {
				List<Object[]> list = tree.get(infoColumns[i].getSeqNoSelection());
				if (list == null) {
					list = new ArrayList<Object[]>();
					tree.put(infoColumns[i].getSeqNoSelection(), list);
				}
				list.add(new Object[]{infoColumns[i], gridFields.get(i)});				
			}
		}
		
		for (Integer i : tree.keySet()) {
			List<Object[]> list = tree.get(i);
			for(Object[] value : list) {
				addSelectionColumn((MInfoColumn)value[0], (GridField)value[1]);
			}
		}
		
		if (checkAND == null) {
			if (parameterGrid.getRows() != null && parameterGrid.getRows().getFirstChild() != null) {
				Row row = (Row) parameterGrid.getRows().getFirstChild();
				int col = row.getChildren().size();
				while (col < 6) {
					row.appendChild(new Space());
					col++;
				}
				createAndCheckbox();
				row.appendChild(checkAND);
			}
		}
		evalDisplayLogic();
		initParameters();
		dynamicDisplay(null);
		
		//
		init();
	}
	
	/**
     *  Add Selection Column to first Tab
	 * @param infoColumn 
     *  @param mField field
    **/
   /* protected void addSelectionColumn(MInfoColumn infoColumn, GridField mField)
    {
        int displayLength = mField.getDisplayLength();
        if (displayLength <= 0 || displayLength > FIELDLENGTH)
            mField.setDisplayLength(FIELDLENGTH);
        else
            displayLength = 0;

        //  Editor
        WEditor editor = null;
        if (mField.getDisplayType() == DisplayType.PAttribute) 
        {
        	editor = new WInfoPAttributeEditor(infoContext, p_WindowNo, mField);
	        editor.setReadWrite(true);
        }
        else 
        {
	        editor = WebEditorFactory.getEditor(mField, false);
	        editor.setReadWrite(true);
	        editor.dynamicDisplay();
	        editor.addValueChangeListener(this);
	        editor.fillHorizontal();
        }
        Label label = editor.getLabel();
        Component fieldEditor = editor.getComponent();

        //
        if (displayLength > 0)      //  set it back
            mField.setDisplayLength(displayLength);
        //
        if (label != null) {
        	if (infoColumn.getQueryOperator().equals(X_AD_InfoColumn.QUERYOPERATOR_Gt) ||
        		infoColumn.getQueryOperator().equals(X_AD_InfoColumn.QUERYOPERATOR_GtEq) ||
        		infoColumn.getQueryOperator().equals(X_AD_InfoColumn.QUERYOPERATOR_Le) ||
        		infoColumn.getQueryOperator().equals(X_AD_InfoColumn.QUERYOPERATOR_LeEq) ||
        		infoColumn.getQueryOperator().equals(X_AD_InfoColumn.QUERYOPERATOR_NotEq )) {
        		label.setValue(label.getValue() + " " + infoColumn.getQueryOperator());
        	}
        }

        addSearchParameter(label, fieldEditor);
        
        editors.add(editor);
        
        editor.showMenu();
        
        if (infoColumn.isIdentifier()) {
        	identifiers.add(editor);
        }

        fieldEditor.addEventListener(Events.ON_OK, this);		

        mField.addPropertyChangeListener(editor);
                
        mField.setValue(mField.getDefaultForPanel(), true);

    }   // addSelectionColumn*/

	@Override
	protected void addSearchParameter(Label label, Component fieldEditor) {
		Row panel = null;
        if (parameterGrid.getRows().getChildren().isEmpty())
        {
        	panel = new Row();
        	parameterGrid.getRows().appendChild(panel);
        }
        else
        {
        	panel = (Row) parameterGrid.getRows().getLastChild();
        	if (panel.getChildren().size() == 6)
        	{
        		if (parameterGrid.getRows().getChildren().size() == 1) 
        		{
        			createAndCheckbox();
					panel.appendChild(checkAND);
        		}
        		else
        		{
        			panel.appendChild(new Space());
        		}
        		panel = new Row();
            	parameterGrid.getRows().appendChild(panel); 
        	}
        }
        if (!(fieldEditor instanceof Checkbox))
        {
        	Div div = new Div();
        	div.setStyle("text-align: right;");
        	div.appendChild(label);
        	if (label.getDecorator() != null){
        		div.appendChild (label.getDecorator());
        	}
        	panel.appendChild(div);
        } else {
        	panel.appendChild(new Space());
        }
        
        // add out parent to add menu for this field, without outerDiv, a new cell will auto make for menu.
        Div outerParent = new Div();
        outerParent.appendChild(fieldEditor);
        panel.appendChild(outerParent);
	}

	@Override
	protected void createAndCheckbox() {
		checkAND = new Checkbox();
		checkAND.setLabel(Msg.getMsg(Env.getCtx(), "SearchAND", true));
		String tips = Msg.getMsg(Env.getCtx(), "SearchAND", false);
		if (!Util.isEmpty(tips)) 
		{
			checkAND.setTooltiptext(tips);
		}
		checkAND.setChecked(true);
		checkAND.addEventListener(Events.ON_CHECK, this);
	}
	
	protected void createTranslatedSubCheckbox() {
		checkTranslatedSub = new Checkbox();
		checkTranslatedSub.setLabel("NotTranslated");
		checkTranslatedSub.setChecked(false);
		checkTranslatedSub.addEventListener(Events.ON_CHECK, this);
		
		/*checkTranslatedSub.addEventListener(Events.ON_CHECK, new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (contentPanel.getLayout() != null) {
					updateSubcontent();
				}
			}
		});*/
	}
	
	protected void createTranslatedMainCheckbox() {
		checkTranslatedMain = new Checkbox();
		checkTranslatedMain.setLabel("NotTranslated");
		/*String tips = Msg.getMsg(Env.getCtx(), "NotTranslated", false);
		if (!Util.isEmpty(tips)) 
		{
			checkTranslatedMain.setTooltiptext(tips);
		}*/
		checkTranslatedMain.setChecked(true);
		checkTranslatedMain.addEventListener(Events.ON_CHECK, this);
	}
    
	@Override
	protected boolean hasZoom() {
		return !isLookup() && infoWindow != null && infoWindow.getName().equalsIgnoreCase(mainInfoWindow);// and subinfo
	}

	/** Return true if there is an 'IsActive' criteria */
	boolean hasIsActiveEditor1() {
		for (WEditor editor : editors) {
			if (editor.getGridField() != null && "IsActive".equals(editor.getGridField().getColumnName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void prepareTable() {		
		List<ColumnInfo> list = new ArrayList<ColumnInfo>();
		String keyTableAlias = tableInfos[0].getSynonym() != null && tableInfos[0].getSynonym().trim().length() > 0 
				? tableInfos[0].getSynonym()
				: tableInfos[0].getTableName();
					
		String keySelectClause = keyTableAlias+"."+p_keyColumn;
		list.add(new ColumnInfo(" ", keySelectClause, IDColumn.class));
		//
		int i = 0;
		for(MInfoColumn infoColumn : infoColumns) 
		{						
			if (infoColumn.isDisplayed(infoContext, p_WindowNo)) 
			{
				ColumnInfo columnInfo = null;
				String colSQL = infoColumn.getSelectClause();
	
				if (! colSQL.toUpperCase().contains(" AS "))
					colSQL += " AS " + infoColumn.getColumnName();
				if (infoColumn.getAD_Reference_ID() == DisplayType.ID) 
				{
					if (infoColumn.getSelectClause().equalsIgnoreCase(keySelectClause))
						continue;
					
					columnInfo = new ColumnInfo(infoColumn.get_Translation("Name"), colSQL, DisplayType.getClass(infoColumn.getAD_Reference_ID(), true));
				}
				else if (DisplayType.isLookup(infoColumn.getAD_Reference_ID()))
				{
					if (infoColumn.getAD_Reference_ID() == DisplayType.List)
					{
						WEditor editor = null;
						editor = WebEditorFactory.getEditor(gridFields.get(i), true);
				        editor.setMandatory(false);
				        editor.setReadWrite(false);
				        editorMap.put(colSQL, editor);
						columnInfo = new ColumnInfo(infoColumn.get_Translation("Name"), colSQL, ValueNamePair.class, (String)null);
					}
					else
					{
						columnInfo = createLookupColumnInfo(tableInfos, gridFields.get(i), infoColumn);
					}					
				}
				else  
				{
					columnInfo = new ColumnInfo(infoColumn.get_Translation("Name"), colSQL, DisplayType.getClass(infoColumn.getAD_Reference_ID(), true));
				}
				columnInfo.setColDescription(infoColumn.get_Translation("Description"));
				columnInfo.setGridField(gridFields.get(i));
				list.add(columnInfo);
				if (keyColumnOfView == infoColumn){
					if (columnInfo.getColClass().equals(IDColumn.class)) 
						isIDColumnKeyOfView = true;
					indexKeyOfView = list.size() - 1;
				}
			}		
			i++;
		}
		
		if (keyColumnOfView == null){
			isIDColumnKeyOfView = true;// because use main key
		}
		
		columnInfos = list.toArray(new ColumnInfo[0]);
		prepareTable(columnInfos, infoWindow.getFromClause(), p_whereClause, infoWindow.getOrderByClause());		
	}
		
	@Override
	protected void refresh(Object obj, EmbedWinInfo relatedInfo)
	{
		StringBuilder sql = new StringBuilder();
		sql.append(relatedInfo.getInfoSql()); // delete get sql method from MInfoWindow
		/////////////////////////////////////
		if(checkTranslatedSub != null && checkTranslatedSub.isChecked())
			sql.append(" AND "+"istranslated = ?");
		//
		if (log.isLoggable(Level.FINEST))
			log.finest(sql.toString());
		
		Object linkPara = null;
		if (obj != null && obj instanceof IDColumn){
			IDColumn ID = (IDColumn) obj;
			linkPara = ID.getRecord_ID();
		}else if (obj != null){
			linkPara = obj.toString();
		}else {
			//TODO:hard case
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql.toString(), null);
			//TODO: implicit type conversion. will exception in some case must recheck
			if (relatedInfo.getTypeDataOfLink().equals(String.class)){
				pstmt.setString(1, (String)linkPara);
			}else if (relatedInfo.getTypeDataOfLink().equals(int.class)){
				pstmt.setInt(1, Integer.parseInt(linkPara.toString()));				
			}else{
				pstmt.setObject(1, linkPara);
			}
			//
			if(checkTranslatedSub != null && checkTranslatedSub.isChecked())
				pstmt.setString(2, "N");
			
			rs = pstmt.executeQuery();
			loadEmbedded(rs, relatedInfo);
		}
		catch (Exception e)
		{
			log.log(Level.WARNING, sql.toString(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
	} // refresh
	
	/**
	 * Specific refresh for subinfo
	 * @param obj
	 * @param relatedInfo
	 * @param orig
	 * @param tran
	 */
	protected void refresh(Object obj, EmbedWinInfo relatedInfo, 
			Object orig, Object tran)
	{		
		StringBuilder sql = new StringBuilder();
		sql.append(relatedInfo.getInfoSql()); // delete get sql method from MInfoWindow
		//
		////////////////////////////////////
		if(checkTranslatedSub != null && checkTranslatedSub.isChecked())
			sql.append(" AND "+"istranslated = ?");
		//
		if (log.isLoggable(Level.FINEST))
			log.finest(sql.toString());
		
		/*Object linkPara = null;
		if (obj != null && obj instanceof IDColumn){
			IDColumn ID = (IDColumn) obj;
			linkPara = ID.getRecord_ID();
		}else if (obj != null){
			linkPara = obj.toString();
		}else {
			//TODO:hard case
		}*/
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql.toString(), null);
			//TODO: implicit type conversion. will exception in some case must recheck
			if (relatedInfo.getTypeDataOfLink().equals(String.class)){
				pstmt.setString(1, (String)obj);
			}else if (relatedInfo.getTypeDataOfLink().equals(int.class)){
				pstmt.setInt(1, (int)obj);
				
			}else{
				pstmt.setObject(1, obj);
			}
			//
			int paramIndex = 1;
			
			if (orig instanceof String){
				pstmt.setString(++paramIndex, (String)orig);
			}else{
				pstmt.setObject(++paramIndex, orig);
			}
			if (tran instanceof String){
				pstmt.setString(++paramIndex, (String)tran);
			}else{
				pstmt.setObject(++paramIndex, tran);
			}
			//
			if(checkTranslatedSub != null && checkTranslatedSub.isChecked())
				pstmt.setString(++paramIndex, "N");
			
			rs = pstmt.executeQuery();
			loadEmbedded(rs, relatedInfo);
		}
		catch (Exception e)
		{
			log.log(Level.WARNING, sql.toString(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
	}	//	refresh
	
	private int origIndex = -1;
	private int tranIndex = -1;
	
	@Override
	protected void updateSubcontent (){
		
		int row = contentPanel.getSelectedRow();
		if (row >= 0) {
			for (EmbedWinInfo embed : embeddedWinList1) {
				// default link column is key column
				int indexData = 0;
				if (columnDataIndex.containsKey(embed.getParentLinkColumnID())){
					// get index of link column
					indexData = p_layout.length + columnDataIndex.get(embed.getParentLinkColumnID());
				}
				
				if(embed.getInfowin().getName().equalsIgnoreCase(subInfoWindow) 
						&& infoWindow.getName().equalsIgnoreCase(mainInfoWindow) 
						&& origIndex != -1 && tranIndex != -1 && languageEditor != null) {
					
					refresh(languageEditor.getValue(), embed, 
							contentPanel.getValueAt(row, origIndex), 
							contentPanel.getValueAt(row, tranIndex));
				} else 
					refresh(contentPanel.getValueAt(row,indexData),embed);
			}// refresh for all
		}else{
			for (EmbedWinInfo embed : embeddedWinList1) {
				refresh(embed);
			}
		}
	} // updatesub
	
	/**
	 * @author xolali IDEMPIERE-1045
	 * getInfoColumnslayout(MInfoWindow info)
	 */
	public ArrayList<ColumnInfo> getInfoColumnslayout(MInfoWindow info){

		AccessSqlParser sqlParser = new AccessSqlParser("SELECT * FROM " + info.getFromClause());
		TableInfo[] tableInfos = sqlParser.getTableInfo(0);

		MInfoColumn[] infoColumns = info.getInfoColumns(tableInfos);
		ArrayList<ColumnInfo> list = new ArrayList<ColumnInfo>();
		String keyTableAlias = tableInfos[0].getSynonym() != null && tableInfos[0].getSynonym().trim().length() > 0
				? tableInfos[0].getSynonym()
						: tableInfos[0].getTableName();

				String keySelectClause = keyTableAlias + "." + p_keyColumn;

				for (MInfoColumn infoColumn : infoColumns)
				{
					if (infoColumn.isDisplayed(infoContext, p_WindowNo))
					{
						ColumnInfo columnInfo = null;
						String colSQL = infoColumn.getSelectClause();
						if (! colSQL.toUpperCase().contains(" AS "))
							colSQL += " AS " + infoColumn.getColumnName();
						if (infoColumn.getAD_Reference_ID() == DisplayType.ID)
						{
							if (infoColumn.getSelectClause().equalsIgnoreCase(keySelectClause))
								continue;

							columnInfo = new ColumnInfo(infoColumn.get_Translation("Name"), colSQL, DisplayType.getClass(infoColumn.getAD_Reference_ID(), true));
						}
						else if (DisplayType.isLookup(infoColumn.getAD_Reference_ID()))
						{
							if (infoColumn.getAD_Reference_ID() == DisplayType.List)
							{
								WEditor editor = null;							
						        editor = WebEditorFactory.getEditor(getGridField(infoColumn), true);
						        editor.setMandatory(false);
						        editor.setReadWrite(false);
						        editorMap.put(colSQL, editor);
								columnInfo = new ColumnInfo(infoColumn.get_Translation("Name"), colSQL, ValueNamePair.class, (String)null);
							}
							else
							{
								GridField field = getGridField(infoColumn);
								columnInfo = createLookupColumnInfo(tableInfos, field, infoColumn);
							}
						}
						else
						{
							columnInfo = new ColumnInfo(infoColumn.get_Translation("Name"), colSQL, DisplayType.getClass(infoColumn.getAD_Reference_ID(), true));
						}
						columnInfo.setColDescription(infoColumn.get_Translation("Description"));
						columnInfo.setGridField(getGridField(infoColumn));
						list.add(columnInfo);
						
						//
						/*if(infoColumn.getName().equalsIgnoreCase("not translated")
								&& infoColumn.getSelectClause().toLowerCase().contains("istranslated")
								&& infoColumn.getAD_Reference().getName().equalsIgnoreCase("yes-no"))
							createTranslatedSubCheckbox();*/
					}
				}

				return   list;
	}	
	
	@Override
	protected boolean loadInfoRelatedTabs() {
		embeddedPane1=new Tabbox();
		embeddedWinList1=new ArrayList <EmbedWinInfo>();
		
		if (infoWindow == null)
			return false;

		// topinfoColumns = infoWindow.getInfoColumns();
		relatedInfoList = infoWindow.getInfoRelated(true);
		Tabpanels tabPanels = new Tabpanels();
		Tabs tabs = new Tabs();

		if (relatedInfoList.length > 0) { // setup the panel

			//embeddedPane.setTitle(Msg.translate(Env.getCtx(), "Related Information"));
			
			ZKUpdateUtil.setHeight(embeddedPane1, "100%");
			//tabPanels = new Tabpanels();
			embeddedPane1.appendChild(tabPanels);
			//tabs = new Tabs();
			embeddedPane1.appendChild(tabs);
		}

		for (MInfoRelated relatedInfo:relatedInfoList) {
			String tableName = null;		
			int infoRelatedID = relatedInfo.getRelatedInfo_ID(); 

			MInfoWindow embedInfo = new MInfoWindow(Env.getCtx(), infoRelatedID, null);

			AccessSqlParser sqlParser = new AccessSqlParser("SELECT * FROM " + embedInfo.getFromClause());
			TableInfo[] tableInfos = sqlParser.getTableInfo(0);
			if (tableInfos[0].getSynonym() != null && tableInfos[0].getSynonym().trim().length() > 0){
				tableName = tableInfos[0].getSynonym().trim();
			}

			WListbox embeddedTbl = new WListbox();
			String m_sqlEmbedded;

			if (embedInfo != null) {
				ArrayList<ColumnInfo> list = new ArrayList<ColumnInfo>();
				list = getInfoColumnslayout(embedInfo);
				//  Convert ArrayList to Array
				ColumnInfo[] s_layoutEmbedded  = new ColumnInfo[list.size()];
				list.toArray(s_layoutEmbedded);

				/**	From Clause							*/
				String s_sqlFrom = embedInfo.getFromClause();
				/** Where Clause need original, translation, language						*/
				String s_sqlWhere = relatedInfo.getLinkColumnName() + "=?";//ad_language
				//
				if(embedInfo.getName().equalsIgnoreCase(subInfoWindow) 
						&& infoWindow.getName().equalsIgnoreCase(mainInfoWindow)) {
					// related trl_view synonym
					s_sqlWhere += " AND original like ? AND translation like ?"; /*AND v.ad_language=?";*/
				}
				
				//
				/* if(checkTranslatedSub != null && checkTranslatedSub.isChecked())
					s_sqlWhere += " AND isTranslated = ?";		
				 */
				//
				m_sqlEmbedded = embeddedTbl.prepareTable(s_layoutEmbedded, s_sqlFrom, s_sqlWhere, false, tableName);
				
				embeddedTbl.setMultiSelection(false);

				embeddedTbl.autoSize();

				embeddedTbl.getModel().addTableModelListener(this);
				ZKUpdateUtil.setVflex(embeddedTbl, "1");
				
				//Xolali - add embeddedTbl to list, add m_sqlembedded to list
				EmbedWinInfo ewinInfo = new EmbedWinInfo(embedInfo,embeddedTbl,m_sqlEmbedded,relatedInfo.getLinkColumnName(), relatedInfo.getLinkInfoColumn(), relatedInfo.getParentRelatedColumn_ID());
				embeddedWinList1.add(ewinInfo);

				MInfoWindow riw = (MInfoWindow) relatedInfo.getRelatedInfo();
				String tabTitle;
				if (riw != null)
					tabTitle = Util.cleanAmp(riw.get_Translation("Name"));
				else
					tabTitle = relatedInfo.getName();
				Tab tab = new Tab(tabTitle);
				tabs.appendChild(tab);
				Tabpanel desktopTabPanel = new Tabpanel();
				//desktopTabPanel.
				ZKUpdateUtil.setHeight(desktopTabPanel, "100%");
				desktopTabPanel.appendChild(embeddedTbl);
				
				//
				if(checkTranslatedSub != null)
					desktopTabPanel.appendChild(checkTranslatedSub);
				
				tabPanels.appendChild(desktopTabPanel);
				
			}
		}
		return true;
	}
	
	@Override
	protected void renderContentPane(Center center) {				
		Div div = new Div();
		div.setStyle("width :100%; height: 100%");
		ZKUpdateUtil.setVflex(div, "1");
		ZKUpdateUtil.setHflex(div, "1");
		div.appendChild(contentPanel);	
		//
		//createTranslatedMainCheckbox();
		if(checkTranslatedMain != null)
			div.appendChild(checkTranslatedMain);
		//
		Borderlayout inner = new Borderlayout();
		ZKUpdateUtil.setWidth(inner, "100%");
		ZKUpdateUtil.setHeight(inner, "100%");
		int height = SessionManager.getAppDesktop().getClientInfo().desktopHeight * 90 / 100;
		if (isLookup())
			inner.setStyle("border: none; position: relative; ");
		else
			inner.setStyle("border: none; position: absolute; ");
		inner.appendCenter(div);
		//true will conflict with listbox scrolling
		inner.getCenter().setAutoscroll(false);

		if (embeddedWinList1.size() > 0) {
			South south = new South();
			int detailHeight = (height * 25 / 100);
			ZKUpdateUtil.setHeight(south, detailHeight + "px");
			south.setAutoscroll(true);
			south.setCollapsible(true);
			south.setSplittable(true);
			south.setTitle(Msg.translate(Env.getCtx(), "Related Information"));
			south.setTooltiptext(Msg.translate(Env.getCtx(), "Related Information"));

			south.addEventListener(Events.ON_SWIPE, new EventListener<SwipeEvent>() {
				
				@Override
				public void onEvent(SwipeEvent event) throws Exception {
					South south = (South) event.getTarget();
					if ("down".equals(event.getSwipeDirection())) {
						south.setOpen(false);
					}
				}
			});
			south.setSclass("south-collapsible-with-title");
			south.setAutoscroll(true);
			//south.sets
			inner.appendChild(south);
			embeddedPane1.setSclass("info-product-tabbedpane");
			ZKUpdateUtil.setVflex(embeddedPane1, "1");
			ZKUpdateUtil.setHflex(embeddedPane1, "1");

			south.appendChild(embeddedPane1);
		}// render embedded
		center.appendChild(inner);
	}
	
	/** embedded Panel **/
    private Tabbox embeddedPane1;
    private ArrayList <EmbedWinInfo> embeddedWinList1;

	// Elaine 2008/11/25
	protected Borderlayout borderlayout;

	protected Tabbox tabbedPane;
    protected String m_sqlContext;
    
    int mWindowNo = 0;

	/** Instance Button				*/
	protected Button	m_configButton = null;
	
	public InfoTranslation(int WindowNo, String tableName, String keyColumn,
			String queryValue, boolean multipleSelection, String whereClause,
			int AD_InfoWindow_ID) {	//	lookup=true
		this(WindowNo, tableName, keyColumn, queryValue, multipleSelection,
				whereClause, AD_InfoWindow_ID, true);
		
		log.warning("=33============");
	}
	
	public InfoTranslation(int WindowNo, String tableName, String keyColumn,
			String queryValue, boolean multipleSelection, String whereClause,
			int AD_InfoWindow_ID, boolean lookup) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection,
				whereClause, AD_InfoWindow_ID, lookup);
		
		log.warning("=22============");		
	}
	
	@Override
    protected void executeQuery() {
    	prepareTable();
    	origIndex = findColumnIndex("Original");
		tranIndex = findColumnIndex("Translation");
    	super.executeQuery();
    }
	
	@Override
	protected void renderWindow() {
		super.renderWindow();
		//contentPanel.setMaxlength(10);
		
		m_configButton = confirmPanel.createButton("Configuration");
		confirmPanel.addComponentsLeft(m_configButton);
		m_configButton.setEnabled(true);
		m_configButton.setVisible(true);
		//m_configButton.setImage("");
		
		m_configButton.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			
			@Override
			public void onEvent(Event event) throws Exception {
				configButtonClick();
			}
		});
	}
	
	public void configButtonClick() 
	{			
		final ConfigurationWindow cw = new ConfigurationWindow(this);
		cw.addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				//update/reload editors
				
				if (tableEditor==null||columnEditor==null)return;
				tableEditor.actionRefresh(); // ?
				columnEditor.actionRefresh();
			}
		});
	}
	
	/**
	 * enable or disable all control button
	 *  Enable OK, History, Zoom if row/s selected
     *  ---
     *  Changes: Changed the logic for accommodating multiple selection
     *  @author ashley
	 */
	/*protected void enableButtons (boolean enable)
	{
		confirmPanel.getOKButton().setEnabled(enable); //red1 allow Process for 1 or more records

		if (hasHistory())
			confirmPanel.getButton(ConfirmPanel.A_HISTORY).setEnabled(enable);
		if (hasZoom()) {
			for (EmbedWinInfo embed : embeddedWinList1) {				
				if(embed.getInfowin().getName().equalsIgnoreCase(subInfoWindow))
					confirmPanel.getButton(ConfirmPanel.A_ZOOM).setEnabled((((WListbox)embed.getInfoTbl()).getSelectedRow() != -1) ); 
				
			}
		} 
		if (hasProcess())
			confirmPanel.getButton(ConfirmPanel.A_PROCESS).setEnabled(enable);
		// IDEMPIERE-1334 start
		for (Button btProcess : btProcessList){
			btProcess.setEnabled(enable);
		}
		if (btCbbProcess != null){
			btCbbProcess.setEnabled(enable);
		}
		
		if (btMenuProcess != null){
			btMenuProcess.setEnabled(enable);
		}
		
		if (cbbProcess != null){
			cbbProcess.setEnabled(enable);
		}
		// IDEMPIERE-1334 end
	}   //  enableButtons*/
	
	
	/*private void onDoubleClick()
	{
		if (isLookup())
		{
			dispose(true);
		}
		else
		{
			for (EmbedWinInfo embed : embeddedWinList1) {				
				if(embed.getInfowin().getName().equalsIgnoreCase(subInfoWindow))
					zoom(embed);
			}
		}
	}*/
	
	/*public void zoom() 
    {
		zoom(zoomEmbed);
    }*/
	
	
	@Override
	public void onEvent(Event event) 
	{
    	WListbox zoomEmbed = null;
    	//
		if (event.getName().equals(Events.ON_FOCUS) && event.getTarget() != null && 
				event.getTarget().getAttribute(ATT_INFO_PROCESS_KEY) != null){
			
			MProcess process = (MProcess)event.getTarget().getAttribute(ATT_INFO_PROCESS_KEY);
			SessionManager.getAppDesktop().updateHelpTooltip(process.get_Translation(MProcess.COLUMNNAME_Name), process.get_Translation(MProcess.COLUMNNAME_Description), process.get_Translation(MProcess.COLUMNNAME_Help), null);
		}
		else if (event.getName().equals(Events.ON_FOCUS)) {
    		for (WEditor editor : editors)
    		{
    			if (editor.isComponentOfEditor(event.getTarget()))
    			{
        			SessionManager.getAppDesktop().updateHelpTooltip(editor.getGridField());
        			return;
    			}
    		}
    	}else if (event.getName().equals(Events.ON_SELECT) && event.getTarget().equals(cbbProcess)){
    		// update help panel when change select item in combobox process
    		Comboitem selectedItem = cbbProcess.getSelectedItem();
    		if (selectedItem != null && selectedItem.getValue() != null){
    			MProcess selectedValue = (MProcess)selectedItem.getValue();
    			
        		SessionManager.getAppDesktop().updateHelpTooltip(selectedValue.get_Translation(MProcess.COLUMNNAME_Name), selectedValue.get_Translation(MProcess.COLUMNNAME_Description), selectedValue.get_Translation(MProcess.COLUMNNAME_Help), null);
    		}
    		    		
    	}else if (event.getName().equals(Events.ON_OK) && event.getTarget() != null){ // event when push enter at parameter
    		Component tagetComponent = event.getTarget();
    		boolean isCacheEvent = false;// when event from parameter component, catch it and process at there
    		for(WEditor editor : editors) {
    			Component editorComponent = editor.getComponent();
    			if (editorComponent instanceof EditorBox){
    				editorComponent = ((EditorBox)editorComponent).getTextbox();
    			}
    			if (editorComponent.equals(tagetComponent)){
    				// IDEMPIERE-2136
        			if (editor instanceof WSearchEditor){
        				if (((WSearchEditor)editor).isShowingDialog()){
    						return;
    					}
        			}
    				isCacheEvent = true;
    				break;
    			}
    		}
    		
    		if (isCacheEvent){
    			boolean isParameterChange = isParameteChangeValue();
        		// when change parameter, also requery
        		if (isParameterChange){
        			if (!isQueryByUser)
        				onUserQuery();
        		}else if (m_lookup && contentPanel.getSelectedIndex() >= 0){
        			// do nothing when parameter not change and at window mode, or at dialog mode but select non record    			
        			onOk();
        		}else {
        			// parameter not change. do nothing.
        		}
    		}else{
    			super.onEvent(event);
    		}
    		
    	}
		//
    	else if (event.getTarget().equals(confirmPanel.getButton(ConfirmPanel.A_ZOOM))) {
    		if(infoWindow.getName().equalsIgnoreCase(mainInfoWindow)) {
    			for (EmbedWinInfo embed : embeddedWinList1) {
    				if(embed.getInfowin().getName().equalsIgnoreCase(subInfoWindow)) {
    					zoomEmbed = (WListbox)embed.getInfoTbl();
    					break;
    				}
    			}
    		}
    		
            if(zoomEmbed != null) {
            	// zoom
				if (!contentPanel.getChildren().isEmpty() 
						&& contentPanel.getSelectedRowKey()!=null) {	
					zoom(zoomEmbed);
					if (isLookup())
						this.detach();
				}
            }else
            	super.onEvent(event);		
        }
    	
    	else
    	{
    		super.onEvent(event);
    	}		
    }
	
	
	/**
	 *	Specific Zoom for sub table
	 */
	public void zoom(WListbox embed) 
	{		
		//Integer ID = contentPanel.getSelectedRowKey();
		int row = embed.getSelectedRow();
		
		if(row == -1)
			return;
		
		Integer id = -1;
		String table=null;

		if (log.isLoggable(Level.INFO)) 
			log.info("InfoTranslation.zoom");
		
		int zoomtableIndex = -1, termIdIndex = -1;
		
		for(int i = 0; i < embed.getLayoutInfo().length; i++) {
			if(embed.getLayoutInfo()[i].getColHeader().equals("DB Table Name")) {
				zoomtableIndex = i;
			}
			else if(embed.getLayoutInfo()[i].getColHeader().equals("term_id")) {
				termIdIndex = i;
			}
			if(zoomtableIndex != -1 && termIdIndex != -1)
				break;
		}		
	
		id = (Integer) embed.getValueAt(row, termIdIndex); 
		table = (String) embed.getValueAt(row, zoomtableIndex);
		String lang = languageEditor.getDisplay();
			
		final MQuery query = new MQuery(table);
		
		query.addRestriction(table.substring(0, table.length()-3)+"ID", MQuery.EQUAL , id);
		query.addRestriction("AD_Language", MQuery.EQUAL , lang);
		
		query.setZoomTableName(table);
		query.setZoomColumnName(table.substring(0, table.length()-3)+"ID");
		query.setZoomValue(id);
		/*String sql = "SELECT COUNT(*) FROM " + table + " WHERE "+query.getWhereClause(true);
		int count = DB.getSQLValue(null, sql);
		query.setRecordCount(count);*/	
		query.setRecordCount(1); // should always be one
		
		//table and table_trl same window_id
		String isSOTrx = Env.getContext(Env.getCtx(), p_WindowNo, "IsSOTrx");
		if (!isLookup() && Util.isEmpty(isSOTrx)) {
			isSOTrx = "Y";
		}
		
		int AD_Window_ID = -1;
		AD_Window_ID = getAD_Window_ID(table/*.substring(0, table.length()-4)*/, isSOTrx.equalsIgnoreCase("Y"));

		AEnv.zoom(AD_Window_ID, query);
	}	//	zoom
	
	//rewritten so it doesn't use values of master info
	@Override
	protected int getAD_Window_ID (String tableName, boolean isSOTrx)
	{
		int m_SO_Window_ID = -1;
		int m_PO_Window_ID = -1;
		/*if (!isSOTrx && m_PO_Window_ID > 0)
			return m_PO_Window_ID;
		if (m_SO_Window_ID > 0)
			return m_SO_Window_ID;*/
		//
		String sql = "SELECT AD_Window_ID, PO_Window_ID FROM AD_Table WHERE TableName=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setString(1, tableName);
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				m_SO_Window_ID = rs.getInt(1);
				m_PO_Window_ID = rs.getInt(2);
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		//
		if (!isSOTrx && m_PO_Window_ID > 0)
			return m_PO_Window_ID;
		return m_SO_Window_ID;
	}	//	getAD_Window_ID
	
		
	//if(checkAND.ischecked()) relation between tableeditor and columneditor or maybe that's redundant :)
	
	// for table and column combo
	private WTableDirEditor tableEditor;
	private WTableDirEditor columnEditor;
	private WTableDirEditor languageEditor; 
	
	/**
	 * 
	 */
	public void init() {
		
		for(WEditor editor : editors) {
			if(editor instanceof WTableDirEditor ) {
				if(editor.getGridField() != null) {
					if(editor.getGridField().getColumnName().equals("TableName")) {				
						tableEditor = (WTableDirEditor) editor;		
					}
					else if(editor.getGridField().getColumnName().equals("ColumnName")) {
						columnEditor = (WTableDirEditor) editor;
					}
					else if(editor.getGridField().getColumnName().equals("AD_Language_ID")) {
						languageEditor = (WTableDirEditor) editor;
					}
				}
			}
		}
	}
	
}
