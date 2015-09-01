package autoimage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
    protected double fieldRotation;
//    protected long dynamicRange;
    protected String[] binningDesc;
    protected Map<String,Integer> binningOptions;
    protected int bitDepth;

    public Detector() {
        type="unknown";
        label="unknown";
        bitDepth=-1;
        width_Pixel=1;
        height_Pixel=1;
        fieldRotation=FieldOfView.ROTATION_UNKNOWN;
        binningOptions=new HashMap<String,Integer>();
        binningDesc=parseBinningDesc(binningOptions);
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
            binningOptions=new HashMap<String,Integer>();
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
    public Detector (String lab, int pixX, int pixY, int bdepth, Map<String,Integer> binning, double frot) {
        type="unknown";
        label=lab;
        width_Pixel=pixX;
        height_Pixel=pixY;
        bitDepth=bdepth;
        binningOptions=binning;
        binningDesc=parseBinningDesc(binning);
        fieldRotation=frot;
    }
    
    
    public Detector(Detector det) {
        this (det.label,det.width_Pixel, det.height_Pixel, det.bitDepth, null, det.fieldRotation);
        Map<String,Integer> bin=new HashMap<String, Integer>();
        bin.putAll(det.binningOptions);
        setBinningOptions(bin);
    }
    
    private String[] parseBinningDesc(Map<String,Integer> binning) {
        if (binning!=null) {
            String[] b=new String[binning.size()];
            int i=0;
            for (String s:binning.keySet()) {
                b[i]=s;
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
        for (String option:binningDesc) {
            bin=bin+option+" ("+Integer.toString(binningOptions.get(option))+"), ";
        }
        return "Type: "+type
                +"; Label: "+label
                +"; Chip width: "+Integer.toString(width_Pixel)+" px"
                +"; Chip height: "+Integer.toString(height_Pixel)+" px"
                +"; Bit depth: "+Integer.toString(bitDepth)
                +"; Binning options: "+bin
                +"; Field rotation (rad): "+(fieldRotation == FieldOfView.ROTATION_UNKNOWN ? "unknown" :Double.toString(fieldRotation));
    }
    
    public void setBinningOptions(Map<String, Integer> binOpt) {
        binningOptions=binOpt;
        binningDesc=parseBinningDesc(binOpt);
    }
    
    public Map<String,Integer> getBinningOptions() {
        return binningOptions;
    }
    
    public String getBinningDesc(int index) {
        if (index>=0 && index<binningDesc.length) {
            return binningDesc[index];
        } else {
            return null;
        }    
    }
    
    public Integer getBinningFactor(String option, int defaultVal) {
        Integer bin=binningOptions.get(option);
        if (bin==null) {
            return new Integer(defaultVal);
        } else {
            return bin;
        }
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

/*        
    public JSONObject toJSONObject() {
        JSONObject detObj = new JSONObject();
        return detObj;
    }
*/    
    
    public boolean allowsBinning() {
        return (binningDesc.length>1);
    }
    
}
