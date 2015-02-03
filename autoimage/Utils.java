/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import autoimage.AcqSetting.ScheduledTime;
import autoimage.dataprocessors.ExtDataProcessor;
import autoimage.dataprocessors.SiteInfoUpdater;
import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.plugin.RGBStackMerge;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import mmcorej.TaggedImage;
import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.LUDecompositionImpl;
import org.apache.commons.math.linear.RealMatrix;
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
    

    public static ImageProcessor[] createImageProcessor(TaggedImage ti, boolean scale) {
        try {
            int width = ti.tags.getInt(MMTags.Image.WIDTH);
            int height = ti.tags.getInt(MMTags.Image.HEIGHT);
            String pixType = ti.tags.getString(MMTags.Image.PIX_TYPE);
            ImageProcessor[] ipArray=null;
            if (pixType.equals("GRAY8")) {
                ipArray=new ImageProcessor[1];
                ipArray[0] = new ByteProcessor(width, height, (byte[]) ti.pix);
                if (!scale)
                    ipArray[0].setMinAndMax(0, 255);
            } else if (pixType.equals("GRAY16")) {
                ipArray=new ImageProcessor[1];
                ipArray[0] = new ShortProcessor(width, height, (short[]) ti.pix, null);
                if (!scale)
                    ipArray[0].setMinAndMax(0, 65535);
            } else if (pixType.equals("RGB32")) {
                ipArray=new ImageProcessor[1];
                if (ti.pix instanceof byte[]) {
                    //convert byte[] to int[] 
                    byte[] byteArray=(byte[])ti.pix;
                    int[] intArray = new int[byteArray.length/4];
                    for (int i=0; i<intArray.length; ++i) {
                        intArray[i] =  byteArray[4*i]
                                     + (byteArray[4*i + 1] << 8)
                                     + (byteArray[4*i + 2] << 16);
                    }
                    ipArray[0]=new ColorProcessor(width, height, intArray);
                } else {
                    ipArray[0]=new ColorProcessor(width, height, (int[]) ti.pix);
                }
                if (!scale)
                    ipArray[0].setMinAndMax(0, 255);
            } else if (pixType.equals("RGB64")) {
                    if (ti.pix instanceof short[]) {
                        short[] shortArray=(short[])ti.pix;
                        ipArray=new ImageProcessor[3];
                        for (int i=0; i<3; ++i) {//iterate over B, G, R channels
                            short[] channelArray=new short[shortArray.length/4];
                            for (int pixelPos=0; pixelPos<channelArray.length; pixelPos++) {
                                channelArray[pixelPos] =  shortArray[4*pixelPos+i];
                            }
                            //create new ShortProcessor for this channel and add to array
                            //return in R, G, B order
                            ipArray[2-i]=new ShortProcessor(width, height, channelArray, null);
                            if (!scale)
                                ipArray[2-i].setMinAndMax(0, 65535);
                  	}
                    }
                //cannot handle this
            } else {
                ipArray=null;
            }    
            return ipArray;
        } catch (JSONException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public static ImagePlus createImagePlus(TaggedImage ti, boolean scale) {
        try {
            ImageProcessor[] ipArray=createImageProcessor(ti, scale);
            if (ipArray!=null && ipArray.length==1) {
                return new ImagePlus(ti.tags.getString(MMTags.Image.POS_NAME),ipArray[0]);
            } else {
                ImagePlus[] impArray=new ImagePlus[ipArray.length]; 
                for (int i=0; i<ipArray.length; i++) {
                    impArray[i]=new ImagePlus(Integer.toString(i),ipArray[i]);
                }
                RGBStackMerge merger=new RGBStackMerge();
                return merger.mergeHyperstacks(impArray, false);
            }
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
            DefaultMutableTreeNode infoProcNode=new DefaultMutableTreeNode(new SiteInfoUpdater(ProcessorTree.PROC_NAME_ACQ_ENG,null));
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
//        ImagePlus imp = new Opener().openImage(dataFile.getAbsolutePath());
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

    
    public static JSONObject readMetadata(Object element, boolean createCopy) throws JSONException {
        JSONObject meta=null;
        if (element instanceof TaggedImage) {
            if (createCopy) {
                meta=new JSONObject(((TaggedImage)element).tags.toString());
            } else{
                meta=((TaggedImage)element).tags;
            }    
        } else if (element instanceof java.io.File) {
            meta=Utils.parseMetadata((java.io.File)element);
        }
        return meta;
    }
    
    public static JSONObject readMetadataSummary(Object element, boolean createCopy) throws JSONException {
        return readMetadata(element, false).getJSONObject(MMTags.Root.SUMMARY);
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
        String directory = summary.getString(MMTags.Summary.DIRECTORY); //"Directory"
        String prefix = summary.getString(MMTags.Summary.PREFIX);
        String positionName=ti.tags.getString(MMTags.Image.POS_NAME); //"PositionName"
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
        tags[fieldArray.length]=ExtImageTags.AREA_NAME;
//        tags[fieldArray.length+8]="AreaIndex";
        tags[fieldArray.length+1]=ExtImageTags.CLUSTER_INDEX;
        tags[fieldArray.length+2]=ExtImageTags.AREA_COMMENT;
        tags[fieldArray.length+3]=ExtImageTags.SITE_INDEX;
/*        
        tags[fieldArray.length+4]=ExtImageTags.CHANNEL_INDEX_ORIG;
        tags[fieldArray.length+5]=ExtImageTags.SLICE_INDEX_ORIG;
        tags[fieldArray.length+6]=ExtImageTags.POS_INDEX_ORIG;
        tags[fieldArray.length+7]=ExtImageTags.FRAME_INDEX_ORIG;*/
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
    
    //expects two 2D vectors
    public static double angle2D_Rad(Point2D.Double v1, Point2D.Double v2) {
        return Math.acos((v1.x*v2.x + v1.y*v2.y) / (Math.sqrt(v1.x*v1.x + v1.y*v1.y)*Math.sqrt(v2.x*v2.x + v2.y*v2.y)));
    }
       

    
    
    public static AffineTransform calcAffTransform(Point2D.Double[] src, Point2D.Double[] dst) throws Exception {
        if (src == null || dst == null || src.length != dst.length || src.length==0)
            throw new Exception("calcAffTransform: illegal arguments.");
        AffineTransform at=null;
        //single point: translation
        if (src.length == 1) {
            at = new AffineTransform();
            at.translate(dst[0].x - src[0].x, dst[0].y - src[0].y);
        }
        //two points: translation, scale, rotation
        if (src.length == 2) {
/*            Point2D.Double v1=new Point2D.Double(src[1].x-src[0].x, src[1].y-src[0].y);
            Point2D.Double v2=new Point2D.Double(dst[1].x-dst[0].x, dst[1].y-dst[0].y);
            IJ.log("v1: "+v1.toString()+", v2:"+v2.toString());
            at = new AffineTransform();
            double angleRad=0;
            double scale=1.0;
            if (!v1.equals(v2)) {
                angleRad=Utils.angle2D_Rad(v1, v2);
                IJ.log("angleRad: "+angleRad);
                double length_src=Math.sqrt(
                        ((src[1].x-src[0].x)*(src[1].x-src[0].x))
                       +((src[1].y-src[0].y)*(src[1].y-src[0].y)));
                double length_dst=Math.sqrt(
                        ((dst[1].x-dst[0].x)*(dst[1].x-dst[0].x))
                       +((dst[1].y-dst[0].y)*(dst[1].y-dst[0].y)));
                scale=length_dst/length_src;
                at.rotate(angleRad);
                //uniform scale
                at.scale(scale,scale);
                at.translate(dst[0].x - src[0].x, dst[0].y - src[0].y);
            }
            IJ.log("1. angle: "+angleRad/Math.PI*180+", scale: "+scale+", translate: "+(dst[0].x-src[0].x)+"/"+(dst[0].y-src[0].y));
            IJ.log("1. "+at.toString());
            */
            double dx=-(src[1].getY()-src[0].getY());
            double dy=src[1].getX()-src[0].getX();
            Point2D src2=new Point2D.Double(src[0].getX()+dx,src[0].getY()+dy);
            dx=-(dst[1].getY()-dst[0].getY());
            dy=(dst[1].getX()-dst[0].getX());
            Point2D dst2=new Point2D.Double(dst[0].getX()+dx,dst[0].getY()+dy);
            Array2DRowRealMatrix x = new Array2DRowRealMatrix(new double[][] {
            { src[0].getX(), src[1].getX(), src2.getX() }, { src[0].getY(), src[1].getY(), src2.getY() },{ 1, 1, 1 } });
            Array2DRowRealMatrix y = new Array2DRowRealMatrix(new double[][] {
            { dst[0].getX(), dst[1].getX(), dst2.getX() }, { dst[0].getY(), dst[1].getY(), dst2.getY() },{ 0, 0, 0 } });
            double determinant=new LUDecompositionImpl(x).getDeterminant();
            RealMatrix inv_x=new LUDecompositionImpl(x).getSolver().getInverse();
            double[][] matrix = y.multiply(inv_x).getData();
            at = new AffineTransform(new double[] { matrix[0][0], matrix[1][0], matrix[0][1], matrix[1][1], matrix[0][2], matrix[1][2] });
//            IJ.log("1mod. angle: "+angleRad/Math.PI*180+", scale: "+scale+", translate: "+(dst[0].x-src[0].x)+"/"+(dst[0].y-src[0].y));
            IJ.log("1mod. "+at.toString());
            IJ.log("1mod. angle: "+Math.atan2(at.getShearY(), at.getScaleY())/Math.PI*180);
        }
        //three points: translation, scale, rotation, shear
        if (src.length > 2) {
            Array2DRowRealMatrix x = new Array2DRowRealMatrix(new double[][] {
            { src[0].getX(), src[1].getX(), src[2].getX() }, { src[0].getY(), src[1].getY(), src[2].getY() },{ 1, 1, 1 } });
            Array2DRowRealMatrix y = new Array2DRowRealMatrix(new double[][] {
            { dst[0].getX(), dst[1].getX(), dst[2].getX() }, { dst[0].getY(), dst[1].getY(), dst[2].getY() },{ 0, 0, 0 } });
            double determinant=new LUDecompositionImpl(x).getDeterminant();
            RealMatrix inv_x=new LUDecompositionImpl(x).getSolver().getInverse();
            double[][] matrix = y.multiply(inv_x).getData();
            at = new AffineTransform(new double[] { matrix[0][0], matrix[1][0], matrix[0][1], matrix[1][1], matrix[0][2], matrix[1][2] });
        }
        return at;
    }
    
    public static double MedianDouble(List<Double> values){
        Collections.sort(values); 
        if (values.size() % 2 == 1)
            return values.get((values.size()+1)/2-1);
        else {
            return (values.get(values.size()/2-1) + values.get(values.size()/2)) / 2.0;
        }
    }
    
    public static boolean isNaN(Object x) {
        return x!=x;
    }
    
    public static Point2D rotatePoint(Point2D point, double rad) {
        double x = (Math.cos(rad) * (point.getX()) - Math.sin(rad) * (point.getY()));
        double y = (Math.sin(rad) * (point.getX()) + Math.cos(rad) * (point.getY()));
        return new Point2D.Double(x,y);
    }

    public static Point2D rotatePoint(Point2D point, Point2D anchor, double rad) {
        double x = (int)(anchor.getX() + Math.cos(rad) * (point.getX() - anchor.getX()) - Math.sin(rad) * (point.getY() - anchor.getY()));
        double y = (int)(anchor.getY() + Math.sin(rad) * (point.getX() - anchor.getX()) + Math.cos(rad) * (point.getY() - anchor.getY()));
        return new Point2D.Double(x,y);
    }
    
    public static double distance(Point2D l1start, Point2D l1end, Point2D l2start, Point2D l2end) {
        double dist=0;
        double m1=0;
        double m2=0;
        double mi=0;
        double b1;
        double b2;
        double bi;
        if (l1end.getX() == l1start.getX() && l2end.getX() == l2start.getX()) {
            dist=Math.abs(l2start.getX() - l1start.getX());
        }
        if (l1end.getX() != l1start.getX() && l2end.getX() != l2start.getX()) {
            m1=(l1end.getY()-l1start.getY())/(l1end.getX()-l1start.getX());
    //        b1=l1start.getY()-m1*l1start.getX();
            m2=(l2end.getY()-l2start.getY())/(l2end.getX()-l2start.getX());
            b2=l2start.getY()-m2*l2start.getX();
            
            mi=-1/m1;
            bi=l1end.getY()-mi*l1end.getX();
            double xi=(m2-mi)/(bi-b2);
            double yi=mi*xi+bi;
            
            dist=Math.sqrt((xi-l1end.getX()) * (xi-l1end.getX()) + (yi-l1end.getY()) * (yi-l1end.getY()));
        }    
        return dist;
    }
    
    public static TaggedImage openAsTaggedImage(String filename) throws JSONException {
        ImagePlus imp=IJ.openImage(filename);
        TaggedImage ti=ImageUtils.makeTaggedImage(imp.getProcessor());
        ti.tags=readMetadata(new File(filename),true);
        return ti;
    }    
    
    public static String getExtension(File f) {
        return f.getName().substring(f.getName().lastIndexOf("."));
    }
    
    public void leastSquareFitFor3DPlane(List<RefArea> refPoints) {
        //convert into coordinate arrays
        double x[] = new double[refPoints.size()];
        double y[] = new double[refPoints.size()];
        double z[] = new double[refPoints.size()];
        double xm=0;//avarage x
        double ym=0;//average y
        double zm=0;//average z
        int i=0;
        for (RefArea ra:refPoints) {
            x[i]=ra.getStageCoordX();
            y[i]=ra.getStageCoordY();
            z[i]=ra.getStageCoordZ()-ra.getLayoutCoordZ();
            xm+=x[i];
            ym+=y[i];
            zm+=z[i];
            i++;
        }
        xm/=refPoints.size();
        ym/=refPoints.size();
        zm/=refPoints.size();
        //subtract mean to avoid illdefined equations down the line
        for (i=0; i< x.length; i++) {
            x[i]=x[i]-xm;
            y[i]=y[i]-ym;
            z[i]=z[i]-zm;
        }
        //create matrix
        double m[][] = new double[3][3];
        m[0][0]=0;
        for (i=0; i<x.length; i++) {
            m[0][0]+=x[i]*x[i];
            m[0][1]+=x[i]*y[i];
            m[0][2]+=x[i];
            m[1][0]+=x[i]*y[i];
            m[1][1]+=y[i]*y[i];
            m[1][2]+=y[i];
            m[2][0]+=x[i];
            m[2][1]+=y[i];
        }
        m[2][2]=1;
        
        double n[] = new double[3];
        for (i=0; i<x.length; i++) {
            n[0]+=x[i]*z[i];
            n[1]+=y[i]*z[i];
            n[2]+=z[i];
        }
    }    
    
}
