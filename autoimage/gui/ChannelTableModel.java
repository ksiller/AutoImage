package autoimage.gui;

import autoimage.api.Channel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Karsten Siller
 */
public class ChannelTableModel extends AbstractTableModel {

    public final String[] COLUMN_NAMES = new String[]{"Configuration", "Exp [ms]", "z-Offset", "Color"};
    private List<Channel> channels;

    public ChannelTableModel(List<Channel> cl) {
        super();
        setData(cl);
    }

    public void setData(List<Channel> cl) {
        if (cl == null) {
            cl = new ArrayList<Channel>();
        }
        this.channels = cl;
    }

    public List<Channel> getChannelList() {
        return channels;
    }

    @Override
    public int getRowCount() {
        return channels.size();
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
        Channel c;
        if (channels != null & rowIndex < channels.size()) {
            c = channels.get(rowIndex);
            if (colIndex == 0) {
                return c.getName();
            } else if (colIndex == 1) {
                return c.getExposure();
            } else if (colIndex == 2) {
                return c.getZOffset();
            } else if (colIndex == 3) {
                return c.getColor();
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
        Channel c;
        if (channels != null & rowIndex < channels.size()) {
            c = channels.get(rowIndex);
            if (colIndex == 0) {
                c.setName((String) value);
                fireTableCellUpdated(rowIndex, colIndex);
            } else if (colIndex == 1) {
                c.setExposure(new Double((Double) value));
                fireTableCellUpdated(rowIndex, colIndex);
            } else if (colIndex == 2) {
                c.setZOffset(new Double((Double) value));
                fireTableCellUpdated(rowIndex, colIndex);
            } else if (colIndex == 3) {
                c.setColor((Color) value);
                fireTableCellUpdated(rowIndex, colIndex);
            }
        }
    }

    public void addRow(Object value) {
        Channel c = (Channel) value;
        channels.add(c);
        fireTableRowsInserted(getRowCount(), getRowCount());
    }

    public Channel getRowData(int rowIdx) {
        if (rowIdx >= 0 && rowIdx < channels.size()) {
            return channels.get(rowIdx);
        } else {
            return null;
        }
    }

    public int rowDown(int[] rowIdx) {
        if (rowIdx.length == 1 & rowIdx[0] < getRowCount() - 1) {
            Collections.swap(channels, rowIdx[0], rowIdx[0] + 1);
            fireTableRowsUpdated(rowIdx[0], rowIdx[0] + 1);
            return rowIdx[0] + 1;
        } else if (rowIdx[0] >= 0 && rowIdx[rowIdx.length - 1] < getRowCount() - 1) {
            Channel c = channels.get(rowIdx[rowIdx.length - 1] + 1);
            channels.add(rowIdx[0], c.duplicate());
            channels.remove(rowIdx[rowIdx.length - 1] + 2);
            fireTableRowsUpdated(rowIdx[0], rowIdx[rowIdx.length - 1] + 1);
            return rowIdx[0] + 1;
        }
        return rowIdx[0];
    }

    public int rowUp(int[] rowIdx) {
        if (rowIdx.length == 1 & rowIdx[0] > 0) {
            Collections.swap(channels, rowIdx[0], rowIdx[0] - 1);
            fireTableRowsUpdated(rowIdx[0] - 1, rowIdx[0]);
            return rowIdx[0] - 1;
        } else if (rowIdx[0] > 0 && rowIdx[rowIdx.length - 1] < getRowCount()) {
            Channel c = channels.get(rowIdx[0] - 1);
            channels.add(rowIdx[rowIdx.length - 1] + 1, c.duplicate());
            channels.remove(rowIdx[0] - 1);
            fireTableRowsUpdated(rowIdx[0] - 1, rowIdx[rowIdx.length - 1]);
            return rowIdx[0] - 1;
        }
        return rowIdx[0];
    }

    public void removeRow(Object element) {
        for (int i = 0; i < channels.size(); i++) {
            if (((Channel) element).equals(channels.get(i))) {
                channels.remove(i);
                fireTableRowsDeleted(i, i);
            }
        }
    }

    public void removeRows(int[] rowIdx) {
        for (int i = rowIdx[rowIdx.length - 1]; i >= rowIdx[0]; i--) {
            channels.remove(i);
        }
        fireTableRowsDeleted(rowIdx[0], rowIdx[rowIdx.length - 1]);
    }

    public void removeAllRows() {
        for (int i =channels.size()-1; i>=0; i--) {
            channels.remove(i);
        }
        fireTableRowsDeleted(0,channels.size()-1);
    }

    public int rowDown(int rowIdx) {
        if (rowIdx >= 0 && rowIdx < channels.size() - 1) {
            Channel channel = channels.get(rowIdx);
            channels.remove(rowIdx);
            channels.add(rowIdx + 1, channel);
            return rowIdx + 1;
        }
        return rowIdx;
    }

    public int rowUp(int rowIdx) {
        if (rowIdx >= 1 && rowIdx < channels.size()) {
            Channel channel = channels.get(rowIdx);
            channels.remove(rowIdx);
            channels.add(rowIdx - 1, channel);
            return rowIdx - 1;
        }
        return rowIdx;
    }

}

