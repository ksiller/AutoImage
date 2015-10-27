package autoimage;

import autoimage.api.ExtImageTags;
import autoimage.api.SampleArea;
import autoimage.gui.ProcessorTree;
import autoimage.dataprocessors.ExtDataProcessor;
import autoimage.dataprocessors.SiteInfoUpdater;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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
import org.micromanager.utils.ReportingUtils;

/**
 *
 * @author Karsten
 */
public class Utils {

    /**
     * Rotates a point around the origin (0/0).
     * @param point Point to be rotated.
     * @param rad Rotation angle in rad.
     * @return Rotated point. The original point object remains unaltered.
     */
    public static Point2D rotatePoint(Point2D point, double rad) {
        double x = (Math.cos(rad) * (point.getX()) - Math.sin(rad) * (point.getY()));
        double y = (Math.sin(rad) * (point.getX()) + Math.cos(rad) * (point.getY()));
        return new Point2D.Double(x,y);
    }

    /**
     * Rotates a point around an anchor point.
     * @param point Point to be rotated.
     * @param anchor Anchor point around which the rotation occurs. (0/0) results in rotation around the origin.
     * @param rad Rotation angle in rad.
     * @return Rotated point. The original point object remains unaltered.
     */
    public static Point2D rotatePoint(Point2D point, Point2D anchor, double rad) {
        double x = (int)(anchor.getX() + Math.cos(rad) * (point.getX() - anchor.getX()) - Math.sin(rad) * (point.getY() - anchor.getY()));
        double y = (int)(anchor.getY() + Math.sin(rad) * (point.getX() - anchor.getX()) + Math.cos(rad) * (point.getY() - anchor.getY()));
        return new Point2D.Double(x,y);
/*        
        Point2D rotPoint=new Point2D.Double(point.getX(),point.getY());
        AffineTransform at=new AffineTransform();
        at.translate(anchor.getX(), anchor.getY());
        at.rotate(rad);
        at.translate(-anchor.getX(), -anchor.getY());
        at.transform(point, rotPoint);
        return rotPoint;*/
    }
    
    /**
     * Extracts the rotation component for an AffineTransform object
     * @param at AffineTransform for which the rotation component is to be determined
     * @return Rotation angle in rad
     */
    public static double getRotation(AffineTransform at) {
        return Math.atan2(at.getShearY(), at.getScaleY());
    }
    
    /**
     * Parses a Path2D object for all segments for which type==PathIterator.SEG_MOVETO (path start points)
     * @param path The Path2D object that will be parsed for MOVETO segments 
     * @return List of all 2D coordinates specified as SEG_MOVETO segments (=starting points). Returns null if th path object is null. Returns an empty list if no "move-to"segments are found.
     * Note a Path may contain multiple subpaths each with its own SEG_MOVETO segment
     */
    public static List<Point2D> getMoveToPoints(Path2D path) {
        if (path==null) {
            return null;
        }
        PathIterator pi=path.getPathIterator(null);
        List<Point2D> points=new ArrayList<Point2D>();
        while (!pi.isDone()) {
            double coords[]=new double[6];
            int type=pi.currentSegment(coords);
            if (type==PathIterator.SEG_MOVETO) {
                points.add(new Point2D.Double(coords[0],coords[1]));
            }
            pi.next();
        }
        return points;
    }

    /**
     * Creates a rectangular Path2D object defined by two points corresponding to opposite corners.
     * Note reversing start and end points does not affect the rectangle's shape, but does reverse the path direction along the rectangles outline 
     * @param start 2D point defining starting corner of rectangle
     * @param opposite 2D point defining rectangle corner opposite of start point
     * @return Path2D object of the rectangle specified by start and end points
     */
    public static Path2D createRectanglePath(Point2D start, Point2D opposite) {
        Path2D path=new Path2D.Double();
        path.moveTo(start.getX(), start.getY());
        path.lineTo(start.getX(), opposite.getY());
        path.lineTo(opposite.getX(), opposite.getY());
        path.lineTo(opposite.getX(),start.getY());
        path.lineTo(start.getX(), start.getY());
        path.closePath();
        return path;
    }
    
    /**
     * Measures angle between two 2D vectors
     * @param v1 2D vector; coordinates are interpreted as endpoints of vector originating at (0/0)
     * @param v2 2D vector; coordinates are interpreted as endpoints of vector originating at (0/0)
     * @return angle (in rad) between the vectors v1 and v2
     */
    public static double angle2D_Rad(Point2D.Double v1, Point2D.Double v2) {
        return Math.acos((v1.x*v2.x + v1.y*v2.y) / (Math.sqrt(v1.x*v1.x + v1.y*v1.y)*Math.sqrt(v2.x*v2.x + v2.y*v2.y)));
    }
       
    /**
     * Calculates the AffineTransform that best fits the mapping of an array of 2D source points to an array of 2D destination points.
     * @param src array of Point2D defining source points
     * @param dst array of Point2D defining source points
     * @return AffineTransform that best fits src-to-dst point mapping
     * @throws Exception 
     */
    public static AffineTransform calcAffTransform(Point2D[] src, Point2D[] dst) throws Exception {
        if (src == null || dst == null || src.length != dst.length || src.length==0)
            throw new Exception("calcAffTransform: illegal arguments.");
        AffineTransform at=null;
        //single point: translation
        if (src.length == 1) {
            at = new AffineTransform();
            at.translate(dst[0].getX() - src[0].getX(), dst[0].getY() - src[0].getY());
        }
        //two points: translation, scale, rotation
        if (src.length == 2) {
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
//            IJ.log("angle: "+angleRad/Math.PI*180+", scale: "+scale+", translate: "+(dst[0].x-src[0].x)+"/"+(dst[0].y-src[0].y));
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
        JSONObject meta=MMCoreUtils.parseMetadataFromFile(f);
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
        JSONObject meta=MMCoreUtils.parseMetadataFromFile(f);
        if (meta==null)
            return null;
        return meta.getString(MMTags.Image.CHANNEL_NAME);
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
    

/*    
    public static double lineDistance(Point2D l1start, Point2D l1end, Point2D l2start, Point2D l2end) {
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
*/    
    
    public static String getExtension(File f) {
        return f.getName().substring(f.getName().lastIndexOf("."));
    }
    
    public static Map<String,String> getAvailableAreaClasses() throws IOException {
        Class clazz = SampleArea.class;
        URL location = clazz.getResource('/'+clazz.getName().replace('.', '/')+".class");
        String locationStr=location.toString().substring(0, location.toString().indexOf(".jar!")+6);
        String jarFileStr=locationStr.substring(locationStr.indexOf("file:")+5,locationStr.length()-2);
        //replace forward slash with OS specific path separator
        jarFileStr.replace("/",File.pathSeparator);
        URLClassLoader classLoader;
        JarFile jarFile;
        jarFile = new JarFile(jarFileStr);
        Enumeration e = jarFile.entries();

        URL[] urls = { new URL(locationStr) };
        classLoader = URLClassLoader.newInstance(urls,
            //    this.getClass().getClassLoader());
        SampleArea.class.getClassLoader());

        int i=0;
        Map<String, String> availableAreaClasses = new HashMap<String,String>();
        while (e.hasMoreElements()) {
            JarEntry je = (JarEntry) e.nextElement();
            if(je.isDirectory() || !je.getName().endsWith(".class")){
                continue;
            }
             // -6 to remove ".class"
            String className = je.getName().substring(0,je.getName().length()-6);
            className = className.replace('/', '.');
            try {
                clazz=Class.forName(className);
                IJ.log(clazz.getName());
                //only add non-abstract SampleArea classes that support custom layouts
                if (SampleArea.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                    SampleArea area=(SampleArea)clazz.newInstance();
                    if ((area.supportedLayouts() & SampleArea.SUPPORT_CUSTOM_LAYOUT) == SampleArea.SUPPORT_CUSTOM_LAYOUT) {
                        availableAreaClasses.put(area.getShapeType(), className);
                    }   
                }
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                //catch all other exceptions so we can continue with remaining class definitions
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                IJ.log(ex.toString());
                for (StackTraceElement s:ex.getStackTrace()) {
                    IJ.log(s.getClassName()+", "+s.getMethodName()+", "+s.getLineNumber());
                }
            }                       
        }
        return availableAreaClasses; 
    }

    
/*    
    //expects rad
    public static AffineTransform createRotationAffineTrans(double rad) {
        AffineTransform at=new AffineTransform();
        at.rotate(rad);
        return at;
    }
*/
}
