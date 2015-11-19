package autoimage.area;

import autoimage.api.BasicArea;
import autoimage.Tile;
import autoimage.Utils;
import autoimage.gui.NumberTableCellRenderer;
import autoimage.gui.AreaPreviewPanel;
//import ij.IJ;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
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
 * @author Karsten
 */
public class PolygonArea extends BasicArea {

    private List<Point2D> points;
    
    protected final static String TAG_POINT_ARRAY = "POINT_ARRAY";
    protected final static String TAG_COORD_X = "COORD_X";
    protected final static String TAG_COORD_Y = "COORD_Y";
    protected final static String TAG_NUMBER = "NUMBER";
    
    private final static DecimalFormat NUMBER_FORMAT = new DecimalFormat("###,###,##0.000");

    public void setPoints(List<Point2D> list) {
        points=list;
        this.createShape();
        this.setRelDefaultPos();
        this.createGeneralPath();
    }

    private class PointTableModel extends AbstractTableModel {

        private List<Point2D> pointList;//vertices that define polygon centered at origin (0/0)
        private PolygonArea area;
        private final String[] columnHeader=new String[]{"X (mm)", "Y (mm)"};
        private boolean isAbsolute;
        
        public PointTableModel(PolygonArea area, List<Point2D> list) {
            super();
            this.area=area;
            isAbsolute=true;
            setData(list,false);
        }

        public void enableAbsolute(boolean b) {
            if (b!=isAbsolute) {
                isAbsolute=b;
                updateAllRows();;
            }    
        }
        
        public void setData(List<Point2D> pl, boolean updateView) {
            if (pl == null) {
                pl = new ArrayList<Point2D>();
            }
            pointList = pl;
            if (updateView) {
                fireTableDataChanged();
            }
        }
        
        @Override
        public int getRowCount() {
            return pointList.size();
        }

        @Override
        public int getColumnCount() {
            return columnHeader.length;
        }

        @Override
        public String getColumnName(int colIdx) {
            return columnHeader[colIdx];
        }

        @Override
        public Class getColumnClass(int colIdx) {
            return getValueAt(0, colIdx).getClass();
        }

                
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex <0 || rowIndex >= pointList.size())
                return null;
            Point2D rel=pointList.get(rowIndex);
            Point2D p=new Point2D.Double(rel.getX(),rel.getY());
            if (isAbsolute) {
                AffineTransform at;
                at = area.getShapeToPathTransform();
                at.transform(rel, p);
            }
            switch (columnIndex) {
                case 0: {
                            return p.getX() / 1000;
                        }
                case 1: {
                            return p.getY() / 1000;
                        }
                default: return null;    
            }
        }
            
        @Override
        public boolean isCellEditable(int rowIndex, int colIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int colIndex) {
            Point2D point=null;
            if (pointList != null & rowIndex < pointList.size()) {
//                point = pointList.get(rowIndex);
                switch (colIndex) {
                    case 0: {
                        point=new Point2D.Double((Double)value * 1000,(Double)getValueAt(rowIndex,1)*1000);
                        break;
                    }
                    case 1: {
                        point=new Point2D.Double((Double)getValueAt(rowIndex,0)*1000,(Double)value * 1000);
                        break;
                    }
                }
                if (!isAbsolute) {
                    pointList.get(rowIndex).setLocation(point);
                } else {
                    try {
                        Point2D rel=point;
                        AffineTransform at;
                        at = area.getPathToShapeTransform();
                        at.transform(point, rel);
                        pointList.get(rowIndex).setLocation(rel);
                    } catch (NoninvertibleTransformException ex) {
                        Logger.getLogger(PolygonArea.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                fireTableCellUpdated(rowIndex, colIndex);
            }
        }

        private void insertRow(int rowIdx, Point2D point) {
            points.add(rowIdx, point);
            fireTableRowsInserted(rowIdx,rowIdx);
        }

        private void addRow(Point2D point) {
            points.add(point);
            fireTableRowsInserted(points.size()-1,points.size()-1);
        }

        private void removeRows(int[] selRows) {
            //sort and remove from bottom to top
            Arrays.sort(selRows);
            for (int i=selRows.length-1; i>=0; i--) {
                points.remove(selRows[i]);
                fireTableRowsDeleted(selRows[i],selRows[i]);
            }
        }
        
        /*
        @Param rowIdx: array of indices in model
        @Param firstMinusOneIndex: model rowindex that corresponds to 1 above selection in view
        */
        public void rowsUp(int[] rowIdx, int firstMinusOneIndex) {
            Point2D temp=points.get(firstMinusOneIndex);
            //move first entry in selection one row up
            points.set(firstMinusOneIndex, points.get(rowIdx[0]));
            //iterate for the rest
            for (int i=0; i<rowIdx.length-1; i++) {
                points.set(rowIdx[i], points.get(rowIdx[i+1]));
            }
            points.set(rowIdx[rowIdx.length-1],temp);
            fireTableRowsUpdated(0,getRowCount()-1);
        }
        
        /*
        @Param rowIdx: array of indices in model
        @Param lastPlusOneIndex: model rowindex that corresponds to 1 below selection in view
        */
        public void rowsDown(int[] rowIdx, int lastPlusOneIndex) {
            Point2D temp=points.get(lastPlusOneIndex);
            //move last entry in selection one row down 
            points.set(lastPlusOneIndex, points.get(rowIdx[rowIdx.length-1]));
            //iterate for the rest
            for (int i=rowIdx.length-1; i>0; i--) {
                points.set(rowIdx[i], points.get(rowIdx[i-1]));
            }
            points.set(rowIdx[0],temp);
            
            fireTableRowsUpdated(0,getRowCount()-1);
        }

        private void updateAllRows() {
            this.fireTableRowsUpdated(0, this.getRowCount()-1);
//            fireTableDataChanged();
        }

        private boolean isAbsolute() {
            return isAbsolute;
        }
    
    }
    
    
    
    public PolygonArea() {
        super();
        points=new ArrayList<Point2D>();
        createShape();
        setRelDefaultPos();
        createGeneralPath();
    }
    
    public PolygonArea(String n) { //expects name identifier
        super(n);
        points=new ArrayList<Point2D>();
        createShape();
        setRelDefaultPos();
        createGeneralPath();
    }
    
    /**
     * 
     * @param n
     * @param id
     * @param ox
     * @param oy
     * @param oz
     * @param pList: vertex points centered around (0/0)
     * @param s
     * @param anot 
     */
    public PolygonArea(String n, int id, double ox, double oy, double oz, List<Point2D> pList, boolean s, String anot) {
        super(n,id,ox,oy,oz,s,anot);
//        createArea(pList, ox, oy);
        points=pList;
        createShape();
        setRelDefaultPos();
        createGeneralPath();
    }

/*
    private double area(List<Point2D> pList) {
        if (pList != null) {
            int i, j, n = pList.size();
            double area = 0;

            for (i = 0; i < n; i++) {
                j = (i + 1) % n;
                area += pList.get(i).getX() * pList.get(j).getY();
                area -= pList.get(j).getX() * pList.get(i).getY();
            }
            area /= 2.0;
            return area;
        } else {
            return 0;
        }
    }
    
    public Point2D centerOfMass(List<Point2D> pList) {
        //no vertex points
        if (pList==null || pList.size()==0) 
            return null;
        //1 vertex point
        if (pList.size()==1) {
            return new Point2D.Double(pList.get(0).getX(),pList.get(0).getY());
        }
        //2 vertex points
        if (pList.size()==2) {
            return new Point2D.Double((pList.get(1).getX()+pList.get(0).getX())/2,(pList.get(1).getY()+pList.get(0).getY())/2);
        }
        //multiple vertex points
        double cx = 0;
        double cy = 0;
        double area = area(pList);
        int i;
        int j;
        int n = pList.size();

        double factor = 0;
        for (i = 0; i < n; i++) {
            j = (i + 1) % n;
            factor = (pList.get(i).getX() * pList.get(j).getY()
                            - pList.get(j).getX() * pList.get(i).getY());
            cx += (pList.get(i).getX() + pList.get(j).getX()) * factor;
            cy += (pList.get(i).getY() + pList.get(j).getY()) * factor;
        }
        area *= 6.0d;
        factor = 1 / area;
        cx *= factor;
        cy *= factor;
        return new Point2D.Double(cx,cy);
    }
*/    

       
    @Override
    protected void initializeFromJSONObject(JSONObject obj) throws JSONException {
        JSONArray listarray=obj.getJSONArray(TAG_POINT_ARRAY);
        points = new ArrayList<Point2D>();
        if (listarray!=null) {
            for (int i=0; i<listarray.length(); i++) {
                points.add(new Point2D.Double(0,0));
            }
            for (int i=0; i<listarray.length(); i++) {
                JSONObject pointObj=listarray.getJSONObject(i);
                int number=pointObj.getInt(TAG_NUMBER);
                double x=pointObj.getDouble(TAG_COORD_X);
                double y=pointObj.getDouble(TAG_COORD_Y);
                points.set(number,new Point2D.Double(x,y));
            }
        }       
    }

    @Override
    protected void addFieldsToJSONObject(JSONObject obj) throws JSONException {
        JSONArray listarray=new JSONArray();
        if (points!=null) {    
            int i=0;
            for (Point2D point:points) {
                JSONObject pointObj=new JSONObject();
                pointObj.put(TAG_NUMBER, i);
                pointObj.put(TAG_COORD_X, point.getX());
                pointObj.put(TAG_COORD_Y, point.getY());
                listarray.put(pointObj);
                i++;
            }
        }    
        obj.put(TAG_POINT_ARRAY, listarray);
    }

    @Override
    public String getShapeType() {
        return "Polygon";
    }

    @Override
    public BasicArea duplicate() {
        PolygonArea newArea = new PolygonArea(this.getName());
        newArea.setId(this.getId());
        newArea.affineTrans=new AffineTransform(this.affineTrans);
        newArea.setSelectedForAcq(isSelectedForAcq());
        newArea.setSelectedForMerge(isSelectedForMerge());
        newArea.setComment(this.comment);
        newArea.setAcquiring(this.acquiring);
//        newArea.setTilingSetting(this.tiling.duplicate());
        newArea.tilePosList=new ArrayList<Tile>(this.getTilePositions());
        newArea.setUnknownTileNum(this.hasUnknownTileNum());
        newArea.createShape();
        newArea.setRelDefaultPos();
        newArea.createGeneralPath();
        return newArea;
    }

    private void updatePreviewPanel(JTable vertexTable, AreaPreviewPanel previewPanel) {
        if (((PointTableModel)vertexTable.getModel()).isAbsolute()) {
            previewPanel.setPath(generalPath, centerXYPos, getShapeBoundsDiagonale());
        } else {
            previewPanel.setPath(shape, getShapeBoundsDiagonale());
        }
        int[] rows=vertexTable.getSelectedRows();
        if (rows!=null) {
            for (int row:rows) {
                row=Math.min(vertexTable.convertRowIndexToModel(row), vertexTable.getRowCount()-1);
                Point2D selVertex=points.get(vertexTable.convertRowIndexToModel(row));
                double radius=getShapeBoundsDiagonale()/30;
                Path2D selPath=new Path2D.Double(new Ellipse2D.Double(
                        selVertex.getX()-radius/2,
                        selVertex.getY()-radius/2,
                        radius,
                        radius));
                if (((PointTableModel)vertexTable.getModel()).isAbsolute()) {
/*                    AffineTransform at=AffineTransform.getTranslateInstance(getCenterXYPos().getX(), getCenterXYPos().getY());
                    at.concatenate(affineTrans);
                    selPath.transform(at);*/
                    selPath.transform(this.getShapeToPathTransform());
                }
                previewPanel.addPath(selPath,Color.RED,Color.RED);
            }
        }    
        previewPanel.repaint();
    }
    
    
    @Override
    public BasicArea showConfigDialog(Rectangle2D layoutBounds) {
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
        
        final JTable pointTable = new JTable(new PointTableModel(this,points));
        pointTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        
        final AreaPreviewPanel previewPanel = new AreaPreviewPanel(generalPath, getShapeBoundsDiagonale());
        previewPanel.setPreferredSize(new Dimension (160,160));

        final JRadioButton absoluteButton = new JRadioButton("Absolute");
        absoluteButton.setActionCommand("absolute");
        absoluteButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                PointTableModel model=(PointTableModel)pointTable.getModel();
                model.enableAbsolute(true);
                updatePreviewPanel(pointTable,previewPanel);
            }
        });

        final JRadioButton relativeButton = new JRadioButton("Relative to Area Center");
        relativeButton.setActionCommand("relative");
        relativeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                PointTableModel model=(PointTableModel)pointTable.getModel();
                model.enableAbsolute(false);
                updatePreviewPanel(pointTable,previewPanel);
            }
        });


        //Group the radio buttons.
        ButtonGroup group = new ButtonGroup();
        group.add(absoluteButton);
        group.add(relativeButton);
        
        JPanel buttonPanel = new JPanel();
        JLabel label = new JLabel("Point list:");
        buttonPanel.add(label);
        buttonPanel.add(absoluteButton);
        buttonPanel.add(relativeButton);
        
        absoluteButton.setSelected(true);
        
        pointTable.setDefaultRenderer(Double.class, new NumberTableCellRenderer(new DecimalFormat("###,##0.000")));        
        pointTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    updatePreviewPanel(pointTable, previewPanel);
                }
            }
            
        });
        pointTable.getModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent evt) {
                if (points!=null) { // {&& points.size() > 0) {
                    PointTableModel model=(PointTableModel)pointTable.getModel();
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
                    updatePreviewPanel(pointTable,previewPanel);
                }
            }
        });
        
        topLeftXField.addPropertyChangeListener("value", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ((points!=null && points.size() > 0) && ((Double)evt.getNewValue() != (Double)evt.getOldValue())) {
                    PointTableModel model=(PointTableModel)pointTable.getModel();
//                    double dx=((Double)evt.getNewValue() * 1000)-getTopLeftX();
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
                if ((points!=null && points.size() > 0) && ((Double)evt.getNewValue() != (Double)evt.getOldValue())) {
                    double newValue = ((Number)topLeftYField.getValue()).doubleValue()*1000;
                    if (newValue != getTopLeftY()) {
                        PointTableModel model=(PointTableModel)pointTable.getModel();
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
                if ((points!=null && points.size() > 0) && ((Double)evt.getNewValue() != (Double)evt.getOldValue())) {
                    double newValue = ((Number)centerXField.getValue()).doubleValue()*1000;
                    if (newValue != centerXYPos.getX()) {
                        PointTableModel model=(PointTableModel)pointTable.getModel();
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
                if ((points!=null && points.size() > 0) && ((Double)evt.getNewValue() != (Double)evt.getOldValue())) {
                    double newValue = ((Number)centerYField.getValue()).doubleValue()*1000;
                    if (newValue != centerXYPos.getY()) {
                        PointTableModel model=(PointTableModel)pointTable.getModel();
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
                        PointTableModel model=(PointTableModel)pointTable.getModel();
                        //using (0/0) as anchor since translation to centerXYPos is dealt with independently
                        setAffineTransform(AffineTransform.getRotateInstance(newValue,0,0));
                        topLeftXField.setValue(new Double(getTopLeftX()/1000));
                        topLeftYField.setValue(new Double(getTopLeftY()/1000));
                        model.updateAllRows();
                        if (absoluteButton.isSelected()) {
                            updatePreviewPanel(pointTable,previewPanel);
                        }
                    }
                }
            }
        });
        
        SpringLayout springLayout = new SpringLayout();
        JPanel tablePanel=new JPanel();
        tablePanel.setLayout(springLayout);
        tablePanel.setPreferredSize(new Dimension(400,250));
        
               
        JScrollPane scrollPane=new JScrollPane(pointTable);
        tablePanel.add(previewPanel);
        tablePanel.add(scrollPane);

        JButton newButton = new JButton(new ImageIcon(getClass().getResource("/autoimage/resources/add2.png")));
        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                Point2D point=new Point2D.Double(
                        shape.getBounds2D().getMinX(), 
                        shape.getBounds2D().getMinY());
                PointTableModel model=(PointTableModel)pointTable.getModel();
                model.addRow(point);
                pointTable.getSelectionModel().setSelectionInterval(pointTable.getRowCount()-1,pointTable.getRowCount()-1);
            }
        });
        newButton.setToolTipText("Create new vertex point");
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
                int selRowCount=pointTable.getSelectedRowCount();
                if (selRowCount > 0) {
                    int[] selRows=pointTable.getSelectedRows();
                    int[] rowsInModel=new int[selRows.length];
                    for (int i=0; i<selRows.length; i++){
                        rowsInModel[i]=pointTable.convertRowIndexToModel(selRows[i]);
                    }
                    PointTableModel model=(PointTableModel)pointTable.getModel();
                    model.removeRows(rowsInModel);
                    if (pointTable.getRowCount()>0) {
                        int row=Math.min(pointTable.getRowCount()-1,selRows[0]);
                        pointTable.getSelectionModel().setSelectionInterval(row,row);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Select at least one vertex point.");
                }
            }
        });
        removeButton.setToolTipText("Remove selected vertex points");
        tablePanel.add(removeButton);
        springLayout.putConstraint(SpringLayout.NORTH, removeButton, northConstraint+=3, SpringLayout.NORTH, tablePanel);
        springLayout.putConstraint(SpringLayout.SOUTH, removeButton, northConstraint+=buttonHeight, SpringLayout.NORTH, tablePanel);
        springLayout.putConstraint(SpringLayout.EAST, removeButton, 0, SpringLayout.EAST, newButton);
        springLayout.putConstraint(SpringLayout.WEST, removeButton, 0, SpringLayout.WEST, newButton);
      
        final JButton moveUpButton = new JButton(new ImageIcon(getClass().getResource("/autoimage/resources/Up.png")));
        moveUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selRows=pointTable.getSelectedRows();
                if (selRows.length==0) {
                    JOptionPane.showMessageDialog(null, "Select at least one vertex point.");
                    return;
                }
                if (selRows.length > 0 & selRows[0] > 0) {
//                    int[] rowsInModel=new int[selRows.length];                
                    int newSelRowInView=selRows[0]-1;
                    //
                    int firstMinusOneIndex=pointTable.convertRowIndexToModel(selRows[0]-1);
                    //convert view row indices retrieved from table to corresponding indices in model
                    for (int i=0; i<selRows.length; i++) {
                        selRows[i]=pointTable.convertRowIndexToModel(selRows[i]);
                    }
                    PointTableModel model=(PointTableModel)pointTable.getModel();
                    model.rowsUp(selRows, firstMinusOneIndex);
                    pointTable.setRowSelectionInterval(newSelRowInView, newSelRowInView + selRows.length - 1);
                } 
            }
        });
        moveUpButton.setToolTipText("Move selected vertex points up in list");
        tablePanel.add(moveUpButton);
        springLayout.putConstraint(SpringLayout.NORTH, moveUpButton, northConstraint+=3, SpringLayout.NORTH, tablePanel);
        springLayout.putConstraint(SpringLayout.SOUTH, moveUpButton, northConstraint+=buttonHeight, SpringLayout.NORTH, tablePanel);
        springLayout.putConstraint(SpringLayout.EAST, moveUpButton, 0, SpringLayout.EAST, newButton);
        springLayout.putConstraint(SpringLayout.WEST, moveUpButton, 0, SpringLayout.WEST, newButton);      
              
        final JButton moveDownButton = new JButton(new ImageIcon(getClass().getResource("/autoimage/resources/Down.png")));
        moveDownButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selRows = pointTable.getSelectedRows();
                if (selRows.length==0) {
                    JOptionPane.showMessageDialog(null, "Select at least one vertex point.");
                    return;
                }
                if (selRows.length > 0 & selRows[selRows.length - 1] < pointTable.getRowCount()) {
//                    int[] rowsInModel=new int[selRows.length];                
                    int newSelRowInView=selRows[0]+1;
                    //
                    int lastPlusOneIndex=pointTable.convertRowIndexToModel(selRows[selRows.length-1]+1);
                    //convert view row indices retrieved from table to corresponding indices in model
                    for (int i=0; i<selRows.length; i++) {
                        selRows[i]=pointTable.convertRowIndexToModel(selRows[i]);
                    }
                    PointTableModel model=(PointTableModel)pointTable.getModel();
                    model.rowsDown(selRows, lastPlusOneIndex);
                    pointTable.setRowSelectionInterval(newSelRowInView, newSelRowInView + selRows.length - 1);
                }
            }
        });
        moveDownButton.setToolTipText("Move selected vertex points down in list");
        tablePanel.add(moveDownButton);
        springLayout.putConstraint(SpringLayout.NORTH, moveDownButton, northConstraint+=3, SpringLayout.NORTH, tablePanel);
        springLayout.putConstraint(SpringLayout.SOUTH, moveDownButton, northConstraint+=buttonHeight, SpringLayout.NORTH, tablePanel);
        springLayout.putConstraint(SpringLayout.EAST, moveDownButton, 0, SpringLayout.EAST, newButton);
        springLayout.putConstraint(SpringLayout.WEST, moveDownButton, 0, SpringLayout.WEST, newButton);      
              
        JPanel combinedPanel = new JPanel();
        combinedPanel.setLayout(new BoxLayout(combinedPanel, BoxLayout.PAGE_AXIS));
        combinedPanel.add(optionPanel);
        combinedPanel.add(Box.createRigidArea(new Dimension(0,15)));
        combinedPanel.add(buttonPanel);
        combinedPanel.add(Box.createRigidArea(new Dimension(0,5)));
        combinedPanel.add(tablePanel);
        
        pointTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        int result;
        boolean invalidParams;
        do {
            invalidParams=false;
            result = JOptionPane.showConfirmDialog(
                    null, 
                    combinedPanel, 
                    getShapeType()+": Configuration", 
                    JOptionPane.OK_CANCEL_OPTION);
            createShape();
            centerShape();
            createGeneralPath();
            setRelDefaultPos();
            if (result == JOptionPane.OK_OPTION && points.size() < 3) {
                JOptionPane.showMessageDialog(null,"At least three vertex points need to be defined.");
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

    /**
     * 
     * @param pList List of Point2D containing x/y coordinates for all vertices. Connection order between vertices is given by order of objects in pList
     * @return Path2D object describing the polygon
     */
    private Path2D createPolygonFromPoints(List<Point2D> pList) {
        if (pList==null || pList.isEmpty()) {
            return new Path2D.Double();
        } else {    
            Path2D poly=new Path2D.Double();
            poly.moveTo(pList.get(0).getX(), pList.get(0).getY());
            if (pList.size()<=1) {
            }
            if (pList.size()>1) {
                for (int i=1; i< pList.size(); i++) {
                    poly.lineTo(pList.get(i).getX(), pList.get(i).getY());
                }
                poly.closePath();
            }
            return poly;
        }
    }
    
    protected void centerShape() {
        if (shape==null || points==null) {
            return;
        }
        double cx=shape.getBounds2D().getCenterX();
        double cy=shape.getBounds2D().getCenterY();
        //if properly centered, cx and cy are 0,
        //if not we center of shape needs to be translated to origin
        AffineTransform at=new AffineTransform();
        at.translate(-cx, -cy);
        shape.transform(at);
        //update vertex points
        for (Point2D p:points) {
            p.setLocation(p.getX()-cx, p.getY()-cy);
        }
        //need to update centerXYPos to ensure that the generalPath positioning remains unchanged
        Point2D centerOffset = new Point2D.Double(-cx,-cy);// shape translation vector
        //apply the affineTrans to shape shape translation vector
        affineTrans.transform(centerOffset, centerOffset);
        //apply inverted adjusted translation vector
        centerXYPos=new Point2D.Double(centerXYPos.getX()-centerOffset.getX(),centerXYPos.getY()-centerOffset.getY());
    }
    /**
     * Points are expected to be relative coordinates defining this.shape centered around origin (0/0)
     */
    @Override
    protected void createShape() {
        if (points!=null && !points.isEmpty()) {
            //create polygon
            shape=createPolygonFromPoints(points);
            //just to make sure, recenter shape and adjust centerXYPos accordingly
//            Point2D centerOffset=centerPath(shape);
//            setCenter(centerXYPos.getX()-centerOffset.getX(),centerXYPos.getY()-centerOffset.getY());        
        } else {
            shape=new Path2D.Double();
        }
    }  
    
    @Override
    public int supportedLayouts() {
        return BasicArea.SUPPORT_CUSTOM_LAYOUT;
    }
        
}
