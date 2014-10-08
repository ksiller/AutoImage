/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import ij.IJ;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
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
class AcquisitionLayout  implements PropertyChangeListener {
    
    private String name;
    private boolean isEmpty;
    private boolean isModified;
    private double originX;
    private double originY;
    private double width;
    private double height;
    private String version;
    private AffineTransform stageToLayoutTransform;
    private AffineTransform layoutToStageTransform;
    private Vec3d normalVec;
    private File file;
    
    private static int inst=0;

    private double escapeZPos; //z-stage is moved to this position when moving xystage to avoid collision with plate

    private List<Area> areas;
    private List<RefArea> landmarks;
    private ProgressMonitor tileCalcMonitor;
    private TileCalcTask tileTask;
//    private TileManager tileManager;
  
    public static final String TAG_VERSION="VERSION";
    public static final String TAG_NAME="NAME";
    public static final String TAG_LAYOUT_WIDTH="LAYOUT_WIDTH";
    public static final String TAG_LAYOUT_HEIGHT="LAYOUT_HEIGHT";
    public static final String TAG_LAYOUT="LAYOUT";
    public static final String TAG_LANDMARK="LANDMARK";
    public static final String TAG_TILE_SEED_FILE="TILE_SEEDS";
    public static final String TAG_STAGE_X="STAGE_X";
    public static final String TAG_STAGE_Y="STAGE_Y";
    public static final String TAG_STAGE_Z="STAGE_Z";
    public static final String TAG_PHYS_WIDTH="PHYS_WIDTH";
    public static final String TAG_PHYS_HEIGHT="PHYS_HEIGHT";
    public static final String TAG_LAYOUT_COORD_X="LAYOUT_COORD_X";
    public static final String TAG_LAYOUT_COORD_Y="LAYOUT_COORD_Y";
    public static final String TAG_LAYOUT_COORD_Z="LAYOUT_COORD_Z";
    public static final String TAG_REF_IMAGE_FILE="REF_IMAGE_FILE";
    
    private static final int STITCH_ALL_SLICES = -1;
    private static final int STITCH_CENTER_SLICE = -2;
    private static final int STITCH_ALL_TIMEPOINTS = -1;
    private static final int FILE_READING_OK = 1;
    private static final int FILE_READING_INCOMPLETE = -3;
    private static final int FILE_DROPPED_COORDS = -4;
    private static final int FILE_NOT_FOUND = -1;
    private static final int FILE_FORMAT_ERROR=-2;
    private static final int FILE_NO_COORDS=-3;
    private static final int FILE_NO_MATCHING_AREAS=-5;
    private static final double ESCAPE_ZPOS_SAFETY=50; //keeps z-stage at least 50um below plate
    
    
    private int indent;
    
    
    class TileCalcTask extends SwingWorker<Void, Void> {
        
        private ThreadPoolExecutor executor;
        
        public TileCalcTask(ThreadPoolExecutor executor) {
            this.executor=executor;
        }   
        
        @Override
        public Void doInBackground() {
            int progress = 0;
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

    
    public AcquisitionLayout(JSONObject obj, File f) {
        isEmpty=true;
        if (obj!=null) {
            landmarks=new ArrayList<RefArea>();
            try {
                areas=new ArrayList<Area>();
                version=obj.getString(TAG_VERSION);
                name=obj.getString(TAG_NAME);
                width=obj.getDouble(TAG_LAYOUT_WIDTH);
                height=obj.getDouble(TAG_LAYOUT_HEIGHT);
                JSONArray areaArray=obj.getJSONArray(Area.TAG_AREA_ARRAY);
                for (int i=0;i<areaArray.length(); i++) {
                    JSONObject areaObj=areaArray.getJSONObject(i);
                    Area area=Area.createFromJSONObject(areaObj);
                    area.setId(i);
                    areas.add(area);
                }
                JSONArray landmarkArray=obj.getJSONArray(RefArea.TAG_LANDMARK_ARRAY);
                for (int i=0;i<landmarkArray.length(); i++) {
                    JSONObject landmarkObj=landmarkArray.getJSONObject(i);
                    landmarks.add(new RefArea(landmarkObj));
                }
                isEmpty=false;
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(AcquisitionLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(AcquisitionLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(AcquisitionLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JSONException ex) {
                Logger.getLogger(AcquisitionLayout.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(AcquisitionLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public JSONObject toJSONObject () throws JSONException {
        JSONObject obj=new JSONObject();
        obj.put(TAG_VERSION,version);
        obj.put(TAG_NAME,name);
        obj.put(TAG_LAYOUT_WIDTH,width);
        obj.put(TAG_LAYOUT_HEIGHT,height);
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
    
    //is true if no gaps exist between tiles
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
    
    //returns minimal realtive tile overlap (0 <= tileOverlap <=1) to eliminate all gaps
    //considers camera field rotation and stage-to-layout rotation
    public double closeTilingGaps(FieldOfView fov, double accuracy) {
        double newOverlap=0;
        double lowOverlap=0;
        double highOverlap=1;
        Rectangle2D fovROI=fov.getROI_Pixel(1);
        while (highOverlap-lowOverlap > accuracy) {
            newOverlap=(highOverlap-lowOverlap)/2+lowOverlap;

//            IJ.log("AcquisitionLayout.closingTilingGaps: while loop, before Area.calulateTileOffset: angle: "+Double.toString(fov.getFieldRotation()/Math.PI*180)+"new overlap: "+Double.toString(newOverlap));
            Point2D tileOffset=Area.calculateTileOffset(fovROI.getWidth(), fovROI.getHeight(), newOverlap);
            double centerX=tileOffset.getX()/2;
            double centerY=tileOffset.getY()/2;
//            IJ.log("AcquisitionLayout.closingTilingGaps: while loop, after Area.calulateTileOffset, tileOffset: "+tileOffset.toString());

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
//                IJ.log("a["+i+"].centerX: "+Double.toString(a[i].getBounds2D().getCenterX())+", centerY: "+Double.toString(a[i].getBounds2D().getCenterY()));    

            }
//            IJ.log("centerX: "+Double.toString(centerX)+", centerY: "+Double.toString(centerY));    

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

/*    
    public static int getNoOfMappedStagePos(List<RefArea> refList) {
        int mappedPoints=0;
        if (refList!=null) {
            for (RefArea rp:refList)
                if (rp.isStagePosFound())
                    mappedPoints++;
        }
        return mappedPoints;
    }
*/    
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
    public double getTilt() {
        Vec3d vertical=new Vec3d (0,0,1);
        return vertical.angle(normalVec);
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
//        IJ.log("AcquisitionLayout.calcNormalVector: begin...again");
        List<RefArea> mappedLandmarks=getMappedLandmarks();
        if (mappedLandmarks.size() >= 2) {
//            IJ.log("2 or more landmarks");
            Vec3d v1;// = new Vector3d(0,0,0);
            Vec3d v2;// = new Vector3d(0,0,0);
            Vec3d v3;// = new Vector3d(0,0,0);
            
            if (mappedLandmarks.size() == 2) {//2 mapped stage positions
                v1=new Vec3d(mappedLandmarks.get(0).getStageCoordX(),mappedLandmarks.get(0).getStageCoordY(),mappedLandmarks.get(0).getStageCoordZ()-mappedLandmarks.get(0).getLayoutCoordZ());
                v2=new Vec3d(mappedLandmarks.get(1).getStageCoordX(),mappedLandmarks.get(1).getStageCoordY(),mappedLandmarks.get(1).getStageCoordZ()-mappedLandmarks.get(1).getLayoutCoordZ());
                v3=new Vec3d(v2.y-v1.y,-(v2.x-v1.x),v1.z);
            
            } else {// at least 3 mapped stage positions
                v1=new Vec3d(mappedLandmarks.get(0).getStageCoordX(),mappedLandmarks.get(0).getStageCoordY(),mappedLandmarks.get(0).getStageCoordZ()-mappedLandmarks.get(0).getLayoutCoordZ());
                v2=new Vec3d(mappedLandmarks.get(1).getStageCoordX(),mappedLandmarks.get(1).getStageCoordY(),mappedLandmarks.get(1).getStageCoordZ()-mappedLandmarks.get(1).getLayoutCoordZ());
                v3=new Vec3d(mappedLandmarks.get(2).getStageCoordX(),mappedLandmarks.get(2).getStageCoordY(),mappedLandmarks.get(2).getStageCoordZ()-mappedLandmarks.get(2).getLayoutCoordZ());
                //need to catch case of 3 co-linear vectors
                try {
                    if ((Vec3d.cross(v2.minus(v1), v3.minus(v1))).length() == 0) {//co-linear vectors
                        v3=new Vec3d(v2.y-v1.y,-(v2.x-v1.x),v1.z);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(AcquisitionLayout.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            Vec3d a=new Vec3d(v2.x-v1.x,v2.y-v1.y,v2.z-v1.z);
            Vec3d b=new Vec3d(v3.x-v1.x,v3.y-v1.y,v3.z-v1.z);
            //normalVec = new Vec3d();
            normalVec = Vec3d.cross(a,b);
            if (normalVec.z<0)
            normalVec.negate();
            double len=Math.sqrt(normalVec.x*normalVec.x+normalVec.y*normalVec.y);
            if (len!=0)
                normalVec.scale(1/len);
//        normalVec.normalize();
        } else {
//            IJ.log("no or singe landmarks");
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
                Vec3d p3=convertLayoutToStagePos(width,height,0);
/*            x=rp.convertLayoutCoordToStageCoord_X(width);
            y=rp.convertLayoutCoordToStageCoord_Y(height);
            double z3=((-normalVec.x*(x-rp.getStageCoordX())-normalVec.y*(y-rp.getStageCoordY()))/normalVec.z)+rp.getStageCoordZ()-rp.getLayoutCoordZ();
*/            
                Vec3d p4=convertLayoutToStagePos(0,height,0);
/*            x=rp.convertLayoutCoordToStageCoord_X(0);
            y=rp.convertLayoutCoordToStageCoord_Y(height);
            double z4=((-normalVec.x*(x-rp.getStageCoordX())-normalVec.y*(y-rp.getStageCoordY()))/normalVec.z)+rp.getStageCoordZ()-rp.getLayoutCoordZ();
*/            
                escapeZPos=Math.min(Math.min(Math.min(p1.z,p2.z),p3.z),p4.z)-ESCAPE_ZPOS_SAFETY;
            } catch (Exception ex) {
                escapeZPos=0;
                Logger.getLogger(AcquisitionLayout.class.getName()).log(Level.SEVERE, null, ex);
            }
//        } else
//          escapeZPos=0;
//        IJ.log("AcquisitionLayout.calcNormalVector: end.");
    }
    
    public Vec3d convertStagePosToLayoutPos(double stageX, double stageY, double stageZ) {
        Point2D xy=convertStageToLayoutPos_XY(new Point2D.Double(stageX,stageY));
        double z=0;
        Vec3d lCoord=new Vec3d(xy.getX(),xy.getY(),z);
        return lCoord;
    }
    
    public Vec3d convertLayoutToStagePos(double layoutX, double layoutY, double layoutZ) throws Exception {
        Point2D xy=convertLayoutToStagePos_XY(new Point2D.Double(layoutX,layoutY));
        double z=getStageZPosForLayoutPos(layoutX,layoutY);
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
    
    public double getStageZPosForLayoutPos(double layoutX, double layoutY) throws Exception {
        if (getNoOfMappedStagePos() == 0)
            throw new Exception("Converting z position: no Landmarks defined");
        RefArea rp=getMappedLandmarks().get(0);
//        double z=((-normalVec.x*(layoutX-rp.getStageCoordX())-normalVec.y*(layoutY-rp.getStageCoordY()))/normalVec.z)+rp.getStageCoordZ()-rp.getLayoutCoordZ();
        double z=((-normalVec.x*(layoutX-rp.getLayoutCoordX())-normalVec.y*(layoutY-rp.getLayoutCoordY()))/normalVec.z)+rp.getStageCoordZ()-rp.getLayoutCoordZ();
        return z;
    }
    
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
    
    
    public double getHeight() {
        return height;
    }
    
    

    public double getStagePosX(Area a, int tileIndex) {
        if (a!=null && landmarks!=null && landmarks.size()>0) 
            return landmarks.get(0).getStageCoordX()-landmarks.get(0).getLayoutCoordX()+a.getCenterX();
        else
            return 0;
    }
    
    public double getStagePosY(Area a, int tileIndex) {
        if (a!=null && landmarks!=null && landmarks.size()>0) 
            return landmarks.get(0).getStageCoordY()-landmarks.get(0).getLayoutCoordY()+a.getCenterY();
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
            Logger.getLogger(AcquisitionLayout.class.getName()).log(Level.SEVERE, null, ex);
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
    
/*    public void setLandmarkFound(boolean b) {
        for (RefArea landmark : landmarks) {
            landmark.setStagePosMapped(b);
        }
    }
*/    
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
        for (int i=0; i<areas.size(); i++)
            if (areas.get(i).isSelectedForAcq())
                sel++;
        return sel;
    }
    
    
    public int getFirstContainingAreaIndex(double lx, double ly) {
        int i=0;
        int index=-1;
        boolean found=false;
        while ((index==-1) && (i<areas.size())) {
                if (areas.get(i).isInArea(lx,ly))
                    index=i;
                i++;
        }
        return index;
    }
    
    
    public Area getFirstContainingArea(double lx, double ly, double fovX, double fovY) {
        int i=0;
        int index=-1;
        boolean found=false;
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
            boolean found=false;
            while ((index==-1) && (i<areas.size())) {
                if (areas.get(i).doesFovTouchArea(x,y,fovX,fovY))
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
        width=19999; //physical dim in um
        height=10000; //physical dim in um
        areas = new ArrayList<Area>();
        landmarks=new ArrayList<RefArea>();
        name="not selected";
        file = new File("","not selected");
        isEmpty=true;
    }
    
    
    public void createSBSPlateLayout(File f, int columns, int rows, double w, double h, double a1ColumnOffset, double a1RowOffset, double wellDiam, double wellSpacingX, double wellSpacingY, String wellShape){
        width=w; //physical dim in um
        height=h; //physical dim in um
        double oX=(double)a1ColumnOffset-wellDiam/2;
        double oY=(double)a1RowOffset-wellDiam/2;
        int areaNum=columns*rows;
        areas = new ArrayList<Area>(areaNum);
        int id=1;
        String wellName;
        for (int row=0; row<rows; row++) {
            for (int column=0; column<columns; column++) {
                if (row>=Area.PLATE_ALPHABET.length)
                    wellName=Integer.toString(id);
                else
                    wellName=Area.PLATE_ALPHABET[row]+Integer.toString(column+1);
                Area a=null;
                if (wellShape.equals("rectangle"))
                    a = new RectArea(wellName, id, oX+wellSpacingX*column, oY+wellSpacingY*row,0,wellDiam,wellDiam,false,"");
                else if (wellShape.equals("ellipse"))
                    a = new EllipseArea(wellName, id, oX+wellSpacingX*column, oY+wellSpacingY*row,0,wellDiam,wellDiam,false,"");
                areas.add(a);
                id++;
            }    
        }
                 
        addLandmark(new RefArea("Landmark 1",0,0,0,oX+wellDiam/2-512/2,oY+wellDiam/2-512/2,0,512,512,"landmark_1.tif")); //expects stage then layout coords
        
//        saveLayoutToXMLFile(f);    
    }
    
    /*
    private void loadLayoutFromXMLFile(File f) throws FileNotFoundException, XMLStreamException {
 //       IJ.log("AcquisitionLayout.loadLayoutFromXMLFile -start");
        boolean ok=true;
            XMLStreamReader xrt = null;
            try {
                XMLInputFactory xif = XMLInputFactory.newInstance();
                xrt = xif.createXMLStreamReader(new FileInputStream(f));
                int areaCount=0;
                areas=new ArrayList<Area>();
                while(xrt.hasNext()) {
                    int element = xrt.next();
                    if (element == XMLStreamReader.START_ELEMENT){
                        if (xrt.getLocalName().equals(TAG_LAYOUT))
                            ok=readLayoutParam(xrt);    
                        else if  (xrt.getLocalName().equals(TAG_LANDMARK))
                            addLandmark(readLandmarkParam(xrt));
                        else if  (xrt.getLocalName().equals(Area.TAG_AREA)) {
                            areaCount++;
                            Area a=readAreaParam(xrt);
                            a.setId(areaCount);
                            areas.add(a);
                        }    
                    }
                }
                xrt.close();
            } catch (XMLStreamException ex) { 
                if (xrt!=null)
                    xrt.close();
                throw new XMLStreamException();
            }    
    }


    public void saveLayoutToXMLFile(File f) {
        if (f==null)
            return;
        XMLUtils.initialize();
        XMLOutputFactory xof =  XMLOutputFactory.newInstance(); 
        try { 
            try {
                String fname=f.getAbsolutePath();
                XMLStreamWriter xtw; 
                xtw = xof.createXMLStreamWriter(new FileOutputStream(fname), "UTF-8"); 
                xtw.writeStartDocument("utf-8","1.0"); 
                xtw.writeCharacters(XMLUtils.LINE_FEED);
                
                XMLUtils.wStartElement(xtw, TAG_LAYOUT);
               

                    XMLUtils.writeLine(xtw, TAG_VERSION, "1.0");
                    XMLUtils.writeLine(xtw, TAG_NAME, name);
                    XMLUtils.writeLine(xtw, TAG_LAYOUT_WIDTH, java.lang.Double.toString(width));
                    XMLUtils.writeLine(xtw, TAG_LAYOUT_HEIGHT, java.lang.Double.toString(height));
                    
                    for (int i=0; i<landmarks.size(); i++) {
                        writeLandmark(xtw, landmarks.get(i));  
                    }   
                    for (int i=0; i<areas.size(); i++) {
                        writeArea(xtw, areas.get(i));
                    }
                
                XMLUtils.wEndElement(xtw);
                
                xtw.writeEndDocument(); 
                xtw.flush();
                xtw.close(); 
            } catch (XMLStreamException ex) { 

            } 
        } catch (FileNotFoundException ex) { 

        }     
    }
    */
    private void setLandmarkStageCoord(int index, double sX, double sY, double sZ) {
        if (landmarks!=null & landmarks.size()>index)
            landmarks.get(index).setStageCoord(sX, sY, sZ);
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
                Toolkit.getDefaultToolkit().beep();
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
                    //determine topLeftX, topLeftY, width and height of cluster
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
/*
    public Area readAreaSiteCoords(XMLStreamReader xrt) {
        String sh="undefined";
        String n="";
        double aW=-1;
        double aH=-1;
        double tLX=-1;
        double tLY=-1;
        double pZ=-1;
        boolean ignoreTiles=false;
        List<String> areaParams = new ArrayList<String>();
        List<Tile> tl = new ArrayList<Tile>();
        try {
            while (xrt.hasNext()) {
                int element=xrt.next();
                if (element==XMLStreamReader.END_ELEMENT && xrt.getLocalName().equals(Area.TAG_AREA))
                    break;
                else {
                    if (element == XMLStreamReader.START_ELEMENT && xrt.getLocalName().equals(Area.TAG_AREA)) { 
                        areaParams.add(xrt.getElementText());
                    }
                    if (element == XMLStreamReader.START_ELEMENT && xrt.getLocalName().equals(Area.TAG_CLUSTER)) {
                        List<String> clusterParams=new ArrayList<String>();
                        while (xrt.hasNext()) {
                            element=xrt.next();
                            if (element==XMLStreamReader.END_ELEMENT && xrt.getLocalName().equals(Area.TAG_CLUSTER))
                                break;
                            if (element==XMLStreamReader.START_ELEMENT && xrt.getLocalName().equals(Area.TAG_TILE))
                                break;
                            clusterParams.add(xrt.getElementText());
                         }
                    }
                    if (element == XMLStreamReader.START_ELEMENT && xrt.getLocalName().equals(Area.TAG_TILE)) {
                        
                    }
                }
            }
            Area area=null;
            if (areaParams.size()>0 && areaParams.get(0).contains("RectArea")) 
                area=new RectArea();
            else if (areaParams.size()>0 && areaParams.get(0).contains("EllipseArea")) 
                area=new EllipseArea();
            else if (areaParams.size()>0 && areaParams.get(0).contains("DonutArea")) 
                area=new DonutArea();           
            if (area!=null)
                area.setAreaParams(areaParams);
 //           IJ.log("areaParams.get(0): "+areaParams.get(0));
            /*try {
                IJ.log("areaParams.get(0): "+areaParams.get(0));
                Class areaClass = Class.forName("autoimage."+areaParams.get(0));
                area = areaClass.newInstance();
                ((Area)area).setAreaParams(areaParams);
            } catch (Throwable e) {
                System.err.println(e);
            }
            return area;
        } catch (XMLStreamException ex) {
            return null;
        }
    }
  */  
            
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
//        IJ.log("AcquisitionLayout.readTileCoordFromXMLFile -start");
        int returnValue=FILE_READING_OK;
        
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
//        IJ.log("AcquisitionLayout.loadLayoutFromXMLFile -end");
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

    public void removeAreaId(int id) {
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
    
    public boolean isModifed() {
        return isModified;
    }
        
}

