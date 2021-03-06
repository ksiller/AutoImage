package autoimage.data.layout.area;

import autoimage.data.layout.Tile;
import autoimage.utils.Utils;
import autoimage.api.BasicArea;
import autoimage.gui.views.NumberTableCellRenderer;
import autoimage.gui.views.AreaPreviewPanel;
import autoimage.apptools.LayoutManagerDlg;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten Siller
 */
public class CompoundArea extends BasicArea {
    
    public final static String TAG_COMBINATION_MODE = "COMBINATION_MODE";
    private final static DecimalFormat NUMBER_FORMAT = new DecimalFormat("###,###,##0.000");
    
    public final static int COMBINED_OR = 1;
    public final static int COMBINED_AND = 2;
    public final static int COMBINED_NOT = 3;
    public final static int COMBINED_XOR = 4;
    
    private List<BasicArea> areas;
    private int combinationMode;
    private static Map<String, String> availableAreaClasses=null;
    private String lastAreaType;
    
    private class AreaTableModel extends AbstractTableModel {

        public final String[] COLUMN_NAMES = new String[]{"Shape Name", "Type", "Center (mm)", "Width (mm)", "Height (mm)"};
        private List<BasicArea> areas;

        public AreaTableModel(List<BasicArea> al) {
            super();
            setData(al,false);
        }

        /**
         * 
         * @param al
         * @param updateView 
         */
        public void setData(List<BasicArea> al, boolean updateView) {
            if (al == null) {
                al = new ArrayList<BasicArea>();
            }
            this.areas = al;
            if (updateView) {
                fireTableDataChanged();
            }
        }

        /**
         * 
         * @return List of all areas in table model
         */
        public List<BasicArea> getAreaList() {
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
            return getValueAt(0, colIdx).getClass();
        }

        @Override
        public Object getValueAt(int rowIndex, int colIndex) {
            BasicArea a;
            if (areas != null & rowIndex < areas.size()) {
                a = areas.get(rowIndex);
                switch (colIndex) {
                    case 0: 
                        return a.getName();
                    case 1: 
                        return a.getShapeType();
                    case 2: 
                        /* display subarea's center position in layout coordinates
                           get subarea's center position and apply this compound area's 
                           shape-to-path transform 
                        */
                        Point2D absCenter = new Point2D.Double();
                        getShapeToPathTransform().transform(a.getCenterXYPos(), absCenter);
                        return NUMBER_FORMAT.format(absCenter.getX()/1000)+" / "+NUMBER_FORMAT.format(absCenter.getY()/1000);
                    case 3: 
                        /* display subarea's width in layout dimensions;
                           takes into account this compiund area's affine transform (e.g. rotation)  
                        */
                        Path2D absArea=new Path2D.Double(a);
                        absArea.transform(getShapeToPathTransform());
                        return absArea.getBounds2D().getWidth()/1000;
                    case 4: 
                        /* display subarea's height in layout dimensions;
                           takes into account this compiund area's affine transform (e.g. rotation)  
                        */
                        absArea=new Path2D.Double(a);
                        absArea.transform(getShapeToPathTransform());
                        return absArea.getBounds2D().getHeight()/1000;
                    default: return null;
                }
            } else {
                return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int colIndex) {
            //only able to edit subarea name in table
            return (colIndex == 1);
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int colIndex) {
            BasicArea area;
            if (areas != null & rowIndex < areas.size()) {
                area = areas.get(rowIndex);
                switch (colIndex) {
                    case 0: {
                        area.setName((String) value);
                        fireTableCellUpdated(rowIndex, colIndex);
                        break;
                    }
                }
            }
        }

        /**
         * Adds an area to the table model
         * @param area BasicArea object to be added to table
         */
        public void addRow(BasicArea area) {
            areas.add(area);
            fireTableRowsInserted(getRowCount(), getRowCount());
        }

        /**
         * Retrieves the area places in a specified row in the table model.
         * @param rowIdx row index in table model
         * @return BasicaArea object in the row [rowIdx], returns null if table model is empty or roIdx is out of bounds
         */
        public BasicArea getRowData(int rowIdx) {
            if (areas!=null && rowIdx >= 0 && rowIdx < areas.size()) {
                return areas.get(rowIdx);
            } else {
                return null;
            }
        }

        /** 
         * Moves a group of rows down by one row in the table model.
         * @Param rowIdx: array of indices in model
         * @Param lastPlusIndex: model row index that corresponds to one below selection in view
         */
        public int rowsDown(int[] rowIdx, int lastPlusOneIndex) {
            BasicArea temp=areas.get(lastPlusOneIndex);
            //move last entry in selection down one
            areas.set(lastPlusOneIndex, areas.get(rowIdx[rowIdx.length-1]));
            for (int i=rowIdx.length-1; i>0; i--) {
                areas.set(rowIdx[i], areas.get(rowIdx[i-1]));
            }
            areas.set(rowIdx[0],temp);
            fireTableRowsUpdated(0,getRowCount()-1);
            return 0;
        }

        /**
         * Moves a group of rows up by one row in the table model.
         * @Param rowIdx: array of indices in model
         * @Param firstMinusOneIndex: model row index that corresponds to one above selection in view
         */
        public int rowsUp(int[] rowIdx, int firstMinusOneIndex) {
            BasicArea temp=areas.get(firstMinusOneIndex);
            //move first entry in selection up one
            areas.set(firstMinusOneIndex, areas.get(rowIdx[0]));
            for (int i=0; i<rowIdx.length-1; i++) {
                areas.set(rowIdx[i], areas.get(rowIdx[i+1]));
            }
            areas.set(rowIdx[rowIdx.length-1],temp);
            fireTableRowsUpdated(0,getRowCount()-1);
            return 0;
        }

        public void removeRow(Object element) {
            for (int i = 0; i < areas.size(); i++) {
                if (((BasicArea) element).getId() == areas.get(i).getId()) {
                    areas.remove(i);
                    fireTableRowsDeleted(i, i);
                }
            }
        }

        public void removeRows(int[] rowIdx) {
            for (int i = rowIdx[rowIdx.length - 1]; i >= rowIdx[0]; i--) {
                areas.remove(i);
            }
            fireTableRowsDeleted(rowIdx[0], rowIdx[rowIdx.length - 1]);
        }

        /**
         * Replaces the area found at row [rowIdx] in table model with rarea
         * @param rowIdx
         * @param area 
         */
        private void setRowData(int rowIdx, BasicArea rarea) {
            if (rowIdx >=0 && rowIdx < areas.size()) { 
                areas.set(rowIdx, rarea);
                fireTableRowsUpdated(rowIdx,rowIdx);
            }    
        }

        /**
         * Convenience method to force update of all rows and notification of TableModelListeners
         */
        private void updateAllRows() {
            fireTableRowsUpdated(0, getRowCount()-1);
        }

    }
    // end AreaTableModel


    public CompoundArea() {
        this("NewArea");
    }
    
    public CompoundArea(String n) { //expects name identifier
        this(n, -1, 0,0,0,new ArrayList<BasicArea>(),COMBINED_OR,false,"");
    }
    
    public CompoundArea(String n, int id, double ox, double oy, double oz, List<BasicArea> a, int mode, boolean selForAcq, String anot) {
        super(n,id,ox,oy,oz,selForAcq,anot);
        areas = a;
        combinationMode=mode;
        createShape();
        centerShape();
        setRelDefaultPos();
        createGeneralPath();
    }

    @Override
    protected void initializeFromJSONObject(JSONObject obj) throws JSONException {
        areas = new ArrayList<BasicArea> ();
        JSONArray areaArray=obj.getJSONArray(TAG_AREA_ARRAY);
        for (int i=0; i<areaArray.length(); i++) {
            try {
                areas.add(BasicArea.createFromJSONObject(areaArray.getJSONObject(i)));
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(CompoundArea.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(CompoundArea.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(CompoundArea.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        combinationMode=obj.getInt(TAG_COMBINATION_MODE);
    }

    @Override
    protected void addFieldsToJSONObject(JSONObject obj) throws JSONException {
        JSONArray areaArray=new JSONArray();
        for (BasicArea area:areas) {
            areaArray.put(area.toJSONObject());
        }
        obj.put(TAG_AREA_ARRAY, areaArray);
        obj.put(TAG_COMBINATION_MODE, combinationMode);
    }    


    @Override
    public String getShapeType() {
        return "Compound Area";
    }

    @Override
    public BasicArea duplicate() {
        CompoundArea newArea = new CompoundArea(this.getName());
        newArea.setId(this.getId());
        newArea.centerXYPos=new Point2D.Double(this.getCenterXYPos().getX(), this.getCenterXYPos().getY());
        newArea.setRelativeZPos(this.relativeZPos);
        newArea.affineTrans=new AffineTransform(this.affineTrans);
        newArea.setSelectedForAcq(isSelectedForAcq());
        newArea.setSelectedForMerge(isSelectedForMerge());
        newArea.setComment(this.comment);
//        newArea.setAcquiring(this.acquiring);
        newArea.tilePosList=new ArrayList<Tile>(this.getTilePositions());
        newArea.setUnknownTileNum(this.hasUnknownTileNum());
        newArea.areas = new ArrayList<BasicArea>();
        for (BasicArea area:this.areas) {
            newArea.areas.add(area.duplicate());
        }
        newArea.combinationMode=this.combinationMode;
        newArea.createShape();
//        newArea.centerShape();
        newArea.setRelDefaultPos();
        newArea.createGeneralPath();
        return newArea;    
    }

    /**
     * Adds area to the list of areas managed by this compound area. Creates a new list of the current list is null.
     * @param area 
     */
    public void addArea(BasicArea area) {
        if (areas==null) {
            areas = new ArrayList<BasicArea>();
        }
        areas.add(area);
    }
    
    /**
     * Removes an area from areas list
     * @param area BasicArea object to be removed
     * @return true if area was found and removed, false if area list is empty or area was not removed.
     */
    public boolean removeArea(BasicArea area) {
        if (areas==null) {
            return false;
        }
        return areas.remove(area);
    }
    
    /**
     * Calculates the bounds of this.shape and translates it so that this.shape's center is placed at the origin (0/0)
     */
    protected void centerShape() {
        double cx=shape.getBounds2D().getCenterX();
        double cy=shape.getBounds2D().getCenterY();
        //if properly centered, cx and cy should be 0
        //if not we translate created shape to origin        
        Point2D translationVec = new Point2D.Double(-cx,-cy);// shape translation vector
        //apply the affineTrans to shape translation vector
        affineTrans.transform(translationVec, translationVec);
        //update all areas in this compound area
        for (BasicArea area:areas) {
            area.setCenter(area.getCenterXYPos().getX()-cx, area.getCenterXYPos().getY()-cy);
/*            //apply inverted adjusted translation vector
            area.setCenter(area.getCenterXYPos().getX()+translationVec.getX(), area.getCenterXYPos().getY()+translationVec.getY());*/
        }
        //translate shape
        AffineTransform at=new AffineTransform();
        at.translate(-cx, -cy);
        shape.transform(at);
        //update centerXYPos to ensure that the generalPath positioning remains unchanged
        //apply inverted adjusted translation vector
        centerXYPos=new Point2D.Double(centerXYPos.getX()-translationVec.getX(),centerXYPos.getY()-translationVec.getY());
    }
    
    /**
     * Updates and forces repaint of a AreaPreviewPanel object based on currently calculated this.generalPath and selected subarea in subarea table.
     * @param shapeTable JTable object that contains list of all subareas
     * @param previewPanel The AreaPreviewPanel object to be updated
     */
    private void updatePreviewPanel(JTable shapeTable, AreaPreviewPanel previewPanel) {
        int[] rows=shapeTable.getSelectedRows();
        previewPanel.setPath(generalPath, centerXYPos, getShapeBoundsDiagonale());
        for (int row:rows) {
            row=Math.min(shapeTable.convertRowIndexToModel(row), shapeTable.getRowCount()-1);
            BasicArea area=areas.get(shapeTable.convertRowIndexToModel(row));
            Path2D selPath=new Path2D.Double(area.getGeneralPath());
            selPath.transform(getShapeToPathTransform());
            previewPanel.addPath(selPath,Color.RED,null);
        }
        previewPanel.repaint();
    }
    
    
    @Override
    public BasicArea showConfigDialog(final Rectangle2D layoutBounds) {
        if (availableAreaClasses==null) {
            try {
                availableAreaClasses=Utils.getAvailableAreaClasses();
            } catch (IOException ex) {
                Logger.getLogger(CompoundArea.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        centerShape();
        
        JPanel optionPanel = new JPanel();
        GridLayout layout = new GridLayout(0,4);
        optionPanel.setLayout(layout);
        
        JLabel l=new JLabel("Name:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        JFormattedTextField nameField = new JFormattedTextField();
        nameField.setColumns(10);
        nameField.setValue(new String(name));
        optionPanel.add(nameField);

        l=new JLabel("Z Offset (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        JFormattedTextField zField = new JFormattedTextField();
        zField.setColumns(10);
        zField.setValue(new Double(relativeZPos) / 1000);
        optionPanel.add(zField);
             
        l=new JLabel("Origin X (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField topLeftXField = new JFormattedTextField();
        topLeftXField.setColumns(10);
        topLeftXField.setValue(new Double(getTopLeftX()) / 1000);
        optionPanel.add(topLeftXField);
        
        l=new JLabel("Origin Y (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField topLeftYField = new JFormattedTextField();
        topLeftYField.setColumns(10);
        topLeftYField.setValue(new Double(getTopLeftY()) / 1000);
        optionPanel.add(topLeftYField);
        
        l=new JLabel("Center X (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField centerXField = new JFormattedTextField();
        centerXField.setColumns(10);
        centerXField.setValue(new Double(centerXYPos.getX()) / 1000);
        optionPanel.add(centerXField);

        l=new JLabel("Center Y (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField centerYField = new JFormattedTextField();
        centerYField.setColumns(10);
        centerYField.setValue(new Double(centerXYPos.getY()) / 1000);
        optionPanel.add(centerYField);

        l=new JLabel("Width (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JLabel widthField = new JLabel(NUMBER_FORMAT.format(generalPath.getBounds2D().getWidth() / 1000));
        optionPanel.add(widthField);

        l=new JLabel("Height (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JLabel heightField = new JLabel(NUMBER_FORMAT.format(generalPath.getBounds2D().getHeight() / 1000));
        optionPanel.add(heightField);
        
        l=new JLabel("Rotation (degree):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField rotationField = new JFormattedTextField();
        rotationField.setColumns(10);
        rotationField.setValue(new Double(Utils.getRotation(affineTrans))/Math.PI*180);
        optionPanel.add(rotationField);        
        
        final JTable shapeTable = new JTable(new AreaTableModel(areas));
        shapeTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        
        final AreaPreviewPanel previewPanel = new AreaPreviewPanel(generalPath, getShapeBoundsDiagonale());
        previewPanel.setPreferredSize(new Dimension (160,160));
        
        JPanel modePanel = new JPanel();
        JLabel label = new JLabel("Shape combination mode:");
        JToggleButton xorButton=new JToggleButton("Exclusive Or");
        xorButton.setIcon(new ImageIcon(getClass().getResource("/autoimage/resources/Combined_Xor.png")));
        xorButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                combinationMode=COMBINED_XOR;
                createShape();
                centerShape();
                createGeneralPath();
                centerXField.setValue(new Double(centerXYPos.getX()) / 1000);
                centerYField.setValue(new Double(centerXYPos.getY()) / 1000);
                topLeftXField.setValue(new Double(getTopLeftX()) / 1000);
                topLeftYField.setValue(new Double(getTopLeftY()) / 1000);
                widthField.setText(NUMBER_FORMAT.format(generalPath.getBounds2D().getWidth()/ 1000));
                heightField.setText(NUMBER_FORMAT.format(generalPath.getBounds2D().getHeight()/ 1000));
                updatePreviewPanel(shapeTable,previewPanel);
            }
            
        });
        JToggleButton andButton=new JToggleButton("Intersect");
        andButton.setIcon(new ImageIcon(getClass().getResource("/autoimage/resources/Combined_And.png")));
        andButton.setPreferredSize(xorButton.getPreferredSize());
        andButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                combinationMode=COMBINED_AND;
                createShape();
                centerShape();
                createGeneralPath();
                centerXField.setValue(new Double(centerXYPos.getX()) / 1000);
                centerYField.setValue(new Double(centerXYPos.getY()) / 1000);
                topLeftXField.setValue(new Double(getTopLeftX()) / 1000);
                topLeftYField.setValue(new Double(getTopLeftY()) / 1000);
                widthField.setText(NUMBER_FORMAT.format(generalPath.getBounds2D().getWidth()/ 1000));
                heightField.setText(NUMBER_FORMAT.format(generalPath.getBounds2D().getHeight()/ 1000));
                updatePreviewPanel(shapeTable,previewPanel);
            }
            
        });
        JToggleButton orButton=new JToggleButton("Combine");
        orButton.setIcon(new ImageIcon(getClass().getResource("/autoimage/resources/Combined_Or.png")));
        orButton.setPreferredSize(xorButton.getPreferredSize());
        orButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                combinationMode=COMBINED_OR;
                createShape();
                centerShape();
                createGeneralPath();
                centerXField.setValue(new Double(centerXYPos.getX()) / 1000);
                centerYField.setValue(new Double(centerXYPos.getY()) / 1000);
                topLeftXField.setValue(new Double(getTopLeftX()) / 1000);
                topLeftYField.setValue(new Double(getTopLeftY()) / 1000);
                widthField.setText(NUMBER_FORMAT.format(generalPath.getBounds2D().getWidth()/ 1000));
                heightField.setText(NUMBER_FORMAT.format(generalPath.getBounds2D().getHeight()/ 1000));
                updatePreviewPanel(shapeTable,previewPanel);
            }
            
        });
        JToggleButton notButton=new JToggleButton("Exclude");
        notButton.setIcon(new ImageIcon(getClass().getResource("/autoimage/resources/Combined_Not.png")));
        notButton.setPreferredSize(xorButton.getPreferredSize());
        notButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                combinationMode=COMBINED_NOT;
                createShape();
                centerShape();
                createGeneralPath();
                centerXField.setValue(new Double(centerXYPos.getX()) / 1000);
                centerYField.setValue(new Double(centerXYPos.getY()) / 1000);
                topLeftXField.setValue(new Double(getTopLeftX()) / 1000);
                topLeftYField.setValue(new Double(getTopLeftY()) / 1000);
                widthField.setText(NUMBER_FORMAT.format(generalPath.getBounds2D().getWidth()/ 1000));
                heightField.setText(NUMBER_FORMAT.format(generalPath.getBounds2D().getHeight()/ 1000));
                updatePreviewPanel(shapeTable,previewPanel);
            }
            
        });
        ButtonGroup modeButtonGroup= new ButtonGroup();
        modeButtonGroup.add(andButton);
        modeButtonGroup.add(orButton);
        modeButtonGroup.add(xorButton);
        modeButtonGroup.add(notButton);
        modePanel.add(label);
        modePanel.add(andButton);
        modePanel.add(orButton);
        modePanel.add(xorButton);
        modePanel.add(notButton);
        switch (combinationMode) {
            case COMBINED_AND:
                andButton.setSelected(true);
                break;
            case COMBINED_OR:
                orButton.setSelected(true);
                break;
            case COMBINED_XOR:
                xorButton.setSelected(true);
                break;
            case COMBINED_NOT:
                notButton.setSelected(true);
                break;
        }
        
        shapeTable.setDefaultRenderer(Double.class, new NumberTableCellRenderer(new DecimalFormat("###,##0.000")));
        shapeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    updatePreviewPanel(shapeTable,previewPanel);
                }
            }
        });
        shapeTable.getModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent evt) {
                if (areas!=null) { // {&& points.size() > 0) {
                    AreaTableModel model=(AreaTableModel)shapeTable.getModel();
                    createShape();
                    centerShape();
                    setRelDefaultPos();
                    createGeneralPath();
                    topLeftXField.setValue(new Double(getTopLeftX() / 1000));
                    topLeftYField.setValue(new Double(getTopLeftY() / 1000));
                    centerXField.setValue(new Double(getCenterXYPos().getX() / 1000));
                    centerYField.setValue(new Double(getCenterXYPos().getY() / 1000));
                    widthField.setText(NUMBER_FORMAT.format(generalPath.getBounds2D().getWidth() / 1000));
                    heightField.setText(NUMBER_FORMAT.format(generalPath.getBounds2D().getHeight() / 1000));
                    updatePreviewPanel(shapeTable,previewPanel);
                }
            }
        });
        
        topLeftXField.addPropertyChangeListener("value", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ((areas!=null && areas.size() > 0) && ((Double)evt.getNewValue() != (Double)evt.getOldValue())) {
                    AreaTableModel model=(AreaTableModel)shapeTable.getModel();
                    double newValue = ((Number)topLeftXField.getValue()).doubleValue()*1000;
                    if (newValue != getTopLeftX()) {
                        setTopLeftX(newValue);//recalculates center pos
                        centerXField.setValue(new Double(getCenterXYPos().getX() / 1000));
                        model.updateAllRows();
                    }
                }
            }
        });

        topLeftYField.addPropertyChangeListener("value", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ((areas!=null && areas.size() > 0) && ((Double)evt.getNewValue() != (Double)evt.getOldValue())) {
                    double newValue = ((Number)topLeftYField.getValue()).doubleValue()*1000;
                    if (newValue != getTopLeftY()) {
                        AreaTableModel model=(AreaTableModel)shapeTable.getModel();
                        setTopLeftY(newValue);//recalculates centerXYPos and generalPath
                        centerYField.setText(getCenterXYPos()==null ? "?" : NUMBER_FORMAT.format(getCenterXYPos().getY() / 1000));
                        model.updateAllRows();
                    }
                }
            }
        });

        centerXField.addPropertyChangeListener("value", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ((areas!=null && areas.size() > 0) && ((Double)evt.getNewValue() != (Double)evt.getOldValue())) {
                    double newValue = ((Number)centerXField.getValue()).doubleValue()*1000;
                    if (newValue != centerXYPos.getX()) {
                        AreaTableModel model=(AreaTableModel)shapeTable.getModel();
                        setCenter(newValue, centerXYPos.getY());//recalculates generalPath
                        topLeftXField.setValue(new Double(getTopLeftX() / 1000));
                        model.updateAllRows();
                    }
                }
            }
        });
        
        centerYField.addPropertyChangeListener("value", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ((areas!=null && areas.size() > 0) && ((Double)evt.getNewValue() != (Double)evt.getOldValue())) {
                    double newValue = ((Number)centerYField.getValue()).doubleValue()*1000;
                    if (newValue != centerXYPos.getY()) {
                        AreaTableModel model=(AreaTableModel)shapeTable.getModel();
                        setCenter(centerXYPos.getX(),newValue);//recalculates generalPath
                        topLeftYField.setText(NUMBER_FORMAT.format(getTopLeftY() / 1000));
                        model.updateAllRows();
                    }
                }
            }
        });
        
        rotationField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == rotationField) {
                    double newValue = ((Number)rotationField.getValue()).doubleValue();
                    //convert to rad
                    newValue=newValue/180*Math.PI;
                    if (newValue != Utils.getRotation(affineTrans)) {
                        AreaTableModel model=(AreaTableModel)shapeTable.getModel();
                        //using (0/0) as anchor since translation to centerXYPos is dealt with independently
                        setAffineTransform(AffineTransform.getRotateInstance(newValue,0,0));
                        topLeftXField.setValue(new Double(getTopLeftX()/1000));
                        topLeftYField.setValue(new Double(getTopLeftY()/1000));
                        model.updateAllRows();
                    }
                }
            }
        });
        
        SpringLayout springLayout = new SpringLayout();
        JPanel tablePanel=new JPanel();
        tablePanel.setLayout(springLayout);
        tablePanel.setPreferredSize(new Dimension(400,250));
        
               
        JScrollPane scrollPane=new JScrollPane(shapeTable);
        tablePanel.add(previewPanel);
        tablePanel.add(scrollPane);

        JButton newButton = new JButton(new ImageIcon(getClass().getResource("/autoimage/resources/add2.png")));
        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                
                if (availableAreaClasses==null || availableAreaClasses.size() == 0) {
                    JOptionPane.showMessageDialog(null, "No area class definitions found. New areas cannot be created.");
                    return;
                }
                String[] classDescr=new String[availableAreaClasses.size()];
                Iterator it=availableAreaClasses.keySet().iterator();
                int i=0;
                while (it.hasNext()) {
                    classDescr[i]=(String)it.next();
                    i++;
                }
                Arrays.sort(classDescr);
                String selOption;
                if ("".equals(lastAreaType)) {
                    selOption=classDescr[0];
                } else
                    selOption=lastAreaType;
                String selectedType=(String)JOptionPane.showInputDialog(null, "Area type:", "Select Area Type", JOptionPane.OK_CANCEL_OPTION, null, classDescr, selOption);
                if (selectedType!=null) {
                    Class clazz;
                    try {
                        clazz = Class.forName(availableAreaClasses.get(selectedType));
                        BasicArea newArea=((BasicArea)clazz.newInstance());
                        //initialize new area with centerXYPos of this compound shape --> user effectively will work with more intuitive absolute layout coordinate system
                        newArea.setCenter(centerXYPos.getX(),centerXYPos.getY());
                        newArea=newArea.showConfigDialog(layoutBounds);
                        if (newArea!=null) {
                            AreaTableModel model=(AreaTableModel)shapeTable.getModel();
                            //translate new area's centerXYPos to express it as relative coordinate to this compound area's centerXYPos
                            newArea.setCenter(-centerXYPos.getX(),-centerXYPos.getY());
                            newArea.setId(-1);
                            model.addRow(newArea);
                            shapeTable.changeSelection(shapeTable.getRowCount()-1,shapeTable.getRowCount(),false,false);
//                            createShape();
//                            centerShape();
//                            setRelDefaultPos();
//                            createGeneralPath();
            //                model.updateAllRows();
                            lastAreaType=selectedType;
                        }
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(LayoutManagerDlg.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InstantiationException ex) {
                        Logger.getLogger(LayoutManagerDlg.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(LayoutManagerDlg.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                
                
                
            }
        });
        newButton.setToolTipText("Add new shape");
        tablePanel.add(newButton);

        int northConstraint = 12;
        final int buttonHeight = 22;      
        springLayout.putConstraint(SpringLayout.NORTH, newButton, northConstraint, SpringLayout.NORTH, tablePanel);     
        springLayout.putConstraint(SpringLayout.SOUTH, newButton, northConstraint+=buttonHeight, SpringLayout.NORTH, tablePanel);
        springLayout.putConstraint(SpringLayout.EAST, newButton, -0, SpringLayout.EAST, tablePanel);
        springLayout.putConstraint(SpringLayout.WEST, newButton, -22, SpringLayout.EAST, tablePanel);

        springLayout.putConstraint(SpringLayout.NORTH, previewPanel, 12, SpringLayout.NORTH, tablePanel);     
        springLayout.putConstraint(SpringLayout.SOUTH, previewPanel, 0, SpringLayout.SOUTH, tablePanel);
        springLayout.putConstraint(SpringLayout.EAST, previewPanel, tablePanel.getPreferredSize().height-12, SpringLayout.WEST, tablePanel);
        springLayout.putConstraint(SpringLayout.WEST, previewPanel, 0, SpringLayout.WEST, tablePanel);

        springLayout.putConstraint(SpringLayout.NORTH, scrollPane, 0, SpringLayout.NORTH, newButton);
        springLayout.putConstraint(SpringLayout.SOUTH, scrollPane, 0, SpringLayout.SOUTH, tablePanel);
        springLayout.putConstraint(SpringLayout.EAST, scrollPane, -6, SpringLayout.WEST, newButton);
        springLayout.putConstraint(SpringLayout.WEST, scrollPane, 6, SpringLayout.EAST, previewPanel);

        final JButton removeButton = new JButton(new ImageIcon(getClass().getResource("/autoimage/resources/delete.png")));
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                int selRowCount=shapeTable.getSelectedRowCount();
                if (selRowCount > 0) {
                    int[] selRows=shapeTable.getSelectedRows();
                    int[] rowsInModel=new int[selRows.length];
                    for (int i=0; i<selRows.length; i++){
                        rowsInModel[i]=shapeTable.convertRowIndexToModel(selRows[i]);
                    }
                    AreaTableModel model=(AreaTableModel)shapeTable.getModel();
                    model.removeRows(rowsInModel);
//                    updatePreviewPanel(shapeTable, previewPanel);
                } else {
                    JOptionPane.showMessageDialog(null, "Select at least one shape.");
                }
            }
        });
        removeButton.setToolTipText("Remove selected shapes");
        tablePanel.add(removeButton);
        springLayout.putConstraint(SpringLayout.NORTH, removeButton, northConstraint+=3, SpringLayout.NORTH, tablePanel);
        springLayout.putConstraint(SpringLayout.SOUTH, removeButton, northConstraint+=buttonHeight, SpringLayout.NORTH, tablePanel);
        springLayout.putConstraint(SpringLayout.EAST, removeButton, 0, SpringLayout.EAST, newButton);
        springLayout.putConstraint(SpringLayout.WEST, removeButton, 0, SpringLayout.WEST, newButton);
      
        JButton editButton = new JButton(new ImageIcon(getClass().getResource("/autoimage/resources/wrench_orange.png")));
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (shapeTable.getSelectedRowCount()!=1) {
                    JOptionPane.showMessageDialog(null, "Select a single shape for editing.");
                    return;
                }
                int index=shapeTable.getSelectedRow();
                AreaTableModel model=(AreaTableModel)shapeTable.getModel();
                int rowInModel=shapeTable.convertRowIndexToModel(index);
                BasicArea area=model.getRowData(rowInModel);
                try {
                    BasicArea areaCopy=BasicArea.createFromJSONObject(area.toJSONObject());
                    //pass copy, so area can remain unchanged if user clicks 'Cancel'
                    //translate area with centerXYPos vector of this compound shape --> user effectively will work with more intuitive absolute layout coordinate system
                    Point2D absCenter=new Point2D.Double();
                    getShapeToPathTransform().transform(areaCopy.getCenterXYPos(), absCenter);
                    areaCopy.setCenter(absCenter.getX(),absCenter.getY());
                    AffineTransform cat=new AffineTransform(areaCopy.getAffineTransform());
                    cat.concatenate(affineTrans);
                    areaCopy.setAffineTransform(cat);
                    BasicArea modArea=areaCopy.showConfigDialog(layoutBounds);
                    if (modArea!=null) {// user clicked OK in dialog
                        //translate new area's centerXYPos to express it as relative coordinate to this compound area's centerXYPos
                        Point2D relCenter=new Point2D.Double();
                        try {
                            getPathToShapeTransform().transform(modArea.getCenterXYPos(),relCenter);
                            modArea.setCenter(relCenter.getX(), relCenter.getY());
                        } catch (NoninvertibleTransformException ex) {
                            Logger.getLogger(CompoundArea.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        cat=modArea.getAffineTransform();
                        try {
                            cat.concatenate(affineTrans.createInverse());
                        } catch (NoninvertibleTransformException ex) {
                            Logger.getLogger(CompoundArea.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        modArea.setAffineTransform(cat);
                        model.setRowData(rowInModel,modArea);
                    }            
                } catch (JSONException ex) {
                    Logger.getLogger(LayoutManagerDlg.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(LayoutManagerDlg.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InstantiationException ex) {
                    Logger.getLogger(LayoutManagerDlg.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(LayoutManagerDlg.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        editButton.setToolTipText("Edit shape");
        tablePanel.add(editButton);
        springLayout.putConstraint(SpringLayout.NORTH, editButton, northConstraint+=3, SpringLayout.NORTH, tablePanel);
        springLayout.putConstraint(SpringLayout.SOUTH, editButton, northConstraint+=buttonHeight, SpringLayout.NORTH, tablePanel);
        springLayout.putConstraint(SpringLayout.EAST, editButton, 0, SpringLayout.EAST, newButton);
        springLayout.putConstraint(SpringLayout.WEST, editButton, 0, SpringLayout.WEST, newButton);      

        final JButton moveUpButton = new JButton(new ImageIcon(getClass().getResource("/autoimage/resources/Up.png")));
        moveUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selRows=shapeTable.getSelectedRows();
                if (selRows.length==0) {
                    JOptionPane.showMessageDialog(null, "Select one or more shapes.");
                    return;
                }
                if (selRows.length > 0 & selRows[0] > 0) {
//                    int[] rowsInModel=new int[selRows.length];                
                    int newSelRowInView=selRows[0]-1;
                    //
                    int firstMinusOneIndex=shapeTable.convertRowIndexToModel(selRows[0]-1);
                    //convert view row indices retrieved from table to corresponding indices in model
                    for (int i=0; i<selRows.length; i++) {
                        selRows[i]=shapeTable.convertRowIndexToModel(selRows[i]);
                    }
                    AreaTableModel model=(AreaTableModel)shapeTable.getModel();
                    model.rowsUp(selRows, firstMinusOneIndex);
                    shapeTable.setRowSelectionInterval(newSelRowInView, newSelRowInView + selRows.length - 1);
                } 
            }
        });
        moveUpButton.setToolTipText("Move selected shapes up in list");
        tablePanel.add(moveUpButton);
        springLayout.putConstraint(SpringLayout.NORTH, moveUpButton, northConstraint+=3, SpringLayout.NORTH, tablePanel);
        springLayout.putConstraint(SpringLayout.SOUTH, moveUpButton, northConstraint+=buttonHeight, SpringLayout.NORTH, tablePanel);
        springLayout.putConstraint(SpringLayout.EAST, moveUpButton, 0, SpringLayout.EAST, newButton);
        springLayout.putConstraint(SpringLayout.WEST, moveUpButton, 0, SpringLayout.WEST, newButton);      
              
        final JButton moveDownButton = new JButton(new ImageIcon(getClass().getResource("/autoimage/resources/Down.png")));
        moveDownButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selRows = shapeTable.getSelectedRows();
                if (selRows.length==0) {
                    JOptionPane.showMessageDialog(null, "Select one or more shapes.");
                    return;
                }
                if (selRows.length > 0 & selRows[selRows.length - 1] < shapeTable.getRowCount()) {
//                    int[] rowsInModel=new int[selRows.length];                
                    int newSelRowInView=selRows[0]+1;
                    //
                    int lastPlusOneIndex=shapeTable.convertRowIndexToModel(selRows[selRows.length-1]+1);
                    //convert view row indices retrieved from table to corresponding indices in model
                    for (int i=0; i<selRows.length; i++) {
                        selRows[i]=shapeTable.convertRowIndexToModel(selRows[i]);
                    }
                    AreaTableModel model=(AreaTableModel)shapeTable.getModel();
                    model.rowsDown(selRows, lastPlusOneIndex);
                    shapeTable.setRowSelectionInterval(newSelRowInView, newSelRowInView + selRows.length - 1);
                }
            }
        });
        moveDownButton.setToolTipText("Move selected shapes down in list");
        tablePanel.add(moveDownButton);
        springLayout.putConstraint(SpringLayout.NORTH, moveDownButton, northConstraint+=3, SpringLayout.NORTH, tablePanel);
        springLayout.putConstraint(SpringLayout.SOUTH, moveDownButton, northConstraint+=buttonHeight, SpringLayout.NORTH, tablePanel);
        springLayout.putConstraint(SpringLayout.EAST, moveDownButton, 0, SpringLayout.EAST, newButton);
        springLayout.putConstraint(SpringLayout.WEST, moveDownButton, 0, SpringLayout.WEST, newButton);      
              
        JPanel combinedPanel = new JPanel();
        combinedPanel.setLayout(new BoxLayout(combinedPanel, BoxLayout.PAGE_AXIS));
        combinedPanel.add(optionPanel);
        combinedPanel.add(Box.createRigidArea(new Dimension(0,15)));
        combinedPanel.add(Box.createRigidArea(new Dimension(0,5)));
        combinedPanel.add(modePanel);
        combinedPanel.add(Box.createRigidArea(new Dimension(0,5)));
        combinedPanel.add(tablePanel);
        
        shapeTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        int result;
        boolean invalidParams;
        do {
            invalidParams=false;
            result = JOptionPane.showConfirmDialog(null, combinedPanel, 
                getShapeType()+": Configuration", JOptionPane.OK_CANCEL_OPTION);
            createShape();
            centerShape();
            createGeneralPath();
            setRelDefaultPos();
            if (result == JOptionPane.OK_OPTION && areas.size() < 2) {
                JOptionPane.showMessageDialog(null,"At least two shapes need to be be defined.");
                invalidParams=true;
            } else if (result == JOptionPane.OK_OPTION && !isInsideRect(layoutBounds)) {
                JOptionPane.showMessageDialog(null,"The area does not fit into the layout.");
                invalidParams=true;
            }
        } while (result == JOptionPane.OK_OPTION && invalidParams);
        if (result == JOptionPane.CANCEL_OPTION) {
            return null;
        } else {
            setName((String)nameField.getValue());
            //center pos will be set automatically
            setRelativeZPos(((Number)zField.getValue()).doubleValue() * 1000);
            return this;
        }            
    }

    @Override
    public int supportedLayouts() {
        return BasicArea.SUPPORT_CUSTOM_LAYOUT;
    }

    @Override
    protected void createShape() {
        if (areas==null || areas.isEmpty()) {
            shape=new Path2D.Double();
            return;
        }
        java.awt.geom.Area combinedArea = null; 
        for (BasicArea area:areas) {
            if (combinedArea==null) {
                combinedArea=new java.awt.geom.Area(area);
            } else {
                switch (combinationMode) {
                    case COMBINED_OR: 
                        combinedArea.add(new java.awt.geom.Area(area));
                        break;
                    case COMBINED_AND: 
                        combinedArea.intersect(new java.awt.geom.Area(area));
                        break;
                    case COMBINED_NOT: 
                        combinedArea.subtract(new java.awt.geom.Area(area));
                        break;
                    case COMBINED_XOR: 
                        combinedArea.exclusiveOr(new java.awt.geom.Area(area));
                        break;
                }        
            }
        }
        shape=new Path2D.Double(combinedArea);
    }

    @Override
    public double getShapeBoundsDiagonale() {
        if (areas==null) {
            return 0;
        }
        Path2D p=new Path2D.Double();
        for (BasicArea a:areas) {
            p.append(a, false);
        }
        Point2D point=new Point2D.Double(0,0);
        return point.distance(new Point2D.Double(p.getBounds2D().getWidth(), p.getBounds2D().getHeight()));
    }
    
    @Override
    protected void setRelDefaultPos() {
        relDefaultXYPos=new Point2D.Double(0,0);
    }

}
