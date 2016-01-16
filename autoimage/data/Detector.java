package autoimage.data;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Karsten Siller
 */
public class Detector  {
        
    protected String type;
    protected String label;
    protected int width_Pixel;
    protected int height_Pixel;
    protected Rectangle unbinnedRoi;
    protected double fieldRotation;
    protected Map<String,Integer> binningMap;
    protected int bitDepth;
    protected long bytesPerPixel;

    public Detector() {
        type="unknown";
        label="unknown";
        bitDepth=-1;
        bytesPerPixel=0;
        width_Pixel=0;
        height_Pixel=0;
        unbinnedRoi=new Rectangle(0,0,0,0);
        fieldRotation=FieldOfView.ROTATION_UNKNOWN;
        binningMap=new HashMap<String,Integer>();
    }

    public Detector (String lab, int pixX, int pixY, Rectangle r, int bdepth, long bytes, Map<String,Integer> binning, double frot) {
        type="unknown";
        label=lab;
        width_Pixel=pixX;
        height_Pixel=pixY;
        unbinnedRoi=r;
        bitDepth=bdepth;
        bytesPerPixel=bytes;
        binningMap=binning;
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
        binningMap=bin;
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
        return parseBinningFactors();
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
        unbinnedRoi=r;
        width_Pixel=r.width;
        height_Pixel=r.height;
    }
    
    public Rectangle getUnbinnedRoi() {
        return unbinnedRoi;
    }  
    
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
