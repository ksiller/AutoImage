package autoimage.area;

import autoimage.api.SampleArea;
import autoimage.Tile;
import autoimage.api.TilingSetting;
import autoimage.gui.NumberTableCellRenderer;
import ij.IJ;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
public class PolygonArea extends SampleArea {

    private List<Point2D> points;
//    private Path2D polygon;
    
    protected final static String TAG_POINT_ARRAY = "POINT_ARRAY";
    protected final static String TAG_COORD_X = "COORD_X";
    protected final static String TAG_COORD_Y = "COORD_Y";
    protected final static String TAG_NUMBER = "NUMBER";
    
    private final static DecimalFormat NUMBER_FORMAT = new DecimalFormat("###,###,##0.000");

    private class PointTableModel extends AbstractTableModel {

        private List<Point2D> pointList;
        private final String[] columnHeader=new String[]{"X (mm)", "Y (mm)"};
        
        public PointTableModel(List<Point2D> list) {
            super();
            setData(list,false);
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
            switch (columnIndex) {
                case 0: {
                            return pointList.get(rowIndex).getX() / 1000;
                        }
                case 1: {
                            return pointList.get(rowIndex).getY() / 1000;
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
            Point2D point;
            if (pointList != null & rowIndex < pointList.size()) {
                point = pointList.get(rowIndex);
                switch (colIndex) {
                    case 0: {
                        point.setLocation((Double)value * 1000,point.getY());
                        fireTableCellUpdated(rowIndex, colIndex);
                        break;
                    }
                    case 1: {
                        point.setLocation(point.getX(),(Double)value * 1000);
                        fireTableCellUpdated(rowIndex, colIndex);
                        break;
                    }
                }
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
            //move last entry in selection oen row up
            points.set(firstMinusOneIndex, points.get(rowIdx[0]));
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
            for (int i=rowIdx.length-1; i>0; i--) {
                points.set(rowIdx[i], points.get(rowIdx[i-1]));
            }
            points.set(rowIdx[0],temp);
            fireTableRowsUpdated(0,getRowCount()-1);
        }
    
    }
    
    
    
    public PolygonArea() {
        super();
        createArea(new ArrayList<Point2D>(),0,0);
        createGeneralPath();
        calcCenterAndDefaultPos();
    }
    
    public PolygonArea(String n) { //expects name identifier
        super(n);
        createArea(new ArrayList<Point2D>(),0,0);
        createGeneralPath();
        calcCenterAndDefaultPos();
    }
    
    public PolygonArea(String n, int id, double ox, double oy, double oz, List<Point2D> pList, boolean s, String anot) {
        super(n,id,ox,oy,oz,0.0,0.0,s,anot);
        createArea(pList, ox, oy);
        createGeneralPath();
        calcCenterAndDefaultPos();
    }
        
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
    
    //vertex points are provided as  coordinates RELATIVE to offsetX/offsetY
    //to provide absolute coordinates, pass offsetX=0, offsetY=0
    private void createArea(List<Point2D> pList, double offsetX, double offsetY) {
        if (pList!=null) {
            points=new ArrayList<Point2D>(pList.size());
            Path2D polygon=new Path2D.Double();
            if (pList.size()<=1) {
                if (pList.size()==1) {
                    points.add(new Point2D.Double(offsetX+pList.get(0).getX(), offsetY+pList.get(0).getY()));
                    topLeftX=points.get(0).getX();
                    topLeftY=points.get(0).getX();
                } else {
                    topLeftX=offsetX;
                    topLeftY=offsetY;
                }    
            }
            if (pList.size()>1) {
                points.add(new Point2D.Double(offsetX+pList.get(0).getX(), offsetY+pList.get(0).getY()));
                polygon.moveTo(points.get(0).getX(), points.get(0).getY());
                for (int i=1; i< pList.size(); i++) {
                    points.add(new Point2D.Double(offsetX+pList.get(i).getX(), offsetY+pList.get(i).getY()));
                    polygon.lineTo(points.get(i).getX(), points.get(i).getY());
                }
                polygon.closePath();
            }
            topLeftX=polygon.getBounds2D().getX();
            topLeftY=polygon.getBounds2D().getY();
            width=polygon.getBounds2D().getWidth();
            height=polygon.getBounds2D().getHeight();
            shape=polygon;
        } else {
//            polygon=null;
            shape=null;
        }
//        calcCenterAndDefaultPos();
    }
    
    //vertex points are provided as ABSOLUTE coordinates
    private void createArea(List<Point2D> pList) {
        createArea(pList,0,0);
        createGeneralPath();
        calcCenterAndDefaultPos();
    }
    
    @Override
    public void setWidth(double w) {
        //do nothing because width is determined by vertex points
    }

    @Override
    public void setHeight(double h) {
        //do nothing because width is determined by vertex points
    }
    
    //leads to horizontal translation
    @Override
    public void setTopLeftX(double x) {
        double deltaX=x-topLeftX;
        if (points!=null) {
/*            for (Point2D point:points) {
                point.setLocation(point.getX()+deltaX, point.getY());
            }
            topLeftX=x;
            createArea(points, topLeftX, topLeftY);*/
            createArea(points,deltaX,0);
            createGeneralPath();
            calcCenterAndDefaultPos();
        }
    }
    
    //leads to vertical translation
    @Override
    public void setTopLeftY(double y) {
        double deltaY=y-topLeftY;
        if (points!=null) {
/*            for (Point2D point:points) {
                point.setLocation(point.getX(), point.getY()+deltaY);
            }
            topLeftY=y;
            createArea(points, topLeftX, topLeftY);*/
            createArea(points,0,deltaY);
            createGeneralPath();
            calcCenterAndDefaultPos();
        }
    }
    
    @Override
    public void setTopLeft(double x, double y) {
        double deltax=x-topLeftX;
        double deltay=y-topLeftY;
        if (points!=null) {
/*            for (Point2D point:points) {
                point.setLocation(point.getX()+deltax, point.getY()+deltay);
            }
            topLeftX=x;
            topLeftY=y;
            createArea(points, topLeftX, topLeftY);*/
            createArea(points,deltax,deltay);
            createGeneralPath();
            calcCenterAndDefaultPos();
        }
    }
    
    @Override
    protected void initializeFromJSONObject(JSONObject obj) throws JSONException {
        JSONArray listarray=obj.getJSONArray(TAG_POINT_ARRAY);
        List<Point2D> pList = new ArrayList<Point2D>();
        if (listarray!=null) {
            for (int i=0; i<listarray.length(); i++) {
                pList.add(new Point2D.Double(0,0));
            }
            for (int i=0; i<listarray.length(); i++) {
                JSONObject pointObj=listarray.getJSONObject(i);
                int number=pointObj.getInt(TAG_NUMBER);
                double x=pointObj.getDouble(TAG_COORD_X);
                double y=pointObj.getDouble(TAG_COORD_Y);
                pList.set(number,new Point2D.Double(x,y));
            }
        }        
        createArea(pList,0,0);
        createGeneralPath();
        calcCenterAndDefaultPos();
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
/*
    @Override
    public double getCenterX() {
        return topLeftX + polygon.getBounds2D().getWidth()/2;
    }

    @Override
    public double getCenterY() {
        return topLeftY + polygon.getBounds2D().getHeight()/2;
    }
*/
    @Override
    public void drawArea(Graphics2D g2d, boolean showZProfile) {
        g2d.setColor(getFillColor(showZProfile));
/*        
        int x = (int) Math.round(topLeftX*physToPixelRatio);
        int y = (int) Math.round(topLeftY*physToPixelRatio);
        int w = (int) Math.round(width*physToPixelRatio);
        int h = (int) Math.round(height*physToPixelRatio);*/
/*        AffineTransform at=new AffineTransform();
        Shape polyShape=polygon.createTransformedShape(at);*/
        g2d.fill(shape); 
        g2d.setColor(getBorderColor());
        g2d.draw(shape); 
    }

    @Override
    public void drawTiles(Graphics2D g2d, double fovX, double fovY, TilingSetting setting) {
        drawTileByTileOvl(g2d, fovX, fovY, setting);
    }

    @Override
    public boolean isInArea(double x, double y) {
        return shape.contains(x,y);
    }

    @Override
    public boolean isFovInsideArea(double centerX, double centerY, double fovWidth, double fovHeight) {
        return shape.contains(centerX-fovWidth/2, centerY-fovHeight/2, fovWidth, fovHeight);
    }

    @Override
    public boolean doesFovTouchArea(double centerX, double centerY, double fovWidth, double fovHeight) {
        return shape.intersects(centerX-fovWidth/2, centerY-fovHeight/2, fovWidth, fovHeight);
    }

    @Override
    public boolean isInsideRect(Rectangle2D r) {
        return r.contains(shape.getBounds2D());
    }

    @Override
    public SampleArea duplicate() {
        PolygonArea newArea = new PolygonArea(this.getName());
//        newArea.shape=this.getShapeLabel();
        newArea.setId(this.getId());
        newArea.affineTrans=new AffineTransform(this.affineTrans);
        newArea.setSelectedForAcq(isSelectedForAcq());
        newArea.setSelectedForMerge(isSelectedForMerge());
        newArea.setComment(this.comment);
        newArea.setAcquiring(this.acquiring);
//        newArea.setTilingSetting(this.tiling.duplicate());
        newArea.tilePosList=new ArrayList<Tile>(this.getTilePositions());
        newArea.setUnknownTileNum(this.hasUnknownTileNum());
        newArea.createArea(points,0,0);
        createGeneralPath();
        calcCenterAndDefaultPos();
        return newArea;
    }

/*
    @Override
    public void calculateCenterPos() {
        centerXYPos=centerOfMass(points);
    }

    @Override
    public void calculateDefaultPos() {
        defaultXYPos=centerOfMass(points);
    }
*/
    
    @Override
    public void calcCenterAndDefaultPos() {
        centerXYPos=centerOfMass(points);
        defaultXYPos=centerXYPos;
    }

    //returns absolute coordinates
    @Override
    public List<Point2D> getOutlinePoints() {
        return points;
    }

    public List<Point2D> getOutlinePointsRel() {
        if (points==null)
            return null;
        List<Point2D> list=new ArrayList<Point2D>(points.size());
        for (Point2D p:points) {
            list.add(new Point2D.Double(p.getX()-topLeftX,p.getY()-topLeftY));
        }
        return list;
    }
    
    @Override
    public SampleArea showConfigDialog(Rectangle2D layoutBounds) {
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
        topLeftXField.setValue(new Double(topLeftX) / 1000);
        optionPanel.add(topLeftXField);
        
        l=new JLabel("Center X (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JLabel centerXField = new JLabel(getCenterXYPos()==null ? "unknown" : NUMBER_FORMAT.format(getCenterXYPos().getX() / 1000));
        optionPanel.add(centerXField);

        l=new JLabel("Origin Y (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField topLeftYField = new JFormattedTextField();
        topLeftYField.setColumns(10);
        topLeftYField.setValue(new Double(topLeftY) / 1000);
        optionPanel.add(topLeftYField);
        
        l=new JLabel("Center Y (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JLabel centerYField = new JLabel(getCenterXYPos()==null ? "unknown" : NUMBER_FORMAT.format(getCenterXYPos().getY() / 1000));
        optionPanel.add(centerYField);

        l=new JLabel("Width (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JLabel widthField = new JLabel(NUMBER_FORMAT.format(width / 1000));
        optionPanel.add(widthField);

        l=new JLabel("Height (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JLabel heightField = new JLabel(NUMBER_FORMAT.format(height / 1000));
        optionPanel.add(heightField);
        
        final JTable pointTable = new JTable(new PointTableModel(points));
        pointTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        
        final JRadioButton absoluteButton = new JRadioButton("Absolute");
        absoluteButton.setActionCommand("absolute");
        absoluteButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                PointTableModel model=(PointTableModel)pointTable.getModel();
                model.setData(getOutlinePoints(), true);
            }
        });

        final JRadioButton relativeButton = new JRadioButton("Relative to Origin");
        relativeButton.setActionCommand("relative");
        relativeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                PointTableModel model=(PointTableModel)pointTable.getModel();
                model.setData(getOutlinePointsRel(), true);
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
        pointTable.getModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent evt) {
                IJ.log("tableChanged: "+evt.toString());
                if (points!=null) { // {&& points.size() > 0) {
                    if ((evt.getColumn() == 0 || evt.getColumn() == 1)) {
                        IJ.log("     columnChanged");
                        PointTableModel model=(PointTableModel)pointTable.getModel();
                        if (absoluteButton.isSelected()) {
                            createArea(points);
                            model.setData(getOutlinePoints(), true);
                        } else if (relativeButton.isSelected()){
                            createArea(model.pointList,topLeftX, topLeftY);
                            model.setData(getOutlinePointsRel(), true);
                        } 
                        createGeneralPath();
                        calcCenterAndDefaultPos();
                    } else {
                        IJ.log("     other changes:" + evt.getColumn());
                    }
                    topLeftXField.setValue(new Double(topLeftX / 1000));
                    topLeftYField.setValue(new Double(topLeftY / 1000));
                    centerXField.setText(getCenterXYPos()==null ? "?" : NUMBER_FORMAT.format(getCenterXYPos().getX() / 1000));
                    centerYField.setText(getCenterXYPos()==null ? "?" : NUMBER_FORMAT.format(getCenterXYPos().getY() / 1000));
                    widthField.setText(NUMBER_FORMAT.format(width / 1000));
                    heightField.setText(NUMBER_FORMAT.format(height / 1000));
                }
            }
        });
        
        topLeftXField.addPropertyChangeListener("value", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ((points!=null && points.size() > 0) && ((Double)evt.getNewValue() != (Double)evt.getOldValue())) {
                    PointTableModel model=(PointTableModel)pointTable.getModel();
                    if (absoluteButton.isSelected()) {
                        double dx=((Double)evt.getNewValue() * 1000)-topLeftX;
                        createArea(points,dx,0);
                        model.setData(getOutlinePoints(),true);
                    } else if (relativeButton.isSelected()) {
                        createArea(model.pointList,(Double)evt.getNewValue() * 1000,topLeftY);
                        model.setData(getOutlinePointsRel(),true);
                    }
                    createGeneralPath();
                    calcCenterAndDefaultPos();
                    centerXField.setText(getCenterXYPos()==null ? "?" : NUMBER_FORMAT.format(getCenterXYPos().getX() / 1000));
                }
            }
        });

        topLeftYField.addPropertyChangeListener("value", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ((points!=null && points.size() > 0) && ((Double)evt.getNewValue() != (Double)evt.getOldValue())) {
                    PointTableModel model=(PointTableModel)pointTable.getModel();
                    if (absoluteButton.isSelected()) {
                        double dy=((Double)evt.getNewValue() * 1000)-topLeftY;
                        createArea(points,0,dy);
                        model.setData(getOutlinePoints(),true);
                    } else if (relativeButton.isSelected()) {
                        createArea(model.pointList,topLeftX,(Double)evt.getNewValue() * 1000);
                        model.setData(getOutlinePointsRel(),true);
                    }
                    createGeneralPath();
                    calcCenterAndDefaultPos();
                    centerYField.setText(getCenterXYPos()==null ? "?" : NUMBER_FORMAT.format(getCenterXYPos().getY() / 1000));
                }
            }
        });

        SpringLayout springLayout = new SpringLayout();
        JPanel tablePanel=new JPanel();
        tablePanel.setLayout(springLayout);
        tablePanel.setPreferredSize(new Dimension(160,250));
        JScrollPane scrollPane=new JScrollPane(pointTable);
        tablePanel.add(scrollPane);

        JButton newButton = new JButton(new ImageIcon(getClass().getResource("/autoimage/resources/add2.png")));
        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                Point2D point=new Point2D.Double(0,0);
                PointTableModel model=(PointTableModel)pointTable.getModel();
                model.addRow(point);
                pointTable.changeSelection(pointTable.getRowCount()-1,pointTable.getRowCount(),false,false);
                createArea(points,0,0);
                createGeneralPath();
                calcCenterAndDefaultPos();
                model.setData(getOutlinePoints(),true);
            }
        });
        newButton.setToolTipText("Create new vertex point");
        tablePanel.add(newButton);

        int northConstraint = 12;
        final int buttonHeight = 22;      
        springLayout.putConstraint(SpringLayout.NORTH, newButton, northConstraint, SpringLayout.NORTH, tablePanel);     
        springLayout.putConstraint(SpringLayout.SOUTH, newButton, northConstraint+=buttonHeight, SpringLayout.NORTH, tablePanel);
        springLayout.putConstraint(SpringLayout.EAST, newButton, -6, SpringLayout.EAST, tablePanel);
        springLayout.putConstraint(SpringLayout.WEST, newButton, -28, SpringLayout.EAST, tablePanel);

        springLayout.putConstraint(SpringLayout.NORTH, scrollPane, 0, SpringLayout.NORTH, newButton);
        springLayout.putConstraint(SpringLayout.SOUTH, scrollPane, -12, SpringLayout.SOUTH, tablePanel);
        springLayout.putConstraint(SpringLayout.EAST, scrollPane, -6, SpringLayout.WEST, newButton);
        springLayout.putConstraint(SpringLayout.WEST, scrollPane, 6, SpringLayout.WEST, tablePanel);

        final JButton removeButton = new JButton(new ImageIcon(getClass().getResource("/autoimage/resources/delete.png")));
        removeButton.addActionListener(new ActionListener() {
           @Override
           public void actionPerformed(ActionEvent arg0) {
               int selRowCount=pointTable.getSelectedRowCount();
               if (selRowCount > 0) {
                   int[] selRows=pointTable.getSelectedRows();
                   for (int i=0; i<selRows.length; i++){
                       selRows[i]=pointTable.convertRowIndexToModel(selRows[i]);
                   }    
                   PointTableModel model=(PointTableModel)pointTable.getModel();
                   model.removeRows(selRows);
                   createArea(points,0,0);
                    createGeneralPath();
                    calcCenterAndDefaultPos();
                   model.setData(getOutlinePoints(),true);
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
                    int newSelRowInView=selRows[0]-1;
                    //
                    int firstMinusOneIndex=pointTable.convertRowIndexToModel(selRows[0]-1);
                    //convert view row indices retrieved from table to corresponding indices in model
                    for (int i=0; i<selRows.length; i++) {
                        selRows[i]=pointTable.convertRowIndexToModel(selRows[i]);
                    }
                    PointTableModel model=(PointTableModel)pointTable.getModel();
                    model.rowsUp(selRows, firstMinusOneIndex);
                    createArea(points,0,0);
                    createGeneralPath();
                    calcCenterAndDefaultPos();
                    model.setData(getOutlinePoints(),true);
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
                    int newSelRowInView=selRows[0]+1;
                    //
                    int lastPlusOneIndex=pointTable.convertRowIndexToModel(selRows[selRows.length-1]+1);
                    //convert view row indices retrieved from table to corresponding indices in model
                    for (int i=0; i<selRows.length; i++) {
                        selRows[i]=pointTable.convertRowIndexToModel(selRows[i]);
                    }
                    PointTableModel model=(PointTableModel)pointTable.getModel();
                    model.rowsDown(selRows, lastPlusOneIndex);
                    createArea(points,0,0);
                    createGeneralPath();
                    calcCenterAndDefaultPos();
                    model.setData(getOutlinePoints(),true);
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
        int result;
        boolean invalidParams;
        do {
            invalidParams=false;
            result = JOptionPane.showConfirmDialog(null, combinedPanel, 
                getShapeType()+": Configuration", JOptionPane.OK_CANCEL_OPTION);
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
    
    @Override
    public int supportedLayouts() {
        return SampleArea.SUPPORT_CUSTOM_LAYOUT;
    }
    
    @Override
    protected void createShape() {
        createArea(points,0,0);
//        createGeneralPath();
//        calcCenterAndDefaultPos();
    }
    
}
