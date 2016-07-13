package autoimage.data.layout;

import org.json.JSONException;
import org.json.JSONObject;


/**
 *
 * @author Karsten Siller
 */
public class TilingSetting {

    public enum Mode {FULL, CENTER, RANDOM, ADAPTIVE, FILE}
    
    private static Object[] tilingModeOptions = new Object[]{
        Mode.FULL,
        Mode.CENTER,
        Mode.RANDOM,
        Mode.ADAPTIVE};
    //Mode.File not supported yet

    private final Mode mode;
    private final String tileCoordFName; //abspath to file with tilingcoords
    //private final AcqSetting tileCoordSource; //used for runtime tile coord calculation
    private final boolean cluster;
    private final int nrClusterTileX;// irrelevant if Full tiling
    private final int nrClusterTileY;// irrelevant if Full tiling
    private final boolean siteOverlap;
    private final int maxSites;
    private final boolean insideOnly;
    private final double overlap;
    private final int direction;
    
    public static final int DR_TILING = 0;
    public static final int UR_TILING = 1;//TILING_UP;//2
    public static final int DL_TILING = 2;//TILING_LEFT;//4
    public static final int UL_TILING = 3;//TILING_UP+TILING_LEFT;//6
    public static final int RD_TILING = 4;//TILING_HORIZONTAL_FIRST;//1
    public static final int LD_TILING = 5;//TILING_HORIZONTAL_FIRST+TILING_LEFT;//5
    public static final int RU_TILING = 6;//TILING_HORIZONTAL_FIRST+TILING_UP;//3
    public static final int LU_TILING = 7;//TILING_HORIZONTAL_FIRST+TILING_UP+TILING_LEFT;//7
    
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
    

    
    private TilingSetting(Mode m, String tileFileName, boolean c, int nrCX, int nrCY, boolean sOverlap, int mSites, boolean iOnly, double tOverlap, int tileDir) {
        mode=m;
        tileCoordFName=tileFileName;
        cluster=c;
        nrClusterTileX=nrCX;
        nrClusterTileY=nrCY;
        siteOverlap=sOverlap;
        maxSites=mSites;
        insideOnly=iOnly;
        overlap=tOverlap;
        direction=tileDir;
    }
      
    public Builder copy() {
        Builder builder = new Builder()
                .mode(this.mode)
                .tileCoordFilename(this.tileCoordFName)
                .cluster(this.cluster)
                .clusterGrid(this.nrClusterTileX,this.nrClusterTileY)
                .siteOverlap(this.siteOverlap)
                .maxSites(this.maxSites)
                .insideOnly(this.insideOnly)
                .tileOverlap(this.overlap)
                .direction(this.direction);
        return builder;
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

    
    /**
     * Factory to create TileSetting objects
     */
    public static class Builder {
        private Mode mode=Mode.FULL;
        private String tileCoordFName="";
        private boolean cluster=true;
        private int nrClusterTileX=1;
        private int nrClusterTileY=1;
        private boolean siteOverlap=true;
        private int maxSites=1;
        private boolean insideOnly=false;
        private double overlap=0;
        private int direction=DR_TILING;        
    

        public Builder() {
        }
        
        public Builder(JSONObject obj) throws JSONException, IllegalArgumentException {
            String jsonExceptionStr="";
//            boolean illegalArgument=false;
            if (obj!=null) {
                String m;
                try {
                    m = obj.getString(TAG_MODE);
                    mode=Mode.valueOf(m);
                } catch (JSONException ex) {
                    jsonExceptionStr+=TAG_MODE+",";
                }
                try {
                    tileCoordFName=obj.getString(TAG_ROI_SOURCE_FILE);
                } catch (JSONException ex) {
                    jsonExceptionStr+=TAG_ROI_SOURCE_FILE+",";
                }
                try {
                    cluster=obj.getBoolean(TAG_CLUSTER);
                } catch (JSONException ex) {
                    jsonExceptionStr+=TAG_MODE+",";
                }
                try {
                    nrClusterTileX=obj.getInt(TAG_NR_CLUSTER_TILE_X);
                } catch (JSONException ex) {
                    jsonExceptionStr+=TAG_MODE+",";
                }
                try {
                    nrClusterTileY=obj.getInt(TAG_NR_CLUSTER_TILE_Y);
                } catch (JSONException ex) {
                    jsonExceptionStr+=TAG_MODE+",";
                }
                try {
                    siteOverlap=obj.getBoolean(TAG_SITE_OVERLAP);
                } catch (JSONException ex) {
                    jsonExceptionStr+=TAG_MODE+",";
                }
                try {
                    maxSites=obj.getInt(TAG_MAX_SITES);
                } catch (JSONException ex) {
                    jsonExceptionStr+=TAG_MODE+",";
                }
                try {
                    insideOnly=obj.getBoolean(TAG_INSIDE_ONLY);
                } catch (JSONException ex) {
                    jsonExceptionStr+=TAG_MODE+",";
                }
                try {
                    overlap=obj.getDouble(TAG_OVERLAP);
                } catch (JSONException ex) {
                    jsonExceptionStr+=TAG_MODE+",";
                }
                try {
                    direction=obj.getInt(TAG_DIRECTION);
                } catch (JSONException ex) {
                    jsonExceptionStr+=TAG_MODE+",";
                }
                if (!"".equals(jsonExceptionStr)) {
                    throw new JSONException(jsonExceptionStr);
                }
            } else {
                throw new IllegalArgumentException();
            }
        }        
    
        public Builder mode(Mode m) {
            mode=m;
            return this;
        }
        
        public Builder tileCoordFilename(String fn) {
            tileCoordFName=fn;
            return this;
        }
        
        public TilingSetting build() {
            TilingSetting ts=new TilingSetting(
                    mode,
                    tileCoordFName,
                    cluster, 
                    nrClusterTileX, 
                    nrClusterTileY, 
                    siteOverlap, 
                    maxSites, 
                    insideOnly, 
                    overlap, 
                    direction);
            return ts;
        }

        public Builder cluster(boolean cluster) {
            this.cluster=cluster;
            return this;
        }

        public Builder clusterGrid(int nrClusterTileX, int nrClusterTileY) {
            this.nrClusterTileX=nrClusterTileX;
            this.nrClusterTileY=nrClusterTileY;
            return this;
        }

        public Builder siteOverlap(boolean siteOverlap) {
            this.siteOverlap=siteOverlap;
            return this;
        }

        public Builder maxSites(int maxSites) {
            this.maxSites=maxSites;
            return this;
        }

        public Builder insideOnly(boolean insideOnly) {
            this.insideOnly=insideOnly;
            return this;
        }

        public Builder tileOverlap(double overlap) {
            this.overlap=overlap;
            return this;
        }

        public Builder direction(int direction) {
            this.direction=direction;
            return this;
        }
    }

}
