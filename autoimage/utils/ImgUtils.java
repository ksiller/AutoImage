package autoimage.utils;

import autoimage.utils.Utils;
import static autoimage.utils.MMCoreUtils.SCALE_AUTO;
import static autoimage.utils.MMCoreUtils.SCALE_CAMERA;
import static autoimage.utils.MMCoreUtils.SCALE_NONE;
import autoimage.api.ExtImageTags;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import mmcorej.TaggedImage;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.api.MMTags;
import org.micromanager.utils.MMScriptException;

/**
 *
 * @author Karsten Siller
 */

public class ImgUtils {
    
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
            Long slice = ImgUtils.getSliceIndex(f);
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
            meta = ImgUtils.parseMetadataFromFile((File) element);
        }
        return meta;
    }

    
    public static TaggedImage openAsTaggedImage(String filename) throws JSONException {
        TaggedImage ti=null;   
        ImagePlus imp=IJ.openImage(filename);
        if (imp!=null && imp.getProperty("Info") != null) {
            JSONObject meta = new JSONObject((String)imp.getProperty("Info"));
            ti=createTaggedImage(imp,meta);
        }
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

    public static String createPathForSite(File f, String workDir, boolean create) throws JSONException {
        JSONObject meta=ImgUtils.parseMetadataFromFile(f);
        if (meta==null)
            return null;
        String area = meta.getString(ExtImageTags.AREA_NAME);
        String cluster=Long.toString(meta.getLong(ExtImageTags.CLUSTER_INDEX));
        String site=Long.toString(meta.getLong(ExtImageTags.SITE_INDEX));
        File path;
        if (!cluster.equals("-1"))
            path=new File(workDir,area+"-Cluster"+cluster+"-Site"+site);
        else
            path=new File(workDir,area+"-Site"+site);
        if (!path.exists() && create) {
            path.mkdirs(); 
        }    
        return path.getAbsolutePath();
    }
 
    public static String getChannelName(File f) throws JSONException {
        JSONObject meta=ImgUtils.parseMetadataFromFile(f);
        if (meta==null)
            return null;
        return meta.getString(MMTags.Image.CHANNEL_NAME);
    }
 
 
    public static Map<String,List<File>> getChannelMap(List<File> list) throws JSONException {
        Map<String,List<File>> map=new HashMap<String,List<File>>();
        for (File f:list) {
            String ch=ImgUtils.getChannelName(f);
            List<File> zList;
            if (!map.containsKey(ch)) {
                zList=new ArrayList<File>();
                map.put(ch,zList);
            } else {
                zList=(List<File>)map.get(ch);
            }
            zList.add(f);
        }
        return map;
    }
    
    
    public static boolean calibrateImage(ImagePlus imp, JSONObject imageMeta) {
        try {
            double pixSize=imageMeta.getDouble("PixelSizeUm");
            Calibration cal=imp.getCalibration();
            cal.pixelWidth=pixSize;
            cal.pixelHeight=pixSize;
            imp.setCalibration(cal);
            return true;
        } catch (JSONException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    } 
    
}
