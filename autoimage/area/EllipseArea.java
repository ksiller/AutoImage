package autoimage.area;

//import static autoimage.Area.COLOR_ACQUIRING_AREA;
//import static autoimage.Area.COLOR_AREA_BORDER;
//import static autoimage.Area.COLOR_MERGE_AREA_BORDER;
//import static autoimage.Area.COLOR_SELECTED_AREA_BORDER;
import autoimage.Tile;
import autoimage.TilingSetting;
import autoimage.area.Area;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.Ellipse2D;
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
 * @author Karsten
 */
class EllipseArea extends Area {

    public EllipseArea() {
        super();
    }
    
    public EllipseArea(String n) { //expects name identifier
        super(n);
    }
    
    public EllipseArea(String n, int id, double ox, double oy, double oz, double w, double h, boolean s, String anot) {
        super(n,id,ox,oy,oz,w,h,s,anot);
    }

    /*
    public EllipseArea duplicate(EllipseArea a) {
        EllipseArea newArea = new EllipseArea(a.getName());
        newArea.shape=a.getShape();
        newArea.id=a.getId();
        newArea.topLeftX=a.getTopLeftX();
        newArea.topLeftY=a.getTopLeftY();
        newArea.relPosZ=a.getRelPosZ();
        newArea.width=a.getWidth();
        newArea.height=a.getHeight();
        newArea.selectedForAcq=a.isSelectedForAcq();
        newArea.comment=a.getComment();
        newArea.acquiring=a.isAcquiring();
        newArea.tilingMode=a.getTilingMode();
        newArea.tileOverlap=a.tileOverlap;
        newArea.tilePosList=new ArrayList<Tile>(a.getTilePositions());
        return newArea;
    }*/

    
    @Override
    public String getShape() {
        return "Ellipse";
    }

/*@Override
    public String[] getXMLTags() {
        String[] s = new String[7];
        s[0]="SHAPE";
        s[1]="NAME";
        s[2]="WIDTH";
        s[3]="HEIGHT";
        s[4]="TOP_LEFT_X";
        s[5]="TOP_LEFT_Y";
        s[6]="REL_POS_Z";
        return s;
    }
*/  
    /*
    @Override
    public boolean setAreaParams (List<String> params) {
         //if (super.setAreaParams(params)) {
         if (params.size()>=8) {
//            shape=params.get(1);
            name=params.get(2);
            width=Double.parseDouble(params.get(3));
            height=Double.parseDouble(params.get(4));
            topLeftX=Double.parseDouble(params.get(5));
            topLeftY=Double.parseDouble(params.get(6));
            relPosZ=Double.parseDouble(params.get(7));
            selectedForAcq=Boolean.parseBoolean(params.get(8));
            return true;
        } else {
            name="undefined";
            width=1;
            height=1;
            topLeftX=0;
            topLeftY=0;
            relPosZ=0;
            selectedForAcq=false;
            return false;
        }
    }
    
    @Override
    protected Map<String,String> createParamHashMap() {
        Map<String,String> map = super.createParamHashMap();
        map.put(TAG_CLASS,this.getClass().getName());
        return map;
    }
*/

/*   @Override
    public double getCenterX () {
        return topLeftX+width/2;
    }
    
    @Override
    public double getCenterY () {
        return topLeftY+height/2;
    }
*/    
    @Override
    public void drawArea(Graphics2D g2d, int bdPix, double physToPixelRatio, boolean showZProfile) {
/*        if (acquiring) {
            g2d.setColor(COLOR_ACQUIRING_AREA);
        } else
            g2d.setColor(COLOR_UNSELECTED_AREA);
*/        
        g2d.setColor(getFillColor(showZProfile));
        int x = bdPix + (int) Math.round(topLeftX*physToPixelRatio);
        int y = bdPix + (int) Math.round(topLeftY*physToPixelRatio);
        int w = (int) Math.round(width*physToPixelRatio);
        int h = (int) Math.round(height*physToPixelRatio);    
        g2d.fillOval(x,y,w,h); 
        g2d.setColor(getBorderColor());
/*        
        if (selectedForMerge)
            g2d.setColor(COLOR_MERGE_AREA_BORDER);
        else {
            if (selectedForAcq)
                g2d.setColor(COLOR_SELECTED_AREA_BORDER);
            else    
                g2d.setColor(COLOR_AREA_BORDER);
        }*/
        g2d.draw(new Ellipse2D.Double(x, y, w,h)); 
    }
    
    
    @Override
    public void drawTiles(Graphics2D g2d, int bdPix, double physToPixelRatio, double fovX, double fovY, TilingSetting setting) {
        drawTileByTileOvl(g2d, bdPix, physToPixelRatio, fovX, fovY, setting);
    }
    
    
    @Override
    public boolean isInArea(double x, double y) {//checks of coordinate is inside area
        Ellipse2D.Double e=new Ellipse2D.Double(topLeftX, topLeftY, width, height);
        return e.contains(x,y);
    }

    @Override
    public boolean isFovInsideArea(double x, double y, double fovX, double fovY) {//checks of coordinate is inside area
        Ellipse2D.Double area = new Ellipse2D.Double(topLeftX,topLeftY,width,height);
        return area.contains(x-fovX/2,y-fovY/2,fovX,fovY);
    }

    @Override
    public boolean doesFovTouchArea(double x, double y, double fovX, double fovY) {//checks of coordinate is inside area
        Ellipse2D.Double area = new Ellipse2D.Double(topLeftX,topLeftY,width,height);
        return area.intersects(x-fovX/2,y-fovY/2,fovX,fovY);
    }

    @Override
    public boolean isInsideRect(Rectangle2D r) { //checks if entire area is inside rectangle
        return ((topLeftX>=r.getX()) && (topLeftX+width<=r.getX()+r.getWidth()) && (topLeftY>=r.getY()) && (topLeftY+height<=r.getY()+r.getHeight()));
    } 

    @Override
    public Area duplicate() {
        EllipseArea newArea = new EllipseArea(this.getName());
//        EllipseArea.shape=this.getShape();
        newArea.setId(this.getId());
        newArea.setTopLeftX(this.topLeftX);
        newArea.setTopLeftY(this.topLeftY);
        newArea.setRelPosZ(this.relPosZ);
        newArea.setWidth(this.width);
        newArea.setHeight(this.height);
        newArea.setSelectedForAcq(isSelectedForAcq());
        newArea.setSelectedForMerge(isSelectedForMerge());
        newArea.setComment(this.comment);
        newArea.setAcquiring(this.acquiring);
//        newArea.setTilingSetting(this.tiling.duplicate());
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

/*    
    @Override
    public void calculateCenterPos() {
        centerPos= new Point2D.Double(topLeftX+width/2,topLeftY+height/2);
    }

    @Override
    public void calculateDefaultPos() {
        if (centerPos==null)
            calculateCenterPos();
        defaultPos=centerPos;
    }
*/
    
    @Override
    public void calcCenterAndDefaultPos() {
        centerPos= new Point2D.Double(topLeftX+width/2,topLeftY+height/2);
        defaultPos=centerPos;
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
    public Area showConfigDialog(Rectangle2D bounds) {
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

        l=new JLabel("Relative Z (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        JFormattedTextField zField = new JFormattedTextField();
        zField.setColumns(10);
        zField.setValue(new Double(relPosZ / 1000));
        optionPanel.add(zField);
             
        l=new JLabel("Origin X (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField topLeftXField = new JFormattedTextField();
        topLeftXField.setColumns(10);
        topLeftXField.setValue(new Double(topLeftX / 1000));
        optionPanel.add(topLeftXField);
        
        l=new JLabel("Center X (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField centerXField = new JFormattedTextField();
        centerXField.setColumns(10);
        centerXField.setValue(new Double(getCenterPos().getX() / 1000));
        optionPanel.add(centerXField);

        l=new JLabel("Origin Y (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField topLeftYField = new JFormattedTextField();
        topLeftYField.setColumns(10);
        topLeftYField.setValue(new Double(topLeftY / 1000));
        optionPanel.add(topLeftYField);
        
        l=new JLabel("Center Y (mm):",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        final JFormattedTextField centerYField = new JFormattedTextField();
        centerYField.setColumns(10);
        centerYField.setValue(new Double(getCenterPos().getY() / 1000));
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
                
        topLeftXField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == topLeftXField) {
                    double newValue = ((Number)topLeftXField.getValue()).doubleValue() * 1000;
                    if (newValue != topLeftX) {
                        setTopLeftX(newValue);
                        centerXField.setValue(new Double(getCenterPos().getX() / 1000));
                    }
                }
            }
        });

        centerXField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == centerXField) {
                    double newValue = ((Number)centerXField.getValue()).doubleValue() * 1000;
                    if (newValue != getCenterPos().getX()) {
                        setTopLeftX(newValue-width/2);
                        topLeftXField.setValue(new Double(topLeftX / 1000));
                    }
                }
            }
        });
        
        topLeftYField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == topLeftYField) {
                    double newValue = ((Number)topLeftYField.getValue()).doubleValue() * 1000;
                    if (newValue != topLeftY) {
                        setTopLeftY(newValue);//recalculates center pos
                        centerYField.setValue(new Double(getCenterPos().getY() / 1000));
                    }
                }
            }
        });

        centerYField.addPropertyChangeListener("value",new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == centerYField) {
                    double newValue = ((Number)centerYField.getValue()).doubleValue() * 1000;
                    if (newValue != getCenterPos().getY()) {
                        setTopLeftY(newValue-height/2);
                        topLeftYField.setValue(new Double(topLeftY / 1000));
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
                        centerXField.setValue(new Double(getCenterPos().getX() / 1000));
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
                        centerYField.setValue(new Double(getCenterPos().getY() / 1000));
                    }
                }
            }
        });
        
        int result;
        do {
            result = JOptionPane.showConfirmDialog(null, optionPanel, 
                getShape()+": Configuration", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION && !isInsideRect(bounds)) {
                JOptionPane.showMessageDialog(null,"The area does not fit into the layout.");
            }
        } while (result == JOptionPane.OK_OPTION && !isInsideRect(bounds));

        if (result == JOptionPane.CANCEL_OPTION) {
            return null;
        } else {           
            setName((String)nameField.getValue());
            setTopLeft(((Number)topLeftXField.getValue()).doubleValue()*1000, ((Number)topLeftYField.getValue()).doubleValue()*1000);
            setWidth(((Number)widthField.getValue()).doubleValue()*1000);
            setHeight(((Number)heightField.getValue()).doubleValue()*1000);
            //center pos will be set automatically
            setRelPosZ(((Number)zField.getValue()).doubleValue()*1000);            
            return this;
        }
    }
    
    @Override
    public int supportedLayouts() {
        return Area.SUPPORT_CUSTOM_LAYOUT;
    }



}
