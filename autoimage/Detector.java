package autoimage;

import ij.IJ;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Karsten Siller
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
    protected Rectangle unbinnedRoi;
    protected double fieldRotation;
//    protected long dynamicRange;
//    protected String[] binningDesc;
//    protected int[] binningFactors;
    protected Map<String,Integer> binningMap;
    protected int bitDepth;
    protected long bytesPerPixel;

    public Detector() {
        type="unknown";
        label="unknown";
        bitDepth=-1;
        bytesPerPixel=0;
        width_Pixel=1;
        height_Pixel=1;
        unbinnedRoi=new Rectangle(0,0,0,0);
        fieldRotation=FieldOfView.ROTATION_UNKNOWN;
        binningMap=new HashMap<String,Integer>();
//        binningDesc=parseBinningDesc(binningMap);
    }
    /*
    public Detector(JSONObject detObj) throws JSONException {
        if (detObj==null) {
            type="unknown";
            label="unknown";
            width_Pixel=1;
            height_Pixel=1;
            fieldRotation=FieldOfView.ROTATION_UNKNOWN;
            binningDesc=new String[] {};
            binningMap=new HashMap<String,Integer>();
        } else {
            type=detObj.getString(TAG_TYPE);
            label=detObj.getString(TAG_LABEL);
            width_Pixel=detObj.getInt(TAG_PIXEL_X);
            height_Pixel=detObj.getInt(TAG_PIXEL_Y);
        //    fieldRotation=detObj.getDouble(TAG_FIELD_ROTATION);
            fieldRotation=FieldOfView.ROTATION_UNKNOWN;
            JSONArray binningOpt=detObj.getJSONArray(TAG_BINNING_OPTIONS);
            binningDesc=new String[binningOpt.length()];
            for (int i=0; i<binningOpt.length(); i++) {
                binningDesc[i]=binningOpt.getString(i);
            }
        }     
    }
    */
    public Detector (String lab, int pixX, int pixY, Rectangle r, int bdepth, long bytes, Map<String,Integer> binning, double frot) {
        type="unknown";
        label=lab;
        width_Pixel=pixX;
        height_Pixel=pixY;
        unbinnedRoi=r;
        bitDepth=bdepth;
        bytesPerPixel=bytes;
        binningMap=binning;
//        binningDesc=parseBinningDesc(binning);
        fieldRotation=frot;
    }
    
    
    public Detector(Detector det) {
        this (det.label,
                det.width_Pixel, 
                det.height_Pixel, 
                new Rectangle (det.unbinnedRoi.x, det.unbinnedRoi.y, det.unbinnedRoi.width, det.unbinnedRoi.height), 
                det.bitDepth,
                det.bytesPerPixel,
                null, 
                det.fieldRotation);
        Map<String,Integer> bin=new HashMap<String, Integer>();
        bin.putAll(det.binningMap);
        setBinningMap(bin);
    }
    
    public String getLabel() {
        return label;
    }
    
    private String[] parseBinningDesc() {
        if (binningMap!=null) {
            String[] b=binningMap.keySet().toArray(new String[binningMap.size()]);
            Arrays.sort(b);
            return b;
        } else {
            return null;
        }
    }
    
    private int[] parseBinningFactors() {
        if (binningMap!=null) {
            int[] b=new int[binningMap.size()];
            int i=0;
            String[] keys=binningMap.keySet().toArray(new String[binningMap.size()]);
            Arrays.sort(keys);
            for (String s:keys) {
                b[i]=binningMap.get(s);
                i++;
            }
            Arrays.sort(b);
            return b;
        } else {
            return null;
        }
    }
    
    @Override
    public String toString() {
        String bin="";
        for (String option:getBinningDescriptions()) {
            bin=bin+option+" ("+Integer.toString(binningMap.get(option))+"), ";
        }
        return "Type: "+type
                +"; Label: "+label
                +"; Chip : "+Integer.toString(width_Pixel)+"x"+Integer.toString(height_Pixel)+" px"
                +"; Unbinned ROI: "+Integer.toString(unbinnedRoi.x)+", "+Integer.toString(unbinnedRoi.y)+", "+Integer.toString(unbinnedRoi.width)+", "+Integer.toString(unbinnedRoi.height)
                +"; Bit depth: "+Integer.toString(bitDepth)
                +"; Bytes per pixel: "+Long.toString(bytesPerPixel)
                +"; Binning options: "+bin
                +"; Field rotation (rad): "+(fieldRotation == FieldOfView.ROTATION_UNKNOWN ? "unknown" :Double.toString(fieldRotation));
    }
    
    public void setBinningMap(Map<String, Integer> binOpt) {
        binningMap=binOpt;
//        binningDesc=parseBinningDesc(binOpt);
//        binningFactors=parseBinningFactors(binOpt);
    }
    
    public void addBinningOption(String desc, int factor) {
        if (binningMap==null) {
            binningMap=new HashMap<String,Integer>();
        }
        binningMap.put(desc, factor);
    }
    
    public Map<String,Integer> getBinningMap() {
        return binningMap;
    }
    
    public String getBinningDesc(int index) {
        String[] desc=parseBinningDesc();
        if (index>=0 && index<desc.length) {
            return desc[index];
        } else {
            return null;
        }    
    }
    
    public String[] getBinningDescriptions() {
        return parseBinningDesc();
    }
    
    public Integer getBinningFactor(String option, int defaultVal) {
        Integer bin=binningMap.get(option);
        if (bin==null) {
            return new Integer(defaultVal);
        } else {
            return bin;
        }
    }
        
    public int[] getBinningFactors() {
        return this.parseBinningFactors();
    }
    
    public double getFieldRotation() {
        return fieldRotation;
    }

    
    public void setFieldRotation(double angle) {
        fieldRotation=angle;
    }

/*    
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
*/
    public void setUnbinnedRoi (Rectangle r) {
        IJ.log("Detector("+label+").setUnbinnedRoi:" + unbinnedRoi.toString());
        unbinnedRoi=r;
        width_Pixel=r.width;
        height_Pixel=r.height;
    }
    
    public Rectangle getUnbinnedRoi() {
        return unbinnedRoi;
    }
/*        
    public JSONObject toJSONObject() {
        JSONObject detObj = new JSONObject();
        return detObj;
    }
*/    
    
    public boolean allowsBinning() {
        return (binningMap.size()>1);
    }
    
    public int getBitDepth() {
        return bitDepth;
    }
    
    public void setBitDepth(int b) {
        bitDepth=b;
    }

    public long getByteperPixel() {
        return bytesPerPixel;
    }
    
    public void setBytesPerPixel(long b) {
        bytesPerPixel=b;
    }

}
