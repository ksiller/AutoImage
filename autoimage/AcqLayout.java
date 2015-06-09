/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import autoimage.area.Area;
import ij.IJ;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

    
    
/**
 *
 * @author Karsten Siller
 */
public class AcqLayout  implements PropertyChangeListener {
    
    protected String name;
    protected boolean isEmpty;
    protected boolean isModified;
    protected double width;
    protected double height;
    protected double length;
    protected String bottomMaterial;
    protected double bottomThickness;
    protected String version;
    private AffineTransform stageToLayoutTransform;
    private AffineTransform layoutToStageTransform;
    private Vec3d normalVec;
    protected File file;
    
    private double escapeZPos; //z-stage is moved to this position when moving xystage to avoid collision with plate

    protected List<Area> areas;
    protected List<RefArea> landmarks;
    private ProgressMonitor tileCalcMonitor;
    private TileCalcTask tileTask;
  
    public static final String TAG_VERSION="VERSION";
    public static final String TAG_CLASS_NAME="CLASS";
    public static final String TAG_NAME="NAME";
    public static final String TAG_LAYOUT_WIDTH="LAYOUT_WIDTH";
    public static final String TAG_LAYOUT_LENGTH="LAYOUT_LENGTH";
    public static final String TAG_LAYOUT_HEIGHT="LAYOUT_HEIGHT";
    public static final String TAG_LAYOUT_BOTTOM_MATERIAL="BOTTOM_MATERIAL";
    public static final String TAG_LAYOUT_BOTTOM_THICKNESS="BOTTOM_THICKNESS";
    public static final String TAG_LAYOUT="LAYOUT";
    public static final String TAG_LANDMARK="LANDMARK";
    public static final String TAG_TILE_SEED_FILE="TILE_SEEDS";
    public static final String TAG_STAGE_X="STAGE_X";
    public static final String TAG_STAGE_Y="STAGE_Y";
    public static final String TAG_STAGE_Z="STAGE_Z";
    public static final String TAG_LAYOUT_COORD_X="LAYOUT_COORD_X";
    public static final String TAG_LAYOUT_COORD_Y="LAYOUT_COORD_Y";
    public static final String TAG_LAYOUT_COORD_Z="LAYOUT_COORD_Z";
//    public static final String TAG_REF_IMAGE_FILE="REF_IMAGE_FILE";
    
    private static final String VERSION="1.0";
    private static final double ESCAPE_ZPOS_SAFETY=50; //keeps z-stage at least 50um below plate
    
    
    
    class TileCalcTask extends SwingWorker<Void, Void> {
        
        private ThreadPoolExecutor executor;
        
        public TileCalcTask(ThreadPoolExecutor executor) {
            this.executor=executor;
        }   
        
        @Override
        public Void doInBackground() {
//            int progress = 0;
            setProgress(0);
            try {
                Thread.sleep(20);
                while (!executor.isTerminated() && !isCancelled()) {
                    Thread.sleep(200);
                    setProgress((int)executor.getCompletedTaskCount());
                }
            } catch (InterruptedException ignore) {
            }
            return null; 
        }
 
        @Override
        public void done() {
//            executor.shutdown();
            if (isCancelled()) {
            //    IJ.showMessage("cancelled");
            } else {
              //  IJ.showMessage("finished");
//            tileCalcMonitor.setProgress(0);
            }    
        }
    }
    
    public class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {
    
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.out.println(r.toString() + " is rejected");
        }
    }    

    
    public AcqLayout() {
        createEmptyLayout();
        

    }

/*    public AcqLayout(JSONObject obj, File f) {
        isEmpty=true;
        IJ.log("AcqLayout.loading from JSONObject");
        if (obj!=null) {
            try {
                initializeFromJSONObject(obj);
                isEmpty=false;
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JSONException ex) {
                Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
            }
        }    
        if (isEmpty) {
            version="1.0";
            createEmptyLayout();
        } else {
            isModified=false;
            file=f;
        }
        try {
            calcStageToLayoutTransform();
//        tileManager=new TileManager(this);
        } catch (Exception ex) {
            Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
*/  
    
    public static AcqLayout loadLayout(File file) {//returns true if layout has been changed
//        IJ.log("AcqFrame.loadLayout: "+absPath);
        BufferedReader br;
        StringBuilder sb=new StringBuilder();
        JSONObject layoutObj=null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) !=null) {
                sb.append(line);
            }
            JSONObject obj=new JSONObject(sb.toString());
            layoutObj=obj.getJSONObject(AcqLayout.TAG_LAYOUT);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
//        acqLayout=new AcqLayout(layoutObj, file);
        AcqLayout layout=AcqLayout.createFromJSONObject(layoutObj, file);
        return layout;
    }
    
    public static AcqLayout createFromJSONObject(JSONObject obj, File f) {
        IJ.log("AcqLayout.loading from JSONObject");
        //create empty layout;
        AcqLayout layout=null;
        if (obj!=null) {
            String className;
            try {
                //dynamic class loading
                className = obj.getString(TAG_CLASS_NAME);
                Class clazz=Class.forName(className);
                layout=(AcqLayout) clazz.newInstance();
                layout.initializeFromJSONObject(obj);
                layout.isEmpty=false;
                layout.isModified=false;
                layout.file=f;
                try {
                    layout.calcStageToLayoutTransform();
        //        tileManager=new TileManager(this);
                } catch (Exception ex) {
                    IJ.log("Error calculating stageToLayout transform");
                    Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
                }        
            } catch (ClassNotFoundException ex) {
                layout=null;
                Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                layout=null;
                Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                layout=null;
                Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JSONException ex) {
                layout=null;
                Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return layout;
    }
    
    protected void initializeFromJSONObject(JSONObject obj) throws JSONException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        landmarks=new ArrayList<RefArea>();
        areas=new ArrayList<Area>();
        version=obj.getString(TAG_VERSION);
        name=obj.getString(TAG_NAME);
        width=obj.getDouble(TAG_LAYOUT_WIDTH);
        length=obj.getDouble(TAG_LAYOUT_LENGTH);
        height=obj.getDouble(TAG_LAYOUT_HEIGHT);
        bottomMaterial=obj.getString(TAG_LAYOUT_BOTTOM_MATERIAL);
        bottomThickness=obj.getDouble(TAG_LAYOUT_BOTTOM_THICKNESS);
        JSONArray areaArray=obj.getJSONArray(Area.TAG_AREA_ARRAY);
        for (int i=0;i<areaArray.length(); i++) {
            JSONObject areaObj=areaArray.getJSONObject(i);
            Area area=Area.createFromJSONObject(areaObj);
            areas.add(area);
        }
        JSONArray landmarkArray=obj.getJSONArray(RefArea.TAG_LANDMARK_ARRAY);
        for (int i=0;i<landmarkArray.length(); i++) {
            JSONObject landmarkObj=landmarkArray.getJSONObject(i);
            landmarks.add(new RefArea(landmarkObj));
        }
    }
    
    public JSONObject toJSONObject () throws JSONException {
        IJ.log(this.getClass().getName());
        JSONObject obj=new JSONObject();
        obj.put(TAG_VERSION,version);
        obj.put(TAG_CLASS_NAME,this.getClass().getName());
        obj.put(TAG_NAME,name);
        obj.put(TAG_LAYOUT_WIDTH,width);
        obj.put(TAG_LAYOUT_LENGTH,length);
        obj.put(TAG_LAYOUT_HEIGHT,height);
        obj.put(TAG_LAYOUT_BOTTOM_MATERIAL,bottomMaterial);
        obj.put(TAG_LAYOUT_BOTTOM_THICKNESS,bottomThickness);
        JSONArray areaArray=new JSONArray();
        for (Area a:areas) {
            areaArray.put(a.toJSONObject());
        }
        obj.put(Area.TAG_AREA_ARRAY,areaArray);
        JSONArray landmarkArray=new JSONArray();
        for (RefArea refa:landmarks) {
            landmarkArray.put(refa.toJSONObject());
        }
        obj.put(RefArea.TAG_LANDMARK_ARRAY,landmarkArray);
        return obj;
    }
    
    //returns true if gaps exist between tiles
    //considers camera field rotation and stage-to-layout rotation
    public boolean hasGaps(FieldOfView fov, double tileOverlap) {
        Rectangle2D fovROI=fov.getROI_Pixel(1);
        Point2D tileOffset=Area.calculateTileOffset(fovROI.getWidth(), fovROI.getHeight(), tileOverlap);
        AffineTransform rot=new AffineTransform();
        rot.rotate(fov.getFieldRotation()-getStageToLayoutRot(),fovROI.getWidth()/2,fovROI.getHeight()/2);
        AffineTransform tOrigin=new AffineTransform();
        tOrigin.translate(-fovROI.getWidth()/2,-fovROI.getHeight()/2);
        java.awt.geom.Area[] a=new java.awt.geom.Area[4];
        for (int i=0; i<4; i++) {
            a[i]=new java.awt.geom.Area(fovROI);
            a[i].transform(rot);
            a[i].transform(tOrigin);
            switch (i) {
                case 1: {
                    AffineTransform transl=new AffineTransform();
                    transl.translate(tileOffset.getX(), 0);
                    a[i].transform(transl);
                    break;
                }    
                case 2: {
                    AffineTransform transl=new AffineTransform();
                    transl.translate(0,tileOffset.getY());
                    a[i].transform(transl);
                    break;
                }    
                case 3: {
                    AffineTransform transl=new AffineTransform();
                    transl.translate(tileOffset.getX(), tileOffset.getY());
                    a[i].transform(transl);
                    break;
                }    
            }
        }

        double centerX=tileOffset.getX()/2;
        double centerY=tileOffset.getY()/2;
        return (!(a[0].contains(centerX, centerY) && a[3].contains(centerX, centerY)) 
                                        || (a[1].contains(centerX, centerY) && a[2].contains(centerX, centerY)));
    }
    
    public void setWidth(double w) {
        width=w;
    }
    
    public void setHeight(double h) {
        height=h;
    }
    
    public void setLength(double l) {
        length=l;
    }
    
    //returns minimal relative tile overlap (0 <= tileOverlap <=1) to eliminate all gaps
    //considers camera field rotation and stage-to-layout rotation
    public double closeTilingGaps(FieldOfView fov, double accuracy) {
        double newOverlap=0;
        double lowOverlap=0;
        double highOverlap=1;
        Rectangle2D fovROI=fov.getROI_Pixel(1);
        while (highOverlap-lowOverlap > accuracy) {
            newOverlap=(highOverlap-lowOverlap)/2+lowOverlap;

            Point2D tileOffset=Area.calculateTileOffset(fovROI.getWidth(), fovROI.getHeight(), newOverlap);
            double centerX=tileOffset.getX()/2;
            double centerY=tileOffset.getY()/2;

            AffineTransform rot=new AffineTransform();
            rot.rotate(fov.getFieldRotation()-getStageToLayoutRot(),fovROI.getWidth()/2,fovROI.getHeight()/2);
            AffineTransform tOrigin=new AffineTransform();
            tOrigin.translate(-fovROI.getWidth()/2,-fovROI.getHeight()/2);
            java.awt.geom.Area[] a=new java.awt.geom.Area[4];            
            for (int i=0; i<4; i++) {
                a[i]=new java.awt.geom.Area(fovROI);
                a[i].transform(rot);
                a[i].transform(tOrigin);
                switch (i) {
                    case 1: {
                        AffineTransform transl=new AffineTransform();
                        transl.translate(tileOffset.getX(), 0);
                        a[i].transform(transl);
//                      a[i]=a[0].createTransformedArea(transl);
                        break;
                    }    
                    case 2: {
                        AffineTransform transl=new AffineTransform();
                        transl.translate(0,tileOffset.getY());
                        a[i].transform(transl);
                        break;
                    }    
                    case 3: {
                        AffineTransform transl=new AffineTransform();
                        transl.translate(tileOffset.getX(), tileOffset.getY());
                        a[i].transform(transl);
                        break;
                    }
                }
            }
            if ((a[0].contains(centerX, centerY) && a[3].contains(centerX, centerY)) 
                    || (a[1].contains(centerX, centerY) && a[2].contains(centerX, centerY))) {
                highOverlap=newOverlap;
            } else {//tiling gap
                lowOverlap=newOverlap;
            }
        }
        return newOverlap;
    }
    
    
    //in radians
    public double getStageToLayoutRot() {
        if (stageToLayoutTransform!=null)
            return Math.atan2(stageToLayoutTransform.getShearY(), stageToLayoutTransform.getScaleY());
        else
            return 0;
    }
    
    //in radians
    public double getLayoutToStageRot() {
        if (layoutToStageTransform!=null)
            return Math.atan2(layoutToStageTransform.getShearY(), layoutToStageTransform.getScaleY());
        else
            return 0;
    }
    
    public File getFile() {
        return file;
    }
    
    public void setFile(File f) {
        file=f;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String n) {
        name=n;
    }
    
    public int createUniqueAreaId() {
       boolean exists=true;
       int id=0;
       while (exists) {
            id++;
            exists=false;
            for (int i=0; i<areas.size(); i++) {
                if (id==areas.get(i).getId()) {
                    exists=true;
                    break;
                }
            }
       }
       return id;
    }   
  
    public int getNoOfMappedStagePos() {
        int mappedPoints=0;
        if (landmarks!=null) {
            for (RefArea rp:landmarks)
                if (rp.isStagePosFound())
                    mappedPoints++;
        }
        return mappedPoints;
    }
    
    // calculates 2D affine transform and normal vector for layout
    public void calcStageToLayoutTransform() {
        List<RefArea> mappedLandmarks=getMappedLandmarks();
        if (mappedLandmarks.size() > 0) {
            Point2D.Double[] src=new Point2D.Double[mappedLandmarks.size()];
            Point2D.Double[] dst=new Point2D.Double[mappedLandmarks.size()];
            int i=0;
            for (RefArea rp:mappedLandmarks) {
                src[i]= new Point2D.Double(rp.getStageCoordX(),rp.getStageCoordY());
                dst[i]= new Point2D.Double(rp.getLayoutCoordX(),rp.getLayoutCoordY());
                i++;
            }
            try {
                stageToLayoutTransform=Utils.calcAffTransform(src, dst);
            } catch (Exception ex) {
                stageToLayoutTransform=new AffineTransform();
            }
        } else {
            stageToLayoutTransform=new AffineTransform();
        }
        try {    
            layoutToStageTransform=stageToLayoutTransform.createInverse();
        } catch (NoninvertibleTransformException ex) {
            layoutToStageTransform=new AffineTransform();
        }
        calcNormalVector();        
    }
    
    //returns angle between vertical vector and normalVec of layout (in degree)
    public double getTilt() throws Exception {
        if (normalVec.z >= 0) {
            Vec3d vertical=new Vec3d (0,0,1);
            return vertical.angle(normalVec);
        } else {
            Vec3d vertical=new Vec3d (0,0,-1);
            return vertical.angle(normalVec);
        }    
    }

    //returns list of RefArea for which stagePosMapped==true
    public List<RefArea> getMappedLandmarks() {
        List<RefArea> list=new ArrayList<RefArea>(landmarks.size());
        if (landmarks!=null) {
            for (RefArea rp:landmarks) {
                if (rp.isStagePosFound()) {
                    list.add(rp);
                }    
            }
        }
        return list;
    }
    
    private void calcNormalVector() {
        List<RefArea> mappedLandmarks=getMappedLandmarks();
        if (mappedLandmarks.size() >= 2) {
            Vec3d v1;// = new Vector3d(0,0,0);
            Vec3d v2;// = new Vector3d(0,0,0);
            Vec3d v3;// = new Vector3d(0,0,0);
            
            if (mappedLandmarks.size() == 2) {//2 mapped stage positions
                v1=new Vec3d(mappedLandmarks.get(0).getStageCoordX(),mappedLandmarks.get(0).getStageCoordY(),mappedLandmarks.get(0).getStageCoordZ()-mappedLandmarks.get(0).getLayoutCoordZ());
                v2=new Vec3d(mappedLandmarks.get(1).getStageCoordX(),mappedLandmarks.get(1).getStageCoordY(),mappedLandmarks.get(1).getStageCoordZ()-mappedLandmarks.get(1).getLayoutCoordZ());
                v3=new Vec3d(v1.x+v2.y-v1.y,v1.y-(v2.x-v1.x),v1.z);
            
            } else {// at least 3 mapped stage positions
                v1=new Vec3d(mappedLandmarks.get(0).getStageCoordX(),mappedLandmarks.get(0).getStageCoordY(),mappedLandmarks.get(0).getStageCoordZ()-mappedLandmarks.get(0).getLayoutCoordZ());
                v2=new Vec3d(mappedLandmarks.get(1).getStageCoordX(),mappedLandmarks.get(1).getStageCoordY(),mappedLandmarks.get(1).getStageCoordZ()-mappedLandmarks.get(1).getLayoutCoordZ());
                v3=new Vec3d(mappedLandmarks.get(2).getStageCoordX(),mappedLandmarks.get(2).getStageCoordY(),mappedLandmarks.get(2).getStageCoordZ()-mappedLandmarks.get(2).getLayoutCoordZ());
                //need to catch case of 3 co-linear vectors
                try {
                    if ((Vec3d.cross(v2.minus(v1), v3.minus(v1))).length() == 0) {//co-linear vectors
                        //ignore third point and reset
                        v3=new Vec3d(v2.y-v1.y,-(v2.x-v1.x),v1.z);
                    }
                } catch (Exception ex) {
                    //this can never occur
                    Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
//            Vec3d a=new Vec3d(v2.x-v1.x,v2.y-v1.y,v2.z-v1.z);
//            Vec3d b=new Vec3d(v3.x-v1.x,v3.y-v1.y,v3.z-v1.z);
            try {
                Vec3d a = v2.minus(v1);
                Vec3d b = v3.minus(v1);
//                IJ.log("a: "+a.toString()+ ", b: "+ b.toString());
                //normalVec = new Vec3d();
                normalVec = Vec3d.cross(a,b);
            } catch (Exception ex) {
                //this can never occur
                Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
            }
/*            
            if (normalVec.z<0)
                normalVec.negate();
*/
            double len=Math.sqrt(normalVec.x*normalVec.x+normalVec.y*normalVec.y+normalVec.z*normalVec.z);

            if (len!=0)
                normalVec.scale(1/len);
            
//        normalVec.normalize();
        } else {
            normalVec=new Vec3d(0,0,1);
        }    
        //determine lowest corner to set z-pos when moving xy-stage

//        RefArea rp = getLandmark(0);
//        if (rp!=null) {
            try {
                Vec3d p1=convertLayoutToStagePos(0,0,0);
/*            double x=rp.convertLayoutCoordToStageCoord_X(0);
            double y=rp.convertLayoutCoordToStageCoord_Y(0);
            double z1=((-normalVec.x*(x-rp.getStageCoordX())-normalVec.y*(y-rp.getStageCoordY()))/normalVec.z)+rp.getStageCoordZ()-rp.getLayoutCoordZ();
*/            
                Vec3d p2=convertLayoutToStagePos(width,0,0);
/*            x=rp.convertLayoutCoordToStageCoord_X(width);
            y=rp.convertLayoutCoordToStageCoord_Y(0);
            double z2=((-normalVec.x*(x-rp.getStageCoordX())-normalVec.y*(y-rp.getStageCoordY()))/normalVec.z)+rp.getStageCoordZ()-rp.getLayoutCoordZ();
*/            
                Vec3d p3=convertLayoutToStagePos(width,length,0);
/*            x=rp.convertLayoutCoordToStageCoord_X(width);
            y=rp.convertLayoutCoordToStageCoord_Y(length);
            double z3=((-normalVec.x*(x-rp.getStageCoordX())-normalVec.y*(y-rp.getStageCoordY()))/normalVec.z)+rp.getStageCoordZ()-rp.getLayoutCoordZ();
*/            
                Vec3d p4=convertLayoutToStagePos(0,length,0);
/*            x=rp.convertLayoutCoordToStageCoord_X(0);
            y=rp.convertLayoutCoordToStageCoord_Y(length);
            double z4=((-normalVec.x*(x-rp.getStageCoordX())-normalVec.y*(y-rp.getStageCoordY()))/normalVec.z)+rp.getStageCoordZ()-rp.getLayoutCoordZ();
*/            
                escapeZPos=Math.min(Math.min(Math.min(p1.z,p2.z),p3.z),p4.z)-ESCAPE_ZPOS_SAFETY;
            } catch (Exception ex) {
                escapeZPos=0;
//                Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
            }
//        } else
//          escapeZPos=0;
    }
    
    public Vec3d convertStageToLayoutPos(double stageX, double stageY, double stageZ) throws Exception {
        Point2D layoutXY=convertStageToLayoutPos_XY(new Point2D.Double(stageX,stageY));
        //double layoutZ=0;
        double layoutZ=stageZ-getStageZPosForStageXYPos(stageX,stageY);
        Vec3d lCoord=new Vec3d(layoutXY.getX(),layoutXY.getY(),layoutZ);
        return lCoord;
    }
    
    public Vec3d convertLayoutToStagePos(double layoutX, double layoutY, double layoutZ) throws Exception {
        Point2D xy=convertLayoutToStagePos_XY(new Point2D.Double(layoutX,layoutY));
//        double z=getStageZPosForLayoutPos(layoutX,layoutY);
        //get z stage position in layout reference plane (layoutZ=0);
        double z=getStageZPosForStageXYPos(xy.getX(),xy.getY());
        //add layoutZ
        Vec3d sCoord=new Vec3d(xy.getX(),xy.getY(),z+layoutZ);
        return sCoord;
    }
      
    public Point2D convertStageToLayoutPos_XY(Point2D stageXY) {
        Point2D layoutXY=new Point2D.Double();
        stageToLayoutTransform.transform(stageXY, layoutXY);
        return layoutXY;
    }
    
    public Point2D convertLayoutToStagePos_XY(Point2D layoutXY) {
        Point2D stageXY=new Point2D.Double();
        layoutToStageTransform.transform(layoutXY, stageXY);
        return stageXY;
    }
    
    //returns stageZ pos that corresponds to layout reference plane (layoutZ = 0) at stageX/stageY pos
    public double getStageZPosForStageXYPos(double stageX, double stageY) throws Exception {
        if (getNoOfMappedStagePos() == 0)
            throw new Exception("Converting z position: no Landmarks defined");
        RefArea rp=getMappedLandmarks().get(0);
//        double z=((-normalVec.x*(layoutX-rp.getStageCoordX())-normalVec.y*(layoutY-rp.getStageCoordY()))/normalVec.z)+rp.getStageCoordZ()-rp.getLayoutCoordZ();
        //ensure that length of normal vector = 1;
//        Vec3d normalVecN=Vec3d.normalized(normalVec);
        //calculate z pos in layout reference plane
        double z=((-normalVec.x*(stageX-rp.getStageCoordX())-normalVec.y*(stageY-rp.getStageCoordY()))/normalVec.z)+rp.getStageCoordZ()-rp.getLayoutCoordZ();
        return z;
    }
/*    
    //faulty: does not take stageToLayoutTransform into account
    public double getStageZPosForLayoutPos(double layoutX, double layoutY) throws Exception {
        if (getNoOfMappedStagePos() == 0)
            throw new Exception("Converting z position: no Landmarks defined");
        RefArea rp=getMappedLandmarks().get(0);
//        double z=((-normalVec.x*(layoutX-rp.getStageCoordX())-normalVec.y*(layoutY-rp.getStageCoordY()))/normalVec.z)+rp.getStageCoordZ()-rp.getLayoutCoordZ();
        double z=((-normalVec.x*(layoutX-rp.getLayoutCoordX())-normalVec.y*(layoutY-rp.getLayoutCoordY()))/normalVec.z)+rp.getStageCoordZ()-rp.getLayoutCoordZ();
        return z;
    }
*/    
    public double getEscapeZPos() {
        return escapeZPos;
    }
    
    public AffineTransform getStageToLayoutTransform() {
        return stageToLayoutTransform;
    }
    
    public AffineTransform getLayoutToStageTransform() {
        return layoutToStageTransform;
    }
    

    public Vec3d getNormalVector() {
        return normalVec;
    }
    
    public boolean isEmpty() {
        return isEmpty;
    }
    
    public double getWidth() {
        return width;
    }
    
    public double getLength() {
        return length;
    }
    
    public double getHeight() {
        return height;
    }    

    public String getBottomMaterial() {
        return bottomMaterial;
    }
    
    public double getBottomThickness() {
        return bottomThickness;
    }
    
    public double getStagePosX(Area a, int tileIndex) {
        if (a!=null && landmarks!=null && landmarks.size()>0) 
            return landmarks.get(0).getStageCoordX()-landmarks.get(0).getLayoutCoordX()+a.getCenterPos().getX();
        else
            return 0;
    }
    
    public double getStagePosY(Area a, int tileIndex) {
        if (a!=null && landmarks!=null && landmarks.size()>0) 
            return landmarks.get(0).getStageCoordY()-landmarks.get(0).getLayoutCoordY()+a.getCenterPos().getY();
        else
            return 0;
    }
    
    public List<RefArea> getLandmarks(){
        return landmarks;
    }
    
    public RefArea getLandmark(int index) {
        if (landmarks!=null && landmarks.size()>index)
            return landmarks.get(index);
        else 
            return null;
    }
    
    public void setLandmarks(List<RefArea> lm) {
        landmarks=lm;
        try {
            calcStageToLayoutTransform();
        } catch (Exception ex) {
            Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void deselectAllLandmarks() {
        if (landmarks!=null) {
            for (int i=0; i<landmarks.size(); i++) {
                landmarks.get(i).setSelected(false);
            }
        }
    }
    
    public void setLandmark(int index, RefArea lm) {
        if (landmarks!=null & landmarks.size()>index) {
            RefArea oldLm=landmarks.get(index);
            oldLm.setStageCoord(lm.getStageCoordX(), lm.getStageCoordY(), lm.getStageCoordZ());
            oldLm.setLayoutCoord(lm.getLayoutCoordX(),lm.getLayoutCoordY(), lm.getLayoutCoordZ());
            oldLm.setDimension(lm.getPhysWidth(), lm.getPhysHeight());
            oldLm.setZDefined(lm.isZPosDefined());
            oldLm.setName(lm.getName());
            oldLm.setRefImageFile(lm.getRefImageFile());
            oldLm.setStagePosMapped(lm.isStagePosFound());
            oldLm.setSelected(lm.isSelected());
        } else
            addLandmark(lm);
    }
     
    private void setLandmarkStageCoord(int index, double sX, double sY, double sZ) {
        if (landmarks!=null & landmarks.size()>index)
            landmarks.get(index).setStageCoord(sX, sY, sZ);
    }
    
    public void addLandmark(RefArea lm) {
        if (landmarks==null)
            landmarks=new ArrayList<RefArea>();
        landmarks.add(lm);
    }
    
    public void deleteLandmark(int index) {
        landmarks.remove(index);
    }
    
    public void deleteLandmark(RefArea lm) {
        landmarks.remove(lm);
    }
    
    public List<Area> getAreaArray() {
        return areas;
    }

    public void setAreaArray(List<Area> al) {
        areas=al;
    }
    
    public Area getAreaByIndex(int index) {
        if (index>=0 & areas!=null & index<areas.size())
            return areas.get(index);
        else
            return null;
    }
    
    public Area getAreaByName(String name) {
        if (name==null)
            return null;
        for (Area a:areas) {
            if (a.getName().equals(name))
                return a;
        }
        return null;
    }
    
    public Area getAreaByLayoutPos(double lx, double ly) {
        for (Area a:areas) {
            if (a.isInArea(lx,ly))
                return a;
        }
        return null;
    }
    
    //areas are loaded with sequential IDs, starting with 1 --> Area with id is most likey to be in areas.get(id-1). If not, the entire AreaArray is searched for the ID
    public Area getAreaById(int id) {
        if ((id < 1 || id > areas.size()) || id!=areas.get(id-1).getId()) {
            int index=-1;
            for (int i=0; i<areas.size(); i++) {
                if (id==areas.get(i).getId()) {
//                    index=i;
//                    break;
                    return areas.get(i);
                } 
            }
            if (index!=-1)
                return areas.get(index);
            else
                return null;
        }
        else return areas.get(id-1);
    }
    
    public void addArea(Area a) {
        if (a.getName().equals("")) {
//            IJ.log("name not defined");   
  
            a.setName("Area "+Integer.toString(areas.size()+1));
        }
        areas.add(a);
        isModified=true;
    }
    
    public void deleteArea(int index) {
        areas.remove(index);
        isModified=true;
    }
        
    public int getNoOfSelectedAreas() {
        int sel=0;
        for (Area area : areas) {
            if (area.isSelectedForAcq()) {
                sel++;
            }
        }
        return sel;
    }
    
    public int getNoOfSelectedClusters() {
        int sel=0;
        for (Area area : areas) {
            if (area.isSelectedForAcq()) {
                sel=area.getNoOfClusters()+sel;
            }
        }
        return sel;
        
    }

    //returns null if layout coordinate is not inside any area
    public Area getFirstContainingArea(double lx, double ly) {
        int i=0;
        int index=-1;
        while ((index==-1) && (i<areas.size())) {
            if (areas.get(i).isInArea(lx,ly))
                index=i;
            i++;
        }
        if (index!=-1)
            return areas.get(index);
        else
            return null;
    }

    //returns null if fov surrounding layout coordinate does not touch any area
    public Area getFirstTouchingArea(double lx, double ly, double fovX, double fovY) {
        int i=0;
        int index=-1;
        while ((index==-1) && (i<areas.size())) {
            if (areas.get(i).doesFovTouchArea(lx,ly, fovX, fovY))
                index=i;
            i++;
        }
        if (index!=-1)
            return areas.get(index);
        else
            return null;
    }
    
    //parameters are absolute stage positions
    public Area getFirstContainingAreaAbs(double stageX, double stageY, double fovX, double fovY) {
        int i=0;
        int index=-1;
        RefArea lm=getLandmark(0);
        if (lm!=null) {
            double x=stageX-(lm.getStageCoordX()-lm.getLayoutCoordX());
            double y=stageY-(lm.getStageCoordY()-lm.getLayoutCoordY());
            Point2D xy=convertStageToLayoutPos_XY(new Point2D.Double(stageX, stageY));
            boolean found=false;
            while ((index==-1) && (i<areas.size())) {
                if (areas.get(i).doesFovTouchArea(xy.getX(),xy.getY(),fovX,fovY))
                    index=i;
                i++;
            }
            if (index!=-1)
                return areas.get(index);
            else
                return null;
        } else
            return null;
    }
    
    public int[] getAllContainingAreaIndices(double x, double y) {
        int[] indices = new int[areas.size()];
        int j=0;
        for (int i=0; i<areas.size(); i++) {
            if (areas.get(i).isInArea(x,y))
                indices[j]=i;    
        }
        return indices;
    }
    
    
    public ArrayList<Area> getAllAreasTouching(double x, double y) {
        ArrayList<Area> a = new ArrayList<Area>(areas.size());
        for (int i=0; i<areas.size(); i++) {
            if (areas.get(i).isInArea(x,y))
                a.add(areas.get(i));    
        }
        return a; 
    }

    public ArrayList<Area> getUnselectedAreasTouching(double x, double y) {
        ArrayList<Area> al = new ArrayList<Area>(areas.size());
        for (Area area:areas) {
            if (!area.isSelectedForAcq() && area.isInArea(x,y))
                al.add(area);    
        }
        return al; 
    }

    public ArrayList<Area> getSelectedAreasTouching(double x, double y) {
        ArrayList<Area> al = new ArrayList<Area>(areas.size());
        for (Area area:areas) {
            if (area.isSelectedForAcq() && area.isInArea(x,y))
                al.add(area);    
        }
        return al; 
    }

    public ArrayList<Area> getAllAreasInsideRect(Rectangle2D.Double r) {
        ArrayList<Area> a = new ArrayList<Area>(areas.size());
        for (int i=0; i<areas.size(); i++) {
            if (areas.get(i).isInsideRect(r))
                a.add(areas.get(i));    
        }
        return a; 
    }
    
    public ArrayList<Area> getUnselectedAreasInsideRect(Rectangle2D.Double r) {
        ArrayList<Area> al = new ArrayList<Area>(areas.size());
        for (Area area:areas) {
            if (!area.isSelectedForAcq() && area.isInsideRect(r))
                al.add(area);    
        }
        return al; 
    }
        
    public ArrayList<Area> getSelectedAreasInsideRect(Rectangle2D.Double r) {
        ArrayList<Area> al = new ArrayList<Area>(areas.size());
        for (Area area:areas) {
            if (area.isSelectedForAcq() && area.isInsideRect(r))
                al.add(area);    
        }
        return al; 
    }
        
    private void createEmptyLayout() {
        version=VERSION;
        width=19999; //physical dim in um
        length=10000; //physical dim in um
        height=1000; //physical dim in um
        bottomMaterial="Glass";
        bottomThickness=170; //in um
        areas = new ArrayList<Area>();
        landmarks=new ArrayList<RefArea>();
        name="not selected";
        file = new File("","not selected");
        escapeZPos = 50; //in um; z-stage is moved to this position when moving xystage to avoid collision with plate
        isEmpty=true;
        isModified=false;;
        calcStageToLayoutTransform();
    
/*
    ProgressMonitor tileCalcMonitor;
    TileCalcTask tileTask;        
*/                
    }
    

    //fired by TileCalcTask
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName()) ) {
            int progress = (Integer) evt.getNewValue();
            tileCalcMonitor.setProgress(progress);
            String message =
                String.format("Completed %d%%.\n", (int)((double)progress/areas.size()*100));
            tileCalcMonitor.setNote(message);
            if (tileCalcMonitor.isCanceled() || tileTask.isDone()) {
                if (tileCalcMonitor.isCanceled()) {
                    tileTask.cancel(true);
                } 
//                tileCalcMonitor.close();
            }
        }
    }
    
   
    private void writeSingleTile(XMLStreamWriter xtw, Tile t) throws XMLStreamException {
        XMLUtils.wStartElement(xtw, Area.TAG_TILE);
            XMLUtils.writeLine(xtw, Area.TAG_NAME, t.name);
            XMLUtils.writeLine(xtw, Area.TAG_CENTER_X, java.lang.Double.toString(t.centerX));
            XMLUtils.writeLine(xtw, Area.TAG_CENTER_Y, java.lang.Double.toString(t.centerY));
            XMLUtils.writeLine(xtw, Area.TAG_REL_POS_Z, java.lang.Double.toString(t.relZPos));
        XMLUtils.wEndElement(xtw);
    }

    
    public void writeSingleArea(XMLStreamWriter xtw, Area a, TilingSetting setting) throws XMLStreamException {
        XMLUtils.wStartElement(xtw, Area.TAG_AREA);
            XMLUtils.writeLine(xtw, Area.TAG_CLASS, a.getClass().getName());
            XMLUtils.writeLine(xtw, Area.TAG_NAME, a.getName());
            XMLUtils.writeLine(xtw, Area.TAG_TOP_LEFT_X, java.lang.Double.toString(a.getTopLeftX()));
            XMLUtils.writeLine(xtw, Area.TAG_TOP_LEFT_Y, java.lang.Double.toString(a.getTopLeftY()));
            XMLUtils.writeLine(xtw, Area.TAG_WIDTH, java.lang.Double.toString(a.getWidth()));
            XMLUtils.writeLine(xtw, Area.TAG_HEIGHT, java.lang.Double.toString(a.getHeight()));
            XMLUtils.writeLine(xtw, Area.TAG_REL_POS_Z, java.lang.Double.toString(a.getRelPosZ()));
            XMLUtils.writeLine(xtw, Area.TAG_COMMENT, a.getComment());
            List<Tile> tl=a.getTilePositions();
            if (tl!=null) {
                if (setting.getMode()==TilingSetting.Mode.FULL || !setting.isCluster()) {
                    for (Tile tile:tl) {
                        writeSingleTile(xtw,tile);
                    }    
                } else {
                    //determine topLeftX, topLeftY, width and length of cluster
                    int lastIndex=-1;
                    int firstIndex=-1;
                    int tilesPerCluster=setting.getNrClusterTileX()*setting.getNrClusterTileY();
                    int clusters=Math.round(a.getTileNumber()/tilesPerCluster);
                    for (int i=0; i< clusters; i++) {
                        double x=0;
                        double y=0;
                        double relZPos=0;
                        for (int j=0;j<tilesPerCluster; j++) {
                            relZPos+=tl.get(i*tilesPerCluster+j).relZPos;
                            x=x+tl.get(i*tilesPerCluster+j).centerX;
                            y=y+tl.get(i*tilesPerCluster+j).centerY;
                        }    
                        relZPos=relZPos/tilesPerCluster;
                        x=x/tilesPerCluster;
                        y=y/tilesPerCluster;
//                        Area.Tile tile=a.createTile("Site"+Integer.toString(i),x,y,relZPos);
                        //write Cluster info
                        XMLUtils.wStartElement(xtw, Area.TAG_CLUSTER);
                            XMLUtils.writeLine(xtw, Area.TAG_NAME, "Cluster"+java.lang.Integer.toString(i+1));
                            XMLUtils.writeLine(xtw, Area.TAG_CENTER_X, java.lang.Double.toString(x));
                            XMLUtils.writeLine(xtw, Area.TAG_CENTER_Y, java.lang.Double.toString(y));
                            XMLUtils.writeLine(xtw, Area.TAG_REL_POS_Z, java.lang.Double.toString(relZPos));
                            //write individual Tile info
                            for (int j=0;j<tilesPerCluster; j++) {
                                writeSingleTile(xtw,tl.get(i*tilesPerCluster+j));
                            }    
                        XMLUtils.wEndElement(xtw);
                    }    
                }
            }
        XMLUtils.wEndElement(xtw);
    }
  
    public void saveTileCoordsToXMLFile(String fname, TilingSetting setting, double tileW, double tileH, double pixSize) {
        XMLUtils.initialize();
        XMLOutputFactory xof =  XMLOutputFactory.newInstance(); 
        try { 
            try { 
                XMLStreamWriter xtw; 
                xtw = xof.createXMLStreamWriter(new FileOutputStream(fname), "UTF-8"); 
                xtw.writeStartDocument("utf-8","1.0"); 
                xtw.writeCharacters(XMLUtils.LINE_FEED);
                
                XMLUtils.wStartElement(xtw, TAG_TILE_SEED_FILE);
               

                    XMLUtils.writeLine(xtw, TAG_VERSION, "1.0");
                    
                        XMLUtils.wStartElement(xtw, Tile.TILECONFIG_TAG);
                            XMLUtils.writeLine(xtw, Tile.TAG_TILE_WIDTH, java.lang.Double.toString(tileW));
                            XMLUtils.writeLine(xtw, Tile.TAG_TILE_HEIGHT, java.lang.Double.toString(tileH));
                            XMLUtils.writeLine(xtw, Tile.TAG_TILE_OVERLAP, java.lang.Double.toString(setting.getTileOverlap()));
                            XMLUtils.writeLine(xtw, Tile.TAG_PIX_SIZE, java.lang.Double.toString(pixSize));
                            XMLUtils.writeLine(xtw, Tile.TAG_UNITS, "um");
                        XMLUtils.wEndElement(xtw);
                    
                    for (Area a:areas) {
                        if (a.isSelectedForAcq())
                            writeSingleArea(xtw, a, setting);
                    }
                
                XMLUtils.wEndElement(xtw);
                
                xtw.writeEndDocument(); 
                xtw.flush();
                xtw.close(); 
            } catch (XMLStreamException ex) { 
                IJ.log(getClass().getName()+": saveFileCoordsToXMLFile");
            } 
        } catch (FileNotFoundException ex) { 

        }     
    }

            
    private void readTileCoordsForArea(XMLStreamReader xrt, List<List<Tile>> groupList) {
        String areaName="";
        String clusterName="";
        String tileName="";
        double centerX=0;
        double centerY=0;
        double relZ=0;
        List<Tile> tileList = new ArrayList<Tile>();
        try {
            while (xrt.hasNext()) {
                xrt.next();
                int event=xrt.getEventType();
                String local=null;
                if (event == XMLStreamReader.START_ELEMENT || event == XMLStreamReader.END_ELEMENT) {
                    local=xrt.getLocalName();
                }
                if (xrt.getEventType()==XMLStreamReader.END_ELEMENT && local.equals(Area.TAG_AREA)) {
                    break;
                } else if (event == XMLStreamReader.START_ELEMENT) {
                    if (local.equals(Area.TAG_NAME)) {
                        areaName=xrt.getElementText()+"-";
                    } else if (local.equals(Area.TAG_CLUSTER)) { //read cluster
                        tileList=new ArrayList<Tile>();
                        while (xrt.hasNext()) {
                            xrt.next();
                            event=xrt.getEventType();
                            if (event == XMLStreamReader.START_ELEMENT || event == XMLStreamReader.END_ELEMENT) {
                                local=xrt.getLocalName();
                            }
                            if (event==XMLStreamReader.END_ELEMENT && local.equals(Area.TAG_CLUSTER)) {
                                clusterName="";
                                groupList.add(tileList);
                                tileList=null; //st to null so that tileList does not get added again at end of parsing
                                break;
                            } else if (event == XMLStreamReader.START_ELEMENT) {
                                if (local.equals(Area.TAG_NAME)) {
                                    clusterName=xrt.getElementText()+"-";
                                } else if (local.equals(Area.TAG_TILE)) { //read tile
                                    while (xrt.hasNext()) {
                                        xrt.next();
                                        event=xrt.getEventType();
                                        if (event == XMLStreamReader.START_ELEMENT || event == XMLStreamReader.END_ELEMENT) {
                                            local=xrt.getLocalName();
                                        }
                                        if (event==XMLStreamReader.END_ELEMENT && local.equals(Area.TAG_TILE))
                                                break;
                                        else if (event == XMLStreamReader.START_ELEMENT) {
                                            if (local.equals(Area.TAG_NAME)) {
                                                tileName=xrt.getElementText();
                                            } else if (local.equals(Area.TAG_CENTER_X)) {
                                                centerX=Double.parseDouble(xrt.getElementText());
                                            } else if (local.equals(Area.TAG_CENTER_Y)) {
                                                centerY=Double.parseDouble(xrt.getElementText());
                                            } else if (local.equals(Area.TAG_REL_POS_Z)) {
                                                relZ=Double.parseDouble(xrt.getElementText());
                                            }    
                                        
                                        }
                                    }    
                                    tileList.add(Tile.createTile(areaName+tileName,centerX,centerY,relZ));
                                }
                            }    
                        }   
                    } else if (event == XMLStreamReader.START_ELEMENT && local.equals(Area.TAG_TILE)) { //read tile
                        while (xrt.hasNext()) {
                            xrt.next();
                            event=xrt.getEventType();
                            if (event == XMLStreamReader.START_ELEMENT || event == XMLStreamReader.END_ELEMENT) {
                                local=xrt.getLocalName();
                            }
                            if (event==XMLStreamReader.END_ELEMENT && local.equals(Area.TAG_TILE)) {
                                break;
                            } else if (event == XMLStreamReader.START_ELEMENT) {
                                if (local.equals(Area.TAG_NAME)) {
                                        tileName=xrt.getElementText();
                                } else if (local.equals(Area.TAG_CENTER_X)) {
                                        centerX=Double.parseDouble(xrt.getElementText());
                                } else if (local.equals(Area.TAG_CENTER_Y)) {
                                        centerY=Double.parseDouble(xrt.getElementText());
                                } else if (local.equals(Area.TAG_REL_POS_Z)) {
                                        relZ=Double.parseDouble(xrt.getElementText());
                                }  
                            }  
                        }
                        tileList.add(Tile.createTile(areaName+tileName,centerX,centerY,relZ));
                    }
                }
            }
            if (tileList!=null) //if not cluster, tileList still needs to be added to groupList
                groupList.add(tileList);
        } catch (XMLStreamException ex) {
        }        
    } 
       
    public List<List<Tile>> readTileCoordsFromXMLFile(String fname) {
        List<List<Tile>> list = new ArrayList<List<Tile>>();
        try {
            try {
                XMLInputFactory xif = XMLInputFactory.newInstance();
                XMLStreamReader xrt = xif.createXMLStreamReader(new FileInputStream(fname));
                while(xrt.hasNext()) {
                    xrt.next();
                    if (xrt.getEventType() == XMLStreamReader.START_ELEMENT){
                        String s=xrt.getLocalName();
                        if (s.equals(TAG_TILE_SEED_FILE)) {
                                
                        } else if (s.equals(Area.TAG_AREA)) {
                            readTileCoordsForArea(xrt,list);
                        }    
                    }
                }
                xrt.close();
            } catch (XMLStreamException ex) { 
                return list;
            } 
        } catch (FileNotFoundException ex) { 
            return null;
        } 
        return list;
       
    }
        
    //returns index of first area with duplicate name
    public int hasDuplicateAreaNames() {
        if (areas!=null) {
            for (int i=0; i<areas.size(); i++) {
                for (int j=i+1; j<areas.size(); j++) {
                    if (areas.get(i).getName().equals(areas.get(j).getName()))
                        return i;
                }
            }    
            return -1;
        } else
            return -1;
    }

    public void removeAreaById(int id) {
        if (areas!=null) {
            for (int i=0; i<areas.size(); i++) {
                if (areas.get(i).getId()==id)
                    areas.remove(i);
            }
        }
        isModified=true;
    }

    public void setModified(boolean b) {
        isModified=b;
    }
    
    public boolean isModified() {
        return isModified;
    }
    
    public boolean isAreaRenamingAllowed() {
        return true;
    }
    
    public boolean isAreaMergingAllowed() {
        return true;
    }
    
    public boolean isAreaAdditionAllowed() {
        return true;
    }
    
    public boolean isAreaRemovalAllowed() {
        return true;
    }
    
    public Comparator getAreaNameComparator() {
        return Area.NameComparator;
    }

    public Comparator getTileNumberComparator() {
        return Area.TileNumComparator;
    }
}

