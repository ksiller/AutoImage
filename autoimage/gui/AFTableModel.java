package autoimage.gui;

import java.util.Iterator;
import javax.swing.table.AbstractTableModel;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten Siller
 */
public class AFTableModel extends AbstractTableModel {
    public final String[] COLUMN_NAMES = new String[]{"Property", "Value"};
    private String[][] afSettings; // holds Property/Value pairs

    public AFTableModel(JSONObject settings) {
        super();
        setData(settings);
    }

    /**
     * Converts autofocus settings into String[][] describing property/value pairs.
     * 
     * @param settings JSONObject describing autofocus settings
     */
    public final void setData(JSONObject settings) {
        if (settings == null) {
            afSettings=new String[2][0];
        } else {
            afSettings=new String[2][settings.length()];
            Iterator keys=settings.keys();
            int row=0;
            while (keys.hasNext()) {
                String key=(String)keys.next();
                afSettings[0][row]=key;
                try {
                    afSettings[1][row]=settings.getString(key);
                } catch (JSONException ex) {
                    afSettings[1][row]="no entry";
                }
                row++;
            }
        }
    }

    @Override
    public int getRowCount() {
        return afSettings[0].length;
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return COLUMN_NAMES[columnIndex];
    }

    @Override
    public Class getColumnClass(int colIndex) {
        return getValueAt(0, colIndex).getClass();
    }

    @Override
    public Object getValueAt(int rowIndex, int colIndex) {
        if (rowIndex<getRowCount() && colIndex<getColumnCount())
            return afSettings[colIndex][rowIndex];
        else 
            return "";
    }

    @Override
    public boolean isCellEditable(int rowIndex, int colIndex) {
        return false;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int colIndex) {
        if (rowIndex<getRowCount() && colIndex<getColumnCount()) {
            afSettings[colIndex][rowIndex]=(String)value;
        }    
    }
    
}
