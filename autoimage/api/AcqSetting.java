package autoimage.api;

import autoimage.Detector;
import autoimage.FieldOfView;
import autoimage.MMCoreUtils;
import autoimage.DoxelManager;
import autoimage.Utils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import mmcorej.CMMCore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.api.Autofocus;
import org.micromanager.utils.MMException;
import org.micromanager.utils.ReportingUtils;

/**
 *
 * @author Karsten Siller
 */
public class AcqSetting {
    public static final String[] ACQ_ORDER_LIST = {"Time, Position, Z, Channel",
                                            "Time, Position, Channel, Z",
                                            "Position, Time, Z, Channel",
                                            "Position, Time, Channel, Z"};
    public static final int MAX_HOUR_INT = 999;
    public static final int MAX_TILE_OVERLAP = 50;//in percent
//    public static final int MAX_MAG_FACTOR = 16;
    public static final int MAX_CLUSTER_X = 10;
    public static final int MAX_CLUSTER_Y = 10;
    public static final int MAX_SITES = 100000;
    
    public static final String TAG_ACQ_SETTING = "ACQUISITION_SETTING";
    public static final String TAG_ACQ_SETTING_ARRAY = "ACQ_SETTING_ARRAY";
    public static final String TAG_VERSION="VERSION";
    public static final String TAG_SETTING="SETTING";
    public static final String TAG_NAME = "NAME";
    public static final String TAG_OBJ_GROUP_STR = "OBJECTIVE_GROUP_STR";
    public static final String TAG_CHANNEL_GROUP_STR = "CHANNEL_GROUP_STR";
    public static final String TAG_CHANNEL_SHUTTER_OPEN = "CHANNEL_SHUTTER_OPEN";
       
    public static final String TAG_OBJ_LABEL = "OBJECTIVE_LABEL";
    public static final String TAG_BINNING = "BINNING";
    public static final String TAG_FIELD_OF_VIEW = "FIELD_OF_VIEW";
    public static final String TAG_AUTOFOCUS = "AUTOFOCUS";
    public static final String TAG_AUTOFOCUS_SETTINGS = "AUTOFOCUS_SETTINGS";
    public static final String TAG_AUTOFOCUS_DEVICE_NAME = "AUTOFOCUS_DEVICE_NAME";
    public static final String TAG_AUTOFOCUS_PROPERTIES = "AUTOFOCUS_PROPERTY_ARRAY";
    public static final String TAG_AUTOFOCUS_SKIP_FRAMES = "AUTOFOCUS_SKIP_FRAMES";
    public static final String TAG_Z_STACK = "Z_STACK";
    public static final String TAG_Z_STACK_CENTERED = "Z_STACK_CENTERED";
    public static final String TAG_Z_BEGIN = "Z_BEGIN";
    public static final String TAG_Z_END = "Z_END";
    public static final String TAG_Z_STEP_SIZE = "Z_STEP_SIZE";
    public static final String TAG_Z_SHUTTER_OPEN = "Z_SHUTTER_OPEN";
    public static final String TAG_SLICES = "SLICES";
    public static final String TAG_TIMELAPSE = "TIMELAPSE";
    public static final String TAG_MILLIS_INTERVAL = "MILLIS_INTERVAL";
    public static final String TAG_FRAMES = "FRAMES";
    public static final String TAG_ACQ_ORDER = "ACQ_ORDER";
    public static final String TAG_IMAGE_PROCESSOR_TREE = "IMAGE_PROCESSOR_TREE";
    public static final String TAG_FILE_PROCESSOR_TREE = "FILE_PROCESSOR_TREE";
    public static final String TAG_START_TIME = "START_TIME";
    
    private String name;            //visible in GUI
    private String objectiveDevStr="";//label of objective turret device
    private String channelGroupStr; //visible in GUI
    private String objLabel;        //visible in GUI
    private double objPixSize;      //internal based on existing MM calibration
    private List<Detector> detectors;
    private FieldOfView fieldOfView;
    private String binningStr;            //visible in GUI 
    private TilingSetting tiling;
    private boolean isModified;
    protected DoxelManager doxelManager;

    private boolean autofocus;      //visible
    private int afSkipFrames;       //visible
    private JSONObject autofocusSettings;
    
    private boolean zStack;         //visible
    private boolean zStackCentered; //visible
    private double zBegin;          //visible
    private double zEnd;            //visible
    private double zStepSize;       //visible
    private int slices;             //visible
    private boolean zShutterOpen;   //visible
    
    private boolean timelapse;      //visible
    private int hours;              //internal for convenience
    private int minutes;            //internal for convenience
    private int seconds;            //internal for convenience
    private int milliseconds;       //internal for convenience
    private long intervalInMS;      //stored
    private int frames;             //visible
    private List<Channel> channels; //visible 
    private boolean chShutterOpen;  //visible
    private int acqOrder;           //visible
    private List<Runnable> runnables; //no used yet
    private DefaultMutableTreeNode imageProcRoot; //stores image processing tree
    private ScheduledTime startTime;         //visible
    private Calendar absoluteStart;   //internal

    public static class ScheduledTime {
        public final static int ASAP = 0;
        public final static int DELAY = 1;
        public final static int ABSOLUTE = 2;
        
        private final static String TAG_TYPE = "TYPE";
        private final static String TAG_START_TIME = "START_TIME";
                
        public int type;
        public long startTimeMS;
        
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

        @Override
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
    
    
    /**
     * 
     * @param n label for this acquisition setting
     */
    public AcqSetting(String n) {
        this(n,"", -1d);
    }

    /**
     * 
     * @param n label for this acquisition setting
     * @param objective label of the objective to be used (needs to correspond to a valid entry in the objective turret device group
     * @param oPixSize pixel size of chosen objective, camera binning is assumed to be 1.
     */
    public AcqSetting(String n, String objective, double oPixSize) {
        this(n, objective, oPixSize, "1", false);
    }

    /**
     * 
     * @param n label for this acquisition setting
     * @param objective label of the objective to be used (needs to correspond to a valid entry in the objective turret device group
     * @param oPixSize pixel size of chosen objective, camera binning is assumed to be 1.
     * @param bin label that describes the binning value (appropriate values are defined by camera device adapter
     * @param autof boolean value to enable or disable use of autofocus during acquisition.
     */
    public AcqSetting(String n, String objective, double oPixSize, String bin, boolean autof) {
        name=n;                     
        binningStr=bin;
        objectiveDevStr="";
        
        autofocus=autof;
        autofocusSettings=new JSONObject();
        afSkipFrames=0;
        channelGroupStr="";
        channels=new ArrayList<Channel>();
        chShutterOpen=false;
        startTime=new ScheduledTime(ScheduledTime.ASAP,0);
        zStack=false;
        zStackCentered=true;
        zBegin=0;
        zEnd=0;
        zStepSize=0;
        zShutterOpen=false;
        slices=1;
        timelapse=false;
        intervalInMS=0;
        frames=1;
        acqOrder=1;
        runnables=new ArrayList<Runnable>();
        imageProcRoot=Utils.createDefaultImageProcTree();
        tiling=new TilingSetting();
        isModified=false;
        doxelManager = new DoxelManager(null, this);
        detectors=new ArrayList<Detector>();
        fieldOfView = new FieldOfView(0,0,FieldOfView.ROTATION_UNKNOWN);
        setObjective(objective,oPixSize);        
    }
        
    public AcqSetting (JSONObject obj) throws JSONException, ClassNotFoundException, InstantiationException, IllegalArgumentException, IllegalAccessException {
        this("");
        if (obj!=null) {
            name=obj.getString(TAG_NAME);
            objectiveDevStr=obj.getString(TAG_OBJ_GROUP_STR);
            channelGroupStr=obj.getString(TAG_CHANNEL_GROUP_STR);
            try {
                chShutterOpen=obj.getBoolean(TAG_CHANNEL_SHUTTER_OPEN);
            } catch (JSONException e) {
                chShutterOpen=false;
            }
            binningStr=obj.getString(TAG_BINNING);
            fieldOfView = new FieldOfView(0,0,FieldOfView.ROTATION_UNKNOWN);
/*            try {
                JSONObject fovObj=obj.getJSONObject(TAG_FIELD_OF_VIEW);
                fieldOfView = new FieldOfView(fovObj);
            } catch (JSONException e) {
                ReportingUtils.logError("Cannot load detector from acquisition settings. Instantiating standard detector");
                fieldOfView = new FieldOfView(1,1,FieldOfView.ROTATION_UNKNOWN);
            }  */  
            setObjective(obj.getString(TAG_OBJ_LABEL),-1);
            autofocus=obj.getBoolean(TAG_AUTOFOCUS);
            autofocusSettings=obj.getJSONObject(TAG_AUTOFOCUS_SETTINGS);
            try {
                afSkipFrames=obj.getInt(TAG_AUTOFOCUS_SKIP_FRAMES);
            } catch (JSONException e) {
                afSkipFrames=0;
            }
            zStack=obj.getBoolean(TAG_Z_STACK);
            zStackCentered=obj.getBoolean(TAG_Z_STACK_CENTERED);
            zBegin=obj.getDouble(TAG_Z_BEGIN);
            zEnd=obj.getDouble(TAG_Z_END);
            zStepSize=obj.getDouble(TAG_Z_STEP_SIZE);
            try {
                zShutterOpen=obj.getBoolean(TAG_Z_SHUTTER_OPEN);
            } catch (JSONException e) {
                zShutterOpen=false;
            }
            slices=obj.getInt(TAG_SLICES);                    
            timelapse=obj.getBoolean(TAG_TIMELAPSE);
            try {
                intervalInMS=obj.getLong(TAG_MILLIS_INTERVAL);
            } catch (JSONException e) {
                intervalInMS=0;
            }
            hours=(int)Math.floor(intervalInMS / 3600000);
            minutes=(int)Math.floor((intervalInMS -(hours*3600000)) / 60000);
            seconds=(int)Math.floor((intervalInMS -(hours*3600000 + minutes*60000)) / 1000);
            milliseconds=(int)(intervalInMS % 1000);
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
        fieldOfView = new FieldOfView(0,0,FieldOfView.ROTATION_UNKNOWN);
        detectors=new ArrayList<Detector>();
    }
    
    public List<Detector> getDetectors() {
        return detectors;
    }
    
    public void setDetectors(List<Detector> det) {
        detectors=det;
        if (!det.isEmpty()) {
            Detector firstDetector=det.get(0);
            fieldOfView=new FieldOfView(
//                    firstDetector.getFullWidth_Pixel(), 
//                    firstDetector.getFullHeight_Pixel(),
                    firstDetector.getUnbinnedRoi().width,
                    firstDetector.getUnbinnedRoi().height,
                    firstDetector.getUnbinnedRoi(),
                    firstDetector.getFieldRotation());
        } else {
            fieldOfView=new FieldOfView(
                    0,
                    0,
                    null,
                    FieldOfView.ROTATION_UNKNOWN);
        }
//        fieldOfView.createFullChipPath(objPixSize);
        fieldOfView.createRoiPath(objPixSize);   
    }
    
    public void addDetector(Detector det) {
        if (detectors==null) {
            detectors=new ArrayList<Detector>();
        }
        detectors.add(det);
        if (detectors.size()==1) {
            //first detector
            //this ensures that fieldOfView is updated
            setDetectors(detectors);
        }
    }
    
    public Detector getDetector(int index) {
        if (detectors!=null && index>=0 && index<detectors.size()) {
            return detectors.get(index);
        } else {
            return null;
        }
    }
    
    public static JSONObject convertAutofocusSettings(Autofocus af, CMMCore core_) throws JSONException, MMException {
        JSONObject obj=new JSONObject();
        obj.put(TAG_AUTOFOCUS_DEVICE_NAME, af.getDeviceName());
        JSONObject properties=new JSONObject();
        try {
            for (String propertyName : af.getPropertyNames()) {
                properties.put(propertyName, af.getPropertyValue(propertyName));
            }
        } catch (ClassCastException ex) {
            //caused by bug in CoreAutofocus.getPropertyNames(), fixed in 1.4.20
            for (String propertyName : MMCoreUtils.getPropertyNamesForAFDevice(af.getDeviceName(),core_)) {
                properties.put(propertyName, af.getPropertyValue(propertyName));
            }
//            throw new MMException("Cannot read properties for "+af.getDeviceName());
        }
        obj.put(TAG_AUTOFOCUS_PROPERTIES, properties);
        return obj;
    }

    public void setAutofocusSettings(Autofocus af, CMMCore core_) throws MMException{
        try {
            autofocusSettings=AcqSetting.convertAutofocusSettings(af, core_);
        } catch (JSONException ex) {
            autofocusSettings=new JSONObject();
            ReportingUtils.logError(ex, "cannot set autofocus settings for "+af.getDeviceName());
            throw new MMException(ex.getMessage());
        } catch (MMException ex) {
            autofocusSettings=new JSONObject();
            ReportingUtils.logError(ex, "cannot set autofocus settings for "+af.getDeviceName());
            throw new MMException(ex.getMessage());
        }
    }
    //returns null if settings are not defind or device name not found
    public String getAutofocusDevice() {
        String device=null;
        if (autofocusSettings!=null) {
            try {
                device=autofocusSettings.getString(TAG_AUTOFOCUS_DEVICE_NAME);
            } catch (JSONException ex) {
//                Logger.getLogger(AcqSetting.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return device;
    }
    
    public JSONObject getAutofocusProperties() {
        JSONObject properties=null;
        if (autofocusSettings!=null) {
            try {
                properties=autofocusSettings.getJSONObject(TAG_AUTOFOCUS_PROPERTIES);
            } catch (JSONException ex) {
                Logger.getLogger(AcqSetting.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return properties;
    }

    public JSONObject getAutofocusSettings() {
        return autofocusSettings;
    }
    
    public int getAutofocusSkipFrames() {
        return afSkipFrames;
    }
    
    public void setAutofocusSkipFrames(int skipFrames) {
        if (skipFrames >=0) {
            afSkipFrames=skipFrames;
        }
    }
    
    public boolean isKeepZShutterOpen() {
        return zShutterOpen;
    }
    
    public void setKeepZShutterOpen(boolean b) {
        zShutterOpen=b;
    }
    
    public boolean isKeepChShutterOpen() {
        return chShutterOpen;
    }
    
    public void setKeepChShutterOpen(boolean b) {
        chShutterOpen=b;
    }
    
    //trys to apply autofocusSettings to af object 
    public void applyAutofocusSettingsToDevice(Autofocus af) throws MMException {
        String property="";
        String key="";
        if (af!=null && af.getDeviceName().equals(getAutofocusDevice())) {
            JSONObject properties=getAutofocusProperties();
            Iterator<String> keys=properties.keys();
            while (keys.hasNext()) {
                key=keys.next();
                try {
                    property = properties.getString(key);
                    af.setPropertyValue(key, property);
                } catch (JSONException ex) {
                    throw (new MMException("AcqSetting.applyAutofocusSettings: error setting property for "+af.getDeviceName()+", key: "+key+", property:" + property));
                }
            }
            af.applySettings();
        } else {
            throw (new MMException("AcqSetting.applyAutofocusSettings: AF device=null or "+ getAutofocusDevice()+" not found"));
        }
    }
    
    public JSONObject toJSONObject() throws JSONException {
        JSONObject obj=new JSONObject();
        obj.put(TAG_NAME,name);
        obj.put(TAG_OBJ_GROUP_STR, objectiveDevStr);
        obj.put(TAG_CHANNEL_GROUP_STR, channelGroupStr);
        obj.put(TAG_CHANNEL_SHUTTER_OPEN, chShutterOpen);
        obj.put(TAG_OBJ_LABEL, objLabel);
        obj.put(TAG_BINNING, binningStr);
//        obj.put(TAG_FIELD_OF_VIEW, fieldOfView.toJSONObject());
        obj.put(TAG_AUTOFOCUS, autofocus);
        obj.put(TAG_AUTOFOCUS_SETTINGS, autofocusSettings);
        obj.put(TAG_AUTOFOCUS_SKIP_FRAMES, afSkipFrames);
        obj.put(TAG_Z_STACK, zStack);
        obj.put(TAG_Z_STACK_CENTERED, zStackCentered);
        obj.put(TAG_Z_BEGIN, zBegin);
        obj.put(TAG_Z_END, zEnd);
        obj.put(TAG_Z_STEP_SIZE, zStepSize);
        obj.put(TAG_Z_SHUTTER_OPEN, zShutterOpen);
        obj.put(TAG_SLICES, slices);                    
        obj.put(TAG_TIMELAPSE, timelapse);
        obj.put(TAG_MILLIS_INTERVAL, intervalInMS);                    
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
        AcqSetting copy = new AcqSetting(this.name, this.objLabel, this.objPixSize, this.binningStr, this.autofocus);
        for (Channel c:channels)
            copy.getChannels().add(c.duplicate());
        copy.setStartTime(new ScheduledTime(this.startTime.type,this.startTime.startTimeMS));
        copy.enableZStack(this.zStack);
        copy.setZBegin(this.zBegin);
        copy.setZEnd(this.zEnd);
        copy.setZStepSize(this.zStepSize);
        copy.setZSlices(this.slices);
        copy.enableTimelapse(this.timelapse);
        copy.setTotalMilliSInterval(this.intervalInMS);
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
    
    public FieldOfView getFieldOfView() {
        return fieldOfView;
    }
 
 /*   
    public void setFieldOfView(FieldOfView fov) {
        fieldOfView=fov;
    }
 */
    
    public DoxelManager getDoxelManager() {
        return doxelManager;
    }
    
    protected void setDoxelManager (DoxelManager dManager) {
        doxelManager=dManager;
    }
    
    public void setObjectiveDevStr(String oGroup) {
        objectiveDevStr=oGroup;
    }
    
    public String getObjectiveDevStr() {
        return objectiveDevStr;
    }
    
    public void setChannelGroupStr(String cGroup) {
        channelGroupStr=cGroup;
    }

    public String getChannelGroupStr() {
        return channelGroupStr;
    }
    
    public void setStartTime(ScheduledTime start) {
        startTime=start;
    }
    
    public ScheduledTime getStartTime() {
        return startTime;
    }
    
/*    public void setTotalTiles(long tt) {
        totalTiles=tt;
    }
    
    public long getTotalTiles() {
        return totalTiles;
    }
*/    
    public void setName(String n) {
        name=n;
    }
    
    public String getName() {
        return name;
    }

    public double getTileWidth_UM() {
//        return tileWidth;
        return fieldOfView.getRoiWidth_UM(objPixSize);
    }
    
    public double getTileHeight_UM() {
//        return tileHeight;
        return fieldOfView.getRoiHeight_UM(objPixSize);    
    }
       
    public void setObjective(String label, double oPixSize) {
        objLabel=label;
        objPixSize=oPixSize;
//        fieldOfView.createFullChipPath(objPixSize);
        fieldOfView.createRoiPath(objPixSize);
    }
    
    public String getObjective() {
        return objLabel;
    }
    
    public double getObjPixelSize() {
        return objPixSize;
    }
    
    public double getImagePixelSize() {
        return objPixSize*detectors.get(0).getBinningFactor(binningStr, -1);
    }
    
    public void setBinning(String bin) {
        binningStr=bin;
    }

    public String getBinningDesc() {
        return binningStr;
    }

    public int getBinningFactor() {
        return detectors.get(0).getBinningFactor(binningStr, -1);
    }
    
    public void setTilingMode(TilingSetting.Mode tm) {
        tiling.mode=tm;
    }
    
    public TilingSetting.Mode getTilingMode() {
        return tiling.mode;
    }
    
    public void setTilingDir(int td) {
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
    
    public List<String> getChannelNames() {
        if (channels==null) {
            return null;
        } else {
            List<String> chNames=new ArrayList<String>(channels.size());
            for (Channel ch:channels) {
                chNames.add(ch.getName());
            }
            return chNames;
        }
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
        intervalInMS=hours*3600000+minutes*60000+seconds*1000+milliseconds;
    }
    
    public int getHoursInterval() {
        return hours;
    }
    
    public void setMinutesInterval(int m) {
        minutes=m;
        intervalInMS=hours*3600000+minutes*60000+seconds*1000+milliseconds;
    }
    
    public int getMinutesInterval() {
        return minutes;
    }
    
    public void setSecondsInterval(int s) {
        seconds=s;
        intervalInMS=hours*3600000+minutes*60000+seconds*1000+milliseconds;
    }
    
    public int getSecondsInterval() {
        return seconds;
    }
    
    public void setMillisecondsInterval(int millis) {
        milliseconds=millis;
        intervalInMS=hours*3600000+minutes*60000+seconds*1000+milliseconds;
    }
    
    public int getMillisecondsInterval() {
        return milliseconds;
    }
    
    public void setTotalMilliSInterval(long ms) {
        intervalInMS=ms;
        hours=(int) Math.floor(intervalInMS/1000/3600);
        minutes=(int) Math.floor(((intervalInMS/1000)%3600)/60);
        seconds=(int)((intervalInMS/1000) -(hours*3600+minutes*60));
        milliseconds=(int)(intervalInMS - (hours*3600000 + minutes*60000 + seconds*1000));
    }
    
    public long getTotalIntervalInMilliS() {
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
