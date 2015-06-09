package autoimage.area;

import autoimage.AcqLayout;
import autoimage.ExtImageTags;
import autoimage.FieldOfView;
import autoimage.Tile;
import autoimage.TileManager;
import autoimage.TilingSetting;
import autoimage.Vec3d;
import ij.IJ;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.api.MultiStagePosition;
import org.micromanager.api.PositionList;

/**
 *
 * @author Karsten Siller
 */
public abstract class Area {
    public static final String TAG_AREA = "AREA";
    public static final String TAG_AREA_ARRAY = "AREA_ARRAY";
    public static final String TAG_CLASS = "CLASS";
    public static final String TAG_NAME = "NAME";
    public static final String TAG_ID="ID";
 //   public static final String TAG_SHAPE = "SHAPE";
    public static final String TAG_COMMENT = "COMMENT";
    public static final String TAG_TOP_LEFT_X = "TOP_LEFT_X";
    public static final String TAG_TOP_LEFT_Y = "TOP_LEFT_Y";
    public static final String TAG_CENTER_X = "CENTER_X";
    public static final String TAG_CENTER_Y = "CENTER_Y";
    public static final String TAG_WIDTH = "WIDTH";
    public static final String TAG_HEIGHT = "HEIGHT";
    public static final String TAG_RING_WIDTH = "RING_WIDTH";
    public static final String TAG_REL_POS_Z = "REL_POS_Z";
    public static final String TAG_SELECTED = "SELECTED";

    public static final String TAG_TILE = "TILE";
    public static final String TAG_CLUSTER = "CLUSTER";
    
    public static final int SUPPORT_CUSTOM_LAYOUT = 2;
    public static final int SUPPORT_WELLPLATE_LAYOUT = 1;
    public static final int SUPPORT_ALL_LAYOUTS = 128;
    
    protected String name;
    protected static double cameraRot = FieldOfView.ROTATION_UNKNOWN;//in radians relative to x-y stage axis, NOT layout
    protected static double stageToLayoutRot = 0; //in radians
    protected static boolean optimizedForCameraRotation = true;
//    protected static String shape;
//    protected TilingSetting tiling;
    protected List<Tile> tilePosList;//has absolute layout positions in um
    protected int tileNumber;
    protected int id; //unique identifier, reflects order of addition 
    protected int index; //index in arraylist of selected areas; required for metadata annotation 
    protected double topLeftX; //in um
    protected double topLeftY; //in um
    protected Point2D centerPos;
    protected Point2D defaultPos;
    protected double relPosZ; //in um relative to flat layout bottom
    protected double width; //in um
    protected double height; //in um
    protected boolean selectedForAcq;
    protected boolean selectedForMerge;
    protected String comment;
    protected boolean acquiring;
//    private boolean abortTileCalc;
    private boolean unknownTileNum; //is set when tilingmode is "runtime" or "file", or pixel size is not calibrated
    private int noOfClusters;
    
    public static final Color COLOR_UNSELECTED_AREA = Color.GRAY;
    public static final Color COLOR_AREA_BORDER = Color.WHITE;
    public static final Color COLOR_SELECTED_AREA_BORDER = Color.YELLOW;
    public static final Color COLOR_MERGE_AREA_BORDER = Color.RED; //new Color(51,115,188);
    public static final Color COLOR_SELECTED_AREA = Color.RED;
    public static final Color COLOR_ACQUIRING_AREA = new Color(203,188,47);//Color.YELLOW;
    public static final Color COLOR_TILE_GRID = Color.RED;
    public static final String[] PLATE_ALPHABET={"A","B","C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "AA", "AB", "AC", "AD", "AE", "AF"};
    public static final String TILE_NAME_PADDING = "%06d"; //padds tile name with preceding '0' to fill up to 6 digits
    public static final String CLUSTER_NAME_PADDING = "%06d";
    
    protected static final long DELAY_TO_CANCEL = 1000;
    protected static final int MAX_TILING_ATTEMPTS = 200;

           
    public Area() {
        this("NewArea", -1, 0,0,0,0,0,false,"");
    }
    
    public Area(String n) { //expects name identifier
        this(n, -1, 0,0,0,0,0,false,"");
    }
    
    //expects layout coordinates in phys dim
    public Area(String n, int id, double ox, double oy, double oz, double w, double h, boolean s, String anot) {
        name=n;
        this.id=id;
        index=-1;
        topLeftX=ox;
        topLeftY=oy;
        relPosZ=oz;
        width=w;
        height=h;
        selectedForAcq=s;
//        abortTileCalc=false;
        comment=anot;
        acquiring=false;
//        tiling=new TilingSetting();
        tilePosList=null;
        unknownTileNum=true;
        noOfClusters=0;
        calcCenterAndDefaultPos();
//        centerPos=calculateCenterPos();
//        defaultPos=calculateDefaultPos();
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
    
    public Tile createTile(String n, double x, double y, double z) {
        return new Tile(n,x,y,z);
    }
    
    public void setAcquiring(boolean b) {
//        IJ.log("Area.setAcquiring");
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
/*    
    public void abortTileCalc() {
        abortTileCalc=true;
    }
    
    public void enableTileCalc() {
        abortTileCalc=false;
    }
*/    
/*    public void setTilePosList(List<Tile> tl) {
        tilePosList=tl;
    }
    
    public List<Tile> getTilePositionList() {
        return tilePosList;
    }
*/    
    private List<Tile> copyCluster(int clusterNr, double offsetX, double offsetY, List<Tile> source) {
        List<Tile> newCluster=null;
        if (source!=null) {
            newCluster=new ArrayList<Tile>(source.size());
            int i=1;
            for (Tile t:source) {
                newCluster.add(new Tile(createTileName(clusterNr,i),t.centerX+offsetX,t.centerY+offsetY,t.relZPos));
                i++;
            }
        }
        return newCluster;
    }
    
    static String createTileName(int clusterNr, int siteNr) {
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
            return 0;
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
//                    IJ.log("Area.createCluster: interrupted, Area "+name);
                    break;
                }    
                x = startx+tileOffset.getX()*col;
                row=vDir == 1 ? 0 : yTiles-1;
                for (int j=0; j<yTiles; j++) {   
                    y = starty+tileOffset.getY()*row;
    //                IJ.log(name+", test Tile: "+Integer.toString(i*yTiles+j)+", "+Double.toString(x)+","+Double.toString(y));
                    Tile t=new Tile(createTileName(clusterNr,siteCounter), x, y, relPosZ);                
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
//                    IJ.log("Area.createCluster: interrupted, Area "+name);
                    break;
                }    
                y = starty+tileOffset.getY()*row;
                col=hDir == 1 ? 0 : xTiles-1;
                for (int i=0; i<xTiles; i++) {  
                    x = startx+tileOffset.getX()*col;
  //                  IJ.log(name+", test Tile: "+Integer.toString(i*yTiles+j)+", "+Double.toString(x)+","+Double.toString(y));
                    Tile t=new Tile(createTileName(clusterNr,siteCounter), x, y, relPosZ);                
                    tilePosList.add(t);
   //                 IJ.log(name+", "+Integer.toString(i*yTiles+j)+", "+Double.toString(x)+","+Double.toString(y));
                    col+=hDir;
                    siteCounter++;
                }
                row+=vDir;
                hDir=-hDir;
            }            
        }
  //      IJ.log("Area.creatCluster: end"); 
        return siteCounter;
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
//       IJ.log("Area.calculateTilePositions: xTiles: "+Integer.toString(xTiles));
//        IJ.log("Area.calculateTilePositions: yTiles: "+Integer.toString(yTiles));
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
//                    IJ.log("Area.calcFullTilePositions: interrupted, Area "+name);
                    break;
                }    
                x = startx+tileOffset.getX()*col;
                row=vDir == 1 ? 0 : yTiles-1;
                for (int j=0; j<yTiles; j++) {   
                    y = starty+tileOffset.getY()*row;
//                    IJ.log(name+", test Tile: "+Integer.toString(i*yTiles+j)+", "+Double.toString(x)+","+Double.toString(y));
                    if (acceptTilePos(x,y,fovBounds.getWidth(),fovBounds.getHeight(),insideOnly)) {
                        siteCounter++;
//                        Tile t=new Tile("Site"+paddedTileIndex(siteCounter), x, y, relPosZ);  
                        Tile t=new Tile(createTileName(0,siteCounter), x, y, relPosZ);
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
//                    IJ.log("Area.calcFullTilePositions: interrupted, Area "+name);
                    break;
                }    
                y = starty+tileOffset.getY()*row;
                col=hDir == 1 ? 0 : xTiles-1;
                for (int i=0; i<xTiles; i++) {  
                    x = startx+tileOffset.getX()*col;
 //                   IJ.log(name+", test Tile: "+Integer.toString(i*yTiles+j)+", "+Double.toString(x)+","+Double.toString(y));
                    if (acceptTilePos(x,y,fovBounds.getWidth(),fovBounds.getHeight(),insideOnly)) {
                        siteCounter++;
//                        Tile t=new Tile("Site"+paddedTileIndex(siteCounter), x, y, relPosZ);                
                        Tile t=new Tile(createTileName(0,siteCounter), x, y, relPosZ);
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
        unknownTileNum=Thread.currentThread().isInterrupted();
        noOfClusters = (tilePosList.size() > 0 ?  1 :  0);
        return tilePosList.size();
    }

    private int calcRandomTilePositions(double fovX, double fovY, TilingSetting setting) throws InterruptedException {
        Rectangle2D fovBounds=getFovBounds(fovX, fovY);
        Point2D tileOffset=calculateTileOffset(fovX, fovY, setting.getTileOverlap());

        int size=setting.getMaxSites();
        if (setting.isCluster())
            size=size*setting.getNrClusterTileX()*setting.getNrClusterTileY();
        int iterations=0;
        double w;
        double h;
        if (setting.isCluster() && (setting.getNrClusterTileX()>1 || setting.getNrClusterTileY()>1)) {
            w=fovBounds.getWidth()+tileOffset.getX()*(setting.getNrClusterTileX()-1);
            h=fovBounds.getHeight()+tileOffset.getY()*(setting.getNrClusterTileY()-1);
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
            while (!Thread.currentThread().isInterrupted() && noOfClusters < setting.getMaxSites() && attempts < MAX_TILING_ATTEMPTS) {// && !abortTileCalc) {
                double x=topLeftX+Math.random()*width;
                double y=topLeftY+Math.random()*height;
                if (acceptTilePos(x,y,w,h,setting.isInsideOnly())) {
                    if (setting.isCluster() && (setting.getNrClusterTileX()>1 || setting.getNrClusterTileY()>1)) {
                        if (createCluster(noOfClusters,x,y,fovX,fovY, setting) > 0) {
                            noOfClusters++;
                        }
                    } else {
                        if (setting.isSiteOverlap() || (!setting.isSiteOverlap() && !overlapsOtherTiles(new Rectangle2D.Double(x-fovBounds.getWidth()/2,y-fovBounds.getHeight()/2,fovX,fovY),fovX,fovY))) {
                            tilePosList.add(new Tile(createTileName(0,noOfClusters), x, y, relPosZ));
                            noOfClusters++;
                        }
                    }
                }
                attempts++;
            }
            iterations++;
        } while (!Thread.currentThread().isInterrupted() && noOfClusters < setting.getMaxSites() && iterations < (MAX_TILING_ATTEMPTS/10)); // && !abortTileCalc) {
    
        unknownTileNum=(Thread.currentThread().isInterrupted() || (noOfClusters < setting.getMaxSites() && iterations >= MAX_TILING_ATTEMPTS/10));
        if (noOfClusters < setting.getMaxSites() && iterations >= MAX_TILING_ATTEMPTS/10.0d) {
            return -1;//unsuccessful
        } else {
            return tilePosList.size();
        }
    }
    
    private int calcCenterTilePositions(double fovX, double fovY, TilingSetting tSetting) throws InterruptedException {
        int size=1;
        if (tSetting.isCluster())
            size=size*tSetting.getNrClusterTileX()*tSetting.getNrClusterTileY();
//        tilePosList.clear();
        tilePosList = new ArrayList<Tile>(size);
        if (centerPos==null || defaultPos==null) {
            calcCenterAndDefaultPos();
        }
        addTilesAroundSeed(0,centerPos.getX(), centerPos.getY(), fovX, fovY, tSetting);
        unknownTileNum=Thread.currentThread().isInterrupted();
        noOfClusters=tilePosList.size() > 0 ? 1 : 0;
        return tilePosList.size();
    }
    
    
    private synchronized int addTilesAroundSeed(int clusterNr,double seedX, double seedY, double fovX, double fovY, TilingSetting tSetting) {
        Point2D tileOffset=Area.calculateTileOffset(fovX, fovY, tSetting.getTileOverlap());
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
                tilePosList.add(new Tile(createTileName(clusterNr,0), seedX, seedY, relPosZ));
                return 1;
            }    
        } else
            return 0;
    }
    
    
    //NEED TO SET RELZPOS
    private int calcRuntimeTilePositions(TileManager tileManager,double fovX, double fovY, TilingSetting tSetting) throws InterruptedException {
        if (tileManager==null || tileManager.getAreaMap().isEmpty()) {
            IJ.log("    Area.calcRuntimeTilePosition: "+(tileManager==null ? "tileManager=null" : "no ROIs"));
            unknownTileNum=true;
            tilePosList.clear();
            return 0;
        }
        tileManager.consolidateTiles(0);
        List<Tile> seedList=tileManager.getTiles(name);
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
                if (addTilesAroundSeed(acceptedNr,seedList.get(index).centerX, seedList.get(index).centerY, fovX, fovY, tSetting) > 0) {
                    acceptedNr++;
                }    
                seedList.remove(index);
            }
            for (Tile t:tilePosList) {
                IJ.log("    Area.calcRuntimeTilePositions: layout: "+t.centerX+", "+t.centerY+", "+t.relZPos);
            }
        } else {
            tilePosList.clear();
        }   
        unknownTileNum=Thread.currentThread().isInterrupted();
        return tilePosList.size();
    }

    public synchronized int calcTilePositions(TileManager tileManager,double fovX, double fovY, TilingSetting setting) throws InterruptedException {
        if (selectedForAcq) {
            switch (setting.getMode()) {
                case FULL:      
                            return calcFullTilePositions(fovX, fovY, setting);
                case CENTER:    
                            return calcCenterTilePositions(fovX, fovY, setting);
                case RANDOM:    
                            return calcRandomTilePositions(fovX, fovY, setting);
                case RUNTIME:
                            return calcRuntimeTilePositions(tileManager, fovX, fovY, setting);
                case FILE:      {
                            unknownTileNum=true;
                            return 0;
                        }
            }
        }
        return 0;
    }

    public void drawTileByTileOvl(Graphics2D g2d, int bdPix, double physToPixelRatio, double fovX, double fovY, TilingSetting setting) { /*AcqSetting setting) {*/
        if (tilePosList!=null) {
            g2d.setColor(COLOR_TILE_GRID);
                Composite oldComposite=g2d.getComposite();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.25f));
            for (int i=0; i<tilePosList.size(); i++) {
                Tile t=tilePosList.get(i);
                int xo=bdPix+(int)Math.round((t.centerX-fovX/2)*physToPixelRatio);
                int yo=bdPix+(int)Math.round((t.centerY-fovY/2)*physToPixelRatio);
                int xCenter=bdPix+(int)Math.round(t.centerX*physToPixelRatio);
                int yCenter=bdPix+(int)Math.round(t.centerY*physToPixelRatio);
                int w=(int)Math.round(fovX*physToPixelRatio);
                int h=(int)Math.round(fovY*physToPixelRatio);
                
                AffineTransform at=g2d.getTransform();
                g2d.translate(xCenter,yCenter);
                g2d.rotate(stageToLayoutRot);
                if (cameraRot != FieldOfView.ROTATION_UNKNOWN) {
                    g2d.rotate(-cameraRot);                        
                }
                g2d.translate(-xCenter,-yCenter);
                g2d.fillRect(xo,yo,w,h);
                g2d.setTransform(at);
                
            }    
            g2d.setComposite(oldComposite);
        }
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
        if (object != null && object instanceof Area)
            same = this.id==((Area) object).getId();
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
//        calculateCenterPos();
//        calculateDefaultPos();
    }
    
    public void setTopLeftX(double x) {
        topLeftX=x;
        calcCenterAndDefaultPos();
//        calculateCenterPos();
//        calculateDefaultPos();
    }
    
    public void setTopLeftY(double y) {
        topLeftY=y;
        calcCenterAndDefaultPos();
//        calculateCenterPos();
//        calculateDefaultPos();
    }
    
    public double getTopLeftY() {
        return topLeftY;
    } 
    
    public Point2D getCenterPos() {
        if (centerPos==null) {
            calcCenterAndDefaultPos();
//            calculateCenterPos();
        }
        return centerPos;
    }
    
    public Point2D getDefaultPos() {
        if (defaultPos==null) {
            calcCenterAndDefaultPos();
//            calculateDefaultPos();
        }
        return defaultPos;
    }
    
    public double getRelPosZ() {
        return relPosZ;
    }
    
    public void setRelPosZ(double z) {
        relPosZ=z;
    }
 
    public double getWidth() {
        return width;
    } 
    
    public void setWidth(double value) {
        width=value;
        calcCenterAndDefaultPos();
//        calculateCenterPos();
//        calculateDefaultPos();
    }
    
    public double getHeight() {
        return height;
    } 
    
    public void setHeight(double value) {
        height=value;
        calcCenterAndDefaultPos();
//        calculateCenterPos();
//        calculateDefaultPos();
    }
    
    public void setSelectedForAcq(boolean b) {
        selectedForAcq=b;
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
            Logger.getLogger(Area.class.getName()).log(Level.SEVERE, null, ex);
        }
        return info;
    }
    
    public PositionList addTilePositions(PositionList pl, ArrayList<JSONObject> posInfoL, String xyStageLabel, String zStageLabel, AcqLayout aLayout) {
        if (pl==null)
            pl=new PositionList();
        for (Tile tile:tilePosList) {
            Vec3d stage;
            try {
                stage = aLayout.convertLayoutToStagePos(tile.centerX,tile.centerY, tile.relZPos);
//                stage = aLayout.convertLayoutToStagePos(tile.centerX,tile.centerY, relPosZ);
                MultiStagePosition msp=new MultiStagePosition(xyStageLabel, stage.x, stage.y, zStageLabel, stage.z);
                msp.setLabel(name+"-"+tile.name);
                pl.addPosition(msp);
                if (posInfoL!=null)
                    posInfoL.add(createSiteInfo(tile.name));
            } catch (Exception ex) {
                IJ.log("Area.addTilePosition: "+ex.getMessage());
                Logger.getLogger(Area.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return pl;
    }
    
    public List<Tile> getTilePositions() {
        return tilePosList;
    }
/*        
    public boolean setAreaParams (List<String> params) {
        if (params!=null && params.size()>=1 && params.get(0).equals(this.getName())) 
            return true;
        else 
            return false;
    }

    public void setAreaParams (Map<String,String> params) {
        if (params!=null) {
//            String s=params.get(TAG_SHAPE);
//            if (s!=null)
//                shape=s;
            String s=params.get(TAG_NAME);
            if (s!=null)
                name=s;
            s=params.get(TAG_WIDTH);
            if (s!=null)
                width=Double.parseDouble(s);
            s=params.get(TAG_HEIGHT);
            if (s!=null)
                height=Double.parseDouble(s);
            s=params.get(TAG_TOP_LEFT_X);
            if (s!=null)
                topLeftX=Double.parseDouble(s);
            s=params.get(TAG_TOP_LEFT_Y);
            if (s!=null)
                topLeftY=Double.parseDouble(s);
            s=params.get(TAG_REL_POS_Z);
            if (s!=null)
                relPosZ=Double.parseDouble(s);
            s=params.get(TAG_SELECTED);
            if (s!=null)
                selectedForAcq=Boolean.parseBoolean(s);
            s=params.get(TAG_COMMENT);
            if (s!=null)
                comment=s;    
        }       
    }
    
    protected Map<String,String> createParamHashMap() {
        Map<String,String> map = new HashMap<String,String>();
        map.put(TAG_CLASS,"AREA-ABSTRACT CLASS");
//        map.put(TAG_SHAPE,shape);
        map.put(TAG_NAME,name);
        map.put(TAG_WIDTH,Double.toString(width));
        map.put(TAG_HEIGHT,Double.toString(height));
        map.put(TAG_TOP_LEFT_X,Double.toString(topLeftX));
        map.put(TAG_TOP_LEFT_Y,Double.toString(topLeftY));
        map.put(TAG_REL_POS_Z,Double.toString(relPosZ));
        map.put(TAG_SELECTED,Boolean.toString(selectedForAcq));
        map.put(TAG_COMMENT,comment);
        return map;
    }
*/    

    private static String paddedTileIndex(int index) {
        return String.format(TILE_NAME_PADDING, index);
    }
    
/*   
    //update uses area' relPos as base position and adds offset based on xy distance to RefArea and layouts normalVec
    public void calculateTilePositionAbsZ(ArrayList<RefArea> rpList, Vec3d normalVec) {
        if (rpList!=null & rpList.size()>0) {
            double baseZ=rpList.get(0).getStageCoordZ()-rpList.get(0).getLayoutCoordZ();
            double z;
            for (int i=0; i<tilePosList.size(); i++) {
                z=relPosZ+baseZ;
//                tilePosList.get(i).setRelZPos=z;
            }
        }
    }
*/                 
    public boolean acceptTilePos(double x, double y, double fovX, double fovY, boolean insideOnly) {
        if (insideOnly)
            return isFovInsideArea(x,y,fovX,fovY);
        else
            return doesFovTouchArea(x,y,fovX,fovY);
    }

    //used to initialize Area from layout file
    public final static Area createFromJSONObject(JSONObject obj) throws JSONException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        String className=obj.getString(TAG_CLASS);
        Class clazz=Class.forName(className);
        Area area=(Area) clazz.newInstance();
        area.name=obj.getString(TAG_NAME);
        area.id=obj.getInt(TAG_ID);
        area.width=obj.getDouble(TAG_WIDTH);
        area.height=obj.getDouble(TAG_HEIGHT);
        area.topLeftX=obj.getDouble(TAG_TOP_LEFT_X);
        area.topLeftY=obj.getDouble(TAG_TOP_LEFT_Y);
        area.relPosZ=obj.getDouble(TAG_REL_POS_Z);
        area.selectedForAcq=obj.getBoolean(TAG_SELECTED);
        area.comment=obj.getString(TAG_COMMENT);
        area.initializeFromJSONObject(obj);
        area.calcCenterAndDefaultPos();
        return area;
    }
    
    //used to create JSONObject descriptionm of Area to write to Layout file
    public final JSONObject toJSONObject() throws JSONException {
        JSONObject obj=new JSONObject();
        obj.put(TAG_CLASS,this.getClass().getName());
        obj.put(TAG_NAME,name);
        obj.put(TAG_ID,id);
        obj.put(TAG_WIDTH,width);
        obj.put(TAG_HEIGHT,height);
        obj.put(TAG_TOP_LEFT_X,topLeftX);
        obj.put(TAG_TOP_LEFT_Y,topLeftY);
        obj.put(TAG_REL_POS_Z,relPosZ);
        obj.put(TAG_SELECTED,selectedForAcq);
        obj.put(TAG_COMMENT,comment);
        addFieldsToJSONObject(obj);
        return obj;
    }
    
    public static Comparator<String> NameComparator = new Comparator<String>() {

        @Override
	public int compare(String a1, String a2) {
            String a1Name = a1.toUpperCase();
            String a2Name = a2.toUpperCase();
            return a1Name.compareTo(a2Name);
        }
    };
        
    public static Comparator<String> TileNumComparator = new Comparator<String>() {

        @Override
	public int compare(String a1, String a2) {
            if (a1.contains("?"))
                return -1;
            if (a2.contains("?"))
                return 1;
            if (a1.contains("?") && a2.contains("?"))
                return 0;
            Integer a1tiles = Integer.parseInt(a1);
            Integer a2tiles = Integer.parseInt(a2);;
            return a1tiles.compareTo(a2tiles);
        }
    };
    
    protected Color getFillColor(boolean showRelZPos) {
        Color color=COLOR_UNSELECTED_AREA;
        if (acquiring) {
            color=COLOR_ACQUIRING_AREA;
        } else {
            if (showRelZPos) {
                if (Math.round(relPosZ)==0) {
                    color=new Color(128,128,128);
                } else if (Math.round(relPosZ) < 0) {
                    color=new Color(64,64,64);
                } else if (Math.round(relPosZ) > 0) {
                    color=new Color(192,192,192);
                }
            }    
        }
        return color;
    }

    protected Color getBorderColor() {
        if (selectedForMerge)
            return COLOR_MERGE_AREA_BORDER;
        else {
            if (selectedForAcq)
                return COLOR_SELECTED_AREA_BORDER;
            else    
                return COLOR_AREA_BORDER;
        }   
    }
    /* 
    Used to convert JSONObject to Area
    derived classes should overwrite this to save fields that are not part of Area class
    */
    abstract protected void initializeFromJSONObject(JSONObject obj) throws JSONException;

    /* 
    Used to create a JSONObject representation of Area
    derived classes should overwrite this to save fields that are not part of Area class
    */
    abstract protected void addFieldsToJSONObject(JSONObject obj) throws JSONException;

    public abstract String getShape();
    
    public abstract List<Point2D> getOutlinePoints();
        
//    public abstract void calculateCenterPos();
    
//    public abstract void calculateDefaultPos();
    
    public abstract void calcCenterAndDefaultPos();
    
    public abstract void drawArea(Graphics2D g2d, int bdPix, double physToPixelRatio, boolean showZProfile);
    
    public abstract void drawTiles(Graphics2D g2d, int bdPix, double physToPixelRatio, double fovX, double fovY, TilingSetting setting);
    
    public abstract boolean isInArea(double x, double y);

    public abstract boolean isFovInsideArea(double centerX, double centerY, double fovWidth, double fovHeight);
    
    public abstract boolean doesFovTouchArea(double centerX, double centerY, double fovWidth, double fovHeight);

    public abstract boolean isInsideRect(Rectangle2D r);
    
    public abstract Area duplicate();
    
    public abstract Area showConfigDialog(Rectangle2D bounds);
    
    public abstract int supportedLayouts();
        
      
}
