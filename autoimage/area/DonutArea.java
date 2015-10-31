package autoimage.area;

import autoimage.api.BasicArea;
import autoimage.Tile;
import autoimage.Utils;
import autoimage.gui.PreviewPanel;
import ij.IJ;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.json.JSONException;
import org.json.JSONObject;


/**
 *
 * @author Karsten Siller
 */
public class DonutArea extends TwoAxesArea {
    
    public static final String TAG_RING_WIDTH = "RING_WIDTH";

    private double ringWidth;
    
    public DonutArea() {
        super();
        setRingWidth(0);
    }
    
    public DonutArea(String n) { //expects name identifier
        super(n);
        setRingWidth(0);
    }
    
    public DonutArea(String n, int id, double ox, double oy, double oz, double w, double h, double ringWidth, boolean selForAcq, String anot) {
        super(n,id,ox,oy,oz,w,h,selForAcq,anot);
        setRingWidth(ringWidth);
    }
        
    public void setRingWidth(double rw) {
        ringWidth=rw;
        createShape();
        setRelDefaultPos();
        createGeneralPath();
    }
    
    public double getRingWidth() {
        return ringWidth;
    }
    
    @Override
    public String getShapeType() {
        return "Donut";
    }
        
    @Override
    public void initializeFromJSONObject(JSONObject obj) throws JSONException {
        super.initializeFromJSONObject(obj);
        if (obj!=null)
            ringWidth=obj.getDouble(TAG_RING_WIDTH);
        else
            ringWidth=1;
    }

    @Override
    protected void addFieldsToJSONObject(JSONObject obj) throws JSONException {
        super.addFieldsToJSONObject(obj);
        if (obj!=null)
            obj.put(TAG_RING_WIDTH,ringWidth);
    }

    @Override
    public BasicArea duplicate() {
        DonutArea newArea = new DonutArea(this.getName());
        newArea.setId(this.getId());
        newArea.centerXYPos=new Point2D.Double(this.getCenterXYPos().getX(), this.getCenterXYPos().getY());
        newArea.setRelativeZPos(this.relativeZPos);
        newArea.width=this.width;
        newArea.height=this.height;
        newArea.affineTrans=new AffineTransform(this.affineTrans);
        newArea.setSelectedForAcq(isSelectedForAcq());
        newArea.setSelectedForMerge(isSelectedForMerge());
        newArea.setComment(this.comment);
        newArea.setAcquiring(this.acquiring);
        newArea.tilePosList=new ArrayList<Tile>(this.getTilePositions());
        newArea.ringWidth=this.ringWidth;
        newArea.setUnknownTileNum(this.hasUnknownTileNum());
        newArea.createShape();
        newArea.setRelDefaultPos();
        newArea.createGeneralPath();
        return newArea;
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
        zField.setValue(new Double(relativeZPos / 1000));
        optionPanel.add(zField);
             
        l=new JLabel("Origin X (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField topLeftXField = new JFormattedTextField();
        topLeftXField.setColumns(10);
        topLeftXField.setValue(new Double(getTopLeftX() / 1000));
        optionPanel.add(topLeftXField);
        
        l=new JLabel("Origin Y (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField topLeftYField = new JFormattedTextField();
        topLeftYField.setColumns(10);
        topLeftYField.setValue(new Double(getTopLeftY() / 1000));
        optionPanel.add(topLeftYField);
        
        l=new JLabel("Center X (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField centerXField = new JFormattedTextField();
        centerXField.setColumns(10);
        centerXField.setValue(new Double(getCenterXYPos().getX() / 1000));
        optionPanel.add(centerXField);

        l=new JLabel("Center Y (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField centerYField = new JFormattedTextField();
        centerYField.setColumns(10);
        centerYField.setValue(new Double(getCenterXYPos().getY() / 1000));
        optionPanel.add(centerYField);

        l=new JLabel("Width (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField widthField = new JFormattedTextField();
        widthField.setColumns(10);
        widthField.setValue(new Double(width / 1000));
        optionPanel.add(widthField);

        l=new JLabel("Height (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField heightField = new JFormattedTextField();
        heightField.setColumns(10);
        heightField.setValue(new Double(height / 1000));
        optionPanel.add(heightField);
        
        l=new JLabel("Rotation (degree):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField rotationField = new JFormattedTextField();
        rotationField.setColumns(10);
        rotationField.setValue(new Double(Utils.getRotation(affineTrans))/Math.PI*180);
        optionPanel.add(rotationField);
        
        l=new JLabel("Ringwidth (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField ringwidthField = new JFormattedTextField();
        ringwidthField.setColumns(10);
        ringwidthField.setValue(new Double(ringWidth / 1000));
        optionPanel.add(ringwidthField);
        
        final PreviewPanel previewPanel = new PreviewPanel(generalPath, getShapeBoundsDiagonale());
        previewPanel.setPreferredSize(new Dimension (160,160));
        
        JPanel allPanels=new JPanel();
        allPanels.add(optionPanel);
        allPanels.add(previewPanel);
        
        topLeftXField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == topLeftXField) {
                    double newValue = ((Number)topLeftXField.getValue()).doubleValue() * 1000;
                    if (newValue != getTopLeftX()) {
                        setTopLeftX(newValue);
                        centerXField.setValue(new Double(getCenterXYPos().getX() / 1000));
                    }
                }
            }
        });

        centerXField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == centerXField) {
                    double newValue = ((Number)centerXField.getValue()).doubleValue() * 1000;
                    if (newValue != getCenterXYPos().getX()) {
//                        setTopLeftX(newValue-width/2);
                        setCenter(newValue,centerXYPos.getY());
                        topLeftXField.setValue(new Double(getTopLeftX() / 1000));
                    }
                }
            }
        });
        
        topLeftYField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == topLeftYField) {
                    double newValue = ((Number)topLeftYField.getValue()).doubleValue() * 1000;
                    if (newValue != getTopLeftY()) {
                        setTopLeftY(newValue);//recalculates center pos
                        centerYField.setValue(new Double(getCenterXYPos().getY() / 1000));
                    }
                }
            }
        });

        centerYField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == centerYField) {
                    double newValue = ((Number)centerYField.getValue()).doubleValue() * 1000;
                    if (newValue != getCenterXYPos().getY()) {
//                        setTopLeftY(newValue-height/2);
                        setCenter(centerXYPos.getX(),newValue);
                        topLeftYField.setValue(new Double(getTopLeftY() / 1000));
                    }
                }
            }
        });
        
        widthField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == widthField) {
                    double newValue = ((Number)widthField.getValue()).doubleValue() * 1000;
                    if (newValue != width) {
                        setWidth(newValue);
                        topLeftXField.setValue(new Double(getTopLeftX()/1000));
                        topLeftYField.setValue(new Double(getTopLeftY()/1000));
                        previewPanel.setPath(generalPath, getShapeBoundsDiagonale());
                        previewPanel.repaint();
                    }
                }
            }
        });

        heightField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == heightField) {
                    double newValue = ((Number)heightField.getValue()).doubleValue() * 1000;
                    if (newValue != height) {
                        setHeight(newValue);
                        topLeftXField.setValue(new Double(getTopLeftX()/1000));
                        topLeftYField.setValue(new Double(getTopLeftY()/1000));
                        previewPanel.setPath(generalPath, getShapeBoundsDiagonale());
                        previewPanel.repaint();
                    }
                }
            }
        });
        
        ringwidthField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == ringwidthField) {
                    double newValue = ((Number)ringwidthField.getValue()).doubleValue() * 1000;
                    if (newValue != ringWidth) {
                        setRingWidth(newValue);
                        previewPanel.setPath(generalPath, getShapeBoundsDiagonale());
                        previewPanel.repaint();
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
                        //using (0/0) as anchor since translation to centerXYPos is dealt with independently
                        setAffineTransform(AffineTransform.getRotateInstance(newValue,0,0));
                        topLeftXField.setValue(new Double(getTopLeftX()/1000));
                        topLeftYField.setValue(new Double(getTopLeftY()/1000));
                        previewPanel.setPath(generalPath, getShapeBoundsDiagonale());
                        previewPanel.repaint();
                    }
                }
            }
        });        
                
        int result;
        do {
            result = JOptionPane.showConfirmDialog(null, allPanels, 
                getShapeType()+": Configuration", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION && !isInsideRect(layoutBounds)) {
                JOptionPane.showMessageDialog(null,"The area does not fit into the layout.");
            }
        } while (result == JOptionPane.OK_OPTION && !isInsideRect(layoutBounds));
        
        if (result == JOptionPane.CANCEL_OPTION) {
            return null;
        } else {
            setName((String)nameField.getValue());
            centerXYPos=new Point2D.Double(
                    ((Number)centerXField.getValue()).doubleValue()*1000, 
                    ((Number)centerYField.getValue()).doubleValue()*1000);
            width=((Number)widthField.getValue()).doubleValue()*1000;
            height=((Number)heightField.getValue()).doubleValue()*1000;
            //center pos will be set automatically
            setRelativeZPos(((Number)zField.getValue()).doubleValue()*1000);            
            setRingWidth(((Number)ringwidthField.getValue()).doubleValue()*1000);
            return this;
        }
            
    }

    @Override
    public int supportedLayouts() {
        return BasicArea.SUPPORT_CUSTOM_LAYOUT;
    }

    @Override
    protected void createShape() {
        java.awt.geom.Area donut = new java.awt.geom.Area(new Ellipse2D.Double(
                -width/2, 
                -height/2, 
                width, 
                height));
        java.awt.geom.Area hole = new java.awt.geom.Area(new Ellipse2D.Double(
                -width/2+ringWidth, 
                -height/2+ringWidth, 
                width-2*ringWidth, 
                height-2*ringWidth));
        donut.subtract(hole);
        shape=new Path2D.Double(donut);
        IJ.log(shape.getBounds2D().toString());
    }

    @Override
    protected void setRelDefaultPos() {
        relDefaultXYPos=new Point2D.Double(0,-height/2+ringWidth/2);
    }
    
}
