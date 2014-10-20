/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import static autoimage.Area.COLOR_ACQUIRING_AREA;
import static autoimage.Area.COLOR_AREA_BORDER;
import static autoimage.Area.COLOR_MERGE_AREA_BORDER;
import static autoimage.Area.COLOR_SELECTED_AREA_BORDER;
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
    @Override
    public boolean setAreaParams (List<String> params) {
         if (/*super.setAreaParams(params) && */params.size()>=8) {
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
        g2d.fillOval(x,y,w,h); 
        if (selectedForMerge)
            g2d.setColor(COLOR_MERGE_AREA_BORDER);
        else {
            if (selectedForAcq)
                g2d.setColor(COLOR_SELECTED_AREA_BORDER);
            else    
                g2d.setColor(COLOR_AREA_BORDER);
        }
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
    public boolean isInsideRect(Rectangle2D.Double r) { //checks if entire area is inside rectangle
        return ((topLeftX>r.x) && (topLeftX+width<r.x+r.width) && (topLeftY>r.y) && (topLeftY+height<r.y+r.height));
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
        return newArea;
    }

    @Override
    protected void initializeFromJSONObject(JSONObject obj) throws JSONException {
    }

    @Override
    protected void addFieldsToJSONObject(JSONObject obj) throws JSONException {
    }

}
