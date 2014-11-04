/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import static autoimage.Area.COLOR_ACQUIRING_AREA;
import static autoimage.Area.COLOR_AREA_BORDER;
import static autoimage.Area.COLOR_MERGE_AREA_BORDER;
import static autoimage.Area.COLOR_SELECTED_AREA_BORDER;
import static autoimage.Area.TAG_CLASS;
import ij.IJ;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten
 */
class DonutArea extends Area {
    private double ringWidth;
    
    public DonutArea() {
        super();
        ringWidth=0;
    }
    
    public DonutArea(String n) { //expects name identifier
        super(n);
        ringWidth=0;
    }
    
    public DonutArea(String n, int id, double ox, double oy, double oz, double w, double h, double ringWidth, boolean s, String anot) {
        super(n,id,ox,oy,oz,w,h,s,anot);
        this.ringWidth=ringWidth;
    }
    
    
    public void setRingWidth(double rw) {
        ringWidth=rw;
    }
    
    public double getRingWidth() {
        return ringWidth;
    }
    
    @Override
    public String getShape() {
        return "Donut";
    }
    
    
/*
@Override
    public String[] getXMLTags() {
        String[] s = new String[8];
        s[0]="SHAPE";
        s[1]="NAME";
        s[2]="WIDTH";
        s[3]="HEIGHT";
        s[4]="TOP_LEFT_X";
        s[5]="TOP_LEFT_Y";
        s[6]="REL_POS_Z";
        s[7]="RING_WIDTH";
        return s;
    }
   */    
    
    
    @Override
    public boolean setAreaParams (List<String> params) {
        if (/*super.setAreaParams(params) &&*/ params.size()>=9) {
//            shape=params.get(1);
            name=params.get(2);
            width=Double.parseDouble(params.get(3));
            height=Double.parseDouble(params.get(4));
            ringWidth=Double.parseDouble(params.get(5));
            topLeftX=Double.parseDouble(params.get(6));
            topLeftY=Double.parseDouble(params.get(7));
            relPosZ=Double.parseDouble(params.get(8));
            selectedForAcq=Boolean.parseBoolean(params.get(9));
            return true;
        } else {
//            shape=getShape();
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
        map.put(TAG_RING_WIDTH,Double.toString(ringWidth));
        return map;
    }
    
    @Override
    public void setAreaParams (Map<String,String> params) {
        if (params!=null) {
            super.setAreaParams(params);
            String s=params.get(TAG_RING_WIDTH);
            if (s!=null)
                ringWidth=Double.parseDouble(s);

        }
    }    
    
    @Override
    public void initializeFromJSONObject(JSONObject obj) throws JSONException {
//        IJ.log("initializing: "+this.getClass().getName());
//        super.initializeFromJSONObject(obj);
//      initialize 'Donut' specific fields, inherited fields initialized by parent class
        if (obj!=null)
            ringWidth=obj.getDouble(TAG_RING_WIDTH);
        else
            ringWidth=1;
    }

    @Override
    protected void addFieldsToJSONObject(JSONObject obj) throws JSONException {
        if (obj!=null)
            obj.put(TAG_RING_WIDTH,ringWidth);
    }
    
/*    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject obj=super.toJSONObject();
        if (obj!=null)
            obj.put(TAG_RING_WIDTH,ringWidth);
        return obj;
    }*/
    
    @Override
    public double getCenterX () {
        return topLeftX+width/2;
    }
    
    @Override
    public double getCenterY () {
        return topLeftY+height/2;
    }
    
    @Override
    public void drawArea(Graphics2D g2d, int bdPix, double physToPixelRatio) {
        if (acquiring) {
            g2d.setColor(COLOR_ACQUIRING_AREA);
        } else
            g2d.setColor(COLOR_UNSELECTED_AREA);
        int x = bdPix + (int) Math.round(topLeftX*physToPixelRatio);
        int y = bdPix + (int) Math.round(topLeftY*physToPixelRatio);
        int w = (int) Math.round(width*physToPixelRatio);
        int h = (int) Math.round(height*physToPixelRatio);  
        
        int holeX = bdPix + (int) Math.round((topLeftX+ringWidth)*physToPixelRatio);
        int holeY = bdPix + (int) Math.round((topLeftY+ringWidth)*physToPixelRatio);
        int holeWidth = (int) Math.round((width-2*ringWidth)*physToPixelRatio);
        int holeHeight = (int) Math.round((height-2*ringWidth)*physToPixelRatio);    

        java.awt.geom.Area donut = new java.awt.geom.Area(new Ellipse2D.Double(x, y, w, h));
        java.awt.geom.Area hole = new java.awt.geom.Area(new Ellipse2D.Double(holeX, holeY, holeWidth, holeHeight));
        donut.subtract(hole);
        g2d.fill(donut);
        if (selectedForMerge)
            g2d.setColor(COLOR_MERGE_AREA_BORDER);
        else
            if (selectedForAcq)
                g2d.setColor(COLOR_SELECTED_AREA_BORDER);
            else    
                g2d.setColor(COLOR_AREA_BORDER);
        g2d.draw(new Ellipse2D.Double(x, y, w,h)); 
        if (selectedForMerge)
            g2d.setColor(COLOR_MERGE_AREA_BORDER);
        else {
            if (selectedForAcq)
                g2d.setColor(COLOR_SELECTED_AREA_BORDER);
            else    
                g2d.setColor(COLOR_AREA_BORDER);
        }        
        g2d.draw(new Ellipse2D.Double(holeX, holeY, holeWidth, holeHeight)); 
    }
    
    
    @Override
    public void drawTiles(Graphics2D g2d, int bdPix, double physToPixelRatio, double fovX, double fovY, TilingSetting setting) {
        drawTileByTileOvl(g2d, bdPix, physToPixelRatio, fovX, fovY, setting);
    }
    
    //checks if coordinate is inside area
    @Override
    public boolean isInArea(double x, double y) {
        Ellipse2D.Double o=new Ellipse2D.Double(topLeftX, topLeftY, width, height);
        Ellipse2D.Double i=new Ellipse2D.Double(topLeftX+ringWidth, topLeftY+ringWidth, width-2*ringWidth, height-2*ringWidth);
        return o.contains(x,y) && !i.contains(x,y);
    }

    @Override
    public boolean isFovInsideArea(double x, double y, double fovX, double fovY) {//checks if FOV is inside area; x,y coordinates are cnter of FOV
        Ellipse2D.Double areaO = new Ellipse2D.Double(topLeftX,topLeftY,width,height);
        Ellipse2D.Double areaI = new Ellipse2D.Double(topLeftX+ringWidth, topLeftY+ringWidth, width-2*ringWidth, height-2*ringWidth);
        Rectangle2D.Double fov = new Rectangle2D.Double(x-fovX/2,y-fovY/2,fovX,fovY);
        return areaO.contains(fov) &&!areaI.intersects(fov);
    }
    
    //checks if field of view rectangle touches this area 
    @Override
    public boolean doesFovTouchArea(double x, double y, double fovX, double fovY) {//checks of coordinate is inside area
        Ellipse2D.Double areaO = new Ellipse2D.Double(topLeftX,topLeftY,width,height);
        Ellipse2D.Double areaI = new Ellipse2D.Double(topLeftX+ringWidth, topLeftY+ringWidth, width-2*ringWidth, height-2*ringWidth);
        Rectangle2D.Double fov = new Rectangle2D.Double(x-fovX/2,y-fovY/2,fovX,fovY);
        return areaO.intersects(fov) &&!areaI.contains(fov);
    }

    //checks if rectangle encloses this entire area
    @Override
    public boolean isInsideRect(Rectangle2D.Double r) { 
        return ((topLeftX>r.x) && (topLeftX+width<r.x+r.width) && (topLeftY>r.y) && (topLeftY+height<r.y+r.height));
    } 

    @Override
    public Area duplicate() {
        DonutArea newArea = new DonutArea(this.getName());
//        newArea.shape=this.getShape();
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
        newArea.setRingWidth(this.ringWidth);
        return newArea;
    }

}
