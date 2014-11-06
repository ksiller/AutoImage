/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten
 */
public class CompoundArea extends Area {
    
    private List<java.awt.geom.Area> subtractAreas;
    private java.awt.geom.Area area;

    private void createArea(List<Shape> addShapes, List<Shape> subtractShapes) {
        if (addShapes!=null && addShapes.size() > 0) {
            area=new java.awt.geom.Area(addShapes.get(0));
            for (int i=1; i<addShapes.size(); i++) {
                area.add(new java.awt.geom.Area(addShapes.get(i)));
            }
            subtractAreas=new ArrayList<java.awt.geom.Area>(subtractShapes.size());
            for (Shape subtractShape : subtractShapes) {
                java.awt.geom.Area subArea=new java.awt.geom.Area(subtractShape);
                subtractAreas.add(subArea);
                area.subtract(subArea);
            }
        }
    }
    
    
    @Override
    protected void initializeFromJSONObject(JSONObject obj) throws JSONException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void addFieldsToJSONObject(JSONObject obj) throws JSONException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getShape() {
        return "Compound";
    }
/*
    @Override
    public double getCenterX() {
        return area.getBounds2D().getWidth()/2;
    }

    @Override
    public double getCenterY() {
        return area.getBounds2D().getHeight()/2;
    }
*/
    @Override
    public void drawArea(Graphics2D g2d, int bdPix, double physToPixelRatio) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void drawTiles(Graphics2D g2d, int bdPix, double physToPixelRatio, double fovX, double fovY, TilingSetting setting) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isInArea(double x, double y) {
        boolean inArea=false;
        if (area.contains(x,y)) {
            if (subtractAreas!=null) {
                for (java.awt.geom.Area subArea:subtractAreas) {
                    if (subArea.contains(x,y)) {
                        break;
                    }
                }
                inArea=true;
            } else {
                inArea=true;
            }
        }
        return inArea;
    }

    @Override
    public boolean isFovInsideArea(double centerX, double centerY, double fovWidth, double fovHeight) {
        boolean inArea=false;
        Rectangle2D fov = new Rectangle2D.Double(centerX-fovWidth/2, centerY-fovHeight/2, fovWidth, fovHeight);
        if (area.contains(fov)) {
            if (subtractAreas!=null) {
                for (java.awt.geom.Area subArea:subtractAreas) {
                    if (subArea.contains(fov)) {
                        break;
                    }
                }
                inArea=true;
            } else {
                inArea=true;
            }
        }
        return inArea;
    }

    @Override
    public boolean doesFovTouchArea(double centerX, double centerY, double fovWidth, double fovHeight) {
        boolean inArea=false;
        Rectangle2D fov = new Rectangle2D.Double(centerX-fovWidth/2, centerY-fovHeight/2, fovWidth, fovHeight);
        if (area.intersects(fov)) {
            if (subtractAreas!=null) {
                for (java.awt.geom.Area subArea:subtractAreas) {
                    if (subArea.contains(fov)) {
                        break;
                    }
                }
                inArea=true;
            } else {
                inArea=true;
            }
        }
        return inArea;
    }

    @Override
    public boolean isInsideRect(Rectangle2D.Double r) {
        return r.contains(area.getBounds2D());
    }

    @Override
    public Area duplicate() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Point2D calculateCenterPos() {
        return new Point2D.Double(topLeftX+width/2,topLeftY+height/2);
    }

    @Override
    public Point2D calculateDefaultPos() {
        return new Point2D.Double(topLeftX+width/2,topLeftY+height/2);
    }
    
}
