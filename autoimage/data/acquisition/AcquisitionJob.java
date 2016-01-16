package autoimage.data.acquisition;

import autoimage.services.ExtTaggedImageSink;
import autoimage.services.JobException;
import autoimage.data.acquisition.SequenceConfig;
import autoimage.events.IDataProcessorNotifier;
import autoimage.data.acquisition.AcqSetting;
import autoimage.api.BasicArea;
import autoimage.data.acquisition.Channel;
import autoimage.api.ExtImageTags;
import autoimage.api.IAcqLayout;
import autoimage.events.IDataProcessorListener;
import autoimage.dataprocessors.ExtDataProcessor;
//import autoimage.dataprocessors.SiteInfoProcessorFactory;
import autoimage.dataprocessors.SiteInfoUpdater;
import autoimage.events.AcqJobActiveProcessorsChangedEvent;
import autoimage.events.JobImageStoredEvent;
import autoimage.gui.models.ProcessorTree;
import com.google.common.eventbus.EventBus;
import ij.IJ;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import mmcorej.CMMCore;
import mmcorej.TaggedImage;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.acquisition.DefaultTaggedImageSink;
import org.micromanager.acquisition.MMImageCache;
import org.micromanager.acquisition.TaggedImageStorageDiskDefault;
import org.micromanager.api.Autofocus;
import org.micromanager.api.DataProcessor;
import org.micromanager.api.IAcquisitionEngine2010;
import org.micromanager.api.ImageCacheListener;
import org.micromanager.api.MMTags;
import org.micromanager.api.PositionList;
import org.micromanager.api.SequenceSettings;
import org.micromanager.api.TaggedImageStorage;
import org.micromanager.utils.AutofocusManager;
import org.micromanager.utils.ChannelSpec;
import org.micromanager.utils.MMException;


/**
 *
 * @author Karsten Siller
 */
public class AcquisitionJob extends Job<SequenceConfig> implements ImageCacheListener, IDataProcessorListener {
    
    private final CMMCore core;
    private final AutofocusManager autofocusManager;
    //private final DataManager dataManager;
    private IAcquisitionEngine2010 engine;
    private final String imageStorageRoot;
    private final String sequencePath;
    private final boolean showAcquisitionDisplay;

    private Autofocus autofocus;
    private PositionList posList;
    private SequenceSettings mdaSettings;

//    private List<Datastore> datastores;
//    private Pipeline pipeline;
    private DefaultMutableTreeNode mainImageStorageNode;
//    private List<ProcessorFactory> processorFactories;
    private MMImageCache imageCache;
    private IDataProcessorListener listener;
    private long imagesReceived;
    private long totalImages;
    private volatile boolean acquisitionComplete;

    
    public static class AcqJobBuilder {
        private final String identifier;
        private long scheduledTimeMS = -1;
        private EventBus jeventBus = null;
        private Runnable preInitCallback = null;
        private Runnable postInitCallback = null;
        private Runnable preRunCallback = null;
        private Runnable postRunCallback = null;
        private long callbackTimeoutMS = 0;
        private SequenceConfig configuration = null;
        private CMMCore core = null;
        private IAcquisitionEngine2010 engine;
        private AutofocusManager afManager = null;
//        private DataManager dataManager = null;
        private String imageStorageRoot = "";
        private boolean showAcquisitionDisplay = false;
        
        
        public AcqJobBuilder(String id) {
            identifier=id;
        }
        
        public AcqJobBuilder eventBus(EventBus eb) {
            jeventBus=eb;
            return this;
        }
        
        public AcqJobBuilder acquisitionDisplay(boolean show) {
            showAcquisitionDisplay=show;
            return this;
        }
        
        public AcqJobBuilder scheduleFor(long timeMS) {
            scheduledTimeMS=timeMS;
            return this;
        }
        
        public AcqJobBuilder preInitCallback(Runnable preInit) {
            preInitCallback=preInit;
            return this;
        }
        
        public AcqJobBuilder postInitCallback(Runnable postInit) {
            postInitCallback=postInit;
            return this;
        }
        
        public AcqJobBuilder preRunCallback(Runnable preRun) {
            preRunCallback=preRun;
            return this;
        }
        
        public AcqJobBuilder postRunCallback(Runnable postRun) {
            postRunCallback=postRun;
            return this;
        }
        
        public AcqJobBuilder callbackTimeout(long timeout) {
            callbackTimeoutMS=timeout;
            return this;
        }
        
        public AcqJobBuilder configure(SequenceConfig config) {
            configuration=config;
            return this;
        }
                
        public AcqJobBuilder acqEngine(IAcquisitionEngine2010 eng) {
            engine=eng;
            return this;
        }
        
        public AcqJobBuilder cmmcore(CMMCore c) {
            core=c;
            return this;
        }
        
/*        public AcqJobBuilder dataManager(DataManager dm) {
            dataManager=dm;
            return this;
        }
*/        
        public AcqJobBuilder autofocusManager(AutofocusManager afm) {
            afManager=afm;
            return this;
        }
        
        public AcqJobBuilder imageStorageRoot(String rootPath) {
            imageStorageRoot=rootPath;
            return this;
        }
        
        public AcquisitionJob build() {
            AcquisitionJob newJob=new AcquisitionJob(
                    jeventBus,
                    engine,
                    core,
                    afManager,
//                    dataManager,
                    configuration,
                    preInitCallback,
                    postInitCallback,
                    preRunCallback,
                    postRunCallback,
                    callbackTimeoutMS,
                    imageStorageRoot,
                    showAcquisitionDisplay);
            newJob.updateAndBroadcastStatus(Job.Status.CREATED);
            return newJob;
        }
    }
    
    private AcquisitionJob(EventBus evtbus, IAcquisitionEngine2010 eng, CMMCore c, AutofocusManager am, SequenceConfig config, Runnable preInit, Runnable postInit, Runnable preRun, Runnable postRun, long timeoutMS, String imgStorageRoot, boolean showAcqDisplay) {
        super(evtbus, config.getAcqSetting().getName(), config, preInit, postInit, preRun, postRun, timeoutMS);
        engine=eng;
        core=c;
        autofocusManager=am;
//        dataManager=dm;
        imageStorageRoot=imgStorageRoot;
        sequencePath=new File(imageStorageRoot,configuration.getAcqSetting().getName()).getAbsolutePath();
//        datastores=new ArrayList<Datastore>(); 
        showAcquisitionDisplay=showAcqDisplay;
        imagesReceived=0;
    } 
    
    @Override
    public void imageReceived(TaggedImage ti) {
        synchronized (this) {
            imagesReceived++;
            progressMap.put(Status.ACQUIRING, (float)imagesReceived/totalImages);
            jeventBus.post(new JobImageStoredEvent<TaggedImage>(this, ti, imagesReceived, totalImages));
        }
    }

    @Override
    public void imagingFinished(String string) {
        if (status==Job.Status.ACQUIRING) {
            updateAndBroadcastStatus(Status.ACQUISITION_DONE);
        }    
    }
    
    @Override
    public void imageProcessed(JSONObject metadata, DataProcessor source) {
        if (source instanceof SiteInfoUpdater) {
            
        }
    }
    
    private SequenceSettings createMDASettings(AcqSetting acqSetting, String imageDestPath) {
        SequenceSettings settings = new SequenceSettings();
        //setup time-lapse
        settings.numFrames = acqSetting.isTimelapse() ? acqSetting.getFrames() : 1;
        settings.intervalMs = acqSetting.getTotalIntervalInMilliS();
        
        //setup z-stack
        settings.slices = new ArrayList<Double>();
        if (acqSetting.isZStack()) {
            double z = acqSetting.getZBegin();
            for (int i = 0; i < acqSetting.getZSlices(); i++) {
                settings.slices.add(z);
                if (acqSetting.getZBegin() < acqSetting.getZEnd()) {
                    z += acqSetting.getZStepSize();
                } else {
                    z -= acqSetting.getZStepSize();
                }
            }
        } else {
            settings.slices.add(new Double(0));
        }
        settings.relativeZSlice = true;
        settings.keepShutterOpenSlices = acqSetting.isKeepZShutterOpen();
        
        //setup channels
        settings.channels = new ArrayList<ChannelSpec>();
        settings.channelGroup = acqSetting.getChannelGroupStr();
        for (Channel c:acqSetting.getChannels()) {
            ChannelSpec cs = new ChannelSpec();
            cs.config = c.getName();
            cs.exposure = c.getExposure();
            cs.color = c.getColor();
            cs.doZStack = acqSetting.isZStack();
            cs.zOffset = c.getZOffset();
            cs.useChannel = true;
            //cs.camera = core.getCameraDevice();
            settings.channels.add(cs);
        }
        settings.keepShutterOpenChannels = acqSetting.isKeepChShutterOpen();
        
        //setup autofocus
        settings.useAutofocus = acqSetting.isAutofocus();
        settings.skipAutofocusCount = acqSetting.getAutofocusSkipFrames();
        //the actual autofocus properties need to be set in MMCore object before starting the acquisition

        //setup xy positions
        settings.usePositionList = true;
        
        //setup acquisition order
        settings.slicesFirst = (acqSetting.getAcqOrder() == 1 | acqSetting.getAcqOrder() == 3);
        settings.timeFirst = acqSetting.getAcqOrder() >= 2;
        
        //save images to disk, use acquisition sequence name as prefix, set root directory 
        settings.save = true;
        settings.prefix = acqSetting.getName();
        settings.root = imageDestPath;
        return settings;
    }
    
    
    /**
     * 
     * @param tileName This is a unique String identifier automatically created when the area's calcTilePosition method is run. 
     * Here, tileName is parsed to extract area, cluster, and site info that is added as additional metadata to the image files.
     * @return JSONObject containing new fields describing the tile's area, cluster, and site info. 
     */
/*    private PropertyMap createSiteInfo(BasicArea area, String tileName) {
        int startIndex=tileName.indexOf("Cluster")+7;
        int endIndex=tileName.indexOf("Site")-1;
        PropertyMap.PropertyMapBuilder pBuilder = dataManager.getPropertyMapBuilder();
        pBuilder.putString(ExtImageTags.AREA_NAME,area.getName());
        pBuilder.putInt(ExtImageTags.AREA_INDEX,area.getIndex());
        pBuilder.putInt(ExtImageTags.CLUSTERS_IN_AREA,area.getNoOfClusters());
        pBuilder.putInt(ExtImageTags.SITES_IN_AREA,area.getTileNumber());
        pBuilder.putInt(ExtImageTags.CLUSTER_INDEX, Integer.parseInt(tileName.substring(startIndex,endIndex)));
        startIndex=tileName.indexOf("Site")+4;
        pBuilder.putInt(ExtImageTags.SITE_INDEX, Integer.parseInt(tileName.substring(startIndex)));
        pBuilder.putString(ExtImageTags.AREA_COMMENT, area.getComment());
        return pBuilder.build();
*/
    private Map<String,Object> createSiteInfo(BasicArea area, String tileName) {
        Map<String, Object> info=new HashMap<String,Object>();
        info.put(ExtImageTags.AREA_NAME,area.getName());
        info.put(ExtImageTags.AREA_INDEX,area.getIndex());
        info.put(ExtImageTags.CLUSTERS_IN_AREA,area.getNoOfClusters());
        info.put(ExtImageTags.SITES_IN_AREA,area.getTileNumber());
        int startIndex=tileName.indexOf("Cluster")+7;
        int endIndex=tileName.indexOf("Site")-1;
        info.put(ExtImageTags.CLUSTER_INDEX, Long.parseLong(tileName.substring(startIndex,endIndex)));
        startIndex=tileName.indexOf("Site")+4;
        info.put(ExtImageTags.SITE_INDEX, Long.parseLong(tileName.substring(startIndex)));
        info.put(ExtImageTags.AREA_COMMENT, area.getComment());
        return info;
        
    }
    
    
    @Override
    protected void initialize() throws JobException, InterruptedException {
        if (imageStorageRoot==null || sequencePath==null) {
            throw new JobException(this, "Illegal image destination path");
        }
        //acqLayout.saveTileCoordsToXMLFile(new File(sequencePath, "TileCoordinates.XML").getAbsolutePath(), properties.getTilingSetting(),properties.getTileWidth_UM(),properties.getTileHeight_UM(),properties.getImagePixelSize());

        IAcqLayout acqLayout=configuration.getAcqLayout();
        AcqSetting acqSetting=configuration.getAcqSetting();

        List<Future<Integer>> resultList = acqLayout.calculateTiles(null, 
                acqSetting.getDoxelManager(),
                acqSetting.getFieldOfView(), 
                acqSetting.getTilingSetting());
        while ((acqLayout.getTilingStatus()==IAcqLayout.TILING_IN_PROGRESS) && !Thread.currentThread().isInterrupted()) {
            Thread.sleep(100);
        }
        for (Future<Integer> tileResult:resultList)  {
            try {
                IJ.log(this.getClass().getName()+".initialize : "+this.identifier+", Tiles:"+tileResult.get());
            } catch (ExecutionException ex) {
                Logger.getLogger(AcquisitionJob.class.getName()).log(Level.SEVERE, null, ex);
                throw new JobException(this,"Tiling execution exception");
            }
        }        
        
        acqLayout.saveTileCoordsToXMLFile(new File(sequencePath, "TileCoordinates.XML").getAbsolutePath(), acqSetting.getTilingSetting(),acqSetting.getTileWidth_UM(),acqSetting.getTileHeight_UM(),acqSetting.getImagePixelSize());

        ArrayList<JSONObject> posInfoList = new ArrayList<JSONObject>();

        int index=0;
        for (BasicArea a:acqLayout.getAreaArray()) {
            if (a.isSelectedForAcq()) {                   
                a.setIndex(index++);
                posList = a.addTilePositions(posList, posInfoList, core.getXYStageDevice(), core.getFocusDevice(), acqLayout);
            }
        }
        mdaSettings = createMDASettings(acqSetting, imageStorageRoot);
        if (mdaSettings == null) {
            throw new JobException(this, "Cannot initialize MDA settings.");
        }

        int slices = acqSetting.isZStack() ? acqSetting.getZSlices() : 1;
        int frames = acqSetting.isTimelapse() ? acqSetting.getFrames() : 1;
        totalImages = frames * slices * posList.getNumberOfPositions() * acqSetting.getChannels().size();

        //initialize Autofocus
        autofocus=null;
        if (acqSetting.isAutofocus()) {
            //select autofocus device and apply autofocus settings
            try {
                autofocusManager.selectDevice(acqSetting.getAutofocusDevice());
                autofocus=autofocusManager.getDevice();
                acqSetting.applyAutofocusSettingsToDevice(autofocus);
                IJ.log("Autofocus device: "+autofocus.getDeviceName() + " initialized.");
            } catch (MMException e) {
                mdaSettings.useAutofocus=false;
                throw new JobException(this, "Autofocus device "+acqSetting.getAutofocusDevice() + " not found or cannot be initiated with current settings.");
            }
        }

        //set positionlist for all siteinfoprocessors
        //set workdir for all ExtDataProcessors
        mainImageStorageNode=null;
        Enumeration<DefaultMutableTreeNode> en = acqSetting.getImageProcessorTree().preorderEnumeration();
        int i=0;    
        while (en.hasMoreElements()) {
            DefaultMutableTreeNode node = en.nextElement();
            DataProcessor dp=(DataProcessor)node.getUserObject();
            File procDir=new File (new File(new File (imageStorageRoot,"Processed"),acqSetting.getName()),"Proc"+String.format("%03d",i)+"-"+dp.getName());
            if (!procDir.mkdirs()) {
                IJ.log(procDir.getAbsolutePath());
                throw new JobException(this, "Directories for Processed Images cannot be created.\n"+procDir.getAbsolutePath());
            }            
            if (dp instanceof SiteInfoUpdater) {
                ((SiteInfoUpdater)dp).setPositionInfoList(posInfoList);
            }    
            if (dp instanceof IDataProcessorNotifier) {
                ((IDataProcessorNotifier)dp).addListener(this);
            }
            if (dp instanceof ExtDataProcessor
                    && ((ExtDataProcessor)dp).getProcName().equals(ProcessorTree.PROC_NAME_IMAGE_STORAGE)) {
                mainImageStorageNode=node;
            }
            if (dp instanceof ExtDataProcessor) {
                ((ExtDataProcessor)dp).setWorkDir(procDir.getAbsolutePath());
                ((ExtDataProcessor)dp).setDone(false);
            }    
            i++;
        }
    }
/*
    @Subscribe
    public void datastoreClosedEvent(AcquisitionEndedEvent e) {
        if (datastores.get(0) == e.getStore()) {
            acquisitionComplete=true;
            IJ.log("CLOSED Acquisition datastore: "+e.getStore().getSavePath());
        } else {
            IJ.log("CLOSED other datastore: "+e.getStore().getSavePath());
        }
    }
    
    @Subscribe
    public void datastoreStartedEvent(AcquisitionStartedEvent e) {
        if (e.getDatastore() == datastores.get(0)) {
            acquisitionComplete=false;
            IJ.log("STARTED Acquisition datastore: "+e.getDatastore().getSavePath());
        } else {
            IJ.log("STARTED other datastore: "+e.getDatastore().getSavePath());
        }
    }
*/    
    
    protected Job runAcquisition() throws JobException, InterruptedException {
        try {
            IAcqLayout acqLayout=configuration.getAcqLayout();
            AcqSetting acqSetting=configuration.getAcqSetting();
            BlockingQueue<TaggedImage> engineOutputQueue = engine.run(mdaSettings, false, posList, autofocus);
            JSONObject summaryMetadata = engine.getSummaryMetadata();
            summaryMetadata.put(ExtImageTags.AREAS,acqLayout.getNoOfSelectedAreas());
            summaryMetadata.put(ExtImageTags.CLUSTERS,acqLayout.getNoOfSelectedClusters());
            summaryMetadata.put(ExtImageTags.STAGE_TO_LAYOUT_TRANSFORM,acqLayout.getStageToLayoutTransform().toString());
            summaryMetadata.put(ExtImageTags.DETECTOR_ROTATION,acqSetting.getFieldOfView().getFieldRotation());
            String prefix=summaryMetadata.getString(MMTags.Summary.PREFIX);

            // Set up the DataProcessor<TaggedImage> sequence                    
            BlockingQueue<TaggedImage> procTreeOutputQueue = ProcessorTree.runImage(engineOutputQueue, 
                    (DefaultMutableTreeNode)acqSetting.getImageProcessorTree().getRoot());

            try {
                TaggedImageStorage storage = new TaggedImageStorageDiskDefault(sequencePath, true, summaryMetadata);
                imageCache = new MMImageCache(storage);
                imageCache.addImageCacheListener(this);
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                } else {
                    throw new JobException(this, "Error initializing storage");
                }
            } 
            if (mainImageStorageNode.getChildCount()>0) {
                // create fileoutputqueue and ProcessorTree
                BlockingQueue<File> fileOutputQueue = new LinkedBlockingQueue<File>(1);
                ProcessorTree.runFile(fileOutputQueue, (DefaultMutableTreeNode)mainImageStorageNode.getChildAt(0));
                ExtTaggedImageSink sink = new ExtTaggedImageSink(procTreeOutputQueue, imageCache, fileOutputQueue);
                sink.start();
            } else {
                DefaultTaggedImageSink sink = new DefaultTaggedImageSink(procTreeOutputQueue, imageCache);                
                sink.start();
            }
            while (!Thread.currentThread().isInterrupted() && status==Job.Status.ACQUIRING) {
                Thread.sleep(200);
            }
        } catch (JSONException je) {
            throw new JobException(this, je.getMessage());
        } finally {
        }
        return this;
    }


    private List<DataProcessor> getProcessorList() {
        DefaultMutableTreeNode node=configuration.getAcqSetting().getImageProcessorTree();
        List<DataProcessor> list=new ArrayList<DataProcessor>();
        Enumeration<DefaultMutableTreeNode> en=node.preorderEnumeration();
        //get allPoints of active processors
        while (en.hasMoreElements()) {
            DataProcessor dp=(DataProcessor)en.nextElement().getUserObject();
            list.add(dp);
        }        
        return list;
    }
    
    
    protected Job finishProcessing() throws JobException, InterruptedException {
        IJ.log("FINISH PROCESSING");
        DefaultMutableTreeNode node=configuration.getAcqSetting().getImageProcessorTree();
        List<DataProcessor> activeProcs=new ArrayList<DataProcessor>();
        boolean interrupted=false;
        do {
            List<DataProcessor> lastActiveProcs=activeProcs;
            activeProcs=new ArrayList<DataProcessor>();
            Enumeration<DefaultMutableTreeNode> en=node.preorderEnumeration();
            //get allPoints of active processors
            while (en.hasMoreElements()) {
                DataProcessor dp=(DataProcessor)en.nextElement().getUserObject();
                if (//!(dp instanceof ExtDataProcessor) ||
                        (dp instanceof ExtDataProcessor && !((ExtDataProcessor)dp).getProcName().equals(ProcessorTree.PROC_NAME_IMAGE_STORAGE))) {
                    if (dp.isAlive()) {
                        if (dp instanceof ExtDataProcessor) {
                            if (!((ExtDataProcessor)dp).isDone()) {
                                activeProcs.add(dp);
                            } 
                        } else {
                            activeProcs.add(dp);
                        }
                    }    
                }
            }
            boolean needToUpdate=false;
            if (activeProcs.size() != lastActiveProcs.size()) {
                needToUpdate=true;
            } else {
                for (DataProcessor lap:lastActiveProcs) {
                    if (!activeProcs.contains(lap)) {
                        needToUpdate=true;
                        break;
                    }
                } 
                if (!needToUpdate) {
                    for (DataProcessor ap:activeProcs) {
                        if (!lastActiveProcs.contains(ap)) {
                            needToUpdate=true;
                            break;
                        }                    
                    }
                }
            }
            if (needToUpdate) {
                jeventBus.post(new AcqJobActiveProcessorsChangedEvent<DataProcessor>(this,activeProcs));
            }
            interrupted=Thread.currentThread().isInterrupted();
            if (activeProcs.size()>0) {
                Thread.sleep(200);
            }
        } while (!interrupted && activeProcs.size()>0);
        return this;
    }

    @Override 
    protected Job run() throws JobException, InterruptedException {
        updateAndBroadcastStatus(Status.ACQUIRING);
        progressMap.put(Status.ACQUIRING, 0f);
        runAcquisition();
        progressMap.put(Status.ACQUIRING, 1f);
        updateAndBroadcastStatus(Status.ACQUISITION_DONE);
        updateAndBroadcastStatus(Status.PROCESSING);
        progressMap.put(Status.PROCESSING, 0f);
        finishProcessing();
        progressMap.put(Status.PROCESSING, 1f);
        updateAndBroadcastStatus(Status.PROCESSING_DONE);
        return this;
    }
    
    @Override
    protected void cleanUp() {
        IJ.log(this.getClass().getName()+".cleanUp");
        //stop acquisition engine
        if (!engine.isFinished()) {
            engine.stop();
        }
        //stop all processors
        DefaultMutableTreeNode node=configuration.getAcqSetting().getImageProcessorTree();
        Enumeration<DefaultMutableTreeNode> en=node.preorderEnumeration();
        while (en.hasMoreElements()) {
            DataProcessor dp=(DataProcessor)en.nextElement().getUserObject();
            dp.requestStop();
        }        
        //clear position/doxel lists
        posList.clearAllPositions();
        configuration.getAcqSetting().getDoxelManager().clearList();
    }
    
}
