package autoimage.area;

import autoimage.Tile;
import autoimage.Utils;
import autoimage.api.BasicArea;
import autoimage.gui.AreaPreviewPanel;
import ij.IJ;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten Siller
 */
public class RegularPolygonArea extends BasicArea {
    
//    private int noOfSides;
    private List<Point2D> points;
    private double radius;
    private int noOfSides;
    
    protected final static String TAG_RADIUS = "RADIUS";
    protected final static String TAG_NO_OF_SIDES = "NO_OF_SIDES";
    
    private final static DecimalFormat NUMBER_FORMAT = new DecimalFormat("###,###,##0.000");


    public RegularPolygonArea() {
        super();
        points=new ArrayList<Point2D>();
        radius=0;
        noOfSides=3;
        setNoOfSides(noOfSides);
    }
    
    public RegularPolygonArea(String n) { //expects name identifier
        super(n);
        points=new ArrayList<Point2D>();
        radius=0;
        noOfSides=3;
        setNoOfSides(noOfSides);
    }
    
    /**
     * 
     * @param n
     * @param id
     * @param ox
     * @param oy
     * @param oz
     * @param r
     * @param sides
     * @param s
     * @param anot 
     */
    public RegularPolygonArea(String n, int id, double ox, double oy, double oz, double r, int sides, boolean s, String anot) {
        super(n,id,ox,oy,oz,s,anot);
//        createArea(pList, ox, oy);
        radius=r;
        noOfSides=sides;
        setNoOfSides(sides);
    }


    public void setRadius(double r) {
        radius=r;
        createShape();
        setRelDefaultPos();
        createGeneralPath();
    }    
   
    
    public double getRadius() {
        return radius;
    }    
        
    
    public void setNoOfSides(int sides) {
        noOfSides=sides;
        createShape();
        setRelDefaultPos();
        createGeneralPath();
    }
    
    
    public int getNoOfSides() {
        return noOfSides;
    }    
    
    
    @Override
    protected void initializeFromJSONObject(JSONObject obj) throws JSONException {
        noOfSides=obj.getInt(TAG_NO_OF_SIDES);
        if (noOfSides>=0) {
            points=new ArrayList<Point2D>(noOfSides);
        } else {
            points=new ArrayList<Point2D>();
        }    
        radius=obj.getDouble(TAG_RADIUS);
    }

    @Override
    protected void addFieldsToJSONObject(JSONObject obj) throws JSONException {
        obj.put(TAG_NO_OF_SIDES, noOfSides);
        obj.put(TAG_RADIUS, radius);
    }

    @Override
    public String getShapeType() {
        return "Regular Polygon";
    }

    @Override
    public BasicArea duplicate() {
        RegularPolygonArea newArea = new RegularPolygonArea(this.getName());
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
        newArea.radius=this.radius;
        newArea.noOfSides=this.noOfSides;
        newArea.setNoOfSides(noOfSides);
        return newArea;
    }


    private void updatePreviewPanel(AreaPreviewPanel previewPanel) {
        IJ.log("updatePreviewPanel begin");
        previewPanel.setPath(generalPath, centerXYPos,radius*2);
        previewPanel.addPath(new Path2D.Double(new Ellipse2D.Double(
                centerXYPos.getX()-radius,
                centerXYPos.getY()-radius,
                radius*2,
                radius*2)),Color.RED,null);
        previewPanel.repaint();
        IJ.log("updatePreviewPanel end");
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
        
        l=new JLabel("No of Sides:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JSpinner noOfSidesField = new JSpinner();
        SpinnerNumberModel spinnerModel=new SpinnerNumberModel(points.size(),3,100,1);
        noOfSidesField.setModel(spinnerModel);
        optionPanel.add(noOfSidesField);

        l=new JLabel("Radius (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField radiusField = new JFormattedTextField();
        radiusField.setColumns(10);
        radiusField.setValue(new Double(radius) / 1000);
        optionPanel.add(radiusField);

        l=new JLabel("Rotation (degree):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField rotationField = new JFormattedTextField();
        rotationField.setColumns(10);
        rotationField.setValue(new Double(Utils.getRotation(affineTrans))/Math.PI*180);
        optionPanel.add(rotationField);        
                
        final AreaPreviewPanel previewPanel = new AreaPreviewPanel(generalPath, getShapeBoundsDiagonale());
        previewPanel.setPreferredSize(new Dimension (160,160));
        
        topLeftXField.addPropertyChangeListener("value", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ((points!=null && points.size() > 0) && ((Double)evt.getNewValue() != (Double)evt.getOldValue())) {
                    double newValue = ((Number)topLeftXField.getValue()).doubleValue()*1000;
                    if (newValue != getTopLeftX()) {
                        setTopLeftX(newValue);//recalculates center pos
                        centerXField.setValue(new Double(getCenterXYPos().getX() / 1000));
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
                        setTopLeftY(newValue);//recalculates centerXYPos and generalPath
                        centerYField.setText(getCenterXYPos()==null ? "?" : NUMBER_FORMAT.format(getCenterXYPos().getY() / 1000));
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
                        setCenter(newValue, centerXYPos.getY());//recalculates generalPath
                        topLeftXField.setValue(new Double(getTopLeftX() / 1000));
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
                        setCenter(centerXYPos.getX(),newValue);//recalculates generalPath
                        topLeftYField.setText(NUMBER_FORMAT.format(getTopLeftY() / 1000));
                    }
                }
            }
        });
        
        radiusField.addPropertyChangeListener("value", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ((points!=null && points.size() > 0) && ((Double)evt.getNewValue() != (Double)evt.getOldValue())) {
                    double newValue = ((Number)radiusField.getValue()).doubleValue()*1000;
                    if (newValue != radius) {
                        setRadius(newValue);//recalculates generalPath
                        topLeftXField.setValue(new Double(getTopLeftX()/1000));
                        topLeftYField.setText(NUMBER_FORMAT.format(getTopLeftY() / 1000));
                        widthField.setText(NUMBER_FORMAT.format(generalPath.getBounds2D().getWidth() / 1000));
                        heightField.setText(NUMBER_FORMAT.format(generalPath.getBounds2D().getHeight() / 1000));
                        updatePreviewPanel(previewPanel);
                    }
                }
            }
        });
        
        noOfSidesField.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                int newValue = ((Number)noOfSidesField.getValue()).intValue();
                if (noOfSides != newValue) {
                    setNoOfSides(newValue);
                    topLeftXField.setValue(new Double(getTopLeftX()/1000));
                    topLeftYField.setValue(new Double(getTopLeftY()/1000));
                    widthField.setText(NUMBER_FORMAT.format(generalPath.getBounds2D().getWidth() / 1000));
                    heightField.setText(NUMBER_FORMAT.format(generalPath.getBounds2D().getHeight() / 1000));
                    updatePreviewPanel(previewPanel);
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
                        //using (0/0) as anchor since translation to centerXYPos is dealt with independently
                        setAffineTransform(AffineTransform.getRotateInstance(newValue,0,0));
                        topLeftXField.setValue(new Double(getTopLeftX()/1000));
                        topLeftYField.setValue(new Double(getTopLeftY()/1000));
                        widthField.setText(NUMBER_FORMAT.format(generalPath.getBounds2D().getWidth() / 1000));
                        heightField.setText(NUMBER_FORMAT.format(generalPath.getBounds2D().getHeight() / 1000));
                        updatePreviewPanel(previewPanel);
                    }
                }
            }
        });
        
        JPanel allPanels=new JPanel();
        allPanels.add(optionPanel);
        allPanels.add(previewPanel);
        
        int result;
        boolean invalidParams;
        do {
            invalidParams=false;
            result = JOptionPane.showConfirmDialog(
                    null, 
                    allPanels, 
                    getShapeType()+": Configuration", 
                    JOptionPane.OK_CANCEL_OPTION);
            createShape();
//            centerShape();
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
    protected Path2D createPolygonFromPoints(List<Point2D> pList) {
        Path2D poly=new Path2D.Double();
        if (pList==null || pList.isEmpty()) {
            poly.moveTo(0,0);
//            poly.closePath();
        } else {    
            poly.moveTo(pList.get(0).getX(), pList.get(0).getY());
            if (pList.size()<=1) {
            }
            if (pList.size()>1) {
                for (int i=1; i< pList.size(); i++) {
                    poly.lineTo(pList.get(i).getX(), pList.get(i).getY());
                }
                poly.closePath();
            }
        }
        return poly;
    }
    
    /*
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
    */
    
    
    /**
     * Points are expected to be relative coordinates defining this.shape centered around origin (0/0)
     */
    @Override
    protected void createShape() {
        if (noOfSides >= 0) {
            points=new ArrayList<Point2D>(noOfSides);
        } else {
            points=new ArrayList<Point2D>();
        }    
        double angle=Math.PI*2/noOfSides;
        for (int i=0; i<noOfSides; i++) {
            double x=Math.sin(angle*i)*radius;
            double y=-Math.cos(angle*i)*radius;
            points.add(new Point2D.Double(x,y));
        }
        shape=createPolygonFromPoints(points);
    }  
    
    @Override
    public int supportedLayouts() {
        return BasicArea.SUPPORT_CUSTOM_LAYOUT;
    }

        
}

