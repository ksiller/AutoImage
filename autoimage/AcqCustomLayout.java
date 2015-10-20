package autoimage;

import autoimage.api.TilingSetting;
import autoimage.api.RefArea;
import autoimage.gui.AcqFrame;
import autoimage.api.SampleArea;
import ij.IJ;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class AcqCustomLayout extends AcqBasicLayout {
        
    private String name;
    private boolean isEmpty;
    private boolean isModified;
    private double width;//in um
    private double height;//in um
    private double length;//in um
    private String bottomMaterial;
    private double bottomThickness;//in um
    private String version;
    private AffineTransform stageToLayoutTransform;
    private AffineTransform layoutToStageTransform;
    private Vec3d normalVec;//normal to layout reference plane
    private File file;
    
    private double escapeZPos; //z-stage is moved to this position when moving xystage to avoid collision with plate

    private List<SampleArea> areas;
    private List<RefArea> landmarks;
    private ThreadPoolExecutor tilingExecutor;
    private boolean tilingAborted=false;
     
    private static final String VERSION="1.0";
    private static final double ESCAPE_ZPOS_SAFETY=50; //keeps z-stage at least 50um below plate
    
    
    public AcqCustomLayout() {
        createEmptyLayout();
    }
/*    
    public static IAcqLayout loadLayout(File file) {//returns true if layout has been changed
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
            layoutObj=obj.getJSONObject(AcqCustomLayout.TAG_LAYOUT);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        IAcqLayout layout=AcqCustomLayout.createFromJSONObject(layoutObj, file);
        return layout;
    }
    
    public static IAcqLayout createFromJSONObject(JSONObject obj, File f) {
        AcqCustomLayout layout=null;
        if (obj!=null) {
            String className;
            try {
                //dynamic class loading
                className = obj.getString(TAG_CLASS_NAME);
                Class clazz=Class.forName(className);
                layout=(AcqCustomLayout) clazz.newInstance();
                layout.initializeFromJSONObject(obj, f);
                try {
                    layout.calcStageToLayoutTransform();
        //        tileManager=new TileManager(this);
                } catch (Exception ex) {
                    IJ.log("Error calculating stageToLayout transform");
                    Logger.getLogger(AcqCustomLayout.class.getName()).log(Level.SEVERE, null, ex);
                }        
            } catch (ClassNotFoundException ex) {
                layout=null;
                Logger.getLogger(AcqCustomLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                layout=null;
                Logger.getLogger(AcqCustomLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                layout=null;
                Logger.getLogger(AcqCustomLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JSONException ex) {
                layout=null;
                Logger.getLogger(AcqCustomLayout.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return layout;
    }
*/    
    @Override
    public void initializeFromJSONObject(JSONObject obj, File f) throws JSONException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        landmarks=new ArrayList<RefArea>();
        areas=new ArrayList<SampleArea>();
        version=obj.getString(TAG_VERSION);
        name=obj.getString(TAG_NAME);
        width=obj.getDouble(TAG_LAYOUT_WIDTH);
        length=obj.getDouble(TAG_LAYOUT_LENGTH);
        height=obj.getDouble(TAG_LAYOUT_HEIGHT);
        bottomMaterial=obj.getString(TAG_LAYOUT_BOTTOM_MATERIAL);
        bottomThickness=obj.getDouble(TAG_LAYOUT_BOTTOM_THICKNESS);
        JSONArray areaArray=obj.getJSONArray(SampleArea.TAG_AREA_ARRAY);
        for (int i=0;i<areaArray.length(); i++) {
            JSONObject areaObj=areaArray.getJSONObject(i);
            SampleArea area=SampleArea.createFromJSONObject(areaObj);
            areas.add(area);
        }
        JSONArray landmarkArray=obj.getJSONArray(RefArea.TAG_LANDMARK_ARRAY);
        for (int i=0;i<landmarkArray.length(); i++) {
            JSONObject landmarkObj=landmarkArray.getJSONObject(i);
            landmarks.add(new RefArea(landmarkObj));
        }
        isEmpty=false;
        isModified=false;
        file=f;
    }
    
    @Override
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
        for (SampleArea a:areas) {
            areaArray.put(a.toJSONObject());
        }
        obj.put(SampleArea.TAG_AREA_ARRAY,areaArray);
        JSONArray landmarkArray=new JSONArray();
        for (RefArea refa:landmarks) {
            landmarkArray.put(refa.toJSONObject());
        }
        obj.put(RefArea.TAG_LANDMARK_ARRAY,landmarkArray);
        return obj;
    }
    
    protected void initializeAreaList(int elements) {
        areas=new ArrayList(elements);
    }
    
    //returns true if gaps exist between tiles
    //considers camera field rotation and stage-to-layout rotation
    @Override
    public boolean hasGaps(FieldOfView fov, double tileOverlap) {
        Rectangle2D fovROI=fov.getROI_Pixel(1);
        Point2D tileOffset=SampleArea.calculateTileOffset(fovROI.getWidth(), fovROI.getHeight(), tileOverlap);
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
    
    @Override
    public void setWidth(double w) {
        width=w;
    }
    
    @Override
    public void setHeight(double h) {
        height=h;
    }
    
    @Override
    public void setLength(double l) {
        length=l;
    }
    
    //returns minimal relative tile overlap (0 <= tileOverlap <=1) to eliminate all gaps
    //considers camera field rotation and stage-to-layout rotation
    @Override
    public double closeTilingGaps(FieldOfView fov, double accuracy) {
        double newOverlap=0;
        double lowOverlap=0;
        double highOverlap=1;
        Rectangle2D fovROI=fov.getROI_Pixel(1);
        while (highOverlap-lowOverlap > accuracy) {
            newOverlap=(highOverlap-lowOverlap)/2+lowOverlap;

            Point2D tileOffset=SampleArea.calculateTileOffset(fovROI.getWidth(), fovROI.getHeight(), newOverlap);
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
    @Override
    public double getStageToLayoutRot() {
        if (stageToLayoutTransform!=null)
            return Math.atan2(stageToLayoutTransform.getShearY(), stageToLayoutTransform.getScaleY());
        else
            return 0;
    }
    
    //in radians
    @Override
    public double getLayoutToStageRot() {
        if (layoutToStageTransform!=null)
            return Math.atan2(layoutToStageTransform.getShearY(), layoutToStageTransform.getScaleY());
        else
            return 0;
    }
    
    @Override
    public File getFile() {
        return file;
    }
    
    @Override
    public void setFile(File f) {
        file=f;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void setName(String n) {
        name=n;
    }
    
    @Override
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
  
    @Override
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
    @Override
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
    @Override
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
    @Override
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
                //create 3rd vector orthogonal to v2-v1
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
                    Logger.getLogger(AcqCustomLayout.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(AcqCustomLayout.class.getName()).log(Level.SEVERE, null, ex);
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
            //single landmark --> create vector that is pefectly vertical
            normalVec=new Vec3d(0,0,1);
        }    
        //determine lowest corner to set escape z-pos when moving xy-stage
        try {
            Vec3d p1=convertLayoutToStagePos(0,0,0);
            Vec3d p2=convertLayoutToStagePos(width,0,0);
            Vec3d p3=convertLayoutToStagePos(width,length,0);
            Vec3d p4=convertLayoutToStagePos(0,length,0);
            escapeZPos=Math.min(Math.min(Math.min(p1.z,p2.z),p3.z),p4.z)-ESCAPE_ZPOS_SAFETY;
        } catch (Exception ex) {
            escapeZPos=0;
        }
    }
    
    @Override
    public Vec3d convertStageToLayoutPos(double stageX, double stageY, double stageZ) throws Exception {
        Point2D layoutXY=convertStageToLayoutPos_XY(new Point2D.Double(stageX,stageY));
        double layoutZ=stageZ-getStageZPosForStageXYPos(stageX,stageY);
        Vec3d lCoord=new Vec3d(layoutXY.getX(),layoutXY.getY(),layoutZ);
        return lCoord;
    }
    
    @Override
    public Vec3d convertLayoutToStagePos(double layoutX, double layoutY, double layoutZ) throws Exception {
        Point2D xy=convertLayoutToStagePos_XY(new Point2D.Double(layoutX,layoutY));
        //get z stage position in layout reference plane (layoutZ=0);
        double z=getStageZPosForStageXYPos(xy.getX(),xy.getY());
        //add layoutZ
        Vec3d sCoord=new Vec3d(xy.getX(),xy.getY(),z+layoutZ);
        return sCoord;
    }
      
    @Override
    public Point2D convertStageToLayoutPos_XY(Point2D stageXY) {
        Point2D layoutXY=new Point2D.Double();
        stageToLayoutTransform.transform(stageXY, layoutXY);
        return layoutXY;
    }
    
    @Override
    public Point2D convertLayoutToStagePos_XY(Point2D layoutXY) {
        Point2D stageXY=new Point2D.Double();
        layoutToStageTransform.transform(layoutXY, stageXY);
        return stageXY;
    }
    
    //returns stageZ pos that corresponds to layout reference plane (layoutZ = 0) at stageX/stageY pos
    @Override
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
    @Override
    public double getEscapeZPos() {
        return escapeZPos;
    }
    
    @Override
    public AffineTransform getStageToLayoutTransform() {
        return stageToLayoutTransform;
    }
    
    @Override
    public AffineTransform getLayoutToStageTransform() {
        return layoutToStageTransform;
    }
    
    @Override
    public Vec3d getNormalVector() {
        return normalVec;
    }
    
    @Override
    public boolean isEmpty() {
        return isEmpty;
    }
    
    @Override
    public double getWidth() {
        return width;
    }
    
    @Override
    public double getLength() {
        return length;
    }
    
    @Override
    public double getHeight() {
        return height;
    }    

    @Override
    public String getBottomMaterial() {
        return bottomMaterial;
    }
    
    @Override
    public void setBottomMaterial(String material) {
        bottomMaterial=material;
    }
    
    @Override
    public double getBottomThickness() {
        return bottomThickness;
    }
    
    @Override
    public void setBottomThickness(double thickness) {
        bottomThickness=thickness;
    }
    
    @Override
    public double getStagePosX(SampleArea a, int tileIndex) {
        if (a!=null && landmarks!=null && landmarks.size()>0) 
            return landmarks.get(0).getStageCoordX()-landmarks.get(0).getLayoutCoordX()+a.getCenterXYPos().getX();
        else
            return 0;
    }
    
    @Override
    public double getStagePosY(SampleArea a, int tileIndex) {
        if (a!=null && landmarks!=null && landmarks.size()>0) 
            return landmarks.get(0).getStageCoordY()-landmarks.get(0).getLayoutCoordY()+a.getCenterXYPos().getY();
        else
            return 0;
    }
    
    @Override
    public List<RefArea> getLandmarks(){
        return landmarks;
    }
    
    @Override
    public RefArea getLandmark(int index) {
        if (landmarks!=null && landmarks.size()>index)
            return landmarks.get(index);
        else 
            return null;
    }
    
    @Override
    public void setLandmarks(List<RefArea> lm) {
        landmarks=lm;
        try {
            calcStageToLayoutTransform();
        } catch (Exception ex) {
            Logger.getLogger(AcqCustomLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void deselectAllLandmarks() {
        if (landmarks!=null) {
            for (int i=0; i<landmarks.size(); i++) {
                landmarks.get(i).setSelected(false);
            }
        }
    }
    
    @Override
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
    
    @Override
    public void addLandmark(RefArea lm) {
        if (landmarks==null)
            landmarks=new ArrayList<RefArea>();
        landmarks.add(lm);
    }
    
    @Override
    public void deleteLandmark(int index) {
        landmarks.remove(index);
    }
    
    @Override
    public void deleteLandmark(RefArea lm) {
        landmarks.remove(lm);
    }
    
    @Override
    public List<SampleArea> getAreaArray() {
        return areas;
    }

    @Override
    public void setAreaArray(List<SampleArea> al) {
        areas=al;
    }
    
    @Override
    public SampleArea getAreaByIndex(int index) {
        if (index>=0 & areas!=null & index<areas.size())
            return areas.get(index);
        else
            return null;
    }
    
    @Override
    public SampleArea getAreaByName(String name) {
        if (name==null)
            return null;
        for (SampleArea a:areas) {
            if (a.getName().equals(name))
                return a;
        }
        return null;
    }
    
    @Override
    public SampleArea getAreaByLayoutPos(double lx, double ly) {
        for (SampleArea a:areas) {
            if (a.contains(lx,ly))
                return a;
        }
        return null;
    }
    
    //areas are loaded with sequential IDs, starting with 1 --> SampleArea with id is most likey to be in areas.get(id-1). If not, the entire AreaArray is searched for the ID
    @Override
    public SampleArea getAreaById(int id) {
        if ((id < 1 || id > areas.size()) || id!=areas.get(id-1).getId()) {
            int index=-1;
            for (SampleArea area : areas) {
                if (id == area.getId()) {
                    return area;
                } 
            }
            if (index!=-1)
                return areas.get(index);
            else
                return null;
        } else {
            return areas.get(id-1);
        }
    }
    
    @Override
    public void addArea(SampleArea a) {
        if (a.getName().equals("")) {
            a.setName("Area "+Integer.toString(areas.size()+1));
        }
        areas.add(a);
        isModified=true;
    }
    
    @Override
    public void deleteArea(int index) {
        areas.remove(index);
        isModified=true;
    }
        
    @Override
    public int getNoOfSelectedAreas() {
        int sel=0;
        for (SampleArea area : areas) {
            if (area.isSelectedForAcq()) {
                sel++;
            }
        }
        return sel;
    }
    
    @Override
    public int getNoOfSelectedClusters() {
        int sel=0;
        for (SampleArea area : areas) {
            if (area.isSelectedForAcq()) {
                sel=area.getNoOfClusters()+sel;
            }
        }
        return sel;
        
    }

    //returns null if layout coordinate is not inside any area
    @Override
    public SampleArea getFirstContainingArea(double lx, double ly) {
        int i=0;
        int index=-1;
        while ((index==-1) && (i<areas.size())) {
            if (areas.get(i).contains(lx,ly))
                index=i;
            i++;
        }
        if (index!=-1)
            return areas.get(index);
        else
            return null;
    }

    //returns null if fov surrounding layout coordinate does not touch any area
    @Override
    public SampleArea getFirstTouchingArea(double lx, double ly, double fovX, double fovY) {
/*        int i=0;
        int index=-1;
        while ((index==-1) && (i<areas.size())) {
            if (areas.get(i).doesFovTouchArea(lx,ly, fovX, fovY))
                index=i;
            i++;
        }
        if (index!=-1)
            return areas.get(index);
        else
            return null;*/
        for (SampleArea area:areas) {
            if (area.doesFovTouchArea(lx, ly, fovX, fovY)) {
                return area;
            }
        }
        //none of the areas touches the 
        return null;
    }
    
    //parameters are absolute stage positions
    @Override
    public SampleArea getFirstContainingAreaAbs(double stageX, double stageY, double fovX, double fovY) {
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
    
    @Override
    public int[] getAllContainingAreaIndices(double x, double y) {
        int[] indices = new int[areas.size()];
        int j=0;
        for (int i=0; i<areas.size(); i++) {
            if (areas.get(i).contains(x,y))
                indices[j]=i;    
        }
        return indices;
    }
    
    
    @Override
    public ArrayList<SampleArea> getAllAreasTouching(double x, double y) {
        ArrayList<SampleArea> list = new ArrayList<SampleArea>(areas.size());
        for (SampleArea area : areas) {
            if (area.contains(x, y)) {
                list.add(area);    
            }
        }
        return list; 
    }

    @Override
    public ArrayList<SampleArea> getUnselectedAreasTouching(double x, double y) {
        ArrayList<SampleArea> list = new ArrayList<SampleArea>(areas.size());
        for (SampleArea area:areas) {
            if (!area.isSelectedForAcq() && area.contains(x,y))
                list.add(area);    
        }
        return list; 
    }

    @Override
    public ArrayList<SampleArea> getSelectedAreasTouching(double x, double y) {
        ArrayList<SampleArea> list = new ArrayList<SampleArea>(areas.size());
        for (SampleArea area:areas) {
            if (area.isSelectedForAcq() && area.contains(x,y))
                list.add(area);    
        }
        return list; 
    }

    @Override
    public ArrayList<SampleArea> getAllAreasInsideRect(Rectangle2D.Double r) {
        ArrayList<SampleArea> a = new ArrayList<SampleArea>(areas.size());
        for (SampleArea area : areas) {
            if (area.isInsideRect(r)) {
                a.add(area);    
            }
        }
        return a; 
    }
    
    @Override
    public ArrayList<SampleArea> getUnselectedAreasInsideRect(Rectangle2D.Double r) {
        ArrayList<SampleArea> al = new ArrayList<SampleArea>(areas.size());
        for (SampleArea area:areas) {
            if (!area.isSelectedForAcq() && area.isInsideRect(r))
                al.add(area);    
        }
        return al; 
    }
        
    @Override
    public ArrayList<SampleArea> getSelectedAreasInsideRect(Rectangle2D.Double r) {
        ArrayList<SampleArea> al = new ArrayList<SampleArea>(areas.size());
        for (SampleArea area:areas) {
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
        areas = new ArrayList<SampleArea>();
        landmarks=new ArrayList<RefArea>();
        name="not selected";
        file = new File("","not selected");
        escapeZPos = 50; //in um; z-stage is moved to this position when moving xystage to avoid collision with plate
        isEmpty=true;
        isModified=false;;
        calcStageToLayoutTransform();    
    }
    

    private void writeSingleTile(XMLStreamWriter xtw, Tile t) throws XMLStreamException {
        XMLUtils.wStartElement(xtw, SampleArea.TAG_TILE);
            XMLUtils.writeLine(xtw, SampleArea.TAG_NAME, t.name);
            XMLUtils.writeLine(xtw, SampleArea.TAG_CENTER_X, java.lang.Double.toString(t.centerX));
            XMLUtils.writeLine(xtw, SampleArea.TAG_CENTER_Y, java.lang.Double.toString(t.centerY));
            XMLUtils.writeLine(xtw, SampleArea.TAG_REL_POS_Z, java.lang.Double.toString(t.zPos));
            XMLUtils.writeLine(xtw, Tile.TAG_IS_ABSOLUTE, java.lang.Boolean.toString(t.isAbsolute));
        XMLUtils.wEndElement(xtw);
    }

    
    @Override
    public void writeSingleArea(XMLStreamWriter xtw, SampleArea a, TilingSetting setting) throws XMLStreamException {
        XMLUtils.wStartElement(xtw, SampleArea.TAG_AREA);
            XMLUtils.writeLine(xtw, SampleArea.TAG_CLASS, a.getClass().getName());
            XMLUtils.writeLine(xtw, SampleArea.TAG_NAME, a.getName());
            XMLUtils.writeLine(xtw, SampleArea.TAG_TOP_LEFT_X, java.lang.Double.toString(a.getTopLeftX()));
            XMLUtils.writeLine(xtw, SampleArea.TAG_TOP_LEFT_Y, java.lang.Double.toString(a.getTopLeftY()));
            XMLUtils.writeLine(xtw, SampleArea.TAG_CENTER_X, java.lang.Double.toString(a.getCenterXYPos().getX()));
            XMLUtils.writeLine(xtw, SampleArea.TAG_CENTER_Y, java.lang.Double.toString(a.getCenterXYPos().getY()));
            XMLUtils.writeLine(xtw, SampleArea.TAG_BOUNDS_WIDTH, java.lang.Double.toString(a.getBounds().getWidth()));
            XMLUtils.writeLine(xtw, SampleArea.TAG_BOUNDS_HEIGHT, java.lang.Double.toString(a.getBounds().getHeight()));
            XMLUtils.writeLine(xtw, SampleArea.TAG_REL_POS_Z, java.lang.Double.toString(a.getRelativeZPos()));
            XMLUtils.writeLine(xtw, SampleArea.TAG_COMMENT, a.getComment());
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
                            relZPos+=tl.get(i*tilesPerCluster+j).zPos;
                            x=x+tl.get(i*tilesPerCluster+j).centerX;
                            y=y+tl.get(i*tilesPerCluster+j).centerY;
                        }    
                        relZPos=relZPos/tilesPerCluster;
                        x=x/tilesPerCluster;
                        y=y/tilesPerCluster;
//                        SampleArea.Tile tile=a.createTile("Site"+Integer.toString(i),x,y,zPos);
                        //write Cluster info
                        XMLUtils.wStartElement(xtw, SampleArea.TAG_CLUSTER);
                            XMLUtils.writeLine(xtw, SampleArea.TAG_NAME, "Cluster"+java.lang.Integer.toString(i+1));
                            XMLUtils.writeLine(xtw, SampleArea.TAG_CENTER_X, java.lang.Double.toString(x));
                            XMLUtils.writeLine(xtw, SampleArea.TAG_CENTER_Y, java.lang.Double.toString(y));
                            XMLUtils.writeLine(xtw, SampleArea.TAG_REL_POS_Z, java.lang.Double.toString(relZPos));
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
  
    @Override
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
                    
                        XMLUtils.wStartElement(xtw, Tile.TAG_TILECONFIG);
                            XMLUtils.writeLine(xtw, Tile.TAG_TILE_WIDTH, java.lang.Double.toString(tileW));
                            XMLUtils.writeLine(xtw, Tile.TAG_TILE_HEIGHT, java.lang.Double.toString(tileH));
                            XMLUtils.writeLine(xtw, Tile.TAG_TILE_OVERLAP, java.lang.Double.toString(setting.getTileOverlap()));
                            XMLUtils.writeLine(xtw, Tile.TAG_PIX_SIZE, java.lang.Double.toString(pixSize));
                            XMLUtils.writeLine(xtw, Tile.TAG_UNITS, "um");
                        XMLUtils.wEndElement(xtw);
                    
                    for (SampleArea a:areas) {
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
                if (xrt.getEventType()==XMLStreamReader.END_ELEMENT && local.equals(SampleArea.TAG_AREA)) {
                    break;
                } else if (event == XMLStreamReader.START_ELEMENT) {
                    if (local.equals(SampleArea.TAG_NAME)) {
                        areaName=xrt.getElementText()+"-";
                    } else if (local.equals(SampleArea.TAG_CLUSTER)) { //read cluster
                        tileList=new ArrayList<Tile>();
                        while (xrt.hasNext()) {
                            xrt.next();
                            event=xrt.getEventType();
                            if (event == XMLStreamReader.START_ELEMENT || event == XMLStreamReader.END_ELEMENT) {
                                local=xrt.getLocalName();
                            }
                            if (event==XMLStreamReader.END_ELEMENT && local.equals(SampleArea.TAG_CLUSTER)) {
                                clusterName="";
                                groupList.add(tileList);
                                tileList=null; //st to null so that tileList does not get added again at end of parsing
                                break;
                            } else if (event == XMLStreamReader.START_ELEMENT) {
                                if (local.equals(SampleArea.TAG_NAME)) {
                                    clusterName=xrt.getElementText()+"-";
                                } else if (local.equals(SampleArea.TAG_TILE)) { //read tile
                                    while (xrt.hasNext()) {
                                        xrt.next();
                                        event=xrt.getEventType();
                                        if (event == XMLStreamReader.START_ELEMENT || event == XMLStreamReader.END_ELEMENT) {
                                            local=xrt.getLocalName();
                                        }
                                        if (event==XMLStreamReader.END_ELEMENT && local.equals(SampleArea.TAG_TILE))
                                                break;
                                        else if (event == XMLStreamReader.START_ELEMENT) {
                                            if (local.equals(SampleArea.TAG_NAME)) {
                                                tileName=xrt.getElementText();
                                            } else if (local.equals(SampleArea.TAG_CENTER_X)) {
                                                centerX=Double.parseDouble(xrt.getElementText());
                                            } else if (local.equals(SampleArea.TAG_CENTER_Y)) {
                                                centerY=Double.parseDouble(xrt.getElementText());
                                            } else if (local.equals(SampleArea.TAG_REL_POS_Z)) {
                                                relZ=Double.parseDouble(xrt.getElementText());
                                            }    
                                        
                                        }
                                    }    
                                    tileList.add(Tile.createTile(areaName+tileName,centerX,centerY,relZ, true));
                                }
                            }    
                        }   
                    } else if (event == XMLStreamReader.START_ELEMENT && local.equals(SampleArea.TAG_TILE)) { //read tile
                        while (xrt.hasNext()) {
                            xrt.next();
                            event=xrt.getEventType();
                            if (event == XMLStreamReader.START_ELEMENT || event == XMLStreamReader.END_ELEMENT) {
                                local=xrt.getLocalName();
                            }
                            if (event==XMLStreamReader.END_ELEMENT && local.equals(SampleArea.TAG_TILE)) {
                                break;
                            } else if (event == XMLStreamReader.START_ELEMENT) {
                                if (local.equals(SampleArea.TAG_NAME)) {
                                        tileName=xrt.getElementText();
                                } else if (local.equals(SampleArea.TAG_CENTER_X)) {
                                        centerX=Double.parseDouble(xrt.getElementText());
                                } else if (local.equals(SampleArea.TAG_CENTER_Y)) {
                                        centerY=Double.parseDouble(xrt.getElementText());
                                } else if (local.equals(SampleArea.TAG_REL_POS_Z)) {
                                        relZ=Double.parseDouble(xrt.getElementText());
                                }  
                            }  
                        }
                        tileList.add(Tile.createTile(areaName+tileName,centerX,centerY,relZ,true));
                    }
                }
            }
            if (tileList!=null) //if not cluster, tileList still needs to be added to groupList
                groupList.add(tileList);
        } catch (XMLStreamException ex) {
        }        
    } 
       
    @Override
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
                                
                        } else if (s.equals(SampleArea.TAG_AREA)) {
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
        
    /**
     * @return index of first area with duplicate name. Returns -1 if all names are unique or list is empty 
     */ 
    @Override
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

    @Override
    public void removeAreaById(int id) {
        if (areas!=null) {
            for (int i=0; i<areas.size(); i++) {
                if (areas.get(i).getId()==id) {
                    areas.remove(i);
                    //id is unique, so loop cen be exited after finding first match
                    break;
                }    
            }
        }
        isModified=true;
    }

    @Override
    public void setModified(boolean b) {
        isModified=b;
    }
    
    @Override
    public boolean isModified() {
        return isModified;
    }
    
    @Override
    public boolean isAreaEditingAllowed() {
        return true;
    }
    
    @Override
    public boolean isAreaMergingAllowed() {
        return true;
    }
    
    @Override
    public boolean isAreaAdditionAllowed() {
        return true;
    }
    
    @Override
    public boolean isAreaRemovalAllowed() {
        return true;
    }
    
    @Override
    public Comparator getAreaNameComparator() {
        return SampleArea.NameComparator;
    }

    @Override
    public Comparator getTileNumberComparator() {
        return SampleArea.TileNumComparator;
    }
    
    @Override
    public long getTotalTileNumber() {
        long totalTiles=0;
        boolean error=false;
        for (SampleArea a:areas) {
            if (a.isSelectedForAcq())
                totalTiles=totalTiles+a.getTileNumber();
            error=error || (a.getTilingStatus()==SampleArea.TILING_ERROR);
        }    
        return (!error ? totalTiles : -totalTiles);
    }
    
    public List<Future<Integer>> calculateTiles(List<SampleArea> areasToTile, final DoxelManager dManager, final double fovWidth_UM, final double fovHeight_UM, final TilingSetting tSetting) {
        IJ.log("AcqLayout.calculateTiles...started");
        tilingAborted=false;
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        tilingExecutor = new ThreadPoolExecutor(2, 4, 10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(areas.size()), threadFactory);
        List<Future<Integer>> resultList = new ArrayList<Future<Integer>>();
//        Thread retilingMonitorThread = new Thread(new AcqFrame.TileCalculationMonitor(retilingExecutor, resultList, processProgressBar, cmd));//, areas));
//        retilingMonitorThread.start();

        if (areasToTile==null) {
            areasToTile=areas;
        }
        for (SampleArea area : areasToTile) {
            if (area.isSelectedForAcq()) {
                final SampleArea a = area;
                resultList.add(tilingExecutor.submit(new Callable<Integer>() { //returns number of tiles as Future<Integer>
                    @Override
                    public Integer call() {
                        try {
                            return a.calcTilePositions(dManager,
                                    fovWidth_UM, 
                                    fovHeight_UM, tSetting);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                            return -1;
                        }
                    }
                }));
            }
        }
        //block addition of new jobs
        tilingExecutor.shutdown();
        return resultList;
    }
    
    @Override
    public int getTilingStatus() {
        if (tilingExecutor==null) {
            return NO_TILING_EXECUTOR;
        } else if (tilingExecutor.isTerminated() && !tilingAborted) {
            return TILING_COMPLETED;
        } else if (tilingExecutor.isTerminated() && tilingAborted) {
            return TILING_ABORTED;
        } else if (tilingExecutor.isTerminating()) {
            return TILING_IS_TERMINATING;
        } else {
            return TILING_IN_PROGRESS;
        }
    }
    
    @Override
    public long getCompletedTilingTasks() {
        if (tilingExecutor!=null) {
            return tilingExecutor.getCompletedTaskCount();
        } else
            return NO_TILING_EXECUTOR;
    }
    
    @Override
    public double getCompletedTilingTasksPercent() {
        if (tilingExecutor!=null) {
            return tilingExecutor.getCompletedTaskCount()/tilingExecutor.getTaskCount()*100;
        } else {
            return NO_TILING_EXECUTOR;
        }
    }
    
    @Override
    public void cancelTileCalculation() {
        if (tilingExecutor != null && !tilingExecutor.isTerminated()) {
            tilingAborted=true;
//            IJ.log("AcqFrame.cancelThreadButtonActionPerformed");
            for (SampleArea a : areas) {
                a.setUnknownTileNum(true);
            }
            tilingExecutor.shutdownNow();
        }  
    }
}

