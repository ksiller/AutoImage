package autoimage.area;

import autoimage.Utils;
import autoimage.api.SampleArea;
import java.awt.GridLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
public abstract class TwoAxesArea extends SampleArea{
    
    public static final String TAG_WIDTH = "WIDTH";
    public static final String TAG_HEIGHT = "HEIGHT";
    
    protected double width;//this is along the x-axis when not rotateted, it remains unchanged when rotating (unlike getBounds.getWidth())
    protected double height;//this is along the y-axis when not rotateted, it remains unchanged when rotating (unlike getBounds.getHeight())

    public TwoAxesArea() {
        super();
        setWidthAndHeight(0,0);
    }
    
    public TwoAxesArea(String n) { //expects name identifier
        super(n);
        setWidthAndHeight(0,0);
    }
       
    public TwoAxesArea(String n, int id, double ox, double oy, double oz, double w, double h, boolean selForAcq, String anot) {
        super(n,id,ox,oy,oz,selForAcq,anot);
        setWidthAndHeight(w,h);
    }
           
    public double getWidth() {
        return width;
    } 
    
    public void setWidth(double value) {
        width=value;
        createShape();
        setRelDefaultPos();
        createGeneralPath();
    }
    
    public double getHeight() {
        return height;
    } 
    
    public void setHeight(double value) {
        height=value;
        createShape();
        setRelDefaultPos();
        createGeneralPath();
    }
    
    public void setWidthAndHeight(double w, double h) {
        width=w;
        height=h;
        createShape();
        setRelDefaultPos();
        createGeneralPath();
    }    

    @Override
    protected void initializeFromJSONObject(JSONObject obj) throws JSONException {
        this.width=obj.getDouble(TAG_WIDTH);
        this.height=obj.getDouble(TAG_HEIGHT);
    }

    @Override
    protected void addFieldsToJSONObject(JSONObject obj) throws JSONException {
        obj.put(TAG_WIDTH,width);
        obj.put(TAG_HEIGHT,height);
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
        zField.setValue(new Double(relativeZPos/1000));
        optionPanel.add(zField);
             
        l=new JLabel("Origin X (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField topLeftXField = new JFormattedTextField();
        topLeftXField.setColumns(10);
        topLeftXField.setValue(new Double(this.getTopLeftX()/1000));
        optionPanel.add(topLeftXField);
        
        l=new JLabel("Origin Y (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField topLeftYField = new JFormattedTextField();
        topLeftYField.setColumns(10);
        topLeftYField.setValue(new Double(this.getTopLeftY())/1000);
        optionPanel.add(topLeftYField);
        
        l=new JLabel("Center X (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField centerXField = new JFormattedTextField();
        centerXField.setColumns(10);
        centerXField.setValue(new Double(getCenterXYPos().getX()/1000));
        optionPanel.add(centerXField);

        l=new JLabel("Center Y (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField centerYField = new JFormattedTextField();
        centerYField.setColumns(10);
        centerYField.setValue(new Double(getCenterXYPos().getY()/1000));
        optionPanel.add(centerYField);

        l=new JLabel("Width (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField widthField = new JFormattedTextField();
        widthField.setColumns(10);
        widthField.setValue(new Double(width)/1000);
        optionPanel.add(widthField);

        l=new JLabel("Height (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField heightField = new JFormattedTextField();
        heightField.setColumns(10);
        heightField.setValue(new Double(height)/1000);
        optionPanel.add(heightField);
        
        l=new JLabel("Rotation (degree):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField rotationField = new JFormattedTextField();
        rotationField.setColumns(10);
        rotationField.setValue(new Double(Utils.getRotation(affineTrans))/Math.PI*180);
        optionPanel.add(rotationField);
        
        topLeftXField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == topLeftXField) {
                    double newValue = ((Number)topLeftXField.getValue()).doubleValue()*1000;
                    if (newValue != getTopLeftX()) {
                        setTopLeftX(newValue);
                        centerXField.setValue(new Double(getCenterXYPos().getX()/1000));
                    }
                }
            }
        });

        centerXField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == centerXField) {
                    double newValue = ((Number)centerXField.getValue()).doubleValue()*1000;
                    if (newValue != getCenterXYPos().getX()) {
//                        setTopLeftX(newValue-width/2);
                        setCenter(newValue,centerXYPos.getY());
                        topLeftXField.setValue(new Double(getTopLeftX()/1000));
                    }
                }
            }
        });
        
        topLeftYField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == topLeftYField) {
                    double newValue = ((Number)topLeftYField.getValue()).doubleValue()*1000;
                    if (newValue != getTopLeftY()) {
                        setTopLeftY(newValue);//recalculates center pos
                        centerYField.setValue(new Double(getCenterXYPos().getY()/1000));
                    }
                }
            }
        });

        centerYField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == centerYField) {
                    double newValue = ((Number)centerYField.getValue()).doubleValue()*1000;
                    if (newValue != getCenterXYPos().getY()) {
//                        setTopLeftY(newValue-height/2);
                        setCenter(centerXYPos.getX(),newValue);
                        topLeftYField.setValue(new Double(getTopLeftY())/1000);
                    }
                }
            }
        });
        
        widthField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == widthField) {
                    double newValue = ((Number)widthField.getValue()).doubleValue()*1000;
                    if (newValue != width) {
                        setWidth(newValue);
//                        centerXField.setValue(new Double(getCenterXYPos().getX()/1000));
                        topLeftXField.setValue(new Double(getTopLeftX()/1000));
                        topLeftYField.setValue(new Double(getTopLeftY()/1000));
                    }
                }
            }
        });

        heightField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == heightField) {
                    double newValue = ((Number)heightField.getValue()).doubleValue()*1000;
                    if (newValue != height) {
                        setHeight(newValue);
//                        centerYField.setValue(new Double(getCenterXYPos().getY()/1000));
                        topLeftXField.setValue(new Double(getTopLeftX()/1000));
                        topLeftYField.setValue(new Double(getTopLeftY()/1000));
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
                        setAffineTransform(Utils.createRotationAffineTrans(newValue));
                        topLeftXField.setValue(new Double(getTopLeftX()/1000));
                        topLeftYField.setValue(new Double(getTopLeftY()/1000));
                    }
                }
            }
        });
        
        int result;
        do {
            result = JOptionPane.showConfirmDialog(null, optionPanel, 
                getShapeType()+": Configuration", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION && !isInsideRect(layoutBounds)) {
                JOptionPane.showMessageDialog(null,"The area does not fit into the layout.");
            }
        } while (result == JOptionPane.OK_OPTION && !isInsideRect(layoutBounds));

        if (result == JOptionPane.CANCEL_OPTION) {
            return null;
        } else {
            setName((String)nameField.getValue());
//            setTopLeft(((Number)topLeftXField.getValue()).doubleValue()*1000, ((Number)topLeftYField.getValue()).doubleValue()*1000);
            centerXYPos=new Point2D.Double(
                    ((Number)centerXField.getValue()).doubleValue()*1000, 
                    ((Number)centerYField.getValue()).doubleValue()*1000);
            //shape and generalPath will be recalculated automatically
            setWidthAndHeight(
                    ((Number)widthField.getValue()).doubleValue()*1000,
                    ((Number)heightField.getValue()).doubleValue()*1000);
            setRelativeZPos(((Number)zField.getValue()).doubleValue()*1000);
            return this;
        }            
    }
    
}


