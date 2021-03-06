package autoimage.data.acquisition;

import autoimage.data.layout.TilingSetting;
import autoimage.utils.MMCoreUtils;
import autoimage.services.DoxelManager;
import autoimage.data.Detector;
import autoimage.data.FieldOfView;
import autoimage.events.config.MMConfigAddedEvent;
import autoimage.events.config.MMConfigChGroupChangedEvent;
import autoimage.events.config.MMConfigDeletedEvent;
import autoimage.events.config.MMConfigListChangedEvent;
import autoimage.events.config.MMConfigUpdatedEvent;
import autoimage.utils.Utils;
import com.google.common.eventbus.EventBus;
import ij.IJ;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
    
    private final EventBus eventBus = new EventBus();
    private String name;            //visible in GUI
    private String objectiveDevStr="";//label of objective turret device
    private String channelGroupStr; //visible in GUI
    private String objLabel;        //visible in GUI
    private double objPixSize;      //internal based on existing MM calibration
    private List<Detector> detectors;
    private final List<MMConfig> usedMMConfigs = Collections.synchronizedList(new ArrayList<MMConfig>());
    private final List<MMConfig> usedMMConfigsForPublic = Collections.unmodifiableList(usedMMConfigs);
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
        tiling=new TilingSetting.Builder().build();
        isModified=false;
        doxelManager = new DoxelManager(null, this);
        detectors=new ArrayList<Detector>();
        fieldOfView = new FieldOfView(0,0,FieldOfView.ROTATION_UNKNOWN);
        setObjective(objective,oPixSize);        
    }
        
    public static AcqSetting AcqSettingFactory (JSONObject obj) throws JSONException, ClassNotFoundException, InstantiationException, IllegalArgumentException, IllegalAccessException {
        AcqSetting newSetting=null;
        if (obj!=null) {
            newSetting=new AcqSetting(obj.getString(TAG_NAME));
            newSetting.setObjectiveDevStr(obj.getString(TAG_OBJ_GROUP_STR));
            newSetting.setChannelGroupStr(obj.getString(TAG_CHANNEL_GROUP_STR));
            try {
                newSetting.setKeepChShutterOpen(obj.getBoolean(TAG_CHANNEL_SHUTTER_OPEN));
            } catch (JSONException e) {
                newSetting.setKeepChShutterOpen(false);
            }
            newSetting.setBinning(obj.getString(TAG_BINNING));
/*            try {
                JSONObject fovObj=obj.getJSONObject(TAG_FIELD_OF_VIEW);
                fieldOfView = new FieldOfView(fovObj);
            } catch (JSONException e) {
                ReportingUtils.logError("Cannot load detector from acquisition settings. Instantiating standard detector");
                fieldOfView = new FieldOfView(1,1,FieldOfView.ROTATION_UNKNOWN);
            }  */  
            newSetting.setObjective(obj.getString(TAG_OBJ_LABEL),-1);
            newSetting.enableAutofocus(obj.getBoolean(TAG_AUTOFOCUS));
            newSetting.autofocusSettings=obj.getJSONObject(TAG_AUTOFOCUS_SETTINGS);
            try {
                newSetting.setAutofocusSkipFrames(obj.getInt(TAG_AUTOFOCUS_SKIP_FRAMES));
            } catch (JSONException e) {
                newSetting.setAutofocusSkipFrames(0);
            }
            newSetting.enableZStack(obj.getBoolean(TAG_Z_STACK));
            newSetting.enableZStackCentered(obj.getBoolean(TAG_Z_STACK_CENTERED));
            newSetting.setZBegin(obj.getDouble(TAG_Z_BEGIN));
            newSetting.setZEnd(obj.getDouble(TAG_Z_END));
            newSetting.setZStepSize(obj.getDouble(TAG_Z_STEP_SIZE));
            try {
                newSetting.setKeepZShutterOpen(obj.getBoolean(TAG_Z_SHUTTER_OPEN));
            } catch (JSONException e) {
                newSetting.setKeepZShutterOpen(false);
            }
            newSetting.setZSlices(obj.getInt(TAG_SLICES));                    
            newSetting.enableTimelapse(obj.getBoolean(TAG_TIMELAPSE));
            try {
                newSetting.setTotalMilliSInterval(obj.getLong(TAG_MILLIS_INTERVAL));
            } catch (JSONException e) {
                newSetting.setTotalMilliSInterval(0);
            }
            newSetting.setFrames(obj.getInt(TAG_FRAMES));
            newSetting.setAcqOrder(obj.getInt(TAG_ACQ_ORDER));                    
            newSetting.setStartTime(new ScheduledTime(obj.getJSONObject(TAG_START_TIME)));
            try {
                newSetting.setTilingSetting(new TilingSetting.Builder(obj.getJSONObject(TilingSetting.TAG_TILING)).build());  
            } catch (JSONException je) {
                IJ.log(je.getMessage().toString());
            }
            JSONArray channelArray=obj.getJSONArray(Channel.TAG_CHANNEL_ARRAY);
            List<Channel> channels=new ArrayList<Channel>();
            for (int i=0; i<channelArray.length(); i++) {
                JSONObject chObj=channelArray.getJSONObject(i);
                channels.add(new Channel(chObj));  
            }
            newSetting.setChannels(channels);
            try {
                JSONObject procTreeObj=obj.getJSONObject("ProcTree");
                if (procTreeObj!=null)
                    newSetting.setImageProcTree(Utils.createProcessorTree(procTreeObj));
                else
                    newSetting.setImageProcTree(Utils.createDefaultImageProcTree());
            } catch (JSONException ex) {
                newSetting.setImageProcTree(Utils.createDefaultImageProcTree());
            }    
            newSetting.fieldOfView = new FieldOfView(0,0,FieldOfView.ROTATION_UNKNOWN);
        }
        return newSetting;
    }
    
    public static AcqSetting createUniqueCopy(final List<AcqSetting> settings, final AcqSetting setting) {
        AcqSetting uniqueCopy=null;
        JSONObject acqSettingObj;
        try {
            acqSettingObj = setting.toJSONObject();
            acqSettingObj.put(TAG_NAME, Utils.createUniqueAcqSettingName(settings, setting.getName()));
            uniqueCopy=AcqSetting.AcqSettingFactory(acqSettingObj);
        } catch (JSONException ex) {
            Logger.getLogger(AcqSetting.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(AcqSetting.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(AcqSetting.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(AcqSetting.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(AcqSetting.class.getName()).log(Level.SEVERE, null, ex);
        }
        return uniqueCopy;
    }
    
    public void registerForEvents(Object listener) {
        eventBus.register(listener);
    }
    
    public void unregisterForEvents(Object listener) {
        eventBus.unregister(listener);
    }
    
    // HARDWARE CONFIGS
    public List<MMConfig> getUsedMMConfigs() {
        return usedMMConfigsForPublic;
    }
    
    public MMConfig putMMConfig(MMConfig newConfig) {
        IJ.log("putMMConfig: "+newConfig.getName()+", "+newConfig.getSelectedPreset());
        int index=-1;
        for (int i=0; i<usedMMConfigs.size(); i++) {
            MMConfig config=usedMMConfigs.get(i);
            if (config.getName().equals(newConfig.getName())) {
                index=i;
                break;
            }
        }
        if (index==-1) {
            //add entry
            usedMMConfigs.add(newConfig);
            eventBus.post(new MMConfigAddedEvent(this,newConfig));
        } else {
            //replace entry
            usedMMConfigs.set(index,newConfig);
            eventBus.post(new MMConfigUpdatedEvent(this,newConfig));
        }
        if (newConfig.isChannel() && !newConfig.getName().equals(channelGroupStr)) {
            //new channel group selected
            String oldChannelGroup=this.channelGroupStr;
            this.channelGroupStr=newConfig.getName();
            eventBus.post(new MMConfigChGroupChangedEvent(this,oldChannelGroup, newConfig.getName()));
        } else if (!newConfig.isChannel() && newConfig.getName().equals(channelGroupStr)) {
            //channel group deselected
            String oldChannelGroup=this.channelGroupStr;
            this.channelGroupStr="";
            eventBus.post(new MMConfigChGroupChangedEvent(this,oldChannelGroup, ""));
        }
        return newConfig;
    }

    public MMConfig deleteMMConfig(String name) {
        IJ.log("deleteMMConfig: ");
        for (MMConfig config:usedMMConfigs) {
            if (config.getName().equals(name)) {
                IJ.log("    deleteMMConfig: "+config.getName()+", "+config.getSelectedPreset());
                if (usedMMConfigs.remove(config)) {
                    if (name.equals(channelGroupStr)) {
                        String oldChannelGroup=this.channelGroupStr;
                        channelGroupStr="";
                        eventBus.post(new MMConfigChGroupChangedEvent(this,oldChannelGroup, ""));
                    }
                    eventBus.post(new MMConfigDeletedEvent(this,config));
                    return config;
                }
            }
        }
        return null;    
    }
    
    public MMConfig updateUsedMMConfig(MMConfig newConfig) {
        IJ.log("updateMMConfig: "+newConfig.getName()+", "+newConfig.getSelectedPreset());
        for (int index=0; index<usedMMConfigs.size(); index++) {
            MMConfig config=usedMMConfigs.get(index);
            if (config.getName().equals(newConfig.getName())) {
                usedMMConfigs.set(index,newConfig);
                eventBus.post(new MMConfigUpdatedEvent(this,newConfig));
                return newConfig;
            }
        }
        //not found in list
        return null;
    }
    
    public void setUsedMMConfigs(List<MMConfig> list) {
        IJ.log("setUsedMMConfigs:");
        usedMMConfigs.clear();
        for (MMConfig config:list) {
            usedMMConfigs.add(config);
        }
        eventBus.post(new MMConfigListChangedEvent(this, usedMMConfigsForPublic));
    }
    
    //END HARDWARE CONFIGS
/*    @Override
    public boolean equals(Object setting) {
        return setting!=null && setting instanceof String && this.name.equals((String)setting);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
*/    
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
        copy.tiling=this.tiling.copy().build();
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
//        tiling.mode=tm;
        tiling=tiling.copy().mode(tm).build();
    }
    
    public TilingSetting.Mode getTilingMode() {
        return tiling.getMode();
    }
    
    public void setTilingDir(int td) {
//        tiling.direction=td;
        tiling=tiling.copy().direction(td).build();
    }
    
    public int getTilingDir() {
        return tiling.getTilingDirection();
    }
    
    public void enableCluster(boolean b){
//        tiling.cluster=b;
        tiling=tiling.copy().cluster(b).build();
    }
    
    public boolean isCluster() {
        return tiling.isCluster();
    }
    
    public void setNrClusterX(int nr) {
//        tiling.nrClusterTileX=nr;
        tiling=tiling.copy().clusterGrid(nr, tiling.getNrClusterTileY()).build();
    }
    
    public int getNrClusterX() {
        return tiling.getNrClusterTileX();
    }
    
    public void setNrClusterY(int nr) {
//        tiling.nrClusterTileY=nr;
        tiling=tiling.copy().clusterGrid(tiling.getNrClusterTileX(), nr).build();
    }
    
    public int getNrClusterY() {
        return tiling.getNrClusterTileY();
    }
    
    public void enableSiteOverlap(boolean b) {
//        tiling.siteOverlap=b;
        tiling=tiling.copy().siteOverlap(b).build();
    }
    
    public boolean isSiteOverlap() {
        return tiling.isSiteOverlap();
    }
    
    public void setTileOverlap(double to) {
//        tiling.overlap=to;
        tiling=tiling.copy().tileOverlap(to).build();
    }
    
    public double getTileOverlap() {
        return tiling.getTileOverlap();
    }
    
    public void setMaxSites(int ms) {
//        tiling.maxSites=ms;
        tiling=tiling.copy().maxSites(ms).build();
    }
    
    public int getMaxSites() {
        return tiling.getMaxSites();
    }
    
    public void enableInsideOnly(boolean b) {
//        tiling.insideOnly=b;
        tiling=tiling.copy().insideOnly(b).build();
    }
    
    public boolean isInsideOnly() {
        return tiling.isInsideOnly();
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
