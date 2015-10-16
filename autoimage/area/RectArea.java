package autoimage.area;

import autoimage.api.SampleArea;
import autoimage.Tile;
import java.awt.GridLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
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
public class RectArea extends SampleArea{
    

    public RectArea() {
        super();
    }
    
    public RectArea(String n) { //expects name identifier
        super(n);
    }
    
    
    public RectArea(String n, int id, double ox, double oy, double oz, double w, double h, boolean s, String anot) {
        super(n,id,ox,oy,oz,w,h,s,anot);
    }
           
    @Override
    public String getShapeType() {
        return "Rectangle";
    }

    @Override
    public boolean isInArea(double x, double y) {//checks of coordinate is inside area
        return ((x>=topLeftX) & (x<topLeftX+width) & (y>=topLeftY) & (y<topLeftY+height));
    }

    @Override
    public boolean isFovInsideArea(double x, double y, double fovX, double fovY) {//checks of coordinate is inside area
        Rectangle2D.Double area = new Rectangle2D.Double(topLeftX,topLeftY,width,height);
        return area.contains(x-fovX/2,y-fovY/2,fovX,fovY);
    }

    @Override
    public boolean doesFovTouchArea(double x, double y, double fovX, double fovY) {//checks of coordinate is inside area
        Rectangle2D.Double area = new Rectangle2D.Double(topLeftX,topLeftY,width,height);
        return area.intersects(x-fovX/2,y-fovY/2,fovX,fovY);
    }

    @Override
    public boolean isInsideRect(Rectangle2D r) { //checks if entire area is inside rectangle
        return ((topLeftX>=r.getX()) && (topLeftX+width<=r.getX()+r.getWidth()) && (topLeftY>=r.getY()) && (topLeftY+height<=r.getY()+r.getHeight()));
    } 

    @Override
    public SampleArea duplicate() {
        RectArea newArea = new RectArea(this.getName());
        newArea.setId(this.getId());
        newArea.setTopLeftX(this.topLeftX);
        newArea.setTopLeftY(this.topLeftY);
        newArea.setRelativeZPos(this.relativeZPos);
        newArea.setWidth(this.width);
        newArea.setHeight(this.height);
        newArea.affineTrans=new AffineTransform(this.affineTrans);
        newArea.setSelectedForAcq(isSelectedForAcq());
        newArea.setSelectedForMerge(isSelectedForMerge());
        newArea.setComment(this.comment);
        newArea.setAcquiring(this.acquiring);
        newArea.tilePosList=new ArrayList<Tile>(this.getTilePositions());
        newArea.setUnknownTileNum(this.hasUnknownTileNum());
        return newArea;
    }

    @Override
    protected void initializeFromJSONObject(JSONObject obj) throws JSONException {
    }

    @Override
    protected void addFieldsToJSONObject(JSONObject obj) throws JSONException {
    }

    @Override
    public void calcCenterAndDefaultPos() {
        centerXYPos=new Point2D.Double(topLeftX+width/2,topLeftY+height/2);
        defaultXYPos=centerXYPos;
    }

    @Override
    public List<Point2D> getOutlinePoints() {
        List<Point2D> list=new ArrayList<Point2D>(4);
        list.add(new Point2D.Double(topLeftX, topLeftY));
        list.add(new Point2D.Double(topLeftX+width, topLeftY));
        list.add(new Point2D.Double(topLeftX+width, topLeftY+height));
        list.add(new Point2D.Double(topLeftX, topLeftY+height));
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
        zField.setValue(new Double(relativeZPos/1000));
        optionPanel.add(zField);
             
        l=new JLabel("Origin X (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField topLeftXField = new JFormattedTextField();
        topLeftXField.setColumns(10);
        topLeftXField.setValue(new Double(topLeftX/1000));
        optionPanel.add(topLeftXField);
        
        l=new JLabel("Center X (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField centerXField = new JFormattedTextField();
        centerXField.setColumns(10);
        centerXField.setValue(new Double(getCenterXYPos().getX()/1000));
        optionPanel.add(centerXField);

        l=new JLabel("Origin Y (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField topLeftYField = new JFormattedTextField();
        topLeftYField.setColumns(10);
        topLeftYField.setValue(new Double(topLeftY)/1000);
        optionPanel.add(topLeftYField);
        
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
        
        topLeftXField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == topLeftXField) {
                    double newValue = ((Number)topLeftXField.getValue()).doubleValue()*1000;
                    if (newValue != topLeftX) {
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
                        setTopLeftX(newValue-width/2);
                        topLeftXField.setValue(new Double(topLeftX/1000));
                    }
                }
            }
        });
        
        topLeftYField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == topLeftYField) {
                    double newValue = ((Number)topLeftYField.getValue()).doubleValue()*1000;
                    if (newValue != topLeftY) {
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
                        setTopLeftY(newValue-height/2);
                        topLeftYField.setValue(new Double(topLeftY)/1000);
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
                        centerXField.setValue(new Double(getCenterXYPos().getX()/1000));
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
                        centerYField.setValue(new Double(getCenterXYPos().getY()/1000));
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
            setTopLeft(((Number)topLeftXField.getValue()).doubleValue()*1000, ((Number)topLeftYField.getValue()).doubleValue()*1000);
            setWidth(((Number)widthField.getValue()).doubleValue()*1000);
            setHeight(((Number)heightField.getValue()).doubleValue()*1000);
            //center pos will be set automatically
            setRelativeZPos(((Number)zField.getValue()).doubleValue()*1000);
            return this;
        }            
    }

    @Override
    public int supportedLayouts() {
        return SampleArea.SUPPORT_CUSTOM_LAYOUT;
    }

    @Override
    protected void createShape() {
        shape=new Rectangle2D.Double(centerXYPos.getX()-width/2, centerXYPos.getY()-height/2, width, height);
    }
    
}

