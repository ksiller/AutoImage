package autoimage;

import ij.IJ;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten
 */
public class FieldOfView {
    
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
   // protected Rectangle roi_Pixel_bin;
    protected Rectangle unbinnedRoi_Pixel;

        
    public FieldOfView(int pixX, int pixY, double fieldRot) {
        this(pixX,pixY,null,fieldRot);
    }

    public FieldOfView(int pixX, int pixY) {
        this(pixX, pixY, null, ROTATION_UNKNOWN);
    }
    
    public FieldOfView (int fullPixX, int fullPixY, Rectangle unbinnedRoi_Pix, double fieldRot) {
        fullChipWidth_Pixel=fullPixX;
        fullChipHeight_Pixel=fullPixY;
        fieldRotation=fieldRot;
        if (unbinnedRoi_Pix==null) {
            clearROI();
        } else {
            unbinnedRoi_Pixel=unbinnedRoi_Pix;
        }
    }
    
    //loads Roi for field of view from file. 
    public FieldOfView(JSONObject fovObj) throws JSONException {
        this(0,0,0);
        int x=fovObj.getInt(TAG_ROI_X_PIXEL);
        int y=fovObj.getInt(TAG_ROI_Y_PIXEL);
        int width=fovObj.getInt(TAG_ROI_WIDTH_PIXEL);
        int height=fovObj.getInt(TAG_ROI_HEIGHT_PIXEL);
        unbinnedRoi_Pixel = new Rectangle(x,y,width,height);
    }
    
    public FieldOfView (FieldOfView fov) {
        this (fov.fullChipWidth_Pixel,fov.fullChipHeight_Pixel, fov.unbinnedRoi_Pixel, fov.fieldRotation);
    }
    
    public JSONObject toJSONObject() throws JSONException {
        JSONObject fovObject = new JSONObject();
        fovObject.put(TAG_ROI_X_PIXEL, unbinnedRoi_Pixel!=null ? unbinnedRoi_Pixel.x : 0);
        fovObject.put(TAG_ROI_Y_PIXEL, unbinnedRoi_Pixel!=null ? unbinnedRoi_Pixel.y : 0);
        fovObject.put(TAG_ROI_WIDTH_PIXEL, unbinnedRoi_Pixel!=null ? unbinnedRoi_Pixel.width : fullChipWidth_Pixel);
        fovObject.put(TAG_ROI_HEIGHT_PIXEL, unbinnedRoi_Pixel!=null ? unbinnedRoi_Pixel.height : fullChipHeight_Pixel);
        return fovObject;
    }
    
    public double getFieldRotation() {
        return fieldRotation;
    }
    
    public void setFieldRotation(double angle) {
        fieldRotation=angle;
    }

    public double getFullWidth_UM(double objPixSize) {
        return fullChipWidth_Pixel*objPixSize;    
    }

    
    public double getFullHeight_UM(double objPixSize) {
        return fullChipHeight_Pixel*objPixSize;    
    }

    public void setFullWidth_Pixel(int pixX) {
        fullChipWidth_Pixel=pixX;
        //ensure that roi width <= full width
        if (unbinnedRoi_Pixel!=null)
            unbinnedRoi_Pixel.width=Math.min(pixX, unbinnedRoi_Pixel.width);
    }

    public void setFullHeight_Pixel(int pixY) {
        fullChipHeight_Pixel=pixY;    
         //ensure that roi height <= full height
        if (unbinnedRoi_Pixel!=null)
            unbinnedRoi_Pixel.height=Math.min(pixY, unbinnedRoi_Pixel.height);
    }

    public void setFullSize_Pixel(int width, int height) {
        setFullWidth_Pixel(width);
        setFullHeight_Pixel(height);
    }
    
    
    public double getRoiWidth_UM(double objPixSize) {
        if (unbinnedRoi_Pixel==null)
            return fullChipWidth_Pixel*objPixSize;
        return Math.min(fullChipWidth_Pixel,unbinnedRoi_Pixel.width)*objPixSize;
    }
    
    public double getRoiHeight_UM(double objPixSize) {
        if (unbinnedRoi_Pixel==null)
            return fullChipHeight_Pixel*objPixSize;
        return Math.min(fullChipHeight_Pixel,unbinnedRoi_Pixel.height)*objPixSize;
    }

    //roi in binned pixel dimensions as obtained by core.getROI()
    //binning used to project to unbinned roi
    public void setRoi_Pixel(Rectangle roi, int binning) {
        if (roi==null) {
            unbinnedRoi_Pixel=null;
            return;
        }
        unbinnedRoi_Pixel.x=Math.min(roi.x*binning,fullChipWidth_Pixel-1);
        unbinnedRoi_Pixel.y=Math.min(roi.y*binning,fullChipHeight_Pixel-1);
        setRoiWidth_Pixel(roi.width,binning);
        setRoiHeight_Pixel(roi.height,binning);
    }
  
    //roiWidth in binned pixel as obtained by core.getROI()
    //binning used to project to unbinned roi
    public void setRoiWidth_Pixel(int roiWidth, int binning) {
        if (roiWidth <= 0)
            return;
        if (unbinnedRoi_Pixel==null) {
            unbinnedRoi_Pixel=new Rectangle(0,0,Math.min(fullChipWidth_Pixel,roiWidth*binning),fullChipHeight_Pixel);
        } else
            unbinnedRoi_Pixel.width=Math.min(fullChipWidth_Pixel-unbinnedRoi_Pixel.x,roiWidth*binning);
    }

    //roiHeight in binned pixel as obtained by core.getROI()
    //binning used to project to unbinned roi
    public void setRoiHeight_Pixel(int roiHeight, int binning) {
        if (roiHeight <= 0)
            return;
        if (unbinnedRoi_Pixel==null)
            unbinnedRoi_Pixel=new Rectangle(0,0,fullChipWidth_Pixel,Math.min(fullChipHeight_Pixel, roiHeight*binning));
        else
            unbinnedRoi_Pixel.height=Math.min(fullChipHeight_Pixel-unbinnedRoi_Pixel.y,roiHeight*binning);
    }

    public Rectangle getROI_Pixel(int binning) {
        if (unbinnedRoi_Pixel == null)
            return null;
        else
            return new Rectangle(unbinnedRoi_Pixel.x/binning,
                    unbinnedRoi_Pixel.y/binning,
                    unbinnedRoi_Pixel.width/binning,
                    unbinnedRoi_Pixel.height/binning);
    }
    
    public void centerRoi() {
        if (unbinnedRoi_Pixel==null)
            return;
        unbinnedRoi_Pixel.x=(int)Math.round(((double)fullChipWidth_Pixel - unbinnedRoi_Pixel.width)/2);
        unbinnedRoi_Pixel.y=(int)Math.round(((double)fullChipHeight_Pixel - unbinnedRoi_Pixel.height)/2);
    }
    
    public boolean isFullSize() {
        return (unbinnedRoi_Pixel==null || (unbinnedRoi_Pixel.width == fullChipWidth_Pixel && unbinnedRoi_Pixel.height == fullChipHeight_Pixel));
    }
    
    
    public void clearROI() {
        unbinnedRoi_Pixel=new Rectangle(0,0,fullChipWidth_Pixel,fullChipHeight_Pixel);
    }
    
    //x-y dist of ROI origin from full chip origin in um
    public Point2D.Double getRoiOffset_UM(double objPixSize) {
        if (isFullSize()) {
            return new Point2D.Double(0,0);
        } else {    
            return new Point2D.Double(unbinnedRoi_Pixel.x*objPixSize,unbinnedRoi_Pixel.y*objPixSize);
        }
    }
    
    
}
