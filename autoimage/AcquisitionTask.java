package autoimage;

import autoimage.api.AcqSetting;
import autoimage.api.BasicArea;
import autoimage.api.Channel;
import autoimage.api.ExtImageTags;
import autoimage.api.IAcqLayout;
import autoimage.dataprocessors.ExtDataProcessor;
import autoimage.dataprocessors.SiteInfoUpdater;
import autoimage.gui.ProcessorTree;
import ij.IJ;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.tree.DefaultMutableTreeNode;
import mmcorej.CMMCore;
import mmcorej.TaggedImage;
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
import org.micromanager.api.ScriptInterface;
import org.micromanager.api.SequenceSettings;
import org.micromanager.api.TaggedImageStorage;
//import org.micromanager.imagedisplay.VirtualAcquisitionDisplay;
import org.micromanager.utils.ChannelSpec;
import org.micromanager.utils.MMException;

/**
 *
 * @author Karsten Siller
 */
public class AcquisitionTask extends TimerTask {

    private final ScriptInterface app_;
    private final CMMCore core;
    private final String imageDestPath;
    private final AcqSetting acqSetting;
    private final IAcqLayout acqLayout;
    private String prefix;
    private MMImageCache imageCache;
    private Autofocus autofocus;
    private final ImageCacheListener imageListener;
    private int totalImages;
    private final File sequencePath;
    private SequenceSettings mdaSettings;
    private PositionList posList;
    private DefaultMutableTreeNode mainImageStorageNode;
//    private VirtualAcquisitionDisplay virtualDisplay;
    private volatile boolean isInitializing=true;
    private volatile boolean isWaiting=true;
    private volatile boolean hasStarted=false;

    public AcquisitionTask (ScriptInterface app, String imageDest, AcqSetting setting, IAcqLayout layout, ImageCacheListener imgListener, File seqPath) {
        app_=app;
        core=app.getMMCore();
        imageDestPath=imageDest;
        acqSetting=setting;
        acqLayout=layout;
        imageListener=imgListener;
        sequencePath=seqPath;
        IJ.log("AcquisitionTask.intialize: "+imageDestPath +", "+sequencePath);
        posList = new PositionList();
    }

    public void initialize() throws MMException, InterruptedException {
        if (imageDestPath==null || sequencePath==null) {
            throw new MMException("Illegal image destination path");
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
        mdaSettings = createMDASettings(acqSetting);
        if (mdaSettings == null) {
            throw new MMException("Cannot initialize MDA settings.");
        }

        int slices = acqSetting.isZStack() ? acqSetting.getZSlices() : 1;
        int frames = acqSetting.isTimelapse() ? acqSetting.getFrames() : 1;
        totalImages = frames * slices * posList.getNumberOfPositions() * acqSetting.getChannels().size();

        //initialize Autofocus
        autofocus=null;
        if (acqSetting.isAutofocus()) {
            //select autofocus device and apply autofocus settings
            try {
                app_.getAutofocusManager().selectDevice(acqSetting.getAutofocusDevice());
                autofocus=app_.getAutofocusManager().getDevice();
                acqSetting.applyAutofocusSettingsToDevice(autofocus);
                IJ.log("Autofocus device: "+autofocus.getDeviceName() + " initialized.");
            } catch (MMException e) {
                IJ.log("Autofocus device "+acqSetting.getAutofocusDevice() + " not found or cannot be initiated with current settings.");
                mdaSettings.useAutofocus=false;
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
            File procDir=new File (new File(new File (imageDestPath,"Processed"),acqSetting.getName()),"Proc"+String.format("%03d",i)+"-"+dp.getName());
            if (!procDir.mkdirs()) {
                IJ.log(procDir.getAbsolutePath());
                throw new MMException("Directories for Processed Images cannot be created.\n"+procDir.getAbsolutePath());
            }            
            if (dp instanceof SiteInfoUpdater) {
                ((SiteInfoUpdater)dp).setPositionInfoList(posInfoList);
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
        isInitializing=false;
    }

    private SequenceSettings createMDASettings(AcqSetting acqSetting) {
        //ok, create new mda sequence settings
        SequenceSettings settings = new SequenceSettings();
        //setup time-lapse
//            settings.numFrames = timelapseCheckBox.isSelected() ? acqSetting.getFrames() : 1;
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
        
//        ChannelTableModel ctm = (ChannelTableModel) channelTable.getModel();
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
    
    public MMImageCache getImageCache() {
        return imageCache;
    }
    
    public int getTotalImages() {
        return totalImages;
    }

    public boolean isInitializing() {
        return isInitializing;
    }

    public boolean isWaiting() {
        return isWaiting;
    }

    public boolean hasStarted() {
        return hasStarted;
    }

    public String getPrefix() {
        return prefix;
    }
    
    @Override
    public void run() {
        try {
            IAcquisitionEngine2010 acqEng2010 = app_.getAcquisitionEngine2010();
            isWaiting=false;
            Calendar now=Calendar.getInstance();
            if (acqSetting.getStartTime().type == AcqSetting.ScheduledTime.ASAP) {
                IJ.log("Sequence '"+acqSetting.getName()+"' started asap at : "+now.getTime().toString());
            } else {
                IJ.log("Sequence '"+acqSetting.getName()+"' started at:  "+now.getTime().toString()+" ("+Long.toString(now.getTimeInMillis() - scheduledExecutionTime())+" ms delay)");            
            }
            BlockingQueue<TaggedImage> engineOutputQueue = acqEng2010.run(mdaSettings, false, posList, autofocus);
            JSONObject summaryMetadata = acqEng2010.getSummaryMetadata();
            summaryMetadata.put(ExtImageTags.AREAS,acqLayout.getNoOfSelectedAreas());
            summaryMetadata.put(ExtImageTags.CLUSTERS,acqLayout.getNoOfSelectedClusters());
            summaryMetadata.put(ExtImageTags.STAGE_TO_LAYOUT_TRANSFORM,acqLayout.getStageToLayoutTransform().toString());
            summaryMetadata.put(ExtImageTags.DETECTOR_ROTATION,acqSetting.getFieldOfView().getFieldRotation());
            prefix=summaryMetadata.getString(MMTags.Summary.PREFIX);

            // Set up the DataProcessor<TaggedImage> sequence                    
            BlockingQueue<TaggedImage> procTreeOutputQueue = ProcessorTree.runImage(engineOutputQueue, 
                    (DefaultMutableTreeNode)acqSetting.getImageProcessorTree().getRoot());

            TaggedImageStorage storage = new TaggedImageStorageDiskDefault(sequencePath.getAbsolutePath(), true, summaryMetadata);
            imageCache = new MMImageCache(storage);
            imageCache.addImageCacheListener(imageListener);

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
       } catch (Exception e) {
        } finally {
        }
        hasStarted=true;
        IJ.log("Sequence '"+acqSetting.getName()+"' running");
    }   
}
