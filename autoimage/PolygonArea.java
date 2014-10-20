/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten
 */
public class PolygonArea extends Area {

    private List<Point2D> points;
    private Path2D polygon;
    
    protected final static String TAG_POINT_ARRAY = "POINT_ARRAY";
//    protected final static String TAG_POINT_OBJECT = "POINT_OBJECT";
    protected final static String TAG_COORD_X = "COORD_X";
    protected final static String TAG_COORD_Y = "COORD_Y";
    protected final static String TAG_NUMBER = "NUMBER";
    
    public PolygonArea() {
        super();
    }
    
    public PolygonArea(String n) { //expects name identifier
        super(n);
    }
    
    public PolygonArea(String n, int id, double ox, double oy, double oz, List<Point2D> pList, boolean s, String anot) {
        super(n,id,ox,oy,oz,0.0,0.0,s,anot);
        createArea(pList, ox, oy);
    }
        
    private void createArea(List<Point2D> pList, double offsetX, double offsetY) {
        if (pList!=null) {
            points=new ArrayList<Point2D>(pList.size());
            polygon=new Path2D.Double();
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
        } else {
            polygon=null;
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
    public String getShape() {
        return "Polygon";
    }

    @Override
    public double getCenterX() {
        return polygon.getBounds2D().getWidth()/2;
    }

    @Override
    public double getCenterY() {
        return polygon.getBounds2D().getHeight()/2;
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
        AffineTransform at=new AffineTransform();
        at.scale(physToPixelRatio,physToPixelRatio);
        at.translate(bdPix, bdPix);
        Shape polyShape=polygon.createTransformedShape(at);
        g2d.fill(polyShape); 
        if (selectedForMerge)
            g2d.setColor(COLOR_MERGE_AREA_BORDER);
        else {
            if (selectedForAcq)
                g2d.setColor(COLOR_SELECTED_AREA_BORDER);
            else    
                g2d.setColor(COLOR_AREA_BORDER);
        }    
        g2d.draw(polyShape); 
    }

    @Override
    public void drawTiles(Graphics2D g2d, int bdPix, double physToPixelRatio, double fovX, double fovY, TilingSetting setting) {
        drawTileByTileOvl(g2d, bdPix, physToPixelRatio, fovX, fovY, setting);
    }

    @Override
    public boolean isInArea(double x, double y) {
        return polygon.contains(x,y);
    }

    @Override
    public boolean isFovInsideArea(double centerX, double centerY, double fovWidth, double fovHeight) {
        return polygon.contains(centerX-fovWidth/2, centerY-fovHeight/2, fovWidth, fovHeight);
    }

    @Override
    public boolean doesFovTouchArea(double centerX, double centerY, double fovWidth, double fovHeight) {
        return polygon.intersects(centerX-fovWidth/2, centerY-fovHeight/2, fovWidth, fovHeight);
    }

    @Override
    public boolean isInsideRect(Rectangle2D.Double r) {
        return r.contains(polygon.getBounds2D());
    }

    @Override
    public Area duplicate() {
        PolygonArea newArea = new PolygonArea(this.getName());
//        newArea.shape=this.getShape();
        newArea.setId(this.getId());
/*        newArea.setTopLeftX(this.topLeftX);
        newArea.setTopLeftY(this.topLeftY);
        newArea.setRelPosZ(this.relPosZ);
        newArea.setWidth(this.width);
        newArea.setHeight(this.height);
*/
        newArea.createArea(points,0,0);
        newArea.setSelectedForAcq(isSelectedForAcq());
        newArea.setSelectedForMerge(isSelectedForMerge());
        newArea.setComment(this.comment);
        newArea.setAcquiring(this.acquiring);
//        newArea.setTilingSetting(this.tiling.duplicate());
        newArea.tilePosList=new ArrayList<Tile>(this.getTilePositions());
        return newArea;
    }
    
}
