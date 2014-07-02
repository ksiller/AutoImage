package autoimage;

import ij.IJ;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    static final String TAG_AREA = "AREA";
    static final String TAG_AREA_ARRAY = "AREA_ARRAY";
    static final String TAG_CLASS = "CLASS";
    static final String TAG_NAME = "NAME";
 //   static final String TAG_SHAPE = "SHAPE";
    static final String TAG_COMMENT = "COMMENT";
    static final String TAG_TOP_LEFT_X = "TOP_LEFT_X";
    static final String TAG_TOP_LEFT_Y = "TOP_LEFT_Y";
    static final String TAG_CENTER_X = "CENTER_X";
    static final String TAG_CENTER_Y = "CENTER_Y";
    static final String TAG_WIDTH = "WIDTH";
    static final String TAG_HEIGHT = "HEIGHT";
    static final String TAG_RING_WIDTH = "RING_WIDTH";
    static final String TAG_REL_POS_Z = "REL_POS_Z";
    static final String TAG_SELECTED = "SELECTED";

    static final String TAG_TILE = "TILE";
    static final String TAG_CLUSTER = "CLUSTER";
    
    protected String name;
//    protected static String shape;
//    protected TilingSetting tiling;
    protected List<Tile> tilePosList;
    protected int tileNumber;
    protected int id;
    protected double topLeftX; //in um
    protected double topLeftY; //in um
    protected double relPosZ; //in um relative to flat layout bottom
    protected double width; //in um
    protected double height; //in um
    protected boolean selectedForAcq;
    protected boolean selectedForMerge;
    protected String comment;
    protected boolean acquiring;
//    private boolean abortTileCalc;
    private boolean unknownTileNum; //is set when tilingmode is "runtime" or "file", or pixel size is not calibrated
    
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
    public static final long DELAY_TO_CANCEL = 1000;

           
    public Area() {
        this("NewArea", -1, 0,0,0,0,0,false,"");
    }
    
    public Area(String n) { //expects name identifier
        this(n, -1, 0,0,0,0,0,false,"");
    }
    
    public Area(String n, int id, double ox, double oy, double oz, double w, double h, boolean s, String anot) {
        name=n;
        this.id=id;
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
    }

/*    
    public String getClassName() {
        return this.getClass().getName();
    }
*/
    
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
    
    private boolean createCluster(int clusterNr,double seedX, double seedY, double fovX, double fovY, TilingSetting setting ) {
        double tileOverlap=setting.getTileOverlap();
        int tilingDir=setting.getTilingDirection();
        double tileOffsetX=(1-tileOverlap)*fovX;
        double tileOffsetY=(1-tileOverlap)*fovY;
        int xTiles=setting.getNrClusterTileX();
        int yTiles=setting.getNrClusterTileY();
        double clusterW=fovX+(xTiles-1)*tileOffsetX;
        double clusterH=fovY+(yTiles-1)*tileOffsetY;
        double startx=seedX-(clusterW/2)+fovX/2;//*physToPixelRatio;
        double starty=seedY-(clusterH/2)+fovY/2;//*physToPixelRatio;

        if (!setting.isClusterOverlap()) {
            for (Tile t:tilePosList) {
                Rectangle2D.Double tRect=new Rectangle2D.Double(t.centerX-fovX/2, t.centerY-fovY/2,fovX,fovY);
                if (tRect.intersects(startx-fovX/2, starty-fovY/2, clusterW, clusterH))
                    return false;
            }
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
                x = startx+tileOffsetX*col;
                row=vDir == 1 ? 0 : yTiles-1;
                for (int j=0; j<yTiles; j++) {   
                    y = starty+tileOffsetY*row;
    //                IJ.log(name+", test Tile: "+Integer.toString(i*yTiles+j)+", "+Double.toString(x)+","+Double.toString(y));
                    siteCounter++;
                    Tile t=new Tile(createTileName(clusterNr,siteCounter), x, y, relPosZ);                
                    tilePosList.add(t);
                    row+=vDir;
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
                y = starty+tileOffsetY*row;
                col=hDir == 1 ? 0 : xTiles-1;
                for (int i=0; i<xTiles; i++) {  
                    x = startx+tileOffsetX*col;
  //                  IJ.log(name+", test Tile: "+Integer.toString(i*yTiles+j)+", "+Double.toString(x)+","+Double.toString(y));
                    siteCounter++;
                    Tile t=new Tile(createTileName(clusterNr,siteCounter), x, y, relPosZ);                
                    tilePosList.add(t);
   //                 IJ.log(name+", "+Integer.toString(i*yTiles+j)+", "+Double.toString(x)+","+Double.toString(y));
                    col+=hDir;
                }
                row+=vDir;
                hDir=-hDir;
            }            
        }
  //      IJ.log("Area.creatCluster: end"); 
        return true;
    }
    
    public int calcFullTilePositions(double fovX, double fovY, TilingSetting setting) throws InterruptedException {
        double tileOverlap=setting.getTileOverlap();
        boolean insideOnly=setting.isInsideOnly();
        int tilingDir=setting.getTilingDirection();
        double tileOffsetX=(1-tileOverlap)*fovX;
        double tileOffsetY=(1-tileOverlap)*fovY;
        int xTiles;
        int yTiles;
        double startx;
        double starty;
        
        if (insideOnly) {
            xTiles=(int)Math.floor((width-fovX)/tileOffsetX)+1;
            yTiles=(int)Math.floor((height-fovY)/tileOffsetY)+1;
        } else {
//            xTiles=(int)Math.ceil(width/tileOffsetX);
//            yTiles=(int)Math.ceil(height/tileOffsetY);
            xTiles=(int)Math.ceil((width-fovX)/tileOffsetX)+1;
            yTiles=(int)Math.ceil((height-fovY)/tileOffsetY)+1;
        }
        startx=fovX/2+(topLeftX-((fovX+(xTiles-1)*tileOffsetX-width)/2));//*physToPixelRatio;
        starty=fovY/2+(topLeftY-((fovY+(yTiles-1)*tileOffsetY-height)/2));//*physToPixelRatio;
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
                x = startx+tileOffsetX*col;
                row=vDir == 1 ? 0 : yTiles-1;
                for (int j=0; j<yTiles; j++) {   
                    y = starty+tileOffsetY*row;
//                    IJ.log(name+", test Tile: "+Integer.toString(i*yTiles+j)+", "+Double.toString(x)+","+Double.toString(y));
                    if (acceptTilePos(x,y,fovX,fovY,insideOnly)) {
                        siteCounter++;
                        Tile t=new Tile("Site"+paddedTileIndex(siteCounter), x, y, relPosZ);                
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
                y = starty+tileOffsetY*row;
                col=hDir == 1 ? 0 : xTiles-1;
                for (int i=0; i<xTiles; i++) {  
                    x = startx+tileOffsetX*col;
 //                   IJ.log(name+", test Tile: "+Integer.toString(i*yTiles+j)+", "+Double.toString(x)+","+Double.toString(y));
                    if (acceptTilePos(x,y,fovX,fovY,insideOnly)) {
                        siteCounter++;
                        Tile t=new Tile("Site"+paddedTileIndex(siteCounter), x, y, relPosZ);                
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
        return tilePosList.size();
    }

    private int calcRandomTilePositions(double fovX, double fovY, TilingSetting setting) throws InterruptedException {
        int size=setting.getMaxSites();
        if (setting.isCluster())
            size=size*setting.getNrClusterTileX()*setting.getNrClusterTileY();
        tilePosList = new ArrayList<Tile>(size);
        int nr=1;
        while (!Thread.currentThread().isInterrupted() && nr <= setting.getMaxSites()) {// && !abortTileCalc) {
            double w;
            double h;
            if (setting.isCluster() && (setting.getNrClusterTileX()>1 || setting.getNrClusterTileY()>1)) {
                double offsetX=fovX*(1-setting.getTileOverlap());
                double offsetY=fovY*(1-setting.getTileOverlap());
                w=fovX+offsetX*(setting.getNrClusterTileX()-1);
                h=fovY+offsetY*(setting.getNrClusterTileY()-1);
            } else {
                w=fovX;
                h=fovY;
            }
            double x=topLeftX+Math.random()*width;
            double y=topLeftY+Math.random()*height;
//            IJ.log("Area.calcRandomPositions: "+nr+", "+x+", "+y+", "+w+", "+h);
            if (acceptTilePos(x,y,w,h,setting.isInsideOnly())) {
                if (setting.isCluster() && (setting.getNrClusterTileX()>1 || setting.getNrClusterTileY()>1)) {
                    if (createCluster(nr,x,y,fovX,fovY, setting)) {
                        nr++;
                    }
                } else {
                    tilePosList.add(new Tile("Site"+paddedTileIndex(nr), x, y, relPosZ));
                    nr++;
                }
            }

        }
        unknownTileNum=Thread.currentThread().isInterrupted();
        return tilePosList.size();
    }
    
    private int calcCenterTilePositions(double fovX, double fovY, TilingSetting setting) throws InterruptedException {
        int size=1;
        if (setting.isCluster())
            size=size*setting.getNrClusterTileX()*setting.getNrClusterTileY();
//        tilePosList.clear();
        tilePosList = new ArrayList<Tile>(size);
        double w;
        double h;
        if (setting.isCluster() && (setting.getNrClusterTileX()>1 || setting.getNrClusterTileY()>1)) {
            double offsetX=fovX*(1-setting.getTileOverlap());
            double offsetY=fovY*(1-setting.getTileOverlap());
            w=fovX+offsetX*(setting.getNrClusterTileX()-1);
            h=fovY+offsetY*(setting.getNrClusterTileY()-1);
        } else {
            w=fovX;
            h=fovY;
        }
        double x=getCenterX();
        double y=getCenterY();
        if (acceptTilePos(x,y,w,h,setting.isInsideOnly())) {
            if (setting.isCluster() && (setting.getNrClusterTileX()>1 || setting.getNrClusterTileY()>1))
                createCluster(1,x,y,fovX,fovY, setting);
            else
                tilePosList.add(new Tile("Site"+paddedTileIndex(1), x, y, relPosZ));
        }
        unknownTileNum=Thread.currentThread().isInterrupted();
        return tilePosList.size();
    }
    
        
    //NEED TO SET RELZPOS
    private int calcRuntimeTilePositions(RuntimeTileManager tileManager,double fovX, double fovY, TilingSetting setting) throws InterruptedException {
        IJ.log("Area.calcRuntimeTilePosition: "+name);
        if (tileManager==null || tileManager.getROIs().isEmpty()) {
            IJ.log("    Area.calcRuntimeTilePosition: tileManager==null or no ROIs");
            
            unknownTileNum=true;
            tilePosList.clear();
            return 0;
        }
        Map<String,List<Tile>> roiMap=tileManager.getROIs();
        if (roiMap.containsKey(name)) {
            IJ.log("    Area.calcRuntimeTilePosition, "+name+", ROIs: "+roiMap.get(name).size());
            List<Tile> seedList=(List<Tile>)roiMap.get(name);
            int size=Math.max(seedList.size(),setting.getMaxSites());
            if (setting.isCluster())
                size=size*setting.getNrClusterTileX()*setting.getNrClusterTileY();
            tilePosList = new ArrayList<Tile>(size);
            int nr=1;
            int acceptedNr=0;
            while (!Thread.currentThread().isInterrupted() && nr<seedList.size() && acceptedNr <= setting.getMaxSites()) {// && !abortTileCalc) {
                double w;
                double h;
                if (setting.isCluster() && (setting.getNrClusterTileX()>1 || setting.getNrClusterTileY()>1)) {
                    double offsetX=fovX*(1-setting.getTileOverlap());
                    double offsetY=fovY*(1-setting.getTileOverlap());
                    w=fovX+offsetX*(setting.getNrClusterTileX()-1);
                    h=fovY+offsetY*(setting.getNrClusterTileY()-1);
                } else {
                    w=fovX;
                    h=fovY;
                }
                double x=seedList.get(nr).centerX;
                double y=seedList.get(nr).centerY;
                IJ.log("Area.calcRuntimePositions: "+name+", "+nr+", "+x+", "+y+", "+w+", "+h);
                if (acceptTilePos(x,y,w,h,setting.isInsideOnly())) {
                    if (setting.isCluster() && (setting.getNrClusterTileX()>1 || setting.getNrClusterTileY()>1)) {
                        if (createCluster(nr,x,y,fovX,fovY, setting)) {
                       //     nr++;
                        }
                    } else {
                        tilePosList.add(new Tile("Site"+paddedTileIndex(nr), x, y, relPosZ));
                     //   nr++;
                    }
                    acceptedNr++;
                }
                nr++;
            }
        } else {
            tilePosList.clear();
        }   
        unknownTileNum=Thread.currentThread().isInterrupted();
        return tilePosList.size();
    }

    public synchronized int calcTilePositions(RuntimeTileManager tileManager,double fovX, double fovY, TilingSetting setting) throws InterruptedException {
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
    
    public synchronized void draw(Graphics2D g2d, int bdPix, double physToPixelRatio, double fovX, double fovY, TilingSetting setting, boolean drawTiles) {
        drawArea(g2d, bdPix, physToPixelRatio);
        if (selectedForAcq && drawTiles && setting!=null) {
            if (tilePosList!=null) {
                drawTiles(g2d, bdPix, physToPixelRatio, fovX, fovY, setting);
            }    
        }
    }
    
 /*
    private void drawTileByTile(Graphics2D g2d, int bdPix, double physToPixelRatio, double fovX, double fovY, TilingSetting setting) {
        if (tilePosList!=null) {
            g2d.setColor(COLOR_TILE_GRID);
            for (int i=0; i<tilePosList.size(); i++) {
                Tile t=tilePosList.get(i);
                double tileOverlap=setting.getTileOverlap();
                double sizeX=Math.min(fovX*(1-tileOverlap), fovX);
                double sizeY=Math.min(fovY*(1-tileOverlap), fovY);
//                int x=bdPix+(int)Math.round((t.getCenterX()-fovX/2)*physToPixelRatio);
//                int y=bdPix+(int)Math.round((t.getCenterY()-fovY/2)*physToPixelRatio);
//                int w=(int)Math.round(fovX*physToPixelRatio);
//                int h=(int)Math.round(fovY*physToPixelRatio);
                int x=bdPix+(int)Math.round((t.centerX-sizeX/2)*physToPixelRatio);
                int y=bdPix+(int)Math.round((t.centerY-sizeY/2)*physToPixelRatio);
                int w=(int)Math.round(sizeX*physToPixelRatio);
                int h=(int)Math.round(sizeY*physToPixelRatio);
                g2d.drawRect(x,y,w,h);
            }    
        }
    }
    */
    public void drawTileByTileOvl(Graphics2D g2d, int bdPix, double physToPixelRatio, double fovX, double fovY, TilingSetting setting) { /*AcqSetting setting) {*/
        if (tilePosList!=null) {
            g2d.setColor(COLOR_TILE_GRID);
                Composite oldComposite=g2d.getComposite();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.25f));
            for (int i=0; i<tilePosList.size(); i++) {
                Tile t=tilePosList.get(i);
                int x=bdPix+(int)Math.round((t.centerX-fovX/2)*physToPixelRatio);
                int y=bdPix+(int)Math.round((t.centerY-fovY/2)*physToPixelRatio);
                int w=(int)Math.round(fovX*physToPixelRatio);
                int h=(int)Math.round(fovY*physToPixelRatio);
                g2d.fillRect(x,y,w,h);
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
    
    public void setTopLeftX(double value) {
        topLeftX=value;
    }
    
    public double getTopLeftY() {
        return topLeftY;
    } 
    
    public void setTopLeftY(double value) {
        topLeftY=value;
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
    }
    
    public double getHeight() {
        return height;
    } 
    
    public void setHeight(double value) {
        height=value;
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
            info.put("Area",name);
            if (tileName.indexOf("Cluster")!=-1) {
                int startIndex=tileName.indexOf("Cluster")+7;
                int endIndex=tileName.indexOf("Site")-1;
                info.put("ClusterIndex", Long.parseLong(tileName.substring(startIndex,endIndex)));
            } else
                info.put("ClusterIndex", "-1");
            int startIndex=tileName.indexOf("Site")+4;
            info.put("SiteIndex", Long.parseLong(tileName.substring(startIndex)));
            info.put("Comment", comment);
        } catch (JSONException ex) {
            Logger.getLogger(Area.class.getName()).log(Level.SEVERE, null, ex);
        }
        return info;
    }
    
    public PositionList addTilePositions(PositionList pl, ArrayList<JSONObject> posInfoL, String xyStageLabel, String zStageLabel, ArrayList<RefArea> rpList, Vec3d normVec) {
        if (rpList==null | rpList.size()<1)
            return null;
        RefArea rp=rpList.get(0);
        double sOffsetX=rp.convertLayoutCoordToStageCoord_X(0);
        double sOffsetY=rp.convertLayoutCoordToStageCoord_Y(0);
        double sOffsetZ=rp.getStageCoordZ()-rpList.get(0).getLayoutCoordZ();
        
        if (pl==null)
            pl=new PositionList();
/*        double x0=rp.getStageCoordX();
        double y0=rp.getStageCoordY();
        double z0=rp.getStageCoordZ()-rp.getLayoutCoordZ();
        double a=normVec.x;
        double b=normVec.y;
        double c=normVec.z;
*/      
        for (Tile tile:tilePosList) {
        //for (int i=0; i<tilePosList.size(); i++) {
        //    Tile t=tilePosList.get(i);
            double x=rp.convertLayoutCoordToStageCoord_X(tile.centerX);
            double y=rp.convertLayoutCoordToStageCoord_Y(tile.centerY);
            double z=((-normVec.x*(x-rp.getStageCoordX())-normVec.y*(y-rp.getStageCoordY()))/normVec.z)+rp.getStageCoordZ()-rp.getLayoutCoordZ()+relPosZ;
            
            MultiStagePosition msp=new MultiStagePosition(xyStageLabel, x, y, zStageLabel, z);
            msp.setLabel(name+"-"+tile.name);
            pl.addPosition(msp);
            if (posInfoL!=null)
                posInfoL.add(createSiteInfo(tile.name));
        }
        return pl;
    }
    
    public List<Tile> getTilePositions() {
        return tilePosList;
    }
    
    //public abstract String[] getXMLTags();
    
    public boolean setAreaParams (List<String> params) {
        if (params!=null && params.size()>=1 && params.get(0).equals(this.getName())) 
            return true;
        else 
            return false;
    }

    public void setAreaParams (Map<String,String> params) {
        if (params!=null) {
/*            String s=params.get(TAG_SHAPE);
            if (s!=null)
                shape=s;*/
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
    

    private static String paddedTileIndex(int index) {
        return String.format(TILE_NAME_PADDING, index);
    }
    
   
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
                 
    public boolean acceptTilePos(double x, double y, double fovX, double fovY, boolean insideOnly) {
        if (insideOnly)
            return isFovInsideArea(x,y,fovX,fovY);
        else
            return doesFovTouchArea(x,y,fovX,fovY);
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject obj=new JSONObject();
        obj.put(TAG_CLASS,this.getClass().getName());
        obj.put(TAG_NAME,name);
        obj.put(TAG_WIDTH,width);
        obj.put(TAG_HEIGHT,height);
        obj.put(TAG_TOP_LEFT_X,topLeftX);
        obj.put(TAG_TOP_LEFT_Y,topLeftY);
        obj.put(TAG_REL_POS_Z,relPosZ);
        obj.put(TAG_SELECTED,selectedForAcq);
        obj.put(TAG_COMMENT,comment);
        return obj;
    }
    
    public void initializeFromJSONObject(JSONObject obj) throws JSONException {
        name=obj.getString(TAG_NAME);
        width=obj.getDouble(TAG_WIDTH);
        height=obj.getDouble(TAG_HEIGHT);
        topLeftX=obj.getDouble(TAG_TOP_LEFT_X);
        topLeftY=obj.getDouble(TAG_TOP_LEFT_Y);
        relPosZ=obj.getDouble(TAG_REL_POS_Z);
        selectedForAcq=obj.getBoolean(TAG_SELECTED);
        comment=obj.getString(TAG_COMMENT);
    }
    
    public static Area createFromJSONObject(JSONObject obj) throws JSONException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        String className=obj.getString(TAG_CLASS);
        Class clazz=Class.forName(className);
        Area area=(Area) clazz.newInstance();
        area.initializeFromJSONObject(obj);
        return area;
    }
    
    public abstract String getShape();
    
    public abstract double getCenterX();
    
    public abstract double getCenterY();
    
    public abstract void drawArea(Graphics2D g2d, int bdPix, double physToPixelRatio);
    
    public abstract void drawTiles(Graphics2D g2d, int bdPix, double physToPixelRatio, double fovX, double fovY, TilingSetting setting);
    
    public abstract boolean isInArea(double x, double y);

    public abstract boolean isFovInsideArea(double x, double y, double fovX, double fovY);
    
    public abstract boolean doesFovTouchArea(double x, double y, double fovX, double fovY);

    public abstract boolean isInsideRect(Rectangle2D.Double r);
    
    public abstract Area duplicate();
      
}
