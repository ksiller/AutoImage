package autoimage;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten Siller
 */
public class FieldOfView implements Shape {
    
//    protected final static String TAG_FULL_WIDTH_PIXEL = "FOV_WIDTH_PIXEL";
//    protected final static String TAG_FULL_HEIGHT_PIXEL = "FOV_HEIGHT_PIXEL";
    protected final static String TAG_ROI_X_PIXEL = "FOV_ROI_X";
    protected final static String TAG_ROI_Y_PIXEL = "FOV_ROI_Y";
    protected final static String TAG_ROI_WIDTH_PIXEL = "FOV_ROI_WIDTH";
    protected final static String TAG_ROI_HEIGHT_PIXEL = "FOV_ROI_HEIGHT";
    public final static double ROTATION_UNKNOWN = 10*Math.PI;
    
    protected int fullChipWidth_Pixel;  //camera: full chip pixel x
    protected int fullChipHeight_Pixel; //camera: full chip pixel y
    protected double fieldRotation; //radians
    protected AffineTransform fieldTransform;
    protected Path2D roiPath;
    protected Path2D fullChipPath;
    protected Path2D nativeRoiPath;
    protected Rectangle unbinnedRoi_Pixel;

    /**
     * 
     * @param fullPixX number of pixels on detector along x-axis
     * @param fullPixY number of pixels on detector along y-axis
     * @param fieldRot rotation of detector field-of-view relative to stage x-y coordinate system
     */    
    public FieldOfView(int fullPixX, int fullPixY, double fieldRot) {
        this(fullPixX,fullPixY,null,fieldRot);
    }

    
    /**
     * 
     * @param fullPixX number of pixels on detector along x-axis
     * @param fullPixY number of pixels on detector along y-axis 
     */
    public FieldOfView(int fullPixX, int fullPixY) {
        this(fullPixX, fullPixY, null, ROTATION_UNKNOWN);
    }
    
    
    /**
     * 
     * @param fullPixX number of pixels on detector along x-axis
     * @param fullPixY number of pixels on detector along y-axis
     * @param unbinnedRoi_Pix roi definition of detector in pixel coordinates
     * @param fieldRot rotation of detector field-of-view relative to stage x-y coordinate system
     */
    public FieldOfView (int fullPixX, int fullPixY, Rectangle unbinnedRoi_Pix, double fieldRot) {
//        fullChipWidth_Pixel=fullPixX;
//        fullChipHeight_Pixel=fullPixY;
        fieldRotation=fieldRot;
        setRoi_Pixel(unbinnedRoi_Pix, 1);
    }
    
    
    /**
     * Loads Roi for field of view from file. 
     * 
     * @param fovObj JSONObject containing fov properties
     */ 
    public FieldOfView(JSONObject fovObj) throws JSONException {
        this(0,0,0);
        int x=fovObj.getInt(TAG_ROI_X_PIXEL);
        int y=fovObj.getInt(TAG_ROI_Y_PIXEL);
        int width=fovObj.getInt(TAG_ROI_WIDTH_PIXEL);
        int height=fovObj.getInt(TAG_ROI_HEIGHT_PIXEL);
        fieldRotation=FieldOfView.ROTATION_UNKNOWN;
        unbinnedRoi_Pixel = new Rectangle(x,y,width,height);
    }
    
    
    /**
     * Initializes a new FieldOfView object with values of an existing FieldOfView object
     * @param fov the FieldOfView object that serves as template
     */
    public FieldOfView (FieldOfView fov) {
        this (fov.fullChipWidth_Pixel,
                fov.fullChipHeight_Pixel, 
                new Rectangle(fov.unbinnedRoi_Pixel), 
                fov.fieldRotation);
    }
    
    
    /**
     * Writes all relevant FieldOfView properties into a JSONObject
     * @return JSONObject containing properties that describe this object  
     * @throws JSONException 
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject fovObject = new JSONObject();
        fovObject.put(TAG_ROI_X_PIXEL, unbinnedRoi_Pixel!=null ? unbinnedRoi_Pixel.getX() : 0);
        fovObject.put(TAG_ROI_Y_PIXEL, unbinnedRoi_Pixel!=null ? unbinnedRoi_Pixel.getY() : 0);
        fovObject.put(TAG_ROI_WIDTH_PIXEL, unbinnedRoi_Pixel!=null ? unbinnedRoi_Pixel.getWidth() : fullChipWidth_Pixel);
        fovObject.put(TAG_ROI_HEIGHT_PIXEL, unbinnedRoi_Pixel!=null ? unbinnedRoi_Pixel.getHeight() : fullChipHeight_Pixel);
        return fovObject;
    }
    
    
    /**
     * Returns this detector's rotation angle relative to x-y stage coordinate system
     * @return rotation angle (in rad)
     */
    public double getFieldRotation() {
        return fieldRotation;
    }
    
    
    /**
     * sets this detector's rotation angle relative to to x-y stage coordinate system
     * @param angle rotation angle (in rad)
     */
    public void setFieldRotation(double angle) {
        fieldRotation=angle;
    }

    
    /**
     * Converts the field-of-view x-axis dimension from pixel into microns.
     * 
     * @param objPixSize The pixel size, e.g. based on chosen objective
     * @return Width of the full chip's field-of-view in micron 
     */
/*    public double getFullWidth_UM(double objPixSize) {
        return fullChipWidth_Pixel*objPixSize;    
    }*/

    
    /**
     * Converts the field-of-view y-axis dimension from pixel into microns.
     * 
     * @param objPixSize The pixel size, e.g. based on chosen objective
     * @return Width of the full chip's field-of-view in micron 
     */
/*    public double getFullHeight_UM(double objPixSize) {
        return fullChipHeight_Pixel*objPixSize;    
    }*/

    
    /** 
     * Set the full width of the field-of-view in pixel dimension.
     * 
     * @param fullPixX number of pixels along x-axis
     */
/*    public void setFullWidth_Pixel(int fullPixX) {
        fullChipWidth_Pixel=fullPixX;
        //ensure that roi width <= full width
        if (unbinnedRoi_Pixel!=null) {
            unbinnedRoi_Pixel.width=Math.min(fullPixX, unbinnedRoi_Pixel.width);
        }    
    }*/

    
    /** 
     * Set the full height of the field-of-view in pixel dimension.
     * 
     * @param fullPixY number of pixels along y-axis
     */
/*    public void setFullHeight_Pixel(int fullPixY) {
        fullChipHeight_Pixel=fullPixY;    
         //ensure that roi height <= full height
        if (unbinnedRoi_Pixel!=null) {
            unbinnedRoi_Pixel.height=Math.min(fullPixY, unbinnedRoi_Pixel.height);
        }    
    }*/

    
    /** 
     * Set the full width and height of the field-of-view in pixel dimension.
     * 
     * @param fullPixX number of pixels along x-axis
     * @param fullPixY number of pixels along y-axis
     */
/*    public void setFullSize_Pixel(int fullPixX, int fullPixY) {
        setFullWidth_Pixel(fullPixX);
        setFullHeight_Pixel(fullPixY);
    }*/
    
    
    /**
     * Converts the current ROI width from pixel into microns.
     * 
     * @param objPixSize The pixel size, e.g. based on chosen objective
     * @return Width of the ROI's in micron 
     */    
    public double getRoiWidth_UM(double objPixSize) {
/*        if (unbinnedRoi_Pixel==null)
            return fullChipWidth_Pixel*objPixSize;
        return Math.min(fullChipWidth_Pixel,unbinnedRoi_Pixel.getWidth())*objPixSize;*/
        return unbinnedRoi_Pixel.width * objPixSize;
    }
    
    
    /**
     * Converts the current ROI width from pixel into microns.
     * 
     * @param objPixSize The pixel size, e.g. based on chosen objective
     * @return Width of the ROI's in micron 
     */    
    public double getRoiHeight_UM(double objPixSize) {
/*        if (unbinnedRoi_Pixel==null)
            return fullChipHeight_Pixel*objPixSize;
        return Math.min(fullChipHeight_Pixel,unbinnedRoi_Pixel.getHeight())*objPixSize;*/
        return unbinnedRoi_Pixel.height * objPixSize;
    }

    /**
     * Sets the unbinned ROI for this field-of-view. 
     * The binnedRoi rectangle corresponds to the Rectangle object obtained by core.getROI().  
     * 
     * @param binnedRoi Rectangle defining ROI in pixel coordinates after binning
     * @param binning binning factor; set to 1 if this is an unbinned ROI
     */
    public void setRoi_Pixel(Rectangle binnedRoi, int binning) {
        if (binnedRoi==null) {
            unbinnedRoi_Pixel=new Rectangle(0,0,0,0);
            return;
        } else {
            unbinnedRoi_Pixel=Utils.scaleRoi(binnedRoi, binning);
        }
        this.fullChipWidth_Pixel=unbinnedRoi_Pixel.width;
        this.fullChipHeight_Pixel=unbinnedRoi_Pixel.height;
    }
  

    /**
     * Calculates the Roi rectangle (in pixel) for a particular binning condition.
     * 
     * @param binning the binning factor
     * @return Rectangle in pixel dimension describing the roi with consideration of the passed binning factor
     */
    public Rectangle getROI_Pixel(int binning) {
        if (unbinnedRoi_Pixel == null) {
            return null;
        } else {
            return new Rectangle(unbinnedRoi_Pixel.x/binning,
                    unbinnedRoi_Pixel.y/binning,
                    unbinnedRoi_Pixel.width/binning,
                    unbinnedRoi_Pixel.height/binning);
        }    
    }
    
    
    /**
     * Retains the current roi width and height but translates the roi's origin (top left corner) such that the Roi is centered on the chip. 
     */
    public void centerRoi() {
        if (unbinnedRoi_Pixel==null)
            return;
        unbinnedRoi_Pixel.x=(int)Math.round(((double)fullChipWidth_Pixel - unbinnedRoi_Pixel.width)/2);
        unbinnedRoi_Pixel.y=(int)Math.round(((double)fullChipHeight_Pixel - unbinnedRoi_Pixel.height)/2);
    }
    
    /**
     * 
     * @return True if no roi is set or the roi's width and height equal width and height of this full size field-of-view  
     */
/*    public boolean isFullSize() {
        return (unbinnedRoi_Pixel==null || (unbinnedRoi_Pixel.width == fullChipWidth_Pixel && unbinnedRoi_Pixel.height == fullChipHeight_Pixel));
    }*/
    
    
    /**
     * Sets the roi such that its width and height corresponds to this full size field-of-view
     */
/*    public void clearROI() {
        unbinnedRoi_Pixel=new Rectangle(0,0,fullChipWidth_Pixel,fullChipHeight_Pixel);
    }*/
    
/*    //x-y dist of ROI origin from full chip origin in um
    public Point2D.Double getRoiOffset_UM(double objPixSize) {
        if (isFullSize()) {
            return new Point2D.Double(0,0);
        } else {    
            return new Point2D.Double(unbinnedRoi_Pixel.x*objPixSize,unbinnedRoi_Pixel.y*objPixSize);
        }
    }*/
    
/*    public boolean isCentered() {
        if (isFullSize()) {
            return true;
        }
        return ((2*unbinnedRoi_Pixel.x+unbinnedRoi_Pixel.width == fullChipWidth_Pixel) 
            && (2*unbinnedRoi_Pixel.y+unbinnedRoi_Pixel.height == fullChipHeight_Pixel)); 
    }
    */
    public void createRoiPath(double objPixSize) {
        if (objPixSize==-1) {
                roiPath=new Path2D.Double(new Rectangle2D.Double(0,0,0,0));
                nativeRoiPath=new Path2D.Double(new Rectangle2D.Double(0,0,0,0));
        } else {
            //translate roi to center
            AffineTransform effectiveTrans=AffineTransform.getScaleInstance(objPixSize, objPixSize);
            if (fieldRotation != ROTATION_UNKNOWN) {
    //            effectiveTrans.concatenate(AffineTransform.getTranslateInstance(fullChipWidth_Pixel/2, fullChipHeight_Pixel/2));
                effectiveTrans.concatenate(AffineTransform.getRotateInstance(-fieldRotation));
            }
            effectiveTrans.concatenate(AffineTransform.getTranslateInstance(-fullChipWidth_Pixel/2, -fullChipHeight_Pixel/2));
            roiPath=new Path2D.Double(effectiveTrans.createTransformedShape(unbinnedRoi_Pixel));

            AffineTransform nativeTrans=AffineTransform.getScaleInstance(objPixSize, objPixSize);
            nativeTrans.concatenate(AffineTransform.getTranslateInstance(-fullChipWidth_Pixel/2, -fullChipHeight_Pixel/2));
            nativeRoiPath=new Path2D.Double(nativeTrans.createTransformedShape(unbinnedRoi_Pixel));
        }
    }

/*    public void createFullChipPath(double objPixSize) {
        if (objPixSize==-1) {
                fullChipPath=new Path2D.Double(new Rectangle2D.Double(0,0,0,0));
        } else {
            //translate roi to center
            AffineTransform at=AffineTransform.getScaleInstance(objPixSize, objPixSize);
            if (fieldRotation != ROTATION_UNKNOWN) {
    //            effectiveTrans.concatenate(AffineTransform.getTranslateInstance(fullChipWidth_Pixel/2, fullChipHeight_Pixel/2));
                at.concatenate(AffineTransform.getRotateInstance(-fieldRotation));
            }
            fullChipPath=new Path2D.Double(at.createTransformedShape(new Rectangle2D.Double(
                    -fullChipWidth_Pixel/2, 
                    -fullChipHeight_Pixel/2,
                    fullChipWidth_Pixel, 
                    fullChipHeight_Pixel)));
        }
    }
*/
    @Override
    public Rectangle getBounds() {
        return roiPath.getBounds();
    }

    @Override
    public Rectangle2D getBounds2D() {
        return roiPath.getBounds2D();
    }

    @Override
    public boolean contains(double x, double y) {
        return roiPath.contains(x, y);
    }

    @Override
    public boolean contains(Point2D p) {
        return roiPath.contains(p);
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return roiPath.intersects(x, y, w, h);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return roiPath.intersects(r);
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return roiPath.contains(x, y, w, h);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return roiPath.contains(r);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return roiPath.getPathIterator(at);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return roiPath.getPathIterator(at, flatness);
    }
    
    public Path2D getEffectiveRoiPath() {
        return roiPath;
    }

/*    public Path2D getFullChipPath() {
        return fullChipPath;
    }*/

    public Path2D getNativeRoiPath() {
        return nativeRoiPath;
    }
}
