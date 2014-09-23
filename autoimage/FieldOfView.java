/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
    protected final static double ROTATION_UNKNOWN = 10*Math.PI;
    
    protected int fullChipWidth_Pixel;  //camera: full chip pixel x
    protected int fullChipHeight_Pixel; //camera: full chip pixel y
    protected double fieldRotation; //radians
   // protected Rectangle roi_Pixel_bin;
    protected Rectangle roi_Pixel_Unbinned;

        
    public FieldOfView(int pixX, int pixY, double fieldRot) {
        this(pixX,pixY,null,fieldRot);
    }

    public FieldOfView(int pixX, int pixY) {
        this(pixX, pixY, null, ROTATION_UNKNOWN);
    }
    
    public FieldOfView (int fullPixX, int fullPixY, Rectangle roiPixUnbinned, double fieldRot) {
        fullChipWidth_Pixel=fullPixX;
        fullChipHeight_Pixel=fullPixY;
        fieldRotation=fieldRot;
        roi_Pixel_Unbinned=roiPixUnbinned;
    }
    
    //loads Roi for field of view from file. 
    public FieldOfView(JSONObject fovObj) {
        this(0,0,0);
        try {
            int x=fovObj.getInt(TAG_ROI_X_PIXEL);
            int y=fovObj.getInt(TAG_ROI_Y_PIXEL);
            int width=fovObj.getInt(TAG_ROI_WIDTH_PIXEL);
            int height=fovObj.getInt(TAG_ROI_HEIGHT_PIXEL);
            roi_Pixel_Unbinned = new Rectangle(x,y,width,height);
        } catch (JSONException e) {
            IJ.log("Error loading field of view ROI from JSONObject");
        }
    }
    
    public FieldOfView (FieldOfView fov) {
        this (fov.fullChipWidth_Pixel,fov.fullChipHeight_Pixel, fov.roi_Pixel_Unbinned, fov.fieldRotation);
    }
    
    public JSONObject toJSONObject() throws JSONException {
        JSONObject fovObject = new JSONObject();
        fovObject.put(TAG_ROI_X_PIXEL, roi_Pixel_Unbinned!=null ? roi_Pixel_Unbinned.x : 0);
        fovObject.put(TAG_ROI_Y_PIXEL, roi_Pixel_Unbinned!=null ? roi_Pixel_Unbinned.y : 0);
        fovObject.put(TAG_ROI_WIDTH_PIXEL, roi_Pixel_Unbinned!=null ? roi_Pixel_Unbinned.width : fullChipWidth_Pixel);
        fovObject.put(TAG_ROI_HEIGHT_PIXEL, roi_Pixel_Unbinned!=null ? roi_Pixel_Unbinned.height : fullChipHeight_Pixel);
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
        if (roi_Pixel_Unbinned!=null)
            roi_Pixel_Unbinned.width=Math.min(pixX, roi_Pixel_Unbinned.width);
    }

    public void setFullHeight_Pixel(int pixY) {
        fullChipHeight_Pixel=pixY;    
         //ensure that roi height <= full height
        if (roi_Pixel_Unbinned!=null)
            roi_Pixel_Unbinned.height=Math.min(pixY, roi_Pixel_Unbinned.height);
    }

    public void setFullSize_Pixel(int width, int height) {
        setFullWidth_Pixel(width);
        setFullHeight_Pixel(height);
    }
    
    
    public double getRoiWidth_UM(double objPixSize) {
        if (roi_Pixel_Unbinned==null)
            return fullChipWidth_Pixel*objPixSize;
        return Math.min(fullChipWidth_Pixel,roi_Pixel_Unbinned.width)*objPixSize;
    }
    
    public double getRoiHeight_UM(double objPixSize) {
        if (roi_Pixel_Unbinned==null)
            return fullChipHeight_Pixel*objPixSize;
        return Math.min(fullChipHeight_Pixel,roi_Pixel_Unbinned.height)*objPixSize;
    }

    //roi in binned pixel dimensions as obtained by core.getROI()
    //binning used to project to unbinned roi
    public void setRoi_Pixel(Rectangle roi, int binning) {
        if (roi==null) {
            roi_Pixel_Unbinned=null;
            return;
        }
        roi_Pixel_Unbinned.x=Math.min(roi.x*binning,fullChipWidth_Pixel-1);
        roi_Pixel_Unbinned.y=Math.min(roi.y*binning,fullChipHeight_Pixel-1);
        setRoiWidth_Pixel(roi.width,binning);
        setRoiHeight_Pixel(roi.height,binning);
    }
  
    //roiWidth in binned pixel as obtained by core.getROI()
    //binning used to project to unbinned roi
    public void setRoiWidth_Pixel(int roiWidth, int binning) {
        if (roiWidth <= 0)
            return;
        if (roi_Pixel_Unbinned==null) {
            roi_Pixel_Unbinned=new Rectangle(0,0,Math.min(fullChipWidth_Pixel,roiWidth*binning),fullChipHeight_Pixel);
        } else
            roi_Pixel_Unbinned.width=Math.min(fullChipWidth_Pixel-roi_Pixel_Unbinned.x,roiWidth*binning);
    }

    //roiHeight in binned pixel as obtained by core.getROI()
    //binning used to project to unbinned roi
    public void setRoiHeight_Pixel(int roiHeight, int binning) {
        if (roiHeight <= 0)
            return;
        if (roi_Pixel_Unbinned==null)
            roi_Pixel_Unbinned=new Rectangle(0,0,fullChipWidth_Pixel,Math.min(fullChipHeight_Pixel, roiHeight*binning));
        else
            roi_Pixel_Unbinned.height=Math.min(fullChipHeight_Pixel-roi_Pixel_Unbinned.y,roiHeight*binning);
    }

    public Rectangle getROI_Pixel(int binning) {
        if (roi_Pixel_Unbinned == null)
            return null;
        else
            return new Rectangle(roi_Pixel_Unbinned.x/binning,
                    roi_Pixel_Unbinned.y/binning,
                    roi_Pixel_Unbinned.width/binning,
                    roi_Pixel_Unbinned.height/binning);
    }
    
    public void centerRoi() {
        if (roi_Pixel_Unbinned==null)
            return;
        roi_Pixel_Unbinned.x=(int)Math.round(((double)fullChipWidth_Pixel - roi_Pixel_Unbinned.width)/2);
        roi_Pixel_Unbinned.y=(int)Math.round(((double)fullChipHeight_Pixel - roi_Pixel_Unbinned.height)/2);
    }
    
    public boolean isFullSize() {
        return (roi_Pixel_Unbinned==null || (roi_Pixel_Unbinned.width == fullChipWidth_Pixel && roi_Pixel_Unbinned.height == fullChipHeight_Pixel));
    }
    
    
    public void clearROI() {
        roi_Pixel_Unbinned=null;
    }
    
    //x-y dist of ROI origin from full chip origin in um
    public Point2D.Double getRoiOffset_UM(double objPixSize) {
        if (isFullSize())
            return new Point2D.Double(0,0);
        return new Point2D.Double(roi_Pixel_Unbinned.x*objPixSize,roi_Pixel_Unbinned.y*objPixSize);
    }
    
    
}
