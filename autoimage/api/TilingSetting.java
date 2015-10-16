package autoimage.api;

import org.json.JSONException;
import org.json.JSONObject;


/**
 *
 * @author Karsten Siller
 */
public class TilingSetting {

    public enum Mode {FULL, CENTER, RANDOM, ADAPTIVE, FILE}
    Mode mode;
    private static Object[] tilingModeOptions = new Object[]{
        Mode.FULL,
        Mode.CENTER,
        Mode.RANDOM,
        Mode.ADAPTIVE};
    //Mode.File not supported yet
    
    String tileCoordFName; //abspath to file with tilingcoords
    AcqSetting tileCoordSource; //used for runtime tile coord calculation
    boolean cluster;
    int nrClusterTileX;// irrelevant if Full tiling
    int nrClusterTileY;// irrelevant if Full tiling
    boolean siteOverlap;
    int maxSites;
    boolean insideOnly;
    double overlap;
    int direction;
    
    public static final byte DR_TILING = 0;
    public static final byte UR_TILING = 1;//TILING_UP;//2
    public static final byte DL_TILING = 2;//TILING_LEFT;//4
    public static final byte UL_TILING = 3;//TILING_UP+TILING_LEFT;//6
    public static final byte RD_TILING = 4;//TILING_HORIZONTAL_FIRST;//1
    public static final byte LD_TILING = 5;//TILING_HORIZONTAL_FIRST+TILING_LEFT;//5
    public static final byte RU_TILING = 6;//TILING_HORIZONTAL_FIRST+TILING_UP;//3
    public static final byte LU_TILING = 7;//TILING_HORIZONTAL_FIRST+TILING_UP+TILING_LEFT;//7
    
    public static final String TAG_TILING = "TILING";   
    public static final String TAG_MODE = "MODE";
    public static final String TAG_ROI_SOURCE_RUNTIME = "ROI_SOURCE_RUNTIME";
    public static final String TAG_ROI_SOURCE_FILE = "ROI_SOURCE_FILE";
    public static final String TAG_CLUSTER = "CLUSTER";
    public static final String TAG_NR_CLUSTER_TILE_X = "NR_CLUSTER_TILE_X";
    public static final String TAG_NR_CLUSTER_TILE_Y = "NR_CLUSTER_TILE_Y";
    public static final String TAG_SITE_OVERLAP = "SITE_OVERLAP";
    public static final String TAG_MAX_SITES = "MAX_SITES";
    public static final String TAG_INSIDE_ONLY = "INSIDE_ONLY";
    public static final String TAG_OVERLAP = "OVERLAP";
    public static final String TAG_DIRECTION = "DIRECTION";
    

    
    public TilingSetting(Mode m, boolean c, int nrCX, int nrCY, boolean sOverlap, int mSites, boolean iOnly, double tOverlap, byte tileDir) {
        mode=m;
        tileCoordFName="";
        cluster=c;
        nrClusterTileX=nrCX;
        nrClusterTileY=nrCY;
        siteOverlap=sOverlap;
        maxSites=mSites;
        insideOnly=iOnly;
        overlap=tOverlap;
        direction=tileDir;
    }
    
    public TilingSetting() {
        mode=Mode.FULL;
        tileCoordFName="";
        cluster=true;
        nrClusterTileX=1;
        nrClusterTileY=1;
        siteOverlap=true;
        maxSites=1;
        insideOnly=false;
        overlap=0;
        direction=DR_TILING;        
    }
    
    public TilingSetting(JSONObject obj) throws JSONException, IllegalArgumentException {
        if (obj!=null) {
            String m=obj.getString(TAG_MODE);
            mode=Mode.valueOf(m);
/*            if (m.equals(Mode.CENTER.toString()))
                mode=Mode.CENTER;
            else if (m.equals(Mode.FULL.toString()))
                mode=Mode.FULL;
            else if (m.equals(Mode.RANDOM.toString()))
                mode=Mode.RANDOM;
            else if (m.equals(Mode.ADAPTIVE.toString()))
                mode=Mode.ADAPTIVE;
            else if (m.equals(Mode.FILE.toString()))
                mode=Mode.FILE;*/
            tileCoordFName=obj.getString(TAG_ROI_SOURCE_FILE);
            cluster=obj.getBoolean(TAG_CLUSTER);
            nrClusterTileX=obj.getInt(TAG_NR_CLUSTER_TILE_X);
            nrClusterTileY=obj.getInt(TAG_NR_CLUSTER_TILE_Y);
            siteOverlap=obj.getBoolean(TAG_SITE_OVERLAP);
            maxSites=obj.getInt(TAG_MAX_SITES);
            insideOnly=obj.getBoolean(TAG_INSIDE_ONLY);
            overlap=obj.getDouble(TAG_OVERLAP);
            direction=obj.getInt(TAG_DIRECTION);
        }
    }
    
    public static Object[] getTilingModes() {
        return tilingModeOptions;
    }
    
    public JSONObject toJSONObject() throws JSONException {
        JSONObject obj=new JSONObject();
        obj.put(TAG_MODE,mode.toString());
        obj.put(TAG_ROI_SOURCE_FILE,tileCoordFName);
        obj.put(TAG_CLUSTER, cluster);
        obj.put(TAG_NR_CLUSTER_TILE_X,nrClusterTileX);
        obj.put(TAG_NR_CLUSTER_TILE_Y,nrClusterTileY);
        obj.put(TAG_SITE_OVERLAP, siteOverlap);
        obj.put(TAG_MAX_SITES, maxSites);
        obj.put(TAG_INSIDE_ONLY, insideOnly);
        obj.put(TAG_OVERLAP,overlap);
        obj.put(TAG_DIRECTION, direction);
        return obj;
    }
    
    public Mode getMode() {
        return mode;
    }
    
    public double getTileOverlap() {
        return overlap;
    }
    
    public boolean isInsideOnly() {
        return insideOnly;
    }
    
    public boolean isCluster() {
        return cluster;
    }
    
    public int getNrClusterTileX() {
        return nrClusterTileX;
    }
    
    public int getNrClusterTileY() {
        return nrClusterTileY;
    }
    
    public boolean isSiteOverlap() {
        return siteOverlap;
    }
    
    public int getMaxSites() {
        return maxSites;
    }
    
    public int getTilingDirection() {
        return direction;
    }

    public TilingSetting duplicate() {
        TilingSetting ts = new TilingSetting();
        ts.mode=this.mode;
        ts.tileCoordFName=this.tileCoordFName;
        ts.cluster=this.cluster;
        ts.nrClusterTileX=this.nrClusterTileX;
        ts.nrClusterTileY=this.nrClusterTileY;
        ts.siteOverlap=this.siteOverlap;
        ts.maxSites=this.maxSites;
        ts.insideOnly=this.insideOnly;
        ts.overlap=this.overlap;
        ts.direction=this.direction; 
        return ts;
    }
    

}
