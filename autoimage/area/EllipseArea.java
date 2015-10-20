package autoimage.area;

import autoimage.api.SampleArea;
import autoimage.Tile;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 *
 * @author Karsten Siller
 */
public class EllipseArea extends TwoAxesArea {

    
    public EllipseArea() {
        super();
    }
    
    public EllipseArea(String n) { //expects name identifier
        super(n);
    }
    
    public EllipseArea(String n, int id, double ox, double oy, double oz, double w, double h, boolean selForAcq, String anot) {
        super(n,id,ox,oy,oz,w,h,selForAcq,anot);
    }

    @Override
    public String getShapeType() {
        return "Ellipse";
    }

    @Override
    public SampleArea duplicate() {
        EllipseArea newArea = new EllipseArea(this.getName());
        newArea.setId(this.getId());
//        newArea.setTopLeftX(this.topLeftX);
//        newArea.setTopLeftY(this.topLeftY);
        newArea.centerXYPos=new Point2D.Double(this.getCenterXYPos().getX(), this.getCenterXYPos().getY());
        newArea.setRelativeZPos(this.relativeZPos);
        newArea.setWidth(this.width);
        newArea.setHeight(this.height);
        newArea.affineTrans=new AffineTransform(this.affineTrans);
        newArea.setSelectedForAcq(isSelectedForAcq());
        newArea.setSelectedForMerge(isSelectedForMerge());
        newArea.setComment(this.comment);
        newArea.setAcquiring(this.acquiring);
//        newArea.setTilingSetting(this.tiling.duplicate());
        newArea.tilePosList=new ArrayList<Tile>(this.getTilePositions());
        newArea.setUnknownTileNum(this.hasUnknownTileNum());
        newArea.createShape();
        newArea.setRelDefaultPos();
        newArea.createGeneralPath();
        return newArea;
    }

    @Override
    public int supportedLayouts() {
        return SampleArea.SUPPORT_CUSTOM_LAYOUT;
    }

    @Override
    public void createShape() {
        shape=new Path2D.Double(new Ellipse2D.Double(-width/2, -height/2, width, height));
    }
    
}
