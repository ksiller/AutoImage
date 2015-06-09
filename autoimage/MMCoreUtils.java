/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.plugin.RGBStackMerge;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import mmcorej.CMMCore;
import mmcorej.StrVector;
import mmcorej.TaggedImage;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.api.MMTags;
import org.micromanager.utils.ImageUtils;
import org.micromanager.utils.MMScriptException;
import org.micromanager.utils.ReportingUtils;

/**
 *
 * @author Karsten
 */
public class MMCoreUtils {
    
    public static List<String> availableChannelList = new ArrayList<String>();
    public final static int SCALE_CAMERA = -2;
    public final static int SCALE_AUTO = -1;
    public final static int SCALE_NONE = 0;

    public static String[] getPropertyNamesForAFDevice(String devName_, CMMCore core_){
        Vector<String> propNames = new Vector<String>();
        try {
            core_.setAutoFocusDevice(devName_);
            StrVector propNamesVect = core_.getDevicePropertyNames(devName_);
            for (int i = 0; i < propNamesVect.size(); i++)
                if (!core_.isPropertyReadOnly(devName_, propNamesVect.get(i))
	                  && !core_.isPropertyPreInit(devName_,
	                        propNamesVect.get(i)))
	               propNames.add(propNamesVect.get(i));
        } catch (Exception e) {
           ReportingUtils.logError(e);
        }
        return propNames.toArray(new String[propNames.size()]);
    }
    
    
    public static String selectDeviceStr(JFrame parent, CMMCore core, String device) {
        StrVector configs = core.getLoadedDevices();
        String[] options = new String[(int) configs.size()];
        //populate array
        for (int i = 0; i < configs.size(); i++) {
            options[i] = configs.get(i);
        }
        String s = (String) JOptionPane.showInputDialog(
                    parent,
                    "Select property group for "+device+":",
                    "Property Group",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    configs.get(0));
        if ((s != null) && (s.length() > 0)) { 
            return s;
        } else {
            return null;
        }
    } 
    
    public static String changeConfigGroupStr(JFrame parent, CMMCore core, String groupName, String groupStr) {
        StrVector configs = core.getAvailableConfigGroups();
        String[] options = new String[(int) configs.size()];
        for (int i = 0; i < configs.size(); i++) {
            options[i] = configs.get(i);
        }
        String s = (String) JOptionPane.showInputDialog(
                    parent,
                    "Select "+groupName+" configuration group:",
                    "Configuration Group",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    configs.get(0));
        if ((s != null) && (s.length() > 0)) {
            groupStr = s;
            configs = core.getAvailableConfigs(groupStr);
            if ((configs == null) || configs.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "No configurations found for "+groupStr+".\n\nPresets need to be defined for this group!");
            }
            return groupStr;
        } else {
            return null;
        }
    } 

    public static String loadAvailableChannelConfigs(JFrame parent, CMMCore core, String cGroupStr) {
        if (cGroupStr==null) {
            return null;
        }
        StrVector availableCh = core.getAvailableConfigs(cGroupStr);
        availableChannelList = new ArrayList<String>();
        if (availableCh != null && !availableCh.isEmpty()) {
            for (String ch:availableCh) {
                availableChannelList.add(ch);
            }
            return cGroupStr;
        } else {
            StrVector configs = core.getAvailableConfigGroups();
            String[] options = new String[(int) configs.size()];
            for (int i = 0; i < configs.size(); i++) {
                options[i] = configs.get(i);
            }
            String s = (String) JOptionPane.showInputDialog(
                    parent,
                    "Could not find Configuration Group '" + cGroupStr + "'.\n\nChoose Channel Configuration Group:",
                    "Error",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    configs.get(0));
            if ((s != null) && (s.length() > 0)) {
                cGroupStr=s;
                availableCh = core.getAvailableConfigs(cGroupStr);
                if ((availableCh == null) || availableCh.isEmpty()) {
                    JOptionPane.showMessageDialog(parent, "No Channel configurations found.\n\nPresets need to be defined in the Channel group!");
                } else {
                    for (String ch:availableCh) {
                        availableChannelList.add(ch);
                    }
                }
                return cGroupStr;
            } else {
                return null;
            }
        }
    }

    
    public static byte[] convertIntToByteArray(int[] pixels) {
        byte[] byteArray = null;
        if (pixels instanceof int[]) {
            //convert int[] to byte[] 
            int[] intArray=(int[])pixels;
            byteArray = new byte[intArray.length*4];
            for (int i=0; i<intArray.length; ++i) {
                byteArray[i*4 + 2] = (byte)(intArray[i] >> 16);
                byteArray[i*4 + 1] = (byte)(intArray[i] >> 8);
                byteArray[i*4] = (byte)(intArray[i]);
            }
        }
        return byteArray;
    }
    
    public static ImageProcessor[] snapImage(CMMCore core, String chGroupStr, String ch, double exp) {
        ImageProcessor[] ipArray = null;
        try {
            core.setConfig(chGroupStr, ch);
            core.setExposure(exp);
            core.waitForSystem();
            core.snapImage();
            Object imgArray=core.getImage();
            int w = (int)core.getImageWidth();
            int h = (int)core.getImageHeight();
            switch ((int)core.getBytesPerPixel()) {
                case 1: {
                    // 8-bit grayscale pixels
                    byte[] img = (byte[]) imgArray;
                    ipArray=new ImageProcessor[1];
                    ipArray[0] = new ByteProcessor(w, h, img, null);
                    break;
                } 
                case 2: {
                    // 16-bit grayscale pixels
                    short[] img = (short[]) imgArray;
                    ipArray=new ImageProcessor[1];
                    ipArray[0] = new ShortProcessor(w, h, img, null);
                    break;
                } 
                case 4: {
                    // color pixels: RGB32
                    if (imgArray instanceof byte[]) {
                        //convert byte[] to int[] 
                        byte[] byteArray=(byte[])imgArray;
                        int[] intArray = new int[byteArray.length/4];
                        for (int i=0; i<intArray.length; ++i) {
                            intArray[i] =  byteArray[4*i]
                  	                 + (byteArray[4*i + 1] << 8)
                  	                 + (byteArray[4*i + 2] << 16);
                  	}
	                imgArray = intArray;
	            }
                    ipArray=new ImageProcessor[1];
	            ipArray[0]=new ColorProcessor(w, h, (int[]) imgArray);
                    break;
                }               
                case 8: {
                    // color pixels: RGB64
                    if (imgArray instanceof short[]) {
                        short[] shortArray=(short[])imgArray;
                        ipArray=new ImageProcessor[3];
                        for (int i=0; i<3; ++i) {//iterate over B, G, R channels
                            short[] channelArray=new short[shortArray.length/4];
                            for (int pixelPos=0; pixelPos<channelArray.length; pixelPos++) {
                                channelArray[pixelPos] =  shortArray[4*pixelPos+i];
                            }
                            //create new ShortProcessor for this channel and add to array
                            //return in R, G, B order
                            ipArray[2-i]=new ShortProcessor(w, h, channelArray, null);
                  	}
                    }
                    break;
                }
                default: {
                    IJ.log("MMCoreUtils.snapImage: Unknown image type ("+Long.toString(core.getBytesPerPixel())+" bytes/pixel)");        
                    break;
                }
            }
        } catch (Exception ex) {
            IJ.log("MMCoreUtils.snapImage: Exception."+ex.getMessage());
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            ipArray = null;
        }
        return ipArray;
    } 
    
    public static ImageProcessor[] snapImageWithZOffset(CMMCore core, String groupStr, String ch, double exp, double zOffs) {
        ImageProcessor[] ip = null;
        try {
            String zStageLabel=core.getFocusDevice();
            if (zOffs!=0) {
                core.setRelativePosition(zStageLabel, zOffs);
                core.waitForDevice(zStageLabel);
            }
            ip=MMCoreUtils.snapImage(core, groupStr, ch, exp);
            if (zOffs!=0) {
                core.setRelativePosition(zStageLabel, -zOffs);
                core.waitForDevice(zStageLabel);
            }            
        } catch (Exception ex) {
            IJ.log("AcqFrame.snapAndDisplayImage: Exception - "+ex.getMessage()+".");
            ip = null;
        }
        return ip;
    }
    
    public static ImagePlus snapImagePlus(CMMCore core, String chGroupStr, String ch, double exp, double zOffset, int scaleMode) {
        ImagePlus imp=null;
        long fullBitRange;
        ImageProcessor[] ipArray=snapImageWithZOffset(core, chGroupStr, ch, exp, zOffset);
        if (ipArray==null) {
            return null;
        }
        if (ipArray.length == 1) {
            imp=new ImagePlus(ch, ipArray[0]);
            if (ipArray[0] instanceof ColorProcessor) {
                //RGB32
                fullBitRange=8;
            } else {
                //8bit or 16 bit grayscale
                fullBitRange=8*core.getBytesPerPixel();
            }    
            switch (scaleMode) {
                case SCALE_CAMERA: {
                        for (ImageProcessor ip:ipArray)
                            ip.setMinAndMax(0, Math.pow(2, core.getImageBitDepth()));
                        break;
                    }
                case SCALE_AUTO: {  
                        for (ImageProcessor ip:ipArray)
                            ip.setMinAndMax(0, ipArray[0].getMax());
                        break; 
                    }
                case SCALE_NONE: {
                        for (ImageProcessor ip:ipArray)
                            ip.setMinAndMax(0, Math.pow(2, fullBitRange));
                        break; 
                    } 
                default: {
                        for (ImageProcessor ip:ipArray)
                            ip.setMinAndMax(0, Math.pow(2, scaleMode)); 
                    }
            }  
        } else {
            //RGB64
            fullBitRange=16;
            ImagePlus[] impArray=new ImagePlus[ipArray.length]; 
            for (int i=0; i<ipArray.length; i++) {
                impArray[i]=new ImagePlus(ch+"("+Integer.toString(i)+")",ipArray[i]);
                impArray[i].getProcessor().setMinAndMax(0, 65535);
            }
            RGBStackMerge merger=new RGBStackMerge();
            imp=merger.mergeHyperstacks(impArray, false);
            imp.setTitle(ch);
        }
 
        IJ.log("fullBitRange: "+Long.toString(fullBitRange));
        Calibration cal = imp.getCalibration();
        cal.setUnit("um");
        cal.pixelWidth = core.getPixelSizeUm();
        cal.pixelHeight = core.getPixelSizeUm();
        imp.setCalibration(cal);        
        return imp;
    }
    
    public static ImagePlus convertToComposite(ImagePlus imp) {                   
        if (imp.getProcessor() instanceof ColorProcessor) {
            return new CompositeImage(imp);
        } else
            return null;
    }

    public static JSONObject parseMetadataFromFile(final File dataFile) throws JSONException {
        JSONObject md = null;
        ImagePlus imp = IJ.openImage(dataFile.getAbsolutePath());
        if (imp != null) {
            if (imp.getProperty("Info") != null) {
                md = new JSONObject((String) imp.getProperty("Info"));
            } else {
                IJ.log("Utils.parseMetadata: imp.getProperty failed");
            }
        } else {
            IJ.log("Utils.parseMetadata: cannot open ImagePlus");
        }
        return md;
    }

    public static String[] getAvailableImageTags() {
        Class c = MMTags.Image.class;
        Field[] fieldArray = c.getFields();
        String[] tags = new String[fieldArray.length + 4];
        for (int i = 0; i < fieldArray.length; i++) {
            try {
                tags[i] = (String) fieldArray[i].get(null);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        tags[fieldArray.length] = ExtImageTags.AREA_NAME;
        tags[fieldArray.length + 1] = ExtImageTags.CLUSTER_INDEX;
        tags[fieldArray.length + 2] = ExtImageTags.AREA_COMMENT;
        tags[fieldArray.length + 3] = ExtImageTags.SITE_INDEX;
        Arrays.sort(tags);
        return tags;
    }

    public static Long getSliceIndex(File f) throws JSONException {
        JSONObject meta = parseMetadataFromFile(f);
        if (meta == null) {
            return null;
        }
        return meta.getLong(MMTags.Image.SLICE_INDEX);
    }

    public static JSONObject readMetadataSummary(Object element, boolean createCopy) throws JSONException {
        return readMetadata(element, createCopy).getJSONObject(MMTags.Root.SUMMARY);
    }

    public static Map<Long, List<File>> getSliceIndexMap(List<File> list) throws JSONException {
        Map<Long, List<File>> map = new HashMap<Long, List<File>>();
        for (File f : list) {
            Long slice = MMCoreUtils.getSliceIndex(f);
            List<File> chList;
            if (!map.containsKey(slice)) {
                chList = new ArrayList<File>();
                map.put(slice, chList);
            } else {
                chList = (List<File>) map.get(slice);
            }
            chList.add(f);
        }
        return map;
    }

    //shallow pixel array copy
    public static TaggedImage createTaggedImage(ImagePlus imp, JSONObject meta) {
        Object pix = null;
        TaggedImage ti = null;
        try {
            String pixType = meta.getString(MMTags.Image.PIX_TYPE);
            if (pixType.equals("GRAY8") || pixType.equals("GRAY16")) {
                pix = imp.getProcessor().getPixels();
            } else if (pixType.equals("RGB32")) {
                pix = imp.getProcessor().getPixels();
                if (pix instanceof int[]) {
                    int[] intArray = (int[]) pix;
                    byte[] byteArray = new byte[intArray.length * 4];
                    for (int i = 0; i < intArray.length; i++) {
                        byteArray[i * 4] = (byte) (intArray[i]);
                        byteArray[i * 4 + 1] = (byte) (intArray[i] >> 8);
                        byteArray[i * 4 + 2] = (byte) (intArray[i] >> 16);
                    }
                    pix = byteArray;
                } else {
                }
            } else if (pixType.equals("RGB64")) {
                ImageProcessor[] ipArray = new ImageProcessor[imp.getStackSize()];
                for (int i = 0; i < imp.getStackSize(); i++) {
                    ipArray[i] = imp.getImageStack().getProcessor(i + 1);
                }
                if (ipArray[0].getPixels() instanceof short[]) {
                    short[] shortArray = new short[((short[]) ipArray[0].getPixels()).length * 4];
                    for (int i = 0; i < 3; i++) {
                        short[] channelPix = (short[]) ipArray[i].getPixels();
                        for (int pixelPos = 0; pixelPos < channelPix.length; pixelPos++) {
                            shortArray[4 * pixelPos + (2 - i)] = channelPix[pixelPos];
                        }
                    }
                    pix = shortArray;
                }
            }
            ti = new TaggedImage(pix, meta);
        } catch (JSONException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ti;
    }

    public static File getImageAsFileObject(TaggedImage ti) throws JSONException {
        JSONObject summary = ti.tags.getJSONObject(MMTags.Root.SUMMARY);
        String directory = summary.getString(MMTags.Summary.DIRECTORY);
        String prefix = summary.getString(MMTags.Summary.PREFIX);
        String positionName = ti.tags.getString(MMTags.Image.POS_NAME);
        String fileName = ti.tags.getString("FileName");
        File f = new File(new File(new File(directory, prefix), positionName), fileName);
        return f;
    }

    public static ImagePlus createImagePlus(TaggedImage ti, int scaleMode) {
        try {
            ImageProcessor[] ipArray = createImageProcessor(ti, scaleMode);
            if (ipArray==null)
                return null;
            if (ipArray.length == 1) {
                //8-bit gray, 16-bit gray, or RGB32
                return new ImagePlus(ti.tags.getString(MMTags.Image.POS_NAME), ipArray[0]);
            } else {
                //RGB64
                ImagePlus[] impArray = new ImagePlus[ipArray.length];
                for (int i = 0; i < ipArray.length; i++) {
                    impArray[i] = new ImagePlus(Integer.toString(i), ipArray[i]);
                }
                RGBStackMerge merger = new RGBStackMerge();
                return merger.mergeHyperstacks(impArray, false);
            }
        } catch (JSONException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static TaggedImage duplicateTaggedImage(TaggedImage ti) throws JSONException, MMScriptException {
        //deep copy of metadata
        JSONObject newMeta = new JSONObject(ti.tags.toString());
        
        //deep copy of pixel array
        if (ti.pix instanceof byte[]) {
            byte[] source=(byte[])ti.pix;
            byte[] dest=new byte[source.length];
            for (int i=0; i<source.length; i++) {
                dest[i]=source[i];
            }
            return new TaggedImage(dest,newMeta);
        } else if(ti.pix instanceof short[]) {
            short[] source=(short[])ti.pix;
            short[] dest=new short[source.length];
            for (int i=0; i<source.length; i++) {
                dest[i]=source[i];
            }
            return new TaggedImage(dest,newMeta);
        } else if(ti.pix instanceof int[]) {
            int[] source=(int[])ti.pix;
            int[] dest=new int[source.length];
            for (int i=0; i<source.length; i++) {
                dest[i]=source[i];
            }
            return new TaggedImage(dest,newMeta);
        } else
            return null;
        
/*        int width = MDUtils.getWidth(ti.tags);
        int height = MDUtils.getHeight(ti.tags);
        String type = MDUtils.getPixelType(ti.tags);
        int ijType = ImagePlus.GRAY8;
        if (type.equals("GRAY16")) {
            ijType = ImagePlus.GRAY16;
        }
        ImageProcessor proc = ImageUtils.makeProcessor(ijType, width, height, ti.pix);
        return new TaggedImage(proc.getPixelsCopy(), newMeta);*/
    }

    public static void writeMetadata(final File dataFile, JSONObject meta) throws JSONException {
        ImagePlus imp = new Opener().openImage(dataFile.getAbsolutePath());
        if (imp != null) {
            imp.setProperty("Info", meta);
            IJ.saveAsTiff(imp, dataFile.getAbsolutePath());
        }
    }

    public static JSONObject readMetadata(Object element, boolean createCopy) throws JSONException {
        JSONObject meta = null;
        if (element instanceof TaggedImage) {
            if (createCopy) {
                meta = new JSONObject(((TaggedImage) element).tags.toString());
            } else {
                meta = ((TaggedImage) element).tags;
            }
        } else if (element instanceof File) {
            meta = MMCoreUtils.parseMetadataFromFile((File) element);
        }
        return meta;
    }

    public static TaggedImage openAsTaggedImage(String filename) throws JSONException {
        ImagePlus imp = IJ.openImage(filename);
        TaggedImage ti = ImageUtils.makeTaggedImage(imp.getProcessor());
        ti.tags = readMetadata(new File(filename), true);
        return ti;
    }

    public static ImageProcessor[] createImageProcessor(TaggedImage ti, int scaleMode) {
        try {
            int width = ti.tags.getInt(MMTags.Image.WIDTH);
            int height = ti.tags.getInt(MMTags.Image.HEIGHT);
            String pixType = ti.tags.getString(MMTags.Image.PIX_TYPE);
            int cameraBitDepth = ti.tags.getInt(MMTags.Summary.BIT_DEPTH);
            ImageProcessor[] ipArray = null;
            if (pixType.equals("GRAY8")) {
                ipArray = new ImageProcessor[1];
                ipArray[0] = new ByteProcessor(width, height, (byte[]) ti.pix);
            } else if (pixType.equals("GRAY16")) {
                ipArray = new ImageProcessor[1];
                ipArray[0] = new ShortProcessor(width, height, (short[]) ti.pix, null);
            } else if (pixType.equals("RGB32")) {
                ipArray = new ImageProcessor[1];
                if (ti.pix instanceof byte[]) {
                    byte[] byteArray = (byte[]) ti.pix;
                    int[] intArray = new int[byteArray.length / 4];
                    for (int i = 0; i < intArray.length; ++i) {
                        intArray[i] = byteArray[4 * i] + (byteArray[4 * i + 1] << 8) + (byteArray[4 * i + 2] << 16);
                    }
                    ipArray[0] = new ColorProcessor(width, height, intArray);
                } else {
                    ipArray[0] = new ColorProcessor(width, height, (int[]) ti.pix);
                }
            } else if (pixType.equals("RGB64")) {
                if (ti.pix instanceof short[]) {
                    short[] shortArray = (short[]) ti.pix;
                    ipArray = new ImageProcessor[3];
                    for (int i = 0; i < 3; ++i) {
                        short[] channelArray = new short[shortArray.length / 4];
                        for (int pixelPos = 0; pixelPos < channelArray.length; pixelPos++) {
                            channelArray[pixelPos] = shortArray[4 * pixelPos + i];
                        }
                        ipArray[2 - i] = new ShortProcessor(width, height, channelArray, null);
                    }
                }
            } 
            if (ipArray!=null) {
                switch (scaleMode) {
                    case SCALE_CAMERA: {
                            for (ImageProcessor ip:ipArray)
                                ip.setMinAndMax(0, Math.pow(2, cameraBitDepth));
                            break;
                        }
                    case SCALE_AUTO: {
                            for (ImageProcessor ip:ipArray)
                                ip.setMinAndMax(0, ipArray[0].getMax());
                            break; 
                    }
                    case SCALE_NONE: {
                        break;
                    } 
                    default: {
                            for (ImageProcessor ip:ipArray)
                                ip.setMinAndMax(0, Math.pow(2, scaleMode)); 
                    }
                }
                IJ.log("SCALING");
            }
            return ipArray;
        } catch (JSONException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
}
