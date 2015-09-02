package autoimage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import org.micromanager.api.DataProcessor;

/**
 *
 * @author Karsten
 */
public class AcqSettingTableModel extends AbstractTableModel {

    public final String[] COLUMN_NAMES = new String[]{"Sequence", "Start Time"};
    private List<AcqSetting> settings;

    public AcqSettingTableModel(List<AcqSetting> al) {
        super();
//            setData(al);
        if (al == null) {
            al = new ArrayList<AcqSetting>();
        }
        this.settings = al;
    }

    public void setData(List<AcqSetting> al) {
        if (al == null) {
            al = new ArrayList<AcqSetting>();
        }
        this.settings = al;            
    }

    public List<AcqSetting> getAcqSettingList() {
        return settings;
    }

    @Override
    public int getRowCount() {
        return settings.size();
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
        AcqSetting s;
        if (settings != null & rowIndex < settings.size()) {
            s = settings.get(rowIndex);
            if (colIndex == 0) {
                return s.getName();
            } else if (colIndex == 1) {
                return s.getStartTime();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int colIndex) {
        return true;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int colIndex) {
        AcqSetting s;
        if (settings != null & rowIndex < settings.size()) {
            s = settings.get(rowIndex);
            if (colIndex == 0) {
                s.setName((String) value);
                fireTableCellUpdated(rowIndex, colIndex);
            } else if (colIndex == 1) {
                s.setStartTime((AcqSetting.ScheduledTime) value);
                fireTableCellUpdated(rowIndex, colIndex);
            }
        }
    }

    public void addRow(Object value, int row) {
        if (row !=-1 && row < settings.size()) {
            settings.add(row, (AcqSetting) value);
            fireTableRowsInserted(row, row);
        } else {
            settings.add((AcqSetting) value);
            fireTableRowsInserted(getRowCount(), getRowCount());
        }
    }

    public AcqSetting getRowData(int rowIdx) {
        if (rowIdx >= 0 && rowIdx < settings.size()) {
            return settings.get(rowIdx);
        } else {
            return null;
        }
    }

    public int rowDown(int[] rowIdx) {
        if (rowIdx.length == 1 && rowIdx[0] < getRowCount() - 1) {
            Collections.swap(settings, rowIdx[0], rowIdx[0] + 1);
            fireTableRowsUpdated(rowIdx[0], rowIdx[0] + 1);
            return rowIdx[0] + 1;
        } else if (rowIdx[0] >= 0 && rowIdx[rowIdx.length - 1] < getRowCount() - 1) {
            AcqSetting s = settings.get(rowIdx[rowIdx.length - 1] + 1);
            settings.add(rowIdx[0], s.duplicate());
            settings.remove(rowIdx[rowIdx.length - 1] + 2);
            fireTableRowsUpdated(rowIdx[0], rowIdx[rowIdx.length - 1] + 1);
            return rowIdx[0] + 1;
        }
        return rowIdx[0];
    }

    public int rowUp(int[] rowIdx) {
        if (rowIdx.length == 1 && rowIdx[0] > 0) {
            Collections.swap(settings, rowIdx[0], rowIdx[0] - 1);
            fireTableRowsUpdated(rowIdx[0] - 1, rowIdx[0]);
            return rowIdx[0] - 1;
        } else if (rowIdx[0] > 0 && rowIdx[rowIdx.length - 1] < getRowCount()) {
            AcqSetting s = settings.get(rowIdx[0] - 1);
            settings.add(rowIdx[rowIdx.length - 1] + 1, s.duplicate());
            settings.remove(rowIdx[0] - 1);
            fireTableRowsUpdated(rowIdx[0] - 1, rowIdx[rowIdx.length - 1]);
            return rowIdx[0] - 1;
        }
        return rowIdx[0];
    }

    public void removeRows(int[] rowIdxArray) {
        for (int i = rowIdxArray.length-1; i>=0; i--) {
            AcqSetting setting=settings.get(rowIdxArray[i]);
            DefaultMutableTreeNode root=setting.getImageProcessorTree();
            Enumeration<DefaultMutableTreeNode> en = root.preorderEnumeration();
            while (en.hasMoreElements()) {
                DefaultMutableTreeNode node = en.nextElement();
                DataProcessor proc=(DataProcessor)node.getUserObject();
                if (proc.isAlive()) {
                    proc.requestStop();
                }
                while (proc.isAlive()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            settings.remove(rowIdxArray[i]);
        }
        //fireTableDataChanged();
        fireTableRowsDeleted(rowIdxArray[0], rowIdxArray[rowIdxArray.length - 1]);
    }

    public int rowDown(int rowIdx) {
        if (rowIdx >= 0 && rowIdx < settings.size() - 1) {
            AcqSetting s = settings.get(rowIdx);
            settings.remove(rowIdx);
            settings.add(rowIdx + 1, s);
            return rowIdx + 1;
        }
        return rowIdx;
    }

    public int rowUp(int rowIdx) {
        if (rowIdx >= 1 && rowIdx < settings.size()) {
            AcqSetting s = settings.get(rowIdx);
            settings.remove(rowIdx);
            settings.add(rowIdx - 1, s);
            return rowIdx - 1;
        }
        return rowIdx;
    }

    protected void updateTileCell(int rowIndex) {
        fireTableCellUpdated(rowIndex, 1);
    }

}
// end AcqSettingTableModel

