package autoimage.area;

import autoimage.api.BasicArea;
import autoimage.Tile;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

/**
 *
 * @author Karsten Siller
 */
public class RectArea extends TwoAxesArea{
    
    public RectArea() {
        super();
    }
    
    public RectArea(String name) {
        super(name);
    }
        
    public RectArea(String n, int id, double centerX, double centerY, double oz, double w, double h, boolean selForAcq, String anot) {
        super(n,id,centerX,centerY,oz,w,h,selForAcq,anot);
    }
           
    @Override
    public String getShapeType() {
        return "Rectangle";
    }
 
    @Override
    public BasicArea duplicate() {
        RectArea newArea = new RectArea(this.getName());
        newArea.setId(this.getId());
//        newArea.setTopLeftX(this.topLeftX);
//        newArea.setTopLeftY(this.topLeftY);
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
        newArea.setUnknownTileNum(this.hasUnknownTileNum());
        newArea.createShape();
        newArea.setRelDefaultPos();
        newArea.createGeneralPath();
        return newArea;
    }

    @Override
    public int supportedLayouts() {
        return BasicArea.SUPPORT_CUSTOM_LAYOUT;
    }

    @Override
    protected void createShape() {
        shape=new Path2D.Double(new Rectangle2D.Double(-width/2, -height/2, width, height));
    }
    
}

