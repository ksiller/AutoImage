/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import ij.IJ;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten
 */
public class AcqSetting {
    public static final String[] ACQ_ORDER_LIST = {"Time, Position, Z, Channel",
                                            "Time, Position, Channel, Z",
                                            "Position, Time, Z, Channel",
                                            "Position, Time, Channel, Z"};
    public static final int MAX_HOUR_INT = 999;
    public static final int MAX_TILE_OVERLAP = 50;
    public static final int MAX_MAG_FACTOR = 16;
    public static final int MAX_CLUSTER_X = 10;
    public static final int MAX_CLUSTER_Y = 10;
    public static final int MAX_SITES = 100000;
    public static final int FILE_IO_OK = 0;
    
    public static final String TAG_ACQ_SETTING = "ACQUISITION_SETTING";
    public static final String TAG_ACQ_SETTING_ARRAY = "ACQ_SETTING_ARRAY";
    public static final String TAG_VERSION="VERSION";
    public static final String TAG_SETTING="SETTING";
    public static final String TAG_NAME = "NAME";
    public static final String TAG_OBJ_GROUP_STR = "OBJECTIVE_GROUP_STR";
    public static final String TAG_CHANNEL_GROUP_STR = "CHANNEL_GROUP_STR";
    
    public static final String TAG_OBJ_LABEL = "OBJECTIVE_LABEL";
    public static final String TAG_BINNING = "BINNING";
    public static final String TAG_FIELD_OF_VIEW = "FIELD_OF_VIEW";
//    public static final String TAG_DETECTOR = "DETECTOR";
    public static final String TAG_AUTOFOCUS = "AUTOFOCUS";
    public static final String TAG_Z_STACK = "Z_STACK";
    public static final String TAG_Z_STACK_CENTERED = "Z_STACK_CENTERED";
    public static final String TAG_Z_BEGIN = "Z_BEGIN";
    public static final String TAG_Z_END = "Z_END";
    public static final String TAG_Z_STEP_SIZE = "Z_STEP_SIZE";
    public static final String TAG_SLICES = "SLICES";
    public static final String TAG_TIMELAPSE = "TIMELAPSE";
    public static final String TAG_HOUR_INTERVAL = "HOUR_INTERVAL";
    public static final String TAG_MINUTE_INTERVAL = "MINUTE_INTERVAL";
    public static final String TAG_SECOND_INTERVAL = "SECOND_INTERVAL";
    public static final String TAG_FRAMES = "FRAMES";
    public static final String TAG_ACQ_ORDER = "ACQ_ORDER";
    public static final String TAG_IMAGE_PROCESSOR_TREE = "IMAGE_PROCESSOR_TREE";
    public static final String TAG_FILE_PROCESSOR_TREE = "FILE_PROCESSOR_TREE";
    public static final String TAG_START_TIME = "START_TIME";
    
    private String name;            //visible in GUI
    private String objectiveDevStr="";
    private String channelGroupStr;
    private String objLabel;        //visible in GUI
    private double objPixSize;      //internal based on existing MM calibration
    private FieldOfView fieldOfView;
    private int binning;            //visible in GUI 
    private TilingSetting tiling;
    //private String path;
    private boolean isModified;
    protected TileManager tileManager;

    private boolean autofocus;      //visible
    private boolean zStack;         //visible
    private boolean zStackCentered; //visible
    private double zBegin;          //visible
    private double zEnd;            //visible
    private double zStepSize;       //visible
    private int slices;             //visible
    private boolean timelapse;      //visible
    private int hours;
    private int minutes;
    private int seconds;
    private long intervalInMS; //visible
    private int frames;             //visible
    private List<Channel> channels; //visible 
    private int acqOrder;           //visible
    private List<Runnable> runnables;
    private DefaultMutableTreeNode imageProcRoot;
    private ScheduledTime startTime;         //visible
    private Calendar absoluteStart;
    private long totalTiles;
    
    
    public static class ScheduledTime {
        protected final static int ASAP = 0;
        protected final static int DELAY = 1;
        protected final static int ABSOLUTE = 2;
        
        private final static String TAG_TYPE = "TYPE";
        private final static String TAG_START_TIME = "START_TIME";
                
        protected int type;
        protected long startTimeMS;
        
        public ScheduledTime (int type, long startMS) {
            this.type=type;
            this.startTimeMS=startMS;
        }

        public ScheduledTime (JSONObject obj) throws JSONException {
            type=obj.getInt(TAG_TYPE);
            startTimeMS=obj.getLong(TAG_START_TIME);
        }

        public JSONObject toJSONObject () throws JSONException {
            JSONObject obj=new JSONObject();
            obj.put(TAG_TYPE, type);
            obj.put(TAG_START_TIME, startTimeMS);
            return obj;
        }
/*        
        public int getType() {
            return type;
        }
        
        public long getStartTimeMS() {
            return startTimeMS;
        }
*/        
        public String toString() {
            String s="";
            switch (type) {
                case AcqSetting.ScheduledTime.ASAP: {
                    s="As soon as possible";
                    break;
                }
                case AcqSetting.ScheduledTime.DELAY: {
                    Calendar cal=Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    cal.setTimeInMillis(startTimeMS);
                    s="+ "
                            + Integer.toString(cal.get(Calendar.HOUR))+"h "
                            + Integer.toString(cal.get(Calendar.MINUTE))+"min "
                            + Integer.toString(cal.get(Calendar.SECOND))+"s ";
                    break;
                }
                case AcqSetting.ScheduledTime.ABSOLUTE: {
                    GregorianCalendar cal=new GregorianCalendar();
//                    cal.clear();
                    cal.setTimeInMillis(startTimeMS);
                    s=cal.getTime().toString();
                    break;
                }
                    
            }
            return s;
        }
    }
    
    
    public AcqSetting(String n) {
        this(n, null,"", -1d);
    }

/*    
    public AcqSetting(String n, int camPixX, int camPixY, String objective, double oPixSize) {
        this(n, camPixX, camPixY, objective, oPixSize, 1, false);
    }
*/
    public AcqSetting(String n, FieldOfView fov, String objective, double oPixSize) {
        this(n, fov, objective, oPixSize, 1, false);
    }

//    public AcqSetting(String n, FieldOfView fov, String objective, double oPixSize, double bin, boolean autof) {
    public AcqSetting(String n, FieldOfView fov, String objective, double oPixSize, int bin, boolean autof) {
        name=n;                     
 //       path="";
        binning=bin;
        objectiveDevStr="";
        
        if (fov == null)
            fieldOfView = new FieldOfView(1,1,FieldOfView.ROTATION_UNKNOWN);
        else
            fieldOfView=fov;
//        setCameraChipSize(detector.getFullDetectorPixelX(), detector.getFullDetectorPixelY());
        setObjective(objective,oPixSize);        
        autofocus=autof;
        channelGroupStr="";
        channels=new ArrayList<Channel>();
        startTime=new ScheduledTime(ScheduledTime.ASAP,0);
        zStack=false;
        zStackCentered=true;
        zBegin=0;
        zEnd=0;
        zStepSize=0;
        slices=1;
        timelapse=false;
        intervalInMS=0;
        frames=1;
        acqOrder=1;
        runnables=new ArrayList<Runnable>();
        imageProcRoot=Utils.createDefaultImageProcTree();
        tiling=new TilingSetting();
        totalTiles=0;
        isModified=false;
        tileManager = new TileManager(null, this);
    }
        
    public AcqSetting (JSONObject obj) throws JSONException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        this("");
        if (obj!=null) {
            name=obj.getString(TAG_NAME);
            objectiveDevStr=obj.getString(TAG_OBJ_GROUP_STR);
            channelGroupStr=obj.getString(TAG_CHANNEL_GROUP_STR);
            binning=obj.getInt(TAG_BINNING);
            try {
                JSONObject fovObj=obj.getJSONObject(TAG_FIELD_OF_VIEW);
                fieldOfView = new FieldOfView(fovObj);
            } catch (JSONException e) {
                IJ.log("Cannot load detector from acquisition settings. Instantiating standard detector");
                fieldOfView = new FieldOfView(1,1,-1);
            }    
            setObjective(obj.getString(TAG_OBJ_LABEL),-1);
            autofocus=obj.getBoolean(TAG_AUTOFOCUS);
            zStack=obj.getBoolean(TAG_Z_STACK);
            zStackCentered=obj.getBoolean(TAG_Z_STACK_CENTERED);
            zBegin=obj.getDouble(TAG_Z_BEGIN);
            zEnd=obj.getDouble(TAG_Z_END);
            zStepSize=obj.getDouble(TAG_Z_STEP_SIZE);
            slices=obj.getInt(TAG_SLICES);                    
            timelapse=obj.getBoolean(TAG_TIMELAPSE);
            hours=obj.getInt(TAG_HOUR_INTERVAL);                    
            minutes=obj.getInt(TAG_MINUTE_INTERVAL);                    
            seconds=obj.getInt(TAG_SECOND_INTERVAL);                    
            intervalInMS=hours*3600000+minutes*60000+seconds*1000;
            frames=obj.getInt(TAG_FRAMES);                    
            acqOrder=obj.getInt(TAG_ACQ_ORDER);                    
            startTime=new ScheduledTime(obj.getJSONObject(TAG_START_TIME));
            tiling=new TilingSetting(obj.getJSONObject(TilingSetting.TAG_TILING));    
            JSONArray channelArray=obj.getJSONArray(Channel.TAG_CHANNEL_ARRAY);
            channels=new ArrayList<Channel>();
            for (int i=0; i<channelArray.length(); i++) {
                JSONObject chObj=channelArray.getJSONObject(i);
                channels.add(new Channel(chObj));  
            }
            try {
                JSONObject procTreeObj=obj.getJSONObject("ProcTree");
                if (procTreeObj!=null)
                    imageProcRoot=Utils.createProcessorTree(procTreeObj);
                else
                    imageProcRoot=Utils.createDefaultImageProcTree();
            } catch (JSONException ex) {
                imageProcRoot=Utils.createDefaultImageProcTree();
            }    
        }
    }
    
    public JSONObject toJSONObject() throws JSONException {
        JSONObject obj=new JSONObject();
        obj.put(TAG_NAME,name);
        obj.put(TAG_OBJ_GROUP_STR, objectiveDevStr);
        obj.put(TAG_CHANNEL_GROUP_STR, channelGroupStr);
        obj.put(TAG_OBJ_LABEL, objLabel);
        obj.put(TAG_BINNING, binning);
        obj.put(TAG_FIELD_OF_VIEW, fieldOfView.toJSONObject());
        obj.put(TAG_AUTOFOCUS, autofocus);
        obj.put(TAG_Z_STACK, zStack);
        obj.put(TAG_Z_STACK_CENTERED, zStackCentered);
        obj.put(TAG_Z_BEGIN, zBegin);
        obj.put(TAG_Z_END, zEnd);
        obj.put(TAG_Z_STEP_SIZE, zStepSize);
        obj.put(TAG_SLICES, slices);                    
        obj.put(TAG_TIMELAPSE, timelapse);
        obj.put(TAG_HOUR_INTERVAL, hours);                    
        obj.put(TAG_MINUTE_INTERVAL, minutes);                    
        obj.put(TAG_SECOND_INTERVAL, seconds);                    
        obj.put(TAG_FRAMES, frames);                    
        obj.put(TAG_ACQ_ORDER, acqOrder);                    
        obj.put(TAG_START_TIME, startTime.toJSONObject());
        obj.put(TilingSetting.TAG_TILING, tiling.toJSONObject());
        JSONArray channelArray=new JSONArray();
        for (int i=0; i<channels.size(); i++) {
            channelArray.put(i,channels.get(i).toJSONObject());
        }
        obj.put(Channel.TAG_CHANNEL_ARRAY,channelArray);
        obj.put("ProcTree", Utils.processortreeToJSONObject(imageProcRoot, imageProcRoot));
        return obj;
    }
    
    public static AcqSetting.ScheduledTime createScheduledTime (int type, long start) {
        return new AcqSetting.ScheduledTime(type,start);
    }
    
    public void setAbsoluteStart(Calendar cal) {
        absoluteStart=cal;
    }
    
    public Calendar getAbsoluteStart() {
        return absoluteStart;
    }
    
    public AcqSetting duplicate() {
//        AcqSetting copy = new AcqSetting(this.name, this.cameraPixX, this.cameraPixY, this.objLabel, this.objPixSize, this.binning, this.autofocus);
        AcqSetting copy = new AcqSetting(this.name, new FieldOfView(fieldOfView), this.objLabel, this.objPixSize, this.binning, this.autofocus);
        for (Channel c:channels)
            copy.getChannels().add(c.duplicate());
        copy.setStartTime(new ScheduledTime(this.startTime.type,this.startTime.startTimeMS));
        copy.enableZStack(this.zStack);
        copy.setZBegin(this.zBegin);
        copy.setZEnd(this.zEnd);
        copy.setZStepSize(this.zStepSize);
        copy.setZSlices(this.slices);
        copy.enableTimelapse(this.timelapse);
        copy.setMilliSInterval(this.intervalInMS);
        copy.setFrames(frames);
        copy.setAcqOrder(this.acqOrder);
        for (Runnable r:runnables) 
            copy.getRunnables().add(r);
        try {
            copy.setImageProcTree(Utils.createProcessorTree(Utils.processortreeToJSONObject(imageProcRoot, imageProcRoot)));
        } catch (JSONException ex) {
            copy.setImageProcTree(Utils.createDefaultImageProcTree());
            Logger.getLogger(AcqSetting.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            copy.setImageProcTree(Utils.createDefaultImageProcTree());
            Logger.getLogger(AcqSetting.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            copy.setImageProcTree(Utils.createDefaultImageProcTree());
            Logger.getLogger(AcqSetting.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            copy.setImageProcTree(Utils.createDefaultImageProcTree());
            Logger.getLogger(AcqSetting.class.getName()).log(Level.SEVERE, null, ex);
        }
        copy.tiling=this.tiling.duplicate();
        isModified=false;
        return copy;
    }
    
    protected FieldOfView getFieldOfView() {
        return fieldOfView;
    }
    
    protected void setFieldOfView(FieldOfView fov) {
        fieldOfView=fov;
    }
    
    protected TileManager getTileManager() {
        return tileManager;
    }
    
     protected void setTileManager (TileManager tManager) {
        tileManager=tManager;
    }
    
    protected void setObjectiveDevStr(String oGroup) {
        objectiveDevStr=oGroup;
    }
    
    protected String getObjectiveDevStr() {
        return objectiveDevStr;
    }
    
    protected void setChannelGroupStr(String cGroup) {
        channelGroupStr=cGroup;
    }

    protected String getChannelGroupStr() {
        return channelGroupStr;
    }
    
    /*
    public void setPath(String path) {
        this.path=path;
    }
    
    public String getPath() {
        return path;
    }
   */
    public void setStartTime(ScheduledTime start) {
        startTime=start;
    }
    
    public ScheduledTime getStartTime() {
        return startTime;
    }
    
    public void setTotalTiles(long tt) {
        totalTiles=tt;
    }
    
    public long getTotalTiles() {
        return totalTiles;
    }
    
    public void setName(String n) {
        name=n;
    }
    
    public String getName() {
        return name;
    }

/*    
    public void setCameraChipSize(int pixX, int pixY) {
//        cameraPixX=pixX;
//        cameraPixY=pixY;
        detector.setFullDetectorPixelX(pixX);
        detector.setFullDetectorPixelY(pixY);
//        calcTileSize();
    }
*/
    
/*    
    public long getCameraChipWidth() {
//        return cameraPixX;
        return detector.getImagePixel_X();
    }
    
    public long getCameraChipHeight() {
//        return cameraPixY;
        return detector.getImagePixel_Y();
    }
*/
    
/*
    //obsolete?
    public void setTileWidth(double tw) {
        tileWidth=tw;
    }
*/    
    public double getTileWidth_UM() {
//        return tileWidth;
        return fieldOfView.getRoiWidth_UM(objPixSize);
    }
/*    
    //obsolete?
    public void setTileHeight(double th) {
        tileHeight=th;
    }
*/    
    
    public double getTileHeight_UM() {
//        return tileHeight;
        return fieldOfView.getRoiHeight_UM(objPixSize);    
    }
    
    
/*
    //obsolete ?
    private void calcTileSize() {
//        tileWidth=cameraPixX*objPixSize;
//        tileHeight=cameraPixY*objPixSize;
        tileWidth=detector.getDetectorROIPixel_X() * objPixSize;
        tileHeight=detector.getDetectorROIPixel_Y() * objPixSize;
    }
*/
    
    public void setObjective(String label, double oPixSize) {
        objLabel=label;
        objPixSize=oPixSize;
//        calcTileSize();
    }
    
    public String getObjective() {
        return objLabel;
    }
    
    public double getObjPixelSize() {
        return objPixSize;
    }
    
    public double getImagePixelSize() {
//        return fieldOfView.getPixelSize();
        return objPixSize*binning;
    }
    
    public void setBinning(int b) {
        binning=b;
    }

    public int getBinning() {
        return binning;
    }
    
    public void setTilingMode(TilingSetting.Mode tm) {
        tiling.mode=tm;
    }
    
    public TilingSetting.Mode getTilingMode() {
        return tiling.mode;
    }
    
    public void setTilingDir(byte td) {
        tiling.direction=td;
    }
    
    public int getTilingDir() {
        return tiling.direction;
    }
    
    public void enableCluster(boolean b){
        tiling.cluster=b;
    }
    
    public boolean isCluster() {
        return tiling.cluster;
    }
    
    public void setNrClusterX(int nr) {
        tiling.nrClusterTileX=nr;
    }
    
    public int getNrClusterX() {
        return tiling.nrClusterTileX;
    }
    
    public void setNrClusterY(int nr) {
        tiling.nrClusterTileY=nr;
    }
    
    public int getNrClusterY() {
        return tiling.nrClusterTileY;
    }
    
    public void enableSiteOverlap(boolean b) {
        tiling.siteOverlap=b;
    }
    
    public boolean isSiteOverlap() {
        return tiling.siteOverlap;
    }
    
    public void setTileOverlap(double to) {
        tiling.overlap=to;
    }
    
    public double getTileOverlap() {
        return tiling.overlap;
    }
    
    public void setMaxSites(int ms) {
        tiling.maxSites=ms;
    }
    
    public int getMaxSites() {
        return tiling.maxSites;
    }
    
    public void enableInsideOnly(boolean b) {
        tiling.insideOnly=b;
    }
    
    public boolean isInsideOnly() {
        return tiling.insideOnly;
    }
    
    public void setTilingSetting(TilingSetting ts) {
        tiling=ts;
    }
    
    public TilingSetting getTilingSetting() {
        return tiling;
    }
    
    public void setAcqOrder(int ao) {
        acqOrder=ao;
    }
    
    public int getAcqOrder() {
        return acqOrder;
    }
    
    public void enableAutofocus(boolean b) {
        autofocus=b;
    }
    
    public boolean isAutofocus() {
        return autofocus;
    }
    
    public void enableZStack(boolean b) {
        zStack=b;
    }
    
    public boolean isZStack() {
        return zStack;
    }
    
    public void enableTimelapse(boolean b) {
        timelapse=b;
    }
    
    public boolean isTimelapse() {
        return timelapse;
    }
    
    public void setChannels(List<Channel> cl) {
        channels=cl;
    }
    
    public List<Channel> getChannels() {
        return channels;
    }
    
    public boolean hasDuplicateChannels() {
        boolean anyDuplicates=false;
        for (int i=0; i<channels.size(); i++)
            for (int j=i+1; j<channels.size(); j++)
                if (channels.get(i).equals(channels.get(j)))
                    anyDuplicates=true;
        return anyDuplicates;
    }
    
    public void enableZStackCentered(boolean b) {
        zStackCentered=b;
    }
    
    public boolean isZStackCentered() {
        return zStackCentered;
    }
    
    public void setZBegin(double zb) {
        zBegin=zb;
    }
    
    public double getZBegin() {
        return zBegin;
    }
    
    public void setZEnd(double ze) {
        zEnd=ze;
    }
    
    public double getZEnd() {
        return zEnd;
    }
    
    public void setZSlices(int zs) {
        slices=zs;
    }
    
    public int getZSlices() {
        return slices;
    }
    
    public void setZStepSize(double ss) {
        zStepSize=ss;
    }
    
    public double getZStepSize() {
        return zStepSize;
    }
    
    public void setHoursInterval(int h) {
        hours=h;
        intervalInMS=hours*3600000+minutes*60000+seconds*1000;
    }
    
    public int getHoursInterval() {
        return hours;
    }
    
    public void setMinutesInterval(int m) {
        minutes=m;
        intervalInMS=hours*3600000+minutes*60000+seconds*1000;
    }
    
    public int getMinutesInterval() {
        return minutes;
    }
    
    public void setSecondsInterval(int s) {
        seconds=s;
        intervalInMS=hours*3600000+minutes*60000+seconds*1000;
    }
    
    public int getSecondsInterval() {
        return seconds;
    }
    
    public void setMilliSInterval(long ms) {
        intervalInMS=ms;
        hours=(int) Math.floor(intervalInMS/1000/3600);
        minutes=(int) Math.floor(((intervalInMS/1000)%3600)/60);
        seconds=(int)((intervalInMS/1000) -(hours*3600+minutes*60));
    }
    
    public long getIntervalInMilliS() {
        return intervalInMS;
    }
    
    public void setFrames(int f) {
        frames=f;
    }
    
    public int getFrames() {
        return frames;
    }
    
    public void setRunnables(List<Runnable> rl) {
        runnables=rl;
    }
    
    public void addRunnable(Runnable r) {
        if (runnables==null)
            runnables = new ArrayList<Runnable>(); 
        runnables.add(r);
    }
    
    public List<Runnable> getRunnables() {
        return runnables;
    }

    public DefaultMutableTreeNode getImageProcessorTree() {
        return imageProcRoot;
    }

    public void setImageProcTree(DefaultMutableTreeNode ipt) {
        imageProcRoot=ipt;
    }

}
