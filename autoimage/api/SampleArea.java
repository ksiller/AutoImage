package autoimage.api;

import autoimage.FieldOfView;
import autoimage.Tile;
import autoimage.DoxelManager;
import autoimage.Vec3d;
import ij.IJ;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.api.MultiStagePosition;
import org.micromanager.api.PositionList;

/**
 *
 * @author Karsten Siller
 */
public abstract class SampleArea implements Shape {
    public static final String TAG_AREA = "AREA";
    public static final String TAG_AREA_ARRAY = "AREA_ARRAY";
    public static final String TAG_CLASS = "CLASS";
    public static final String TAG_NAME = "NAME";
    public static final String TAG_ID="ID";
    public static final String TAG_COMMENT = "COMMENT";
    public static final String TAG_TOP_LEFT_X = "TOP_LEFT_X";
    public static final String TAG_TOP_LEFT_Y = "TOP_LEFT_Y";
    public static final String TAG_CENTER_X = "CENTER_X";
    public static final String TAG_CENTER_Y = "CENTER_Y";
    public static final String TAG_WIDTH = "WIDTH";
    public static final String TAG_HEIGHT = "HEIGHT";
    public static final String TAG_RING_WIDTH = "RING_WIDTH";
    public static final String TAG_AFFINETRANSFORM = "AFFINE_TRANSFORM";//used to realize SampleArea rotation in layout
    public static final String TAG_REL_POS_Z = "REL_POS_Z";
    public static final String TAG_SELECTED = "SELECTED";

    public static final String TAG_TILE = "TILE";
    public static final String TAG_CLUSTER = "CLUSTER";
    
    public static final int SUPPORT_WELLPLATE_LAYOUT = 1;
    public static final int SUPPORT_CUSTOM_LAYOUT = 2;
    public static final int SUPPORT_ALL_LAYOUTS = 128;
    
    public static final int TILING_OK = 1;
    public static final int TILING_ERROR = -1;
    public static final int TILING_UNKNOWN_NUMBER = -2;
        
    protected String name;
    protected static double cameraRot = FieldOfView.ROTATION_UNKNOWN;//in radians relative to x-y stagePos axis, NOT layout
    protected static double stageToLayoutRot = 0; //in radians
    protected static boolean optimizedForCameraRotation = true;
    protected List<Tile> tilePosList;//has absolute layout positions in um
    protected int tileNumber;
    protected int tilingStatus;
    protected int id; //unique identifier, reflects order of addition 
    protected int index; //index in arraylist of selected areas; required for metadata annotation 
    protected double topLeftX; //in um
    protected double topLeftY; //in um
    protected Point2D centerXYPos;
    protected Point2D defaultXYPos;
    protected double relativeZPos; //in um relative to flat layout bottom
    protected double width; //in um
    protected double height; //in um
    protected AffineTransform affineTrans;
    protected Shape shape;
    protected GeneralPath generalPath;
    protected boolean selectedForAcq;
    protected boolean selectedForMerge;
    protected String comment;
    protected boolean acquiring;
    private boolean unknownTileNum; //is set when tilingmode is "runtime" or "file", or pixel tiles is not calibrated
    private int noOfClusters;
    
    public static final String[] PLATE_ROW_ALPHABET={"A","B","C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "AA", "AB", "AC", "AD", "AE", "AF"};
    public static final String[] PLATE_COL_ALPHABET=ColAlphabetFactory();
    public static final String TILE_NAME_PADDING = "%06d"; //padds tile name with preceding '0' to fill up to 6 digits
    public static final String CLUSTER_NAME_PADDING = "%06d";
    
    protected static final long DELAY_TO_CANCEL = 1000;
    protected static final int MAX_TILING_ATTEMPTS = 200;

           
    public SampleArea() {
        this("NewArea", -1, 0,0,0,0,0,false,"");
    }
    
    public SampleArea(String n) { //expects name identifier
        this(n, -1, 0,0,0,0,0,false,"");
    }
    
    //expects layout coordinates in phys dim
    public SampleArea(String n, int id, double ox, double oy, double oz, double w, double h, boolean s, String anot) {
        name=n;
        this.id=id;
        index=-1;
        topLeftX=ox;
        topLeftY=oy;
        relativeZPos=oz;
        width=w;
        height=h;
        selectedForAcq=s;
        comment=anot;
        acquiring=false;
        tilePosList=null;
        tilingStatus=TILING_OK;
        unknownTileNum=true;
        noOfClusters=0;
        affineTrans=new AffineTransform();
        calcCenterAndDefaultPos();
        createShape();
        createGeneralPath();
    }

    private static String[] ColAlphabetFactory() {
        String[] alphabet=new String[48];
        for (int i=0; i<48; i++) {
            alphabet[i]=Integer.toString(i+1);
        }
        return alphabet;
    }       
      
    public void enableOptimizedForCameraRotation(boolean b) {
        optimizedForCameraRotation=true;
    }
    
    public boolean isOptimizedForCameraRotation() {
        return optimizedForCameraRotation;
    }

    public int getNoOfClusters() {
        return noOfClusters;
    }
    
    public void setIndex(int index) {
        this.index=index;
    }
    
    public int getIndex() {
        return index;
    }
    
    //in radians
    public static void setCameraRot(double rot) {
        cameraRot=rot;
    }
    
    //in radians
    public static double getCameraRot() {
        return cameraRot;
    } 
    
    public static void setStageToLayoutRot(double rot) {
        stageToLayoutRot=rot;
    }
    
    public static double getStageToLayoutRot() {
        return stageToLayoutRot;
    } 
    
    public Tile createTile(String n, double x, double y, double z, boolean absolute) {
        return Tile.createTile(n,x,y,z,absolute);
    }
    
    public void setAcquiring(boolean b) {
        acquiring=b;
    }
    
    public void setUnknownTileNum(boolean b) {
        unknownTileNum=b;
    }
    
    public boolean hasUnknownTileNum() {
        return unknownTileNum;
    }
    
    public boolean isAcquiring() {
        return acquiring;
    }

    public int getTilingStatus() {
        return tilingStatus;
    }
    
    private List<Tile> copyCluster(int clusterNr, double offsetX, double offsetY, List<Tile> source) {
        List<Tile> newCluster=null;
        if (source!=null) {
            newCluster=new ArrayList<Tile>(source.size());
            int i=1;
            for (Tile t:source) {
                newCluster.add(new Tile(createTileName(clusterNr,i),t.centerX+offsetX,t.centerY+offsetY,t.zPos, t.isAbsolute));
                i++;
            }
        }
        return newCluster;
    }
    
    private static String createTileName(int clusterNr, int siteNr) {
        final String s="Cluster"+Integer.toString(clusterNr)+"-Site"+paddedTileIndex(siteNr);
        return s;
    }
    
    private boolean overlapsOtherTiles(Rectangle2D.Double r, double fovX, double fovY) {
        Rectangle2D fovBounds=getFovBounds(fovX,fovY);
        for (Tile t:tilePosList) {
            Rectangle2D.Double tRect=new Rectangle2D.Double(t.centerX-fovBounds.getWidth()/2, t.centerY-fovBounds.getHeight()/2,fovBounds.getWidth(),fovBounds.getHeight());
            if (tRect.intersects(r))
                return true;
        }
        return false;
    }
    
    public static Point2D calculateTileOffset(double fovX, double fovY, double overlap) {
        if (optimizedForCameraRotation && cameraRot!=FieldOfView.ROTATION_UNKNOWN) {
            double tileOffsetX;
            double tileOffsetY;
            double xFlipAngle=Math.atan2(fovY,fovX);
            double yFlipAngle=Math.atan2(fovX,fovY);
            if (Math.abs(cameraRot-stageToLayoutRot) < xFlipAngle || Math.abs(cameraRot-stageToLayoutRot) > (Math.PI -xFlipAngle)) {
               tileOffsetX=(1-overlap) * fovX / Math.abs(Math.cos(cameraRot-stageToLayoutRot));
            } else {
               tileOffsetX=(1-overlap) * fovY / Math.abs(Math.sin(cameraRot-stageToLayoutRot));
            }    
            if (Math.abs(cameraRot-stageToLayoutRot) < yFlipAngle || Math.abs(cameraRot-stageToLayoutRot) > (Math.PI -yFlipAngle)) {
               tileOffsetY=(1-overlap) * fovY / Math.abs(Math.cos(cameraRot-stageToLayoutRot));           
            } else {
               tileOffsetY=(1-overlap) * fovX / Math.abs(Math.sin(cameraRot-stageToLayoutRot));
            }    
            return new Point2D.Double(tileOffsetX, tileOffsetY);
        } else {
            return new Point2D.Double((1-overlap) * fovX,(1-overlap) * fovY);
        }
    }
    
    private int createCluster(int clusterNr,double seedX, double seedY, double fovX, double fovY, TilingSetting setting ) {
        double tileOverlap=setting.getTileOverlap();
        int tilingDir=setting.getTilingDirection();

        Point2D tileOffset=calculateTileOffset(fovX, fovY, setting.getTileOverlap());
        Rectangle2D fovBounds=getFovBounds(fovX, fovY);

        int xTiles=setting.getNrClusterTileX();
        int yTiles=setting.getNrClusterTileY();
        double clusterW=fovBounds.getWidth()+(xTiles-1)*tileOffset.getX();
        double clusterH=fovBounds.getHeight()+(yTiles-1)*tileOffset.getY();
        double startx=seedX-(clusterW/2)+fovBounds.getWidth()/2;//*physToPixelRatio;
        double starty=seedY-(clusterH/2)+fovBounds.getHeight()/2;//*physToPixelRatio;

        if (!setting.isSiteOverlap() && overlapsOtherTiles(new Rectangle2D.Double(startx-fovBounds.getWidth()/2, starty-fovBounds.getHeight()/2, clusterW, clusterH),fovX,fovY)) {
            return TILING_ERROR;
        }
        
        double x;
        double y;
        int hDir;
        int vDir;
        boolean vFirst;
        int col;
        int row;
//        byte tilingDirection=(byte) (tilingDir & 7);
        switch (tilingDir) {
            case TilingSetting.DR_TILING:
                vFirst=true;
                hDir=1;
                vDir=1;
                col=0;
                row=0;
                break;
            case TilingSetting.DL_TILING:
                vFirst=true;
                hDir=-1;
                vDir=1;
                col=xTiles-1;
                row=0;
                break;
            case TilingSetting.UR_TILING:
                vFirst=true;
                hDir=1;
                vDir=-1;
                col=0;
                row=yTiles-1;
                break;
            case TilingSetting.UL_TILING:
                vFirst=true;
                hDir=-1;
                vDir=-1;
                col=xTiles-1;
                row=yTiles-1;
                break;
            case TilingSetting.RD_TILING:
                vFirst=false;
                hDir=1;
                vDir=1;
                col=0;
                row=0;
                break;
            case TilingSetting.LD_TILING: 
                vFirst=false;
                hDir=-1;
                vDir=1;
                col=xTiles-1;
                row=0;
                break;
            case TilingSetting.RU_TILING: 
                vFirst=false;
                hDir=1;
                vDir=-1;
                col=0;
                row=yTiles-1;
                break;
            case TilingSetting.LU_TILING: 
                vFirst=false;
                hDir=-1;
                vDir=-1;
                col=xTiles-1;
                row=yTiles-1;
                break;
            default:
                vFirst=true;
                hDir=1;
                vDir=1;
                col=0;
                row=0;
                break;
        }
        int siteCounter=0;
        if (vFirst) {
            for (int i=0; i<xTiles; i++) {  
                if (Thread.currentThread().isInterrupted()) {
                    tilePosList.clear();
//                    IJ.log("SampleArea.createCluster: interrupted, SampleArea "+name);
                    return TILING_UNKNOWN_NUMBER;
                }    
                x = startx+tileOffset.getX()*col;
                row=vDir == 1 ? 0 : yTiles-1;
                for (int j=0; j<yTiles; j++) {   
                    y = starty+tileOffset.getY()*row;
    //                IJ.log(name+", test Tile: "+Integer.toString(i*yTiles+j)+", "+Double.toString(x)+","+Double.toString(y));
                    Tile t=Tile.createTile(createTileName(clusterNr,siteCounter), x, y, relativeZPos,false);                
                    tilePosList.add(t);
                    row+=vDir;
                    siteCounter++;
                }
                col+=hDir;
                vDir=-vDir;
            }
        } else {
            for (int j=0; j<yTiles; j++) {   
                if (Thread.currentThread().isInterrupted()) {
                    tilePosList.clear();
//                    IJ.log("SampleArea.createCluster: interrupted, SampleArea "+name);
                    return TILING_UNKNOWN_NUMBER;
                }    
                y = starty+tileOffset.getY()*row;
                col=hDir == 1 ? 0 : xTiles-1;
                for (int i=0; i<xTiles; i++) {  
                    x = startx+tileOffset.getX()*col;
  //                  IJ.log(name+", test Tile: "+Integer.toString(i*yTiles+j)+", "+Double.toString(x)+","+Double.toString(y));
                    Tile t=Tile.createTile(createTileName(clusterNr,siteCounter), x, y, relativeZPos,false);                
                    tilePosList.add(t);
   //                 IJ.log(name+", "+Integer.toString(i*yTiles+j)+", "+Double.toString(x)+","+Double.toString(y));
                    col+=hDir;
                    siteCounter++;
                }
                row+=vDir;
                hDir=-hDir;
            }            
        }
  //      IJ.log("SampleArea.creatCluster: end"); 
        return TILING_OK;
    }
    
    public static Rectangle2D getFovBounds(double fovX, double fovY) {
        java.awt.geom.Area a= new java.awt.geom.Area(new Rectangle2D.Double(0,0,fovX,fovY));
        AffineTransform rot=new AffineTransform();
        rot.rotate(cameraRot-stageToLayoutRot);
        a.transform(rot);
        return a.getBounds2D();
    }
    
    public int calcFullTilePositions(double fovX, double fovY, TilingSetting setting) throws InterruptedException {
        double tileOverlap=setting.getTileOverlap();
        boolean insideOnly=setting.isInsideOnly();
        int tilingDir=setting.getTilingDirection();
        
        Point2D tileOffset=calculateTileOffset(fovX, fovY, setting.getTileOverlap());
        Rectangle2D fovBounds=getFovBounds(fovX,fovY);
        int xTiles;
        int yTiles;
        double startx;
        double starty;
        
        if (insideOnly) {
            xTiles=(int)Math.floor((width-fovBounds.getWidth())/tileOffset.getX())+1;
            yTiles=(int)Math.floor((height-fovBounds.getHeight())/tileOffset.getY())+1;
        } else {
//            xTiles=(int)Math.ceil(width/tileOffsetX);
//            yTiles=(int)Math.ceil(height/tileOffsetY);
            xTiles=(int)Math.ceil((width-fovBounds.getWidth())/tileOffset.getX())+1;
            yTiles=(int)Math.ceil((height-fovBounds.getHeight())/tileOffset.getY())+1;
        }
        startx=fovBounds.getWidth()/2+(topLeftX-((fovBounds.getWidth()+(xTiles-1)*tileOffset.getX()-width)/2));//*physToPixelRatio;
        starty=fovBounds.getHeight()/2+(topLeftY-((fovBounds.getHeight()+(yTiles-1)*tileOffset.getY()-height)/2));//*physToPixelRatio;
        if (xTiles==0) xTiles=1;
        if (yTiles==0) yTiles=1;
        double x;
        double y;
//       IJ.log("SampleArea.calculateTilePositions: xTiles: "+Integer.toString(xTiles));
//        IJ.log("SampleArea.calculateTilePositions: yTiles: "+Integer.toString(yTiles));
//        tilePosList.clear();
        tilePosList=new ArrayList<Tile>(xTiles*yTiles);
        int hDir;
        int vDir;
        boolean vFirst;
        int col;
        int row;
//        byte tilingDirection=(byte) (tilingDir & 7);
        switch (tilingDir) {
            case TilingSetting.DR_TILING:
                vFirst=true;
                hDir=1;
                vDir=1;
                col=0;
                row=0;
                break;
            case TilingSetting.DL_TILING:
                vFirst=true;
                hDir=-1;
                vDir=1;
                col=xTiles-1;
                row=0;
                break;
            case TilingSetting.UR_TILING:
                vFirst=true;
                hDir=1;
                vDir=-1;
                col=0;
                row=yTiles-1;
                break;
            case TilingSetting.UL_TILING:
                vFirst=true;
                hDir=-1;
                vDir=-1;
                col=xTiles-1;
                row=yTiles-1;
                break;
            case TilingSetting.RD_TILING:
                vFirst=false;
                hDir=1;
                vDir=1;
                col=0;
                row=0;
                break;
            case TilingSetting.LD_TILING: 
                vFirst=false;
                hDir=-1;
                vDir=1;
                col=xTiles-1;
                row=0;
                break;
            case TilingSetting.RU_TILING: 
                vFirst=false;
                hDir=1;
                vDir=-1;
                col=0;
                row=yTiles-1;
                break;
            case TilingSetting.LU_TILING: 
                vFirst=false;
                hDir=-1;
                vDir=-1;
                col=xTiles-1;
                row=yTiles-1;
                break;
            default:
                vFirst=true;
                hDir=1;
                vDir=1;
                col=0;
                row=0;
                break;
        }
        int siteCounter=0;
        if (vFirst) {
            for (int i=0; i<xTiles; i++) {  
                if (Thread.currentThread().isInterrupted()) {
                    tilePosList.clear();
//                    IJ.log("SampleArea.calcFullTilePositions: interrupted, SampleArea "+name);
                    break;
                }    
                x = startx+tileOffset.getX()*col;
                row=vDir == 1 ? 0 : yTiles-1;
                for (int j=0; j<yTiles; j++) {   
                    y = starty+tileOffset.getY()*row;
//                    IJ.log(name+", test Tile: "+Integer.toString(i*yTiles+j)+", "+Double.toString(x)+","+Double.toString(y));
                    if (acceptTilePos(x,y,fovBounds.getWidth(),fovBounds.getHeight(),insideOnly)) {
                        siteCounter++;
//                        Tile t=new Tile("Site"+paddedTileIndex(siteCounter), x, y, relativeZPos);  
                        Tile t=Tile.createTile(createTileName(0,siteCounter), x, y, relativeZPos, false);
                        tilePosList.add(t);
 //                       IJ.log(name+", accepted Tile: "+Integer.toString(col*yTiles+row)+", "+Double.toString(x)+","+Double.toString(y));
                    }
                    row+=vDir;
                }
                col+=hDir;
                vDir=-vDir;
            }
        } else {
            for (int j=0; j<yTiles; j++) {   
                if (Thread.currentThread().isInterrupted()) {
                    tilePosList.clear();
//                    IJ.log("SampleArea.calcFullTilePositions: interrupted, SampleArea "+name);
                    break;
                }    
                y = starty+tileOffset.getY()*row;
                col=hDir == 1 ? 0 : xTiles-1;
                for (int i=0; i<xTiles; i++) {  
                    x = startx+tileOffset.getX()*col;
 //                   IJ.log(name+", test Tile: "+Integer.toString(i*yTiles+j)+", "+Double.toString(x)+","+Double.toString(y));
                    if (acceptTilePos(x,y,fovBounds.getWidth(),fovBounds.getHeight(),insideOnly)) {
                        siteCounter++;
//                        Tile t=new Tile("Site"+paddedTileIndex(siteCounter), x, y, relativeZPos);                
                        Tile t=Tile.createTile(createTileName(0,siteCounter), x, y, relativeZPos,false);
                        tilePosList.add(t);
 //                       IJ.log(name+", acceptedTile: "+Integer.toString(col*yTiles+row)+", "+Double.toString(x)+","+Double.toString(y));
                    }
//                    IJ.log(name+", "+Integer.toString(i*yTiles+j)+", "+Double.toString(x)+","+Double.toString(y));
                    col+=hDir;
                }
                row+=vDir;
                hDir=-hDir;
            }            
        }
        noOfClusters = (tilePosList.size() > 0 ?  1 :  0);
        unknownTileNum=Thread.currentThread().isInterrupted();
        tilingStatus=TILING_OK;
        if (unknownTileNum) {
            tilingStatus=TILING_UNKNOWN_NUMBER;//aborted
        } else if (tilePosList.size() == 0) {
            tilingStatus=TILING_ERROR;//unsuccessful
        }   
        return (tilingStatus==TILING_OK ? tilePosList.size() : tilingStatus);
    }

    private int calcRandomTilePositions(double fovX, double fovY, TilingSetting tSetting) throws InterruptedException {
        Rectangle2D fovBounds=getFovBounds(fovX, fovY);
        Point2D tileOffset=calculateTileOffset(fovX, fovY, tSetting.getTileOverlap());

        int size=tSetting.getMaxSites();
        if (tSetting.isCluster())
            size=size*tSetting.getNrClusterTileX()*tSetting.getNrClusterTileY();
        int iterations=0;
        double w;
        double h;
        if (tSetting.isCluster() && (tSetting.getNrClusterTileX()>1 || tSetting.getNrClusterTileY()>1)) {
            w=fovBounds.getWidth()+tileOffset.getX()*(tSetting.getNrClusterTileX()-1);
            h=fovBounds.getHeight()+tileOffset.getY()*(tSetting.getNrClusterTileY()-1);
        } else {
//                w=tileOffset.getX();
//                h=tileOffset.getY();
            w=fovBounds.getWidth();
            h=fovBounds.getHeight();
        }
        do {
            tilePosList = new ArrayList<Tile>(size);
            noOfClusters=0;
            int attempts=0;
            while (!Thread.currentThread().isInterrupted() && noOfClusters < tSetting.getMaxSites() && attempts < MAX_TILING_ATTEMPTS) {// && !abortTileCalc) {
                double x=topLeftX+Math.random()*width;
                double y=topLeftY+Math.random()*height;
                if (acceptTilePos(x,y,w,h,tSetting.isInsideOnly())) {
                    if (tSetting.isCluster() && (tSetting.getNrClusterTileX()>1 || tSetting.getNrClusterTileY()>1)) {
                        if (createCluster(noOfClusters,x,y,fovX,fovY, tSetting) == TILING_OK) {
                            noOfClusters++;
                        }
                    } else {
                        if (tSetting.isSiteOverlap() || (!tSetting.isSiteOverlap() && !overlapsOtherTiles(new Rectangle2D.Double(x-fovBounds.getWidth()/2,y-fovBounds.getHeight()/2,fovX,fovY),fovX,fovY))) {
                            tilePosList.add(Tile.createTile(createTileName(0,noOfClusters), x, y, relativeZPos, false));
                            noOfClusters++;
                        }
                    }
                }
                attempts++;
            }
            iterations++;
        } while (!Thread.currentThread().isInterrupted() && noOfClusters < tSetting.getMaxSites() && iterations < (MAX_TILING_ATTEMPTS/10)); // && !abortTileCalc) {
    
        tilingStatus=TILING_OK;
        unknownTileNum=(Thread.currentThread().isInterrupted());
        if (unknownTileNum) {
            tilingStatus=TILING_UNKNOWN_NUMBER;//aborted
        } else if (noOfClusters < tSetting.getMaxSites() && iterations >= MAX_TILING_ATTEMPTS/10.0d) {
                tilingStatus=TILING_ERROR;//unsuccessful
        } 
        return (tilingStatus==TILING_OK ? tilePosList.size() : tilingStatus);
    }
    
    private int calcCenterTilePositions(double fovX, double fovY, TilingSetting tSetting) throws InterruptedException {
        int tiles=1;
        if (tSetting.isCluster())
            tiles=tiles*tSetting.getNrClusterTileX()*tSetting.getNrClusterTileY();
        tilePosList = new ArrayList<Tile>(tiles);
        
        if (centerXYPos==null || defaultXYPos==null) {
            calcCenterAndDefaultPos();
        }
        tilingStatus=addTilesAroundSeed(0,centerXYPos.getX(), centerXYPos.getY(), fovX, fovY, tSetting);
        unknownTileNum=tilingStatus==TILING_UNKNOWN_NUMBER;
        noOfClusters=tilingStatus == TILING_OK ? 1 : 0;
        return (tilingStatus==TILING_OK ? tilePosList.size() : tilingStatus);
    }
    
    
    private synchronized int addTilesAroundSeed(int clusterNr,double seedX, double seedY, double fovX, double fovY, TilingSetting tSetting) {
        Point2D tileOffset=SampleArea.calculateTileOffset(fovX, fovY, tSetting.getTileOverlap());
        Rectangle2D fovBounds=getFovBounds(fovX, fovY);
        double w;
        double h;
        if (tSetting.isCluster() && (tSetting.getNrClusterTileX()>1 || tSetting.getNrClusterTileY()>1)) {
            w=fovBounds.getWidth()+tileOffset.getX()*(tSetting.getNrClusterTileX()-1);
            h=fovBounds.getHeight()+tileOffset.getY()*(tSetting.getNrClusterTileY()-1);
        } else {
            w=fovBounds.getWidth();
            h=fovBounds.getHeight();
        }        
        if (acceptTilePos(seedX,seedY,w,h,tSetting.isInsideOnly())) {
            if (tSetting.isCluster() && (tSetting.getNrClusterTileX()>1 || tSetting.getNrClusterTileY()>1)) {
                return createCluster(clusterNr,seedX,seedY,fovX,fovY, tSetting);
            }
            else {
                tilePosList.add(Tile.createTile(createTileName(clusterNr,0), seedX, seedY, relativeZPos, false));
                return TILING_OK;
            }    
        } else
            return TILING_ERROR;
    }
    
    
    //NEED TO SET RELZPOS
    private int calcRuntimeTilePositions(DoxelManager doxelManager,double fovX, double fovY, TilingSetting tSetting) throws InterruptedException {
        if (doxelManager==null || doxelManager.getAreaMap().isEmpty()) {
            IJ.log("    Area.calcRuntimeTilePosition: "+(doxelManager==null ? "doxelManager=null" : "no ROIs"));
            unknownTileNum=true;
            tilePosList.clear();
            tilingStatus=TILING_UNKNOWN_NUMBER;
            return tilingStatus;
        }
        doxelManager.consolidateDoxelsIn2D(0, true);//0% overlap, ignore z-pos
        List<Doxel> seedList=doxelManager.getDoxels(name);
        if (seedList!=null) {
            IJ.log("    Area.calcRuntimeTilePosition, "+name+", ROIs: "+seedList.size());
            int size=Math.max(seedList.size(),tSetting.getMaxSites());
            if (tSetting.isCluster())
                size=size*tSetting.getNrClusterTileX()*tSetting.getNrClusterTileY();
            tilePosList = new ArrayList<Tile>(size);
            int acceptedNr=0;
            while (!Thread.currentThread().isInterrupted() && seedList.size()>0 && acceptedNr <= tSetting.getMaxSites()) {// && !abortTileCalc) {
                //use random index in seedList
                int index=(int)Math.round(Math.random()*(seedList.size()-1));
                //xPos and yPos are layout coordinates (already converted by DoxelManager
                if (addTilesAroundSeed(acceptedNr,seedList.get(index).xPos, seedList.get(index).yPos, fovX, fovY, tSetting) == TILING_OK) {
                    acceptedNr++;
                }    
                seedList.remove(index);
            }
            for (Tile t:tilePosList) {
                IJ.log("    Area.calcRuntimeTilePositions: layout: "+t.centerX+", "+t.centerY+", "+t.zPos);
            }
        } else {
            tilePosList.clear();
        }   
        unknownTileNum=Thread.currentThread().isInterrupted();
        tilingStatus=TILING_OK;
        if (unknownTileNum) {
            tilingStatus=TILING_UNKNOWN_NUMBER;
        } 
        return (tilingStatus==TILING_OK ? tilePosList.size() : tilingStatus);
    }

    public synchronized int calcTilePositions(DoxelManager doxelManager,double fovX, double fovY, TilingSetting setting) throws InterruptedException {
        if (selectedForAcq) {
            switch (setting.getMode()) {
                case FULL:      
                            return calcFullTilePositions(fovX, fovY, setting);
                case CENTER:    
                            return calcCenterTilePositions(fovX, fovY, setting);
                case RANDOM:    
                            return calcRandomTilePositions(fovX, fovY, setting);
                case ADAPTIVE:
                            return calcRuntimeTilePositions(doxelManager, fovX, fovY, setting);
                case FILE:      {
                            unknownTileNum=true;
                            return TILING_UNKNOWN_NUMBER;
                        }
            }
        }
        return 0;
    }

    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id=id;
    }
    
    @Override
    public boolean equals(Object object) {
        boolean same = false;
        if (object != null && object instanceof SampleArea)
            same = this.id==((SampleArea) object).getId();
        return same;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + this.id;
        return hash;
    }
    
    public double getTopLeftX() {
        return topLeftX;
    } 
    
    public void setTopLeft(double x, double y) {
        topLeftX=x;
        topLeftY=y;
        calcCenterAndDefaultPos();
        createShape();
        createGeneralPath();
//        calculateCenterPos();
//        calculateDefaultPos();
    }
    
    public void setTopLeftX(double x) {
        topLeftX=x;
        calcCenterAndDefaultPos();
        createShape();
        createGeneralPath();
//        calculateCenterPos();
//        calculateDefaultPos();
    }
    
    public void setTopLeftY(double y) {
        topLeftY=y;
        calcCenterAndDefaultPos();
        createShape();
        createGeneralPath();
//        calculateCenterPos();
//        calculateDefaultPos();
    }
    
    public double getTopLeftY() {
        return topLeftY;
    } 
    
    public Point2D getCenterXYPos() {
        if (centerXYPos==null) {
            calcCenterAndDefaultPos();
//            calculateCenterPos();
        }
        return centerXYPos;
    }
    
    public Point2D getDefaultXYPos() {
        if (defaultXYPos==null) {
            calcCenterAndDefaultPos();
//            calculateDefaultPos();
        }
        return defaultXYPos;
    }
    
    public double getRelativeZPos() {
        return relativeZPos;
    }
    
    public void setRelativeZPos(double z) {
        relativeZPos=z;
    }
 
    public double getWidth() {
        return width;
    } 
    
    public void setWidth(double value) {
        width=value;
        calcCenterAndDefaultPos();
        createShape();
        createGeneralPath();
//        calculateCenterPos();
//        calculateDefaultPos();
    }
    
    public double getHeight() {
        return height;
    } 
    
    public void setHeight(double value) {
        height=value;
        calcCenterAndDefaultPos();
        createShape();
        createGeneralPath();
//        calculateCenterPos();
//        calculateDefaultPos();
    }
    
    public void setAffineTransform(AffineTransform at) {
        affineTrans=at;
        calcCenterAndDefaultPos();
        createShape();
        createGeneralPath();
    }
    
    public AffineTransform getAffineTransform() {
        return affineTrans;
    }
    
    public void setSelectedForAcq(boolean b) {
        selectedForAcq=b;
        if (!b) 
            tilingStatus=TILING_OK;
    }
    
    public boolean isSelectedForAcq() {
        return selectedForAcq;
    } 

    public void setSelectedForMerge(boolean b) {
        selectedForMerge=b;
    }
    
    public boolean isSelectedForMerge() {
        return selectedForMerge;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String n) {
        name=n;
    }
            
    public String getComment() {
        return comment;
    }

    public void setComment(String c) {
        comment=c;
    }
    
    public int getTileNumber() {
        if (tilePosList!=null)
            return tilePosList.size();
        else return 0;
    }
    
    public JSONObject createSiteInfo(String tileName) {
        JSONObject info=new JSONObject();
        try {
            info.put(ExtImageTags.AREA_NAME,name);
            info.put(ExtImageTags.AREA_INDEX,index);
            info.put(ExtImageTags.CLUSTERS_IN_AREA,noOfClusters);
            info.put(ExtImageTags.SITES_IN_AREA,tilePosList.size());
//            if (tileName.contains("Cluster")) {
                int startIndex=tileName.indexOf("Cluster")+7;
                int endIndex=tileName.indexOf("Site")-1;
                info.put(ExtImageTags.CLUSTER_INDEX, Long.parseLong(tileName.substring(startIndex,endIndex)));
//            } else
//                info.put(ExtImageTags.CLUSTER_INDEX, "-1");
            startIndex=tileName.indexOf("Site")+4;
            info.put(ExtImageTags.SITE_INDEX, Long.parseLong(tileName.substring(startIndex)));
            info.put(ExtImageTags.AREA_COMMENT, comment);
        } catch (JSONException ex) {
            Logger.getLogger(SampleArea.class.getName()).log(Level.SEVERE, null, ex);
        }
        return info;
    }
    
    public PositionList addTilePositions(PositionList pl, ArrayList<JSONObject> posInfoL, String xyStageLabel, String zStageLabel, IAcqLayout aLayout) {
        if (pl==null)
            pl=new PositionList();
        for (Tile tile:tilePosList) {
            Vec3d stagePos;
            try {
                if (tile.isAbsolute) {
                    stagePos=new Vec3d(tile.centerX,tile.centerY,tile.zPos);
                } else {
                    stagePos = aLayout.convertLayoutToStagePos(tile.centerX,tile.centerY, tile.zPos);
    //                stagePos = aLayout.convertLayoutToStagePos(tile.centerX,tile.centerY, relativeZPos);
                }
                MultiStagePosition msp=new MultiStagePosition(xyStageLabel, stagePos.x, stagePos.y, zStageLabel, stagePos.z);
                msp.setLabel(name+"-"+tile.name);
                pl.addPosition(msp);
                if (posInfoL!=null)
                    posInfoL.add(createSiteInfo(tile.name));
            } catch (Exception ex) {
                IJ.log("Area.addTilePosition: "+ex.getMessage());
                Logger.getLogger(SampleArea.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return pl;
    }
    
    public List<Tile> getTilePositions() {
        return tilePosList;
    }

    private static String paddedTileIndex(int index) {
        return String.format(TILE_NAME_PADDING, index);
    }
    
    public boolean acceptTilePos(double x, double y, double fovX, double fovY, boolean insideOnly) {
        if (insideOnly)
            return isFovInsideArea(x,y,fovX,fovY);
        else
            return doesFovTouchArea(x,y,fovX,fovY);
    }

    //used to initialize SampleArea from layout file
    public final static SampleArea createFromJSONObject(JSONObject obj) throws JSONException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        String className=obj.getString(TAG_CLASS);
        Class clazz=null;
        try {
            clazz=Class.forName(className);
        } catch (Exception ex) {
            //open layouts saved before refactoring of SampleArea classes into autoimage.area package, 
            //e.g. "autoimage.RectArea" should be initialized as "autoimage.area.RectArea"
            className=className.substring(0,className.lastIndexOf("."))+".area"+className.substring(className.lastIndexOf("."));
            clazz=Class.forName(className);
        }
        SampleArea area=(SampleArea) clazz.newInstance();
        area.name=obj.getString(TAG_NAME);
        area.id=obj.getInt(TAG_ID);
        area.width=obj.getDouble(TAG_WIDTH);
        area.height=obj.getDouble(TAG_HEIGHT);
        area.topLeftX=obj.getDouble(TAG_TOP_LEFT_X);
        area.topLeftY=obj.getDouble(TAG_TOP_LEFT_Y);
        area.relativeZPos=obj.getDouble(TAG_REL_POS_Z);
        area.selectedForAcq=obj.getBoolean(TAG_SELECTED);
        area.comment=obj.getString(TAG_COMMENT);
        JSONArray atMatrix=obj.getJSONArray(TAG_AFFINETRANSFORM);
        double[] matrix=new double[6];
        for (int i=0; i<atMatrix.length(); i++) {
            matrix[i]=atMatrix.getDouble(i);
        }
        area.affineTrans=new AffineTransform(matrix);
        //read in additional properties that are not part of SampleArea definition
        area.initializeFromJSONObject(obj);
        //create shape and generalPath objects
        area.calcCenterAndDefaultPos();
        area.createShape();
        area.createGeneralPath();
        return area;
    }
    
    //used to create JSONObject descriptionm of SampleArea to write to Layout file
    public final JSONObject toJSONObject() throws JSONException {
        JSONObject obj=new JSONObject();
        obj.put(TAG_CLASS,this.getClass().getName());
        obj.put(TAG_NAME,name);
        obj.put(TAG_ID,id);
        obj.put(TAG_WIDTH,width);
        obj.put(TAG_HEIGHT,height);
        obj.put(TAG_TOP_LEFT_X,topLeftX);
        obj.put(TAG_TOP_LEFT_Y,topLeftY);
        obj.put(TAG_REL_POS_Z,relativeZPos);
        obj.put(TAG_SELECTED,selectedForAcq);
        obj.put(TAG_COMMENT,comment);
        double[] matrix=new double[6];
        affineTrans.getMatrix(matrix);
        JSONArray atMatrixArray=new JSONArray();
        for (double m:matrix)
            atMatrixArray.put(m);
        obj.put(TAG_AFFINETRANSFORM, atMatrixArray);
        addFieldsToJSONObject(obj);
        return obj;
    }
    
    //used to sort areas by name
    public static Comparator<String> NameComparator = new Comparator<String>() {

        @Override
	public int compare(String a1, String a2) {
            String a1Name = a1.toUpperCase();
            String a2Name = a2.toUpperCase();
            return a1Name.compareTo(a2Name);
        }
    };
        
    //used to sort areas by # of tiles
    public static Comparator<String> TileNumComparator = new Comparator<String>() {

        @Override
	public int compare(String a1, String a2) {
            if (a1.contains("?") && a2.contains("?"))
                return 0;
            if (a1.contains("?"))
                return -1;
            if (a2.contains("?"))
                return 1;
            if (a1.contains(" (Error)"))
                a1=a1.substring(0,a1.indexOf(" (Error"));
            if (a2.contains(" (Error)"))
                a2=a2.substring(0,a2.indexOf(" (Error"));
            Integer a1tiles = Integer.parseInt(a1);
            Integer a2tiles = Integer.parseInt(a2);;
            return a1tiles.compareTo(a2tiles);
        }
    };
        
    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return shape.getPathIterator(at);
    }
    
    @Override
    public PathIterator getPathIterator(AffineTransform at, double d) {
        return shape.getPathIterator(at, d);
    }
    
    @Override
    public boolean contains(Point2D p) {
        return shape.contains(p);
    }
    
    @Override
    public boolean contains(double x, double y) {
        return shape.contains(x,y);
    }
    
    @Override
    public boolean contains(Rectangle2D r) {
        return shape.contains(r);
    }
    
    @Override
    public boolean contains(double x, double y, double w, double h) {
        return shape.contains(x,y,w,h);
    }
    
    @Override
    public boolean intersects(Rectangle2D r) {
        return shape.intersects(r);
    }
    
    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return shape.intersects(x,y,w,h);
    }
    
    @Override
    public Rectangle getBounds() {
        return shape.getBounds();
    }
    
    @Override
    public Rectangle2D getBounds2D() {
        return shape.getBounds2D();
    }
    
    protected void createGeneralPath() {
        if (shape!=null) {
            generalPath=new GeneralPath();
            generalPath.append(shape.getPathIterator(affineTrans),false); 
            //generalPath.closePath();
            PathIterator pi=generalPath.getPathIterator(affineTrans);
            IJ.log(this.name+" ("+this.getClass().getName());
            while (!pi.isDone()) {
                double[] coords=new double[6];
                int type=pi.currentSegment(coords);
                IJ.log("   Type:"+type);
                for (double d:coords) {
                    IJ.log("       coord:"+Double.toString(d));
                }
                pi.next();
            }
        }
    }
    
    public GeneralPath getGeneralPath() {
        return generalPath;
    }

            
    /**
     * Used to convert JSONObject to SampleArea, 
     * derived classes should overwrite this to save fields that are not part of SampleArea class
     * @param obj JSONObject with values for all area's fields
     * @throws org.json.JSONException when obj cannot be parsed
     */
    protected abstract void initializeFromJSONObject(JSONObject obj) throws JSONException;

    /** 
     * Used to create a JSONObject representation of SampleArea,
     * derived classes should overwrite this to save fields that are not part of SampleArea class
     * @param obj JSONObject to which additional field values should be added
     * @throws org.json.JSONException when operation on obj generates error
     */
    protected abstract void addFieldsToJSONObject(JSONObject obj) throws JSONException;

    public abstract String getShapeType();
    
    public abstract List<Point2D> getOutlinePoints();
                
    protected abstract void calcCenterAndDefaultPos();
    
    protected abstract void createShape();
            
    public abstract boolean isInArea(double x, double y);

    //redundant with contains?
    public abstract boolean isFovInsideArea(double centerX, double centerY, double fovWidth, double fovHeight);
    
    //redundant with intersect?
    public abstract boolean doesFovTouchArea(double centerX, double centerY, double fovWidth, double fovHeight);

    public abstract boolean isInsideRect(Rectangle2D r);
    
    /**
     * creates new instance and deep copy of this object
     * @return SampleArea
     */
    public abstract SampleArea duplicate();
    
    /**
     * @param layoutBounds width and height of layout (in mm)
     * @return 
     */
    public abstract SampleArea showConfigDialog(Rectangle2D layoutBounds);
    
    /** 
     * @return int value describing layout that handle this SampleArea object
     * 1: AcqPlateLayout (Well Plate Layout)
     * 2: AcqCustomLayout (Custom Layout)
     */
    public abstract int supportedLayouts();

}
