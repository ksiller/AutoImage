/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten
 */
public class Detector  {
    
    public final static String TAG_TYPE = "TYPE";
    public final static String TAG_LABEL = "LABEL";
    public final static String TAG_PIXEL_X = "PIXEL_X";
    public final static String TAG_PIXEL_Y = "PIXEL_Y";
    public final static String TAG_FIELD_ROTATION = "FIELD_ROTATION";
    public final static String TAG_BINNING = "BINNING";
    public final static String TAG_BINNING_OPTIONS = "BINNING_OPTIONS";
    public final static String TAG_GAIN = "GAIN";
    public final static String TAG_OFFSET = "OFFSET";
    public final static String TAG_ROI_X = "ROI_X";
    public final static String TAG_ROI_Y = "ROI_Y";
    public final static String TAG_ROI_WIDTH = "ROI_WIDTH";
    public final static String TAG_ROI_HEIGHT = "ROI_HEIGHT";
    
    protected String type;
    protected String label;
    protected int width_Pixel;
    protected int height_Pixel;
    protected double fieldRotation;
//    protected long dynamicRange;
    protected String[] binningOptions;
    protected int bitDepth;

    public Detector() {
        type="unknown";
        label="unknown";
        bitDepth=-1;
        width_Pixel=1;
        height_Pixel=1;
        fieldRotation=FieldOfView.ROTATION_UNKNOWN;
        binningOptions=new String[] {"1"};
    }
    
    public Detector(JSONObject detObj) throws JSONException {
        if (detObj==null) {
            type="unknown";
            label="unknown";
            width_Pixel=1;
            height_Pixel=1;
            fieldRotation=FieldOfView.ROTATION_UNKNOWN;
            binningOptions=new String[] {"1"};
        } else {
            type=detObj.getString(TAG_TYPE);
            label=detObj.getString(TAG_LABEL);
            width_Pixel=detObj.getInt(TAG_PIXEL_X);
            height_Pixel=detObj.getInt(TAG_PIXEL_Y);
        //    fieldRotation=detObj.getDouble(TAG_FIELD_ROTATION);
            fieldRotation=FieldOfView.ROTATION_UNKNOWN;
            JSONArray binningOpt=detObj.getJSONArray(TAG_BINNING_OPTIONS);
            binningOptions=new String[binningOpt.length()];
            for (int i=0; i<binningOpt.length(); i++) {
                binningOptions[i]=binningOpt.getString(i);
            }
        }     
    }
    
    public Detector (String lab, int pixX, int pixY, int bdepth, String[] binningOpt, double frot) {
        type="unknown";
        label=lab;
        width_Pixel=pixX;
        height_Pixel=pixY;
        bitDepth=bdepth;
        binningOptions=binningOpt;
        fieldRotation=frot;
    }
    
    
    public Detector(Detector det) {
        this (det.label,det.width_Pixel, det.height_Pixel, det.bitDepth, det.binningOptions.clone(), det.fieldRotation);
    }
    
    @Override
    public String toString() {
        String bin="";
        for (int i=0; i<binningOptions.length; i++)
            bin=bin+binningOptions[i]+", ";
        return "Type: "+type
                +"; Label: "+label
                +"; Chip width: "+Integer.toString(width_Pixel)+" px"
                +"; Chip height: "+Integer.toString(height_Pixel)+" px"
                +"; Bit depth: "+Integer.toString(bitDepth)
                +"; Binning options: "+bin
                +"; Field rotation (rad): "+(fieldRotation == FieldOfView.ROTATION_UNKNOWN ? "unknown" :Double.toString(fieldRotation));
    }
    
    public void setBinningOptions(String[] binOpt) {
        binningOptions=binOpt;
    }
    
    public String[] getBinningOptions() {
        return binningOptions;
    }
    
    public int getBinningOption(int index, int defaultVal) {
        if (binningOptions==null || binningOptions.length < index)
            return defaultVal;
        else
            return Integer.parseInt(binningOptions[index]);
    }
    
    public double getFieldRotation() {
        return fieldRotation;
    }

    
    public void setFieldRotation(double angle) {
        fieldRotation=angle;
    }

    
    public int getFullWidth_Pixel() {
        return width_Pixel;    
    }

    
    public void setFullWidth_Pixel(int pixX) {
        width_Pixel=pixX;    
    }

    
    public int getFullHeight_Pixel() {
        return height_Pixel;
    }

    
    public void setFullHeight_Pixel(int pixY) {
        height_Pixel=pixY;
    }

        
    public JSONObject toJSONObject() {
        JSONObject detObj = new JSONObject();
        return detObj;
    }
    
    
    public boolean allowsBinning() {
        return (binningOptions.length>1 && Integer.parseInt(binningOptions[1]) > 1);
    }
}
