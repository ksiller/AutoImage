/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import autoimage.dataprocessors.ExtDataProcessor;
import autoimage.dataprocessors.SiteInfoDataProcessor;
import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import mmcorej.TaggedImage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.api.DataProcessor;
import org.micromanager.api.MMTags;
import org.micromanager.utils.ImageUtils;
import org.micromanager.utils.MDUtils;
import org.micromanager.utils.MMScriptException;
import org.micromanager.utils.ReportingUtils;

/**
 *
 * @author Karsten
 */
public class Utils {
    

    public static ImageProcessor createImageProcessor(TaggedImage ti) {
        try {
            int width = ti.tags.getInt(MMTags.Image.WIDTH);
            int height = ti.tags.getInt(MMTags.Image.HEIGHT);
            String pixType = ti.tags.getString(MMTags.Image.PIX_TYPE);
            ImageProcessor ip=null;
            if (pixType.equals("GRAY8")) {
                ip = new ByteProcessor(width, height, (byte[]) ti.pix);
            } else if (pixType.equals("GRAY16")) {
                ip = new ShortProcessor(width, height, (short[]) ti.pix, null);
            } else if (pixType.equals("RGB32")) {
                //cannot handle this
            } else if (pixType.equals("RGB64")) {
                //cannot handle this
            } else {
                ip=null;
            }    
            return ip;
        } catch (JSONException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public static ImagePlus createImagePlus(TaggedImage ti) {
        try {
            ImageProcessor ip=createImageProcessor(ti);
            return new ImagePlus(ti.tags.getString(MMTags.Image.POS_NAME),ip);    
        } catch (JSONException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }    
    
    public static TaggedImage duplicateTaggedImage(TaggedImage ti) throws JSONException, MMScriptException {
       TaggedImage copyTI = new TaggedImage(ti.pix,ti.tags);
/*       ImageProcessor proc=ImageUtils.makeProcessor(ti);
       copyTI=ImageUtils.makeTaggedImage(
                    proc.getPixelsCopy(), 
                    MDUtils.getChannelIndex(ti.tags),
                    MDUtils.getSliceIndex(ti.tags),
                    MDUtils.getPositionIndex(ti.tags),
                    MDUtils.getFrameIndex(ti.tags),
                    MDUtils.getWidth(ti.tags),
                    MDUtils.getHeight(ti.tags),
                    ImageUtils.getImageProcessorType(proc));
       */
        JSONObject newMeta=new JSONObject(ti.tags.toString());
        IJ.log("Utils.duplicateTaggedImage: successful copy of metadata");
        int width = MDUtils.getWidth(ti.tags);
        int height = MDUtils.getHeight(ti.tags);
        String type = MDUtils.getPixelType(ti.tags);
        int ijType = ImagePlus.GRAY8;
        if (type.equals("GRAY16")) {
            ijType = ImagePlus.GRAY16;
        }
        ImageProcessor proc = ImageUtils.makeProcessor(ijType, width, height, ti.pix);
        copyTI=new TaggedImage(proc.getPixelsCopy(),newMeta);
       
       return copyTI;
    }       
    
    public static DefaultMutableTreeNode createDefaultImageProcTree() {
//        DefaultMutableTreeNode root=new DefaultMutableTreeNode(new ExtDataProcessor(ProcessorTree.PROC_NAME_ACQ_ENG));
            DefaultMutableTreeNode infoProcNode=new DefaultMutableTreeNode(new SiteInfoDataProcessor(ProcessorTree.PROC_NAME_ACQ_ENG,null));
//            root.add(infoProcNode);
/*               List<String> chList=new ArrayList<String>();
               chList.add("DAPI");
               chList.add("FITC");
               chList.add("Cy5");
               chList.add("Rhodamine");
               List<Long> sliceList=new ArrayList<Long>();
               sliceList.add(new Long(0));
               sliceList.add(new Long(1));
        
               DefaultMutableTreeNode RGBProcNode=new DefaultMutableTreeNode(new ZProj("", null, null));*/
//            root.add(infoProcNode);
               DefaultMutableTreeNode storageProcNode=new DefaultMutableTreeNode(new ExtDataProcessor(ProcessorTree.PROC_NAME_IMAGE_STORAGE,""));
               infoProcNode.add(storageProcNode);
          //     storageProcNode.add(RGBProcNode);

        return infoProcNode;//root;
    }
    
    private DefaultMutableTreeNode copyNode(DefaultMutableTreeNode OriginNode){
        DefaultMutableTreeNode Copy = new DefaultMutableTreeNode(OriginNode.toString());
        if(OriginNode.isLeaf()){
            return Copy;
        }else{
            int cc = OriginNode.getChildCount();
            for(int i=0;i<cc;i++){
                Copy.add(copyNode((DefaultMutableTreeNode)OriginNode.getChildAt(i)));
            }
            return Copy;
        }
    }

    
    private static String getString(ByteBuffer buffer) {
      try {
         return new String(buffer.array(), "UTF-8");
      } catch (UnsupportedEncodingException ex) {
         ReportingUtils.logError(ex);
         return "";
      }
   }
    
    
    public static JSONObject parseMetadata(final File dataFile) throws JSONException {
        JSONObject md = null;
        ImagePlus imp = new Opener().openImage(dataFile.getAbsolutePath());
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

    public static void writeMetadata(final File dataFile, JSONObject meta) throws JSONException {
        ImagePlus imp = new Opener().openImage(dataFile.getAbsolutePath());
        if (imp != null) {
            imp.setProperty("Info",meta);
            IJ.saveAsTiff(imp,dataFile.getAbsolutePath());
        }    
    }

    public static File getImageAsFileObject(TaggedImage ti) throws JSONException {
        JSONObject summary=ti.tags.getJSONObject(MMTags.Root.SUMMARY);
        String directory = summary.getString("Directory");
        String prefix = summary.getString(MMTags.Summary.PREFIX);
        String positionName=ti.tags.getString("PositionName");
        String fileName = ti.tags.getString("FileName");
        File f=new File(new File(new File(directory,prefix),positionName),fileName);
        return f;
    }        
    
    
    public static boolean isDescendantOfImageStorageNode(DefaultMutableTreeNode root, DefaultMutableTreeNode node) {
        Enumeration<DefaultMutableTreeNode> en = node.pathFromAncestorEnumeration(root);
        while (en.hasMoreElements()) {
            DefaultMutableTreeNode n=en.nextElement();
            DataProcessor dp=(DataProcessor)n.getUserObject();
            if (dp instanceof ExtDataProcessor && ((ExtDataProcessor)dp).getProcName().equals(ProcessorTree.PROC_NAME_IMAGE_STORAGE))
                return true;
        }
        return false;
    }
    
    public static String[] getAvailableImageTags() {   
        Class c=org.micromanager.api.MMTags.Image.class;
        Field[] fieldArray=c.getFields();
        String[] tags = new String[fieldArray.length+4];
        for (int i=0; i<fieldArray.length; i++) {
            try {
                tags[i]=(String)fieldArray[i].get(null);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        tags[fieldArray.length]="Area";
        tags[fieldArray.length+1]="ClusterIndex";
        tags[fieldArray.length+2]="Comment";
        tags[fieldArray.length+3]="SiteIndex";
        Arrays.sort(tags);
        return tags;
    }
        
/*    public static void copyFile(File from, File to) throws IOException {
        if (!to.exists()) {
            to.createNewFile();
        }
        try {
            FileChannel in = new FileInputStream(from).getChannel();
            FileChannel out = new FileOutputStream(to).getChannel();
            out.transferFrom(in, 0, in.size());
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
    }
*/
    public static void copyFile(File source, File dest) throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            inputChannel.close();
            outputChannel.close();
        }
    }

    public static String createPathForSite(File f, String workDir, boolean create) throws JSONException {
        JSONObject meta=parseMetadata(f);
        if (meta==null)
            return null;
        String area = meta.getString("Area");
        String cluster=Long.toString(meta.getLong("ClusterIndex"));
        String site=Long.toString(meta.getLong("SiteIndex"));
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
        JSONObject meta=parseMetadata(f);
        if (meta==null)
            return null;
        return meta.getString(MMTags.Image.CHANNEL_NAME);
    }
 
    public static Long getSliceIndex(File f) throws JSONException {
        JSONObject meta=parseMetadata(f);
        if (meta==null)
            return null;
        return meta.getLong(MMTags.Image.SLICE_INDEX);
    }
 
    public static Map<String,List<File>> getChannelMap(List<File> list) throws JSONException {
        Map<String,List<File>> map=new HashMap<String,List<File>>();
        for (File f:list) {
            String ch=Utils.getChannelName(f);
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
    
    public static Map<Long,List<File>> getSliceIndexMap(List<File> list) throws JSONException {
        Map<Long,List<File>> map=new HashMap<Long,List<File>>();
        for (File f:list) {
            Long slice=Utils.getSliceIndex(f);
            List<File> chList;
            if (!map.containsKey(slice)) {
                chList=new ArrayList<File>();
                map.put(slice,chList);
            } else {
                chList=(List<File>)map.get(slice);
            }
            chList.add(f);
        }
        return map;
    }
    
    public static boolean calibrateImage(ImagePlus imp, JSONObject meta) {
        try {
            double pixSize=meta.getDouble("PixelSizeUm");
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
    
    public static boolean imageStoragePresent(DefaultMutableTreeNode root) {
        Enumeration<DefaultMutableTreeNode> en = root.preorderEnumeration();
        while (en.hasMoreElements()) {
            DefaultMutableTreeNode node = en.nextElement();
            DataProcessor dp=(DataProcessor)node.getUserObject();
            if (dp instanceof ExtDataProcessor && ((ExtDataProcessor)dp).getProcName().equals(ProcessorTree.PROC_NAME_IMAGE_STORAGE)) {
                return true;
            }    
        }
        return false;
    }    
    
    public static JSONObject processortreeToJSONObject(DefaultMutableTreeNode root, DefaultMutableTreeNode node) throws JSONException {
        JSONObject pobj=new JSONObject();
        //Enumeration<DefaultMutableTreeNode> en=node.breadthFirstEnumeration();
        DataProcessor dp=(DataProcessor)node.getUserObject();
        JSONObject obj;
        if (dp instanceof ExtDataProcessor) {
            obj=((ExtDataProcessor)dp).getParameters();
        } else {
            obj=new JSONObject();
            obj.put("ProcName",dp.getClass().getSimpleName());
            obj.put("ClassName",dp.getClass().getName());
        }
        if (Utils.isDescendantOfImageStorageNode(root, node))
            obj.put("DataType",File.class.getName());
        else 
            obj.put("DataType",TaggedImage.class.getName());
        JSONArray processorArray=new JSONArray();
        for (int i=0; i<node.getChildCount(); i++) {
//        while (en.hasMoreElements()) {
//            DefaultMutableTreeNode childNode=en.nextElement();
            DefaultMutableTreeNode childNode=(DefaultMutableTreeNode)node.getChildAt(i);
            dp=(DataProcessor)childNode.getUserObject();
            processorArray.put(processortreeToJSONObject(root,childNode));
//            if (childNode.getChildCount()>0) {
//                dpObj.put("Child", ProcessortreeToJSONObject((DefaultMutableTreeNode)childNode.getFirstChild()));
//            } 
        }
        obj.put("ChildProcessors",processorArray);
        pobj.put("Processor", obj);
        return pobj;
    }
    
    
    public static DefaultMutableTreeNode createProcessorTree(JSONObject procObj) throws JSONException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        JSONObject proc=procObj.getJSONObject("Processor");
        String className=proc.getString("ClassName");
//        Class<?> clazz=Class.forName(className,true,this.getClass().getClassLoader());//DataProcessor.class.getClassLoader());
        Class<?> clazz=Class.forName(className);//DataProcessor.class.getClassLoader());
        DataProcessor dp=(DataProcessor)clazz.newInstance();
/*        DataProcessor dp;
        try {
            dp=createInstance(clazz<TaggedImage>);
        } catch (Exception ex) {
            dp=null;
        }*/
        if (dp instanceof ExtDataProcessor) {
            ((ExtDataProcessor)dp).setParameters(proc);
        } else {
        }
        DefaultMutableTreeNode node=new DefaultMutableTreeNode(dp);
        try {
            JSONArray children=proc.getJSONArray("ChildProcessors");
            if (children!=null && children.length()>0) {
                for (int i=0; i<children.length(); i++) {
                    JSONObject c=children.getJSONObject(i);
                    DefaultMutableTreeNode cNode=createProcessorTree(c);
                    node.add(cNode);
                }
            }
        } catch (JSONException ex) {
            IJ.log("Utils.createProcessorTree: "+className+": no children");
        }
        return node;
    }
    

}
