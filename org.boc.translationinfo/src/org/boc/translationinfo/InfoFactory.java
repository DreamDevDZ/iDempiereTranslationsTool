package org.boc.translationinfo;

import org.adempiere.webui.factory.IInfoFactory;
import org.adempiere.webui.info.InfoWindow;
import org.adempiere.webui.panel.InfoPanel;
import org.compiere.model.GridField;
import org.compiere.model.Lookup;
import org.compiere.model.MInfoWindow;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

public class InfoFactory implements IInfoFactory{

	private static CLogger log = CLogger.getCLogger(InfoFactory.class);
	
	@Override
	public InfoPanel create(int WindowNo, String tableName, String keyColumn,
			String value, boolean multiSelection, String whereClause,
			int AD_InfoWindow_ID, boolean lookup) {
		// TODO Auto-generated method stub
		log.warning("=1============");
		
		InfoPanel info =null;
		if(tableName.equalsIgnoreCase(CreateView.trl_view)) {
			
			CreateView.createTCView();
			CreateView.createTrlView();
			
			info = new InfoTranslation(WindowNo, tableName, keyColumn, value, multiSelection, whereClause, AD_InfoWindow_ID, lookup);
			
			if (!info.loadedOK()) 
			{
				info.dispose(false);
				info = null;
			}
		}
		
		return info;
	}

	@Override
	public InfoPanel create(Lookup lookup, GridField field, String tableName,
			String keyColumn, String value, boolean multiSelection,
			String whereClause, int AD_InfoWindow_ID) {
		// TODO Auto-generated method stub
		log.warning("=2============");
		
		InfoPanel info =null;
		if(tableName.equalsIgnoreCase(CreateView.trl_view)) {	
			info = create(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection, whereClause, AD_InfoWindow_ID, true);
		}
		
		return info;
	}

	@Override
	public InfoWindow create(int AD_InfoWindow_ID) {
		// TODO Auto-generated method stub	
		MInfoWindow infoWindow = new MInfoWindow(Env.getCtx(), AD_InfoWindow_ID, (String)null);
		String tableName = infoWindow.getAD_Table().getTableName();
		String keyColumn = tableName + "_ID";
		//infoWindow.getName()
		
		log.warning("=3="+AD_InfoWindow_ID+"="+tableName+"==========");
		
		InfoPanel info = null;
		if(tableName.equalsIgnoreCase(CreateView.trl_view)) {	
			info = create(-1, tableName, keyColumn, null, false, null, AD_InfoWindow_ID, false);
		}
		
		if (info instanceof InfoWindow)
			return (InfoWindow) info;
		else
			return null;
	}

}
