package autoimage.gui.models;

import autoimage.api.IAcqLayout;
import autoimage.data.layout.Landmark;
import autoimage.events.LandmarkAddedEvent;
import autoimage.events.LandmarkDeletedEvent;
import autoimage.events.LandmarkListEvent;
import autoimage.events.LandmarkUpdatedEvent;
import com.google.common.eventbus.Subscribe;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Karsten Siller
 */
public class LandmarkTableModel extends AbstractTableModel {
    public final String[] COLUMN_NAMES = new String[]{"Name", "Stage X", "Stage Y", "Stage Z", "Layout X", "Layout Y", "Layout Z", "Status"};
    private IAcqLayout acqLayout;
    private List<Landmark> landmarks;

    public static LandmarkTableModel createInstance(final IAcqLayout layout) {
        LandmarkTableModel model=new LandmarkTableModel(layout);
        //to receive notification from layout when landmarks change
        layout.registerForEvents(model);
        return model;
    }
    
    private LandmarkTableModel(IAcqLayout layout) {
        super();
        if (layout==null) {
            throw new IllegalArgumentException("Layout object cannot be null");
        }
        acqLayout=layout;
        landmarks = layout.getLandmarks();
    }

    public List<Landmark> getLandmarks() {
        return landmarks;
    }

    @Override
    public int getRowCount() {
        return landmarks.size();
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
    public boolean isCellEditable(int rowIndex, int colIndex) {
        return (colIndex == 0 || colIndex == 4 || colIndex == 5 || colIndex == 6);
    }

    public void setStagePos(int row, double x, double y, double z) {
        if (row >= 0 && row < getRowCount()) {
            Landmark r=landmarks.get(row)
                    .copy()
                    .setStageCoord(x, y, z)
                    .setStagePosMapped(true)
                    .build();
            acqLayout.setLandmark(row, r);
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int colIndex) {
        if (landmarks != null & rowIndex < landmarks.size()) {
            Landmark.Builder builder=landmarks.get(rowIndex).copy();
            switch (colIndex) {
                case 0: {
                    builder.setName((String) value);
                    break;
                }
                case 1: {
                    builder.setStageCoordX((Double) value);
                    break;
                }
                case 2: {
                    builder.setStageCoordY((Double) value);
                    break;
                }
                case 3: {
                    builder.setStageCoordZ((Double) value);
                    break;
                }
                case 4: {
                    builder.setLayoutCoordX((Double) value);
                    break;
                }
                case 5: {
                    builder.setLayoutCoordY((Double) value);
                    break;
                }
                case 6: {
                    builder.setLayoutCoordZ((Double) value);
                    break;
                }
                case 7: {
                    builder.setStagePosMapped(((String) value).equals("mapped"));
                    break;
                }
            }
            acqLayout.setLandmark(rowIndex, builder.build());
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int colIndex) {
        Landmark a;
        if (landmarks != null && rowIndex < landmarks.size()) {
            a = landmarks.get(rowIndex);
            switch (colIndex) {
                case 0: {
                    return a.getName();
                }
                case 1: {
                    return a.getStageCoordX();
                }
                case 2: {
                    return a.getStageCoordY();
                }
                case 3: {
                    return a.getStageCoordZ();
                }
                case 4: {
                    return a.getLayoutCoordX();
                }
                case 5: {
                    return a.getLayoutCoordY();
                }
                case 6: {
                    return a.getLayoutCoordZ();
                }
                case 7: {
                    return (a.isStagePosMapped() ? "mapped" : "not mapped");
                }
            } 
            return null;
        } else {
            return null;
        }
    }

    public Landmark getRowData(int rowIdx) {
        if (rowIdx >= 0 && rowIdx < landmarks.size()) {
            return landmarks.get(rowIdx);
        } else {
            return null;
        }
    }

    public void setAcqLayout(IAcqLayout layout) {
        if (layout==null) {
            throw new IllegalArgumentException("Layout object cannot be null");
        }
        acqLayout=layout;
        acqLayout.registerForEvents(this);
        landmarks=layout.getLandmarks();
        fireTableDataChanged();
    }
    
    @Subscribe
    public void addedLandmark(LandmarkAddedEvent e) {
        if (e.getLayout()==acqLayout) {
            fireTableRowsInserted(e.getListIndex(), e.getListIndex());
        }
    }

    @Subscribe
    public void deletedLandmark(LandmarkDeletedEvent e) {
        if (e.getLayout()==acqLayout) {
            fireTableRowsDeleted(e.getListIndex(), e.getListIndex());
        }
    }

    @Subscribe
    public void updatedLandmark(LandmarkUpdatedEvent e) {
        if (e.getLayout()==acqLayout) {
            fireTableRowsUpdated(e.getListIndex(), e.getListIndex());
        }
    }

    @Subscribe
    public void landmarkListChanged(LandmarkListEvent e) {
        if (e.getLayout()==acqLayout) {
            fireTableDataChanged();
        }
    }
   
    public void addLandmark(Landmark lm) {
        acqLayout.addLandmark(lm);
    }

    public Landmark deleteLandmark(int row) {
        return acqLayout.deleteLandmark(row);
    }

    public Landmark updateLandmark(int row, Landmark lm) {
        return acqLayout.setLandmark(row, lm);
    }

    public void setLandmarks(List<Landmark> list) {
        acqLayout.setLandmarks(list);
    }
    

}