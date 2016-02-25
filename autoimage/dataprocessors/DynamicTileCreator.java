package autoimage.dataprocessors;

import autoimage.ImgUtils;
import autoimage.api.ExtImageTags;
import autoimage.api.Doxel;
import autoimage.api.IDoxelListener;

//share last directory for script files with ScriptAnalyzer
import static autoimage.dataprocessors.ScriptAnalyzer.scriptDir;

import bsh.EvalError;
import bsh.Interpreter;
import ij.IJ;
import ij.Prefs;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.api.MMTags;

    
/**
 *
 * @author Karsten Siller
 */
public class DynamicTileCreator extends ScriptAnalyzer implements IDataProcessorOption<String> {
    
    protected List<Doxel> doxelList;
    protected List<String> options_;
    protected List<String> selectedSeq;
    private final List<IDoxelListener> doxelListeners;
    private final ExecutorService listenerExecutor;

    public DynamicTileCreator () {
        this("","","",null);
    }
    
    public DynamicTileCreator (final String script, final String args, String path, IDoxelListener listener) {
        this(false,script, args, path, listener,false);
    }

    public DynamicTileCreator (boolean procIncompl,final String script, final String args, String path, IDoxelListener listener, boolean saveRT) {
        super(procIncompl,script, args, path, saveRT);
        doxelListeners=new ArrayList<IDoxelListener>();
        if (listener!=null)
            doxelListeners.add(listener);
        selectedSeq=new ArrayList<String> ();
        listenerExecutor = Executors.newFixedThreadPool(1);
        //always separate by timepoint, xy position (stage pos, cluster, area)
        criteriaKeys.add(MMTags.Image.FRAME_INDEX);
        criteriaKeys.add(MMTags.Image.POS_INDEX);
        criteriaKeys.add(ExtImageTags.CLUSTER_INDEX);
        criteriaKeys.add(ExtImageTags.AREA_INDEX);
        criteriaKeys.add(ExtImageTags.AREA_COMMENT);
    }
    
    @Override
    public JSONObject getParameters() throws JSONException {
        JSONObject obj=super.getParameters();
        JSONArray names=new JSONArray();
        for (String name:selectedSeq) {
            names.put(name);
        }
        obj.put("SettingNames", names);
        return obj;
    }
    
    @Override
    public void setParameters(JSONObject obj) throws JSONException {
        super.setParameters(obj);
        JSONArray names=obj.getJSONArray("SettingNames");
        selectedSeq=new ArrayList<String>();
        for (int i=0; i<names.length(); i++) {
            selectedSeq.add(names.getString(i));
        }
    }
    
    @Override
    public String getProcName() {
        return "Dynamic Tiling: "+new File(script_).getName()+" ["+args_+"]";
    }
    
    @Override
    public void setScriptVariables(Interpreter interpreter) throws EvalError {
        doxelList= new ArrayList<Doxel>();
        interpreter.set("doxelList",doxelList);
//        interpreter.set("workDir",workDir);
    }

    public void addDoxelListener(IDoxelListener listener) {
        doxelListeners.add(listener);
    }
    
    public void removeDoxelListener(IDoxelListener listener) {
        doxelListeners.remove(listener);
    }
    
    public void removeAllDoxelListeners() {
        doxelListeners.clear();
    }
    
    //files is list of working copies
    @Override
    protected void processResults(List<File> files) {
        IJ.log(    "    Processed Results:"+script_+".");
        try {
            JSONObject meta=ImgUtils.parseMetadataFromFile(files.get(0));
            String positionName=meta.getString(MMTags.Image.POS_NAME);
            int imgWidth=meta.getInt(MMTags.Image.WIDTH);
            int imgHeight=meta.getInt(MMTags.Image.HEIGHT);
            double zStepSize=meta.getDouble(MMTags.Image.ZUM);
            JSONObject summary=meta.getJSONObject(MMTags.Root.SUMMARY);
            double pixSize=summary.getDouble(MMTags.Summary.PIXSIZE);
            double detectorAngle=summary.getDouble(ExtImageTags.DETECTOR_ROTATION);
            //absolute x, y, z, t coordinates of first image in processed group
            double firstImgX=meta.getDouble(MMTags.Image.XUM);
            double firstImgY=meta.getDouble(MMTags.Image.YUM);
            double firstImgZ=meta.getDouble(MMTags.Image.ZUM);
            double firstElapsedTimeMS=meta.getDouble(MMTags.Image.ELAPSED_TIME_MS);
            Object firstTime=meta.get(MMTags.Image.TIME);
            
            final String area=meta.getString(ExtImageTags.AREA_NAME);
            double cosinus=Math.cos(detectorAngle);
            double sinus=Math.sin(detectorAngle);
            if (doxelList!=null && doxelList.size()>0){
                final List<Doxel> doxelList=new ArrayList<Doxel>(this.doxelList.size());
                for (final Doxel doxel:this.doxelList) {

                    IJ.log("    doxel (image): ("+doxel.toString(false));
                    
                    //1. translate to center -> dx/dy
                    double dx;//offset from image center x in um
                    double dy;//offset from image center y in um
                    double dz;//offset from from first image z in um
                    //try to convert x,y, z coordinates to micron
                    try {
                        doxel.convertToMicron();
                    } catch (IllegalArgumentException ex) {
                        
                    }
                    if (Doxel.IS_PIXEL.equals(doxel.unitX)) {
                        //if unitX is not set, doxel coordinates are in pixel
                        //--> convert pixel to micron
                        dx=pixSize*(doxel.xPos-imgWidth/2);
                    } else {
                        //roi coordinates are in micron
                        dx=doxel.xPos-pixSize*imgWidth/2;
                    }
                    if (Doxel.IS_PIXEL.equals(doxel.unitY)) {
                        //if unitY is not set, doxel coordinates are in pixel
                        //--> convert pixel to micron
                        dy=pixSize*(doxel.yPos-imgHeight/2);
                    } else {
                        //roi coordinates are in micron
                        dy=doxel.yPos-pixSize*imgHeight/2;                        
                    }
                    
                    //2. calculate z-offset [in um] relative to first image in list
                    if (Doxel.IS_PIXEL.equals(doxel.unitZ)) {
                        //if unitZ is not set, doxel coordinates are in z-step increments
                        //--> convert pixel to micron
                        dz=doxel.zPos*zStepSize;
                    } else {
                        //roi coordinates already in micron relative to first image in list
                        dz=doxel.zPos;                        
                    }
                    
                    //3. rotate to compensate for camera rotation
                    double x = (cosinus * dx) - (sinus * dy); //in um
                    double y = (sinus * dx) + (cosinus * dy); //in um
                    
                    //4. calculate doxel's absolute xy stage position and relative z position [in um]
                    doxel.xPos=firstImgX + x;
                    doxel.yPos=firstImgY + y;
                    doxel.zPos=firstImgZ + dz;
                    doxel.unitX=Doxel.IS_MICROMETER;
                    doxel.unitY=Doxel.IS_MICROMETER;
                    doxel.unitZ=Doxel.IS_MICROMETER;
                    
                    //5. add other metadata
                    doxel.time=firstElapsedTimeMS;//in millisecond
                    doxel.unitTime=Doxel.IS_MILLISEC;
                    doxel.put(ExtImageTags.AREA_NAME, area);
                    doxel.put(MMTags.Image.POS_NAME,positionName);
                    
                    doxelList.add(doxel);

                    IJ.log("    doxel (relative to center of first image): ("+ dx +"/"+ dy +"/"+ dz +") [um/um/um]");
                    IJ.log("    first image center (absolute): ("+(firstImgX)+"/"+(firstImgY)+"/"+(firstImgZ)+") [um/um/um]");
                    IJ.log("    doxel (absolute): "+doxel.toString(false));
                }
                this.doxelList.clear();
                    
                synchronized (doxelListeners) {
                    for (final IDoxelListener tm : doxelListeners) {
                        listenerExecutor.submit(new Runnable() {
                              @Override
                              public void run() {
                                tm.doxelListAdded(doxelList,this);
                              }
                           });
                    }
                }
      
                IJ.log("    "+Integer.toString(doxelList.size())+" Doxels added for area "+area);
            } else {
                IJ.log("    no Doxels created");
            }    
        } catch (JSONException ex) {
            IJ.log("    Script "+script_+"Problem parsing JSONObject.");
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    @Override
    public void makeConfigurationGUI() {
        JPanel optionPanel = new JPanel();
        BoxLayout layout = new BoxLayout(optionPanel,BoxLayout.Y_AXIS);
        optionPanel.setLayout(layout);

        JLabel l=new JLabel("Select acquisition sequences that will utilize the dynamic Tile coordinates");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.add(l);
        JCheckBox[] optCB;
        if (options_!=null) {
            optCB = new JCheckBox[options_.size()];
            int i=0;
            for (String aname:options_) {
                optCB[i] = new JCheckBox(aname);
                if (selectedSeq!=null)
                    optCB[i].setSelected(selectedSeq.contains(aname));
                optionPanel.add(optCB[i]);
                i++;
            }
        } else {
            optCB = new JCheckBox[0];
        }
        
        
        l=new JLabel("Separate Images for Analysis by:");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.add(l);
        
        JPanel cbPanel=new JPanel();
        cbPanel.setLayout(new GridLayout(0,2));
        JCheckBox channelCB=new JCheckBox("Channels");
        cbPanel.add(channelCB);
        JCheckBox positionsCB=new JCheckBox("XY-Positions");
        cbPanel.add(positionsCB);
        JCheckBox slicesCB=new JCheckBox("Z-Positions");
        cbPanel.add(slicesCB);
        JCheckBox clustersCB=new JCheckBox("Clusters/Area");
        cbPanel.add(clustersCB);
        JCheckBox framesCB=new JCheckBox("Timepoints");
        cbPanel.add(framesCB);
        JCheckBox areasCB=new JCheckBox("Areas");
        cbPanel.add(areasCB);
        JCheckBox commentsCB=new JCheckBox("Comments");
        cbPanel.add(commentsCB);
        cbPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        optionPanel.add(cbPanel);
        Component filler=Box.createRigidArea(new Dimension (10,10));
        optionPanel.add(filler);
        
        channelCB.setSelected(criteriaKeys.contains(MMTags.Image.CHANNEL) || criteriaKeys.contains(MMTags.Image.CHANNEL_INDEX));
        slicesCB.setSelected(criteriaKeys.contains(MMTags.Image.SLICE) || criteriaKeys.contains(MMTags.Image.SLICE_INDEX));
        framesCB.setSelected(true);//criteriaKeys.contains(MMTags.Image.FRAME) || criteriaKeys.contains(MMTags.Image.FRAME_INDEX));
        positionsCB.setSelected(true);//(criteriaKeys.contains(MMTags.Image.POS_NAME) || criteriaKeys.contains(MMTags.Image.POS_INDEX));
        clustersCB.setSelected(true);//(criteriaKeys.contains(ExtImageTags.CLUSTER_INDEX));
        areasCB.setSelected(true);//(criteriaKeys.contains(ExtImageTags.AREA_NAME) || criteriaKeys.contains(ExtImageTags.AREA_INDEX));
        commentsCB.setSelected(true);//(criteriaKeys.contains(ExtImageTags.AREA_COMMENT));
        
        framesCB.setEnabled(false);
        positionsCB.setEnabled(false);
        clustersCB.setEnabled(false);
        areasCB.setEnabled(false);
        commentsCB.setEnabled(false);
        
        l=new JLabel("Script File:");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.add(l);
        JPanel filePanel = new JPanel();
        BoxLayout flayout = new BoxLayout(filePanel,BoxLayout.X_AXIS);
        final JFormattedTextField scriptField = new JFormattedTextField();
        scriptField.setColumns(30);
        scriptField.setValue(script_);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        filePanel.add(scriptField);
        JButton fileButton=new JButton("Browse");
        filePanel.add(fileButton);
        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                JFileChooser fc=new JFileChooser();
                File scriptFile=new File(scriptField.getText()).getParentFile();
                if (scriptFile != null)
                    fc.setCurrentDirectory(scriptFile);
                else {
                    if (scriptDir.equals("")) {
                        fc.setCurrentDirectory(new File(Prefs.getImageJDir()));
                    } else {
                        fc.setCurrentDirectory(new File(scriptDir));
                    }
                }    
                int result=fc.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    scriptField.setValue(fc.getSelectedFile().getAbsolutePath());
                    scriptDir=fc.getCurrentDirectory().getAbsolutePath();
                }    
            }
        });
        filePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.add(filePanel);
        l=new JLabel("Script Arguments:");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.add(l);
        JTextArea argField = new JTextArea();
        argField.setColumns(30);
        argField.setRows(10);
        argField.setLineWrap( true );
        argField.setWrapStyleWord( true );
        argField.setSize(argField.getPreferredSize().width, 1);
        argField.setAlignmentX(Component.LEFT_ALIGNMENT);
        argField.setText(args_.replace(" ","\n"));
        optionPanel.add(argField);
            
        JCheckBox saveCB = new JCheckBox("Save Numeric Results");
        saveCB.setSelected(saveRT_);
        optionPanel.add(saveCB);
        JCheckBox procCB = new JCheckBox("Process On-the-Fly"); 
        procCB.setSelected(processOnTheFly);
        optionPanel.add(procCB);
        
        int result=-100;
        File scriptfile=null;
        do {    
            if (result!=-100) {
                JOptionPane.showMessageDialog(null,"Script file "+scriptfile.getAbsolutePath()+"not found.");
            }
            result = JOptionPane.showConfirmDialog(null, optionPanel, 
                this.getClass().getName(), JOptionPane.OK_CANCEL_OPTION);
            scriptfile=new File(scriptField.getText());
        } while (result == JOptionPane.OK_OPTION && !scriptfile.exists());
        if (result == JOptionPane.OK_OPTION) {
            selectedSeq.clear();
            for (int i=0; i<optCB.length; i++) {
                if (optCB[i].isSelected()) {
                    selectedSeq.add(optCB[i].getText());
                }    
            }            
            criteriaKeys=new ArrayList<String>();
            if (channelCB.isSelected()) {
                criteriaKeys.add(MMTags.Image.CHANNEL_INDEX);
            }
            if (slicesCB.isSelected()) {
                criteriaKeys.add(MMTags.Image.SLICE_INDEX);
            }
            if (framesCB.isSelected()) {
                criteriaKeys.add(MMTags.Image.FRAME_INDEX);
            }
            if (positionsCB.isSelected()) {
                criteriaKeys.add(MMTags.Image.POS_INDEX);
            }
            if (clustersCB.isSelected()) {
                criteriaKeys.add(ExtImageTags.CLUSTER_INDEX);
                criteriaKeys.add(ExtImageTags.AREA_INDEX);
            }
            if (areasCB.isSelected()) {
                criteriaKeys.add(ExtImageTags.AREA_INDEX);
            }
            if (commentsCB.isSelected()) {
                criteriaKeys.add(ExtImageTags.AREA_COMMENT);
            }
            script_=scriptField.getText();
            args_=argField.getText().replaceAll("\n", " ");
            saveRT_=saveCB.isSelected();
            processOnTheFly=procCB.isSelected();
        }
    }

    @Override
    public void setOptions(List<String> options) {
        options_=options;
    }

    @Override
    public List<String> getOptions() {
        return options_;
    }
    
    public List<String> getSelSeqNames() {
        return selectedSeq;
    }
}
