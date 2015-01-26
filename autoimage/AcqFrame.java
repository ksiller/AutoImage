/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;


import autoimage.dataprocessors.BranchedProcessor;
import autoimage.dataprocessors.ExtDataProcessor;
import autoimage.dataprocessors.ImageTagFilter;
import autoimage.dataprocessors.ImageTagFilterLong;
import autoimage.dataprocessors.ImageTagFilterOpt;
import autoimage.dataprocessors.ImageTagFilterOptLong;
import autoimage.dataprocessors.ImageTagFilterOptString;
import autoimage.dataprocessors.ImageTagFilterString;
import autoimage.dataprocessors.RoiFinder;
import autoimage.dataprocessors.ScriptAnalyzer;
import autoimage.dataprocessors.SiteInfoUpdater;
import autoimage.olddp.NoFilterSeqAnalyzer;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.LUT;
import ij.process.ShortProcessor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import mmcorej.CMMCore;
import mmcorej.Configuration;
import mmcorej.StrVector;
import mmcorej.TaggedImage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.acquisition.DefaultTaggedImageSink;
import org.micromanager.acquisition.MMImageCache;
import org.micromanager.acquisition.TaggedImageStorageDiskDefault;
import org.micromanager.api.Autofocus;
import org.micromanager.api.DataProcessor;
import org.micromanager.api.IAcquisitionEngine2010;
import org.micromanager.api.ImageCache;
import org.micromanager.api.ImageCacheListener;
import org.micromanager.api.MMListenerInterface;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.MMTags;
import org.micromanager.api.PositionList;
import org.micromanager.api.ScriptInterface;
import org.micromanager.api.SequenceSettings;
import org.micromanager.api.TaggedImageAnalyzer;
import org.micromanager.api.TaggedImageStorage;
import org.micromanager.internalinterfaces.AcqSettingsListener;
import org.micromanager.utils.ChannelSpec;
import org.micromanager.utils.ImageUtils;
import org.micromanager.utils.MMException;
import org.micromanager.utils.MMScriptException;
import org.micromanager.utils.ReportingUtils;


/**
 *
 * @author Karsten Siller
 */
public class AcqFrame extends javax.swing.JFrame implements ActionListener, TableModelListener, WindowListener, IStageMonitorListener, ILiveListener, IRefPointListener, IMergeAreaListener, MMListenerInterface, AcqSettingsListener, ImageCacheListener, IDataProcessorListener {//, LiveModeListener {

    //gui, core, acqEngine, mmplugins
    private final ScriptInterface app;
    private boolean imagePipelineSupported;
    private CMMCore core;
    private IAcquisitionEngine2010 acqEng2010;
    private final static String MMPLUGINSDIR="mmplugins";
    private final static String DEVICE_CONTROL_DIR = "Device_Control";
    private final static String STAGE_CONTROL_FILE = "StageControl.jar";
    
    //Dialogs
    private RefPointListDialog refPointListDialog;
    private MergeAreasDlg mergeAreasDialog;
    private CameraRotDlg cameraRotDialog;
    private ZOffsetDlg zOffsetDialog;
    private MMPlugin stageControlPlugin;

    //Monitors and Task Executors
    private ThreadPoolExecutor retilingExecutor;
    private DisplayUpdater displayUpdater;
    private final LiveModeMonitor liveModeMonitor;
    private final StagePosMonitor stageMonitor;
    private AcquisitionTask acquisitionTask;
//    private final List<MMListenerInterface> MMListeners_ = (List<MMListenerInterface>) Collections.synchronizedList(new ArrayList<MMListenerInterface>());
//    private TileManager tileManager;

    //MM Config Paramters
    private String objectiveDevStr;
    private String objectivePropStr;
    private String zStageLabel;
    private String xyStageLabel;
    private List<String> availableObjectives;
    private Rectangle cameraROI;
    private Detector currentDetector;
    
    //Settings
    private File expSettingsFile;
    private String imageDestPath;
    private String dataProcessorPath;
    private AcqLayout acqLayout;
    private List<AcqSetting> acqSettings;
   // private Map<String,String> sequenceDirMap;
//    private int cAcqSettingIdx;
    private AcqSetting currentAcqSetting;
    private TilingSetting prevTilingSetting;
    private String prevObjLabel;
    
    //Operational Status
    private boolean instrumentOnline;
    private boolean updatingAcqSettingTable=false;
    private boolean moveToMode;
    private boolean zoomMode;
    private boolean selectMode;
    private boolean commentMode;
    private boolean mergeAreasMode;
    private boolean marking; //true in selectMode and annotationMode when left mouse button pressed and dragging
    private Point markStartScreenPos; //screen pos when left mouse button pressed
    private Point markEndScreenPos; //screeen pos when left mouse button released; dragging: markStartScreenPos != markEnScreenPos 
    private boolean isLeftMouseButton;
    private boolean isRightMouseButton;
    private boolean isInitializing = false;
    private boolean isAcquiring = false;
    private boolean isProcessing = false;
    private boolean isAborted = false;
    private boolean isWaiting = false;//true when user pressed 'Acquire' and app is waiting for AcquisitionTask to start acquiring at desired time
    private boolean recalculateTiles = false;
    private boolean calculating;
    private boolean retilingAborted;
    private Area lastArea;
    
    private final Cursor zoomCursor;
    private static final Cursor moveToCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
    private static final Cursor normCursor = new Cursor(Cursor.DEFAULT_CURSOR);
    private static final String NOT_CALIBRATED = "<???>";
    private static final String NOT_SPECIFIED = "<not specified>";
    private static final String NOT_SAVED = "not saved";
    private static final String SELECTING_AREA = "Selecting Area...";
    private static final String ADJUSTING_SETTINGS = "Adjusting Settings...";
    
    private static final double MAX_EXPOSURE = 2000;
    private static final double MAX_SATURATION = 0.015;
    
    protected static final String TAG_ROOT_DIR = "ROOT_DIR";
    protected static final String TAG_EXP_BASE_NAME = "EXP_BASE_NAME";
    protected static final String TAG_EXP_SETTING = "EXPERIMENT_SETTING";
    protected static final String TAG_VERSION="VERSION";
    protected static final String TAG_NAME="NAME";
    protected static final String TAG_STORAGE_DESTINATION="STORAGE_DESTINATION";
    protected static final String TAG_EXP_NAME="EXPERIMENT_NAME";
    protected static final String TAG_LAYOUT_FILE="LAYOUT_FILE";
    protected static final String TAG_ACQ_SETTING_FILE="ACQ_SETTING_FILE";
    protected static final String TAG_PROCESSOR_TREE_FILE="PROCESSOR_TREE_FILE";
    
    //Menu items
    private static final String CMD_NEW_DATA = "Acquire new data";
    private static final String CMD_REVIEW_DATA = "Review data";
    private static final String CMD_CAMERA_ROTATION = "Check camera rotation";
    private static final String CMD_Z_OFFSET = "Set Z-Offset";
    private static final String CMD_MANAGE_LAYOUT = "Manage plate layout";

    @Override
    public void slmExposureChanged(String string, double d) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    private class AcquisitionTask extends TimerTask {

        private final AcqSetting acqSetting;
        private Autofocus autofocus;
        private final ImageCacheListener imageListener;
        private int totalImages;
        private boolean hasStarted;
        private File sequenceDir;
        private SequenceSettings mdaSettings;
        private PositionList posList;
        private DefaultMutableTreeNode mainImageStorageNode;
        private final ScriptInterface app_;
        
        AcquisitionTask (ScriptInterface app, AcqSetting setting, AcqLayout layout, ImageCacheListener imgListener, File seqDir) throws MMException {
//            IJ.log("AcquisitionTask.constructor: begin");
            app_=app;
            acqSetting=setting;
            imageListener=imgListener;
            sequenceDir=seqDir;
            hasStarted=false;
            isInitializing=false;
            posList = new PositionList();

            ArrayList<JSONObject> posInfoList = new ArrayList<JSONObject>();

            //tileManager.convertStagePosMap(acqLayout);
            int index=0;
            for (Area a:layout.getAreaArray()) {
                if (a.isSelectedForAcq()) {                   
              //      try {
                        //a.calcTilePositions(tManager,tileWidth, tileHeight, tSetting);
                        a.setAreaIndex(index++);
                        posList = a.addTilePositions(posList, posInfoList, xyStageLabel, zStageLabel, layout);
              //      } catch (InterruptedException ie) {
              //      }
                }
            }
//            IJ.log("AcquisitionTask.constructor: before createMDASettings");
            mdaSettings = createMDASettings(acqSetting);
//            IJ.log("AcquisitionTask.constructor: after createMDASettings");
            if (mdaSettings == null) {
                throw new MMException("Cannot initialize MDA settings.");
            }

            int slices = setting.isZStack() ? setting.getZSlices() : 1;
            int frames = setting.isTimelapse() ? setting.getFrames() : 1;
            totalImages = frames * slices * posList.getNumberOfPositions() * setting.getChannels().size();

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
            Enumeration<DefaultMutableTreeNode> en = setting.getImageProcessorTree().preorderEnumeration();
            int i=0;    
            while (en.hasMoreElements()) {
                    DefaultMutableTreeNode node = en.nextElement();
                    DataProcessor dp=(DataProcessor)node.getUserObject();
                    File procDir=new File (new File(new File (imageDestPath,"Processed"),setting.getName()),"Proc"+String.format("%03d",i)+"-"+dp.getName());
                    if (!procDir.mkdirs()) {
                        throw new MMException("Directories for Processed Images cannot be created.");
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
            isWaiting=true;
//            IJ.log("AcquisitionTask.constructor: end");
        }
        
        public int getTotalImages() {
            return totalImages;
        }
        
        public boolean hasStarted() {
            return hasStarted;
        }
        
        @Override
        public void run() {
            isWaiting=false;
            isInitializing=true;
            Calendar now=Calendar.getInstance();
            if (acqSetting.getStartTime().type == AcqSetting.ScheduledTime.ASAP) {
                IJ.log("Sequence '"+acqSetting.getName()+"' started asap at : "+now.getTime().toString());
            } else {
                IJ.log("Sequence '"+acqSetting.getName()+"' started at:  "+now.getTime().toString()+" ("+Long.toString(now.getTimeInMillis() - scheduledExecutionTime())+" ms delay)");            
            }try {
                acqEng2010 = app_.getAcquisitionEngine2010();
                BlockingQueue<TaggedImage> engineOutputQueue = acqEng2010.run(mdaSettings, false, posList, autofocus);
                JSONObject summaryMetadata = acqEng2010.getSummaryMetadata();
                summaryMetadata.put(ExtImageTags.AREAS,acqLayout.getNoOfSelectedAreas());
                summaryMetadata.put(ExtImageTags.CLUSTERS,acqLayout.getNoOfSelectedClusters());
                summaryMetadata.put(ExtImageTags.STAGE_TO_LAYOUT_TRANSFORM,acqLayout.getStageToLayoutTransform().toString());
                summaryMetadata.put(ExtImageTags.DETECTOR_ROTATION,currentAcqSetting.getFieldOfView().getFieldRotation());
                // Set up the DataProcessor<TaggedImage> sequence                    


                BlockingQueue<TaggedImage> procTreeOutputQueue = ProcessorTree.runImage(engineOutputQueue, 
                        (DefaultMutableTreeNode)acqSetting.getImageProcessorTree().getRoot());

                TaggedImageStorage storage = new TaggedImageStorageDiskDefault(sequenceDir.getAbsolutePath(), true, summaryMetadata);
    //                    TaggedImageStorage storage = new TaggedImageStorageMultipageTiff(sequenceDir.getAbsolutePath(), true, summaryMetadata, true, false, true);
                MMImageCache imageCache = new MMImageCache(storage);
                imageCache.addImageCacheListener(imageListener);
                //setup and start new DisplayUpdater
                displayUpdater = new DisplayUpdater(imageCache, acqSetting.getChannels(),acqSetting.getImagePixelSize());
                displayUpdater.execute();

                if (mainImageStorageNode.getChildCount()>0) {
                    //create fileoutputqueue and ProcessorTree
                    BlockingQueue<File> fileOutputQueue = new LinkedBlockingQueue<File>(1);
                    ProcessorTree.runFile(fileOutputQueue, (DefaultMutableTreeNode)mainImageStorageNode.getChildAt(0));
                    // Start pumping images into the ImageCache
    //                  DefaultTaggedImageSink sink = new DefaultTaggedImageSink(procTreeOutputQueue, imageCache);
                    ExtTaggedImageSink sink = new ExtTaggedImageSink(procTreeOutputQueue, imageCache, fileOutputQueue);
                    sink.start();
                } else {
                    DefaultTaggedImageSink sink = new DefaultTaggedImageSink(procTreeOutputQueue, imageCache);                
                    sink.start();
                }
            } catch (Exception e) {
            } finally {
            }
            isInitializing=false;
            hasStarted=true;
            IJ.log("Sequence '"+acqSetting.getName()+"' running");
        }   
    }
    //WindowListener interface
    @Override
    public void windowOpened(WindowEvent we) {
  /*      if (we.getSource() == cameraRotDialog) {
            IJ.showMessage("cameraRotDialog opened");
        }
        if (we.getSource() == refPointListDialog) {
            IJ.showMessage("refPointListDialog opened");
        }
        if (we.getSource() == zOffsetDialog) {
            IJ.showMessage("zOffsetDialog opened");
        }
        if (we.getSource() == mergeAreasDialog) {
            IJ.showMessage("mergeAreasDialog opened");
        }*/
    }

    @Override
    public void windowClosing(WindowEvent we) {
/*        if (we.getSource() == mergeAreasDialog) {
            IJ.showMessage("mergeAreasDialog closing");
        }        
        if (we.getSource() == refPointListDialog) {
            IJ.showMessage("refPointListDialog closing");
        }
        if (we.getSource() == zOffsetDialog) {
            IJ.showMessage("zOffsetDialog closing");
        }*/
        if (we.getSource() == cameraRotDialog) {
            CameraRotDlg.Measurement m=cameraRotDialog.getResult();
            if (m!=null) {                        
                int result=JOptionPane.showConfirmDialog(null, "Do you want to discard measurements?","Camera Rotation Measurement",JOptionPane.YES_NO_OPTION);
                if (result==JOptionPane.NO_OPTION) {    
//                    setCameraRotation(m.cameraAngle);
//                    cameraRotDialog.dispose();
//                    acqLayoutPanel.repaint();        

//                        } else {
//                            JOptionPane.showMessageDialog(null, "Camera rotation angle could not be determined.");
                } else {
                    cameraRotDialog.dispose();
                }    
                IJ.log("AcqFrame: currentDetector.fieldRotation="+currentDetector.fieldRotation);
            } else {
                cameraRotDialog.dispose();
            }
        }
    }    

    @Override
    public void windowClosed(WindowEvent we) {
        if (we.getSource() == mergeAreasDialog) {
//            IJ.showMessage("mergeAreasDialog closed");
            mergeAreasMode = false;
            areaTable.repaint();
            acqLayoutPanel.revalidate();
            acqLayoutPanel.repaint();
            selectButton.setEnabled(true);
            commentButton.setEnabled(true);
            moveToScreenCoordButton.setEnabled(true);
            setLandmarkButton.setEnabled(true);
            for (Area a : acqLayout.getAreaArray()) {
                a.setSelectedForMerge(false);
            }
            setMergeAreasBounds(null);
            return;
        }        
        if (we.getSource() == cameraRotDialog) {
            if (liveModeMonitor!=null) {
                liveModeMonitor.removeListener(cameraRotDialog);
            }
            cameraRotDialog=null;
        }
/*        if (we.getSource() == refPointListDialog) {
            IJ.showMessage("refPointListDialog closed");
        }*/
        if (we.getSource() == zOffsetDialog) {
            if (liveModeMonitor!=null) {
                liveModeMonitor.removeListener(zOffsetDialog);
            }
            if (stageMonitor!=null) {
                stageMonitor.removeListener(zOffsetDialog);
            }
            zOffsetDialog=null;
        }

    }

    @Override
    public void windowIconified(WindowEvent we) {
    }

    @Override
    public void windowDeiconified(WindowEvent we) {
    }

    @Override
    public void windowActivated(WindowEvent we) {
    }

    @Override
    public void windowDeactivated(WindowEvent we) {
    }

    // AcqSettingsListener interface
    @Override
    public void settingsChanged() {
        //settings in AcqFrame and MDA window are not synchronized at the moment
//        IJ.log("AcqFrame.settingsChanged: ");
        JOptionPane.showMessageDialog(this,"AcqFrame.settingsChanged: acquisition settings changed");
    }

    //ImageCacheListener interface
    @Override
    public void imageReceived(TaggedImage ti) {
/*        try {
            if (!isAborted) {
                JSONObject summary=ti.tags.getJSONObject(MMTags.Root.SUMMARY);
                int frameIdx=ti.tags.getInt(MMTags.Image.FRAME_INDEX);
                int frames=summary.getInt(MMTags.Summary.FRAMES);
                int sliceIdx=ti.tags.getInt(MMTags.Image.SLICE_INDEX);
                int slices=summary.getInt(MMTags.Summary.SLICES);
                int channelIdx=ti.tags.getInt(MMTags.Image.CHANNEL_INDEX);
                int channels=summary.getInt(MMTags.Summary.CHANNELS);
                int positionIdx=ti.tags.getInt(MMTags.Image.POS_INDEX);
                int positions=summary.getInt(MMTags.Summary.POSITIONS);
                timepointLabel.setText(currentAcqSetting.getName() + ", Timepoint: " + Integer.toString(frameIdx + 1));
                progressBar.setValue(progressBar.getValue() + 1);
            }    
        } catch (JSONException ex) {
            IJ.log(ex.getMessage());
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }

    @Override
    public void imagingFinished(String string) {
//        IJ.log("AcqFrame.imagingFinished(Listener): begin. ");
        final AcqSetting acqSetting=currentAcqSetting;
        currentAcqSetting.getTileManager().clearList();
        IJ.log("Finished acquiring sequence: "+currentAcqSetting.getName()+"\n");

        /* 
            string!=null: passed  by ImageCache when it receives a "Poison" image (=acquisition is done)
        
            string==null: used by AcqFrame to indicate acquisition has been aborted 
        */
        if (string!=null) {
            
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    
                    @Override
                    public void run() {
                        final JFrame frame=new JFrame("Image Processing: "+acqSetting.getName());
                        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        frame.setPreferredSize(new Dimension(400,420));
                        frame.setResizable(false);
                        frame.getContentPane().setLayout(new GridLayout(0,1));
                        
                        JLabel label=new JLabel("Active Processors");
                        final JList list=new JList();
                        list.setVisibleRowCount(10);
                        final JScrollPane scrollPane = new JScrollPane(list);
                        scrollPane.setPreferredSize(new Dimension(250, 80));
                        scrollPane.setAlignmentX(LEFT_ALIGNMENT);
                        //scrollPane.add(list);
                        JPanel listPanel = new JPanel();
                        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.PAGE_AXIS));
                        listPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                        listPanel.add(label);
                        listPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                        listPanel.add(scrollPane);

//                                boolean stillRunning=true;
                        final DefaultMutableTreeNode node=acqSetting.getImageProcessorTree();
                        
                        final SwingWorker<Void, String[]> worker=new SwingWorker<Void,String[]>() {

                            @Override
                            protected Void doInBackground() throws Exception {
                                List<String> activeProcs;
                                do {
                                    activeProcs=new ArrayList<String>();
                                    Enumeration<DefaultMutableTreeNode> en=node.preorderEnumeration();
                                    //get list of active processors
                                    while (en.hasMoreElements()) {
                                        DataProcessor dp=(DataProcessor)en.nextElement().getUserObject();
                                        if (//!(dp instanceof ExtDataProcessor) ||
                                                (dp instanceof ExtDataProcessor && !((ExtDataProcessor)dp).getProcName().equals(ProcessorTree.PROC_NAME_IMAGE_STORAGE))) {
                                            if (dp.isAlive()) {
                                                if (dp instanceof ExtDataProcessor) {
                                                    if (!((ExtDataProcessor)dp).isDone()) {
                                                        activeProcs.add(dp.getName());
                                                    } 
                                                } else {
                                                    //                           IJ.log("DataProcessor ALIVE/RUNNING: "+dp.getName());
                                                    activeProcs.add(dp.getName());
                                                }
                                            }    
                                        }
                                    }

                                    try {
                                        String[] procNames=new String[activeProcs.size()];
                                        for (int i=0; i<activeProcs.size(); i++) {
                                            procNames[i]=activeProcs.get(i);
                                        }
                                        publish(procNames);
                                        Thread.sleep(200);
                                    } catch (InterruptedException ex) {
                                        Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                } while (activeProcs.size()>0);
                                return null;
                            }
                            
                            @Override
                            protected void process(List<String[]> chunks) {
                                String[] procNames=chunks.get(chunks.size()-1);
                                if (procNames.length!=list.getModel().getSize())
                                    IJ.log(acqSetting.getName()+": "+procNames.length+" active Processors");
                                //create simple animation to indicate that processors are alive
                                for (int i=0; i<procNames.length; i++) {
                                    long x=1+(System.currentTimeMillis()/1000) % 5;
                                    for (int j=0; j<x; j++) {
                                        procNames[i]+=".";
                                    }
                                }
                                //update list with active processor names
                                list.setListData(procNames);
//                                scrollPane.revalidate();
//                                scrollPane.repaint();
                            }

                            @Override
                            protected void done() {
                                frame.dispose();
                                IJ.log("Finished processing of sequence: "+acqSetting.getName());
                                isProcessing=false;
                            }    
                        };//end SwingWorker
                       
                        final JButton abortButton=new JButton("Abort Processing");
                        abortButton.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                abortButton.setEnabled(false);
                                Enumeration<DefaultMutableTreeNode> en=node.preorderEnumeration();
                                //get list of active processors
                                while (en.hasMoreElements()) {
                                    DataProcessor dp=(DataProcessor)en.nextElement().getUserObject();
                                    dp.requestStop();
                                }
                            }
                        });
                        JPanel buttonPanel=new JPanel();
                        buttonPanel.add(abortButton);

                        frame.getContentPane().add(listPanel, BorderLayout.PAGE_START);
                        frame.getContentPane().add(buttonPanel);
                        frame.pack();
                        frame.setLocationRelativeTo(null);
                        frame.setVisible(true);
                            
                        worker.execute();
                    } //end run
                    
                    
                }); //end invokeLater
                while (isProcessing) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }    
        
        
        int currentIndex=acqSettings.indexOf(currentAcqSetting);
        if ((acqSettings.size() > currentIndex + 1) && !isAborted) {
            acqSettingTable.setRowSelectionInterval(currentIndex + 1, currentIndex + 1);
//        if ((acqSettings.size() > cAcqSettingIdx + 1) && !isAborted) {
//            acqSettingTable.setRowSelectionInterval(cAcqSettingIdx + 1, cAcqSettingIdx + 1);
            runAcquisition(currentAcqSetting);
        } else {
//            tileManager.clearAllSeeds();
            acquireButton.setText("Acquire");
            progressBar.setValue(0);
            isAcquiring = false;
            enableGUI(true);
//            progressBar.setStringPainted(false);
            timepointLabel.setText("Timepoint:");
            //restore saved cameraROI
/*            try {
                core.setROI(cameraROI.x, cameraROI.y, cameraROI.width, cameraROI.height);
            } catch (Exception ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }*/
            //select first sequence setting;
            recalculateTiles = false;
//            acqSettingListBox.setSelectedIndex(0);            
            acqSettingTable.setRowSelectionInterval(0, 0);
            recalculateTiles = true;

//            IJ.log("AcqFrame.imagingFinished(Listener): end");
        }
    }
    //end ImageCacheListener
    
    
    //IDataProcessorListener
    //called by implementations of IDataProcessorNotifier (for example SiteInfoUpdater
    @Override
    public void imageProcessed(final JSONObject metadata, final DataProcessor source) {
        if (source instanceof SiteInfoUpdater && ((SiteInfoUpdater)source).getProcName().equals(ProcessorTree.PROC_NAME_ACQ_ENG)) {
            SwingUtilities.invokeLater(new Runnable(){ 
                @Override
                public void run() {
                    try {
                        JSONObject summary=metadata.getJSONObject(MMTags.Root.SUMMARY);
                        int frameIdx=metadata.getInt(MMTags.Image.FRAME_INDEX);
                        timepointLabel.setText(currentAcqSetting.getName() + ", Timepoint: " + Integer.toString(frameIdx + 1));
                        progressBar.setValue(progressBar.getValue() + 1);
                    } catch (JSONException ex) {
                        Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    }
    //end IDataProcessorListener
    
    private void setCameraRotation(double cameraAngle) {
        if (cameraAngle == FieldOfView.ROTATION_UNKNOWN) {
            String angleStr=JOptionPane.showInputDialog(null, "Camera field rotation could not be determined.\n\n"
                    + "Enter camera field rotation angle. Press 'Cancel' to leave current camera field rotation angle.", 
                    (currentDetector.getFieldRotation() == FieldOfView.ROTATION_UNKNOWN ? 0 :currentDetector.getFieldRotation()/Math.PI*180));
            if (!angleStr.equals("")) //ok button
                try {
                    cameraAngle=Math.PI/180*Double.parseDouble(angleStr);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null,"Invalid number.");
                    return;
                }
            else
                return;  // cancel button
        }
        currentDetector.setFieldRotation(cameraAngle);
        for (AcqSetting setting:acqSettings) {
            setting.getFieldOfView().setFieldRotation(cameraAngle);
        }
        Area.setCameraRot(cameraAngle);
        RefArea.setCameraRot(cameraAngle);

        List<AcqSetting> settingsToUpdate=new ArrayList<AcqSetting>();
        List<Double> newTileOverlaps=new ArrayList<Double>();
        String message="";
        for (AcqSetting setting:acqSettings) {
            if (setting.getTileOverlap() < 0 
                    || (setting.getTilingMode()!=TilingSetting.Mode.FULL && (!setting.isCluster() || (setting.getNrClusterX()==1 && setting.getNrClusterY()==1)))) {
                message=message+"Setting '"+setting.getName()+"': Ok\n";
            } else {    
                if (!acqLayout.hasGaps(setting.getFieldOfView(),setting.getTileOverlap())) {
                    message=message+"Setting '"+setting.getName()+"': Ok.\n";
                } else {//tiling gap
                    double newOverlap=acqLayout.closeTilingGaps(setting.getFieldOfView(), 0.005);
                    
                    //round up to next percentage to ensure that no gaps remain
                    newOverlap = Math.ceil(newOverlap * 100)/100;
                            
                    settingsToUpdate.add(setting);
                    newTileOverlaps.add(newOverlap);
                    message=message+"Setting '"+setting.getName()+"': Tiling gaps, suggested tile overlap: "+Integer.toString((int)Math.ceil(newOverlap*100))+"%.\n";
                } 
            }
        }    
        if (settingsToUpdate.size() > 0) {
            int result=JOptionPane.showConfirmDialog(null,"The current camera field rotation will cause tiling gaps.\n\n"
                        + message+"\n"
                        +"Do you want to use suggested tile overlaps?","Warning", JOptionPane.YES_NO_OPTION);                                                        
            if (result == JOptionPane.YES_OPTION) {
                int i=0;
                for (AcqSetting setting:settingsToUpdate) {
                    setting.setTileOverlap(newTileOverlaps.get(i));
                    if (setting==currentAcqSetting) {
                        tileOverlapField.setText(Integer.toString((int)Math.ceil(newTileOverlaps.get(i)*100)));
                    }
                }
            }
        }
        calcTilePositions(null, currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), ADJUSTING_SETTINGS);
    }
    
    private void showCameraRotDlg(boolean modal) {
        double stepSize;
        try {
            //try to set step size to 50% of field of view --> 50% overlap
            //will use objective set in MM not objective of currentAcqSetting
            stepSize=Math.min(currentDetector.getFullWidth_Pixel(), currentDetector.getFullHeight_Pixel())*core.getPixelSizeUm()/2;
        } catch (Exception ex) {
            stepSize=100;
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
//        IJ.log("AcqFrame.liveModeMonitor: before open"+Integer.toString(liveModeMonitor.getNoOfListeners()));
        if (cameraRotDialog == null) {
            cameraRotDialog = new  CameraRotDlg(
                this,
                app,
                currentAcqSetting.getChannelGroupStr(),
                stepSize,
                modal);
            cameraRotDialog.setIterations(5);
            cameraRotDialog.addWindowListener(this);
            app.addMMListener(cameraRotDialog);
            liveModeMonitor.addListener(cameraRotDialog);
            cameraRotDialog.addCancelButtonListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    CameraRotDlg.Measurement m=cameraRotDialog.getResult();
                    if (m!=null) {                        
                        int result=JOptionPane.showConfirmDialog(null, "Do you want to discard measurements?","Camera Rotation Measurement",JOptionPane.YES_NO_OPTION);
                        if (result==JOptionPane.NO_OPTION) {    
//                            setCameraRotation(m.cameraAngle);
//                            acqLayoutPanel.repaint();        

//                        } else {
//                            JOptionPane.showMessageDialog(null, "Camera rotation angle could not be determined.");
                        } else {
                            cameraRotDialog.dispose();
                        }    
                        IJ.log("AcqFrame: currentDetector.fieldRotation="+currentDetector.fieldRotation);
                    }
                    cameraRotDialog.dispose();
                }
            });
            cameraRotDialog.addOkButtonListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    CameraRotDlg.Measurement m=cameraRotDialog.getResult();
                    if (m!=null) {
                        setCameraRotation(m.cameraAngle);
                        acqLayoutPanel.repaint();        

                    } else {
                        JOptionPane.showMessageDialog(null, "Camera rotation angle could not be determined.");
                        setCameraRotation(FieldOfView.ROTATION_UNKNOWN);
                        acqLayoutPanel.repaint();        
                    }
                    cameraRotDialog.dispose();
                    IJ.log("AcqFrame: currentDetector.fieldRotation="+currentDetector.fieldRotation);
                }
            });
        } else {
            //cameraRotDialog.setChannelGroup(currentAcqSetting.getChannelGroupStr());
            cameraRotDialog.setStageStepSize(stepSize);
        }
        cameraRotDialog.setVisible(true);
//        IJ.log("AcqFrame.liveModeMonitor: after open"+Integer.toString(liveModeMonitor.getNoOfListeners()));
                
    }
    
    private void showZOffsetDlg(boolean modal) {
        if (zOffsetDialog == null) {
            zOffsetDialog = new  ZOffsetDlg(this,app,currentAcqSetting.getChannelGroupStr(),modal);
            zOffsetDialog.addWindowListener(this);
            if (liveModeMonitor!=null) {
                liveModeMonitor.addListener(zOffsetDialog);
            }
            if (stageMonitor!=null) {
                stageMonitor.addListener(zOffsetDialog);
            }
            zOffsetDialog.addApplyListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String groupName=zOffsetDialog.getGroupName();
                    if (!groupName.equals(currentAcqSetting.getChannelGroupStr())) {
                        JOptionPane.showMessageDialog(null, "Group name mismatch. Z-offset settings cannot be added to current channel table.");
                    }
                    List<ZOffsetDlg.ChannelData> cdList=zOffsetDialog.getConfigData();
                    if (cdList!=null) {
                        for (ZOffsetDlg.ChannelData cd:cdList) {
                            if (cd.isSet()) {
                                boolean found=false;
                                for (Channel ch:currentAcqSetting.getChannels()) {
                                    if (ch.getName().equals(cd.getConfigName())) {
                                        ch.setZOffset(cd.getZOffset());
                                        if (ch.getExposure() != cd.getExposure()) {
                                            int result=JOptionPane.showConfirmDialog(null, "Exposure for channel "+ch.getName() + " has changed.\n\n"
                                                    +"Do you want to update the exposure setting?", "AutoImag: Set Z-Offset",JOptionPane.YES_NO_OPTION);
                                            if (result==JOptionPane.YES_OPTION) {
                                                ch.setExposure(cd.getExposure());
                                            }
                                        }
                                        found=true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    int result=JOptionPane.showConfirmDialog(null, "Do you want to add configuration "+cd.getConfigName()+" to the channel selection", "AutoImag: Set Z-Offset",JOptionPane.YES_NO_OPTION);
                                    if (result==JOptionPane.YES_OPTION) {
                                        currentAcqSetting.getChannels().add(new Channel(cd.getConfigName(),cd.getExposure(),cd.getZOffset(),new Color(127,127,127)));
//                                        channelTable.setModel(new ChannelTableModel(currentAcqSetting.getChannels()));
                                        initializeChannelTable(currentAcqSetting);
                                    }
                                }
                            }
                        }
                    }    
                }
            });
        }
        zOffsetDialog.setGroupData(currentAcqSetting.getChannelGroupStr(),zOffsetDialog.convertChannelList(currentAcqSetting.getChannels()),true);
//        zOffsetDialog.updateConfigData(currentAcqSetting.getChannelGroupStr(),zOffsetDialog.convertChannelList(currentAcqSetting.getChannels()));
        zOffsetDialog.selectGroup(currentAcqSetting.getChannelGroupStr());
        zOffsetDialog.setVisible(true);
    }

    private void showManagePlateLayout(boolean modal) {
        PlateConfiguration plateConfig=new PlateConfiguration();
        LayoutPlateManagerDlg plateDialog=new LayoutPlateManagerDlg(this, modal);
        plateConfig.fileLocation=Prefs.getHomeDir();
        plateDialog.setConfiguration(plateConfig);
        plateDialog.setVisible(true);
        
        
    /*    if (zOffsetDialog == null) {
            zOffsetDialog = new  ZOffsetDlg(this,app,currentAcqSetting.getChannelGroupStr(),modal);
            zOffsetDialog.addWindowListener(this);
            if (liveModeMonitor!=null) {
                liveModeMonitor.addListener(zOffsetDialog);
                    }
            if (stageMonitor!=null) {
                stageMonitor.addListener(zOffsetDialog);
                                            }
            zOffsetDialog.addApplyListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String groupName=zOffsetDialog.getGroupName();
                    if (!groupName.equals(currentAcqSetting.getChannelGroupStr())) {
                        JOptionPane.showMessageDialog(null, "Group name mismatch. Z-offset settings cannot be added to current channel table.");
                    }
                    List<ZOffsetDlg.ChannelData> cdList=zOffsetDialog.getConfigData();
                    if (cdList!=null) {
                        for (ZOffsetDlg.ChannelData cd:cdList) {
                            if (cd.isSet()) {
                                boolean found=false;
                                for (Channel ch:currentAcqSetting.getChannels()) {
                                    if (ch.getName().equals(cd.getConfigName())) {
                                        ch.setZOffset(cd.getZOffset());
                                        if (ch.getExposure() != cd.getExposure()) {
                                            int result=JOptionPane.showConfirmDialog(null, "Exposure for channel "+ch.getName() + " has changed.\n\n"
                                                    +"Do you want to update the exposure setting?", "AutoImag: Set Z-Offset",JOptionPane.YES_NO_OPTION);
                                            if (result==JOptionPane.YES_OPTION) {
                                                ch.setExposure(cd.getExposure());
                                            }
                                        }
                                        found=true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    int result=JOptionPane.showConfirmDialog(null, "Do you want to add configuration "+cd.getConfigName()+" to the channel selection", "AutoImag: Set Z-Offset",JOptionPane.YES_NO_OPTION);
                                    if (result==JOptionPane.YES_OPTION) {
                                        currentAcqSetting.getChannels().add(new Channel(cd.getConfigName(),cd.getExposure(),cd.getZOffset(),new Color(127,127,127)));
//                                        channelTable.setModel(new ChannelTableModel(currentAcqSetting.getChannels()));
                                        initializeChannelTable(currentAcqSetting);
                                    }
                                }
                            }
                        }
                    }    
                }
            });
        }
        zOffsetDialog.setGroupData(currentAcqSetting.getChannelGroupStr(),zOffsetDialog.convertChannelList(currentAcqSetting.getChannels()),true);
//        zOffsetDialog.updateConfigData(currentAcqSetting.getChannelGroupStr(),zOffsetDialog.convertChannelList(currentAcqSetting.getChannels()));
        zOffsetDialog.selectGroup(currentAcqSetting.getChannelGroupStr());
        zOffsetDialog.setVisible(true);*/
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(CMD_CAMERA_ROTATION)) {
            showCameraRotDlg(false);
        } else
        if (e.getActionCommand().equals(CMD_Z_OFFSET)) {
            showZOffsetDlg(false);
        }
        if (e.getActionCommand().equals(CMD_MANAGE_LAYOUT)) {
            showManagePlateLayout(true);
        }
    }

    
    //ILiveListener
    @Override
    public void liveModeChanged(final boolean isLive) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                liveButton.setText(isLive ? "Stop Live" : "Live");
                snapButton.setEnabled(!isLive && !calculating && !isAcquiring);
                autoExposureButton.setEnabled(!isLive && !calculating && !isAcquiring);
        //        acquireButton.setEnabled(!isLive && !calculating && !isAcquiring && acqLayout.getNoOfMappedStagePos() > 0);
            }
        });
    }
    //end ILiveListener
    
    
    //IMergeAreasListener
    @Override
    public void mergeAreaSelectionChanged(List<Area> mergingAreas) {
       if (mergingAreas!=null & mergingAreas.size()>1) {
            double minX=mergingAreas.get(0).getTopLeftX();
            double maxX=minX+mergingAreas.get(0).getWidth();
            double minY=mergingAreas.get(0).getTopLeftY();
            double maxY=minY+mergingAreas.get(0).getHeight();
            double z=0;
            for (Area area:mergingAreas) {
//            for (int row=1;row<atm.getRowCount();row++) {
                minX=Math.min(minX, area.getTopLeftX());
                minY=Math.min(minY, area.getTopLeftY());
                maxX=Math.max(maxX, area.getTopLeftX()+area.getWidth());
                maxY=Math.max(maxY, area.getTopLeftY()+area.getHeight());
            }
            setMergeAreasBounds(new Rectangle2D.Double(minX,minY,maxX-minX,maxY-minY));
       } else
            setMergeAreasBounds(null);
    }

    @Override
    public void mergeAreas(List<Area> mergingAreas) {
        if (mergingAreas!=null & mergingAreas.size()>1) {
            double minX=mergingAreas.get(0).getTopLeftX();
            double maxX=minX+mergingAreas.get(0).getWidth();
            double minY=mergingAreas.get(0).getTopLeftY();
            double maxY=minY+mergingAreas.get(0).getHeight();
            double z=0;
            for (Area area:mergingAreas) {
//            for (int row=1;row<atm.getRowCount();row++) {
                minX=Math.min(minX, area.getTopLeftX());
                minY=Math.min(minY, area.getTopLeftY());
                maxX=Math.max(maxX, area.getTopLeftX()+area.getWidth());
                maxY=Math.max(maxY, area.getTopLeftY()+area.getHeight());
            }
            List<Area> layoutAreas=acqLayout.getAreaArray();
            Area mergedArea=new RectArea(createNewAreaName(),acqLayout.createUniqueAreaId(),minX, minY, 0, maxX-minX, maxY-minY, false, "");
            layoutAreas.removeAll(mergingAreas);
            layoutAreas.add(mergedArea);
//            removeAllAreas();//in MergeAreasDlg
            acqLayout.setModified(true);
        } else {
            IJ.log(getClass().getName()+": mergingAreas == null");
        }    
    }
    //end IMergeAreaListener
    
    
    //MMListenerInterface
    @Override
    public void propertiesChangedAlert() {
    }

    @Override
    public void propertyChangedAlert(String string, String string1, String string2) {
    }

    @Override
    public void configGroupChangedAlert(String string, String string1) {
    }

    @Override
    public void systemConfigurationLoaded() {
        instrumentOnline=false;
        recalculateTiles=false;
        acqEng2010 = app.getAcquisitionEngine2010();// .getAcquisitionEngine();
        core = app.getMMCore();
        zStageLabel = core.getFocusDevice();
        xyStageLabel = core.getXYStageDevice();
        List<String> groupStrList=Arrays.asList(core.getAvailableConfigGroups().toArray());
        //get cCameraLabel from core; readout chip size, binning (stored in currentDetector)
        //param: null --> set fovRotation to FieldOfView.ROTATION_UNKNOWN
        getCameraSpecs(null); //detector pixel size
        loadAvailableObjectiveLabels();
        
        AcqSetting setting=currentAcqSetting;
        setting.getFieldOfView().setFullSize_Pixel(currentDetector.getFullWidth_Pixel(),currentDetector.getFullHeight_Pixel());
        setting.getFieldOfView().setFieldRotation(currentDetector.getFieldRotation());
        if (setting.getChannelGroupStr() == null 
                || !groupStrList.contains(setting.getChannelGroupStr())) {
            setting.setChannelGroupStr(changeConfigGroupStr("Channel",""));
        }        
        if (!availableObjectives.contains(setting.getObjective())) {
            JOptionPane.showMessageDialog(this, "Objective "+setting.getObjective()+" not found. Choosing alternative.");
            setting.setObjective(availableObjectives.get(0), getObjPixelSize(availableObjectives.get(0)));
        } else {
            setting.setObjective(setting.getObjective(), getObjPixelSize(setting.getObjective()));
        }
        
        initializeChannelTable(currentAcqSetting);
        recalculateTiles=true;
        instrumentOnline=true;
        IJ.log("System Configuration loaded.");
    }

    @Override
    public void pixelSizeChangedAlert(double d) {
    }

    @Override
    public void stagePositionChangedAlert(String string, double d) {
    }

    @Override
    public void xyStagePositionChanged(String string, double d, double d1) {
    }

    @Override
    public void exposureChanged(String string, double d) {
    }
    //end MMListenerInterface
    
    
    //IStageMonitorListener
    @Override
    public void stagePositionChanged(final Double[] stagePos) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                stagePosXLabel.setText((stagePos[0]!=null ? String.format("%1$,.2f", stagePos[0]) : "???"));
                stagePosYLabel.setText((stagePos[1]!=null ? String.format("%1$,.2f", stagePos[1]) : "???"));
                stagePosZLabel.setText((stagePos[2]!=null ? String.format("%1$,.2f", stagePos[2]) : "???"));

        //        if (refPointListDialog != null) {
        //            refPointListDialog.updateStagePosLabel(stagePos[0], stagePos[1], stagePos[2]);
        //        }

        //        ((LayoutPanel) acqLayoutPanel).setCurrentXYStagePos(stagePos[0], stagePos[1]);
                if (stagePos[0]!=null && stagePos[1]!=null) {
                    Area a = acqLayout.getFirstContainingAreaAbs(stagePos[0], stagePos[1], currentAcqSetting.getTileWidth_UM(), currentAcqSetting.getTileHeight_UM());
                    if (a != lastArea) {
                        if (a != null) {
                            areaLabel.setText(a.getName());
                            a.setAcquiring(true);
                        } else {
                            areaLabel.setText("");
                        }
                        if (lastArea != null) {
                            lastArea.setAcquiring(false);
                        }
                        lastArea = a;
                    }
                    acqLayoutPanel.repaint(); 
                }
            }
        });

    }

    //called from RefPointListDialog on EDT
    @Override
    public void referencePointsChanged(List<RefArea> refAreas) {
//        IJ.showMessage("AcqFrame.referencePointChanged");
        setLandmarkFound(acqLayout.getNoOfMappedStagePos() > 0);
        calcTilePositions(
                null, 
                currentAcqSetting.getFieldOfView(), 
                currentAcqSetting.getTilingSetting(), 
                "Adjusting...");        
        acqLayoutPanel.repaint();
    }

    @Override
    public void selectedRefPointChanged(RefArea refArea) {
        acqLayoutPanel.repaint();
    }

    public class DisplayUpdater extends SwingWorker<Void, TaggedImage> implements ImageCacheListener {

        private boolean finished;
        private final List<ImagePlus> impList;
        private final ImageCache imageCache;
        private double pixelSize;

        public DisplayUpdater(ImageCache ic, List<Channel> channels, Double pixelSize) {
            super();
            finished = false;
            impList = new ArrayList<ImagePlus>();
/*            for (Channel c : channels) {
                ImagePlus imp=new ImagePlus(c.getName());
                Calibration cal=imp.getCalibration();
                cal.setUnit("um");
                cal.pixelWidth = pixelSize;
                cal.pixelHeight = pixelSize;                
                imp.setCalibration(cal);
                impList.add(imp);
            }*/
            imageCache=ic;
            imageCache.addImageCacheListener(this);
            this.pixelSize=pixelSize;
        }

        @Override
        protected Void doInBackground() {
            while (!finished) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return null;
        }

        @Override
        protected void process(List<TaggedImage> images) {
            if (!finished) {
                for (TaggedImage lastImage:images) {
                    //TaggedImage lastImage = images.get(images.size() - 1);

                    final JSONObject metadata = lastImage.tags;
                    final Object pixel = lastImage.pix;
                    ImageProcessor ip = Utils.createImageProcessor(lastImage);
                    if (ip!=null) {
                        boolean newWindow=false;
                        ImagePlus imp=null;
                        try {
                            int index = metadata.getInt(MMTags.Image.CHANNEL_INDEX);
                            if (index>=impList.size()) {
                                imp=new ImagePlus(metadata.getString(MMTags.Image.CHANNEL_NAME));
                                impList.add(imp);
                                Calibration cal=imp.getCalibration();
                                cal.setUnit("um");
                                cal.pixelWidth = pixelSize;
                                cal.pixelHeight = pixelSize;                
                                imp.setCalibration(cal);
                                newWindow=true;
                            } else {
                                imp = impList.get(index);
                                newWindow=false;
                            }    
                            JSONArray color=metadata.getJSONObject(MMTags.Root.SUMMARY).getJSONArray(MMTags.Summary.COLORS);
                            if (color!=null)
                                ip.setLut(LUT.createLutFromColor(new Color(color.getInt(index))));
                        } catch (JSONException je) {
                            IJ.log("DisplayUpdater.process: JSONException - cannot parse image metadata title");
                        }
                        imp.setDisplayRange(0, 1024);
                        imp.setProcessor(ip);
                        try {
                            imp.setTitle(metadata.getString(MMTags.Image.CHANNEL) + ": Area " + metadata.getString(ExtImageTags.AREA_NAME) + ", t" + (metadata.getInt(MMTags.Image.FRAME_INDEX)) + ", z" + (metadata.getInt(MMTags.Image.SLICE_INDEX)));
                        } catch (JSONException je) {
                            IJ.log("DisplayUpdater.process: JSONException - cannot parse image title");
                        }
                        imp.show();
/*                        ImageWindow window=imp.getWindow();
                        if (window!=null && newWindow) {
                            window.setSize((int)window.getSize().getWidth()/2,(int)window.getSize().getHeight()/2);
                            imp.getCanvas().fitToWindow();
                        }*/
                    }
                }
            }
        }

        @Override
        protected void done() {
            imageCache.removeImageCacheListener(this);
            for (int i = impList.size() - 1; i >= 0; i--) {
                impList.get(i).close();
            }
            impList.clear();
            IJ.log("DisplayUpdater.done.");

        }

        @Override
        public void imageReceived(TaggedImage ti) {
            if (!finished) {
                publish(ti);
            }
        }

        @Override
        public void imagingFinished(String string) {
            finished = true;
        }
    }

    public class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.out.println(r.toString() + " is rejected");
        }
    }



    public class TileCalcMonitor extends SwingWorker<Void, Integer> {

        private final ThreadPoolExecutor executor;
//        private final ThreadPoolExecutor executor;
        private final List<Future<Integer>> resultList;
        private final JProgressBar progressBar;
        private final String command;
        private final Object restoreObj;
        // private long startMS;

        public TileCalcMonitor(ThreadPoolExecutor executor, List<Future<Integer>> resultList, JProgressBar pb, String cmd, Object restoreObj) {
            this.executor = executor;
            this.resultList = resultList;
            this.progressBar = pb;
            this.command = cmd;
            this.restoreObj = restoreObj;
            if (pb != null) {
                this.progressBar.setMinimum(0);
                this.progressBar.setMaximum(100);
                this.progressBar.setString(cmd);
                this.progressBar.setStringPainted(true);
            }
            //   startMS=start;
        }

        @Override
        protected Void doInBackground() throws Exception {
            while (!executor.isTerminated() && !this.isCancelled()) {
                publish((int) Math.round((double) executor.getCompletedTaskCount() / executor.getTaskCount() * 100));
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                }
            }
            return null;
        }

        @Override
        protected void process(final List<Integer> chunks) {
            if (progressBar != null) {
                /*                Integer progress=chunks.get(chunks.size()-1);
                 progressBar.setValue(progress);
                 progressBar.repaint();*/
                for (int progress : chunks) {
                    progressBar.setValue(progress);
                    progressBar.repaint();
                }
            }
        }

        @Override
        protected void done() {
            if (progressBar != null) {
                progressBar.setValue(0);
                progressBar.setString("");
                progressBar.repaint();
            }
            calculating = false;
            boolean successful=true;
            if (!retilingAborted) {
                //check if all areas tiled successfully (Future<Integer> in resultList >=0;
                for (Future<Integer> tiles:resultList) {
                    try {
                        if (tiles.get()==-1) {
                            JOptionPane.showMessageDialog(null, "Tiling unsuccessful. Reverting to previous settings.");
                            successful=false;
    //                        cancelCalculateTiles();
                            break;
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (ExecutionException ex) {
                        Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            if (retilingAborted || !successful) {
                recalculateTiles = false;
                if (command.equals(ADJUSTING_SETTINGS)) {
                    currentAcqSetting.setObjective(prevObjLabel, getObjPixelSize(prevObjLabel));
                    currentAcqSetting.setTilingSetting(prevTilingSetting);
                    ((LayoutPanel) acqLayoutPanel).setAcqSetting(currentAcqSetting, false);
                    recalculateTiles = true;
                    updateAcqSettingTab(currentAcqSetting);
                    calcTilePositions(null, currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), "Restoring...");
                } else if (command.equals(SELECTING_AREA) && restoreObj != null) {
                    AreaTableModel atm = (AreaTableModel) areaTable.getModel();
                    for (Area area : ((List<Area>) restoreObj)) {
                        int id = area.getId();
                        for (int j = 0; j < atm.getRowCount(); j++) {
                            int idInRow = (Integer) atm.getValueAt(j, 4);
                            // IJ.log(Integer.toString(id)+", "+Integer.toString(idInRow));
                            if (idInRow == id) {
                                atm.setValueAt(false, j, 0);
                            }
                        }
                    }
                }
                retilingAborted = false;
            } else {
                long totalTiles=0;
/*                for (Future<Integer> result:resultList) {
                    int tiles=0; 
                    try {
                        tiles = result.get();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (ExecutionException ex) {
                        Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    totalTiles=totalTiles+tiles;
                  }*/
                calcTotalTileNumber();
            }    
            enableGUI(true);
            updateAcqLayoutPanel();
        }
    }
    
    
    private class AreaTableModel extends AbstractTableModel {

//        private static final long serialVersionUID = 1L;
        public final String[] COLUMN_NAMES = new String[]{"", "Area", "Tiles", "Comment"};
        private List<Area> areas;

        public AreaTableModel(List<Area> al) {
            super();
            setData(al);
        }

        public void setData(List<Area> al) {
            if (al == null) {
                al = new ArrayList<Area>();
            }
            this.areas = al;
        }

        public List<Area> getAreaList() {
            return areas;
        }

        @Override
        public int getRowCount() {
            return areas.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return COLUMN_NAMES[columnIndex];
        }

        @Override
        public Class getColumnClass(int colIndex) {
            return getValueAt(0, colIndex).getClass();
        }

        @Override
        public Object getValueAt(int rowIndex, int colIndex) {
            Area a;
            if (areas != null & rowIndex < areas.size()) {
                a = areas.get(rowIndex);
                if (colIndex == 0) {
                    return a.isSelectedForAcq();
                } else if (colIndex == 1) {
                    return a.getName();
                } else if (colIndex == 2) {
                    DecimalFormat df = new DecimalFormat("###,###,##0");
                    if (a.isSelectedForAcq()) {
                        if (!a.hasUnknownTileNum())
                            return df.format(a.getTileNumber());
                        else
                            return "???";
                    } else {
                        return df.format(0);
                    }
                } else if (colIndex == 3) {
                    return a.getComment();
                } else if (colIndex == 4) {
                    return a.getId();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int colIndex) {
            return (colIndex == 0 || colIndex == 1 | colIndex == 3);
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int colIndex) {
            Area area;
            if (areas != null & rowIndex < areas.size()) {
                area = areas.get(rowIndex);
                switch (colIndex) {
                    case 0: {
                        area.setSelectedForAcq((Boolean) value);
                        fireTableCellUpdated(rowIndex, colIndex);
                        fireTableCellUpdated(rowIndex, 2);
                        break;
                    }
                    case 1: {
                        area.setName(new String((String) value));
                        fireTableCellUpdated(rowIndex, colIndex);
                        break;
                    }
                    case 3: {
                        area.setComment(new String((String) value));
                        fireTableCellUpdated(rowIndex, colIndex);
                        break;
                    }
                }
            }
        }

        public void addRow(Object value) {
            Area a = (Area) value;
            areas.add(a);
            fireTableRowsInserted(getRowCount(), getRowCount());
        }

        public Area getRowData(int rowIdx) {
            if (rowIdx >= 0 && rowIdx < areas.size()) {
                return areas.get(rowIdx);
            } else {
                return null;
            }
        }

        public int rowDown(int[] rowIdx) {
            //           IJ.log("AreaTableModel.rowDown: "+Integer.toString(rowIdx[0])+", "+Integer.toString(rowIdx[rowIdx.length-1])+", "+Integer.toString(rowIdx.length));
            if (rowIdx.length == 1 & rowIdx[0] < getRowCount() - 1) {
                Collections.swap(areas, rowIdx[0], rowIdx[0] + 1);
                fireTableRowsUpdated(rowIdx[0], rowIdx[0] + 1);
                return rowIdx[0] + 1;
            } else if (rowIdx[0] >= 0 && rowIdx[rowIdx.length - 1] < getRowCount() - 1) {
                Area a = acqLayout.getAreaById((Integer) getValueAt(rowIdx[rowIdx.length - 1] + 1, 4));
                areas.add(rowIdx[0], a.duplicate());
                areas.remove(rowIdx[rowIdx.length - 1] + 2);
                /*                for (int row=rowIdx[0]; row<=rowIdx[rowIdx.length-1]+1; row++) {   
                 for (int i=0; i<=3; i++) {
                 fireTableCellUpdated(row,i);
                 }
                 }*/
                fireTableRowsUpdated(rowIdx[0], rowIdx[rowIdx.length - 1] + 1);
                return rowIdx[0] + 1;
            }
            return rowIdx[0];
        }

        public int rowUp(int[] rowIdx) {
//            IJ.log("AreaTableModel.rowUp: "+Integer.toString(rowIdx[0])+", "+Integer.toString(rowIdx[rowIdx.length-1])+", "+Integer.toString(rowIdx.length));
            if (rowIdx.length == 1 & rowIdx[0] > 0) {
                Collections.swap(areas, rowIdx[0], rowIdx[0] - 1);
                fireTableRowsUpdated(rowIdx[0] - 1, rowIdx[0]);
                return rowIdx[0] - 1;
            } else if (rowIdx[0] > 0 && rowIdx[rowIdx.length - 1] < getRowCount()) {
                Area a = acqLayout.getAreaById((Integer) getValueAt(rowIdx[0] - 1, 4));
                areas.add(rowIdx[rowIdx.length - 1] + 1, a.duplicate());
                areas.remove(rowIdx[0] - 1);
                /*                for (int row=rowIdx[0]-1; row<=rowIdx[rowIdx.length-1]; row++) {   
                 for (int i=0; i<=3; i++) {
                 fireTableCellUpdated(row,i);
                 }
                 }*/
                fireTableRowsUpdated(rowIdx[0] - 1, rowIdx[rowIdx.length - 1]);
                return rowIdx[0] - 1;
            }
            return rowIdx[0];
        }

        public void removeRow(Object element) {
            for (int i = 0; i < areas.size(); i++) {
                if (((Area) element).getId() == areas.get(i).getId()) {
                    areas.remove(i);
                    fireTableRowsDeleted(i, i);
                }
            }
        }

        public void removeRows(int[] rowIdx) {
            for (int i = rowIdx[rowIdx.length - 1]; i >= rowIdx[0]; i--) {
                areas.remove(i);
            }
            fireTableRowsDeleted(rowIdx[0], rowIdx[rowIdx.length - 1]);
        }

        private void updateTileCell(int rowIndex) {
            fireTableCellUpdated(rowIndex, 2);
        }
    }
    // end AreaTableModel

    
    
    class TableHeaderSelector extends MouseAdapter  {  
//        TableHeaderEditor editor;  

        public TableHeaderSelector(JTable t)  {  
//            editor = new TableHeaderEditor(this);  
        }  

        @Override
        public void mousePressed(MouseEvent e)  {  
            JTableHeader th = (JTableHeader)e.getSource();  
            int col= th.columnAtPoint(e.getPoint());
            if(col==0) {//th.getHeaderRect(col).contains(e.getPoint())) {  
                TableColumn column = th.getColumnModel().getColumn(col);  
                String oldValue = (String)column.getHeaderValue();  
                //String value = (String)editor.showEditor(th, oldValue);
                
                StrVector groups=core.getAvailableConfigGroups();
                String message = "Select configuration group that contains channel definitions";  
                String value = (String)JOptionPane.showInputDialog(th.getTable(),  
                                                    message,  
                                                    "Channel Group Selector",  
                                                    JOptionPane.INFORMATION_MESSAGE,  
                                                    null,  
                                                    groups.toArray(),  
                                                    currentAcqSetting.getChannelGroupStr());  

                if (!value.equals(oldValue)) {
                    currentAcqSetting.setChannelGroupStr(value);
                    initializeChannelTable(currentAcqSetting);
                }
            }    
        }  
    }  
     
    
    private class ChannelTableModel extends AbstractTableModel {

        public final String[] COLUMN_NAMES = new String[]{"Configuration", "Exposure", "z-Offset", "Color"};
        private List<Channel> channels;

        public ChannelTableModel(List<Channel> cl) {
            super();
            setData(cl);
        }

        public void setData(List<Channel> cl) {
            if (cl == null) {
                cl = new ArrayList<Channel>();
            }
            this.channels = cl;
        }

        public List<Channel> getChannelList() {
            return channels;
        }

        @Override
        public int getRowCount() {
            return channels.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return COLUMN_NAMES[columnIndex];
        }

        @Override
        public Class getColumnClass(int colIndex) {
            return getValueAt(0, colIndex).getClass();
        }

        @Override
        public Object getValueAt(int rowIndex, int colIndex) {
            Channel c;
            if (channels != null & rowIndex < channels.size()) {
                c = channels.get(rowIndex);
                if (colIndex == 0) {
                    return c.getName();
                } else if (colIndex == 1) {
                    return c.getExposure();
                } else if (colIndex == 2) {
                    return c.getZOffset();
/*                } else if (colIndex == 3) {
                    return c.getStitch();*/
                } else if (colIndex == 3) {
                    return c.getColor();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int colIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int colIndex) {
            Channel c;
            if (channels != null & rowIndex < channels.size()) {
                c = channels.get(rowIndex);
                if (colIndex == 0) {
                    c.setName((String) value);
                    fireTableCellUpdated(rowIndex, colIndex);
                } else if (colIndex == 1) {
                    c.setExposure(new Double((Double) value));
                    fireTableCellUpdated(rowIndex, colIndex);
                } else if (colIndex == 2) {
                    c.setZOffset(new Double((Double) value));
                    fireTableCellUpdated(rowIndex, colIndex);
                } else if (colIndex == 3) {
                    c.setColor((Color) value);
                    fireTableCellUpdated(rowIndex, colIndex);
                }
            }
        }

        public void addRow(Object value) {
            Channel c = (Channel) value;
            channels.add(c);
            fireTableRowsInserted(getRowCount(), getRowCount());
        }

        public Channel getRowData(int rowIdx) {
            if (rowIdx >= 0 && rowIdx < channels.size()) {
                return channels.get(rowIdx);
            } else {
                return null;
            }
        }

        public int rowDown(int[] rowIdx) {
//            IJ.log("ChannelTableModel.rowDown: "+Integer.toString(rowIdx[0])+", "+Integer.toString(rowIdx[rowIdx.length-1])+", "+Integer.toString(rowIdx.length));
            if (rowIdx.length == 1 & rowIdx[0] < getRowCount() - 1) {
                Collections.swap(channels, rowIdx[0], rowIdx[0] + 1);
                fireTableRowsUpdated(rowIdx[0], rowIdx[0] + 1);
                return rowIdx[0] + 1;
            } else if (rowIdx[0] >= 0 && rowIdx[rowIdx.length - 1] < getRowCount() - 1) {
                Channel c = channels.get(rowIdx[rowIdx.length - 1] + 1);
                channels.add(rowIdx[0], c.duplicate());
                channels.remove(rowIdx[rowIdx.length - 1] + 2);
                fireTableRowsUpdated(rowIdx[0], rowIdx[rowIdx.length - 1] + 1);
                return rowIdx[0] + 1;
            }
            return rowIdx[0];
        }

        public int rowUp(int[] rowIdx) {
            //           IJ.log("ChannelTableModel.rowUp: "+Integer.toString(rowIdx[0])+", "+Integer.toString(rowIdx[rowIdx.length-1])+", "+Integer.toString(rowIdx.length));
            if (rowIdx.length == 1 & rowIdx[0] > 0) {
                Collections.swap(channels, rowIdx[0], rowIdx[0] - 1);
                fireTableRowsUpdated(rowIdx[0] - 1, rowIdx[0]);
                return rowIdx[0] - 1;
            } else if (rowIdx[0] > 0 && rowIdx[rowIdx.length - 1] < getRowCount()) {
                Channel c = channels.get(rowIdx[0] - 1);
                channels.add(rowIdx[rowIdx.length - 1] + 1, c.duplicate());
                channels.remove(rowIdx[0] - 1);
                fireTableRowsUpdated(rowIdx[0] - 1, rowIdx[rowIdx.length - 1]);
                return rowIdx[0] - 1;
            }
            return rowIdx[0];
        }

        public void removeRow(Object element) {
            for (int i = 0; i < channels.size(); i++) {
                if (((Channel) element).equals(channels.get(i))) {
                    channels.remove(i);
                    fireTableRowsDeleted(i, i);
                }
            }
        }

        public void removeRows(int[] rowIdx) {
            for (int i = rowIdx[rowIdx.length - 1]; i >= rowIdx[0]; i--) {
                channels.remove(i);
            }
            fireTableRowsDeleted(rowIdx[0], rowIdx[rowIdx.length - 1]);
        }

        public void removeAllRows() {
            for (int i =channels.size()-1; i>=0; i--) {
                channels.remove(i);
            }
            fireTableRowsDeleted(0,channels.size()-1);
        }
        
        public int rowDown(int rowIdx) {
            if (rowIdx >= 0 && rowIdx < channels.size() - 1) {
                Channel channel = channels.get(rowIdx);
                channels.remove(rowIdx);
                channels.add(rowIdx + 1, channel);
                return rowIdx + 1;
            }
            return rowIdx;
        }

        public int rowUp(int rowIdx) {
            if (rowIdx >= 1 && rowIdx < channels.size()) {
                Channel channel = channels.get(rowIdx);
                channels.remove(rowIdx);
                channels.add(rowIdx - 1, channel);
                return rowIdx - 1;
            }
            return rowIdx;
        }

    }
    // end ChannelTableModel

    private class AcqSettingTableModel extends AbstractTableModel {

        public final String[] COLUMN_NAMES = new String[]{"Sequence", "Start Time"};
        private List<AcqSetting> settings;

        public AcqSettingTableModel(List<AcqSetting> al) {
            super();
//            setData(al);
            if (al == null) {
                al = new ArrayList<AcqSetting>();
            }
            this.settings = al;
        }
        
        public void setData(List<AcqSetting> al) {
            if (al == null) {
                al = new ArrayList<AcqSetting>();
            }
            this.settings = al;            
        }
        
        public List<AcqSetting> getAcqSettingList() {
            return settings;
        }

        @Override
        public int getRowCount() {
            return settings.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return COLUMN_NAMES[columnIndex];
        }

        @Override
        public Class getColumnClass(int colIndex) {
            return getValueAt(0, colIndex).getClass();
        }

        @Override
        public Object getValueAt(int rowIndex, int colIndex) {
            AcqSetting s;
            if (settings != null & rowIndex < settings.size()) {
                s = settings.get(rowIndex);
                if (colIndex == 0) {
                    return s.getName();
                } else if (colIndex == 1) {
                    return s.getStartTime();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int colIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int colIndex) {
            AcqSetting s;
            if (settings != null & rowIndex < settings.size()) {
                s = settings.get(rowIndex);
                if (colIndex == 0) {
                    s.setName((String) value);
                    fireTableCellUpdated(rowIndex, colIndex);
                } else if (colIndex == 1) {
                    s.setStartTime((AcqSetting.ScheduledTime) value);
                    fireTableCellUpdated(rowIndex, colIndex);
                }
            }
        }

        public void addRow(Object value, int row) {
            if (row != -1 && row < settings.size()) {
                settings.add(row, (AcqSetting) value);
                fireTableRowsInserted(row, row);
            } else {
                settings.add((AcqSetting) value);
                fireTableRowsInserted(getRowCount(), getRowCount());
            }
        }

        public AcqSetting getRowData(int rowIdx) {
            if (rowIdx >= 0 && rowIdx < settings.size()) {
                return settings.get(rowIdx);
            } else {
                return null;
            }
        }

        public int rowDown(int[] rowIdx) {
            if (rowIdx.length == 1 && rowIdx[0] < getRowCount() - 1) {
                Collections.swap(settings, rowIdx[0], rowIdx[0] + 1);
                fireTableRowsUpdated(rowIdx[0], rowIdx[0] + 1);
                return rowIdx[0] + 1;
            } else if (rowIdx[0] >= 0 && rowIdx[rowIdx.length - 1] < getRowCount() - 1) {
                AcqSetting s = settings.get(rowIdx[rowIdx.length - 1] + 1);
                settings.add(rowIdx[0], s.duplicate());
                settings.remove(rowIdx[rowIdx.length - 1] + 2);
                fireTableRowsUpdated(rowIdx[0], rowIdx[rowIdx.length - 1] + 1);
                return rowIdx[0] + 1;
            }
            return rowIdx[0];
        }

        public int rowUp(int[] rowIdx) {
            if (rowIdx.length == 1 && rowIdx[0] > 0) {
                Collections.swap(settings, rowIdx[0], rowIdx[0] - 1);
                fireTableRowsUpdated(rowIdx[0] - 1, rowIdx[0]);
                return rowIdx[0] - 1;
            } else if (rowIdx[0] > 0 && rowIdx[rowIdx.length - 1] < getRowCount()) {
                AcqSetting s = settings.get(rowIdx[0] - 1);
                settings.add(rowIdx[rowIdx.length - 1] + 1, s.duplicate());
                settings.remove(rowIdx[0] - 1);
                fireTableRowsUpdated(rowIdx[0] - 1, rowIdx[rowIdx.length - 1]);
                return rowIdx[0] - 1;
            }
            return rowIdx[0];
        }

        public void removeRows(int[] rowIdxArray) {
            for (int i = rowIdxArray.length-1; i>=0; i--) {
                AcqSetting setting=settings.get(rowIdxArray[i]);
                DefaultMutableTreeNode root=setting.getImageProcessorTree();
                Enumeration<DefaultMutableTreeNode> en = root.preorderEnumeration();
                while (en.hasMoreElements()) {
                    DefaultMutableTreeNode node = en.nextElement();
                    DataProcessor proc=(DataProcessor)node.getUserObject();
                    if (proc.isAlive()) {
                        IJ.log("Remove AcqSetting '"+setting.getName()+"': requesting DataProcessor '"+proc.getName()+"' to stop");
                        proc.requestStop();
                    }
                    while (proc.isAlive()) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    IJ.log("Remove AcqSetting '"+setting.getName()+"': DataProcessor '"+proc.getName()+"' is not alive");
                }
                settings.remove(rowIdxArray[i]);
            }
            fireTableRowsDeleted(rowIdxArray[0], rowIdxArray[rowIdxArray.length - 1]);
        }

        public int rowDown(int rowIdx) {
            if (rowIdx >= 0 && rowIdx < settings.size() - 1) {
                AcqSetting s = settings.get(rowIdx);
                settings.remove(rowIdx);
                settings.add(rowIdx + 1, s);
                return rowIdx + 1;
            }
            return rowIdx;
        }

        public int rowUp(int rowIdx) {
            if (rowIdx >= 1 && rowIdx < settings.size()) {
                AcqSetting s = settings.get(rowIdx);
                settings.remove(rowIdx);
                settings.add(rowIdx - 1, s);
                return rowIdx - 1;
            }
            return rowIdx;
        }
        
        private void updateTileCell(int rowIndex) {
            fireTableCellUpdated(rowIndex, 1);
        }

    }
    // end AcqSettingTableModel

    class InNumberFilter extends DocumentFilter {

        @Override
        public void insertString(DocumentFilter.FilterBypass fp, int offset, String string, AttributeSet aset)
                throws BadLocationException {
            int len = string.length();
            boolean isValidInteger = true;

            for (int i = 0; i < len; i++) {
                if (!Character.isDigit(string.charAt(i))) {
                    isValidInteger = false;
                    break;
                }
            }
            if (isValidInteger) {
                super.insertString(fp, offset, string, aset);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }

        @Override
        public void replace(DocumentFilter.FilterBypass fp, int offset, int length, String string, AttributeSet aset)
                throws BadLocationException {
            int len = string.length();
            boolean isValidInteger = true;

            for (int i = 0; i < len; i++) {
                if (!Character.isDigit(string.charAt(i))) {
                    isValidInteger = false;
                    break;
                }
            }
            if (isValidInteger) {
                super.replace(fp, offset, length, string, aset);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    class IntNumberVerifier extends InputVerifier {

        private int minValue;
        private int maxValue;

        public IntNumberVerifier(int min, int max) {
            super();
            minValue = min;
            maxValue = max;
        }

        @Override
        public boolean verify(JComponent jc) {
            JTextField tf = (JTextField) jc;
            try {
                int value = Integer.parseInt(tf.getText());
                if (value >= minValue && value <= maxValue) {
                    return true;
                } else {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }

        }
    }
    

    class TilingIntVerifier extends InputVerifier {

        int minVal;
        int maxVal;

        public TilingIntVerifier(int min, int max) {
            super();
            minVal = min;
            maxVal = max;
        }

        @Override
        public boolean verify(JComponent jc) {
            /*            calcTilePositions(null, currentAcgetTileWidth_UMTileWidth(), currentAcgetTileHeight_UMileHeight(),currentAcqSetting.getTilingSetting(), "Verifying...");
             while (calculating) {};*/
//            IJ.showMessage(jc.getName(),"verifying");
            JTextField field = (JTextField) jc;
            boolean passed = false;
            try {
                int n = Integer.parseInt(field.getText());
                passed = (n >= minVal && n <= maxVal);
            } catch (NumberFormatException nfe) {
            }
            return passed;
        }

        @Override
        public boolean shouldYieldFocus(JComponent input) {
            boolean inputOk = verify(input);
            if (!inputOk) {
                JOptionPane.showMessageDialog(null, "Number is out of range (" + Integer.toString(minVal) + "-" + Integer.toString(maxVal) + ")");
                ((JTextField) input).selectAll();
            }
            return inputOk;
        }
    }

    private class DirectionIconListRenderer extends DefaultListCellRenderer {

        private Map<Object, Icon> icons = null;

        public DirectionIconListRenderer(Map<Object, Icon> map) {
            this.icons = map;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Icon icon = icons.get(value);
            label.setText("");
            label.setIcon(icon);
            return label;
        }
    }

    AcqFrame(ScriptInterface gui) {
        this.app = gui;
        IJ.log("Micro-Manager: "+gui.getVersion());
        try {
            /* version Strings on Mac and Windows build are different in nightly build 1.4.18
            - Windows:  1.4.18 XXXXXXXX
            - Mac:      1.4.18-XXXXXXXX
            this may change in future releases, so test for both
            */
            imagePipelineSupported=!versionLessThan("1.4.18", " ");
        } catch (MMScriptException ex) {
            try {
                imagePipelineSupported=!versionLessThan("1.4.18", "-");
            } catch (MMScriptException ex1) {
                imagePipelineSupported=false;
            }
        }
        acqEng2010 = gui.getAcquisitionEngine2010();// .getAcquisitionEngine();
        core = gui.getMMCore();
        zStageLabel = core.getFocusDevice();
        xyStageLabel = core.getXYStageDevice();

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image cursorImage = toolkit.getImage(getClass().getClassLoader().getResource("autoimage/resources/ZoomCursor.png"));
        Point cursorHotSpot = new Point(11, 11);
        zoomCursor = toolkit.createCustomCursor(cursorImage, cursorHotSpot, "Zoom Cursor");

        acqSettings=null;
        recalculateTiles = false;
        zoomMode = false;
        selectMode = false;
        commentMode = false;
        mergeAreasMode = false;

        instrumentOnline = false; //to ensure that during app initialization instrument does not respond 
        initComponents();
        InputVerifier doubleVerifier = new InputVerifier() {

            @Override
            public boolean verify(JComponent input) {
                if (input instanceof JFormattedTextField) {
                    return ((JFormattedTextField)input).isEditValid();
                } else {
                    return true;
                }
                
            }
        };
        zStackBeginField.setInputVerifier(doubleVerifier);
        zStackEndField.setInputVerifier(doubleVerifier);
        zStepSizeField.setInputVerifier(doubleVerifier);
        zSlicesSpinner.setInputVerifier(doubleVerifier);
        
        
        JMenuBar menubar = new JMenuBar();
        
        JMenu expMenu = new JMenu("Experiments");
        JMenuItem newData=new JMenuItem(CMD_NEW_DATA);
        newData.addActionListener(this);
        expMenu.add(newData);
        JMenuItem reviewData=new JMenuItem(CMD_REVIEW_DATA);
        reviewData.addActionListener(this);
        expMenu.add(reviewData);
        
        JMenu utilMenu = new JMenu("Utilities");
        JMenuItem manageLayout=new JMenuItem(CMD_MANAGE_LAYOUT);
        manageLayout.addActionListener(this);
        utilMenu.add(manageLayout);
        JMenuItem checkCamRotation=new JMenuItem(CMD_CAMERA_ROTATION);
        checkCamRotation.addActionListener(this);
        utilMenu.add(checkCamRotation);
        JMenuItem setZOffset=new JMenuItem(CMD_Z_OFFSET);
        setZOffset.addActionListener(this);
        utilMenu.add(setZOffset);
        
        menubar.add(expMenu);
        menubar.add(utilMenu);
        
        setJMenuBar(menubar);
        
        instantiateStageControlPlugin();
        
/*        Rule columnView = new Rule(Rule.HORIZONTAL, true);
        columnView.setPreferredWidth((int)acqLayoutPanel.getPreferredSize().getWidth());
        Rule rowView = new Rule(Rule.VERTICAL, true);
        rowView.setPreferredHeight((int)acqLayoutPanel.getPreferredSize().getHeight());
        layoutScrollPane.setColumnHeaderView(columnView);
        layoutScrollPane.setRowHeaderView(rowView);
*/        
        //initialize comboBox to select TilingMode
        Object[] tilingModeOptions = new Object[]{
            TilingSetting.Mode.FULL,
            TilingSetting.Mode.CENTER,
            TilingSetting.Mode.RANDOM,
            TilingSetting.Mode.RUNTIME};
//            TilingSetting.Mode.FILE};
//        tilingModeComboBox.removeAllItems();
        tilingModeComboBox.setModel(new DefaultComboBoxModel(tilingModeOptions));
//        for (int i = 0; i < tilingModeOptions.length; i++) {
//            tilingModeComboBox.addItem(tilingModeOptions[i]);
//        }

        //initialize combobox to select TilingDirection
        Map<Object, Icon> icons = new HashMap<Object, Icon>();
        icons.put(TilingSetting.DR_TILING, createImageIcon("autoimage/resources/zigzagDR.png", "down and right"));
        icons.put(TilingSetting.UR_TILING, createImageIcon("autoimage/resources/zigzagUR.png", "up and right"));
        icons.put(TilingSetting.DL_TILING, createImageIcon("autoimage/resources/zigzagDL.png", "down and left"));
        icons.put(TilingSetting.UL_TILING, createImageIcon("autoimage/resources/zigzagUL.png", "up and left"));
        icons.put(TilingSetting.RD_TILING, createImageIcon("autoimage/resources/zigzagRD.png", "right and down"));
        icons.put(TilingSetting.LD_TILING, createImageIcon("autoimage/resources/zigzagLD.png", "left and down"));
        icons.put(TilingSetting.RU_TILING, createImageIcon("autoimage/resources/zigzagRU.png", "right and up"));
        icons.put(TilingSetting.LU_TILING, createImageIcon("autoimage/resources/zigzagLU.png", "left and up"));
        Object[] tilingDirOptions = new Object[]{
            TilingSetting.DR_TILING,
            TilingSetting.UR_TILING,
            TilingSetting.DL_TILING,
            TilingSetting.UR_TILING,
            TilingSetting.RD_TILING,
            TilingSetting.LD_TILING,
            TilingSetting.RU_TILING,
            TilingSetting.LU_TILING};
//        tilingDirComboBox.removeAllItems();
//        for (int i = 0; i < tilingDirOptions.length; i++) {
//            tilingDirComboBox.addItem(tilingDirOptions[i]);
//        }
        tilingDirComboBox.setModel(new DefaultComboBoxModel(tilingDirOptions));
        tilingDirComboBox.setRenderer(new DirectionIconListRenderer(icons));

        //get cCameraLabel from core; readout chip size, binning (stored in currentDetector)
        //param: null --> set fovRotation to FieldOfView.ROTATION_UNKNOWN
        getCameraSpecs(null); //detector pixel size

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        //format areaTable
        areaTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        //areaTable.getModel().addTableModelListener(this);

        //
        ((AbstractDocument) clusterXField.getDocument()).setDocumentFilter(new NumberFilter(2, false));
        clusterXField.setInputVerifier(new TilingIntVerifier(1, 20));
        ((AbstractDocument) clusterYField.getDocument()).setDocumentFilter(new NumberFilter(2, false));
        clusterYField.setInputVerifier(new TilingIntVerifier(1, 20));
        ((AbstractDocument) tileOverlapField.getDocument()).setDocumentFilter(new NumberFilter(3, true));
        tileOverlapField.setInputVerifier(new TilingIntVerifier(-500, 50));
        ((AbstractDocument) maxSitesField.getDocument()).setDocumentFilter(new NumberFilter(6, false));
        maxSitesField.setInputVerifier(new TilingIntVerifier(0, 1000));


        //format channelTable 
        channelTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        channelTable.getTableHeader().setReorderingAllowed(false);
        channelTable.getTableHeader().addMouseListener(new TableHeaderSelector(channelTable));

        //Timelapse fields
        ((AbstractDocument) intHourField.getDocument()).setDocumentFilter(new NumberFilter(3, false));
        ((AbstractDocument) intMinField.getDocument()).setDocumentFilter(new NumberFilter(2, false));
        ((AbstractDocument) intSecField.getDocument()).setDocumentFilter(new NumberFilter(2, false));
        ((AbstractDocument) framesField.getDocument()).setDocumentFilter(new NumberFilter(4, false));

        //set AcqOrderComboBox
        for (int i = 0; i < AcqSetting.ACQ_ORDER_LIST.length; i++) {
            acqOrderList.addItem(AcqSetting.ACQ_ORDER_LIST[i]);
        }
        
        //initialize acquisition JProgressBar
        progressBar.setStringPainted(false);

//        acqSettingListBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//        tileManager = new TileManager(null);
        
        loadPreferences();
        loadAvailableObjectiveLabels();
        
        //load last settings
        expSettingsFile = new File(Prefs.getHomeDir(),"LastExpSettings.txt");
        loadExpSettings(expSettingsFile, true);
//        IJ.showMessage("after loadExp "+(acqSettingTable.getColumnModel().getColumn(1).getCellEditor() instanceof StartTimeEditor ? "yes" : "no"));

        //format acgSettingTable
        acqSettingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        acqSettingTable.getSelectionModel().setSelectionInterval(0, 0);
        acqSettingTable.setDefaultEditor(AcqSetting.ScheduledTime.class, new StartTimeEditor());
                
        sequenceTabbedPane.setBorder(BorderFactory.createTitledBorder(
                        "Sequence: "+currentAcqSetting.getName()));
        updateAutofocusToolTip(currentAcqSetting);
/*        try {
            core.setChannelGroup(currentAcqSetting.getChannelGroupStr());
        } catch (Exception ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        }*/

        //initialize processorTreeView
        processorTreeView.setEditable(true);
        DefaultTreeCellRenderer dtcr = new ProcessorTreeCellRenderer();
        processorTreeView.setCellRenderer(dtcr);
        ToolTipManager.sharedInstance().registerComponent(processorTreeView);
        
        recalculateTiles = true;

        setLandmarkFound(false);
        if (acqLayoutPanel != null) {
            ((LayoutPanel) acqLayoutPanel).setShowZProfile(showZProfileCheckBox.isSelected());
        }

        //initialize and start stage and live-mode monitors
        //refresh every 100ms
        stageMonitor = new StagePosMonitor(gui,100);
        stageMonitor.addListener(this);
        stageMonitor.addListener((LayoutPanel)acqLayoutPanel);
        stageMonitor.execute();        
        liveModeMonitor = new LiveModeMonitor(gui,100);
        liveModeChanged(liveModeMonitor.isLive());
        liveModeMonitor.addListener(this);
        liveModeMonitor.execute();

        instrumentOnline = true;
        gui.addMMListener(this);
    }

    
    /* based on MMStudioFrame.versionLessThan
       - changed split character from " " to "-"
       - removed Error messages
    */
    public boolean versionLessThan(String version, String separator) throws MMScriptException {
        try {
            String[] v = app.getVersion().split(separator, 2);
            String[] m = v[0].split("\\.", 3);
            String[] v2 = version.split(separator, 2);
            String[] m2 = v2[0].split("\\.", 3);
            for (int i=0; i < 3; i++) {
               if (Integer.parseInt(m[i]) < Integer.parseInt(m2[i])) {
                  return true;
               }
               if (Integer.parseInt(m[i]) > Integer.parseInt(m2[i])) {
                  return false;
               }
            }
            if (v2.length < 2 || v2[1].equals("") )
               return false;
            if (v.length < 2 ) {
               return true;
            }
            if (Integer.parseInt(v[1]) < Integer.parseInt(v2[1])) {
               return false;
            }
            return true;
        } catch (NumberFormatException ex) {
            throw new MMScriptException ("Format of version String should be \"a.b.c\"");
        }
     } 
    
    private ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL = getClass().getClassLoader().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            IJ.log("AcqFrame.createImageIcon: Icon file not found: " + path);
            return null;
        }
    }

    public void calcTotalTileNumber() {
        long totalTiles=0;
        for (Area a:acqLayout.getAreaArray()) {
            if (a.isSelectedForAcq())
                totalTiles=totalTiles+a.getTileNumber();
        }    
        currentAcqSetting.setTotalTiles(totalTiles);
        ((AcqSettingTableModel)acqSettingTable.getModel()).updateTileCell(acqSettingTable.getSelectedRow());
    }

    public void cleanUp() {
        if (stageMonitor != null) {
            stageMonitor.cancel(true);
        }
//        cameraRotDialog.dispose();
        if (liveModeMonitor!=null) {
            liveModeMonitor.cancel(true);
        }
        if (acqLayout.isModifed()) {
            int save = JOptionPane.showConfirmDialog(null, "Acquisition layout has been modified.\n\nDo you want to save it?", "", JOptionPane.YES_NO_OPTION);
            if (save == JOptionPane.YES_OPTION) {
                saveLayout();
            }
        }
        //restore saved cameraROI
        try {
            core.setROI(cameraROI.x, cameraROI.y, cameraROI.width, cameraROI.height);
        } catch (Exception ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        }

        savePreferences();
        saveExperimentSettings(new File(Prefs.getHomeDir(),"LastExpSettings.txt"));
//        acqEng.removeSettingsListener(this);
    }

    public void setMergeAreasBounds(Rectangle2D.Double rect) {
        if (acqLayoutPanel != null) {
            ((LayoutPanel) acqLayoutPanel).setMergeAreasBounds(rect);
        }
    }

    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        jLabel36 = new javax.swing.JLabel();
        settingsPanel = new javax.swing.JPanel();
        sequenceTabbedPane = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        areaTable = new javax.swing.JTable();
        areaUpButton = new javax.swing.JButton();
        areaDownButton = new javax.swing.JButton();
        newAreaButton = new javax.swing.JButton();
        removeAreaButton = new javax.swing.JButton();
        mergeAreasButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        totalAreasLabel = new javax.swing.JLabel();
        jLabel37 = new javax.swing.JLabel();
        totalTilesLabel = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        timelapseCheckBox = new javax.swing.JCheckBox();
        clusterYField = new javax.swing.JTextField();
        tilingDirComboBox = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        objectiveComboBox = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        tileSizeLabel = new javax.swing.JLabel();
        zStackCheckBox = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        pixelSizeLabel = new javax.swing.JLabel();
        jLabel34 = new javax.swing.JLabel();
        clusterLabel2 = new javax.swing.JLabel();
        maxSitesField = new javax.swing.JTextField();
        maxSitesLabel = new javax.swing.JLabel();
        tileOverlapField = new javax.swing.JTextField();
        jLabel33 = new javax.swing.JLabel();
        autofocusCheckBox = new javax.swing.JCheckBox();
        jLabel12 = new javax.swing.JLabel();
        binningComboBox = new javax.swing.JComboBox();
        jLabel10 = new javax.swing.JLabel();
        clusterCheckBox = new javax.swing.JCheckBox();
        insideOnlyCheckBox = new javax.swing.JCheckBox();
        acqOrderList = new javax.swing.JComboBox();
        autofocusButton = new javax.swing.JButton();
        tilingModeComboBox = new javax.swing.JComboBox();
        clusterXField = new javax.swing.JTextField();
        siteOverlapCheckBox = new javax.swing.JCheckBox();
        clusterLabel1 = new javax.swing.JLabel();
        clearRoiButton = new javax.swing.JButton();
        closeGapsButton = new javax.swing.JButton();
        autofocusImportButton = new javax.swing.JButton();
        acqModePane = new javax.swing.JTabbedPane();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        channelTable = new javax.swing.JTable();
        jPanel6 = new javax.swing.JPanel();
        addChannelButton = new javax.swing.JButton();
        removeChannelButton = new javax.swing.JButton();
        channelUpButton = new javax.swing.JButton();
        channelDownButton = new javax.swing.JButton();
        snapButton = new javax.swing.JButton();
        liveButton = new javax.swing.JButton();
        zOffsetButton = new javax.swing.JButton();
        autoExposureButton = new javax.swing.JButton();
        zStackPanel = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        zStackCenteredCheckBox = new javax.swing.JCheckBox();
        jLabel23 = new javax.swing.JLabel();
        zStackTotalDistLabel = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        reverseButton = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        zSlicesSpinner = new javax.swing.JSpinner();
        zStepSizeField = new javax.swing.JFormattedTextField();
        zStackBeginField = new javax.swing.JFormattedTextField();
        zStackEndField = new javax.swing.JFormattedTextField();
        jPanel3 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        intHourField = new javax.swing.JTextField();
        intMinField = new javax.swing.JTextField();
        intSecField = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        framesField = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        durationText = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        jLabel28 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        processorTreeView = new javax.swing.JTree();
        addImageTagFilterButton = new javax.swing.JButton();
        addChannelFilterButton = new javax.swing.JButton();
        addZFilterButton = new javax.swing.JButton();
        addScriptAnalyzerButton = new javax.swing.JButton();
        removeProcessorButton = new javax.swing.JButton();
        addROIFinderButton = new javax.swing.JButton();
        editProcessorButton = new javax.swing.JButton();
        addAreaFilterButton = new javax.swing.JButton();
        addDataProcFromFileButton = new javax.swing.JButton();
        addImageStorageButton = new javax.swing.JButton();
        addFrameFilterButton = new javax.swing.JButton();
        loadProcTreeButton = new javax.swing.JButton();
        saveProcTreeButton = new javax.swing.JButton();
        loadImagePipelineButton = new javax.swing.JButton();
        sequenceListPanel = new javax.swing.JPanel();
        acqSettingUpButton = new javax.swing.JButton();
        acqSettingDownButton = new javax.swing.JButton();
        deleteAcqSettingButton = new javax.swing.JButton();
        jScrollPane7 = new javax.swing.JScrollPane();
        acqSettingTable = new javax.swing.JTable();
        saveAcqSettingButton = new javax.swing.JButton();
        addAcqSettingButton = new javax.swing.JButton();
        loadAcqSettingButton = new javax.swing.JButton();
        jPanel11 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        experimentTextField = new javax.swing.JTextField();
        loadExpSettingFileButton = new javax.swing.JButton();
        saveExpSettingFileButton = new javax.swing.JButton();
        loadLayoutButton = new javax.swing.JButton();
        saveLayoutButton = new javax.swing.JButton();
        browseImageDestPathButton = new javax.swing.JButton();
        jLabel32 = new javax.swing.JLabel();
        expSettingsFileLabel = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        layoutFileLabel = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        rootDirLabel = new javax.swing.JLabel();
        processProgressBar = new javax.swing.JProgressBar();
        acquireButton = new javax.swing.JButton();
        cancelThreadButton = new javax.swing.JButton();
        statusPanel = new javax.swing.JPanel();
        stagePosXLabel = new javax.swing.JLabel();
        stagePosYLabel = new javax.swing.JLabel();
        jLabel30 = new javax.swing.JLabel();
        areaLabel = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();
        stagePosZLabel = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        timepointLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        setLandmarkButton = new javax.swing.JButton();
        moveToScreenCoordButton = new javax.swing.JToggleButton();
        zoomButton = new javax.swing.JToggleButton();
        cursorLabel = new javax.swing.JLabel();
        selectButton = new javax.swing.JToggleButton();
        commentButton = new javax.swing.JToggleButton();
        showZProfileCheckBox = new javax.swing.JCheckBox();
        stageControlButton = new javax.swing.JButton();
        layoutScrollPane = new javax.swing.JScrollPane();
        acqLayoutPanel = new LayoutPanel(null,null);

        jLabel36.setText("jLabel36");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("AutoImage");
        setBounds(new java.awt.Rectangle(0, 22, 1024, 710));
        setMinimumSize(new java.awt.Dimension(1024, 710));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        settingsPanel.setMaximumSize(new java.awt.Dimension(436, 671));
        settingsPanel.setMinimumSize(new java.awt.Dimension(436, 650));
        settingsPanel.setPreferredSize(new java.awt.Dimension(400, 400));
        settingsPanel.setRequestFocusEnabled(false);
        settingsPanel.setSize(new java.awt.Dimension(0, 649));

        sequenceTabbedPane.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Sequence:", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Lucida Grande", 0, 12))); // NOI18N
        sequenceTabbedPane.setPreferredSize(new java.awt.Dimension(410, 540));

        areaTable.setModel(new AreaTableModel(null));
        areaTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        areaTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(areaTable);

        areaUpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Up.png"))); // NOI18N
        areaUpButton.setToolTipText("Move selected Area(s) up");
        areaUpButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        areaUpButton.setMaximumSize(new java.awt.Dimension(26, 26));
        areaUpButton.setPreferredSize(new java.awt.Dimension(26, 26));
        areaUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                areaUpButtonActionPerformed(evt);
            }
        });

        areaDownButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Down.png"))); // NOI18N
        areaDownButton.setToolTipText("Move selected Area(s) down");
        areaDownButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        areaDownButton.setMaximumSize(new java.awt.Dimension(26, 26));
        areaDownButton.setPreferredSize(new java.awt.Dimension(26, 26));
        areaDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                areaDownButtonActionPerformed(evt);
            }
        });

        newAreaButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/add2.png"))); // NOI18N
        newAreaButton.setToolTipText("Add new Area to Layout");
        newAreaButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        newAreaButton.setMaximumSize(new java.awt.Dimension(26, 26));
        newAreaButton.setMinimumSize(new java.awt.Dimension(24, 26));
        newAreaButton.setPreferredSize(new java.awt.Dimension(26, 26));
        newAreaButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newAreaButtonActionPerformed(evt);
            }
        });

        removeAreaButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/delete.png"))); // NOI18N
        removeAreaButton.setToolTipText("Remove Area from Layout");
        removeAreaButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        removeAreaButton.setMaximumSize(new java.awt.Dimension(26, 26));
        removeAreaButton.setMinimumSize(new java.awt.Dimension(24, 26));
        removeAreaButton.setPreferredSize(new java.awt.Dimension(26, 26));
        removeAreaButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeAreaButtonActionPerformed(evt);
            }
        });

        mergeAreasButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/merge.png"))); // NOI18N
        mergeAreasButton.setToolTipText("Merge Areas");
        mergeAreasButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        mergeAreasButton.setMaximumSize(new java.awt.Dimension(26, 26));
        mergeAreasButton.setPreferredSize(new java.awt.Dimension(26, 26));
        mergeAreasButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mergeAreasButtonActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel3.setText("Selected Areas:");

        totalAreasLabel.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        totalAreasLabel.setText("0");

        jLabel37.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel37.setText("Tiles: ");

        totalTilesLabel.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        totalTilesLabel.setText("0");

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(6, 6, 6)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup()
                        .add(jLabel3)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(totalAreasLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 48, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jLabel37)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(totalTilesLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 337, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, areaDownButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(mergeAreasButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, areaUpButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, removeAreaButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, newAreaButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(totalAreasLabel)
                    .add(jLabel37)
                    .add(totalTilesLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(newAreaButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(0, 0, 0)
                        .add(removeAreaButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(areaUpButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(0, 0, 0)
                        .add(areaDownButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(mergeAreasButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE))
                .add(3, 3, 3))
        );

        sequenceTabbedPane.addTab("Areas", jPanel2);

        timelapseCheckBox.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        timelapseCheckBox.setText("Time-lapse");
        timelapseCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                timelapseCheckBoxItemStateChanged(evt);
            }
        });

        clusterYField.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        clusterYField.setText("jTextField2");
        clusterYField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                clusterYFieldFocusLost(evt);
            }
        });
        clusterYField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clusterYFieldActionPerformed(evt);
            }
        });

        tilingDirComboBox.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        tilingDirComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        tilingDirComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tilingDirComboBoxActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel1.setText("Objective:");

        objectiveComboBox.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        objectiveComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        objectiveComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                objectiveComboBoxActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel4.setText("Tile overlap (%):");

        jLabel5.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel5.setText("Tile:");

        tileSizeLabel.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        tileSizeLabel.setText("jLabel6");

        zStackCheckBox.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        zStackCheckBox.setText("Z-Stack");
        zStackCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                zStackCheckBoxItemStateChanged(evt);
            }
        });
        zStackCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zStackCheckBoxActionPerformed(evt);
            }
        });

        jLabel6.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel6.setText("Order:");

        pixelSizeLabel.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        pixelSizeLabel.setText("jLabel13");

        jLabel34.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel34.setText("Direction:");

        clusterLabel2.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        clusterLabel2.setText("Tiles");

        maxSitesField.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        maxSitesField.setText("jTextField1");
        maxSitesField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                maxSitesFieldFocusLost(evt);
            }
        });
        maxSitesField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                maxSitesFieldActionPerformed(evt);
            }
        });

        maxSitesLabel.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        maxSitesLabel.setText("Site/Cluster #:");

        tileOverlapField.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        tileOverlapField.setText("jTextField1");
        tileOverlapField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                tileOverlapFieldFocusLost(evt);
            }
        });
        tileOverlapField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tileOverlapFieldActionPerformed(evt);
            }
        });

        jLabel33.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel33.setText("Tiling:");

        autofocusCheckBox.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        autofocusCheckBox.setText("Autofocus");
        autofocusCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                autofocusCheckBoxItemStateChanged(evt);
            }
        });

        jLabel12.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel12.setText("Pixel:");

        binningComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        binningComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                binningComboBoxActionPerformed(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel10.setText("Binning:");

        clusterCheckBox.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        clusterCheckBox.setText("Cluster:");
        clusterCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clusterCheckBoxActionPerformed(evt);
            }
        });

        insideOnlyCheckBox.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        insideOnlyCheckBox.setText("Inside only");
        insideOnlyCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                insideOnlyCheckBoxActionPerformed(evt);
            }
        });

        acqOrderList.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        acqOrderList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                acqOrderListActionPerformed(evt);
            }
        });

        autofocusButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/wrench_orange.png"))); // NOI18N
        autofocusButton.setToolTipText("Autofocus Configuration");
        autofocusButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autofocusButtonActionPerformed(evt);
            }
        });

        tilingModeComboBox.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        tilingModeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        tilingModeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tilingModeComboBoxActionPerformed(evt);
            }
        });

        clusterXField.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        clusterXField.setText("jTextField2");
        clusterXField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                clusterXFieldFocusLost(evt);
            }
        });
        clusterXField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clusterXFieldActionPerformed(evt);
            }
        });

        siteOverlapCheckBox.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        siteOverlapCheckBox.setText("Overlapping sites");
        siteOverlapCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                siteOverlapCheckBoxActionPerformed(evt);
            }
        });

        clusterLabel1.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        clusterLabel1.setText("x");

        clearRoiButton.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        clearRoiButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/icons/arrow_refresh.png"))); // NOI18N
        clearRoiButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearRoiButtonActionPerformed(evt);
            }
        });

        closeGapsButton.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        closeGapsButton.setText("Close Gaps");
        closeGapsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeGapsButtonActionPerformed(evt);
            }
        });

        autofocusImportButton.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        autofocusImportButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/icons/arrow_refresh.png"))); // NOI18N
        autofocusImportButton.setToolTipText("Apply autofocus settings");
        autofocusImportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autofocusImportButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel10Layout = new org.jdesktop.layout.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel10Layout.createSequentialGroup()
                .add(3, 3, 3)
                .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel10Layout.createSequentialGroup()
                        .add(autofocusCheckBox)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(autofocusButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 24, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(autofocusImportButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(zStackCheckBox)
                        .add(26, 26, 26)
                        .add(timelapseCheckBox))
                    .add(jPanel10Layout.createSequentialGroup()
                        .add(jLabel6)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(acqOrderList, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 216, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jPanel10Layout.createSequentialGroup()
                        .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(jPanel10Layout.createSequentialGroup()
                                .add(jLabel12)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(pixelSizeLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 70, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(jLabel5))
                            .add(jPanel10Layout.createSequentialGroup()
                                .add(jLabel33)
                                .add(2, 2, 2)
                                .add(tilingModeComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 111, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .add(6, 6, 6)
                        .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel10Layout.createSequentialGroup()
                                .add(tileSizeLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 177, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(clearRoiButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jPanel10Layout.createSequentialGroup()
                                .add(jLabel34)
                                .add(3, 3, 3)
                                .add(tilingDirComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 61, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(insideOnlyCheckBox))))
                    .add(jPanel10Layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(objectiveComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 189, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel10)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(binningComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 61, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel10Layout.createSequentialGroup()
                            .add(jLabel4)
                            .add(3, 3, 3)
                            .add(tileOverlapField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 49, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(3, 3, 3)
                            .add(closeGapsButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 86, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(3, 3, 3)
                            .add(siteOverlapCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel10Layout.createSequentialGroup()
                            .add(clusterCheckBox)
                            .add(0, 0, 0)
                            .add(clusterXField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 48, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(1, 1, 1)
                            .add(clusterLabel1)
                            .add(0, 0, 0)
                            .add(clusterYField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 18, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(0, 0, 0)
                            .add(clusterLabel2)
                            .add(18, 18, 18)
                            .add(maxSitesLabel)
                            .add(3, 3, 3)
                            .add(maxSitesField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(9, Short.MAX_VALUE))
        );

        jPanel10Layout.linkSize(new java.awt.Component[] {clusterXField, clusterYField}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel10Layout.createSequentialGroup()
                .add(0, 0, 0)
                .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(objectiveComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(binningComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(clearRoiButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jLabel12)
                        .add(pixelSizeLabel)
                        .add(jLabel5)
                        .add(tileSizeLabel)))
                .add(2, 2, 2)
                .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(jLabel33)
                    .add(tilingModeComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel34)
                    .add(tilingDirComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(insideOnlyCheckBox))
                .add(3, 3, 3)
                .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(maxSitesLabel)
                        .add(maxSitesField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(clusterLabel2)
                    .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(clusterXField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(clusterCheckBox))
                    .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(clusterYField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(clusterLabel1)))
                .add(3, 3, 3)
                .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(tileOverlapField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(closeGapsButton)
                    .add(siteOverlapCheckBox))
                .add(4, 4, 4)
                .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel6)
                    .add(acqOrderList, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(0, 0, 0)
                .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(autofocusCheckBox)
                    .add(autofocusButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(zStackCheckBox)
                    .add(timelapseCheckBox)
                    .add(autofocusImportButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(0, 0, 0))
        );

        acqModePane.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        acqModePane.setMinimumSize(new java.awt.Dimension(72, 300));

        channelTable.setModel(new ChannelTableModel(null));
        channelTable.setColumnSelectionAllowed(true);
        channelTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane3.setViewportView(channelTable);
        channelTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        addChannelButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/add2.png"))); // NOI18N
        addChannelButton.setToolTipText("Add Channel Configuration");
        addChannelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addChannelButtonActionPerformed(evt);
            }
        });

        removeChannelButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/delete.png"))); // NOI18N
        removeChannelButton.setToolTipText("Remove Channel Configuration");
        removeChannelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeChannelButtonActionPerformed(evt);
            }
        });

        channelUpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Up.png"))); // NOI18N
        channelUpButton.setToolTipText("Move Channel Configuration up");
        channelUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                channelUpButtonActionPerformed(evt);
            }
        });

        channelDownButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Down.png"))); // NOI18N
        channelDownButton.setToolTipText("Move Channel Configuration down");
        channelDownButton.setPreferredSize(new java.awt.Dimension(26, 26));
        channelDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                channelDownButtonActionPerformed(evt);
            }
        });

        snapButton.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        snapButton.setText("Snap");
        snapButton.setToolTipText("Snap Image(s) using selected Channel Configurations ");
        snapButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                snapButtonActionPerformed(evt);
            }
        });

        liveButton.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        liveButton.setText("Live");
        liveButton.setToolTipText("Live Acquisition using selected Channel Configuration");
        liveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                liveButtonActionPerformed(evt);
            }
        });

        zOffsetButton.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        zOffsetButton.setText("z-Offset");
        zOffsetButton.setToolTipText("Live Acquisition using selected Channel Configuration");
        zOffsetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zOffsetButtonActionPerformed(evt);
            }
        });

        autoExposureButton.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        autoExposureButton.setText("Auto-Exp");
        autoExposureButton.setToolTipText("Snap Image(s) using selected Channel Configurations ");
        autoExposureButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoExposureButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel6Layout.createSequentialGroup()
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, snapButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel6Layout.createSequentialGroup()
                                .add(addChannelButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(2, 2, 2)
                                .add(removeChannelButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .add(2, 2, 2)
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, liveButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 46, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jPanel6Layout.createSequentialGroup()
                                .add(channelUpButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(2, 2, 2)
                                .add(channelDownButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(0, 0, Short.MAX_VALUE))))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel6Layout.createSequentialGroup()
                        .add(0, 0, Short.MAX_VALUE)
                        .add(zOffsetButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 94, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(autoExposureButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel6Layout.createSequentialGroup()
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(removeChannelButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(addChannelButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(channelUpButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(channelDownButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(snapButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(liveButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(autoExposureButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(2, 2, 2)
                .add(zOffsetButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel6Layout.linkSize(new java.awt.Component[] {liveButton, snapButton}, org.jdesktop.layout.GroupLayout.VERTICAL);

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(6, 6, 6)
                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 227, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 97, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(6, 6, 6))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(3, 3, 3)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .add(jPanel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 92, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(3, 3, 3))
        );

        acqModePane.addTab("Channels", jPanel4);

        jLabel19.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel19.setText("Begin:");

        jLabel20.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel20.setText("End:");

        jLabel21.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel21.setText("Slices:");

        jLabel22.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel22.setText("Step size:");

        zStackCenteredCheckBox.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        zStackCenteredCheckBox.setText("Centered around reference Z-position");
        zStackCenteredCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                zStackCenteredCheckBoxItemStateChanged(evt);
            }
        });
        zStackCenteredCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zStackCenteredCheckBoxActionPerformed(evt);
            }
        });

        jLabel23.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel23.setText("Total Z Dist:");

        zStackTotalDistLabel.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        zStackTotalDistLabel.setText("0.00");

        jLabel24.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel24.setText("um");
        jLabel24.setMaximumSize(new java.awt.Dimension(18, 15));
        jLabel24.setMinimumSize(new java.awt.Dimension(18, 15));

        jLabel25.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel25.setText("um");
        jLabel25.setMaximumSize(new java.awt.Dimension(18, 15));
        jLabel25.setMinimumSize(new java.awt.Dimension(18, 15));
        jLabel25.setPreferredSize(new java.awt.Dimension(18, 15));

        jLabel26.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel26.setText("um");
        jLabel26.setMaximumSize(new java.awt.Dimension(18, 15));
        jLabel26.setMinimumSize(new java.awt.Dimension(18, 15));

        reverseButton.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        reverseButton.setText("Reverse");
        reverseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reverseButtonActionPerformed(evt);
            }
        });

        jLabel9.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel9.setText("um");

        zSlicesSpinner.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        zSlicesSpinner.setModel(new javax.swing.SpinnerNumberModel(1, 1, 999, 1));
        zSlicesSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                zSlicesSpinnerStateChanged(evt);
            }
        });

        zStepSizeField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0.000"))));
        zStepSizeField.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        zStepSizeField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                zStepSizeFieldPropertyChange(evt);
            }
        });

        zStackBeginField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0.000"))));
        zStackBeginField.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        zStackBeginField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                zStackBeginFieldPropertyChange(evt);
            }
        });

        zStackEndField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0.000"))));
        zStackEndField.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        zStackEndField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                zStackEndFieldPropertyChange(evt);
            }
        });

        org.jdesktop.layout.GroupLayout zStackPanelLayout = new org.jdesktop.layout.GroupLayout(zStackPanel);
        zStackPanel.setLayout(zStackPanelLayout);
        zStackPanelLayout.setHorizontalGroup(
            zStackPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(zStackPanelLayout.createSequentialGroup()
                .add(6, 6, 6)
                .add(zStackPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(zStackPanelLayout.createSequentialGroup()
                        .add(zStackPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(zStackPanelLayout.createSequentialGroup()
                                .add(zStackPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(zStackPanelLayout.createSequentialGroup()
                                        .add(9, 9, 9)
                                        .add(jLabel20))
                                    .add(jLabel19))
                                .add(6, 6, 6)
                                .add(zStackPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(zStackEndField)
                                    .add(zStackBeginField)))
                            .add(zStackPanelLayout.createSequentialGroup()
                                .add(1, 1, 1)
                                .add(reverseButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 101, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .add(6, 6, 6)
                        .add(zStackPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, zStackPanelLayout.createSequentialGroup()
                                .add(zStackPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                    .add(jLabel25, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 24, Short.MAX_VALUE)
                                    .add(jLabel26, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .add(12, 12, 12)
                                .add(zStackPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                    .add(jLabel21)
                                    .add(jLabel22))
                                .add(6, 6, 6))
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, zStackPanelLayout.createSequentialGroup()
                                .add(jLabel23)
                                .add(8, 8, 8)))
                        .add(zStackPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(zStackPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                .add(zSlicesSpinner)
                                .add(zStepSizeField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                            .add(zStackTotalDistLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 69, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(6, 6, 6)
                        .add(zStackPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel24, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel9)))
                    .add(zStackCenteredCheckBox))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        zStackPanelLayout.setVerticalGroup(
            zStackPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, zStackPanelLayout.createSequentialGroup()
                .add(0, 0, 0)
                .add(zStackCenteredCheckBox)
                .add(zStackPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel19)
                    .add(jLabel21, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(zSlicesSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(zStackBeginField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(zStackPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(zStackPanelLayout.createSequentialGroup()
                        .add(zStackPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(zStackPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(jLabel20)
                                .add(jLabel22)
                                .add(jLabel24, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(zStackEndField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .add(2, 2, 2)
                        .add(zStackPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel23)
                            .add(zStackTotalDistLabel)
                            .add(reverseButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel9)))
                    .add(zStepSizeField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(143, 143, 143))
        );

        acqModePane.addTab("Z-Stack", zStackPanel);

        jLabel13.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel13.setText("Interval:");

        intHourField.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        intHourField.setText("0");
        intHourField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                intHourFieldFocusLost(evt);
            }
        });
        intHourField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                intHourFieldActionPerformed(evt);
            }
        });

        intMinField.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        intMinField.setText("0");
        intMinField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                intMinFieldFocusLost(evt);
            }
        });
        intMinField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                intMinFieldActionPerformed(evt);
            }
        });

        intSecField.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        intSecField.setText("0");
        intSecField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                intSecFieldFocusLost(evt);
            }
        });
        intSecField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                intSecFieldActionPerformed(evt);
            }
        });

        jLabel14.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel14.setText("h");

        jLabel15.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel15.setText("min");

        jLabel16.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel16.setText("s");

        jLabel17.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel17.setText("Timepoints:");

        framesField.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        framesField.setText("1");
        framesField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                framesFieldFocusLost(evt);
            }
        });
        framesField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                framesFieldActionPerformed(evt);
            }
        });

        jLabel18.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel18.setText("Duration:");

        durationText.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        durationText.setText("jLabel19");
        durationText.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(8, 8, 8)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jLabel17)
                    .add(jLabel13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 59, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel18))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(framesField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 56, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jPanel3Layout.createSequentialGroup()
                                .add(intHourField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 40, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(3, 3, 3)
                                .add(jLabel14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(intMinField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 40, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(3, 3, 3)
                                .add(jLabel15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(intSecField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 40, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(3, 3, 3)
                                .add(jLabel16)))
                        .addContainerGap())
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, durationText, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(6, 6, 6)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel13)
                    .add(intHourField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(intSecField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(intMinField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel14)
                    .add(jLabel15)
                    .add(jLabel16))
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel17)
                    .add(framesField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(jLabel18)
                        .add(0, 0, Short.MAX_VALUE))
                    .add(durationText, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        acqModePane.addTab("Time-lapse", jPanel3);

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jPanel5Layout.createSequentialGroup()
                        .add(6, 6, 6)
                        .add(acqModePane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 363, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .add(0, 0, 0)
                .add(jPanel10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(6, 6, 6)
                .add(acqModePane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 140, Short.MAX_VALUE)
                .add(6, 6, 6))
        );

        sequenceTabbedPane.addTab("Acq Settings", jPanel5);

        jLabel28.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel28.setText("Image Processors/Analyzers:");

        jScrollPane4.setViewportView(processorTreeView);

        addImageTagFilterButton.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        addImageTagFilterButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Filter_Tag.png"))); // NOI18N
        addImageTagFilterButton.setToolTipText("Filter: Image Tag");
        addImageTagFilterButton.setMaximumSize(new java.awt.Dimension(24, 24));
        addImageTagFilterButton.setMinimumSize(new java.awt.Dimension(24, 24));
        addImageTagFilterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addImageTagFilterButtonActionPerformed(evt);
            }
        });

        addChannelFilterButton.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        addChannelFilterButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Filter_Channel.png"))); // NOI18N
        addChannelFilterButton.setToolTipText("Filter: Channel");
        addChannelFilterButton.setMaximumSize(new java.awt.Dimension(24, 24));
        addChannelFilterButton.setMinimumSize(new java.awt.Dimension(24, 24));
        addChannelFilterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addChannelFilterButtonActionPerformed(evt);
            }
        });

        addZFilterButton.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        addZFilterButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Filter_Z.png"))); // NOI18N
        addZFilterButton.setToolTipText("Filter: Z-Position");
        addZFilterButton.setMaximumSize(new java.awt.Dimension(24, 24));
        addZFilterButton.setMinimumSize(new java.awt.Dimension(24, 24));
        addZFilterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addZFilterButtonActionPerformed(evt);
            }
        });

        addScriptAnalyzerButton.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        addScriptAnalyzerButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/script.png"))); // NOI18N
        addScriptAnalyzerButton.setToolTipText("Script Analyzer");
        addScriptAnalyzerButton.setMaximumSize(new java.awt.Dimension(24, 24));
        addScriptAnalyzerButton.setMinimumSize(new java.awt.Dimension(24, 24));
        addScriptAnalyzerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addScriptAnalyzerButtonActionPerformed(evt);
            }
        });

        removeProcessorButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/delete.png"))); // NOI18N
        removeProcessorButton.setToolTipText("Remove Data Processor/Image Analyzer ");
        removeProcessorButton.setMaximumSize(new java.awt.Dimension(24, 24));
        removeProcessorButton.setMinimumSize(new java.awt.Dimension(24, 24));
        removeProcessorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeProcessorButtonActionPerformed(evt);
            }
        });

        addROIFinderButton.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        addROIFinderButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/roi.png"))); // NOI18N
        addROIFinderButton.setToolTipText("ROI Finder (script)");
        addROIFinderButton.setMaximumSize(new java.awt.Dimension(24, 24));
        addROIFinderButton.setMinimumSize(new java.awt.Dimension(24, 24));
        addROIFinderButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addROIFinderButtonActionPerformed(evt);
            }
        });

        editProcessorButton.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        editProcessorButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/edit.png"))); // NOI18N
        editProcessorButton.setToolTipText("Edit Processor/Analyzer Parameters");
        editProcessorButton.setMaximumSize(new java.awt.Dimension(24, 24));
        editProcessorButton.setMinimumSize(new java.awt.Dimension(24, 24));
        editProcessorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editProcessorButtonActionPerformed(evt);
            }
        });

        addAreaFilterButton.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        addAreaFilterButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Area.png"))); // NOI18N
        addAreaFilterButton.setToolTipText("Filter: Area");
        addAreaFilterButton.setMaximumSize(new java.awt.Dimension(24, 24));
        addAreaFilterButton.setMinimumSize(new java.awt.Dimension(24, 24));
        addAreaFilterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAreaFilterButtonActionPerformed(evt);
            }
        });

        addDataProcFromFileButton.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        addDataProcFromFileButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/DP_open.png"))); // NOI18N
        addDataProcFromFileButton.setToolTipText("Load DataProcessor class");
        addDataProcFromFileButton.setMaximumSize(new java.awt.Dimension(24, 24));
        addDataProcFromFileButton.setMinimumSize(new java.awt.Dimension(24, 24));
        addDataProcFromFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataProcFromFileButtonActionPerformed(evt);
            }
        });

        addImageStorageButton.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        addImageStorageButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/harddisk.png"))); // NOI18N
        addImageStorageButton.setToolTipText("Image Storage");
        addImageStorageButton.setMaximumSize(new java.awt.Dimension(24, 24));
        addImageStorageButton.setMinimumSize(new java.awt.Dimension(24, 24));
        addImageStorageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addImageStorageButtonActionPerformed(evt);
            }
        });

        addFrameFilterButton.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        addFrameFilterButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Filter_Frame.png"))); // NOI18N
        addFrameFilterButton.setToolTipText("Filter: Frame");
        addFrameFilterButton.setMaximumSize(new java.awt.Dimension(24, 24));
        addFrameFilterButton.setMinimumSize(new java.awt.Dimension(24, 24));
        addFrameFilterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addFrameFilterButtonActionPerformed(evt);
            }
        });

        loadProcTreeButton.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        loadProcTreeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/openDoc.png"))); // NOI18N
        loadProcTreeButton.setToolTipText("Load Processor Tree");
        loadProcTreeButton.setMaximumSize(new java.awt.Dimension(24, 24));
        loadProcTreeButton.setMinimumSize(new java.awt.Dimension(24, 24));
        loadProcTreeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadProcTreeButtonActionPerformed(evt);
            }
        });

        saveProcTreeButton.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        saveProcTreeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/saveDoc.png"))); // NOI18N
        saveProcTreeButton.setToolTipText("Save Processor Tree");
        saveProcTreeButton.setMaximumSize(new java.awt.Dimension(24, 24));
        saveProcTreeButton.setMinimumSize(new java.awt.Dimension(24, 24));
        saveProcTreeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveProcTreeButtonActionPerformed(evt);
            }
        });

        loadImagePipelineButton.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        loadImagePipelineButton.setText("IP");
        loadImagePipelineButton.setToolTipText("Load active DataProcessor(s) in Image Processor Pipeline");
        loadImagePipelineButton.setMaximumSize(new java.awt.Dimension(24, 24));
        loadImagePipelineButton.setMinimumSize(new java.awt.Dimension(24, 24));
        loadImagePipelineButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadImagePipelineButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel9Layout = new org.jdesktop.layout.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel9Layout.createSequentialGroup()
                .add(6, 6, 6)
                .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel9Layout.createSequentialGroup()
                        .add(jScrollPane4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 315, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(3, 3, 3)
                        .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel9Layout.createSequentialGroup()
                                .add(addDataProcFromFileButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(loadImagePipelineButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jPanel9Layout.createSequentialGroup()
                                .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jPanel9Layout.createSequentialGroup()
                                        .add(addChannelFilterButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(addZFilterButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(jPanel9Layout.createSequentialGroup()
                                        .add(addFrameFilterButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(addAreaFilterButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(jPanel9Layout.createSequentialGroup()
                                        .add(removeProcessorButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(editProcessorButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(jPanel9Layout.createSequentialGroup()
                                        .add(loadProcTreeButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(saveProcTreeButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(addImageStorageButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(jPanel9Layout.createSequentialGroup()
                                        .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                            .add(addImageTagFilterButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                            .add(addScriptAnalyzerButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(addROIFinderButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                .add(0, 0, Short.MAX_VALUE)))
                        .add(6, 6, 6))
                    .add(jPanel9Layout.createSequentialGroup()
                        .add(jLabel28)
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel28)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 316, Short.MAX_VALUE)
                    .add(jPanel9Layout.createSequentialGroup()
                        .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(addZFilterButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(addChannelFilterButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(addAreaFilterButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(addFrameFilterButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(addImageTagFilterButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(addScriptAnalyzerButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(addROIFinderButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(addDataProcFromFileButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(loadImagePipelineButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(addImageStorageButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(loadProcTreeButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(saveProcTreeButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(editProcessorButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(removeProcessorButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        sequenceTabbedPane.addTab("Process", jPanel9);

        sequenceListPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Acquisition Sequences", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Lucida Grande", 0, 12))); // NOI18N

        acqSettingUpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Up.png"))); // NOI18N
        acqSettingUpButton.setToolTipText("Move Acquisition Setting up");
        acqSettingUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                acqSettingUpButtonActionPerformed(evt);
            }
        });

        acqSettingDownButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Down.png"))); // NOI18N
        acqSettingDownButton.setToolTipText("Move Acquisition Setting down");
        acqSettingDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                acqSettingDownButtonActionPerformed(evt);
            }
        });

        deleteAcqSettingButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/delete.png"))); // NOI18N
        deleteAcqSettingButton.setToolTipText("Remove Acquisition Setting");
        deleteAcqSettingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteAcqSettingButtonActionPerformed(evt);
            }
        });

        acqSettingTable.setModel(new AcqSettingTableModel(null));
        jScrollPane7.setViewportView(acqSettingTable);

        saveAcqSettingButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/saveDoc.png"))); // NOI18N
        saveAcqSettingButton.setToolTipText("Save Acquisition Setting");
        saveAcqSettingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAcqSettingButtonActionPerformed(evt);
            }
        });

        addAcqSettingButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/add2.png"))); // NOI18N
        addAcqSettingButton.setToolTipText("Add new Acquisition Setting");
        addAcqSettingButton.setMaximumSize(new java.awt.Dimension(24, 24));
        addAcqSettingButton.setMinimumSize(new java.awt.Dimension(24, 24));
        addAcqSettingButton.setPreferredSize(new java.awt.Dimension(24, 24));
        addAcqSettingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAcqSettingButtonActionPerformed(evt);
            }
        });

        loadAcqSettingButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/openDoc.png"))); // NOI18N
        loadAcqSettingButton.setToolTipText("Load Acquisition Setting");
        loadAcqSettingButton.setMaximumSize(new java.awt.Dimension(24, 24));
        loadAcqSettingButton.setMinimumSize(new java.awt.Dimension(24, 24));
        loadAcqSettingButton.setPreferredSize(new java.awt.Dimension(24, 24));
        loadAcqSettingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadAcqSettingButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout sequenceListPanelLayout = new org.jdesktop.layout.GroupLayout(sequenceListPanel);
        sequenceListPanel.setLayout(sequenceListPanelLayout);
        sequenceListPanelLayout.setHorizontalGroup(
            sequenceListPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(sequenceListPanelLayout.createSequentialGroup()
                .add(3, 3, 3)
                .add(jScrollPane7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 337, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(6, 6, 6)
                .add(sequenceListPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(addAcqSettingButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(acqSettingUpButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(loadAcqSettingButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(sequenceListPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(deleteAcqSettingButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(saveAcqSettingButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(acqSettingDownButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(6, 6, 6))
        );
        sequenceListPanelLayout.setVerticalGroup(
            sequenceListPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(sequenceListPanelLayout.createSequentialGroup()
                .add(0, 0, 0)
                .add(sequenceListPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(sequenceListPanelLayout.createSequentialGroup()
                        .add(jScrollPane7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .add(3, 3, 3))
                    .add(sequenceListPanelLayout.createSequentialGroup()
                        .add(sequenceListPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(deleteAcqSettingButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(addAcqSettingButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(2, 2, 2)
                        .add(sequenceListPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(saveAcqSettingButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(loadAcqSettingButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(2, 2, 2)
                        .add(sequenceListPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(acqSettingUpButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(acqSettingDownButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(4, 4, 4))))
        );

        jPanel11.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Experiment", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Lucida Grande", 0, 12))); // NOI18N

        jLabel8.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel8.setText("Experiment:");

        experimentTextField.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        experimentTextField.setText("jTextField1");
        experimentTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                experimentTextFieldFocusLost(evt);
            }
        });

        loadExpSettingFileButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/openDoc.png"))); // NOI18N
        loadExpSettingFileButton.setPreferredSize(new java.awt.Dimension(24, 24));
        loadExpSettingFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadExpSettingFileButtonActionPerformed(evt);
            }
        });

        saveExpSettingFileButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/saveDoc.png"))); // NOI18N
        saveExpSettingFileButton.setPreferredSize(new java.awt.Dimension(24, 24));
        saveExpSettingFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveExpSettingFileButtonActionPerformed(evt);
            }
        });

        loadLayoutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/openDoc.png"))); // NOI18N
        loadLayoutButton.setPreferredSize(new java.awt.Dimension(24, 24));
        loadLayoutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadLayoutButtonActionPerformed(evt);
            }
        });

        saveLayoutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/saveDoc.png"))); // NOI18N
        saveLayoutButton.setPreferredSize(new java.awt.Dimension(24, 24));
        saveLayoutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveLayoutButtonActionPerformed(evt);
            }
        });

        browseImageDestPathButton.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        browseImageDestPathButton.setText("...");
        browseImageDestPathButton.setPreferredSize(new java.awt.Dimension(24, 24));
        browseImageDestPathButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseImageDestPathButtonActionPerformed(evt);
            }
        });

        jLabel32.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel32.setText("Settings:");

        expSettingsFileLabel.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        expSettingsFileLabel.setText("jLabel33");

        jLabel2.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel2.setText("Layout:");

        layoutFileLabel.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        layoutFileLabel.setText("jLabel3");

        jLabel7.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel7.setText("Save to:");

        rootDirLabel.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        rootDirLabel.setText("jLabel8");

        org.jdesktop.layout.GroupLayout jPanel11Layout = new org.jdesktop.layout.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11Layout.createSequentialGroup()
                .add(6, 6, 6)
                .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel11Layout.createSequentialGroup()
                        .add(jLabel8)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(experimentTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 258, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jPanel11Layout.createSequentialGroup()
                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel2)
                            .add(jLabel7)
                            .add(jLabel32))
                        .add(3, 3, 3)
                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(expSettingsFileLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 283, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(rootDirLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 283, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(layoutFileLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 283, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .add(6, 6, 6)
                .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel11Layout.createSequentialGroup()
                        .add(loadExpSettingFileButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(2, 2, 2)
                        .add(saveExpSettingFileButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jPanel11Layout.createSequentialGroup()
                        .add(loadLayoutButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(2, 2, 2)
                        .add(saveLayoutButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(browseImageDestPathButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(6, 6, 6))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11Layout.createSequentialGroup()
                .add(0, 0, 0)
                .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel11Layout.createSequentialGroup()
                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel32, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(expSettingsFileLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(2, 2, 2)
                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(layoutFileLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(2, 2, 2)
                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(rootDirLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(2, 2, 2)
                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel8)
                            .add(experimentTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(jPanel11Layout.createSequentialGroup()
                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(loadExpSettingFileButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, saveExpSettingFileButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(2, 2, 2)
                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(saveLayoutButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(loadLayoutButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(2, 2, 2)
                        .add(browseImageDestPathButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jPanel11Layout.linkSize(new java.awt.Component[] {browseImageDestPathButton, loadExpSettingFileButton, saveExpSettingFileButton, saveLayoutButton}, org.jdesktop.layout.GroupLayout.VERTICAL);

        acquireButton.setText("Acquire");
        acquireButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                acquireButtonActionPerformed(evt);
            }
        });

        cancelThreadButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/delete.png"))); // NOI18N
        cancelThreadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelThreadButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout settingsPanelLayout = new org.jdesktop.layout.GroupLayout(settingsPanel);
        settingsPanel.setLayout(settingsPanelLayout);
        settingsPanelLayout.setHorizontalGroup(
            settingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, settingsPanelLayout.createSequentialGroup()
                .add(6, 6, 6)
                .add(settingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(settingsPanelLayout.createSequentialGroup()
                        .add(6, 6, 6)
                        .add(acquireButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 122, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(processProgressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 182, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(cancelThreadButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(85, 85, 85))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, settingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel11, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, sequenceListPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, sequenceTabbedPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
        );
        settingsPanelLayout.setVerticalGroup(
            settingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(settingsPanelLayout.createSequentialGroup()
                .add(jPanel11, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(sequenceListPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(sequenceTabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE)
                .add(3, 3, 3)
                .add(settingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(acquireButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(processProgressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(cancelThreadButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(12, 12, 12))
        );

        statusPanel.setMaximumSize(new java.awt.Dimension(32767, 130));
        statusPanel.setMinimumSize(new java.awt.Dimension(566, 130));
        statusPanel.setPreferredSize(new java.awt.Dimension(906, 130));
        statusPanel.setSize(new java.awt.Dimension(0, 130));

        stagePosXLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        stagePosXLabel.setText("---");

        stagePosYLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        stagePosYLabel.setText("---");

        jLabel30.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        jLabel30.setText("Y:");

        areaLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        areaLabel.setText("Area:");

        jLabel31.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        jLabel31.setText("Z:");

        stagePosZLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        stagePosZLabel.setText("---");

        jLabel29.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        jLabel29.setText("X:");

        timepointLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        timepointLabel.setText("Timepoint:");

        setLandmarkButton.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        setLandmarkButton.setText("Landmark");
        setLandmarkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setLandmarkButtonActionPerformed(evt);
            }
        });

        moveToScreenCoordButton.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        moveToScreenCoordButton.setText("Move To");
        moveToScreenCoordButton.setSize(new java.awt.Dimension(70, 29));
        moveToScreenCoordButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveToScreenCoordButtonActionPerformed(evt);
            }
        });

        zoomButton.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        zoomButton.setText("Zoom");
        zoomButton.setToolTipText("Left click to zoom in, right click to zoom out.");
        zoomButton.setSize(new java.awt.Dimension(70, 29));
        zoomButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomButtonActionPerformed(evt);
            }
        });

        cursorLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        cursorLabel.setText("Layout:");
        cursorLabel.setPreferredSize(new java.awt.Dimension(100, 16));

        selectButton.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        selectButton.setText("Select");
        selectButton.setSize(new java.awt.Dimension(70, 29));
        selectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectButtonActionPerformed(evt);
            }
        });

        commentButton.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        commentButton.setText("Comment");
        commentButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                commentButtonActionPerformed(evt);
            }
        });

        showZProfileCheckBox.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        showZProfileCheckBox.setText("Z-Profile");
        showZProfileCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showZProfileCheckBoxActionPerformed(evt);
            }
        });

        stageControlButton.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        stageControlButton.setText("Stage");
        stageControlButton.setSize(new java.awt.Dimension(70, 29));
        stageControlButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stageControlButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout statusPanelLayout = new org.jdesktop.layout.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelLayout.createSequentialGroup()
                .add(0, 0, 0)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(statusPanelLayout.createSequentialGroup()
                        .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(statusPanelLayout.createSequentialGroup()
                                .add(jLabel29)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(stagePosXLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 84, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jLabel30))
                            .add(statusPanelLayout.createSequentialGroup()
                                .add(zoomButton)
                                .add(0, 0, 0)
                                .add(selectButton)))
                        .add(0, 0, 0)
                        .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(statusPanelLayout.createSequentialGroup()
                                .add(stagePosYLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 90, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel31))
                            .add(statusPanelLayout.createSequentialGroup()
                                .add(commentButton)
                                .add(0, 0, 0)
                                .add(stageControlButton)
                                .add(0, 0, 0)
                                .add(moveToScreenCoordButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 77, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .add(0, 0, 0)
                        .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(statusPanelLayout.createSequentialGroup()
                                .add(stagePosZLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 91, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(18, 18, 18)
                                .add(cursorLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 39, Short.MAX_VALUE)
                                .add(55, 55, 55))
                            .add(statusPanelLayout.createSequentialGroup()
                                .add(setLandmarkButton)
                                .add(0, 0, 0)
                                .add(showZProfileCheckBox)
                                .add(0, 0, Short.MAX_VALUE))))
                    .add(statusPanelLayout.createSequentialGroup()
                        .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(statusPanelLayout.createSequentialGroup()
                                .add(timepointLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 180, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(6, 6, 6)
                                .add(areaLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 316, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 545, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(0, 0, Short.MAX_VALUE))))
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, statusPanelLayout.createSequentialGroup()
                .add(0, 0, 0)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(setLandmarkButton)
                    .add(moveToScreenCoordButton)
                    .add(zoomButton)
                    .add(selectButton)
                    .add(commentButton)
                    .add(showZProfileCheckBox)
                    .add(stageControlButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel29)
                    .add(stagePosXLabel)
                    .add(jLabel30)
                    .add(stagePosYLabel)
                    .add(jLabel31)
                    .add(stagePosZLabel)
                    .add(cursorLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(6, 6, 6)
                .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(6, 6, 6)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(timepointLabel)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, areaLabel))
                .add(12, 12, 12))
        );

        layoutScrollPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                layoutScrollPaneComponentResized(evt);
            }
        });

        acqLayoutPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                acqLayoutPanelMouseMoved(evt);
            }
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                acqLayoutPanelMouseDragged(evt);
            }
        });
        acqLayoutPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                acqLayoutPanelMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                acqLayoutPanelMouseReleased(evt);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                acqLayoutPanelMouseClicked(evt);
            }
        });
        layoutScrollPane.setViewportView(acqLayoutPanel);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(6, 6, 6)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(statusPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 585, Short.MAX_VALUE)
                    .add(layoutScrollPane))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(settingsPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 421, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(6, 6, 6))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(6, 6, 6)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(settingsPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 680, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(layoutScrollPane)
                        .add(6, 6, 6)
                        .add(statusPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 104, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .add(6, 6, 6))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    @Override
    public void tableChanged(TableModelEvent e) {
//        IJ.log("AcqFrame.tableChanged: "+e.getType());
        int row = e.getFirstRow();
        int column = e.getColumn();
        if (e.getSource() == areaTable.getModel() && (e.getColumn() == 0 || e.getColumn() == TableModelEvent.ALL_COLUMNS || e.getType() == TableModelEvent.DELETE || e.getType() == TableModelEvent.INSERT)) {
            /*            int id=(Integer)atm.getValueAt(row,4);
             Area a=acqLayout.getAreaById(id);
             a.setSelected((Boolean)atm.getValueAt(row, column));
             IJ.log("AcqFrame.tableChanged: clicked checkbox in row "+Integer.toString(row)+", "+Boolean.toString(a.isSelected()));*/
            /*            if (currentAcqSetting!=null) {
             calcTilePositions(currengetTileWidth_UMgetTileWidth(),currengetTileHeight_UMetTileHeight(), currentAcqSetting.getTilingSetting());               
             }   */
            AreaTableModel atm = (AreaTableModel) areaTable.getModel();
            if (recalculateTiles && column == 0 && (Boolean) atm.getValueAt(row, 0)) {
                List<Area> al = new ArrayList<Area>(1);
                al.add(atm.getRowData(row));
                calcTilePositions(al, currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), SELECTING_AREA);
            } else
                calcTotalTileNumber();
            updateAcqLayoutPanel();
        } else if (e.getSource() == acqSettingTable.getModel() && (e.getColumn() == 0 
                || e.getColumn() == TableModelEvent.ALL_COLUMNS 
                || e.getType() == TableModelEvent.DELETE 
                || e.getType() == TableModelEvent.INSERT)) {
//                || e.getType() == TableModelEvent.UPDATE)) {
            updatingAcqSettingTable=true;
/*            for (AcqSetting setting:acqSettings)
                acqSettingComboBox.addItem(setting.getName());*/
//            initializeProgressTree(currentAcqSetting);
            updateProcessorTreeView(currentAcqSetting);
            if (e.getType() == TableModelEvent.INSERT) {
//                IJ.showMessage("insert");
            }
            updateAutofocusToolTip(currentAcqSetting);
            updatingAcqSettingTable=false;
        }
    }

    //handles selection events in acqSettingTable;  called from anonymous class
    public void acqSettingSelectionChanged(ListSelectionEvent e) {
        ListSelectionModel lsm = (ListSelectionModel) e.getSource();
//            int firstIndex = e.getFirstIndex();
//            int lastIndex = e.getLastIndex();
        int minIndex = lsm.getMinSelectionIndex();
        if (minIndex >= 0) {
//            cAcqSettingIdx = minIndex;
            AcqSetting newSetting = acqSettings.get(minIndex);
            if (currentAcqSetting != newSetting) {
                currentAcqSetting = newSetting;
                sequenceTabbedPane.setBorder(BorderFactory.createTitledBorder(
                        "Sequence: "+currentAcqSetting.getName()));
                prevTilingSetting = currentAcqSetting.getTilingSetting().duplicate();
                prevObjLabel = currentAcqSetting.getObjective();
                calcTilePositions(null, currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), ADJUSTING_SETTINGS);
                ((LayoutPanel) acqLayoutPanel).setAcqSetting(currentAcqSetting, true);
                updateAcqSettingTab(currentAcqSetting);
                updateProcessorTreeView(currentAcqSetting);
                updateAutofocusToolTip(currentAcqSetting);
/*                try {
                    core.setChannelGroup(currentAcqSetting.getChannelGroupStr());
                } catch (Exception ex) {
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                }*/
                acqSettingTable.requestFocusInWindow();
            }
        }
    }

    public void setLandmarkFound(boolean b) {
//        IJ.log("AcqFrame.setLandmarkFound("+b+")");
        acquireButton.setEnabled(b);
        moveToScreenCoordButton.setEnabled(b);
        newAreaButton.setEnabled(b);
        if (moveToMode) {
            moveToMode = b;
        }
    }

    private SequenceSettings createMDASettings(AcqSetting acqSetting) {
//        experimentName=acqSetting.getName();
        SequenceSettings settings = new SequenceSettings();

//        try {
//            StrVector availableChannels = core.getAvailableConfigs(channelGroupStr);
            if (acqSetting.getChannelGroupStr()==null || acqSetting.getChannelGroupStr().equals("")) {
                String chGroupStr=MMCoreUtils.loadAvailableChannelConfigs(this,"", core);
                currentAcqSetting.setChannelGroupStr(chGroupStr);
            }    
            StrVector availableChannels = core.getAvailableConfigs(acqSetting.getChannelGroupStr());
            if (availableChannels == null || availableChannels.isEmpty()) {
                acqSetting.setChannelGroupStr(MMCoreUtils.loadAvailableChannelConfigs(this,acqSetting.getChannelGroupStr(), core));
            }
            if (!initializeChannelTable(acqSetting)) {
                return null;
            }
            /*
             if (Math.abs(zStackStepSize) < acqEng.getMinZStepUm()) {
             zStackStepSize = acqEng.getMinZStepUm();
             }
             */
            settings.numFrames = timelapseCheckBox.isSelected() ? acqSetting.getFrames() : 1;
            settings.slices = new ArrayList<Double>();
            if (zStackCheckBox.isSelected()) {
         //       double z = NumberUtils.displayStringToDouble(zStackBeginField_Old.getText());
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
            settings.channels = new ArrayList<ChannelSpec>();
            ChannelTableModel ctm = (ChannelTableModel) channelTable.getModel();
//            settings.channelGroup = channelGroupStr;
            settings.channelGroup = acqSetting.getChannelGroupStr();
            for (int i = 0; i < ctm.getRowCount(); i++) {
                Channel c = ctm.getRowData(i);
                ChannelSpec cs = new ChannelSpec();
                cs.config = c.getName();
                cs.exposure = c.getExposure();
                cs.color = c.getColor();
                cs.doZStack = zStackCheckBox.isSelected();
                cs.zOffset = c.getZOffset();
                cs.useChannel = true;
                //cs.camera = core.getCameraDevice();
                settings.channels.add(cs);
            }
            settings.useAutofocus = autofocusCheckBox.isSelected();
            settings.usePositionList = true;
            settings.slicesFirst = (acqOrderList.getSelectedIndex() == 1 | acqOrderList.getSelectedIndex() == 3);
            settings.timeFirst = acqOrderList.getSelectedIndex() >= 2;
            settings.save = true;
            settings.keepShutterOpenChannels = false;
            settings.keepShutterOpenSlices = false;
            settings.intervalMs = acqSetting.getIntervalInMilliS();
            settings.skipAutofocusCount = 0;
            settings.prefix = acqSetting.getName();
            settings.root = imageDestPath;
//            IJ.log("AcqFrame.createMDASettings: imageDestPath"+imageDestPath);
/*        } catch (ParseException p) {
            ReportingUtils.showError(p);
            return null;
        }*/
        return settings;
    }
    
    private void enableGUI(boolean b) {
        moveToScreenCoordButton.setEnabled(b);
        stageControlButton.setEnabled(b);
        setLandmarkButton.setEnabled(b && !acqLayout.isEmpty());
//        findLandmarkButton.setEnabled(b);
        snapButton.setEnabled(b);
        liveButton.setEnabled(b);
        autoExposureButton.setEnabled(b);
        zOffsetButton.setEnabled(b);
        commentButton.setEnabled(b);
        loadLayoutButton.setEnabled(b);
        saveLayoutButton.setEnabled(b);
        loadExpSettingFileButton.setEnabled(b);
        saveExpSettingFileButton.setEnabled(b);
        experimentTextField.setEnabled(b);
        browseImageDestPathButton.setEnabled(b);
        
        newAreaButton.setEnabled(b && !acqLayout.isEmpty());
        removeAreaButton.setEnabled(b && !acqLayout.isEmpty());
        areaUpButton.setEnabled(b && !acqLayout.isEmpty());
        areaDownButton.setEnabled(b && !acqLayout.isEmpty());
        mergeAreasButton.setEnabled(b && !acqLayout.isEmpty());
        areaTable.setEnabled(b);

        acqSettingTable.setEnabled(b);
        addAcqSettingButton.setEnabled(b);
        deleteAcqSettingButton.setEnabled(b);
        loadAcqSettingButton.setEnabled(b);
        saveAcqSettingButton.setEnabled(b);
        acqSettingUpButton.setEnabled(b);
        acqSettingDownButton.setEnabled(b);
        objectiveComboBox.setEnabled(b);
        binningComboBox.setEnabled(b);
        tilingModeComboBox.setEnabled(b);
        tilingDirComboBox.setEnabled(b);
        insideOnlyCheckBox.setEnabled(b);
        clusterCheckBox.setEnabled(b && (currentAcqSetting.getTilingMode() != TilingSetting.Mode.FULL));
        clusterXField.setEnabled(b && clusterCheckBox.isSelected() && clusterCheckBox.isEnabled());
        clusterYField.setEnabled(b && clusterCheckBox.isSelected() && clusterCheckBox.isEnabled());
//        clusterOverlapCheckBox.setEnabled(b && clusterCheckBox.isSelected() && clusterCheckBox.isEnabled() && currentAcqSetting.getTilingMode() != TilingSetting.Mode.CENTER);
        siteOverlapCheckBox.setEnabled(b && currentAcqSetting.getTilingMode() == TilingSetting.Mode.RANDOM);
        maxSitesLabel.setEnabled(b && (currentAcqSetting.getTilingMode() == TilingSetting.Mode.RANDOM || currentAcqSetting.getTilingMode() == TilingSetting.Mode.RUNTIME));
        maxSitesField.setEnabled(maxSitesLabel.isEnabled());
        tileOverlapField.setEnabled(b);
        autofocusButton.setEnabled(b);
        autofocusImportButton.setEnabled(b);
        clearRoiButton.setEnabled(b);
        closeGapsButton.setEnabled(b);
        
        addImageTagFilterButton.setEnabled(b);
        addChannelFilterButton.setEnabled(b);
        addZFilterButton.setEnabled(b);
        addFrameFilterButton.setEnabled(b);
        addAreaFilterButton.setEnabled(b);
        addScriptAnalyzerButton.setEnabled(b);
        addROIFinderButton.setEnabled(b);
        addDataProcFromFileButton.setEnabled(b);
        loadImagePipelineButton.setEnabled(b && imagePipelineSupported);
        addImageStorageButton.setEnabled(b);
        removeProcessorButton.setEnabled(b);
        editProcessorButton.setEnabled(b);
//        upProcessorButton.setEnabled(b);
//        downProcessorButton.setEnabled(b);
//        leftProcessorButton.setEnabled(b);
//        rightProcessorButton.setEnabled(b);
        loadProcTreeButton.setEnabled(b);
        saveProcTreeButton.setEnabled(b);

        autofocusCheckBox.setEnabled(b);
        zStackCheckBox.setEnabled(b);
        timelapseCheckBox.setEnabled(b);
        acqOrderList.setEnabled(b);
        selectButton.setEnabled(b);
        channelTable.setEnabled(b);
        addChannelButton.setEnabled(b);
        removeChannelButton.setEnabled(b);
        channelUpButton.setEnabled(b);
        channelDownButton.setEnabled(b);
        enableTimelapsePane(b);
        enableZStackPane(b);
    }
/*
    public JSONObject createSummary(AcqSetting as, SequenceSettings s, PositionList pl) {
        // create summary metadata
        JSONObject summary = new JSONObject();
        try {
            summary.put(MMTags.Summary.SLICES, s.slices.size());
            summary.put(MMTags.Summary.POSITIONS, pl.getNumberOfPositions());
            summary.put(MMTags.Summary.CHANNELS, s.channels.size());
            summary.put(MMTags.Summary.FRAMES, s.numFrames);
            summary.put(MMTags.Summary.SLICES_FIRST, true);
            summary.put(MMTags.Summary.TIME_FIRST, false);

            long d = core.getBytesPerPixel();
            if (d == 2) {
                summary.put(MMTags.Summary.PIX_TYPE, "GRAY16");
                summary.put(MMTags.Summary.IJ_TYPE, ImagePlus.GRAY16);
            } else if (d == 1) {
                summary.put(MMTags.Summary.PIX_TYPE, "GRAY8");
                summary.put(MMTags.Summary.IJ_TYPE, ImagePlus.GRAY8);
            } else {
                System.out.println("Unsupported pixel type");
                return summary;
            }
            summary.put(MMTags.Summary.WIDTH, as.getTileWidth_UM());
            summary.put(MMTags.Summary.HEIGHT, as.getTileHeight_UM());
            summary.put(MMTags.Summary.PREFIX, s.prefix); // Acquisition name

            //these are used to create display settings
            JSONArray chColors = new JSONArray();
            JSONArray chNames = new JSONArray();
            JSONArray chMins = new JSONArray();
            JSONArray chMaxs = new JSONArray();
            for (int ch = 0; ch < s.channels.size(); ch++) {
                chColors.put(s.channels.get(ch).color.getRGB());
                chNames.put(s.channels.get(ch).config);
                chMins.put(0);
                chMaxs.put(d == 2 ? 65535 : 255);
            }
            summary.put(MMTags.Summary.COLORS, chColors);
            summary.put(MMTags.Summary.NAMES, chNames);
            summary.put(MMTags.Summary.CHANNEL_MINS, chMins);
            summary.put(MMTags.Summary.CHANNEL_MAXES, chMaxs);

        } catch (JSONException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        return summary;
    }
*/
    public String createUniqueExpName(String root, String exp) {
        int i = 1;
        String ext = "";
        while (new File(root, exp + ext).exists()) {
            ext = "_" + Integer.toString(i);
            i++;
        }
        return new File(root, exp + ext).getAbsolutePath();
    }

    private boolean setObjectiveAndBinning(AcqSetting setting, boolean updateGUI) {
        if (setting != null) {
            try {
                core.setProperty(objectiveDevStr, "Label", setting.getObjective());
                //core.setConfig(objectiveDevStr, setting.getObjective());
            } catch (Exception e) {
                ReportingUtils.showError(e);
                objectiveDevStr=changeDeviceStr("Objective");
                try {
                    core.setProperty(objectiveDevStr, "Label", "test");//setting.getObjective());
                } catch (Exception ex) {
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
                JOptionPane.showMessageDialog(this,"Error selecting "+objectiveDevStr+", "+setting.getObjective());
                setting.setObjectiveDevStr(objectiveDevStr);
                return false;
            }
            

            try {
                core.setProperty(core.getCameraDevice(), "Binning", Integer.toString(setting.getBinning()));
            
//                if (updateGUI) {
//                    app.refreshGUI();
//                }

            } catch (Exception e) {
                ReportingUtils.showError(e);
                return false;
            } finally {
                if (updateGUI) {
                    app.refreshGUI();
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /*
    private int saveAcquisitionSettingOld(File file, AcqSetting setting) {
        try {
            XMLStreamWriter xtw=AcqSetting.writeXMLHeader(file.getAbsolutePath());
            setting.saveToXMLFile(xtw);
            AcqSetting.closeXMLFile(xtw);
            return XMLUtils.FILE_IO_OK;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            return XMLUtils.FILE_NOT_FOUND;
        } catch (XMLStreamException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            return XMLUtils.FILE_WRITE_ERROR;
        }
    }
    
    private int saveAcquisitionSettingsOld(File file) {
        try {
            XMLStreamWriter xtw=AcqSetting.writeXMLHeader(file.getAbsolutePath());
            for (AcqSetting setting:acqSettings) {
                setting.saveToXMLFile(xtw);
            }
            AcqSetting.closeXMLFile(xtw);
            return XMLUtils.FILE_IO_OK;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            return XMLUtils.FILE_NOT_FOUND;
        } catch (XMLStreamException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            return XMLUtils.FILE_WRITE_ERROR;
        }
    }
    */
    
    private void saveAcquisitionSettings(File file) {
        FileWriter fw=null;
        try {
            fw = new FileWriter(file);
            JSONObject obj=new JSONObject();
            JSONArray settingArray=new JSONArray();
            for (int i=0; i<acqSettings.size(); i++) {
                try {  
                    settingArray.put(i,acqSettings.get(i).toJSONObject());
                } catch (JSONException ex) {
                    JOptionPane.showMessageDialog(this,"Error parsing acquisition setting '"+acqSettings.get(i).getName()+"'.");
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                obj.put(AcqSetting.TAG_ACQ_SETTING_ARRAY, settingArray);
                fw.write(obj.toString(4));  
            } catch (JSONException ex) {
                JOptionPane.showMessageDialog(this,"Error saving acquisition settings.");
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }    
        } catch (IOException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this,"Error saving acquisition settings");
        } finally {
            try {
                fw.close();
            } catch (IOException ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
    
    private void saveAcquisitionSetting(File file, AcqSetting setting) {
        FileWriter fw=null;
        try {
            fw = new FileWriter(file);
                try {
                    fw.write(setting.toJSONObject().toString(4));  
                } catch (JSONException ex) {
                    JOptionPane.showMessageDialog(this,"Error saving setting '"+setting.getName()+"'.");
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            
            } 
        } catch (IOException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Error saving acquisition settings");
        } finally {
            try {
                fw.close();
            } catch (IOException ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
    
    private void saveExperimentSettings(File file) {
        FileWriter fw=null;
        try {
            fw = new FileWriter(file);
            JSONObject expSettingObj=new JSONObject();
            
            try {
                expSettingObj.put(TAG_ROOT_DIR,rootDirLabel.getText());
                expSettingObj.put(TAG_EXP_BASE_NAME,experimentTextField.getText());
            
                JSONObject layoutObj=acqLayout.toJSONObject();
                expSettingObj.put(AcqLayout.TAG_LAYOUT, layoutObj);
            } catch (JSONException ex) {
                JOptionPane.showMessageDialog(this,"Error parsing layout file '"+acqLayout.getName()+"'.");
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
            JSONArray settingArray=new JSONArray();
            for (int i=0; i<acqSettings.size(); i++) {
                try {  
                    settingArray.put(i,acqSettings.get(i).toJSONObject());
                } catch (JSONException ex) {
                    JOptionPane.showMessageDialog(this,"Error parsing acquisition setting '"+acqSettings.get(i).getName()+"'.");
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                expSettingObj.put(AcqSetting.TAG_ACQ_SETTING_ARRAY, settingArray);
                fw.write(expSettingObj.toString(4));  
            } catch (JSONException ex) {
                JOptionPane.showMessageDialog(this,"Error saving acquisition settings.");
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }    
        } catch (IOException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this,"Error saving acquisition settings");
        } finally {
            try {
                fw.close();
            } catch (IOException ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    /*
    private void saveExperimentSettingsOld(File file) {
        File layoutFile=new File(imageDestPath,acqLayout.getFile().getName());
        acqLayout.saveLayoutToXMLFile(layoutFile);
        File acqSettingsFile=new File(imageDestPath,"AcqSettings.XML");
        saveAcquisitionSettings(acqSettingsFile);
        File processorTreeFile=new File(imageDestPath, "ProcTree.txt");
        saveProcessorTree(processorTreeFile, acqSettings);
        try {
            Utils.copyFile(layoutFile, new File(Prefs.getHomeDir(),"LastLayout.XML"));
            Utils.copyFile(acqSettingsFile, new File(Prefs.getHomeDir(),"LastAcqSettings.XML"));
            Utils.copyFile(processorTreeFile, new File(Prefs.getHomeDir(),"LastProcTree.txt"));
        } catch (IOException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            XMLUtils.initialize();
            XMLOutputFactory xof =  XMLOutputFactory.newInstance();
            XMLStreamWriter xtw = xof.createXMLStreamWriter(new FileOutputStream(file.getAbsolutePath()), "UTF-8"); 
            xtw.writeStartDocument("utf-8","1.0"); 
            xtw.writeCharacters(XMLUtils.LINE_FEED);
                        
            XMLUtils.wStartElement(xtw, "EXPERIMENT_SETTINGS");
                XMLUtils.writeLine(xtw, TAG_VERSION, "1.0");
                XMLUtils.writeLine(xtw, TAG_STORAGE_DESTINATION,new File(imageDestPath).getParentFile().getAbsolutePath());
                XMLUtils.writeLine(xtw, TAG_EXP_NAME,new File(imageDestPath).getName());
                XMLUtils.writeLine(xtw, TAG_LAYOUT_FILE, layoutFile.getAbsolutePath());
                XMLUtils.writeLine(xtw, TAG_ACQ_SETTING_FILE, acqSettingsFile.getAbsolutePath());
                XMLUtils.writeLine(xtw, TAG_PROCESSOR_TREE_FILE, processorTreeFile.getAbsolutePath());
            XMLUtils.wEndElement(xtw);
            
            xtw.writeEndDocument(); 
            xtw.flush();
            xtw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex); 
        } catch (XMLStreamException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        }    
    }
    */
    private void saveProcessorTree(File file, AcqSetting setting) {
        FileWriter fw=null;
        try {
            fw = new FileWriter(file);
            JSONObject procDef=new JSONObject();
            JSONArray procTreeArray=new JSONArray();
            JSONObject procObj= new JSONObject();
            procObj.put("SeqSetting",setting.getName());
            procObj.put("ProcTree",Utils.processortreeToJSONObject(setting.getImageProcessorTree(),setting.getImageProcessorTree()));
            procTreeArray.put(procObj);
            procDef.put("Processor_Definition", procTreeArray);
            fw.write(procDef.toString(4));
        } catch (IOException ex) {
            IJ.log("IOException "+ex);
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            IJ.log("JSONException "+ex);
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            IJ.log("Exception "+ex);
        } finally {
            if (fw!=null) {
                try {
                    fw.close();
                } catch (IOException ex) {
                IJ.log("IOException "+ex);
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }       
        }
    }
    
/*    
    private int saveProcessorTreeXML(File file, DefaultMutableTreeNode procRoot) {
        if (procRoot==null) {
            return XMLUtils.FILE_WRITE_ERROR;
        }
        try {
            XMLStreamWriter xtw=XMLUtils.writeXMLHeader(file.getAbsolutePath());
            DataProcessor dp=(DataProcessor)procRoot.getUserObject();
            
            XMLUtils.wStartElement(xtw, "ProcessorTree");
                XMLUtils.wStartElement(xtw, "Processor");
                                
                XMLUtils.wEndElement(xtw);
            XMLUtils.wEndElement(xtw);
            
            XMLUtils.closeXMLFile(xtw);
            return XMLUtils.FILE_IO_OK;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            return XMLUtils.FILE_NOT_FOUND;
        } catch (XMLStreamException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            return XMLUtils.FILE_WRITE_ERROR;
        }
    }
*/    
 
/*    
    private void saveProcessorTree(File file, List<AcqSetting> settings) {
        FileWriter fw=null;
        try {
            fw = new FileWriter(file);
            JSONObject procDef=new JSONObject();
            JSONArray procTreeArray=new JSONArray();
            for (AcqSetting setting:settings) {
                JSONObject procObj= new JSONObject();
                procObj.put("SeqSetting",setting.getName());
                procObj.put("ProcTree",Utils.processortreeToJSONObject(setting.getImageProcessorTree(),setting.getImageProcessorTree()));
                procTreeArray.put(procObj);
            }
            procDef.put("Processor_Definition", procTreeArray);
            fw.write(procDef.toString(4));
            FileWriter fwXML=new FileWriter(new File("/Users/Karsten/Desktop/ProcTree.XML"));
        } catch (IOException ex) {
            IJ.log("IOException "+ex);
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            IJ.log("JSONException "+ex);
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            IJ.log("Exception "+ex);
        } finally {
            if (fw!=null) {
                try {
                    fw.close();
                } catch (IOException ex) {
                IJ.log("IOException "+ex);
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }       
        }
    }
*/
    public void saveAcqLayoutToJSONObjectFile (File f) {
        FileWriter fw;
        try {
            fw = new FileWriter(f);
            try {
                JSONObject obj=acqLayout.toJSONObject();
                if (obj!=null) {
                    fw.write(obj.toString(4));
                }
            } catch (JSONException ex) {
                JOptionPane.showMessageDialog(null,"Error parsing Acquisition Layout as JSONObject.");
                Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,"Error saving Acquisition Layout as JSONObject.");
                Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                fw.close();
            }        
        } catch (IOException ex) {
            Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    

    private String runAcquisition(final AcqSetting setting) {
        IJ.log("XXXXXXXXXXXXXXXXXXX\n\n");
        IJ.log("Next sequence: "+currentAcqSetting.getName());
        
        enableGUI(false);
        
        final File sequenceDir = new File(imageDestPath, setting.getName());
        if (!sequenceDir.exists()) {
            if (!sequenceDir.mkdirs()) {
                JOptionPane.showMessageDialog(this,"Cannot create Directory: " + sequenceDir.getAbsolutePath());
                enableGUI(true);
                return null;
            }
        }
        
        //save tile coordinates in separate file
        new Thread(new Runnable() {
            @Override
            public void run() {
                acqLayout.saveTileCoordsToXMLFile(new File(sequenceDir, "TileCoordinates.XML").getAbsolutePath(), setting.getTilingSetting(),setting.getTileWidth_UM(),setting.getTileHeight_UM(),setting.getImagePixelSize());
            }}).start();
        
        
        String returnValue;
        try {
            Timer timer=new Timer();
            acquisitionTask = new AcquisitionTask(app, currentAcqSetting, acqLayout,this, sequenceDir);
            timer.schedule(acquisitionTask, currentAcqSetting.getAbsoluteStart().getTime());
            
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    final JFrame frame=new JFrame(currentAcqSetting.getName());
                    frame.setPreferredSize(new Dimension(500,200));
                    frame.setResizable(false);
                    
                    JLabel label=new JLabel("Status:");
                    final JLabel msgLabel=new JLabel("");
                                        
                    JPanel panel = new JPanel();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
                    panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
                    panel.add(label);
                    panel.add(Box.createRigidArea(new Dimension(10, 0)));
                    panel.add(msgLabel);
                    panel.add(Box.createHorizontalGlue());

                    frame.getContentPane().add(panel);
                    frame.pack();
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                           
                    SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {

                        @Override
                        protected Void doInBackground() {
                            long secondsLeft=0;
                            while (isInitializing || isWaiting) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                if (isInitializing) {
                                    publish("Initializing...");                                    
                                } else if (isWaiting) {
                                    long deltaMS=acquisitionTask.scheduledExecutionTime() - System.currentTimeMillis();

                                    int seconds = (int)Math.floor(deltaMS / 1000) % 60 ;
                                    int minutes = (int) Math.floor(deltaMS / (1000*60)) % 60;
                                    int hours   = (int) Math.floor(deltaMS / (1000*60*60)) % 24;
                                    int days = (int) Math.floor(deltaMS / (1000*60*60*24));                                    
                                    String d="";
                                    if (days > 0) {
                                        d=d+Integer.toString(days)+" d, ";
                                    }
                                    if (hours > 0 | days > 0) {
                                        d=d+Integer.toString(hours)+" h, ";
                                    }
                                    if (minutes > 0 || hours > 0 || days > 0) {
                                        d=d+Integer.toString(minutes)+" min, ";
                                    }
                                    d=d+Integer.toString(seconds)+" s";
                                    publish("Acquisition will start in "+d+".");// Notify progress
                                }
                            }
                            return null;
                        }

                        @Override
                        protected void process(List<String> chunks) {
                            String msg=chunks.get(chunks.size()-1);
                            msgLabel.setText(msg);
                        }

                        @Override
                        protected void done() {
                            frame.dispose();
                        }

                    };
                    worker.execute();
                    
                }
            });

            setObjectiveAndBinning(setting, true);
            timepointLabel.setText(setting.getName() + ", Timepoint: ");
            acquireButton.setText("Stop");
            progressBar.setValue(0);
            progressBar.setMaximum(acquisitionTask.getTotalImages());
            progressBar.setStringPainted(true);
            returnValue=currentAcqSetting.getName();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex, "Error: "+currentAcqSetting.getName(), JOptionPane.ERROR_MESSAGE);
            returnValue=null;
        }        
        return returnValue;
    }

    public String startAcquisition() {
//        IJ.log("AcqFrame.runningAcquisition");

//	  if (!enoughMemoryAvailable()) {
//	         return null;
//	  }

        if (refPointListDialog != null) {
            if (stageMonitor!=null) {
                stageMonitor.removeListener(refPointListDialog);
            }
            refPointListDialog.dispose();
//                refPointListDialog.closeDialog();
            refPointListDialog=null;
        }

        if (zOffsetDialog != null) {
/*            if (stageMonitor!=null) {
                stageMonitor.removeListener(zOffsetDialog);
            }
            if (liveModeMonitor!=null)
                liveModeMonitor.removeListener(zOffsetDialog);*/
            zOffsetDialog.dispose();
//            zOffsetDialog=null;
        }
        
        AbstractCellEditor ace = (AbstractCellEditor) acqSettingTable.getCellEditor();
        if (ace != null) {
            ace.stopCellEditing();
        }

        ace = (AbstractCellEditor) channelTable.getCellEditor();
        if (ace != null) {
            ace.stopCellEditing();
        }
        AreaTableModel atm = (AreaTableModel) areaTable.getModel();
        ace = (AbstractCellEditor) areaTable.getCellEditor();
        if (ace != null) {
            ace.stopCellEditing();
        }

        
        //camera ROI not supported yet. current ROI saved, cleared before acquisition start, and restored after autoimage closes 
        try {
            FieldOfView fov=currentAcqSetting.getFieldOfView();
            cameraROI = core.getROI();
            if (cameraROI != null
                    && (cameraROI.width != fov.getROI_Pixel(currentAcqSetting.getBinning()).width 
                    || cameraROI.height != fov.getROI_Pixel(currentAcqSetting.getBinning()).height)) {
                JOptionPane.showMessageDialog(this,"ROIs are not supported.\nUsing full camera chip.");
                core.clearROI();
//                getCameraSpecs(currentDee);
                for (AcqSetting setting:acqSettings) {
//                    fov=new FieldOfView(currentDetector.getFullWidth_Pixel(), currentDetector.getFullHeight_Pixel(),currentDetector.getFieldRotation());
//                    setting.setFieldOfView(fov);
                    setting.getFieldOfView().clearROI();
        //            setting.getFieldOfView().clearROI();
                }            
            }
        } catch (Exception ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        }

        /*for future: set roi for camera/detector
        Rectangle roi=currentAcqSetting.getFieldOfView().roi_Pixel_bin;
        try {
            core.setROI(roi.x,roi.y,roi.width,roi.height);
        } catch (Exception ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        */
//        cAcqSettingIdx=0;
        String s = null;
//        if (acqSettings!=null && acqSettings.size() > 0)
        if (currentAcqSetting != null) {
            recalculateTiles = false;
        }
//            acqSettingListBox.setSelectedIndex(0); //updates List, GUI, currentAcqSetting
        acqSettingTable.setRowSelectionInterval(0, 0);
//            currentAcqSetting=acqSettings.get(cAcqSettingIdx);
//            updateAcqSettingTab(currentAcqSetting);
//            ((LayoutPanel)acqLayoutPanel).setAcqSetting(currentAcqSetting);
        recalculateTiles = true;
        imageDestPath = createUniqueExpName(rootDirLabel.getText(), experimentTextField.getText());
//        sequenceDirMap = new HashMap<String, String>();
        File imageDestDir = new File(imageDestPath);
        if (!imageDestDir.exists()) {
            if (!imageDestDir.mkdirs()) {
                JOptionPane.showMessageDialog(this,"Cannot create Directory: " + imageDestPath);
                return null;
            }
        }      
        
        //check if autofocus function is available and set up tilemanagers for all acquisition sequences
        for (AcqSetting setting:acqSettings) {
            if (setting.isAutofocus() && !app.getAutofocusManager().hasDevice(setting.getAutofocusDevice())) {
                JOptionPane.showMessageDialog(this,"Autofocus device "+setting.getAutofocusDevice()+ " not available");
                return null;
            }
            
            //create new copy of processor
            //this is required to make sure stopRequested is false
            try {
                JSONObject procTreeObject = Utils.processortreeToJSONObject(setting.getImageProcessorTree(),setting.getImageProcessorTree());
                DefaultMutableTreeNode newProcTree=Utils.createProcessorTree(procTreeObject);
                setting.setImageProcTree(newProcTree);
                //update reference to image processor tree in GUI
                if (setting == acqSettings.get(0)) {
                    updateProcessorTreeView(setting);
                }
            } catch (JSONException ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            DefaultMutableTreeNode node=setting.getImageProcessorTree();
            Enumeration<DefaultMutableTreeNode> en=node.preorderEnumeration();
            while (en.hasMoreElements()) {
                node=en.nextElement();
                DataProcessor dp=(DataProcessor)node.getUserObject();
                if (dp instanceof IDataProcessorNotifier) {
                    ((IDataProcessorNotifier)dp).addListener(this);
                }
                if (dp instanceof RoiFinder) {
                    RoiFinder roifinder=(RoiFinder)dp;
                    roifinder.removeAllTileManagers();
                    for (String selSeq:roifinder.getSelSeqNames()) {
                        boolean sequenceFound=false;
                        for (AcqSetting as:acqSettings) {
                            if (as.getName().equals(selSeq)) {
                                as.getTileManager().setAcquisitionLayout(acqLayout);
                                as.getTileManager().clearList();
                                roifinder.addTileManager(as.getTileManager());
                                sequenceFound=true;
                                break;
                            }
                        }
                        if (!sequenceFound) {
                            JOptionPane.showMessageDialog(this,"RoiFinder in setting "+setting.getName()+": Cannot find sequence setting "+selSeq);
                            return null;
                        }
                    }
                }    
            }
        }
        
        saveExperimentSettings(new File(imageDestPath,"ExpSettings.txt"));
        
        //convert relative delay startTime to absolute times
        Calendar now=new GregorianCalendar();
        IJ.log("Acquisition starting at: "+now.getTime().toString());
        long lastms=now.getTimeInMillis();
        long ms=0;
        for (AcqSetting setting:acqSettings) {
            switch (setting.getStartTime().type) {
                case AcqSetting.ScheduledTime.ASAP: {
                    //convert to absolute time 
                    ms=lastms;
                    break;
                }
                case AcqSetting.ScheduledTime.DELAY: {
                    //convert to absolute time    
                    ms=lastms + setting.getStartTime().startTimeMS;
                    break;
                }
                case AcqSetting.ScheduledTime.ABSOLUTE: {
                    ms=setting.getStartTime().startTimeMS;
                    break;
                }
            }    
            Calendar scheduled=new GregorianCalendar();
            scheduled.setTimeInMillis(ms);
            IJ.log(setting.getName() +" scheduled for: "+scheduled.getTime().toString());
            setting.setAbsoluteStart(scheduled);
            lastms=ms;
        }
        s = runAcquisition(currentAcqSetting);
        return s;
    }

    private void layoutScrollPaneComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_layoutScrollPaneComponentResized
//        IJ.log("AcqFrame.layoutScrollPaneComponentResized: resizing");
        Rectangle r = layoutScrollPane.getVisibleRect();
        ((LayoutPanel) acqLayoutPanel).calculateScale(r.width, r.height);
    }//GEN-LAST:event_layoutScrollPaneComponentResized

    private void acqLayoutPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_acqLayoutPanelMouseClicked
        if (evt.getSource() == acqLayoutPanel) {
//            IJ.log("MouseClicked: "+Integer.toString(evt.getX())+", "+Integer.toString(evt.getY()));
            double coordX = ((LayoutPanel) acqLayoutPanel).convertPixToLayoutCoord(evt.getX());
            double coordY = ((LayoutPanel) acqLayoutPanel).convertPixToLayoutCoord(evt.getY());
//            IJ.log("Layout Pos: "+Double.toString(coordX)+", "+Double.toString(coordY));
            JViewport vp = layoutScrollPane.getViewport();
            Dimension vpSize = vp.getViewSize();
            Rectangle vpRect = vp.getViewRect();
            if (moveToMode) {
                if (coordX >= 0 && coordX <= acqLayout.getWidth() && coordY >= 0 && coordY < acqLayout.getLength()) {
                    if (((LayoutPanel) acqLayoutPanel).getZoom() != 1) {
                        Point newPos = new Point();
                        newPos.x = Math.max(0, (int) Math.round(evt.getX() - vpRect.width / 2));
                        if (newPos.x + vpRect.width > vpSize.width) {
                            newPos.x = vpSize.width - vpRect.width;
                        }
                        newPos.y = Math.max(0, (int) Math.round(evt.getY() - vpRect.height / 2));
                        if (newPos.y + vpRect.height > vpSize.height) {
                            newPos.y = vpSize.height - vpRect.height;
                        }
                        vp.setViewPosition(newPos);
                    }
                    //RefArea lm = acqLayout.getLandmark(0);
                    if (SwingUtilities.isLeftMouseButton(evt)) {
                        moveToLayoutPos(coordX, coordY);
                    } else if (SwingUtilities.isRightMouseButton(evt)) {                        
                        moveToAreaDefaultPos(acqLayout.getAreaByLayoutPos(coordX, coordY));
                    }
                }
            } else if (zoomMode) {
                double oldZoom = ((LayoutPanel) acqLayoutPanel).getZoom();
                double newZoom = oldZoom;
                if (SwingUtilities.isLeftMouseButton(evt)) {
//                    IJ.log("zooming in: vpSize: "+vpSize.toString()+", vpRect: "+vpRect.toString());
                    newZoom = ((LayoutPanel) acqLayoutPanel).zoomIn(); //calls acqLayoutPanel.setSize
//                    IJ.log("zooming in: vpSize: "+vpSize.toString()+", vpRect: "+vpRect.toString());
//                    IJ.log("-----");
                }
                if (SwingUtilities.isRightMouseButton(evt)) {
//                    IJ.log("zooming out: vpSize: "+vpSize.toString()+", vpRect: "+vpRect.toString());
                    newZoom = ((LayoutPanel) acqLayoutPanel).zoomOut(vpRect.width, vpRect.height); //calls acqLAyoutPanel.setSize
//                    IJ.log("zooming out: vpSize: "+vpSize.toString()+", vpRect: "+vpRect.toString());
//                    IJ.log("-----");
                }
                if ((newZoom != 1) && (newZoom != oldZoom)) {
                    Point newPos = new Point();
                    Dimension vpSizeNew = vp.getViewSize();
//                    Dimension vpSizeNew=new Dimension((int)Math.round(vpSize.width*newZoom/oldZoom),(int)Math.round(vpSize.height*newZoom/oldZoom));
                    Rectangle vpRectNew = vp.getViewRect();
                    double x = (double) evt.getX() * newZoom / oldZoom;
                    newPos.x = Math.max(0, (int) Math.round(x - vpRectNew.width / 2));
                    if (newPos.x + vpRectNew.width > vpSizeNew.width) {
                        newPos.x = vpSizeNew.width - vpRectNew.width;
                    }
                    double y = (double) evt.getY() * newZoom / oldZoom;
                    newPos.y = Math.max(0, (int) Math.round(y - vpRectNew.height / 2));
                    if (newPos.y + vpRectNew.height > vpSizeNew.height) {
                        newPos.y = vpSizeNew.height - vpRectNew.height;
                    }
                    vp.setViewPosition(newPos);
                }
            }
        }
    }//GEN-LAST:event_acqLayoutPanelMouseClicked

    private void acqLayoutPanelMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_acqLayoutPanelMouseMoved
        double coordX = ((LayoutPanel) acqLayoutPanel).convertPixToLayoutCoord(evt.getX());
        double coordY = ((LayoutPanel) acqLayoutPanel).convertPixToLayoutCoord(evt.getY());
        String xStr = String.format("%1$,.2f", coordX);
        String yStr = String.format("%1$,.2f", coordY);
        cursorLabel.setText("Layout: " + xStr + ": " + yStr);
    }//GEN-LAST:event_acqLayoutPanelMouseMoved

    private void acqLayoutPanelMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_acqLayoutPanelMousePressed
        //       if (evt.getSource()==acqLayoutPanel)
        if (selectMode | commentMode | mergeAreasMode) {
            marking = true;
            markStartScreenPos = new Point(evt.getX(), evt.getY());
//            IJ.log("MousePressed: "+Integer.toString(markStartScreenPos.x)+", "+Integer.toString(markStartScreenPos.y));
            ((LayoutPanel) acqLayoutPanel).setAnchorMousePos(markStartScreenPos);
            if (SwingUtilities.isLeftMouseButton(evt)) {
                isLeftMouseButton = true;
                isRightMouseButton = false;
//                    IJ.log("Left MousePressed");
            } else if (SwingUtilities.isRightMouseButton(evt)) {
                isRightMouseButton = true;
                isLeftMouseButton = false;
//                    IJ.log("Right MousePressed");  
            }
        }
    }//GEN-LAST:event_acqLayoutPanelMousePressed

    private Rectangle2D.Double createRectangle2D(Point2D.Double start, Point2D.Double end) {
        double x = Math.min(start.x, end.x);
        double y = Math.min(start.y, end.y);
        double w = Math.abs(start.x - end.x);
        double h = Math.abs(start.y - end.y);

        Rectangle2D.Double r = new Rectangle2D.Double(x, y, w, h);
        return r;
    }

    private boolean identicalPoints(Point p1, Point p2) {
        return (p1.x == p2.x && p1.y == p2.y);
    }

    private void acqLayoutPanelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_acqLayoutPanelMouseReleased
        if ((marking & selectMode) || (marking & mergeAreasMode) || (marking & commentMode)) {
//            IJ.showMessage("EventHandler", "mouseReleased");
//            IJ.log("MouseReleased: "+Integer.toString(evt.getX())+", "+Integer.toString(evt.getY()));
//            double coordX=((LayoutPanel)acqLayoutPanel).convertPixToPhysCoord(evt.getX());
//            double coordY=((LayoutPanel)acqLayoutPanel).convertPixToPhysCoord(evt.getY());
            markEndScreenPos = new Point(evt.getX(), evt.getY());
//            IJ.log("Screen Pos: "+Integer.toString(markEndScreenPos.x)+", "+Integer.toString(markEndScreenPos.y));
//            Point2D.Double markStartPos=new Point2D.Double(((LayoutPanel)acqLayoutPanel).convertPixToPhysCoord(markStartScreenPos.x),((LayoutPanel)acqLayoutPanel).convertPixToPhysCoord(markStartScreenPos.y));
//            Point2D.Double markEndPos=new Point2D.Double(((LayoutPanel)acqLayoutPanel).convertPixToPhysCoord(markEndScreenPos.x),((LayoutPanel)acqLayoutPanel).convertPixToPhysCoord(markEndScreenPos.y));
            Point2D.Double markStartPos = ((LayoutPanel) acqLayoutPanel).convertPixToLayoutCoord(markStartScreenPos);
            Point2D.Double markEndPos = ((LayoutPanel) acqLayoutPanel).convertPixToLayoutCoord(markEndScreenPos);
            List<Area> al=null;//will hold list of all affected areas
            if (!identicalPoints(markStartScreenPos, markEndScreenPos)) { // drag and relase to mark group of areas
//                IJ.log("acqLayoutPanelMouseReleased: dragged");
                if (selectMode) {
                    if (isLeftMouseButton)
                        al = acqLayout.getUnselectedAreasInsideRect(createRectangle2D(markStartPos, markEndPos));
                    else if (isRightMouseButton)
                        al = acqLayout.getSelectedAreasInsideRect(createRectangle2D(markStartPos, markEndPos));
                } else if (mergeAreasMode || commentMode) {
                    al = acqLayout.getAllAreasInsideRect(createRectangle2D(markStartPos, markEndPos));
                }
            } else { //single click
                if (selectMode) {
                    if (isLeftMouseButton)
                        al = acqLayout.getUnselectedAreasTouching(markEndPos.x, markEndPos.y);
                    else if (isRightMouseButton)
                        al = acqLayout.getSelectedAreasTouching(markEndPos.x, markEndPos.y);
                } else if (mergeAreasMode || commentMode) {
                    al = acqLayout.getAllAreasTouching(markEndPos.x, markEndPos.y);
                }
            }
            AreaTableModel atm = (AreaTableModel) areaTable.getModel();
            if (selectMode && al!=null) {
//                for (int i=0; i<al.size(); i++) {
//                    int id=al.get(i).getId();
                recalculateTiles = false;
                for (Area area : al) {
                    int id = area.getId();
                    for (int j = 0; j < atm.getRowCount(); j++) {
                        int idInRow = (Integer) atm.getValueAt(j, 4);
                        // IJ.log(Integer.toString(id)+", "+Integer.toString(idInRow));
                        if (idInRow == id) {
                            if (isLeftMouseButton) {
                                atm.setValueAt(true, j, 0);
                            } else if (isRightMouseButton) {
                                atm.setValueAt(false, j, 0);
                            }
                        }
                    }
                }
                recalculateTiles = true;
                if (isLeftMouseButton) {
                    calcTilePositions(al, currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), "Selecting Area...");
                } //updateAcqLayoutPanel is called automatically when calctile thread is completed or aborted (in TileCalcMonitor.done)
                else {
                    calcTotalTileNumber();
                    updateAcqLayoutPanel();
                }
                /*ArrayList<Area> allAreas=acqLayout.getAreaArray();
                 for (int i=0; i<a.size(); i++) {
                 int id=a.get(i).getId();
                 for (int j=0; j<allAreas.size(); j++) {
                 if (allAreas.get(j).getId()==id) {
                 allAreas.get(j).setSelected(isLeftMouseButton);
                 }    
                 }    
                 }*/
            } else if (mergeAreasMode) {
//                for (int i=0; i<al.size(); i++)
//                    al.get(i).setSelectedForMerge(true);
                for (Area a : al) {
                    a.setSelectedForMerge(true);
                }
                if (mergeAreasDialog != null) {
                    mergeAreasDialog.addAreas(al);
                }
            } else if (commentMode && isLeftMouseButton && al.size() > 0) {
                GenericDialog gd = new GenericDialog("Change Comment");
                gd.addStringField("New Comment:", "");
                gd.showDialog();
                if (!gd.wasCanceled()) {
                    String annot = gd.getNextString();
//                    for (int i=0; i<al.size(); i++) {
//                        int id=al.get(i).getId();
                    for (Area a : al) {
                        int id = a.getId();
                        for (int j = 0; j < atm.getRowCount(); j++) {
                            int idInRow = (Integer) atm.getValueAt(j, 4);
                            if (idInRow == id) {
                                if (isLeftMouseButton) {
                                    atm.setValueAt(annot, j, 3);
                                }
                            }
                        }
                    }
                }
                commentButton.requestFocus();
            }
            ((LayoutPanel) acqLayoutPanel).updateSelRect(markEndScreenPos, false);
        }
        marking = false;
    }//GEN-LAST:event_acqLayoutPanelMouseReleased

    private void acqLayoutPanelMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_acqLayoutPanelMouseDragged
        if (selectMode || mergeAreasMode || commentMode) {
            double coordX = ((LayoutPanel) acqLayoutPanel).convertPixToLayoutCoord(evt.getX());
            double coordY = ((LayoutPanel) acqLayoutPanel).convertPixToLayoutCoord(evt.getY());
            String xStr = String.format("%1$,.2f", coordX);
            String yStr = String.format("%1$,.2f", coordY);
            cursorLabel.setText("Layout: " + xStr + ": " + yStr);
            ((LayoutPanel) acqLayoutPanel).updateSelRect(new Point(evt.getX(), evt.getY()), true);
        }
    }//GEN-LAST:event_acqLayoutPanelMouseDragged

    //expects position subfolder name --> returns area name which is subfolder name minus "-Site xx"
    private String extractAreaName(String s) {
        return s.substring(0, s.indexOf("-Site "));
    }

    //expects position subfolder name --> returns Site name
    private String extractSiteName(String s) {
        return s.substring(s.indexOf("-Site "));
    }

    //adds String ins before last '.' of extension
    private static String insertStr(String filename, String ins) {
        if (filename == null) {
            return null;
        }
        String afterLastSlash = filename.substring(filename.lastIndexOf('/') + 1);
        int afterLastBackslash = afterLastSlash.lastIndexOf('\\') + 1;
        int dotIndex = filename.length() - (afterLastSlash.length() - afterLastSlash.indexOf('.', afterLastBackslash));
        return filename.substring(0, dotIndex) + ins + filename.substring(dotIndex);
    }

    private String createNewAreaName() {
        String s = "";
        boolean exists = true;
        List<Area> areas = acqLayout.getAreaArray();
        int n = 1;
        while (exists) {
            s = "New_Area_" + Integer.toString(n);
            exists = false;
//            for (int i=0; i<areas.size(); i++) {
            for (Area a : areas) {
                if (s.equals(a.getName())) {
                    exists = true;
                }
            }
            n++;
        }
        return s;
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        //cleanUp();
    }//GEN-LAST:event_formWindowClosing

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        /*        if (stageMonitor==null)
         stageMonitor=new StagePosMonitor();
         if (stageMonitor.isDone())
         stageMonitor.execute();*/
    }//GEN-LAST:event_formWindowOpened

    private void saveLayoutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveLayoutButtonActionPerformed
        saveLayout();
    }//GEN-LAST:event_saveLayoutButtonActionPerformed

    private void experimentTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_experimentTextFieldFocusLost
//        experimentName=experimentTextField.getText();
        File f = new File(imageDestPath, experimentTextField.getText());
        if (f.exists()) {
            imageDestPath=createUniqueExpName(rootDirLabel.getText(),experimentTextField.getName());
            experimentTextField.setText(new File(imageDestPath).getName());
            experimentTextField.selectAll();
        } else {
        }
    }//GEN-LAST:event_experimentTextFieldFocusLost

    private void saveExpSettingFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveExpSettingFileButtonActionPerformed

        File file;
        do {    
            JFileChooser fc= new JFileChooser(expSettingsFile);
            int result=fc.showSaveDialog(this);
            if (result == JOptionPane.CANCEL_OPTION)
                return;
            file=fc.getSelectedFile();
            if (file.exists()) {
                int overwrite=JOptionPane.showConfirmDialog(this,"File already exists.","Save Experiment Settings",JOptionPane.YES_NO_CANCEL_OPTION);
                if (overwrite == JOptionPane.CANCEL_OPTION)
                    return;
                if (overwrite == JOptionPane.OK_OPTION)
                    break;
            } else if (!file.isDirectory()) {
                File dir = file.getParentFile();
                if (dir.exists()){
                    break;
                } else {
                    JOptionPane.showMessageDialog(this,"Path "+dir.getAbsolutePath()+" does not exist.");
                }
            } else
                break;
        } while (true);
        expSettingsFile=file;
        expSettingsFileLabel.setText(file.getName());
        expSettingsFileLabel.setToolTipText(file.getAbsolutePath());
        layoutFileLabel.setToolTipText("LOADED FROM: "+file.getAbsolutePath());
        saveExperimentSettings(expSettingsFile);
    }//GEN-LAST:event_saveExpSettingFileButtonActionPerformed

    private void loadExpSettingFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadExpSettingFileButtonActionPerformed
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                String name = f.getName().toLowerCase();

                return name.endsWith(".txt") || f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "txt files";
            }
        });
        fc.setCurrentDirectory(new File(expSettingsFile.getPath()));
        int returnVal = fc.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!Utils.getExtension(f).toLowerCase().equals(".txt")) {
                JOptionPane.showMessageDialog(null, 
                        "Experiment setting files have to be in txt format.\nSettings have not been loaded.", "", JOptionPane.ERROR_MESSAGE);
                return;
            }
            loadExpSettings(fc.getSelectedFile(),false); 
//            IJ.showMessage("after loadExp "+(acqSettingTable.getColumnModel().getColumn(1).getCellEditor() instanceof StartTimeEditor ? "yes" : "no"));
        }
    }//GEN-LAST:event_loadExpSettingFileButtonActionPerformed

    private void acquireButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_acquireButtonActionPerformed
        //deal with aborted acquisition first
        if ((acqEng2010.isRunning() && isAcquiring) || isWaiting) {
            //stop acquisition
            Object[] options = {"Stop all sequences",
                    "Skip to next sequence",
                    "Cancel"};
            int n = JOptionPane.showOptionDialog(this,
                "Do you want to stop this acquisition?",
                "Abort Acquisition",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[2]);
            switch (n) {
                case 0: {//stop all sequences;
                    IJ.log("Aborting all sequences");
                    isAborted = true;
                    isInitializing=false;
                    if (!isWaiting) {
                        acqEng2010.stop();
                    } else {
                        if (acquisitionTask!=null) {
                            acquisitionTask.cancel();
                        } 
                        isWaiting=false;
                        imagingFinished(null);
                    }
                    break;
                }
                case 1: {//skip to next sequence
                    IJ.log("Aborting sequence");
                    isInitializing=false;
                    if (!isWaiting) {
                        acqEng2010.stop();
                    } else {
                        if (acquisitionTask!=null) {
                            acquisitionTask.cancel();
                        } else {
                        }
                        isWaiting=false;
                        imagingFinished(null);
                    }
                    break;
                }
                case 2: {
                    return;
                }
            }
//            imagingFinished(null);
//            displayUpdater.imagingFinished(null);
//            IJ.log("AcqFrame.acquireButtonActionPerformed: Acquisition aborted");
            return;
        }

        //start acquisition
        int dupIndex = acqLayout.hasDuplicateAreaNames();
        if (dupIndex != -1) {
            JOptionPane.showMessageDialog(this, "Each Area name has to be unique.\n"
                    + "Area name '" + acqLayout.getAreaByIndex(dupIndex).getName() + "' is used more than once.", "Acquisition", JOptionPane.ERROR_MESSAGE);
            sequenceTabbedPane.setSelectedIndex(0);
            return;
        }
        if (acqLayout.getNoOfSelectedAreas() < 1) {
            JOptionPane.showMessageDialog(this, "No Areas selected.", "Acquisition", JOptionPane.ERROR_MESSAGE);
            sequenceTabbedPane.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < acqSettings.size(); i++) {//(AcqSetting setting:acqSettings) {
            AcqSetting setting = acqSettings.get(i);
            if (setting.hasDuplicateChannels()) {
                JOptionPane.showMessageDialog(this, "Cannot use same channel twice.", "Acquisition: " + setting.getName(), JOptionPane.ERROR_MESSAGE);
                recalculateTiles = false;
                acqSettingTable.setRowSelectionInterval(i, i);
//                acqSettingListBox.setSelectedValue(setting.getName(),true);
                recalculateTiles = true;
                sequenceTabbedPane.setSelectedIndex(1);
                channelTable.requestFocusInWindow();
                return;
            }
            if (setting.getChannels().size() < 1) {
                JOptionPane.showMessageDialog(this, "Add Channel for acquisition sequence " + setting.getName() + ".", "Acquisition: " + setting.getName(), JOptionPane.ERROR_MESSAGE);
                recalculateTiles = false;
                acqSettingTable.setRowSelectionInterval(i, i);
//                acqSettingListBox.setSelectedValue(setting.getName(), true);
                recalculateTiles = true;
                sequenceTabbedPane.setSelectedIndex(1);
                channelTable.requestFocusInWindow();
                return;
            }
            if (setting.getImagePixelSize() == -1) {
                recalculateTiles = false;
                acqSettingTable.setRowSelectionInterval(i, i);
//                acqSettingListBox.setSelectedValue(setting.getName(), true);
                recalculateTiles = true;
                JOptionPane.showMessageDialog(this, "Pixel size is not calibrated for acquisition sequence\n " + setting.getName() + ".", "Acquisition", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!Utils.imageStoragePresent(setting.getImageProcessorTree())) {
                JOptionPane.showMessageDialog(this, "No Image Storage node defined for Acquisition sequence "+setting.getName());
                recalculateTiles = false;
                acqSettingTable.setRowSelectionInterval(i, i);
                recalculateTiles = true;
                sequenceTabbedPane.setSelectedIndex(2);
                processorTreeView.requestFocusInWindow();
                return;
            }
            if (setting.getTilingMode() == TilingSetting.Mode.RUNTIME) {
                if (setting==acqSettings.get(0)) {
                    recalculateTiles = false;
                    acqSettingTable.setRowSelectionInterval(i, i);
//                    acqSettingListBox.setSelectedValue(setting.getName(), true);
                    recalculateTiles = true;
                    JOptionPane.showMessageDialog(this, "'Runtime Tiling' cannot not be used for the first acquisition sequence.");
                    return;
                }
                boolean roiFinderDefined=false;
                List<String> list=new ArrayList<String>();
                for (AcqSetting set:acqSettings) {
                    if (roiFinderDefined || set.getName().equals(setting.getName())) //ROIFinder needs to be defined in one of prior settings
                        break;
                    list.add(set.getName());
                    Enumeration<DefaultMutableTreeNode> en=set.getImageProcessorTree().preorderEnumeration();
                    while (en.hasMoreElements()) {
                        DefaultMutableTreeNode node=en.nextElement();
                        DataProcessor dp=((DataProcessor)node.getUserObject());
                        if (dp instanceof RoiFinder && (((RoiFinder)dp).getSelSeqNames().contains(setting.getName()))) {
                            roiFinderDefined=true;
                            break;
                        }
                    }
                    
                }
                if (!roiFinderDefined) {
                    String s="";
                    for (String seq:list)
                        s=s+seq+"\n";
                        JOptionPane.showMessageDialog(this, "In order to run acquisition sequence "+setting.getName()+ " in 'Runtime Tiling' mode, \n "
                                + "a ROIFinder DataProcessor needs to be configured in one of these sequences: \n\n"
                                + s);
                        return;
                }
            }
        }
        if (app.isLiveModeOn()) {
            JOptionPane.showMessageDialog(this, "Live Mode is running.\n"
                    + "Stop Live Mode before starting another acquisition.", "Acquisition", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (acqEng2010.isRunning() && !isAcquiring) {
            JOptionPane.showMessageDialog(this, "Acquisition is running in separate program instance.\n"
                    + "Cannot start another acquisition", "Acquisition", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!acqEng2010.isRunning() && !isAcquiring) {
            //try start new acquisition; if not successfull it returns null
            if (currentAcqSetting != null) {
                isAborted = false;
                double fieldRot=0;
                for (AcqSetting setting:acqSettings) {
                    if (setting.getFieldOfView().getFieldRotation() == FieldOfView.ROTATION_UNKNOWN) { 
                        fieldRot=FieldOfView.ROTATION_UNKNOWN;
                        break;
                    }    
                }
//                double fieldRot=currentDetector.getFieldRotation();
                if (fieldRot == FieldOfView.ROTATION_UNKNOWN) {
                    int result=JOptionPane.showConfirmDialog(this,"Camera field rotation unknown.\nTiling may create gaps.\nDo you want to run the camera rotation tool?","Warning",JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.NO_OPTION) {
                        fieldRot=0;
                        for (AcqSetting setting:acqSettings) {
                            setting.getFieldOfView().setFieldRotation(fieldRot);
                        }
                        Area.setCameraRot(fieldRot);
                        RefArea.setCameraRot(fieldRot);
                        acqLayoutPanel.repaint();
//                        break;
                    }
                    if (result ==  JOptionPane.YES_OPTION) {
                        showCameraRotDlg(false);//true: run as modal dialog
                        return;                              
                    }
                    
                }
//                for (AcqSetting setting:acqSettings) {
//                    setting.getFieldOfView().setFieldRotation(fieldRot);
//                }
                isAcquiring = startAcquisition() != null;
                isProcessing=isAcquiring;
            } else {
                JOptionPane.showMessageDialog(this,"Undefined AcqSettings");
            }
            return;
        }
        if (!acqEng2010.isRunning() && isAcquiring) {
            //problem in syncing GUI and acqEng2010 status
//            imagingFinished(null);
            if (displayUpdater != null) {
//                displayUpdater.imagingFinished(null);
            }
//            IJ.log("AcqFrame.acquireButtonActionPerformed: Problem - GUI is unaware that acqEng2010 is not running");
        } else {
//            IJ.log("Acquisition is still running");
        }
    }//GEN-LAST:event_acquireButtonActionPerformed

    private void browseImageDestPathButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseImageDestPathButtonActionPerformed
        JFileChooser fc = new JFileChooser();
        fc.setApproveButtonText("Select");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setCurrentDirectory(new java.io.File(imageDestPath));
        int returnVal = fc.showDialog(null, "Select");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (fc.getSelectedFile().exists()) {
                imageDestPath = fc.getSelectedFile().getAbsolutePath();
            } else {
                imageDestPath = fc.getCurrentDirectory().getAbsolutePath();
            }
            rootDirLabel.setText(imageDestPath);
            rootDirLabel.setToolTipText(imageDestPath);
        }
    }//GEN-LAST:event_browseImageDestPathButtonActionPerformed

    private void loadLayoutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadLayoutButtonActionPerformed
        if (acqLayout!=null && acqLayout.isModifed()) {
            int result=JOptionPane.showConfirmDialog(this, "Acquisition layout "+acqLayout.getName()
                    +" has been modified.\nDo you want to save it before opening a new layout?", 
                    "Acquisition Layout",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (result == JOptionPane.CANCEL_OPTION) {
                return;
            }
            if (result == JOptionPane.YES_OPTION) {
                saveLayout();
            }
        }
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                String name = f.getName().toLowerCase();

                return name.endsWith(".txt") || f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "txt files";
            }
        });
        fc.setCurrentDirectory(new File(acqLayout.getFile().getPath()));
        int returnVal = fc.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!Utils.getExtension(f).toLowerCase().equals(".txt")) {
                JOptionPane.showMessageDialog(null, "Layout files have to be in txt format.\nLayout has not been loaded.", "", JOptionPane.ERROR_MESSAGE);
                return;
            }
            AcqLayout layout=AcqLayout.loadLayout(f);
            if (layout==null) {
                if (f.getName().equals("LastExpSetting.txt"))
                    JOptionPane.showMessageDialog(this, "Last used layout file could not be found or read!");
                else
                    JOptionPane.showMessageDialog(this, "Layout file '"+f.getName()+"' could not be found or read!");
                //create empty layout
                //            acqLayout=new AcqLayout();
            } else {
                acqLayout=layout;
                ((LayoutPanel) acqLayoutPanel).setAcquisitionLayout(acqLayout, getMaxFOV());
                layoutFileLabel.setText(acqLayout.getName());
                layoutFileLabel.setToolTipText("LOADED FROM: "+acqLayout.getFile().getAbsolutePath());
                lastArea = null;
                initializeAreaTable();
                setLandmarkFound(false);
                Area.setStageToLayoutRot(0);
                RefArea.setStageToLayoutRot(0);
                acqLayoutPanel.setCursor(normCursor);
                recalculateTiles = false;
                for (AcqSetting as : acqSettings) {
                    as.enableSiteOverlap(true);
                    as.enableInsideOnly(false);
                }
                siteOverlapCheckBox.setSelected(currentAcqSetting.isSiteOverlap());
                insideOnlyCheckBox.setSelected(currentAcqSetting.isInsideOnly());
                if (mergeAreasDialog != null) {
                    mergeAreasDialog.removeAllAreas();
                    mergeAreasDialog.setAcquisitionLayout(acqLayout);
                }
                if (refPointListDialog!=null) {
                    if (stageMonitor!=null) {
                        stageMonitor.removeListener(refPointListDialog);
                    }
                    refPointListDialog.dispose();
                }                
                refPointListDialog = new RefPointListDialog(this, app, acqLayout);
                refPointListDialog.addWindowListener(this);
                refPointListDialog.addListener(this);
                stageMonitor.addListener(refPointListDialog);
                updatePixelSize(currentAcqSetting.getObjective());
                recalculateTiles = true;
                calcTilePositions(null, currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), ADJUSTING_SETTINGS);
                Rectangle r = layoutScrollPane.getVisibleRect();
                ((LayoutPanel) acqLayoutPanel).calculateScale(r.width, r.height);
                areaTable.revalidate();
                areaTable.repaint();
                //        layoutScrollPane.revalidate();
                //        layoutScrollPane.repaint();
            }
            //            layoutScrollPane.setViewportView(acqLayoutPanel);
        }
    }//GEN-LAST:event_loadLayoutButtonActionPerformed

    private void updateTileOverlap() {
        String s = tileOverlapField.getText();
        double o = 0;
        try {
            o = (double) Integer.parseInt(tileOverlapField.getText()) / 100;
        } catch (NumberFormatException nfe) {
        }
        /*        if ((o > AcqSetting.MAX_TILE_OVERLAP)) {
         JOptionPane.showMessageDialog(null, "Error: Please enter a number between 0-"+Integer.toString(AcqSetting.MAX_TILE_OVERLAP)+".", "Error Message", JOptionPane.ERROR_MESSAGE);
         //tileOverlap=0.2;
         double tileO=0;
         if (acqSettings!=null && acqSettings.size()>0) {
         AcqSetting setting = acqSettings.get(cAcqSettingIdx);
         if (currentAcqSetting!=null) {
         tileO=currentAcqSetting.getTileOverlap();
         }
         tileOverlapField.setText(Integer.toString((int)(tileO*100)));
         tileOverlapField.selectAll();
         } else {*/
        //tileOverlap=(double)o/100;
/*            if (recalculateTiles && acqSettings!=null && acqSettings.size()>0) {
         AcqSetting setting = acqSettings.get(cAcqSettingIdx);*/
        //      && tileOverlapField.getInputVerifier().verify(tileOverlapField)
        if (o != currentAcqSetting.getTileOverlap() && recalculateTiles && !calculating) {
            prevTilingSetting = currentAcqSetting.getTilingSetting().duplicate();
            prevObjLabel = currentAcqSetting.getObjective();
            currentAcqSetting.setTileOverlap(o);
            calcTilePositions(null, currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), ADJUSTING_SETTINGS);
        }
        //            layoutScrollPane.getViewport().revalidate();
        //            layoutScrollPane.getViewport().repaint();
/*        }     */
    }

    private void setFrames() {
        /*        AcqSetting setting=null;
         if (acqSettings!=null && acqSettings.size() > cAcqSettingIdx)
         setting=acqSettings.get(cAcqSettingIdx);*/
        String tpStr = framesField.getText();
        if (tpStr.isEmpty()) {
            tpStr = "0";
            //           timePointsField.setText(tpStr);
        }
        int tp = Integer.parseInt(tpStr);
        if (tp <= 0) {
            JOptionPane.showMessageDialog(null, "Error: Please enter a number bigger than 0", "Error Message", JOptionPane.ERROR_MESSAGE);
            tp = 1;
            if (currentAcqSetting != null) {
                currentAcqSetting.setFrames(tp);
            }
            framesField.setText(Integer.toString(tp));
            framesField.requestFocus();
        } else {
            if (currentAcqSetting != null) {
                currentAcqSetting.setFrames(tp);
            }
            calculateDuration(currentAcqSetting);
        }
    }

    private void setSecInterval() {
        /*        AcqSetting setting=null;
         if (acqSettings!=null && acqSettings.size() > cAcqSettingIdx)
         setting=acqSettings.get(cAcqSettingIdx);*/
        String sStr = intSecField.getText();
        if (sStr.isEmpty()) {
            sStr = "0";
            intSecField.setText(sStr);
            if (currentAcqSetting != null) {
                currentAcqSetting.setSecondsInterval(0);
            }
            calculateDuration(currentAcqSetting);
        } else {
            int s = Integer.parseInt(sStr);
            if ((s < 0) || (s > 59)) {
                JOptionPane.showMessageDialog(this, "Error: Please enter a number between 0-59", "Error Message", JOptionPane.ERROR_MESSAGE);
                if (currentAcqSetting != null) {
                    currentAcqSetting.setSecondsInterval(0);
                }
                intSecField.setText(Integer.toString(0));
                intSecField.requestFocus();
            } else {
                if (currentAcqSetting != null) {
                    currentAcqSetting.setSecondsInterval(s);
                }
                calculateDuration(currentAcqSetting);
            }
        }
    }

    private void setMinInterval() {
        /*        AcqSetting setting=null;
         if (acqSettings!=null && acqSettings.size() > cAcqSettingIdx)
         setting=acqSettings.get(cAcqSettingIdx);*/
        String mStr = intMinField.getText();
        if (mStr.isEmpty()) {
            mStr = "0";
            intMinField.setText(mStr);
            currentAcqSetting.setMinutesInterval(0);
            calculateDuration(currentAcqSetting);
        } else {
            int m = Integer.parseInt(mStr);
            if ((m < 0) || (m > 59)) {
                JOptionPane.showMessageDialog(null, "Error: Please enter a number between 0-59", "Error Message", JOptionPane.ERROR_MESSAGE);
                if (currentAcqSetting != null) {
                    currentAcqSetting.setMinutesInterval(0);
                }
                intMinField.setText(Integer.toString(0));
                intMinField.requestFocus();
            } else {
                if (currentAcqSetting != null) {
                    currentAcqSetting.setMinutesInterval(m);
                }
                calculateDuration(currentAcqSetting);
            }
        }
    }

    private void setHourInterval() {
        /*        AcqSetting setting=null;
         if (acqSettings!=null && acqSettings.size()>cAcqSettingIdx)
         setting=acqSettings.get(cAcqSettingIdx);*/
        String hStr = intHourField.getText();
        if (hStr.isEmpty()) {
            hStr = "0";
            intHourField.setText(hStr);
            if (currentAcqSetting != null) {
                currentAcqSetting.setHoursInterval(0);
            }
            calculateDuration(currentAcqSetting);
        } else {
            int h = Integer.parseInt(hStr);
            if (h > AcqSetting.MAX_HOUR_INT) {
                JOptionPane.showMessageDialog(null, "Error: Please enter a number smaller than " + Integer.toString(AcqSetting.MAX_HOUR_INT), "Error Message", JOptionPane.ERROR_MESSAGE);
                if (currentAcqSetting != null) {
                    currentAcqSetting.setHoursInterval(0);
                }
                intHourField.setText(Integer.toString(0));
                intHourField.requestFocus();
            } else {
                if (currentAcqSetting != null) {
                    currentAcqSetting.setHoursInterval(h);
                }
                calculateDuration(currentAcqSetting);
            }
        }
    }

    private void mergeAreasButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mergeAreasButtonActionPerformed
        if (acqLayout instanceof AcqPlateLayout) {
            JOptionPane.showMessageDialog(this, "Areas cannot be merged in well plate formats.");
            return;
        }
        if (mergeAreasDialog == null) {
            mergeAreasDialog = new MergeAreasDlg(this, acqLayout, app);
            mergeAreasDialog.addWindowListener(this);
            mergeAreasDialog.addListener(this);
        } else {
            mergeAreasDialog.setAcquisitionLayout(acqLayout);
        }
        zoomMode = false;
        selectMode = false;
        selectButton.setSelected(false);
        selectButton.setEnabled(false);
        commentMode = false;
        commentButton.setSelected(false);
        commentButton.setEnabled(false);
        moveToMode = false;
        moveToScreenCoordButton.setSelected(false);
        moveToScreenCoordButton.setEnabled(false);
        setLandmarkButton.setSelected(false);
        setLandmarkButton.setEnabled(false);
        mergeAreasMode = true;
        acqLayoutPanel.setCursor(normCursor);
        mergeAreasDialog.setVisible(true);
    }//GEN-LAST:event_mergeAreasButtonActionPerformed

    private void removeAreaButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeAreaButtonActionPerformed
        List<Area> areas = acqLayout.getAreaArray();
        if (areas.size() > 0) {
            AreaTableModel atm = (AreaTableModel) areaTable.getModel();
            int[] rows = areaTable.getSelectedRows();
            if (rows.length > 0) {
                atm.removeRows(rows);
                acqLayout.setModified(true);
            }
        }
    }//GEN-LAST:event_removeAreaButtonActionPerformed

    private void newAreaButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newAreaButtonActionPerformed
        RefArea rp = acqLayout.getLandmark(0);
        if (rp == null || acqLayout.getNoOfMappedStagePos() == 0) {
            JOptionPane.showMessageDialog(this,"At least one layout landmark needs to be set");
            return;
        } else {
            List<Area> areas = acqLayout.getAreaArray();
            double sX = 0;
            double sY = 0;
            double sZ = 0;
            try {
                sX = core.getXPosition(core.getXYStageDevice());
                sY = core.getYPosition(core.getXYStageDevice());
                sZ = core.getPosition(core.getFocusDevice());
            } catch (Exception ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
            /*            int id=1;
             if (areas!=null & areas.size()>0)
             id=areas.get(areas.size()-1).getId()+1;*/
            double tileWidth=currentAcqSetting.getTileWidth_UM();
            double tileHeight = currentAcqSetting.getTileHeight_UM();
            Vec3d lCoord = acqLayout.convertStagePosToLayoutPos(sX, sY, sZ);
            Area a = new RectArea(createNewAreaName(), acqLayout.createUniqueAreaId(), lCoord.x - tileWidth / 2, lCoord.y - tileHeight / 2, lCoord.z, tileWidth, tileHeight, false, "");
            areas.add(a);
            initializeAreaTable();
            //            AreaTableModel atm=(AreaTableModel)areaTable.getModel();
            //            Object[] data = new Object[]{a.isSelected(), a.getName(),new Integer(0), a.getAnnotation(), a.getId()};
            //            atm.addRow(data);
            acqLayout.setModified(true);
            acqLayoutPanel.repaint();
        }
    }//GEN-LAST:event_newAreaButtonActionPerformed

    private void areaDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_areaDownButtonActionPerformed
        AreaTableModel atm = (AreaTableModel) areaTable.getModel();
        if (atm != null) {
            int[] selRows = areaTable.getSelectedRows();
            if (selRows.length > 0 & selRows[selRows.length - 1] < atm.getRowCount()) {
                int newSelRow = atm.rowDown(selRows);
//                ListSelectionModel lsm = areaTable.getSelectionModel();
                areaTable.setRowSelectionInterval(newSelRow, newSelRow + selRows.length - 1);
                //lsm.addSelectionInterval(newSelRow, newSelRow);
            }
        }
    }//GEN-LAST:event_areaDownButtonActionPerformed

    private void areaUpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_areaUpButtonActionPerformed
        AreaTableModel atm = (AreaTableModel) areaTable.getModel();
        if (atm != null) {
            int[] selRows = areaTable.getSelectedRows();
            if (selRows.length > 0 & selRows[0] > 0) {
                int newSelRow = atm.rowUp(selRows);
//                ListSelectionModel lsm = areaTable.getSelectionModel();
                areaTable.setRowSelectionInterval(newSelRow, newSelRow + selRows.length - 1);
                //lsm.addSelectionInterval(newSelRow, newSelRow);
            }
        }
    }//GEN-LAST:event_areaUpButtonActionPerformed

    private void acqSettingDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_acqSettingDownButtonActionPerformed
        AcqSettingTableModel ctm = (AcqSettingTableModel) acqSettingTable.getModel();
        int[] selRows = acqSettingTable.getSelectedRows();
        if (selRows.length > 0 & selRows[selRows.length - 1] < ctm.getRowCount() - 1) {
            int newSelRow = ctm.rowDown(selRows);
            acqSettingTable.setRowSelectionInterval(newSelRow, newSelRow + selRows.length - 1);
//            cAcqSettingIdx=newSelRow;
        }
    }//GEN-LAST:event_acqSettingDownButtonActionPerformed

    private String createAcqSettingName() {
        boolean unique = false;
        int i = 0;
        while (!unique) {
            i++;
            for (AcqSetting setting : acqSettings) {
                if (setting.getName().equals("Seq_" + Integer.toString(i))) {
                    unique = false;
                    break;
                } else {
                    unique = true;
                }
            }
        }
        return "Seq_" + Integer.toString(i);
    }

    private void addAcqSettingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAcqSettingButtonActionPerformed
        AcqSetting setting = new AcqSetting(createAcqSettingName(), new FieldOfView(currentAcqSetting.getFieldOfView()), availableObjectives.get(0), getObjPixelSize(availableObjectives.get(0)), currentDetector.getBinningOption(0,1), false);
        //passes on copy of currentAcq.fieldOfView, including currentROI setting
//        AcqSetting setting = new AcqSetting(createAcqSettingName(), new FieldOfView(currentAcqSetting.getFieldOfView()), availableObjectives.get(0), getObjPixelSize(availableObjectives.get(0)), false);
        //let user select ChannelGroupStr. if aborted, return value is null --> no sequence added
        String chGroupStr=MMCoreUtils.loadAvailableChannelConfigs(this,"",core);
        if (chGroupStr==null)
            return;
        setting.setChannelGroupStr(chGroupStr);
        AcqSettingTableModel atm = (AcqSettingTableModel) acqSettingTable.getModel();
        int[] selectedRows = acqSettingTable.getSelectedRows();
        int newRow;
        if (selectedRows.length > 0) { //add after last selected row
            newRow = selectedRows[selectedRows.length - 1] + 1;
            atm.addRow(setting, newRow);
        } else {//add to end
            atm.addRow(setting, -1);
            newRow = atm.getRowCount() - 1;
        }

        acqSettingTable.setRowSelectionInterval(newRow, newRow);
        deleteAcqSettingButton.setEnabled(atm.getRowCount() > 1);

        /*
         acqSettings.add(setting);
         DefaultListModel lm = (DefaultListModel)acqSettingListBox.getModel();
         lm.addElement(setting.getName());
         acqSettingListBox.revalidate();
         acqSettingListBox.setSelectedIndex(acqSettings.size()-1);
         */
        //updateAcqSettingGUI(setting);


    }//GEN-LAST:event_addAcqSettingButtonActionPerformed

    private void setClusterXField() {
        /*        String s=clusterXField.getText();
         if (s.isEmpty())
         s="1";*/
        try {
            int nr = Integer.parseInt(clusterXField.getText());
            /*        if (nr<0 | nr > AcqSetting.MAX_CLUSTER_X) {
             JOptionPane.showMessageDialog(null, "Error: Please enter a number between 1-"+Integer.toString(AcqSetting.MAX_CLUSTER_X)+".", "Error Message", JOptionPane.ERROR_MESSAGE);
             clusterXField.setText(Integer.toString(1));
             clusterXField.requestFocus();
             } else {*/
            if (nr != currentAcqSetting.getNrClusterX() && !calculating && recalculateTiles) {
                prevObjLabel = currentAcqSetting.getObjective();
                prevTilingSetting = currentAcqSetting.getTilingSetting().duplicate();
                currentAcqSetting.setNrClusterX(nr);
                calcTilePositions(null, currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), ADJUSTING_SETTINGS);
            }
        } catch (NumberFormatException nfe) {
        }
        /*        }*/
    }

    private void setClusterYField() {
        /*        String s=clusterYField.getText();
         if (s.isEmpty())
         s="1";*/
        try {
            int nr = Integer.parseInt(clusterYField.getText());

            /*        if (nr<0 | nr > AcqSetting.MAX_CLUSTER_Y) {
             JOptionPane.showMessageDialog(null, "Error: Please enter a number between 1-"+Integer.toString(AcqSetting.MAX_CLUSTER_Y)+".", "Error Message", JOptionPane.ERROR_MESSAGE);
             clusterYField.setText(Integer.toString(1));
             clusterYField.requestFocus();
             } else {*/
            if (nr != currentAcqSetting.getNrClusterY() && !calculating && recalculateTiles) {
                prevObjLabel = currentAcqSetting.getObjective();
                prevTilingSetting = currentAcqSetting.getTilingSetting().duplicate();
                currentAcqSetting.setNrClusterY(nr);
                calcTilePositions(null, currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), ADJUSTING_SETTINGS);
            }
        } catch (NumberFormatException nfe) {
        }
        /*        }*/
    }

    private void setMaxSites() {
        try {
            int nr = Integer.parseInt(maxSitesField.getText());
            if (nr != currentAcqSetting.getMaxSites() && !calculating && recalculateTiles) {
                prevObjLabel = currentAcqSetting.getObjective();
                prevTilingSetting = currentAcqSetting.getTilingSetting().duplicate();
                currentAcqSetting.setMaxSites(nr);
                calcTilePositions(null, currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), ADJUSTING_SETTINGS);
            }
        } catch (NumberFormatException nfe) {
        }
    }

    private void cancelCalculateTiles() {
        if (retilingExecutor != null && !retilingExecutor.isTerminated()) {
//            IJ.log("AcqFrame.cancelThreadButtonActionPerformed");
            retilingAborted = true;
            calculating = false;
            for (Area a:acqLayout.getAreaArray())
                a.setUnknownTileNum(true);
            retilingExecutor.shutdownNow();
        }        
    }
    
    private void cancelThreadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelThreadButtonActionPerformed
        cancelCalculateTiles();
    }//GEN-LAST:event_cancelThreadButtonActionPerformed

    private void deleteAcqSettingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteAcqSettingButtonActionPerformed
        if (acqSettingTable.isEditing())
            acqSettingTable.getCellEditor().stopCellEditing();
        AcqSettingTableModel atm = (AcqSettingTableModel) acqSettingTable.getModel();
        if (atm.getRowCount() <= 1) {
            JOptionPane.showMessageDialog(this, "Setting cannot be deleted.\nAt least one setting needs to be defined.");
        } else if (acqSettingTable.getSelectedRowCount() > 0) {
            int[] selectedRows = acqSettingTable.getSelectedRows();
            int firstRow = selectedRows[0];
            atm.removeRows(selectedRows);
            if (firstRow >= atm.getRowCount()) {
                acqSettingTable.setRowSelectionInterval(atm.getRowCount() - 1, atm.getRowCount() - 1);
            } else {
                acqSettingTable.setRowSelectionInterval(firstRow, firstRow);
            }
        }
        deleteAcqSettingButton.setEnabled(atm.getRowCount() > 1);

        /*
         DefaultListModel lm = (DefaultListModel)acqSettingListBox.getModel();
         if (lm.getSize() > 1) {
         int[] selected=(int[])acqSettingListBox.getSelectedIndices();
         if (selected.length > 0) {
         for (int i = selected.length-1; i>=0; i--) {
         lm.removeElementAt(selected[i]);
         acqSettings.remove(selected[i]);
         }     
         }
         acqSettingListBox.revalidate();
         acqSettingListBox.setSelectedIndex(selected[0]);
         } else {
         JOptionPane.showMessageDialog(this, "Setting cannot be deleted.\nAt least one setting needs to be defined.");
         }*/
    }//GEN-LAST:event_deleteAcqSettingButtonActionPerformed

    private void acqSettingUpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_acqSettingUpButtonActionPerformed
        AcqSettingTableModel atm = (AcqSettingTableModel) acqSettingTable.getModel();
        int[] selRows = acqSettingTable.getSelectedRows();
        if (selRows.length > 0 & selRows[0] > 0) {
            int newSelRow = atm.rowUp(selRows);
            acqSettingTable.setRowSelectionInterval(newSelRow, newSelRow + selRows.length - 1);
//            cAcqSettingIdx=newSelRow;
        }
    }//GEN-LAST:event_acqSettingUpButtonActionPerformed

    private ImageTagFilter createImageTagFilter(String procName, String tag, List<String> args, boolean isFile) {
        if (procName==null)
            procName=tag;
        ImageTagFilter itf;
        if (tag.contains("Index")) {
/*            if (isFile) {
                List<Integer> argsInteger= new ArrayList<Integer>(args.size());
                for (int i=0;i<args.size(); i++)
                    argsInteger.add(Integer.parseInt(args.get(i)));
                itf=new ImageTagFilterInt<File>(tag, argsInteger);
            } else {*/
                List<Long> argsLong= new ArrayList<Long>(args.size());
                for (int i=0;i<args.size(); i++)
                    argsLong.add(Long.parseLong(args.get(i)));
                itf=new ImageTagFilterLong<TaggedImage>(tag, argsLong);
//            }
        } else {
            if (isFile)
                itf=new ImageTagFilterString<File>(tag, args);            
            else 
                itf=new ImageTagFilterString<TaggedImage>(tag, args);            
        }
        return itf;
    }
    
    private DefaultMutableTreeNode createAndAddProcessorNode(DefaultMutableTreeNode parent, DataProcessor newDp) {
        DefaultTreeModel tm=(DefaultTreeModel)processorTreeView.getModel();
        DefaultMutableTreeNode newNode=new DefaultMutableTreeNode(newDp);
        DataProcessor lastChildDp=null;
        if (parent.getChildCount()>0) {
            lastChildDp=(DataProcessor)((DefaultMutableTreeNode)parent.getLastChild()).getUserObject();
        }
        if (!(newDp instanceof TaggedImageAnalyzer) && !(newDp instanceof BranchedProcessor)
                && lastChildDp!=null && !(lastChildDp instanceof TaggedImageAnalyzer) && !(lastChildDp instanceof BranchedProcessor)) {
            tm.insertNodeInto(newNode,parent,parent.getChildCount()-1);
//            for (int i=parent.getChildCount()-1;i>0;i--) {
                DefaultMutableTreeNode chNode=(DefaultMutableTreeNode)parent.getLastChild();
                tm.removeNodeFromParent(chNode);
                tm.insertNodeInto(chNode, newNode, 0);
            //}
            processorTreeView.setSelectionPath(new TreePath(((DefaultMutableTreeNode)chNode).getPath()));
        } else {
            DataProcessor dp=(DataProcessor)parent.getUserObject();  
            int pos=0;
            if (parent.getChildCount()>0) {   
                if (lastChildDp instanceof BranchedProcessor || lastChildDp instanceof TaggedImageAnalyzer) {
                 //   pos=0;
                    pos=parent.getChildCount();
                }else {
                    pos=parent.getChildCount()-1;
                }    
            }
            tm.insertNodeInto(newNode, parent, pos);
            processorTreeView.setSelectionPath(new TreePath(newNode.getPath()));
        }
        return newNode;
    }
    
    
    private void addImageTagFilterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addImageTagFilterButtonActionPerformed
        DefaultTreeModel tm=(DefaultTreeModel)processorTreeView.getModel();
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                       processorTreeView.getLastSelectedPathComponent();
        if (selectedNode==null) {
            JOptionPane.showMessageDialog(null, "Select Node in Processor Tree");
            return;
        }
/*        ImageTagFilter itf=showImageTagFilterDlg(null,selectedNode);
        if (itf!=null)
            createAndAddProcessorNode(selectedNode,itf);
        ImageTagFilter itf=showImageTagFilterDlg(null,selectedNode);*/
        ImageTagFilter itf = new ImageTagFilterString(ExtImageTags.AREA_NAME,null);
        itf.makeConfigurationGUI();
        itf.dispose();
        if (!"".equals(itf.getKey())) {
            itf=createImageTagFilter("Filter:",itf.getKey(),itf.getValues(), 
                    Utils.isDescendantOfImageStorageNode(currentAcqSetting.getImageProcessorTree(), selectedNode));
            createAndAddProcessorNode(selectedNode,itf);
        }    
    }//GEN-LAST:event_addImageTagFilterButtonActionPerformed
    

    private void addChannelFilterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addChannelFilterButtonActionPerformed
        
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                       processorTreeView.getLastSelectedPathComponent();
        if (selectedNode==null) {
            JOptionPane.showMessageDialog(null, "Select Node in Processor Tree");
            return;
        }
//        ImageTagFilter itf=showChannelFilterDlg(null, selectedNode);
//        if (itf!=null)
//            createAndAddProcessorNode(selectedNode, itf);
        ImageTagFilterOptString itf=new ImageTagFilterOptString(MMTags.Image.CHANNEL_NAME,null);
//        List<String> channels=new ArrayList<String>((int)availableChannels.size());
//        for (String ch:availableChannels)
//            channels.add(ch);
        List<String> chList=new ArrayList<String>();
        for (Channel ch:currentAcqSetting.getChannels())
            chList.add(ch.getName());
        itf.setOptions(chList);
//        itf.setOptions(Arrays.asList(availableChannels.toArray()));
        itf.makeConfigurationGUI();
        itf.dispose();
        if (!"".equals(itf.getKey())) {
            createAndAddProcessorNode(selectedNode,itf);    
        }    
    }//GEN-LAST:event_addChannelFilterButtonActionPerformed

    
    private void addZFilterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addZFilterButtonActionPerformed
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                       processorTreeView.getLastSelectedPathComponent();
        if (selectedNode==null) {
            JOptionPane.showMessageDialog(null, "Select Node in Processor Tree");
            return;
        }
//        ImageTagFilter itf=showZFilterDlg(null, selectedNode);
//        if (itf!=null)
//            createAndAddProcessorNode(selectedNode, itf);
//        ImageTagFilterOpt itf;
/*        if (Utils.isDescendantOfImageStorageNode(currentAcqSetting.getImageProcessorTree(), selectedNode)) {
            itf=new ImageTagFilterOptInt(MMTags.Image.SLICE_INDEX,null);
//            itf=new ImageTagFilterOptLong(MMTags.Image.SLICE_INDEX,null);
        } else { 
            itf=new ImageTagFilterOptLong(MMTags.Image.SLICE_INDEX,null);
//        }
        List<String> slices=new ArrayList<String>();
        for (long l=0;l<currentAcqSetting.getZSlices();l++)
            slices.add(Long.toString(l));
        itf.setOptions(slices);
        itf.makeConfigurationGUI();
        itf.dispose();
        if (!"".equals(itf.getKey())) {
            createAndAddProcessorNode(selectedNode,itf);    
        }    
        */
        
        
        ImageTagFilterOpt itf=new ImageTagFilterOptLong(MMTags.Image.SLICE_INDEX,null);
        List<String> slices=new ArrayList<String>();
        for (long l=0;l<currentAcqSetting.getZSlices();l++)
            slices.add(Long.toString(l));
        itf.setOptions(slices);
        itf.makeConfigurationGUI();
        itf.dispose();
        if (!"".equals(itf.getKey())) {
            createAndAddProcessorNode(selectedNode,itf);    
        }    
    }//GEN-LAST:event_addZFilterButtonActionPerformed

    
    private void addScriptAnalyzerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addScriptAnalyzerButtonActionPerformed
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                       processorTreeView.getLastSelectedPathComponent();
        DefaultTreeModel tm=(DefaultTreeModel)processorTreeView.getModel();
        if (selectedNode==null) {
            JOptionPane.showMessageDialog(null, "Select Node in Processor Tree");
            return;
        }
        if (!Utils.isDescendantOfImageStorageNode((DefaultMutableTreeNode)tm.getRoot(),selectedNode)) {
            JOptionPane.showMessageDialog(null, "Script Analyzers need to be inserted downstream of Image Storage node");
            return;
        }
//        ScriptAnalyzer sa=showScriptAnalyzerDlg(null, selectedNode);
        ScriptAnalyzer sa =new ScriptAnalyzer();
        sa.makeConfigurationGUI();
        sa.dispose();
        if (!"".equals(sa.getScript())) {
            createAndAddProcessorNode(selectedNode,sa);
        }    
    }//GEN-LAST:event_addScriptAnalyzerButtonActionPerformed

    private void removeProcessorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeProcessorButtonActionPerformed
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                       processorTreeView.getLastSelectedPathComponent();
        if (selectedNode==null) {
            JOptionPane.showMessageDialog(null, "Select Node in Processor Tree");
            return;
        }
        DataProcessor dp = (DataProcessor)selectedNode.getUserObject();
        if (dp instanceof ExtDataProcessor 
                && (((ExtDataProcessor)dp).getProcName().equals(ProcessorTree.PROC_NAME_ACQ_ENG))) {
            JOptionPane.showMessageDialog(null, ((ExtDataProcessor)dp).getProcName()+" cannot be removed.");
            return;            
        }
        DefaultTreeModel tm=(DefaultTreeModel)processorTreeView.getModel();
        if (!selectedNode.isLeaf()) {
            JPanel guiPanel = new JPanel();
            guiPanel.setLayout(new BoxLayout(guiPanel, BoxLayout.PAGE_AXIS));
            JRadioButton rb = new JRadioButton("Remove entire branch");
            rb.setActionCommand("remove");
            rb.setSelected(true);
            guiPanel.add(rb);
            JRadioButton kb = new JRadioButton("Move children to parent node");
            kb.setActionCommand("keep");
            guiPanel.add(kb);
            ButtonGroup bg=new ButtonGroup();
            bg.add(rb);
            bg.add(kb);
           // guiPanel.add(bg);

            JOptionPane pane = new JOptionPane(guiPanel,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
            JDialog dlg=pane.createDialog(this, "Remove Processor");
            dlg.setSize(new Dimension(300,150));
            dlg.setVisible(true);
            if ((Integer)pane.getValue() == JOptionPane.OK_OPTION) {
                if (rb.isSelected()) {
                    tm.removeNodeFromParent(selectedNode);
                } else {
                    DefaultMutableTreeNode node;
                    TreeNode lastChildInParentNode=((DefaultMutableTreeNode)selectedNode.getParent()).getLastChild();
                    DataProcessor dpOfLastChildInParentNode=(DataProcessor)((DefaultMutableTreeNode)lastChildInParentNode).getUserObject();
                    int childCount=selectedNode.getChildCount();
                    for (int i=0; i<childCount;i++) {
                        node=(DefaultMutableTreeNode)selectedNode.getChildAt(0);
                        boolean isDescendantOfStorage=Utils.isDescendantOfImageStorageNode(currentAcqSetting.getImageProcessorTree(),node);
                        tm.removeNodeFromParent(node);
                        int insertPos;
                 
                        if (selectedNode.getParent().getChildCount()==1 
                                || ((DefaultMutableTreeNode)selectedNode.getParent()).getLastChild()==selectedNode
                                || dpOfLastChildInParentNode instanceof TaggedImageAnalyzer 
                                || dpOfLastChildInParentNode instanceof BranchedProcessor) {
                            insertPos=selectedNode.getParent().getChildCount();
                            tm.insertNodeInto(node, 
                                    (DefaultMutableTreeNode)selectedNode.getParent(), 
                                    insertPos);
                        } else {
                            insertPos=selectedNode.getParent().getChildCount()-1;                            
                            if ((DataProcessor)node.getUserObject() instanceof TaggedImageAnalyzer ||
                                (DataProcessor)node.getUserObject() instanceof BranchedProcessor) {
                                tm.insertNodeInto(node, 
                                    (DefaultMutableTreeNode)selectedNode.getParent(), 
                                    insertPos);
                            } else if (!(dpOfLastChildInParentNode instanceof TaggedImageAnalyzer) &&
                                !(dpOfLastChildInParentNode instanceof BranchedProcessor)) {
                                JOptionPane.showMessageDialog(null,"Conflict. Two DataProcessors at same hierarchy level. \nAdding branch point.");
                                DefaultMutableTreeNode branchNode;
                                if (isDescendantOfStorage) {
                                    branchNode=createAndAddProcessorNode((DefaultMutableTreeNode)selectedNode.getParent(),new NoFilterSeqAnalyzer<File>(""));
                                } else {
                                    branchNode=createAndAddProcessorNode((DefaultMutableTreeNode)selectedNode.getParent(),new NoFilterSeqAnalyzer<TaggedImage>(""));
                                } 
                                tm.insertNodeInto(node, branchNode, 0);
                            }   
                        }
                    }
                    tm.removeNodeFromParent(selectedNode);
                }
                for (int i=0;i<processorTreeView.getRowCount();i++)
                    processorTreeView.expandRow(i);      
            }
            
        } else { //is leaf                      
            tm.removeNodeFromParent(selectedNode);
        }

    }//GEN-LAST:event_removeProcessorButtonActionPerformed

    private void addROIFinderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addROIFinderButtonActionPerformed
       DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                       processorTreeView.getLastSelectedPathComponent();
        DefaultTreeModel tm=(DefaultTreeModel)processorTreeView.getModel();
        if (selectedNode==null) {
            JOptionPane.showMessageDialog(null, "Select Node in Processor Tree");
            return;
        }
        if (!Utils.isDescendantOfImageStorageNode((DefaultMutableTreeNode)tm.getRoot(),selectedNode)) {
            JOptionPane.showMessageDialog(null, "Script Analyzers need to be inserted downstream of Image Storage node");
            return;
        }
/*        RoiFinder rf=showRoiFinderDlg(null, selectedNode);
        if (rf!=null) {
            createAndAddProcessorNode(selectedNode,rf);
        }*/
        RoiFinder rf=new RoiFinder();
        List<String> list = new ArrayList<String>();
        for (AcqSetting setting:acqSettings) {
            list.add(setting.getName());
        }                  
        rf.setOptions(list);
        rf.makeConfigurationGUI();
        rf.dispose();
        if (!"".equals(rf.getScript())) {
            createAndAddProcessorNode(selectedNode,rf);
        }    

    }//GEN-LAST:event_addROIFinderButtonActionPerformed

    private void editProcessorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editProcessorButtonActionPerformed
        DefaultTreeModel tm=(DefaultTreeModel)processorTreeView.getModel();
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                       processorTreeView.getLastSelectedPathComponent();
        if (selectedNode==null) {
            JOptionPane.showMessageDialog(null, "Select Node in Processor Tree");
            return;
        }
        DataProcessor dp=(DataProcessor)selectedNode.getUserObject();
//        if (dp instanceof ScriptAnalyzer)
//            showScriptAnalyzerDlg((ScriptAnalyzer)dp, selectedNode);
//        else 
//        if (dp instanceof ImageTagFilter && ((ImageTagFilter)dp).getKey().toLowerCase().equals("channel"))
//            showChannelFilterDlg((ImageTagFilter)dp, selectedNode);
//        else 
//          if (dp instanceof ImageTagFilter && ((ImageTagFilter)dp).getKey().toLowerCase().contains("sliceindex"))
//            showZFilterDlg((ImageTagFilter)dp, selectedNode);
//        else if (dp instanceof ImageTagFilter && ((ImageTagFilter)dp).getKey().toLowerCase().contains("area"))
//            showAreaFilterDlg((ImageTagFilter)dp, selectedNode);
//        else if (dp instanceof ImageTagFilter)
//            showImageTagFilterDlg((ImageTagFilter)dp, selectedNode);  
        if (dp instanceof RoiFinder) {
            List<String> list = new ArrayList<String>();
            for (AcqSetting setting:acqSettings) {
                list.add(setting.getName());
            }                
            ((RoiFinder)dp).setOptions(list);
        }    
        if (dp instanceof ImageTagFilterOpt) {
            ImageTagFilterOpt itfo=(ImageTagFilterOpt)dp;
            if (itfo.getKey().equals(MMTags.Image.CHANNEL_NAME)) {
                List<String> chList=new ArrayList<String>();
                for (Channel ch:currentAcqSetting.getChannels())
                    chList.add(ch.getName());
                itfo.setOptions(chList);
                //itfo.setOptions(Arrays.asList(availableChannels.toArray()));
            } else if (itfo.getKey().equals(ExtImageTags.AREA_NAME)) {
                List<String> areas=new ArrayList<String>(acqLayout.getAreaArray().size());
                for (Area a:acqLayout.getAreaArray())
                    areas.add(a.getName());
                itfo.setOptions(areas);            
            } else if (itfo.getKey().equals(MMTags.Image.FRAME_INDEX) 
                    || itfo.getKey().equals(MMTags.Image.FRAME)) { 
                List<String> frames=new ArrayList<String>();
                for (long l=0;l<currentAcqSetting.getFrames();l++)
                    frames.add(Long.toString(l));
                itfo.setOptions(frames);
            } else if (itfo.getKey().equals(MMTags.Image.SLICE_INDEX) 
                    || itfo.getKey().equals(MMTags.Image.SLICE)) { 
                List<String> slices=new ArrayList<String>();
                for (long l=0;l<currentAcqSetting.getZSlices();l++)
                    slices.add(Long.toString(l));
                itfo.setOptions(slices);
            }
        }
        if (dp instanceof ExtDataProcessor) {
            ((ExtDataProcessor)dp).makeConfigurationGUI();
//            ((ExtDataProcessor)dp).dispose();
        } else {
            try {
                Method guiMethod=DataProcessor.class.getDeclaredMethod("makeConfigurationGUI", new Class[]{});
                guiMethod.invoke(dp,new Object[]{});
//                Method disposeMethod=DataProcessor.class.getDeclaredMethod("dispose", new Class[]{});
            } catch (NoSuchMethodException ex) {
                JOptionPane.showMessageDialog(null,"No configuration dialog found for this processor.");
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                JOptionPane.showMessageDialog(null,"SecurityException.");
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                JOptionPane.showMessageDialog(null,"IllegaAccess.");
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(null,"IllegalArgument.");
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                JOptionPane.showMessageDialog(null,"InvocationTargetException.");
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        tm.nodeChanged(selectedNode);
        
//        processorTreeView.updateUI();
    }//GEN-LAST:event_editProcessorButtonActionPerformed

    private void addAreaFilterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAreaFilterButtonActionPerformed
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                       processorTreeView.getLastSelectedPathComponent();
        if (selectedNode==null) {
            JOptionPane.showMessageDialog(null, "Select Node in Processor Tree");
            return;
        }
//        ImageTagFilter itf=showAreaFilterDlg(null, selectedNode);
//        if (itf!=null)
//            createAndAddProcessorNode(selectedNode,itf);    
        ImageTagFilterOptString itf=new ImageTagFilterOptString(ExtImageTags.AREA_NAME,null);
        List<String> areas=new ArrayList<String>(acqLayout.getAreaArray().size());
        for (Area a:acqLayout.getAreaArray())
            areas.add(a.getName());
        itf.setOptions(areas);
        itf.makeConfigurationGUI();
        itf.dispose();
        if (!"".equals(itf.getKey())) {
            createAndAddProcessorNode(selectedNode,itf); 
        }    
    }//GEN-LAST:event_addAreaFilterButtonActionPerformed

    private void loadAcqSettingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadAcqSettingButtonActionPerformed
        JFileChooser fc=new JFileChooser();
        fc.showOpenDialog(this);
        if (fc.getSelectedFile()!=null) {
            List<AcqSetting> list=loadAcquisitionSettings(fc.getSelectedFile());
            if (list!=null) {
                List<String> settingNames=new ArrayList<String>();
                for (AcqSetting s:acqSettings)
                    settingNames.add(s.getName());
                for (AcqSetting setting:list) {
                    if (!objectiveDevStr.equals(setting.getObjectiveDevStr())) {
                        setting.setObjectiveDevStr(objectiveDevStr);
                    }
                    while (settingNames.contains(setting.getName())) {
                        setting.setName(setting.getName()+"_1");
                    }
                    settingNames.add(setting.getName());
                    acqSettings.add(setting);
                }
//                IJ.log(settingNames.toString());
                initializeAcqSettingTable();
            } else {
                JOptionPane.showMessageDialog(this,"Could not read acquisition settings from file '"+fc.getSelectedFile().getName()+"'.");
            }
        }
    }//GEN-LAST:event_loadAcqSettingButtonActionPerformed

    private void saveAcqSettingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAcqSettingButtonActionPerformed

        JPanel panel = new JPanel();
        BoxLayout layout = new BoxLayout(panel,BoxLayout.Y_AXIS);
        panel.setLayout(layout);

        JPanel filepanel = new JPanel();
        FlowLayout flayout = new FlowLayout();
        filepanel.setLayout(flayout);

        JLabel l=new JLabel("Save As:");
        filepanel.add(l);     
        final JFormattedTextField fnameField = new JFormattedTextField();
        fnameField.setColumns(30);
        filepanel.add(fnameField);
        JButton fileButton=new JButton("Browse");
        filepanel.add(fileButton);
        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                JFileChooser fc=new JFileChooser();
                int result=fc.showSaveDialog(AcqFrame.this);
                if (result == JFileChooser.APPROVE_OPTION)
                    fnameField.setValue(fc.getSelectedFile().getAbsolutePath());
            }
        });
        filepanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(filepanel);
            
        JCheckBox saveAllCB = new JCheckBox("Save all acquisition settings");
        saveAllCB.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(saveAllCB);
        
        File file;
        do {    
            int result = JOptionPane.showConfirmDialog(this, panel, 
                "Save Acquisiton Settings", JOptionPane.OK_CANCEL_OPTION);
            file=new File(fnameField.getText());
            if (result == JOptionPane.CANCEL_OPTION)
                return;
            if (file.exists()) {
                int overwrite=JOptionPane.showConfirmDialog(this,"File already exists.","Save Acquisiton Settings",JOptionPane.YES_NO_CANCEL_OPTION);
                if (overwrite == JOptionPane.CANCEL_OPTION)
                    return;
                if (overwrite == JOptionPane.OK_OPTION)
                    break;
            } else if (!file.isDirectory()) {
                File dir = file.getParentFile();
                if (dir.exists()){
                    break;
                } else {
                    JOptionPane.showMessageDialog(this,"Path "+dir.getAbsolutePath()+" does not exist.");
                }
            } else
                break;
        } while (true);
        if (saveAllCB.isSelected())
            saveAcquisitionSettings(file);
        else
            saveAcquisitionSetting(file,currentAcqSetting);
    }//GEN-LAST:event_saveAcqSettingButtonActionPerformed

    private void addDataProcFromFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addDataProcFromFileButtonActionPerformed
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                       processorTreeView.getLastSelectedPathComponent();
        DefaultTreeModel tm=(DefaultTreeModel)processorTreeView.getModel();
        if (selectedNode==null) {
            JOptionPane.showMessageDialog(null, "Select Node in Processor Tree");
            return;
        }
        
        if (!new File(dataProcessorPath).isDirectory())
            dataProcessorPath=Prefs.getImageJDir();
        JFileChooser fc=new JFileChooser(new File(dataProcessorPath));

        fc.showOpenDialog(fc);
        if (fc.getSelectedFile()!=null) {
            dataProcessorPath=fc.getSelectedFile().getParent();
            String fname=fc.getSelectedFile().getName();
            URLClassLoader classLoader;
            String selClass;
            if (fname.indexOf(".class")>0) {
                selClass=fname.substring(0, fname.lastIndexOf(".class"));
                try {
                    classLoader = new URLClassLoader(
                        new URL[]{new File(fc.getSelectedFile().getParent()).toURI().toURL()},
                        this.getClass().getClassLoader());
                        //DataProcessor.class.getClassLoader());
                } catch (MalformedURLException ex) {
                    JOptionPane.showMessageDialog(this,"Cannot form URL to load class.");
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                }  
            } else if (fname.lastIndexOf(".jar")>0) {
                String jarFileStr=fc.getSelectedFile().getAbsolutePath();
                try {
                    JarFile jarFile;
                    jarFile = new JarFile(jarFileStr);
                    Enumeration e = jarFile.entries();

                    URL[] urls = { new URL("jar:file:" + jarFileStr+"!/") };
                    classLoader = URLClassLoader.newInstance(urls,
                        //    this.getClass().getClassLoader());
                        DataProcessor.class.getClassLoader());

                    int i=0;
                    List<String> classes=new ArrayList<String>();
                    while (e.hasMoreElements()) {
                        JarEntry je = (JarEntry) e.nextElement();
                        if(je.isDirectory() || !je.getName().endsWith(".class")){
                            continue;
                        }
                         // -6 because of .class
                        String className = je.getName().substring(0,je.getName().length()-6);
                        className = className.replace('/', '.');
                        try {
                            Class<?> clazz=Class.forName(className);
                            //only add DataProcessor and not abstract classes to list
                            if (DataProcessor.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                                classes.add(className);
                            }
                        } catch (ClassNotFoundException ex) {
                            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }                        
                        //classes.add(className);
                    }
                    if (classes.isEmpty()) {
                        JOptionPane.showMessageDialog(this,"No classes found.");
                        return;
                    }    
                    JComboBox jcb = new JComboBox(classes.toArray());
                    JOptionPane.showMessageDialog( null, jcb, "Select DataProcessor class:", JOptionPane.QUESTION_MESSAGE);
                    selClass=(String)jcb.getSelectedItem();
                    
                } catch (MalformedURLException ex) {
                    JOptionPane.showMessageDialog(this,"Cannot from URL to load classes in jar file.");
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this,"Problem reading content of jar file.");
                        Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                    return;    
                }
            } else {
                JOptionPane.showMessageDialog(this,".class or .jar file required");
                return;
            }
            try {
                Object dp=null;
                Constructor[] ctors;
                if (Utils.isDescendantOfImageStorageNode(currentAcqSetting.getImageProcessorTree(), selectedNode)) {
                    Class<? extends DataProcessor<File>> processorClass = (Class<? extends DataProcessor<File>>)classLoader.loadClass(selClass);
//                    dp = processorClass.newInstance();
                    ctors = processorClass.getConstructors();
                } else {
                    Class<? extends DataProcessor<TaggedImage>> processorClass = (Class<? extends DataProcessor<TaggedImage>>)classLoader.loadClass(selClass);
                    ctors = processorClass.getConstructors();
                }
                for (Constructor ctor:ctors) {
                        ctor.setAccessible(true);
                        Class<?>[] c=ctor.getParameterTypes();
                        dp=ctor.newInstance(new Object[c.length]);
                        break;
                }
                if (dp instanceof DataProcessor) {
                    List<DataProcessor<TaggedImage>> loadedDPs=app.getImageProcessorPipeline();
                    for (DataProcessor<TaggedImage> ldp:loadedDPs) {
                        if (ldp.getClass().getName().equals(dp.getClass().getName())) {
                            JOptionPane.showMessageDialog(null,"DataProcessor already loaded.");
                            dp=ldp;
                            try {
                                Method guiMethod = dp.getClass().getDeclaredMethod("makeConfigurationGUI", new Class[]{});
                                guiMethod.invoke(dp, new Object[]{});
                            } catch (NoSuchMethodException ex) {
                                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                                JOptionPane.showMessageDialog(null,"No configuration dialog found for this processor.");
                            }
                            break;
                                    //dp.makeConfigurationGUI();
                                    //dp.dispose();
                        }
                    }
                    if (dp instanceof ExtDataProcessor) {
                        ExtDataProcessor edp=(ExtDataProcessor)dp;
                        if (Utils.isDescendantOfImageStorageNode((DefaultMutableTreeNode)tm.getRoot(), selectedNode) && !edp.isSupportedDataType(File.class)) {
                            JOptionPane.showMessageDialog(null,"This DataProcessor cannot be inserted downstream of Image Storage node.");  
                            return;
                        }
                        if (!Utils.isDescendantOfImageStorageNode((DefaultMutableTreeNode)tm.getRoot(), selectedNode) && !edp.isSupportedDataType(TaggedImage.class)) {
                            JOptionPane.showMessageDialog(null,"This DataProcessor cannot be inserted upstream of Image Storage node.");  
                            return;
                        }
                        ((ExtDataProcessor)dp).makeConfigurationGUI();
                        ((ExtDataProcessor)dp).dispose();
                    } else {
                        if (Utils.isDescendantOfImageStorageNode((DefaultMutableTreeNode)tm.getRoot(), selectedNode)) {
                            JOptionPane.showMessageDialog(null,"This DataProcessor cannot be inserted downstream of Image Storage node.");  
                            return;
                        }
                        try {
                            StringBuilder s=new StringBuilder();
                            Method[] methods=dp.getClass().getDeclaredMethods();
                            for (Method m:methods) {
                                s.append("   Method: "+m.getName()+"\n");
                            }
                            IJ.log("Class "+dp.getClass().getName()+": The following methods were found:\n"+s.toString());
                            Method guiMethod=dp.getClass().getDeclaredMethod("makeConfigurationGUI", new Class[]{});
                            guiMethod.invoke(dp, new Object[]{});
                            Method disposeMethod=dp.getClass().getDeclaredMethod("dispose", new Class[]{});
                            disposeMethod.invoke(dp, new Object[]{});
                            IJ.log("Processor loaded");
                        } catch (NoSuchMethodException ex) {
                            JOptionPane.showMessageDialog(null,"No configuration dialog and/or dispose method found for this processor.");
                            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } 
                    createAndAddProcessorNode(selectedNode,(DataProcessor)dp);
                } else {
                    JOptionPane.showMessageDialog(this,"Error: The selected file is not a DataProcessor");                        
                }
            } catch (ClassNotFoundException ex) {
                JOptionPane.showMessageDialog(this,"Error: The selected class cannot be found.");                        
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex+"2");
            } catch (InstantiationException ex) {
                JOptionPane.showMessageDialog(this,"Error: Class instantiation.");                        
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex+"3");
            } catch (IllegalAccessException ex) {
                JOptionPane.showMessageDialog(this,"Error: Illegal access.");                        
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex+"4");
            } catch (SecurityException ex) {
                JOptionPane.showMessageDialog(this,"Error: Security exception.");                        
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this,"Error: Illegal argument.");                        
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                JOptionPane.showMessageDialog(this,"Error: InvocationTargetException.");                        
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (NoSuchMethodException ex) {
//                JOptionPane.showMessageDialog(this,"Error: NoSuchMethodException.");                        
//                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }    
    }//GEN-LAST:event_addDataProcFromFileButtonActionPerformed

    private void addImageStorageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addImageStorageButtonActionPerformed
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                       processorTreeView.getLastSelectedPathComponent();
        DefaultTreeModel tm=(DefaultTreeModel)processorTreeView.getModel();
        if (selectedNode==null) {
            JOptionPane.showMessageDialog(null, "Select Node in Processor Tree");
            return;
        }   
        if (Utils.imageStoragePresent(currentAcqSetting.getImageProcessorTree())) {
            JOptionPane.showMessageDialog(null, "Image storage node already present.");
            return;            
        }
        createAndAddProcessorNode(selectedNode,new ExtDataProcessor(ProcessorTree.PROC_NAME_IMAGE_STORAGE,""));
    }//GEN-LAST:event_addImageStorageButtonActionPerformed

    private void addFrameFilterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addFrameFilterButtonActionPerformed
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                       processorTreeView.getLastSelectedPathComponent();
        if (selectedNode==null) {
            JOptionPane.showMessageDialog(null, "Select Node in Processor Tree");
            return;
        }
        ImageTagFilterOpt itf=new ImageTagFilterOptLong(MMTags.Image.FRAME_INDEX,null);
        List<String> frames=new ArrayList<String>();
        for (long l=0;l<currentAcqSetting.getFrames();l++)
            frames.add(Long.toString(l));
        itf.setOptions(frames);
        itf.makeConfigurationGUI();
        itf.dispose();
        if (!"".equals(itf.getKey())) {
            createAndAddProcessorNode(selectedNode,itf);    
        }    
    }//GEN-LAST:event_addFrameFilterButtonActionPerformed
/*
    private static <E> E createInstance(Class<E> clazz) throws InstantiationException, IllegalAccessException {
        return clazz.newInstance();
    }
*/    
    private DefaultMutableTreeNode createProcessorTree(JSONObject procObj) throws JSONException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        JSONObject proc=procObj.getJSONObject("Processor");
        String className=proc.getString("ClassName");
        Class<?> clazz=Class.forName(className,true,this.getClass().getClassLoader());//DataProcessor.class.getClassLoader());
        DataProcessor dp=(DataProcessor)clazz.newInstance();
/*        DataProcessor dp;
        try {
            dp=createInstance(clazz<TaggedImage>);
        } catch (Exception ex) {
            dp=null;
        }*/
        if (dp instanceof ExtDataProcessor) {
            ((ExtDataProcessor)dp).setParameters(proc);
        } else {
        }
        DefaultMutableTreeNode node=new DefaultMutableTreeNode(dp);
        try {
            JSONArray children=proc.getJSONArray("ChildProcessors");
            if (children!=null && children.length()>0) {
                for (int i=0; i<children.length(); i++) {
                    JSONObject c=children.getJSONObject(i);
                    DefaultMutableTreeNode cNode=createProcessorTree(c);
                    node.add(cNode);
                }
            }
        } catch (JSONException ex) {
            IJ.log(this.getClass().getName()+": "+className+": no children");
        }
        return node;
    }
    
    private DefaultMutableTreeNode loadProcTree(File file, String settingName) {
        DefaultMutableTreeNode root=null;   
        BufferedReader br=null;           
        try {
            br = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            JSONObject procTrees = new JSONObject(sb.toString());
            JSONArray procArray= procTrees.getJSONArray("Processor_Definition");
            if (procArray!=null && procArray.length()>0) {
                JSONObject procTreeObj=null;
                String[] list=new String[procArray.length()];
                boolean found=false;
                for (int i=0; i<procArray.length(); i++) {
                    procTreeObj=procArray.getJSONObject(i);
                    if (settingName.equals(procTreeObj.getString("SeqSetting"))) {
                        found=true;
                        break;
                    }    
                }
                if (found) {
                    procTreeObj=procTreeObj.getJSONObject("ProcTree");
                    try {
                        root = createProcessorTree(procTreeObj);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this,"Cannot load processor tree from file '"+file.getName()+"'.");
                        Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                    } 
                }  else {
                    JOptionPane.showMessageDialog(this,"Error: Processor tree not found for acquisition setting "+settingName);
                } 
            }
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(this,"Cannot find processor file '"+file.getName()+"'.");
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,"Error loading processor file.");
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            JOptionPane.showMessageDialog(this,"Error parsing processor file.");
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (br!=null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    IJ.log(this.getClass().getName()+":  IOException Processor file");
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return root;
        }    
    }
    
    private void loadProcTreeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadProcTreeButtonActionPerformed
        JFileChooser fc=new JFileChooser();
        fc.showOpenDialog(this);
        if (fc.getSelectedFile()!=null) {
            BufferedReader br=null;           
            try {
                br = new BufferedReader(new FileReader(fc.getSelectedFile()));
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    line = br.readLine();
                }
                JSONObject procTrees = new JSONObject(sb.toString());
                JSONArray procArray= procTrees.getJSONArray("Processor_Definition");
                if (procArray!=null && procArray.length()>0) {
                    JSONObject procTreeObj=null;
                    if (procArray.length()>1) {
                        String[] list=new String[procArray.length()];
                        for (int i=0; i<procArray.length(); i++) {
                            procTreeObj=procArray.getJSONObject(i);
                            list[i]=procTreeObj.getString("SeqSetting");
                        }
                        String selProcTree=(String)JOptionPane.showInputDialog(null, 
                            "Select Processor Tree",
                            "Load Processor Tree",
                            JOptionPane.QUESTION_MESSAGE, 
                            null, 
                            list, 
                            list[0]);
                        if (selProcTree==null)
                            return;
                        for (int i=0; i<procArray.length(); i++) {
                            procTreeObj=procArray.getJSONObject(i);
                            if (procTreeObj.getString("SeqSetting").equals(selProcTree))
                                break;
                        }
                    } else {
                        procTreeObj=procArray.getJSONObject(0);
                    }
                    procTreeObj=procTreeObj.getJSONObject("ProcTree");
                    DefaultTreeModel model=(DefaultTreeModel)processorTreeView.getModel();
                    DefaultMutableTreeNode root=null;
                    try {
                        root = createProcessorTree(procTreeObj);
                    } catch (ClassNotFoundException ex) {
                        IJ.log("ClassNotFoundException");
                        Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InstantiationException ex) {
                        IJ.log("InstantiationException");
                        Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalAccessException ex) {
                        IJ.log("IllegalAccessException");
                        Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (root!=null) {
                        currentAcqSetting.setImageProcTree(root);
                        model.setRoot(root);
                        model.reload();
                        for (int i = 0; i < processorTreeView.getRowCount(); i++) {
                            processorTreeView.expandRow(i);
                        }
                    }
                }
            } catch (FileNotFoundException ex) {
                JOptionPane.showMessageDialog(this,"Cannot find Processor file.");
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,"Error loading Processor file.");
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JSONException ex) {
                JOptionPane.showMessageDialog(this,"Error parsing Processor file.");
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (br!=null)
                    try {
                    br.close();
                } catch (IOException ex) {
                    IJ.log(this.getClass().getName()+":  IOException Processor file");
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }//GEN-LAST:event_loadProcTreeButtonActionPerformed

    private void saveProcTreeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveProcTreeButtonActionPerformed
        JPanel panel = new JPanel();
        BoxLayout layout = new BoxLayout(panel,BoxLayout.Y_AXIS);
        panel.setLayout(layout);

        JPanel filepanel = new JPanel();
        FlowLayout flayout = new FlowLayout();
        filepanel.setLayout(flayout);

        JLabel l=new JLabel("Save As:");
        filepanel.add(l);     
        final JFormattedTextField fnameField = new JFormattedTextField();
        fnameField.setColumns(30);
        filepanel.add(fnameField);
        JButton fileButton=new JButton("Browse");
        filepanel.add(fileButton);
        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                JFileChooser fc=new JFileChooser();
                int result=fc.showSaveDialog(AcqFrame.this);
                if (result == JFileChooser.APPROVE_OPTION)
                    fnameField.setValue(fc.getSelectedFile().getAbsolutePath());
            }
        });
        filepanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(filepanel);
            
        JCheckBox saveAllCB = new JCheckBox("Save processor tree for all acquisition settings");
        saveAllCB.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(saveAllCB);
        
        File file;
        do {    
            int result = JOptionPane.showConfirmDialog(this, panel, 
                "Save Processor Tree", JOptionPane.OK_CANCEL_OPTION);
            file=new File(fnameField.getText());
            if (result == JOptionPane.CANCEL_OPTION)
                return;
            if (file.exists()) {
                int overwrite=JOptionPane.showConfirmDialog(this,"File already exists.","Save Processor Tree",JOptionPane.YES_NO_CANCEL_OPTION);
                if (overwrite == JOptionPane.CANCEL_OPTION)
                    return;
                if (overwrite == JOptionPane.OK_OPTION)
                    break;
            } else if (!file.isDirectory()) {
                File dir = file.getParentFile();
                if (dir.exists()){
                    break;
                } else {
                    JOptionPane.showMessageDialog(this,"Path "+dir.getAbsolutePath()+" does not exist.");
                }
            } else
                break;
        } while (true);
        if (saveAllCB.isSelected()) {
            //saveProcessorTree(file,acqSettings);
            //not supported
        } else {
            saveProcessorTree(file,currentAcqSetting);
         //   saveProcessorTreeForXML(new File(file.getParentFile(),"ProcForXML.txt"),currentAcqSetting);
        }    
    }//GEN-LAST:event_saveProcTreeButtonActionPerformed

    private void showZProfileCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showZProfileCheckBoxActionPerformed
        //        ((LayoutPanel)acqLayoutPanel).setShowZProfile(showZProfileCheckBox.isSelected());
        ((LayoutPanel)acqLayoutPanel).enableShowZProfile(showZProfileCheckBox.isSelected());
        acqLayoutPanel.repaint();
    }//GEN-LAST:event_showZProfileCheckBoxActionPerformed

    private void commentButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_commentButtonActionPerformed
        commentMode = !commentMode;
        if (commentMode) {
            acqLayoutPanel.setCursor(normCursor);
            moveToMode = false;
            moveToScreenCoordButton.setSelected(false);
            zoomMode = false;
            zoomButton.setSelected(false);
            selectMode = false;
            selectButton.setSelected(false);
            mergeAreasMode = false;
            mergeAreasButton.setSelected(false);
        }
    }//GEN-LAST:event_commentButtonActionPerformed

    private void selectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectButtonActionPerformed
        selectMode = !selectMode;
        if (selectMode) {
            acqLayoutPanel.setCursor(normCursor);
            moveToMode = false;
            moveToScreenCoordButton.setSelected(false);
            zoomMode = false;
            zoomButton.setSelected(false);
            commentMode = false;
            commentButton.setSelected(false);
            mergeAreasMode = false;
            mergeAreasButton.setSelected(false);
        }
    }//GEN-LAST:event_selectButtonActionPerformed

    private void zoomButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomButtonActionPerformed
        zoomMode = !zoomMode;
        if (zoomMode) {
            acqLayoutPanel.setCursor(zoomCursor);
            moveToMode = false;
            moveToScreenCoordButton.setSelected(false);
            selectMode = false;
            selectButton.setSelected(false);
            commentMode = false;
            commentButton.setSelected(false);
            mergeAreasMode = false;
            mergeAreasButton.setSelected(false);
        } else {
            acqLayoutPanel.setCursor(normCursor);
        }
    }//GEN-LAST:event_zoomButtonActionPerformed

    private void moveToScreenCoordButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveToScreenCoordButtonActionPerformed
        if (acqLayout.getNoOfMappedStagePos() == 0) {
            JOptionPane.showMessageDialog(this,"At least one layout landmark needs to be set first.");
            moveToScreenCoordButton.setSelected(false);
            return;
        }
        moveToMode = !moveToMode;
        //        ((LayoutPanel)acqLayoutPanel).setMoveMode(moveToMode);
        if (moveToMode) {
            acqLayoutPanel.setCursor(moveToCursor);
            zoomMode = false;
            zoomButton.setSelected(false);
            selectMode = false;
            selectButton.setSelected(false);
            commentMode = false;
            commentButton.setSelected(false);
            mergeAreasMode = false;
            mergeAreasButton.setSelected(false);
        } else {
            acqLayoutPanel.setCursor(normCursor);
        }
    }//GEN-LAST:event_moveToScreenCoordButtonActionPerformed

    /*    public static ArrayList<RefArea> cloneRefPointList(ArrayList<RefArea> rpList) {
     ArrayList<RefArea> clonedList = new ArrayList<RefArea>(rpList.size());
     for (RefArea rp : rpList) {
     clonedList.add(new RefArea(rp));
     }
     return clonedList;
     }*/
    private void setLandmarkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setLandmarkButtonActionPerformed
        if (acqLayout.isEmpty()) {
            JOptionPane.showMessageDialog(this, "A layout has to be loaded first.");
            return;
        }
        try {
            String XYStageName = core.getXYStageDevice();
            double sX = core.getXPosition(XYStageName);
            double sY = core.getYPosition(XYStageName);
            String zStr = core.getFocusDevice();
            double sZ = core.getPosition(zStr);                                                //needs to read actual Z-position
            RefArea oldLm = acqLayout.getLandmark(0);
            //            ArrayList<RefArea> oldRPList=cloneRefPointList(acqLayout.getLandmarks());
            if (refPointListDialog == null) {
                refPointListDialog = new RefPointListDialog(this, app, acqLayout);
                if (stageMonitor!=null) {
                    stageMonitor.addListener(refPointListDialog);
                }
                refPointListDialog.addWindowListener(this);
                refPointListDialog.addListener(this);
            } else {
                refPointListDialog.setRefPointList(acqLayout.getLandmarks());
            }
            refPointListDialog.setVisible(true);

            /*            GenericDialog ld=new GenericDialog("Set Landmark");
            ld.addStringField("Landmark:", "Landmark 1", 25);
            ld.addStringField("Filename:", "Landmark.TIF",25);
            ld.addNumericField("X-Offset to Layout Origin: ", oldLm.getLayoutCoordX(), 0);
            ld.addNumericField("Y-Offset to Layout Origin: ", oldLm.getLayoutCoordY(), 0);
            ld.addNumericField("Z-Offset to Layout Focal Plane: ", oldLm.getLayoutCoordZ(), 0);
            ld.addMessage("Current Stage Position\n X: "+Double.toString(sX)+"\n Y: "+Double.toString(sY)+"\n Z: "+Double.toString(sZ));
            ld.showDialog();
            if (!ld.wasCanceled()){
                acqLayout.setLandmark(0,(new RefArea(ld.getNextString(), sX-currentFOVWidth/2,sY-currentFOVHeight/2,sZ,ld.getNextNumber(),ld.getNextNumber(),ld.getNextNumber(),currentFOVWidth,currentFOVHeight, ld.getNextString())));
                //                initializeAreaMoveToListBox();
                //                areaMoveToComboBox.setSelectedItem(0);
                updateAcqLayoutPanel();
                setLandmarkFound(true);
            }*/
        } catch (Exception ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_setLandmarkButtonActionPerformed

    private void loadImagePipelineButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadImagePipelineButtonActionPerformed
        try {
            if (app.versionLessThan("1.4.18")) {
                JOptionPane.showMessageDialog(null, "Use of Image Pipeline requires Micro-Manager vesion 1.4.18 or higher.");
                loadImagePipelineButton.setEnabled(false);
                return;
            }
        } catch (MMScriptException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                       processorTreeView.getLastSelectedPathComponent();
        DefaultTreeModel tm=(DefaultTreeModel)processorTreeView.getModel();
        if (selectedNode==null) {
            JOptionPane.showMessageDialog(null, "Select Node in Processor Tree");
            return;
        }
        List<DataProcessor<TaggedImage>> ipList=app.getImageProcessorPipeline();
        if (ipList==null || ipList.isEmpty()) {
            JOptionPane.showMessageDialog(this,"ImageProcessor Pipeline is empty.");
            return;
        }
        List<DataProcessor<TaggedImage>> activeList = new ArrayList<DataProcessor<TaggedImage>>();
        for (DataProcessor<TaggedImage>dp:ipList) {
            if (dp.getIsEnabled()) {
                activeList.add(dp);
            }
        }
        if (activeList.isEmpty()) {
            JOptionPane.showMessageDialog(this,"There are no active DataProcessors in the ImageProcessor Pipeline.");
            return;            
        }
        if (Utils.isDescendantOfImageStorageNode(currentAcqSetting.getImageProcessorTree(), selectedNode)) {
            JOptionPane.showMessageDialog(null, "Image Pipeline DataProcessors need to be inserted upstream of Image Storage node");
            return;
        }
        for (int i=activeList.size()-1; i>=0; i--) {
            DataProcessor dp=activeList.get(i);
            createAndAddProcessorNode(selectedNode,dp);
        }
        for (int i=0; i<processorTreeView.getRowCount(); i++)
            processorTreeView.expandRow(i);
    }//GEN-LAST:event_loadImagePipelineButtonActionPerformed

    private void siteOverlapCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_siteOverlapCheckBoxActionPerformed
        //        IJ.log("overlap ActionPerformed");
        JComponent c = (JComponent) evt.getSource();
        if (c.getVerifyInputWhenFocusTarget()) {
            //            c.requestFocusInWindow();
            if (!c.hasFocus()) {
                siteOverlapCheckBox.setSelected(!siteOverlapCheckBox.isSelected());
                return;
            }
        }
        if (!calculating && recalculateTiles) {
            prevObjLabel = currentAcqSetting.getObjective();
            prevTilingSetting = currentAcqSetting.getTilingSetting().duplicate();
            currentAcqSetting.enableSiteOverlap(siteOverlapCheckBox.isSelected());
            calcTilePositions(null,currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), ADJUSTING_SETTINGS);
        }
    }//GEN-LAST:event_siteOverlapCheckBoxActionPerformed

    private void clusterXFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clusterXFieldActionPerformed
        JComponent source = (JComponent) evt.getSource();
        if (source.getInputVerifier().shouldYieldFocus(source)) {
            setClusterXField();
        }
    }//GEN-LAST:event_clusterXFieldActionPerformed

    private void clusterXFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_clusterXFieldFocusLost
        //        IJ.log("XField Focus Lost");
        JComponent source = (JComponent) evt.getSource();
        if (source.getInputVerifier().verify(source)) {
            setClusterXField();
        }
    }//GEN-LAST:event_clusterXFieldFocusLost

    private void tilingModeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tilingModeComboBoxActionPerformed
        TilingSetting.Mode newMode = (TilingSetting.Mode) tilingModeComboBox.getSelectedItem();
        clusterCheckBox.setEnabled(newMode != TilingSetting.Mode.FULL);
        clusterXField.setEnabled(newMode != TilingSetting.Mode.FULL);
        clusterYField.setEnabled(newMode != TilingSetting.Mode.FULL);
        clusterLabel1.setEnabled(newMode != TilingSetting.Mode.FULL);
        clusterLabel2.setEnabled(newMode != TilingSetting.Mode.FULL);
        siteOverlapCheckBox.setEnabled(newMode == TilingSetting.Mode.RANDOM);
        maxSitesField.setEnabled(newMode != TilingSetting.Mode.FULL && newMode != TilingSetting.Mode.CENTER);
        maxSitesLabel.setEnabled(newMode != TilingSetting.Mode.FULL && newMode != TilingSetting.Mode.CENTER);
        if (!calculating && recalculateTiles) {
            if (newMode != currentAcqSetting.getTilingMode()) {
                prevObjLabel = currentAcqSetting.getObjective();
                prevTilingSetting = currentAcqSetting.getTilingSetting().duplicate();
                currentAcqSetting.setTilingMode(newMode);
                calcTilePositions(null, currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), ADJUSTING_SETTINGS);
            }
        }
    }//GEN-LAST:event_tilingModeComboBoxActionPerformed

    private void autofocusButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autofocusButtonActionPerformed
        String currentAF=currentAcqSetting.getAutofocusDevice();
        if (currentAF != null) {
            try {
                app.getAutofocusManager().selectDevice(currentAF);
                Autofocus af=app.getAutofocus();
                currentAcqSetting.applyAutofocusSettingsToDevice(af);
                app.getAutofocusManager().refresh();
            } catch (MMException ex) {
                JOptionPane.showMessageDialog(this,"Error selecting Autofocus device: " +ex.getMessage());
                ReportingUtils.logError(ex, this.getClass().getName()+": Autofocus configuration");
            }
        }    
        app.getAutofocusManager().showOptionsDialog();
    }//GEN-LAST:event_autofocusButtonActionPerformed

    private void acqOrderListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_acqOrderListActionPerformed
        //        if (acqSettings!=null && acqSettings.size()>0) {
            if (currentAcqSetting != null && acqSettings.size() > 0) {
                int acqOrderIdx = acqOrderList.getSelectedIndex();
                currentAcqSetting.setAcqOrder(acqOrderIdx);
                //            IJ.log("acqOrderListActionPerformed. "+Integer.toString(acqOrderIdx)+":, "+(String)acqOrderList.getSelectedItem());
            }
    }//GEN-LAST:event_acqOrderListActionPerformed

    private void insideOnlyCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_insideOnlyCheckBoxActionPerformed
        JComponent c = (JComponent) evt.getSource();
        if (c.getVerifyInputWhenFocusTarget()) {
            //            c.requestFocusInWindow();
            if (!c.hasFocus()) {
                insideOnlyCheckBox.setSelected(!insideOnlyCheckBox.isSelected());
                return;
            }
        }
        setTilingInsideAreaOnly(insideOnlyCheckBox.isSelected());
    }//GEN-LAST:event_insideOnlyCheckBoxActionPerformed

    private void clusterCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clusterCheckBoxActionPerformed
        JComponent c = (JComponent) evt.getSource();
        if (c.getVerifyInputWhenFocusTarget()) {
            //            c.requestFocusInWindow();
            if (!c.hasFocus()) {
                clusterCheckBox.setSelected(!clusterCheckBox.isSelected());
                return;
            }
        }
        clusterXField.setEnabled(clusterCheckBox.isSelected());
        clusterYField.setEnabled(clusterCheckBox.isSelected());
        clusterLabel1.setEnabled(clusterCheckBox.isSelected());
        clusterLabel2.setEnabled(clusterCheckBox.isSelected());
        //        clusterOverlapCheckBox.setEnabled(clusterCheckBox.isSelected() && currentAcqSetting.getTilingMode() != TilingSetting.Mode.CENTER);
        if (!calculating && recalculateTiles) {
            //        if (recalculateTiles && acqSettings!=null && acqSettings.size()>0) {
                //            AcqSetting setting = acqSettings.get(cAcqSettingIdx);
                //            setting.enableCluster(clusterCheckBox.isSelected());
                prevObjLabel = currentAcqSetting.getObjective();
                prevTilingSetting = currentAcqSetting.getTilingSetting().duplicate();
                currentAcqSetting.enableCluster(clusterCheckBox.isSelected());
                calcTilePositions(null,currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), ADJUSTING_SETTINGS);
            }
    }//GEN-LAST:event_clusterCheckBoxActionPerformed

    private void binningComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_binningComboBoxActionPerformed
        //        if (acqSettings!=null && acqSettings.size()>0) {
            if (currentAcqSetting != null) {
                //            AcqSetting setting = acqSettings.get(cAcqSettingIdx);
                currentAcqSetting.setBinning(Integer.parseInt((String) binningComboBox.getSelectedItem()));
                updatePixelSize(currentAcqSetting.getObjective());
            }
    }//GEN-LAST:event_binningComboBoxActionPerformed

    private void autofocusCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_autofocusCheckBoxItemStateChanged
        /*        if (acqSettings!=null && acqSettings.size()>0) {
            AcqSetting setting = acqSettings.get(cAcqSettingIdx);*/
            if (currentAcqSetting != null) {
                currentAcqSetting.enableAutofocus(autofocusCheckBox.isSelected());
            }
            if (autofocusCheckBox.isSelected()) {
                zStackCenteredCheckBox.setText("Centered around autofocus Z-position");
            } else {
                zStackCenteredCheckBox.setText("Centered around reference Z-position");
            }
    }//GEN-LAST:event_autofocusCheckBoxItemStateChanged

    private void tileOverlapFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tileOverlapFieldActionPerformed
        JComponent source = (JComponent) evt.getSource();
        if (source.getInputVerifier().shouldYieldFocus(source)) {
            updateTileOverlap();
        }
    }//GEN-LAST:event_tileOverlapFieldActionPerformed

    private void tileOverlapFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_tileOverlapFieldFocusLost
        JComponent source = (JComponent) evt.getSource();
        if (source.getInputVerifier().verify(source)) {
            updateTileOverlap();
        }
    }//GEN-LAST:event_tileOverlapFieldFocusLost

    private void maxSitesFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_maxSitesFieldActionPerformed
        JComponent source = (JComponent) evt.getSource();
        if (source.getInputVerifier().shouldYieldFocus(source)) {
            setMaxSites();
        }
    }//GEN-LAST:event_maxSitesFieldActionPerformed

    private void maxSitesFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_maxSitesFieldFocusLost
        JComponent source = (JComponent) evt.getSource();
        if (source.getInputVerifier().verify(source)) {
            setMaxSites();
        }
    }//GEN-LAST:event_maxSitesFieldFocusLost

    private void zStackCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zStackCheckBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_zStackCheckBoxActionPerformed

    private void zStackCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_zStackCheckBoxItemStateChanged
        if (currentAcqSetting != null) {
            /*        if (acqSettings!=null && acqSettings.size()>0) {
                AcqSetting setting = acqSettings.get(cAcqSettingIdx);*/
                currentAcqSetting.enableZStack(zStackCheckBox.isSelected());
            }
            enableZStackPane(zStackCheckBox.isSelected());
    }//GEN-LAST:event_zStackCheckBoxItemStateChanged

    private void objectiveComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_objectiveComboBoxActionPerformed
        if (evt.getSource() == objectiveComboBox) {
            String selectedObjective = (String) objectiveComboBox.getSelectedItem();
            //            if (acqSettings!=null && acqSettings.size()>cAcqSettingIdx) {
                if (currentAcqSetting != null) {
                    //                AcqSetting setting=acqSettings.get(cAcqSettingIdx);
                    if (!currentAcqSetting.getObjective().equals(selectedObjective)) {
                        if (!calculating) {
                            prevObjLabel = currentAcqSetting.getObjective();
                            prevTilingSetting = currentAcqSetting.getTilingSetting().duplicate();
                            currentAcqSetting.setObjective(selectedObjective, getObjPixelSize(selectedObjective));
                            //                        IJ.log("objectiveComboBoxActionPerformed. "+selectedObjective);
                            //                    try {
                                updatePixelSize(selectedObjective); // calls updateFOVDimension, updateTileOverlap, updateTileSizeLabel
                                //                updateFOVDimension();
                                //                updateTileSizeLabel();
                                //                updateTileOverlap();
                                if (currentAcqSetting.getImagePixelSize() > 0 && recalculateTiles) {
                                    calcTilePositions(null, currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), ADJUSTING_SETTINGS);
                                } else if (currentAcqSetting.getImagePixelSize() <= 0) {
                                    for (Area a:acqLayout.getAreaArray()) {
                                        a.setUnknownTileNum(true);
                                    }
                                    currentAcqSetting.setTotalTiles(-1);
                                    ((AcqSettingTableModel)acqSettingTable.getModel()).updateTileCell(acqSettingTable.getSelectedRow());
                                    acqLayoutPanel.repaint();
                                }
                                //                Rectangle r=layoutScrollPane.getVisibleRect();
                                //                ((LayoutPanel)acqLayoutPanel).calculateScale(r.width,r.height);
                                //                    } catch (Exception ex) {
                                //                        Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                                //                    }
                        }
                    }
                }
            }
    }//GEN-LAST:event_objectiveComboBoxActionPerformed

    private void tilingDirComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tilingDirComboBoxActionPerformed
        if (!calculating && recalculateTiles) {
            currentAcqSetting.setTilingDir((Byte) tilingDirComboBox.getSelectedItem());
        }
    }//GEN-LAST:event_tilingDirComboBoxActionPerformed

    private void clusterYFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clusterYFieldActionPerformed
        JComponent source = (JComponent) evt.getSource();
        if (source.getInputVerifier().shouldYieldFocus(source)) {
            setClusterYField();
        }
    }//GEN-LAST:event_clusterYFieldActionPerformed

    private void clusterYFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_clusterYFieldFocusLost
        //        IJ.log("YField Focus Lost");
        JComponent source = (JComponent) evt.getSource();
        if (source.getInputVerifier().verify(source)) {
            setClusterYField();
        }
    }//GEN-LAST:event_clusterYFieldFocusLost

    private void timelapseCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_timelapseCheckBoxItemStateChanged
        //        if (acqSettings!=null && acqSettings.size()>0) {
            //            AcqSetting setting = acqSettings.get(cAcqSettingIdx);
            if (currentAcqSetting != null) {
                currentAcqSetting.enableTimelapse(timelapseCheckBox.isSelected());
            }
            if (timelapseCheckBox.isSelected()) {
                //           if (acqSettings!=null && acqSettings.size() > cAcqSettingIdx)
                if (currentAcqSetting != null) {
                    calculateDuration(currentAcqSetting);
                } else {
                    calculateDuration(null);
                }
            } else {
                durationText.setText("No Time-lapse acquisition");
            }
            enableTimelapsePane(timelapseCheckBox.isSelected());
    }//GEN-LAST:event_timelapseCheckBoxItemStateChanged

    private void framesFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_framesFieldFocusLost
        setFrames();
    }//GEN-LAST:event_framesFieldFocusLost

    private void framesFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_framesFieldActionPerformed
        setFrames();
    }//GEN-LAST:event_framesFieldActionPerformed

    private void intSecFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_intSecFieldFocusLost
        setSecInterval();
    }//GEN-LAST:event_intSecFieldFocusLost

    private void intSecFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_intSecFieldActionPerformed
        setSecInterval();
    }//GEN-LAST:event_intSecFieldActionPerformed

    private void intMinFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_intMinFieldFocusLost
        setMinInterval();
    }//GEN-LAST:event_intMinFieldFocusLost

    private void intMinFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_intMinFieldActionPerformed
        setMinInterval();
    }//GEN-LAST:event_intMinFieldActionPerformed

    private void intHourFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_intHourFieldFocusLost
        setHourInterval();
    }//GEN-LAST:event_intHourFieldFocusLost

    private void intHourFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_intHourFieldActionPerformed
        setHourInterval();
    }//GEN-LAST:event_intHourFieldActionPerformed

    private void reverseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reverseButtonActionPerformed
        /*        if (acqSettings!=null && acqSettings.size()>0) {
            AcqSetting setting=acqSettings.get(cAcqSettingIdx);*/
            if (currentAcqSetting != null) {
                double tmp = currentAcqSetting.getZBegin();
                currentAcqSetting.setZBegin(currentAcqSetting.getZEnd());
                currentAcqSetting.setZEnd(tmp);
                zStackBeginField.setValue(currentAcqSetting.getZBegin());
                zStackEndField.setValue(currentAcqSetting.getZEnd());
            }
    }//GEN-LAST:event_reverseButtonActionPerformed

    private void zStackCenteredCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zStackCenteredCheckBoxActionPerformed
        IJ.log(this.getClass().getName()+":.zStackCenteredCheckBoxActionPerformed");
        zStackBeginField.setEnabled(!zStackCenteredCheckBox.isSelected());
        zStackEndField.setEnabled(!zStackCenteredCheckBox.isSelected());
        if (currentAcqSetting != null) {
            if (zStackCenteredCheckBox.isSelected()) {
                double dist=currentAcqSetting.getZStepSize()*(currentAcqSetting.getZSlices()-1);
                if (currentAcqSetting.getZBegin()< currentAcqSetting.getZEnd()) {
                    currentAcqSetting.setZBegin(-dist/2);
                    currentAcqSetting.setZEnd(dist/2);
                    zStackBeginField.setValue(-dist/2);
                    zStackEndField.setValue(dist/2);
                } else {
                    currentAcqSetting.setZBegin(+dist/2);
                    currentAcqSetting.setZEnd(-dist/2);
                    zStackBeginField.setValue(dist/2);
                    zStackEndField.setValue(-dist/2);
                }    
            }
            currentAcqSetting.enableZStackCentered(zStackCenteredCheckBox.isSelected());
        }
    }//GEN-LAST:event_zStackCenteredCheckBoxActionPerformed

    private void zStackCenteredCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_zStackCenteredCheckBoxItemStateChanged

    }//GEN-LAST:event_zStackCenteredCheckBoxItemStateChanged

    private void liveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_liveButtonActionPerformed
        if (liveButton.getText().equals("Live")) {
            if (app.isLiveModeOn()) {
                JOptionPane.showMessageDialog(this, "Live Mode is already running.", "Live Mode", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (channelTable.getSelectedRowCount() != 1) {
                JOptionPane.showMessageDialog(this, "Select one channel");
                return;
            }    
            int[] rows = channelTable.getSelectedRows();
            ChannelTableModel ctm = (ChannelTableModel) channelTable.getModel();
            Channel c = ctm.getRowData(rows[0]);
            if (setObjectiveAndBinning(currentAcqSetting, true)) {
                try {
                    core.setExposure(c.getExposure());
                    core.setConfig(currentAcqSetting.getChannelGroupStr(), c.getName());
                    app.refreshGUI();
                    //gui.setConfigChanged(true);
                } catch (Exception e) {
                    ReportingUtils.logError(e.getMessage());
                }
                app.enableLiveMode(true);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        app.getSnapLiveWin().toFront();
                        app.getSnapLiveWin().repaint();
                    }
                });               

            }
        } else {
            app.enableLiveMode(false);
        }      
    }//GEN-LAST:event_liveButtonActionPerformed

    private void snapButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_snapButtonActionPerformed
        AbstractCellEditor ae = (AbstractCellEditor) channelTable.getCellEditor();
        if (ae != null) {
            ae.stopCellEditing();
        }
        /*        AcqSetting setting=null;
        if (acqSettings!=null && acqSettings.size()>cAcqSettingIdx) {
            setting=acqSettings.get(cAcqSettingIdx);
        }*/
        if (channelTable.getSelectedRowCount() < 1) {
            JOptionPane.showMessageDialog(this, "Select at least one channel");
        } else {
            try {
                if (!currentAcqSetting.getObjective().equals(core.getProperty(objectiveDevStr, "Label"))) {
                    JOptionPane.showMessageDialog(this, "Switching to "+currentAcqSetting.getObjective()+" objective.");
                }
            } catch (Exception ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (setObjectiveAndBinning(currentAcqSetting, true)) {
                String chGroupStr=MMCoreUtils.loadAvailableChannelConfigs(this,currentAcqSetting.getChannelGroupStr(),core);
                if (!chGroupStr.equals(currentAcqSetting.getChannelGroupStr())) {
                    currentAcqSetting.setChannelGroupStr(chGroupStr);
                    initializeChannelTable(currentAcqSetting);
                } else {
                    if (chGroupStr!=null) {
                        int[] rows = channelTable.getSelectedRows();
//                        ImageStack stack = new ImageStack(cCameraPixX / currentAcqSetting.getBinning(), cCameraPixY / currentAcqSetting.getBinning());
                        
                        //read out currentROI setting and update ROI in detector of current AcqSetting
                        FieldOfView fov=currentAcqSetting.getFieldOfView();
                        Rectangle snapROI=fov.getROI_Pixel(currentAcqSetting.getBinning());
                        Rectangle coreROI=null;
                        boolean updatedROI=false;
                        
                        try {
                            coreROI=core.getROI();
                            if (coreROI !=null 
                                    && (coreROI.width != fov.getROI_Pixel(currentAcqSetting.getBinning()).width 
                                    || coreROI.height != fov.getROI_Pixel(currentAcqSetting.getBinning()).height)) {
                                JOptionPane.showMessageDialog(this, "ROIs are not supported. Clearing ROI.");
//                                fov.clearROI();
                                core.clearROI();
                                for (AcqSetting setting:acqSettings) {
//                                    FieldOfView fov=new FieldOfView(currentDetector.getFullWidth_Pixel(), currentDetector.getFullHeight_Pixel(),currentDetector.getFieldRotation());
//                                    setting.setFieldOfView(fov);
                                    setting.getFieldOfView().clearROI();
                                }
                            }    
                        } catch (Exception ex) {
                            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        /*
                        // for future: handle ROIs
                        try {
                            coreROI=core.getROI();
                            IJ.showMessage("coreROI: "+coreROI.toString()+ ", snapROI: "+(snapROI==null ? "null" : snapROI.toString()));
                            if (coreROI!=null && !coreROI.equals(snapROI) && JOptionPane.showConfirmDialog(this,"The current camera ROI set in Micro-Manager is different then the ROI \n"
                                        + "defined for this acquisition setting.\n"
                                        + "Do you want to use the Micro-Manager camera ROI for this setting?","",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
                                snapROI=coreROI;
                                updatedROI=true;
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        try {
                            IJ.showMessage("coreROI: "+coreROI.toString()+ ", snapROI: "+(snapROI==null ? "null" : snapROI.toString()));
                            if (snapROI!=null) {
                                core.clearROI();
                                core.setROI(snapROI.x, snapROI.y, snapROI.width, snapROI.height);
                            } else {
                                core.clearROI();
                                stack = new ImageStack(fov.fullChipWidth_Pixel, fov.fullChipHeight_Pixel);
                            }
                            IJ.showMessage("coreROI: "+coreROI.toString()+ ", snapROI: "+(snapROI==null ? "null" : snapROI.toString()));
                        } catch (Exception ex) {
                            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }*/
                        ImageStack stack=null;
                        int i = 0;
                        ChannelTableModel ctm = (ChannelTableModel) channelTable.getModel();
                        for (int row : rows) {
                            Channel c = ctm.getRowData(row);
                            //snapAndDisplayImage(c.getName(),c.getExposure(),c.getZOffset(),c.getColor());
                            ImageProcessor ip = snapChannelImage(c.getName(), c.getExposure(), c.getZOffset());
                            if (stack==null)
                                stack = new ImageStack(ip.getWidth(),ip.getHeight());
                            stack.addSlice(c.getName(),ip, i);
                            i++;
                        }
                        ImagePlus imp = new ImagePlus();
                        
                        imp.setTitle("Snap");
                        Calibration cal = imp.getCalibration();
                        cal.setUnit("um");
                        cal.pixelWidth = currentAcqSetting.getImagePixelSize();
                        cal.pixelHeight = currentAcqSetting.getImagePixelSize();
                        imp.setCalibration(cal);
                        if (stack.getBitDepth()==24) {
                            //isRGB
                            //show conventional ImagePlus
                            //ignore channel color
                            imp.setStack(stack);
                            imp.show();
                        } else {
                            //show composite image with channel LUTs
                            imp.setStack(stack, rows.length, 1, 1);
                            CompositeImage ci = new CompositeImage(imp);
                            i = 0;
                            LUT[] luts = new LUT[rows.length];
                            for (int row : rows) {
                                luts[i] = LUT.createLutFromColor(currentAcqSetting.getChannels().get(row).getColor());//ctm.getRowData(row).getColor());
                                i++;
                            }
                            ci.setLuts(luts);
                            ci.show();
                            IJ.run("Channels Tool...");
                        }
                        /* for future: handle ROIs
                        if (updatedROI && JOptionPane.showConfirmDialog(this,"Do you want to keep this camera ROI for acquisition setting '"
                                +currentAcqSetting.getName()+"'?","",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
                            fov.setRoi_Pixel(snapROI);
                            calcTilePositions(null,fov, currentAcqSetting.getTilingSetting(), ADJUSTING_SETTINGS);
                        }
                        
                        if (coreROI != null)
                            try {
                                core.setROI(coreROI.x,coreROI.y,coreROI.width,coreROI.height);
                        } catch (Exception ex) {
                            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }*/
                    }
                }  
            } else {
                currentAcqSetting.setObjectiveDevStr(changeConfigGroupStr("Objective",""));
            }
        }
    }//GEN-LAST:event_snapButtonActionPerformed

    private void channelDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_channelDownButtonActionPerformed
        ChannelTableModel ctm = (ChannelTableModel) channelTable.getModel();
        int[] selRows = channelTable.getSelectedRows();
        if (selRows.length > 0 & selRows[selRows.length - 1] < ctm.getRowCount() - 1) {
            int newSelRow = ctm.rowDown(selRows);
            channelTable.setRowSelectionInterval(newSelRow, newSelRow + selRows.length - 1);
        }
    }//GEN-LAST:event_channelDownButtonActionPerformed

    private void channelUpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_channelUpButtonActionPerformed
        ChannelTableModel ctm = (ChannelTableModel) channelTable.getModel();
        int[] selRows = channelTable.getSelectedRows();
        if (selRows.length > 0 & selRows[0] > 0) {
            int newSelRow = ctm.rowUp(selRows);
            ListSelectionModel lsm = areaTable.getSelectionModel();
            channelTable.setRowSelectionInterval(newSelRow, newSelRow + selRows.length - 1);
        }
    }//GEN-LAST:event_channelUpButtonActionPerformed

    private void removeChannelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeChannelButtonActionPerformed
        ChannelTableModel ctm = (ChannelTableModel) channelTable.getModel();
        int[] rows = channelTable.getSelectedRows();
        ctm.removeRows(rows);
    }//GEN-LAST:event_removeChannelButtonActionPerformed

    private void addChannelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addChannelButtonActionPerformed
        ChannelTableModel ctm = (ChannelTableModel) channelTable.getModel();
        currentAcqSetting.setChannelGroupStr(MMCoreUtils.loadAvailableChannelConfigs(this,currentAcqSetting.getChannelGroupStr(),core));
        int index = -1;
        if (ctm.getRowCount() > 0) {
            for (int i = 0; i < MMCoreUtils.availableChannelList.size(); i++) {
                for (int j = 0; j < ctm.getRowCount(); j++) {
                    if (ctm.getRowData(j).getName().equals(MMCoreUtils.availableChannelList.get(i))) {
                        index = -1;
                        break;
                    } else {
                        index = i;
                    }
                }
                if (index != -1) {
                    break;
                }
            }
            if (index != -1) {
                ctm.addRow(new Channel(MMCoreUtils.availableChannelList.get(index), 100, 0, Color.GRAY));
            } else {
                JOptionPane.showMessageDialog(this,"All available channel configurations for group '"+currentAcqSetting.getChannelGroupStr()+"' have been added.");
            }
        } else {
            if (MMCoreUtils.availableChannelList.size() > 0) {
                ctm.addRow(new Channel(MMCoreUtils.availableChannelList.get(0), 100, 0, Color.GRAY));
            } else {
                JOptionPane.showMessageDialog(this,"No Channel configurations found.");
            }
        }
    }//GEN-LAST:event_addChannelButtonActionPerformed

    private void zOffsetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zOffsetButtonActionPerformed
        showZOffsetDlg(false);
    }//GEN-LAST:event_zOffsetButtonActionPerformed

    private void clearRoiButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearRoiButtonActionPerformed
        try {
            FieldOfView fov=currentAcqSetting.getFieldOfView();
            cameraROI = core.getROI();
            if (cameraROI != null
                        && (cameraROI.width != fov.getROI_Pixel(currentAcqSetting.getBinning()).width 
                        || cameraROI.height != fov.getROI_Pixel(currentAcqSetting.getBinning()).height)) {
                JOptionPane.showMessageDialog(this,"ROIs are not supported.\nUsing full camera chip.");
            }
            core.clearROI();
        } catch (Exception ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        getCameraSpecs(currentDetector.getFieldRotation());
        for (AcqSetting setting:acqSettings) {
            FieldOfView fov=new FieldOfView(currentDetector.getFullWidth_Pixel(), currentDetector.getFullHeight_Pixel(),currentDetector.getFieldRotation());
            setting.setFieldOfView(fov);
//            setting.getFieldOfView().clearROI();
        }
        if (currentAcqSetting.getImagePixelSize() > 0 && recalculateTiles) {
            calcTilePositions(null, currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), ADJUSTING_SETTINGS);
        } else if (currentAcqSetting.getImagePixelSize() <= 0) {
            for (Area a:acqLayout.getAreaArray()) {
                a.setUnknownTileNum(true);
            }
            currentAcqSetting.setTotalTiles(-1);
            ((AcqSettingTableModel)acqSettingTable.getModel()).updateTileCell(acqSettingTable.getSelectedRow());
            acqLayoutPanel.repaint();
        }
        updatePixelSize(currentAcqSetting.getObjective());
    }//GEN-LAST:event_clearRoiButtonActionPerformed

    private void closeGapsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeGapsButtonActionPerformed
        double newTileOverlap=acqLayout.closeTilingGaps(currentAcqSetting.getFieldOfView(), 0.005);
        if (newTileOverlap!=currentAcqSetting.getTileOverlap()) {
            //round up to next percentage to ensure that no gaps remain
            newTileOverlap=Math.ceil(newTileOverlap*100)/100;
            tileOverlapField.setText(Integer.toString((int)Math.round(newTileOverlap*100)));
            currentAcqSetting.setTileOverlap(Double.parseDouble(tileOverlapField.getText())/100);
            calcTilePositions(null,currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(),"Adjusting...");
        }
    }//GEN-LAST:event_closeGapsButtonActionPerformed

    private void zStepSizeFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_zStepSizeFieldPropertyChange
        if (currentAcqSetting!=null) {
            currentAcqSetting.setZStepSize(((Number)zStepSizeField.getValue()).doubleValue());
            double begin=currentAcqSetting.getZBegin();
            double end=currentAcqSetting.getZEnd();
            double newdist;
            if (!zStackCenteredCheckBox.isSelected()) {
                if (currentAcqSetting.getZStepSize()==0) {
                    currentAcqSetting.setZStepSize(1);
                }
                currentAcqSetting.setZSlices((int) Math.abs(Math.round(end - begin) / currentAcqSetting.getZStepSize()) + 1);
                double olddist=end-begin;
                newdist=Math.abs((currentAcqSetting.getZSlices()-1)*currentAcqSetting.getZStepSize());
                if (olddist > 0) {
                    currentAcqSetting.setZBegin(currentAcqSetting.getZBegin()+(olddist-newdist)/2);
                    currentAcqSetting.setZEnd(currentAcqSetting.getZEnd()-(olddist-newdist)/2);
                } else {
                    currentAcqSetting.setZBegin(currentAcqSetting.getZBegin()-(Math.abs(olddist)-newdist)/2);
                    currentAcqSetting.setZEnd(currentAcqSetting.getZEnd()+(Math.abs(olddist)-newdist)/2);
                }
                zStackBeginField.setValue(currentAcqSetting.getZBegin());
                zStackEndField.setValue(currentAcqSetting.getZEnd());
                zSlicesSpinner.setValue(currentAcqSetting.getZSlices());
            } else {
                newdist=Math.abs((currentAcqSetting.getZSlices()-1)*currentAcqSetting.getZStepSize());
                if (currentAcqSetting.getZBegin() < currentAcqSetting.getZEnd()) {
                    currentAcqSetting.setZBegin(newdist!=0 ? -newdist/2 : 0);
                    currentAcqSetting.setZEnd(newdist!=0 ? newdist/2 : 0);                    
                } else {
                    currentAcqSetting.setZBegin(newdist!=0 ? newdist/2 : 0);
                    currentAcqSetting.setZEnd(newdist!=0 ? -newdist/2 : 0);                    
                }
                zStackBeginField.setValue(currentAcqSetting.getZBegin());
                zStackEndField.setValue(currentAcqSetting.getZEnd());
            }
            zStackTotalDistLabel.setText(String.format("%1$,.3f", newdist));
        }
    }//GEN-LAST:event_zStepSizeFieldPropertyChange

    private void zSlicesSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_zSlicesSpinnerStateChanged
        if (currentAcqSetting!=null) {
            currentAcqSetting.setZSlices((Integer)zSlicesSpinner.getValue());
            double newdist;
            if (!zStackCenteredCheckBox.isSelected()) {
                int currentSlices=currentAcqSetting.getZSlices();
                double stepSize;
                if (currentSlices > 1) {
                    stepSize=Math.abs(currentAcqSetting.getZEnd()-currentAcqSetting.getZBegin()) / (currentSlices-1);
                    currentAcqSetting.setZStepSize(stepSize);
                    zStepSizeField.setValue(stepSize);
                } else {
//                    stepSize=stepSize;
                }    
                newdist=Math.abs((currentAcqSetting.getZSlices()-1)*currentAcqSetting.getZStepSize());
            } else {
                newdist=Math.abs((currentAcqSetting.getZSlices()-1)*currentAcqSetting.getZStepSize());
                if (currentAcqSetting.getZBegin() < currentAcqSetting.getZEnd()) {
                    currentAcqSetting.setZBegin(newdist!=0 ? -newdist/2 : 0);
                    currentAcqSetting.setZEnd(newdist!=0 ? newdist/2 : 0);                    
                } else {
                    currentAcqSetting.setZBegin(newdist!=0 ? newdist/2 : 0);
                    currentAcqSetting.setZEnd(newdist!=0 ? -newdist/2 : 0);                    
                }
                zStackBeginField.setValue(currentAcqSetting.getZBegin());
                zStackEndField.setValue(currentAcqSetting.getZEnd());
            }
            zStackTotalDistLabel.setText(String.format("%1$,.3f", newdist));
        }
    }//GEN-LAST:event_zSlicesSpinnerStateChanged

    private void zStackBeginFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_zStackBeginFieldPropertyChange
        if (currentAcqSetting!=null) {
            currentAcqSetting.setZBegin(((Number)zStackBeginField.getValue()).doubleValue());
            double newdist=Math.abs(currentAcqSetting.getZEnd()-currentAcqSetting.getZBegin());
            if (currentAcqSetting.getZSlices() > 1) {
                double stepSize=newdist / (currentAcqSetting.getZSlices()-1);
                currentAcqSetting.setZStepSize(stepSize);
                zStepSizeField.setValue(stepSize);
            }
            zStackTotalDistLabel.setText(String.format("%1$,.3f", newdist));
        }
    }//GEN-LAST:event_zStackBeginFieldPropertyChange

    private void zStackEndFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_zStackEndFieldPropertyChange
        if (currentAcqSetting!=null) {
            currentAcqSetting.setZEnd(((Number)zStackEndField.getValue()).doubleValue());
            double newdist=Math.abs(currentAcqSetting.getZEnd()-currentAcqSetting.getZBegin());
            if (currentAcqSetting.getZSlices() > 1) {
                double stepSize=newdist / (currentAcqSetting.getZSlices()-1);
                currentAcqSetting.setZStepSize(stepSize);
                zStepSizeField.setValue(stepSize);
            }
            zStackTotalDistLabel.setText(String.format("%1$,.3f", newdist));
        }
    }//GEN-LAST:event_zStackEndFieldPropertyChange

    private void instantiateStageControlPlugin() {
        stageControlPlugin=null;
        File pluginRootDir = new File(System.getProperty("org.micromanager.plugin.path", MMPLUGINSDIR));
        String jarFileStr=new File(new File(pluginRootDir, DEVICE_CONTROL_DIR),STAGE_CONTROL_FILE).getAbsolutePath();
        String stageControlClassName="";
        try {
            JarFile jarFile;
            jarFile = new JarFile(jarFileStr);
            Enumeration e = jarFile.entries();

            URL[] urls = { new URL("jar:file:" + jarFileStr+"!/") };
            URLClassLoader classLoader = URLClassLoader.newInstance(urls,
                //    this.getClass().getClassLoader());
                MMPlugin.class.getClassLoader());

            int i=0;
            List<String> classes=new ArrayList<String>();
            while (e.hasMoreElements()) {
                JarEntry je = (JarEntry) e.nextElement();
                if(je.isDirectory() || !je.getName().endsWith(".class")){
                    continue;
                }
                 // -6 because of .class
                String className = je.getName().substring(0,je.getName().length()-6);
                className = className.replace('/', '.');
                try {
                    Class<?> clazz=Class.forName(className);
                    for (Class<?> iface : clazz.getInterfaces()) {
                        if (iface == MMPlugin.class) {
                            classes.add(className);
                            break;
                        }
                    }
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (classes.isEmpty()) {
                JOptionPane.showMessageDialog(this,"No classes found.");
                return;
            } if (classes.size() > 1) {   
                JComboBox jcb = new JComboBox(classes.toArray());
                JOptionPane.showMessageDialog( null, jcb, "Select plugin class:", JOptionPane.QUESTION_MESSAGE);
                stageControlClassName=(String)jcb.getSelectedItem();
            } else {
                stageControlClassName=classes.get(0);
            }

        } catch (MalformedURLException ex) {
            JOptionPane.showMessageDialog(this,"Cannot from URL to load classes in jar file.");
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            return;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,"Problem reading content of jar file.");
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            return;    
        } 
        if (stageControlClassName!="") {
            Class<?> clazz;
            try {
                clazz = Class.forName(stageControlClassName);
                stageControlPlugin = (MMPlugin) clazz.newInstance();
            } catch (ClassNotFoundException ex) {
                ReportingUtils.logError(ex);
            } catch (InstantiationException ex) {
                ReportingUtils.logError(ex);
            } catch (IllegalAccessException ex) {
                ReportingUtils.logError(ex);
            }
        }             
    }
    
    private void stageControlButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stageControlButtonActionPerformed

        if (stageControlPlugin != null) {
            stageControlPlugin.setApp(app);
            stageControlPlugin.show();
        }    
    }//GEN-LAST:event_stageControlButtonActionPerformed

    private void updateAutofocusToolTip(AcqSetting setting) {
        String text="";
        if (setting.getAutofocusDevice()!=null) {
            text="<html><b>AF method: " +setting.getAutofocusDevice()+"</b><br>";
            JSONObject properties=setting.getAutofocusProperties();
            Iterator<String> keys=properties.keys();
            while (keys.hasNext()) {
                String pname=keys.next();
                try {
                    text+=pname+": "+properties.getString(pname)+"<br>";
                } catch (JSONException ex) {
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            text+="</html>";
        } else {
            text="<html><b>AF method: not selected</b></html>";
        }
//        autofocusButton.setToolTipText(text);
        autofocusCheckBox.setToolTipText(text);
    } 
    
    private void autofocusImportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autofocusImportButtonActionPerformed
        Autofocus af=null;
        try {
            af=app.getAutofocus();
            if (af==null) {
                JOptionPane.showMessageDialog(this,"No autofocus device/plugin installed.");
            }
            currentAcqSetting.setAutofocusSettings(af, core);
            updateAutofocusToolTip(currentAcqSetting);
        } catch (MMException ex) {
            if (af!=null) {
                JOptionPane.showMessageDialog(this,"Error transferrring properties for "+af.getDeviceName());
            }
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_autofocusImportButtonActionPerformed

    private void autoExposureButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoExposureButtonActionPerformed
        AbstractCellEditor ae = (AbstractCellEditor) channelTable.getCellEditor();
        if (ae != null) {
            ae.stopCellEditing();
        }
        /*        AcqSetting setting=null;
        if (acqSettings!=null && acqSettings.size()>cAcqSettingIdx) {
            setting=acqSettings.get(cAcqSettingIdx);
        }*/
        if (channelTable.getSelectedRowCount() != 1) {
            JOptionPane.showMessageDialog(this, "Select one channel");
        } else {
            try {
                if (!currentAcqSetting.getObjective().equals(core.getProperty(objectiveDevStr, "Label"))) {
                    JOptionPane.showMessageDialog(this, "Switching to "+currentAcqSetting.getObjective()+" objective.");
                }
            } catch (Exception ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (setObjectiveAndBinning(currentAcqSetting, true)) {
                String chGroupStr=MMCoreUtils.loadAvailableChannelConfigs(this,currentAcqSetting.getChannelGroupStr(),core);
                if (!chGroupStr.equals(currentAcqSetting.getChannelGroupStr())) {
                    currentAcqSetting.setChannelGroupStr(chGroupStr);
                    initializeChannelTable(currentAcqSetting);
                } else {
                    if (chGroupStr!=null) {
                        final int row = channelTable.getSelectedRow();
//                        ImageStack stack = new ImageStack(cCameraPixX / currentAcqSetting.getBinning(), cCameraPixY / currentAcqSetting.getBinning());
                        
                        //read out currentROI setting and update ROI in detector of current AcqSetting
                        FieldOfView fov=currentAcqSetting.getFieldOfView();
                        Rectangle snapROI=fov.getROI_Pixel(currentAcqSetting.getBinning());
                        Rectangle coreROI=null;
                        boolean updatedROI=false;
                        
                        try {
                            coreROI=core.getROI();
                            if (coreROI !=null 
                                    && (coreROI.width != fov.getROI_Pixel(currentAcqSetting.getBinning()).width 
                                    || coreROI.height != fov.getROI_Pixel(currentAcqSetting.getBinning()).height)) {
                                JOptionPane.showMessageDialog(this, "ROIs are not supported. Clearing ROI.");
//                                fov.clearROI();
                                core.clearROI();
                                for (AcqSetting setting:acqSettings) {
//                                    FieldOfView fov=new FieldOfView(currentDetector.getFullWidth_Pixel(), currentDetector.getFullHeight_Pixel(),currentDetector.getFieldRotation());
//                                    setting.setFieldOfView(fov);
                                    setting.getFieldOfView().clearROI();
                                }
                            }    
                        } catch (Exception ex) {
                            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        final ChannelTableModel ctm = (ChannelTableModel) channelTable.getModel();
                        final Channel c = ctm.getRowData(row);
                        
                        final JFrame guiFrame=this;

                        AutoExposureTool aet=new AutoExposureTool(app,chGroupStr,c.getName(),c.getExposure(),true);
/*                        
                        ExecutorService executor=Executors.newSingleThreadExecutor();
                        Future<Double> future=executor.submit(aet);
                        double exp;
                        try {
                            exp=future.get();
                        } catch (InterruptedException ex) {
                              //exception caused by timeout of future.get()
            //                return new ArrayList<File>();
                        } catch (ExecutionException ex) {
                            //this caused by InterruptedException in callable, so rethrow the InterruptedException to enforce call of cleanUp()
                            IJ.log(this.getClass().getName()+": ExecutionException: caused by callable");
                        }
                        
//                            double exp=aet.getOptimalExposure();
                            if (exp==-1) {
                                JOptionPane.showMessageDialog(this,"Canceled");
                            } else {
                                int result=JOptionPane.showConfirmDialog(guiFrame,"Suggested exposure time for channel "+c.getName()+": "+formatNumber("0.0", exp)+" ms.\n"
                                                + "Adjust exposure time to new value?","Auto-Exposure",JOptionPane.YES_NO_OPTION);
                                            if (result==JOptionPane.YES_OPTION) {
                                                c.setExposure(exp);
                                                ctm.fireTableRowsUpdated(row, row);
                                            }                            
                            }
*/                    
                        enableGUI(false);
                        acquireButton.setEnabled(false);
                        SwingUtilities.invokeLater(new Runnable() {
                            
                            @Override
                            public void run() {
                                final JFrame frame=new JFrame("Auto-Exposure");
                                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                                frame.setPreferredSize(new Dimension(400,120));
                                frame.setResizable(false);
                                frame.getContentPane().setLayout(new GridLayout(0,1));

                                JLabel label=new JLabel("Channel '"+c.getName()+"': Testing exposure");
                                final JLabel expLabel=new JLabel("");

                                JPanel panel = new JPanel();
                                panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
                                panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
                                panel.add(label);
                                panel.add(Box.createRigidArea(new Dimension(10, 0)));
                                panel.add(expLabel);
                                panel.add(Box.createHorizontalGlue());

                                frame.getContentPane().add(panel);

                                final SwingWorker<Double, Double> worker = new SwingWorker<Double, Double>() {

                                    private double optimalExp;
                                    private ImageProcessor ip;

                                    @Override
                                    protected Double doInBackground() {

                                        double newExp=c.getExposure();
                                        publish(newExp);
                                        double maxExp=-1;
                                        double minExp=1;
                                        boolean optimalExpFound=false;
                                        int i=1;
                                        while (!optimalExpFound && !isCancelled()) {
                                            ip = snapChannelImage(c.getName(), newExp , 0);//c.getZOffset());
                                            ImageStatistics stats=ip.getStatistics();
                                            if ((stats.max < Math.pow(2,core.getImageBitDepth())-1)
                                            || stats.maxCount < MAX_SATURATION*ip.getWidth()*ip.getHeight()){
                                                //underexposed
                                                minExp=newExp;
                                                if (maxExp==-1)
                                                    newExp*=2;
                                                else
                                                    newExp=(minExp+maxExp)/2;
                                                if (newExp >= MAX_EXPOSURE) {
                                                    newExp=MAX_EXPOSURE;
                                                    maxExp=MAX_EXPOSURE;
                                                }
                                            }  else {
                                                //overexposed
                                                maxExp=newExp;
                                                newExp=(minExp+maxExp)/2;
                                                if (newExp <=0) {
                                                    break;
                                                }
                                            }
                                            publish(newExp);// Notify progress
                                            optimalExpFound=Math.abs(maxExp/minExp - 1) < 0.025;
                                            try {
                                                Thread.sleep(100);
                                            } catch (InterruptedException ex) {
                                                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                            i++;
                                        }
                                        if (!isCancelled()) {
                                            optimalExp=(maxExp+minExp)/2;
                                        } else {
                                            optimalExp=-1;
                                        }    
                                        return optimalExp;
                                    }

                                    @Override
                                    protected void process(List<Double> exp) {
                                        String msg=formatNumber("0.0", exp.get(exp.size()-1))+" ms";
                                        expLabel.setText(msg);
                                    }

                                    @Override
                                    protected void done() {
                                        frame.dispose();
                                        new ImagePlus("Auto-Exposure: "+c.getName(),ip).show();
                                        if (!isCancelled()) {
                                            if (optimalExp==MAX_EXPOSURE) {
                                                JOptionPane.showMessageDialog(guiFrame, "Reached maximum exposure ("+MAX_EXPOSURE+" ms).");
                                            }
                                            optimalExp=0.1*(Math.round(optimalExp*10));
                                            int result=JOptionPane.showConfirmDialog(guiFrame,"Suggested exposure time for channel "+c.getName()+": "+formatNumber("0.0", optimalExp)+" ms.\n"
                                                + "Adjust exposure time to new value?","Auto-Exposure",JOptionPane.YES_NO_OPTION);
                                            if (result==JOptionPane.YES_OPTION) {
                                                c.setExposure(optimalExp);
                                                ctm.fireTableRowsUpdated(row, row);
                                            }
                                        }
                                        enableGUI(true);
                                        acquireButton.setEnabled(acqLayout.getNoOfMappedStagePos()>0);
                                    }

                                }; //end SwingWorker

                                frame.addWindowListener(new WindowAdapter() {
                                    @Override
                                    public void windowClosing(WindowEvent e) {
                                        worker.cancel(true);
                                    }
                                });

                                JButton cancelButton=new JButton("Cancel");
                                cancelButton.addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        worker.cancel(true);
                                    }
                                });
                                JPanel buttonPanel=new JPanel();
                                buttonPanel.add(cancelButton);
                                frame.getContentPane().add(buttonPanel);

                                frame.pack();
                                frame.setLocationRelativeTo(null);
                                frame.setVisible(true);

                                worker.execute();

                            } //end run()
                        });//end invokeLater()
                    }
                }  
            } else {
                currentAcqSetting.setObjectiveDevStr(changeConfigGroupStr("Objective",""));
            }
        }
    }//GEN-LAST:event_autoExposureButtonActionPerformed


    private void saveLayout() {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(acqLayout.getFile().getParentFile());
        /*        jfc.setFileFilter(new javax.swing.filechooser.FileFilter() {
         public boolean accept(File f) {
         String name = f.getName().toLowerCase();
               
         return name.endsWith(".xml");
         }
 
         public String getDescription() {
         return "XML files";
         }
         });*/
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setMultiSelectionEnabled(false);
        jfc.ensureFileIsVisible(acqLayout.getFile());
        if (jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File f = jfc.getSelectedFile();
            if (!Utils.getExtension(f).toLowerCase().equals(".txt")) {
                JOptionPane.showMessageDialog(this,"Not a txt file");
                f = new File(f.getAbsolutePath() + ".txt");
            }
            if (f.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(null, "Replace " + f.getName(), "", JOptionPane.YES_NO_OPTION);
                if (overwrite != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            acqLayout.setName(f.getName());
            acqLayout.setFile(f);
//            acqLayout.saveLayoutToXMLFile(f);

            FileWriter fw;
            try {
                fw = new FileWriter(f);
                JSONObject layoutObj=new JSONObject();
                try {
                    JSONObject obj=acqLayout.toJSONObject();
                    if (obj!=null) {
                        layoutObj.put(AcqLayout.TAG_LAYOUT,obj);
                        fw.write(layoutObj.toString(4));
                    }
                } catch (JSONException ex) {
                    JOptionPane.showMessageDialog(null,"Error parsing Acquisition Layout as JSONObject.");
                    Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null,"Error saving Acquisition Layout as JSONObject.");
                    Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    fw.close();
                }        
            } catch (IOException ex) {
                Logger.getLogger(AcqLayout.class.getName()).log(Level.SEVERE, null, ex);
            }
            
//            layoutFile = f;
            layoutFileLabel.setText(acqLayout.getName());
            layoutFileLabel.setToolTipText("LOADED FROM: "+acqLayout.getFile().getAbsolutePath());
            acqLayout.setModified(false);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(AcqFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AcqFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AcqFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AcqFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new AcqFrame(null).setVisible(true); //need to check null as parameter
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel acqLayoutPanel;
    private javax.swing.JTabbedPane acqModePane;
    private javax.swing.JComboBox acqOrderList;
    private javax.swing.JButton acqSettingDownButton;
    private javax.swing.JTable acqSettingTable;
    private javax.swing.JButton acqSettingUpButton;
    private javax.swing.JButton acquireButton;
    private javax.swing.JButton addAcqSettingButton;
    private javax.swing.JButton addAreaFilterButton;
    private javax.swing.JButton addChannelButton;
    private javax.swing.JButton addChannelFilterButton;
    private javax.swing.JButton addDataProcFromFileButton;
    private javax.swing.JButton addFrameFilterButton;
    private javax.swing.JButton addImageStorageButton;
    private javax.swing.JButton addImageTagFilterButton;
    private javax.swing.JButton addROIFinderButton;
    private javax.swing.JButton addScriptAnalyzerButton;
    private javax.swing.JButton addZFilterButton;
    private javax.swing.JButton areaDownButton;
    private javax.swing.JLabel areaLabel;
    private javax.swing.JTable areaTable;
    private javax.swing.JButton areaUpButton;
    private javax.swing.JButton autoExposureButton;
    private javax.swing.JButton autofocusButton;
    private javax.swing.JCheckBox autofocusCheckBox;
    private javax.swing.JButton autofocusImportButton;
    private javax.swing.JComboBox binningComboBox;
    private javax.swing.JButton browseImageDestPathButton;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JButton cancelThreadButton;
    private javax.swing.JButton channelDownButton;
    private javax.swing.JTable channelTable;
    private javax.swing.JButton channelUpButton;
    private javax.swing.JButton clearRoiButton;
    private javax.swing.JButton closeGapsButton;
    private javax.swing.JCheckBox clusterCheckBox;
    private javax.swing.JLabel clusterLabel1;
    private javax.swing.JLabel clusterLabel2;
    private javax.swing.JTextField clusterXField;
    private javax.swing.JTextField clusterYField;
    private javax.swing.JToggleButton commentButton;
    private javax.swing.JLabel cursorLabel;
    private javax.swing.JButton deleteAcqSettingButton;
    private javax.swing.JLabel durationText;
    private javax.swing.JButton editProcessorButton;
    private javax.swing.JLabel expSettingsFileLabel;
    private javax.swing.JTextField experimentTextField;
    private javax.swing.JTextField framesField;
    private javax.swing.JCheckBox insideOnlyCheckBox;
    private javax.swing.JTextField intHourField;
    private javax.swing.JTextField intMinField;
    private javax.swing.JTextField intSecField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JLabel layoutFileLabel;
    private javax.swing.JScrollPane layoutScrollPane;
    private javax.swing.JButton liveButton;
    private javax.swing.JButton loadAcqSettingButton;
    private javax.swing.JButton loadExpSettingFileButton;
    private javax.swing.JButton loadImagePipelineButton;
    private javax.swing.JButton loadLayoutButton;
    private javax.swing.JButton loadProcTreeButton;
    private javax.swing.JTextField maxSitesField;
    private javax.swing.JLabel maxSitesLabel;
    private javax.swing.JButton mergeAreasButton;
    private javax.swing.JToggleButton moveToScreenCoordButton;
    private javax.swing.JButton newAreaButton;
    private javax.swing.JComboBox objectiveComboBox;
    private javax.swing.JLabel pixelSizeLabel;
    private javax.swing.JProgressBar processProgressBar;
    private javax.swing.JTree processorTreeView;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton removeAreaButton;
    private javax.swing.JButton removeChannelButton;
    private javax.swing.JButton removeProcessorButton;
    private javax.swing.JButton reverseButton;
    private javax.swing.JLabel rootDirLabel;
    private javax.swing.JButton saveAcqSettingButton;
    private javax.swing.JButton saveExpSettingFileButton;
    private javax.swing.JButton saveLayoutButton;
    private javax.swing.JButton saveProcTreeButton;
    private javax.swing.JToggleButton selectButton;
    private javax.swing.JPanel sequenceListPanel;
    private javax.swing.JTabbedPane sequenceTabbedPane;
    private javax.swing.JButton setLandmarkButton;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JCheckBox showZProfileCheckBox;
    private javax.swing.JCheckBox siteOverlapCheckBox;
    private javax.swing.JButton snapButton;
    private javax.swing.JButton stageControlButton;
    private javax.swing.JLabel stagePosXLabel;
    private javax.swing.JLabel stagePosYLabel;
    private javax.swing.JLabel stagePosZLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JTextField tileOverlapField;
    private javax.swing.JLabel tileSizeLabel;
    private javax.swing.JComboBox tilingDirComboBox;
    private javax.swing.JComboBox tilingModeComboBox;
    private javax.swing.JCheckBox timelapseCheckBox;
    private javax.swing.JLabel timepointLabel;
    private javax.swing.JLabel totalAreasLabel;
    private javax.swing.JLabel totalTilesLabel;
    private javax.swing.JButton zOffsetButton;
    private javax.swing.JSpinner zSlicesSpinner;
    private javax.swing.JFormattedTextField zStackBeginField;
    private javax.swing.JCheckBox zStackCenteredCheckBox;
    private javax.swing.JCheckBox zStackCheckBox;
    private javax.swing.JFormattedTextField zStackEndField;
    private javax.swing.JPanel zStackPanel;
    private javax.swing.JLabel zStackTotalDistLabel;
    private javax.swing.JFormattedTextField zStepSizeField;
    private javax.swing.JToggleButton zoomButton;
    // End of variables declaration//GEN-END:variables

/*    
    private AcqLayout loadLayout(File file) {//returns true if layout has been changed
//        IJ.log("AcqFrame.loadLayout: "+absPath);
        BufferedReader br;
        StringBuilder sb=new StringBuilder();
        JSONObject layoutObj=null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) !=null) {
                sb.append(line);
            }
            JSONObject expSettingsObj=new JSONObject(sb.toString());
            layoutObj=expSettingsObj.getJSONObject(AcqLayout.TAG_LAYOUT);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
//        acqLayout=new AcqLayout(layoutObj, file);
        AcqLayout layout=AcqLayout.createFromJSONObject(layoutObj, file);
        return layout;
    }
*/

    private void setTilingInsideAreaOnly(boolean b) {
//        if (acqSettings!=null && acqSettings.size()>cAcqSettingIdx) {
//            AcqSetting setting = acqSettings.get(cAcqSettingIdx);
//        if (currentAcqSetting!=null) {
        currentAcqSetting.enableInsideOnly(b);
        if (!calculating && recalculateTiles) {
            calcTilePositions(null, currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), ADJUSTING_SETTINGS);
        }
//        }
    }
    

    private String createChannelNamePrefString() {
        String str = "";
        ChannelTableModel ctm = (ChannelTableModel) channelTable.getModel();
        for (int i = 0; i < ctm.getRowCount(); i++) {
            str += ctm.getValueAt(i, 0) + "\n";
        }
        return str;
    }

    private String createChannelExpPrefString() {
        String str = "";
        ChannelTableModel ctm = (ChannelTableModel) channelTable.getModel();
        for (int i = 0; i < ctm.getRowCount(); i++) {
            str += ctm.getValueAt(i, 1) + "\n";
        }
        return str;
    }

    private String createChannelZOffsetPrefString() {
        String str = "";
        ChannelTableModel ctm = (ChannelTableModel) channelTable.getModel();
        for (int i = 0; i < ctm.getRowCount(); i++) {
            str += ctm.getValueAt(i, 2) + "\n";
        }
        return str;
    }

    private String createChannelStitchingPrefString() {
        String str = "";
        ChannelTableModel ctm = (ChannelTableModel) channelTable.getModel();
        for (int i = 0; i < ctm.getRowCount(); i++) {
            str += ctm.getValueAt(i, 3) + "\n";
        }
        return str;
    }

    private String createChannelColorPrefString() {
        String str = "";
        ChannelTableModel ctm = (ChannelTableModel) channelTable.getModel();
        for (int i = 0; i < ctm.getRowCount(); i++) {
            str += Channel.colorToHexString((Color) ctm.getValueAt(i, 4)) + "\n";
        }
        return str;
    }

    private void parseChannelConfig(String chStr, String expStr, String zOffsetStr, String stitchStr, String colorStr) {
        ChannelTableModel ctm = (ChannelTableModel) channelTable.getModel();
        int row = 0;
        int start = 0;
        ArrayList<String> ch = new ArrayList<String>();
        while (start < chStr.length() && chStr.indexOf("\n", start) > 0) {
            int end = chStr.indexOf("\n", start);
            ch.add(chStr.substring(start, end));
            start = end + 1;
            row++;
        }
        row = 0;
        start = 0;
        ArrayList<Double> exp = new ArrayList<Double>();
        while (start < expStr.length() && expStr.indexOf("\n", start) > 0) {
            int end = expStr.indexOf("\n", start);
            exp.add(Double.parseDouble(expStr.substring(start, end)));
            start = end + 1;
            row++;
        }
        row = 0;
        start = 0;
        ArrayList<Double> zOffset = new ArrayList<Double>();
        while (start < zOffsetStr.length() && zOffsetStr.indexOf("\n", start) > 0) {
            int end = zOffsetStr.indexOf("\n", start);
            zOffset.add(Double.parseDouble(zOffsetStr.substring(start, end)));
            start = end + 1;
            row++;
        }
        row = 0;
        start = 0;
        ArrayList<Boolean> stitch = new ArrayList<Boolean>();
        while (start < stitchStr.length() && zOffsetStr.indexOf("\n", start) > 0) {
            int end = stitchStr.indexOf("\n", start);
            stitch.add(Boolean.parseBoolean(stitchStr.substring(start, end)));
            start = end + 1;
            row++;
        }
        row = 0;
        start = 0;
        ArrayList<String> colorList = new ArrayList<String>();
        while (start < colorStr.length() && colorStr.indexOf("\n", start) > 0) {
            int end = colorStr.indexOf("\n", start);
            colorList.add(colorStr.substring(start, end));
            start = end + 1;
            row++;
        }
        /*        useChannelList=new ArrayList<Channel>();
         for (int i=0; i<ch.size(); i++) {
         Color color=Color.GRAY;
         if (i < colorList.size())
         color=Color.decode(colorList.get(i));
         double exposure=0;
         if (i < exp.size())
         exposure=exp.get(i);
         double zOffs=0;
         if (i < zOffset.size())
         zOffs=zOffset.get(i);
         boolean st=false;
         if (i < stitch.size())
         st=stitch.get(i);
         useChannelList.add(new Channel(ch.get(i),exposure,zOffs,st,color));
         }
         */
//        IJ.log("finish parsing Channel Config");
        initializeChannelTable(currentAcqSetting); //adds last used channels to Table if found in availableChannels
    }

    public void loadPreferences() {
        expSettingsFile  = new File(Prefs.get("edu.virginia.autoimage.expSettingsFile", Prefs.getHomeDir()));
        dataProcessorPath=Prefs.get("edu.virginia.autoimage.dataProcessorPath", Prefs.getHomeDir());
        imageDestPath = Prefs.get("edu.virginia.autoimage.rootDir", Prefs.getHomeDir());
        experimentTextField.setText(Prefs.get("edu.virginia.autoimage.experimentBaseName", "Exp"));
        rootDirLabel.setText(imageDestPath);
        rootDirLabel.setToolTipText(imageDestPath);
        objectiveDevStr = Prefs.get("edu.virginia.autoimage.objectiveDevStr", "ObjectiveDevStr");
        showZProfileCheckBox.setSelected(Boolean.parseBoolean(Prefs.get("edu.virginia.autoimage.showZProfile", "true")));
    }

    public void savePreferences() {        
        Prefs.set("edu.virginia.autoimage.expSettingsFile", expSettingsFile.getAbsolutePath());
        Prefs.set("edu.virginia.autoimage.dataProcessorPath", dataProcessorPath);
        Prefs.set("edu.virginia.autoimage.rootDir", rootDirLabel.getText());
        Prefs.set("edu.virginia.autoimage.experimentBaseName", experimentTextField.getText());
        Prefs.set("edu.virginia.autoimage.objectiveDevStr", objectiveDevStr);
        Prefs.set("edu.virginia.autoimage.showZProfile", showZProfileCheckBox.isSelected());

        Prefs.savePreferences();

    }

    private List<AcqSetting> loadAcquisitionSettings(JSONArray acqSettingArray) throws JSONException {
        List<AcqSetting> settings=new ArrayList<AcqSetting>();
        List<String> groupStr=Arrays.asList(core.getAvailableConfigGroups().toArray());
        for (int i=0; i<acqSettingArray.length(); i++) {
            JSONObject acqSettingObj=acqSettingArray.getJSONObject(i);
            try {
                AcqSetting setting=new AcqSetting(acqSettingObj);
                IJ.log("    Loading acquisition sequence # "+Integer.toString(i+1)+", name: "+setting.getName());
//                setting.setCameraChipSize(cCameraPixX, cCameraPixY);
//                setting.setCameraChipSize(currentDetector.getFullWidth_Pixel(), currentDetector.getFullDetectorPixelY());
                setting.getFieldOfView().setFullSize_Pixel(currentDetector.getFullWidth_Pixel(),currentDetector.getFullHeight_Pixel());
                setting.getFieldOfView().setFieldRotation(currentDetector.getFieldRotation());
                if (setting.getChannelGroupStr() == null 
                        || !groupStr.contains(setting.getChannelGroupStr())) {
                    setting.setChannelGroupStr(changeConfigGroupStr("Channel",""));
                }        
                if (!availableObjectives.contains(setting.getObjective())) {
                    JOptionPane.showMessageDialog(this, "Objective "+setting.getObjective()+" not found. Choosing alternative.");
                    setting.setObjective(availableObjectives.get(0), getObjPixelSize(availableObjectives.get(0)));
                } else {
                    setting.setObjective(setting.getObjective(), getObjPixelSize(setting.getObjective()));
                }
                settings.add(setting);
            } catch (ClassNotFoundException ex) {
                IJ.log("Class not found #"+i);
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                IJ.log("InstantiationException #"+i);
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                IJ.log("IllegalAccessException #"+i);
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return settings;
    }
            
    private List<AcqSetting> loadAcquisitionSettings(File file) {
        IJ.log("Loading acquisition settings from "+file.getAbsolutePath()+"...");
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "Acquisition Setting "+file.getAbsolutePath()+" not found.");
            IJ.log("    Acquisition settings file not found.");
            return null;
        } else {    
            List<AcqSetting> settings=new ArrayList<AcqSetting>();
            try {
                BufferedReader br=new BufferedReader(new FileReader(file));
                StringBuilder sb=new StringBuilder();
                String line;
                while ((line = br.readLine()) !=null) {
                    sb.append(line);
                }
                JSONObject expSettingsObj=new JSONObject(sb.toString());
                settings=loadAcquisitionSettings(expSettingsObj.getJSONArray(AcqSetting.TAG_ACQ_SETTING_ARRAY));
                IJ.log("    "+settings.size()+ " acquisition setting(s) found and loaded.");
                if (settings.size() == 0) {
                    FieldOfView fov=new FieldOfView(currentDetector.getFullWidth_Pixel(), currentDetector.getFullHeight_Pixel(),currentDetector.getFieldRotation());
                    AcqSetting setting = new AcqSetting("Seq_1", fov, availableObjectives.get(0), getObjPixelSize(availableObjectives.get(0)),currentDetector.getBinningOption(0,1), false);
                    settings.add(setting);
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JSONException ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
            /*need to check params
             - objective
             - pixel calib
             - binning options 
             - channels
             - tilingmode (in particular random)
             */ 

            //update Model for AcqSetting Tables
    /*        DefaultListModel lm = new DefaultListModel();
            for (AcqSetting setting : settings) {
                lm.addElement(setting.getName());
            }*/
            deleteAcqSettingButton.setEnabled(settings!=null && settings.size() > 1);
            IJ.log("Loading acquisition settings completed.");
            return settings;
        }
    }


    private void updateAcqSettingTab(AcqSetting setting) {
        recalculateTiles = false;
        binningComboBox.setSelectedItem(Integer.toString(setting.getBinning()));
        objectiveComboBox.setSelectedItem(setting.getObjective());
        tilingModeComboBox.setSelectedItem(setting.getTilingMode());
        tilingDirComboBox.setSelectedItem(setting.getTilingDir());
        /*        fullTilingButton.setSelected(setting.getTilingMode()==TilingSetting.Mode.FULL);
         centerTilingButton.setSelected(setting.getTilingMode()==TilingSetting.Mode.CENTER);
         randomTilingButton.setSelected(setting.getTilingMode()==TilingSetting.Mode.RANDOM);
         runtimeTilingButton.setSelected(setting.getTilingMode()==TilingSetting.Mode.RUNTIME);
         fileTilingButton.setSelected(setting.getTilingMode()==TilingSetting.Mode.FILE);*/
        clusterCheckBox.setSelected(setting.isCluster());
        clusterCheckBox.setEnabled(currentAcqSetting.getTilingMode() != TilingSetting.Mode.FULL);
        clusterXField.setText(Integer.toString(setting.getNrClusterX()));
        clusterXField.setEnabled(setting.isCluster() & clusterCheckBox.isEnabled());
        clusterYField.setText(Integer.toString(setting.getNrClusterY()));
        clusterYField.setEnabled(setting.isCluster() & clusterCheckBox.isEnabled());
        clusterLabel1.setEnabled(setting.isCluster() & clusterCheckBox.isEnabled());
        clusterLabel2.setEnabled(setting.isCluster() & clusterCheckBox.isEnabled());
        siteOverlapCheckBox.setSelected(setting.isSiteOverlap());
//        clusterOverlapCheckBox.setEnabled(setting.isCluster() & clusterCheckBox.isEnabled());
        siteOverlapCheckBox.setEnabled(currentAcqSetting.getTilingMode() == TilingSetting.Mode.RANDOM);
        tileOverlapField.setText(Integer.toString((int) (setting.getTileOverlap() * 100)));
        maxSitesField.setText(Integer.toString(setting.getMaxSites()));
        maxSitesField.setEnabled(setting.getTilingMode() == TilingSetting.Mode.RANDOM || setting.getTilingMode() == TilingSetting.Mode.RUNTIME);
        maxSitesLabel.setEnabled(setting.getTilingMode() == TilingSetting.Mode.RANDOM || setting.getTilingMode() == TilingSetting.Mode.RUNTIME);
        insideOnlyCheckBox.setSelected(setting.isInsideOnly());
        acqOrderList.setSelectedItem(AcqSetting.ACQ_ORDER_LIST[setting.getAcqOrder()]);
        autofocusCheckBox.setSelected(setting.isAutofocus());
        zStackCheckBox.setSelected(setting.isZStack());
        timelapseCheckBox.setSelected(setting.isTimelapse());
        enableTimelapsePane(setting.isTimelapse());
        initializeChannelTable(setting);
        zStackCenteredCheckBox.setSelected(setting.isZStackCentered());
        zStackBeginField.setValue(setting.getZBegin());
        zStackEndField.setValue(setting.getZEnd());
        zStepSizeField.setValue(setting.getZStepSize());
        zSlicesSpinner.setValue(setting.getZSlices());
        enableZStackPane(setting.isZStack());
        intHourField.setText(Integer.toString(setting.getHoursInterval()));
        intMinField.setText(Integer.toString(setting.getMinutesInterval()));
        intSecField.setText(Integer.toString(setting.getSecondsInterval()));
        framesField.setText(Integer.toString(setting.getFrames()));
        calculateDuration(setting);
        //updatePixelSize(setting.getObjective());
        recalculateTiles = true;
    }

    private void updateProcessorTreeView(AcqSetting setting) {
        DefaultMutableTreeNode root=setting.getImageProcessorTree();
        DefaultTreeModel model = (DefaultTreeModel)processorTreeView.getModel();
        model.setRoot(root);
        model.reload(root);
        for (int i = 0; i < processorTreeView.getRowCount(); i++) {
            processorTreeView.expandRow(i);
        }
    }
    
    private void loadExpSettings(File file, boolean revertToDefault) {
        List<AcqSetting> settings=null;
        AcqLayout layout=null;
        if (!file.exists()) {
            IJ.log("Loading experiment settings: "+file.getAbsolutePath() + " not found, creating default settings.");
            JOptionPane.showMessageDialog(this, "Experiment setting file "+file.getAbsolutePath()+" not found.");
        } else {   
            IJ.log("Loading experiment settings: "+file.getAbsolutePath());
            try {
                BufferedReader br=new BufferedReader(new FileReader(file));
                StringBuilder sb=new StringBuilder();
                String line;
                while ((line = br.readLine()) !=null) {
                    sb.append(line);
                }
                JSONObject expSettingsObj=new JSONObject(sb.toString());
                
//                layout=new AcqLayout(expSettingsObj.getJSONObject(AcqLayout.TAG_LAYOUT),file);
                //returns null if problem with parsing of JSONObject
                layout=AcqLayout.createFromJSONObject(expSettingsObj.getJSONObject(AcqLayout.TAG_LAYOUT), file);
                settings=loadAcquisitionSettings(expSettingsObj.getJSONArray(AcqSetting.TAG_ACQ_SETTING_ARRAY));  
            } catch (FileNotFoundException ex) {    
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JSONException ex) {    
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (settings!=null || layout!=null) {
            expSettingsFile=file;
            expSettingsFileLabel.setText(expSettingsFile.getName());
            expSettingsFileLabel.setToolTipText(expSettingsFile.getAbsolutePath());
        } else if (revertToDefault) {
            expSettingsFile=new File(Prefs.getHomeDir(),"not found");
            expSettingsFileLabel.setText(expSettingsFile.getName());
            expSettingsFileLabel.setToolTipText("");
        }    
        if (layout==null) {
            if (revertToDefault) {
                IJ.log("    Layout not found. Initializing with empty layout."); 
                JOptionPane.showMessageDialog(this, "Last used layout definition could not be found or read!\n"
                        + "Creating default layout.");
//                layout=new AcqLayout(null,null);//creates emptyLayout
                layout=new AcqLayout();//creates emptyLayout
            } else {
                IJ.log("    Layout not found."); 
                JOptionPane.showMessageDialog(this, "Last used layout definition could not be found or read!");                
            }    
        } else {
            IJ.log("    Layout loaded, file: "+layout.getName()); 
        }
        if (settings==null || settings.size()==0) {
            JOptionPane.showMessageDialog(this,"Could not read acquisition settings from file '"+expSettingsFile.getName()+"'.");
            //set up a default acquisition setting
            if (revertToDefault) {
                IJ.log("    Acquisition settings could not be loaded. Initializing default acquisition settings.");
                settings = new ArrayList<AcqSetting>();
//                AcqSetting setting = new AcqSetting("Seq_1", cCameraPixX, cCameraPixY, availableObjectives.get(0), getObjPixelSize(availableObjectives.get(0)), Integer.parseInt(binningOptions[0]), false);
                FieldOfView fov=new FieldOfView(currentDetector.getFullWidth_Pixel(), currentDetector.getFullHeight_Pixel(),currentDetector.getFieldRotation());
                AcqSetting setting = new AcqSetting("Seq_1", fov, availableObjectives.get(0), getObjPixelSize(availableObjectives.get(0)),currentDetector.getBinningOption(0,1), false);
                settings.add(setting);
                expSettingsFile=new File(Prefs.getHomeDir(),"not found");
                expSettingsFileLabel.setToolTipText("");    
            } else {
                IJ.log("    Acquisition settings could not be loaded.");
            }
        }
        IJ.log("    "+settings.size()+ " acquisition sequence(s) initialized.");
        if (settings!=null) {
            acqSettings=settings;
            for (AcqSetting setting:acqSettings) {
                if (!objectiveDevStr.equals(setting.getObjectiveDevStr())) {
                    setting.setObjectiveDevStr(objectiveDevStr);
                } 
            }
            currentAcqSetting=acqSettings.get(0);

                //update AcqSetting
            initializeAcqSettingTable();
            initializeChannelTable(currentAcqSetting);
            updateProcessorTreeView(currentAcqSetting);

            ((LayoutPanel) acqLayoutPanel).setAcqSetting(currentAcqSetting, true);
            updateAcqSettingTab(currentAcqSetting);
            calculateTotalZDist(currentAcqSetting);
            calculateDuration(currentAcqSetting);
        } 
        IJ.log("    GUI updated for sequence: "+currentAcqSetting.getName());
        //set active layout
        if (layout!=null) {
            acqLayout=layout;        
            ((LayoutPanel) acqLayoutPanel).setAcquisitionLayout(acqLayout, getMaxFOV());
            layoutFileLabel.setText(acqLayout.getName());
            if (acqLayout.isEmpty()) {
                layoutFileLabel.setToolTipText("");
            } else {
                layoutFileLabel.setToolTipText("LOADED FROM: "+acqLayout.getFile().getAbsolutePath());
            } 
            lastArea = null;
            initializeAreaTable();
            setLandmarkFound(false);
            Area.setStageToLayoutRot(acqLayout.getStageToLayoutRot());
            RefArea.setStageToLayoutRot(acqLayout.getStageToLayoutRot());

            acqLayoutPanel.setCursor(normCursor);
            recalculateTiles = false;
            for (AcqSetting as : acqSettings) {
                as.enableSiteOverlap(true);
                as.enableInsideOnly(false);
            }
            siteOverlapCheckBox.setSelected(currentAcqSetting.isSiteOverlap());
            insideOnlyCheckBox.setSelected(currentAcqSetting.isInsideOnly());
            if (mergeAreasDialog != null) {
                mergeAreasDialog.removeAllAreas();
            }
            updatePixelSize(currentAcqSetting.getObjective());
            recalculateTiles = true;
            calcTilePositions(null, currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), ADJUSTING_SETTINGS);
            Rectangle r = layoutScrollPane.getVisibleRect();
            ((LayoutPanel) acqLayoutPanel).calculateScale(r.width, r.height);
            areaTable.revalidate();
            areaTable.repaint();
        }
        IJ.log("    Layout initialized and tiling updated for sequence: "+currentAcqSetting.getName());
        //try to select last used objective
        String selObjective = "";
        if (availableObjectives == null) {
            objectiveComboBox.setEnabled(false);
        } else {
            for (String objStr : availableObjectives) {
                selObjective = currentAcqSetting.getObjective();
                if (selObjective.equals(objStr)) {
                    objectiveComboBox.setSelectedItem(selObjective); //this will also set selectedObjective, currentPixSize, totalTiles;
                    break;
                }
            }
            if (!selObjective.equals(objectiveComboBox.getSelectedItem().toString())) {
                JOptionPane.showMessageDialog(this, selObjective + " objective is no longer installed!");
                selObjective = objectiveComboBox.getSelectedItem().toString();
            }
        }
        updatePixelSize(selObjective); //calls updateFOVDimension, updateTileOverlap, updateTileSizeLabel 
            
        calcTilePositions(null, currentAcqSetting.getFieldOfView(), currentAcqSetting.getTilingSetting(), SELECTING_AREA);
        IJ.log("Loading of experiment settings completed.");
    }    
    
 
    private void loadAvailableObjectiveLabels() {
//        objectiveComboBox.removeAllItems();
        objectiveComboBox.setModel(new DefaultComboBoxModel(new String[]{}));
        boolean found=false;
        try {
            StrVector devices = core.getLoadedDevices();
            for (String devStr : devices) {
                if (devStr.equals(objectiveDevStr)) {
                    found=true;
                    break;
                }
            }
            if (!found) {               
                objectiveDevStr=changeDeviceStr("Objective");
            }   
            if (!objectiveDevStr.equals("")) {
                StrVector props = core.getDevicePropertyNames(objectiveDevStr);
                for (String propsStr : props) {
                    if (propsStr.equals("Label")) {
                        StrVector allowedVals = core.getAllowedPropertyValues(objectiveDevStr, propsStr);
                        availableObjectives = new ArrayList<String>((int)allowedVals.size());
                        for (int j = 0; j < allowedVals.size(); j++) {
                            availableObjectives.add(allowedVals.get(j));
//                            objectiveComboBox.addItem(allowedVals.get(j));
                        }
                        objectiveComboBox.setModel(new DefaultComboBoxModel(allowedVals.toArray()));
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        String obj="";
        for (int i=0; i<objectiveComboBox.getItemCount(); i++)
            obj=obj+(String)objectiveComboBox.getItemAt(i)+", ";         
        IJ.log("Objectives found: objectiveDevStr="+objectiveDevStr+"; "+obj);
    }

    private double getMaxFOV() { //searches for largest pixel size calibration and multiplies it with horiz and vert detector pixel # (for objective with lowest mag)
        double maxFOV = -1;
        if (currentDetector != null && currentDetector.getFullWidth_Pixel() > 0 && currentDetector.getFullHeight_Pixel() > 0) {
            for (String objLabel:availableObjectives) {
                double ps = getObjPixelSize(objLabel);
                if (ps > 0) {
                    maxFOV = Math.max(Math.max(ps * currentDetector.getFullWidth_Pixel(), ps * currentDetector.getFullHeight_Pixel()), maxFOV);
                }
            }
        }
        return maxFOV;
    }

    private void updatePixelSize(String objectiveLabel) {  //updates currentPixSize, FOV (tileSize) and their GUI labels
        if (currentAcqSetting != null) {
            currentAcqSetting.setObjective(objectiveLabel, getObjPixelSize(objectiveLabel));
            double fovWidth = currentAcqSetting.getTileWidth_UM();
            double fovHeight = currentAcqSetting.getTileHeight_UM();
            double pixSize = currentAcqSetting.getImagePixelSize();//considers binning
            if ((fovWidth < 0) || (fovHeight < 0)) {
                tileSizeLabel.setText(NOT_CALIBRATED);
            } else {
                tileSizeLabel.setText(formatNumber("0.00", fovWidth) + "um x " + formatNumber("0.00", fovHeight) + "um");
            }
            if (pixSize > 0) {
                pixelSizeLabel.setText(Double.toString(pixSize) + " um");
            } else {
                pixelSizeLabel.setText(NOT_CALIBRATED);
            }
        } else {
            IJ.log("AcqFrame.updatePixelSize: currentAcqSetting=null");
        }
    }

    private String formatNumber(String pattern, double num) {
        DecimalFormat formatter = new DecimalFormat(pattern);
        return formatter.format(num);
    }

    private void calculateDuration(AcqSetting setting) {
        if (setting != null) {
            //intervalInSeconds = intHours*3600+intMinutes*60+intSeconds;
            int frames = setting.getFrames();
            long totalSeconds = (frames - 1) * setting.getIntervalInMilliS() / 1000;
            if (totalSeconds > 0) {
                String dStr, hStr, minStr, secStr;
                double d = Math.floor(totalSeconds / (3600 * 24));
                dStr = formatNumber("", d);
                totalSeconds = (long) (totalSeconds % (3600 * 24));
                double h = Math.floor(totalSeconds / 3600);
                hStr = formatNumber("00", h);
                totalSeconds = (long) (totalSeconds % 3600);
                double m = Math.floor(totalSeconds / 60);
                minStr = formatNumber("00", m);
                totalSeconds = (long) (totalSeconds % 60);
                secStr = formatNumber("00", (double) totalSeconds);
                if (frames != 1) {
                    durationText.setText(dStr + "d " + hStr + "h " + minStr + "m " + secStr + "s");
                } else {
                    durationText.setText("No Time-lapse acquisition.");
                }
            } else if (frames != 1) {
                durationText.setText("<html>Unknown. " + Integer.toString(frames) + " timepoints will be </br>acquired without time delay.</html>");
            } else {
                durationText.setText("No Time-lapse acquisition");
            }
        }
    }

    private void enableZStackPane(boolean enable) {
        if ((zStackCheckBox.isSelected() && enable) | !enable) {
            acqModePane.setEnabledAt(1, enable);
            Component[] comps = acqModePane.getComponents();
            if (comps[1] instanceof Container) {
                Container componentAsContainer = (Container) comps[1];
                for (Component c : componentAsContainer.getComponents()) {
                    c.setEnabled(enable);
                }
            }
            if (zStackCenteredCheckBox.isSelected()) {
                zStackBeginField.setEnabled(false);
                zStackEndField.setEnabled(false);
            }
        }
    }

    private void enableTimelapsePane(boolean enable) {
        if ((timelapseCheckBox.isSelected() && enable) | !enable) {
            acqModePane.setEnabledAt(2, enable);
            Component[] comps = acqModePane.getComponents();
            if (comps[2] instanceof Container) {
                Container componentAsContainer = (Container) comps[2];
                for (Component c : componentAsContainer.getComponents()) {
                    c.setEnabled(enable);
                }
            }
        }
    }

    private void calculateTotalZDist(AcqSetting setting) {
        double zTotalDist;
        DecimalFormat df = new DecimalFormat("0.00");
        if (zStackCenteredCheckBox.isSelected()) {
            zTotalDist = setting.getZStepSize() * (setting.getZSlices() - 1);
            setting.setZBegin(-zTotalDist / 2);
            setting.setZEnd(zTotalDist / 2);
            zStackBeginField.setValue(setting.getZBegin());
            zStackEndField.setValue(setting.getZEnd());
        } else {
            zTotalDist = Math.abs(setting.getZEnd() - setting.getZBegin());
        }
        zStackTotalDistLabel.setText(df.format(zTotalDist)+" um");
    }


    boolean isInChannelList(String s, List<String> list) {
        boolean b = false;
        if (list != null) //            for (int i=0; i<list.size(); i++) {
        {
            for (String listStr : list) {
//               IJ.log("checking: "+s+" vs "+listStr);
                if (s.toLowerCase().equals(listStr.toLowerCase())) {
                    b = true;
                }
            }
        }
        return b;
    }

    private boolean initializeChannelTable(AcqSetting setting) {
        boolean error = false;
//        int count=0;
        
        setting.setChannelGroupStr(MMCoreUtils.loadAvailableChannelConfigs(this,setting.getChannelGroupStr(),core));

        ChannelTableModel model = (ChannelTableModel) channelTable.getModel();
        for (int i = setting.getChannels().size() - 1; i >= 0; i--) {
            if (!isInChannelList(setting.getChannels().get(i).getName(), MMCoreUtils.availableChannelList)) {
                JOptionPane.showMessageDialog(this, "'" + setting.getChannels().get(i).getName() + "' is not available in configuration '"+setting.getChannelGroupStr()+"'.");
                setting.getChannels().remove(i);
                error = true;
            }
        }
        channelTable.setModel(new ChannelTableModel(setting.getChannels()));
        TableColumn channelColumn = channelTable.getColumnModel().getColumn(0);
        JComboBox comboBox = new JComboBox();
        for (String chStr : MMCoreUtils.availableChannelList) {
            comboBox.addItem(chStr);
//            IJ.log("availableChannel: "+chStr);
        }
        channelColumn.setCellEditor(new DefaultCellEditor(comboBox));                    
        channelColumn.setHeaderValue(setting.getChannelGroupStr());  

        TableColumn colorColumn = channelTable.getColumnModel().getColumn(3);
        colorColumn.setCellEditor(new ColorEditor());
        colorColumn.setCellRenderer(new ColorRenderer(true));
//        IJ.log("AcqFrame.initializeChannelTable: finished");
        channelTable.getModel().addTableModelListener(this);
        return !error;
    }

    private String changeDeviceStr(String device) {
        StrVector configs = core.getLoadedDevices();
        String[] options = new String[(int) configs.size()];
        for (int i = 0; i < configs.size(); i++) {
            options[i] = configs.get(i);
        }
        String s = (String) JOptionPane.showInputDialog(
                    this,
                    "Select property group for "+device+":",
                    "Property Group",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    configs.get(0));
        if ((s != null) && (s.length() > 0)) { 
            return s;
        } else {
            return null;
        }
    } 
    
    private String changeConfigGroupStr(String groupName, String groupStr) {
        StrVector configs = core.getAvailableConfigGroups();
        String[] options = new String[(int) configs.size()];
        for (int i = 0; i < configs.size(); i++) {
            options[i] = configs.get(i);
        }
        String s = (String) JOptionPane.showInputDialog(
                    this,
                    "Select "+groupName+" configuration group:",
                    "Configuration Group",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    configs.get(0));
        if ((s != null) && (s.length() > 0)) {
            groupStr = s;
            configs = core.getAvailableConfigs(groupStr);
            if ((configs == null) || configs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No configurations found for "+groupStr+".\n\nPresets need to be defined for this group!");
            }
            return groupStr;
        } else {
            return null;
        }
    } 

    

    private double getObjPixelSize(String objectiveLabel) {
        double pSize = -1;
        StrVector resolutionName = core.getAvailablePixelSizeConfigs();
        Configuration pConfig;
        int startIndex, endIndex;
        if (resolutionName != null) {
//          for (int i=0; i<resolutionName.size(); i++) {
            for (String resolutionStr : resolutionName) {
                try {
                    pConfig = core.getPixelSizeConfigData(resolutionStr);
                    String s = pConfig.getVerbose();
                    //IJ.log("Pixel Config: "+s);
                    startIndex = s.indexOf("=");
                    s = s.substring(startIndex);
                    endIndex = s.indexOf("<br>");
                    s = s.substring(1, endIndex);
                    //IJ.log(s);
                    if (s.equals(objectiveLabel)) {
                        pSize = core.getPixelSizeUmByID(resolutionStr);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
//      IJ.log("AcqFrame.getPixelSize(): pSize="+Double.toString(pSize*currentBinning));
        return pSize; //pSize * currentBinning
    }

    private void initializeAcqSettingTable() {
        acqSettingTable.setModel(new AcqSettingTableModel(acqSettings));
//        ((AcqSettingTableModel)acqSettingTable.getModel()).setData(acqSettings);
//        TableColumn colorColumn = acqSettingTable.getColumnModel().getColumn(1);
//        colorColumn.setCellEditor(new StartTimeEditor());
//        cAcqSettingIdx = 0;
        currentAcqSetting = acqSettings.get(0);
        acqSettingTable.getModel().addTableModelListener(this);
        ListSelectionModel selectionModel = acqSettingTable.getSelectionModel();
        selectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                acqSettingSelectionChanged(e);
            }
        });
    }

    private void initializeAreaTable() {
//      IJ.log("AcqFrame.initializeAreaTable");  
//      AreaTableModel model = (AreaTableModel)areaTable.getModel();
        if (acqLayout != null) {
            /*ArrayList<Area> areas = acqLayout.getAreaArray();
             Object[][] data;
             if (areas.size() > 0) {
             data = new Object[areas.size()][model.getColumnCount()+1];
             for (int i=0; i<areas.size(); i++){
             data[i]=new Object[]{areas.get(i).isSelected(), areas.get(i).getName(),new Integer(0), areas.get(i).getAnnotation(), areas.get(i).getId()};
             IJ.log("AcqFrame.imitializeAreaTable: setting Area "+Integer.toString(i));
             }
             //                model.setData(data);
             //              areaTable.setModel(new AreaTableModel(data));
             //              areaTable.getModel().addTableModelListener(this);
             } else {
             data = new Object[0][model.getColumnCount()+1];
             //                model.setData(data);
             }*/
            areaTable.setModel(new AreaTableModel(acqLayout.getAreaArray()));
//            areaTable.getModel().addTableModelListener(this);

            /*            ArrayList<Area> al = acqLayout.getAreaArray();
             //DefaultListModel lm = (DefaultListModel)areaListBox.getModel();
             for (int i=0; i<al.size(); i++){
             boolean selectedForAcq=al.get(i).isSelected();
             if (i>=model.getRowCount()) {
             model.addRow(new Object[]{selectedForAcq, al.get(i).getName(),new Integer(0), al.get(i).getAnnotation()});
             } else {
             areaTable.setValueAt(selectedForAcq,i,0);
             areaTable.setValueAt(al.get(i).getName(),i,1);
             }
             }*/
        } else {
            //Object[][] data = new Object[0][model.getColumnCount()+1];
            areaTable.setModel(new AreaTableModel(null));
//             model.setData(null);
        }
        areaTable.getModel().addTableModelListener(this);
        areaTable.getColumnModel().getColumn(0).setMinWidth(20);
        areaTable.getColumnModel().getColumn(0).setMaxWidth(20);
        areaTable.getColumnModel().getColumn(2).setMaxWidth(200);
        areaTable.getColumnModel().getColumn(2).setMinWidth(35);
//       IJ.log("finished initilizeAreaTable");
    }

    //fovRotation: angle rad rotation of camera field of view relative to stage
    //if fovRotation == null or currentDetector ==null --> set fovRotation to FieldOfView.ROTATION_UNKNOWN
    private void getCameraSpecs(Double fovRotation) {
        IJ.log("Loading camera specification.");
        try {
            String cCameraLabel = core.getCameraDevice();
            StrVector props = core.getDevicePropertyNames(cCameraLabel);
            int currentBinning=1;
            int cCameraPixX=-1;
            int cCameraPixY=-1;
            int bitDepth=-1;
            String[] binningOptions= new String[] {"1"};
//            for (int i=0; i<props.size(); i++) {
            for (String propsStr : props) {
                StrVector allowedVals = core.getAllowedPropertyValues(cCameraLabel, propsStr);
                if (propsStr.equals("OnCameraCCDXSize")) {
                    cCameraPixX = Integer.parseInt(core.getProperty(cCameraLabel, propsStr));
                }
                if (propsStr.equals("OnCameraCCDYSize")) {
                    cCameraPixY = Integer.parseInt(core.getProperty(cCameraLabel, propsStr));
                }
                if (propsStr.equals("Binning")) {
                    currentBinning = Integer.parseInt(core.getProperty(cCameraLabel, propsStr));
                    binningOptions=allowedVals.toArray();
                    binningComboBox.setModel(new DefaultComboBoxModel(binningOptions));
                }
                if (propsStr.equals("BitDepth")) {
                    bitDepth = Integer.parseInt(core.getProperty(cCameraLabel, propsStr));
                }
            }
            if (cCameraPixX == -1 || cCameraPixY == -1) {//can't read OnCameraCCD chip size
                core.clearROI();
                core.snapImage();
                Object img=core.getImage();
                bitDepth = (int)core.getBytesPerPixel();                
                if (cCameraPixX==-1 || cCameraPixY==-1) {
                    cCameraPixX = currentBinning * (int) core.getImageWidth();
                    cCameraPixY = currentBinning * (int) core.getImageHeight();
                }
            }            
            if (fovRotation == null || currentDetector==null) {
                currentDetector=new Detector(cCameraLabel, cCameraPixX, cCameraPixY, bitDepth, binningOptions, FieldOfView.ROTATION_UNKNOWN);
                Area.setCameraRot(FieldOfView.ROTATION_UNKNOWN);
                RefArea.setCameraRot(FieldOfView.ROTATION_UNKNOWN);
            } else {
                currentDetector=new Detector(cCameraLabel, cCameraPixX, cCameraPixY, bitDepth, binningOptions, currentDetector.getFieldRotation());                
            }
//            IJ.log("cameraLabel: "+cCameraLabel);
//            IJ.log("cameraPixelX: "+Integer.toString(cCameraPixX));
//            IJ.log("cameraPixelY: "+Integer.toString(cCameraPixY));
//            IJ.log("binning: "+Integer.toString(currentBinning));
        } catch (Exception ex) {
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Problem loading camera specification!"+ex.getMessage());
        }
        IJ.log("Camera specification loaded: "+currentDetector.toString());
    }
/*
    private void calcAndUpdateTotalTileNumber() {
        long totalTiles = 0;
        if (!acqLayout.isEmpty()) {
            if (currentAcqSetting.getPixelSize() > 0) {
                List<Area> areas = acqLayout.getAreaArray();
                for (Area a : areas) {
                    if (a.isSelectedForAcq())
                        totalTiles += a.getTileNumber();;
                    AreaTableModel atm = (AreaTableModel) areaTable.getModel();
                    for (int j = 0; j < areaTable.getRowCount(); j++) {
                        atm.updateTileCell(j);
                    }
                }
            } else {
                totalTiles = -1;
            }
        } else {
            totalTiles = -1;
        }
    }
*/
    
    /*
     private void drawTileGridForAllSelectedAreas() {
     if (currentPixSize > 0) {
     //            totalTiles=acqLayout.drawTileGridForAllSelectedAreas(areaTable);
     totalTiles=acqLayout.calcTileNumberForAllSelectedAreas();
     //need to update tile number for each area in areaTable;
            
     totalTilesLabel.setText(Integer.toString(totalTiles));
     } else {
     totalTiles=-1;
     totalTilesLabel.setText(NOT_CALIBRATED);
     for (int i=0; i<acqLayout.getAreaArray().size(); i++)
     areaTable.setValueAt(0,i, 2);
     }
        
     }
     */
    private void updateAcqLayoutPanel() {
        if (acqLayout != null) {
            //           calcAndUpdateSelAndTotalTileNumber();
            totalAreasLabel.setText(Integer.toString(acqLayout.getNoOfSelectedAreas()));
            totalTilesLabel.setText(Long.toString(currentAcqSetting.getTotalTiles()));
            layoutScrollPane.getViewport().revalidate();
            layoutScrollPane.getViewport().repaint();

        }
    }

    private boolean isZStageInstalled() {
        String focusDeviceStr = core.getFocusDevice();
//        IJ.log(focusDeviceStr);
        return !focusDeviceStr.equals("");
    }

    private ImageProcessor snapChannelImage(String ch, double exp, double zOffs) {
        ImageProcessor ip = null;
        try {
            if (zOffs!=0) {
                core.setRelativePosition(zStageLabel, zOffs);
                core.waitForDevice(zStageLabel);
            }
            ip=MMCoreUtils.snapImage(core, currentAcqSetting.getChannelGroupStr(), ch, exp);
//            core.setConfig(channelGroupStr, ch);
/*            core.setConfig(currentAcqSetting.getChannelGroupStr(), ch);
            core.setExposure(exp);
            core.waitForSystem();
            core.snapImage();
            Object imgArray=core.getImage();
            int w = (int)core.getImageWidth();
            int h = (int)core.getImageHeight();
            switch ((int)core.getBytesPerPixel()) {
                case 1: {
                    // 8-bit grayscale pixels
                    byte[] img = (byte[]) imgArray;
                    ByteProcessor bp = new ByteProcessor(w, h, img, null);
                    ip = bp;
                    break;
                } 
                case 2: {
                    // 16-bit grayscale pixels
                    short[] img = (short[]) imgArray;
                    ShortProcessor sp = new ShortProcessor(w, h, img, null);
                    ip = sp;
                    break;
                } 
                case 4: {
                    // color pixels
                    int type=ImagePlus.COLOR_RGB;
                    if (imgArray instanceof byte[]) {
                        //convert byte[] to int[] 
                        byte[] byteArray=(byte[])imgArray;
                        int[] intArray = new int[byteArray.length/4];
                        for (int i=0; i<intArray.length; ++i) {
                            intArray[i] =  byteArray[4*i]
                  	                 + (byteArray[4*i + 1] << 8)
                  	                 + (byteArray[4*i + 2] << 16);
                  	}
	                imgArray = intArray;
	            }
	            ip=new ColorProcessor(w, h, (int[]) imgArray);
                    IJ.log(this.getClass().getName()+": "+Long.toString(core.getBytesPerPixel())+" bytes/pixel)");        
                    break;
                }
                default: {
                    ip=null;
                    IJ.log(this.getClass().getName()+": Unknown image type ("+Long.toString(core.getBytesPerPixel())+" bytes/pixel)");        
                    break;
                }
            }*/
            if (zOffs!=0) {
                core.setRelativePosition(zStageLabel, -zOffs);
                core.waitForDevice(zStageLabel);
            }
        } catch (Exception ex) {
            IJ.log("AcqFrame.snapAndDisplayImage: Exception - "+ex.getMessage()+".");
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            ip = null;
        }
        return ip;
    }

    private void moveToAbsoluteXYPos(double x, double y) {
        try {
//            if (!core.deviceBusy(core.getXYStageDevice()))
            String xyStage = core.getXYStageDevice();
            core.waitForDevice(xyStage);
            core.setXYPosition(xyStage, x, y);
        } catch (Exception e) {
        }
    }


    private void moveToLayoutPos(double lx, double ly) {
//        RefArea rp = acqLayout.getLandmark(0);
        Vec3d normVec = acqLayout.getNormalVector();
        if (acqLayout.getNoOfMappedStagePos() > 0) {
//            AcqSetting setting=acqSettings.get(cAcqSettingIdx);
            Area a = acqLayout.getFirstContainingArea(lx, ly, currentAcqSetting.getTileWidth_UM(), currentAcqSetting.getTileHeight_UM());
            double areaRelPosZ;
            if (a != null) {
                areaRelPosZ = a.getRelPosZ();
            } else {
                areaRelPosZ = 0;
            }
            try {
                Vec3d stage = acqLayout.convertLayoutToStagePos(lx, ly, areaRelPosZ);
                String xyStageName = core.getXYStageDevice();
//                if (!core.deviceBusy(xyStageName))
                String zStageName = core.getFocusDevice();
                core.waitForDevice(zStageName);
                core.setPosition(zStageName, acqLayout.getEscapeZPos());
                core.waitForDevice(xyStageName);
                core.setXYPosition(xyStageName, stage.x, stage.y);
                core.waitForDevice(zStageName);
                core.setPosition(zStageName, stage.z);
            } catch (Exception ex) {
                app.logError(ex);
                IJ.log("AcqFrame.moveToLayoutPos: "+ex.getMessage());
            }
        }
    }

    private void moveToAreaDefaultPos(Area area) {
        if (area != null) {
            try {
                Vec3d stage = acqLayout.convertLayoutToStagePos(area.getDefaultPos().getX(),area.getDefaultPos().getY(),area.getRelPosZ());
                String xyStageName = core.getXYStageDevice();
                String zStageName = core.getFocusDevice();
                core.waitForDevice(zStageName);
                core.setPosition(zStageName, acqLayout.getEscapeZPos());
                core.waitForDevice(xyStageName);
                core.setXYPosition(xyStageName, stage.x, stage.y);
                core.waitForDevice(xyStageName);
                core.setPosition(zStageName, stage.z);
            } catch (Exception ex) {
                app.logError(ex);
                IJ.log("AcqFrame.moveToArea: "+ex.getMessage());
            }
        } else {
        }
    }

    private void moveToLandmark(int index) {
        RefArea lm = acqLayout.getLandmark(index);
        if (lm != null) {
            try {
                String zStageName = core.getFocusDevice();
                core.waitForDevice(zStageName);
                core.setPosition(zStageName, acqLayout.getEscapeZPos());
                core.waitForDevice(core.getXYStageDevice());
                core.setXYPosition(core.getXYStageDevice(), lm.getStageCoordX(), lm.getStageCoordY());
                core.waitForDevice(zStageName);
                core.setPosition(zStageName, lm.getStageCoordZ());
            } catch (Exception ex) {
                app.logError(ex);
                IJ.log("AcqFrame.moveToLandmark: "+ex.getMessage());
            }
        } else {
        }
    }

    //starts calculation of tile positions in new thread(s)
    //if areas==null, calculates for all areas in layout 
    public void calcTilePositions(List<Area> areas, final FieldOfView fov, final TilingSetting setting, String cmd) {
        //if pixelsize is unknown (<=0), tile positions cannot be calculated
        if (currentAcqSetting.getImagePixelSize()<=0) {
            for (Area a:acqLayout.getAreaArray()) {
                a.setUnknownTileNum(true);
            }
            currentAcqSetting.setTotalTiles(-1);
            ((AcqSettingTableModel)acqSettingTable.getModel()).updateTileCell(acqSettingTable.getSelectedRow());
            return;
        }
        if (areas == null) {
            areas = acqLayout.getAreaArray();
        }
        if (!calculating && areas != null && areas.size() > 0) {
            calculating = true;
            retilingAborted = false;

            enableGUI(false);

            RejectedExecutionHandlerImpl rejectionHandler = new RejectedExecutionHandlerImpl();
            ThreadFactory threadFactory = Executors.defaultThreadFactory();
            retilingExecutor = new ThreadPoolExecutor(2, 4, 10, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(areas.size()), threadFactory, rejectionHandler);
            List<Future<Integer>> resultList = new ArrayList<Future<Integer>>();
            Thread retilingMonitorThread = new Thread(new TileCalcMonitor(retilingExecutor, resultList, processProgressBar, cmd, areas));
            retilingMonitorThread.start();

            for (Area area : areas) {
                if (area.isSelectedForAcq()) {
//                    Runnable retilingThread = new TilingThread(a,fovX, fovY, setting);
                    final Area a = area;
                    resultList.add(retilingExecutor.submit(new Callable<Integer>() { //returns number of tiles as Future<Integer>
                        @Override
                        public Integer call() {
                            try {
                                return a.calcTilePositions(currentAcqSetting.getTileManager(),
                                        fov.getRoiWidth_UM(currentAcqSetting.getObjPixelSize()), 
                                        fov.getRoiHeight_UM(currentAcqSetting.getObjPixelSize()), setting);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                                return -1;
                            }
                        }
                    }));
                }
            }
            retilingExecutor.shutdown();
        }
    }
}
