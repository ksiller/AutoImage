package autoimage;

import autoimage.area.Area;
import ij.IJ;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Karsten Siller
 */
    public class AreaTableModel extends AbstractTableModel {

        public final String[] COLUMN_NAMES = new String[]{"", "Area Id", "Area Name", "Tiles", "Comment"};
        private List<Area> areas;
        private boolean areaRenamingAllowed = true;

        public AreaTableModel(List<Area> al) {
            super();
            setData(al,true);
        }

        public void setData(List<Area> al, boolean updateView) {
            if (al == null) {
                al = new ArrayList<Area>();
            }
            this.areas = al;
            if (updateView) {
                fireTableDataChanged();
            }
        }

        public List<Area> getAreaList() {
            return areas;
        }

        
        @Override
        public int getRowCount() {
            return areas.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int colIdx) {
            return COLUMN_NAMES[colIdx];
        }

        @Override
        public Class getColumnClass(int colIdx) {
            if (areas==null || areas.isEmpty()) {
                return Object.class;
            } else {           
                return getValueAt(0, colIdx).getClass();
            }    
        }

        @Override
        public Object getValueAt(int rowIndex, int colIndex) {
            Area a;
            if (areas != null & rowIndex < areas.size()) {
                a = areas.get(rowIndex);
                if (colIndex == 0) {
                    return a.isSelectedForAcq();
                } else if (colIndex == 1) {
                    return a.getId();
                } else if (colIndex == 2) {
                    return a.getName();
                } else if (colIndex == 3) {
                    DecimalFormat df = new DecimalFormat("###,###,##0");
                    if (a.isSelectedForAcq()) {
                        switch (a.getTilingStatus()) {
                            case Area.TILING_ERROR: return df.format(a.getTileNumber()) + " (Error)";
                            case Area.TILING_UNKNOWN_NUMBER: return "???";    
                            default:return df.format(a.getTileNumber());
                        }
                    } else {
                        return df.format(0);
                    }
                } else if (colIndex == 4) {
                    return a.getComment();
                } else if (colIndex == 5) {
                    return a.getId();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int colIndex) {
            return (colIndex == 0 || (colIndex == 2 && areaRenamingAllowed) || colIndex == 4);
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int colIndex) {
            Area area;
            if (areas != null & rowIndex < areas.size()) {
                area = areas.get(rowIndex);
                switch (colIndex) {
                    case 0: {
                        area.setSelectedForAcq((Boolean) value);
                        fireTableCellUpdated(rowIndex, colIndex);
//                        fireTableCellUpdated(rowIndex, 3);
                        updateTileCell(rowIndex);
                        break;
                    }
                    case 2: {
                        area.setName(new String((String) value));
                        fireTableCellUpdated(rowIndex, colIndex);
                        break;
                    }
                    case 4: {
                        area.setComment(new String((String) value));
                        fireTableCellUpdated(rowIndex, colIndex);
                        break;
                    }
                }
            }
        }

        public void addRow(Object value) {
            Area a = (Area) value;
            areas.add(a);
            fireTableRowsInserted(getRowCount(), getRowCount());
        }

        public Area getRowData(int rowIdx) {
            if (rowIdx >= 0 && rowIdx < areas.size()) {
                return areas.get(rowIdx);
            } else {
                return null;
            }
        }

        /*
        @Param rowIdx: array of indices in model
        @Param lastPlusIndex: model rowindex that corresponds to 1 below selection in view
        */
        public void rowDown(int[] rowIdx, int lastPlusOneIndex) {
            //create copy 
            Area temp=areas.get(lastPlusOneIndex);
            //move last entry in selection
            areas.set(lastPlusOneIndex, areas.get(rowIdx[rowIdx.length-1]));
            //iterate for the rest
            for (int i=rowIdx.length-1; i>0; i--) {
                areas.set(rowIdx[i], areas.get(rowIdx[i-1]));
            }
            areas.set(rowIdx[0],temp);
            fireTableRowsUpdated(0,getRowCount()-1);
//            return 0;
        }

        /*
        @Param rowIdx: array of indices in model
        @Param firstMinusOneIndex: model rowindex that corresponds to 1 below selection in view
        */
        public void rowUp(int[] rowIdx, int firstMinusOneIndex) {
            //create copy 
            Area temp=areas.get(firstMinusOneIndex);
            //move last first entry in selection
            areas.set(firstMinusOneIndex, areas.get(rowIdx[0]));
            //iterate for the rest
            for (int i=0; i<rowIdx.length-1; i++) {
                areas.set(rowIdx[i], areas.get(rowIdx[i+1]));
            }
            areas.set(rowIdx[rowIdx.length-1],temp);
            fireTableRowsUpdated(0,getRowCount()-1);
        }

        public void removeRows(int[] rowIdx) {
            Arrays.sort(rowIdx);
            for (int i = rowIdx.length - 1; i >= 0; i--) {
                areas.remove(rowIdx[i]);
//                fireTableRowsDeleted(rowIdx[i],rowIdx[i]);
            }
            fireTableDataChanged();
        }

        private void updateTileCell(int rowIndex) {
            fireTableCellUpdated(rowIndex, 3);
        }

        protected void setAreaRenamingAllowed(boolean renamingAllowed) {
            areaRenamingAllowed=renamingAllowed;
        }

        public void setRowData(int rowIdx, Area area) {
            areas.set(rowIdx, area);
            fireTableRowsUpdated(rowIdx,rowIdx);
        }

    }
    // end AreaTableModel

