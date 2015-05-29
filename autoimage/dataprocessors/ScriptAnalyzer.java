package autoimage.dataprocessors;

import autoimage.ExtImageTags;
import autoimage.ImageFileQueue;
import autoimage.Utils;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.TargetError;
import ij.IJ;
import ij.Prefs;
import ij.measure.ResultsTable;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.api.MMTags;


    
/**
 * Executes script calls for processing of image groups.
 * Processing parameters can be passed on as arguments to interpreter. 
 * Arguments are defined as String in GUI.
 * 
 * @author Karsten Siller
 */
public class ScriptAnalyzer extends GroupProcessor<File>  {
    
    protected String script_;
    protected String args_;
    protected boolean saveRT_;
    protected ResultsTable rTable_; //new table for each image
    protected static String scriptDir = "";
//    protected TaggedImageStorageDiskDefault storage;
    
    
    public ScriptAnalyzer() {
        this(false,"","","",false);
    }
    
    public ScriptAnalyzer(boolean procIncomplete, final String script, final String args, final String path) {
        this(procIncomplete,script,args,path, false);
    }
    
    public ScriptAnalyzer(boolean procIncomplete, String script, String args, String path, boolean saveRT) {
        super("ScriptAnalyzer: "+new File(script).getName()+" ["+args+"]", path,null,procIncomplete);
        script_=script;
        args_=args;
        saveRT_=saveRT;
        rTable_=null;
        
        processIncompleteGrps=true;
        processOnTheFly=true;
    }
/*    
    @Override
    protected void initialize() {
        storage=null;
    }
*/    
    @Override
    public boolean isSupportedDataType(Class<?> clazz) {
        return clazz==java.io.File.class;
    }
    
    @Override
    public JSONObject getParameters() throws JSONException {
        JSONObject obj=super.getParameters();
        obj.put("Script", script_);
        obj.put("Args", args_);
        obj.put("SaveResultTable", saveRT_);
        return obj;
    }
    
    @Override
    public void setParameters(JSONObject obj) throws JSONException {
        super.setParameters(obj);
        script_=obj.getString("Script");
        args_=obj.getString("Args");
        saveRT_=obj.getBoolean("SaveResultTable");
    }

    @Override
    public String  getProcName() {
       return "Script Analyzer: "+new File(script_).getName();//+" ["+args_+"]";
    }
    
    @Override
    protected boolean acceptElement(File f) {
       return !ImageFileQueue.isPoison(f);
    }
    

    public void setScriptVariables(Interpreter interpreter) throws EvalError {
    }
    
    public void getScriptVariables(Interpreter interpreter) throws EvalError {
    }
    
    public boolean isSaveRT() {
        return saveRT_;
    }
    
    public void enableSaveRT(boolean b) {
        saveRT_=b;
    }
    
    public String getScript() {
        return script_;
    }
    
    public void setScript(String script) {
        script_=script;
    }
    
    public String getArgs() {
        return args_;
    }
    
    public void setArgs(String args) {
        args_=args;
    }
    
    /*
    //creates copy of element
    //if meta!=null, metadata in copied element will be replaced by meta, otherwise keep original metadata
    @Override
    protected File createCopy(File element) {
        JSONObject meta=null;
        File copy=null;
        TaggedImage ti=null;
        ImagePlus imp=null; 
        imp=IJ.openImage(((File)element).getAbsolutePath());
        if (imp!=null && imp.getProperty("Info") != null) {
            try {
                meta = new JSONObject((String)imp.getProperty("Info"));
                String newDir=new File(workDir).getParentFile().getAbsolutePath();
                ImageProcessor ip=imp.getProcessor();
                if (meta.getJSONObject(MMTags.Root.SUMMARY).getString(MMTags.Summary.PIX_TYPE).equals("RGB32")) {
                    //RGB32 elements hold pixel data in int[] --> convert to byte[]
                    ti=new TaggedImage(MMCoreUtils.convertIntToByteArray((int[])ip.getPixels()),new JSONObject(meta.toString()));
                }
                else if (meta.getJSONObject(MMTags.Root.SUMMARY).getString(MMTags.Summary.PIX_TYPE).equals("RGB64")) {
                    if (imp.isComposite()) {
                        CompositeImage ci=(CompositeImage)imp;
                        short[] totalArray=new short[ci.getWidth()*ci.getHeight()*4];
                        for (int channel=0; channel< ci.getNChannels(); channel++) {
                            ImageProcessor proc=imp.getStack().getProcessor(channel+1);
                            short[] chPixels=(short[])proc.getPixels();
                            for (int i=0;i<chPixels.length;i++) {
                                totalArray[(2-channel) + 4*i] = chPixels[i]; // B,G,R
                            }
                        }
                        ti=new TaggedImage(totalArray,new JSONObject(meta.toString()));
                    }

                } else {//8-bit or 16-bit grayscale
                    ti=ImageUtils.makeTaggedImage(ip);
                    ti.tags=new JSONObject(meta.toString());                        
                }
                String newPrefix=new File(workDir).getName();
                //update metadata
                ti.tags=updateTagValue(ti.tags, newDir, newPrefix, true);
                if (storage==null) {
                    storage = new TaggedImageStorageDiskDefault (workDir,true,ti.tags.getJSONObject(MMTags.Root.SUMMARY));
                }
                storage.putImage(ti);

                String posName="";
                File copiedFile=new File(new File(new File(newDir,newPrefix),meta.getString("PositionName")),
                                                ti.tags.getString("FileName"));
                copy=copiedFile;
            } catch (JSONException ex) {
                IJ.log(this.getClass().getName()+ ": Cannot retrieve 'Info' metadata from file. "+ex);
                Logger.getLogger(FilterProcessor.class.getName()).log(Level.SEVERE, null, ex);
//                    copy=super.createCopy(element);
            } catch (Exception ex) {
                IJ.log(this.getClass().getName()+ ": Error writing file to storage. "+ex);
                Logger.getLogger(FilterProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            IJ.log(this.getClass().getName()+": Cannot open image");
//                copy=super.createCopy(element);
        }
       return copy;
    }    
*/
    protected boolean saveResultsTable(List<File> modFiles) {
        if (rTable_==null) {
            return false;
        }
        File rtFile=null;
        try {
            JSONObject meta=Utils.parseMetadata(modFiles.get(0));
            String area = meta.getString(ExtImageTags.AREA_NAME);
            String cluster=Long.toString(meta.getLong(ExtImageTags.CLUSTER_INDEX));
            String site=Long.toString(meta.getLong(ExtImageTags.SITE_INDEX));
            String channel=meta.getString(MMTags.Image.CHANNEL_NAME);
            String cFrame=Long.toString(meta.getLong(MMTags.Image.FRAME_INDEX));
            String cSlice=Long.toString(meta.getLong(MMTags.Image.SLICE_INDEX));
            IJ.log("cframe: "+cFrame);
            IJ.log("cSlice: "+cSlice);
/*            File path;
            path=new File(workDir,area+"-Cluster"+cluster+"-Site"+site);
            try {
                if (!path.exists()) {
                    path.mkdirs(); 
                }
            } catch (Exception e) {
                IJ.log("  Cannot create directory");
            }    
*/
            String resultName="Results-";
            if (criteriaKeys.contains(ExtImageTags.AREA_COMMENT)) {
                resultName+=meta.getString(ExtImageTags.AREA_COMMENT);
            }
            if (criteriaKeys.contains(MMTags.Image.POS_INDEX)) {
                resultName+="-"+area+"-Cluster"+cluster+"-Site"+site;
            } else {
                if (criteriaKeys.contains(ExtImageTags.CLUSTER_INDEX)) {
                    resultName+="-"+area+"-Cluster"+cluster;
                } else {
                    if (criteriaKeys.contains(ExtImageTags.AREA_INDEX)) {
                        resultName+="-"+area;
                    }
                }
            }
            if (criteriaKeys.contains(MMTags.Image.FRAME_INDEX)) {
                resultName+="-Frame"+cFrame;
            }
            if (criteriaKeys.contains(MMTags.Image.CHANNEL_INDEX)) {
                resultName+="-"+channel;
            }
            if (criteriaKeys.contains(MMTags.Image.SLICE_INDEX)) {
                resultName+="-Slice"+cSlice;
            }
            //remove double "--";
            resultName=resultName.replaceAll("--", "-");
            resultName+=".txt";
            String scriptName=new File(script_).getName();
            IJ.log("  script: "+scriptName);
            rtFile=new File(workDir,resultName);
            IJ.log("    saving RT: "+rtFile.getAbsolutePath());
            rTable_.saveAs(rtFile.getAbsolutePath());
            return true;
        } catch (JSONException ex) {
            IJ.log("    saving RT: Cannot parse TaggedImage directory and prefix metadata: saving ResultsTable, script "+script_); 
            return false;
        } catch (FileNotFoundException ex) {
            IJ.log("    saving RT: "+rtFile.getAbsolutePath()+"FileNotFoundException: saving ResultsTable, script "+script_);
            return false;
        } catch (IOException ex) {
            IJ.log(this.getClass().getName()+"    I/O Error: saving ResultsTable, script "+script_);
            return false;
        }   
        
    }
    
    private boolean executePy(List<File> files, String dirForResults) {
        IJ.log(    "Excuting Py script:"+script_+"; args="+args_);
/*        JSONObject params = new JSONObject();
        try {
            params.put("workdir",workDir);
            JSONArray fileArray=new JSONArray();
            for (File f:files) {
                fileArray.put(f.getAbsolutePath());
            }
            params.put("imageFiles",fileArray);
            params.put("args", args_);
        } catch (JSONException ex) {
            Logger.getLogger(ScriptAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        }*/
        String fileList=new String();
        for (File f:files) {
            if (!f.exists()) {
                IJ.log("Problem executing script - File not found: "+f.getAbsolutePath());
                return false;
            }
            fileList+=" filename="+f.getAbsolutePath();
        }
        try {
//           Process process = Runtime.getRuntime().exec("python "+script_+" "+params.toString());
//           Process process = Runtime.getRuntime().exec("python "+script_+" \"workDir="+workDir+fileList+" "+args_+"\"");
           String arg[] = {
               "python",
               script_,
               "workDir="+dirForResults+fileList+" "+args_
           }; 
           Process process = Runtime.getRuntime().exec(arg);
//                process.waitFor();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String returnStr;
                while ((returnStr = reader.readLine()) != null) {
                    IJ.log("    Py script result: "+returnStr);
                }
                reader.close();
        } catch (IOException ex) {
            IJ.log("Problem executing script: "+script_);
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (Exception ex) {
            IJ.log("exception: "+ex);
            return false;
        }
        return true;
    }
    
    private boolean executeBsh(List<File> files, String dirForResults) {
        String sourceFiles[]=new String[files.size()];
            if (files.size()>0) {
            try {
                int i=0;
                for (File f:files) {
                    sourceFiles[i]=f.getAbsolutePath();
                    i++;
                }
                Interpreter interpreter=new bsh.Interpreter();
                interpreter.set("args","workDir="+dirForResults+" "+args_);
                interpreter.set("sourceFiles",sourceFiles);
                setScriptVariables(interpreter);
                interpreter.source(script_);
                rTable_=(ResultsTable)interpreter.get("rt");
                getScriptVariables(interpreter);
                IJ.log(    "Bsh Script executed:"+script_+".");
            } catch ( TargetError e ) {
                IJ.log("    "+this.getClass().getName()+": The script or code called by the script "
                        +script_+" threw an exception: "+ e.getTarget() );
                return false;
            } catch ( EvalError e )    {
                IJ.log("    "+this.getClass().getName()+": There was an error in evaluating the script "+script_+". " + e);
                return false;
            } catch (FileNotFoundException ex) {
                IJ.log("    "+this.getClass().getName()+": Script "+script_+" not found.");
                return false;
            } catch (IOException ex) {
                IJ.log("    "+this.getClass().getName()+"I/O Error in script "+script_+".");
                return false;
            } catch (Exception ex) {
                IJ.log("    "+this.getClass().getName()+"General exception in script "+script_+". "+ex.getMessage());
            } finally {
            }    
        }    
        return true;
    }
        
    private boolean execute(List<File> files) {
        if (files!=null && files.size()>0) {
            String dirForResults=files.get(0).getParent();
            if (!dirForResults.contains(workDir)) {
                dirForResults=workDir;
            } else {
                /* check if all files are within same subdir in workdir, 
                  if so, use this subdir instead of workDir as target dir for script result files
                */
                for (File f:files) {
                    if (!f.getParent().equals(dirForResults)) {
                        dirForResults=workDir;
                        break;
                    }
                }
            }
            if (script_.indexOf(".bsh")!=-1)
                return executeBsh(files, dirForResults);
            else if (script_.indexOf(".py")!=-1)
                return executePy(files, dirForResults);
            else
               return false;
        } else {
            return false;
        }    
    }
        
    protected void processResults(List<File> modFiles) {
        for (File f:modFiles) {
            IJ.log("    Processed File:"+f.getAbsolutePath()+".");
        }
        IJ.log("    Processed Results:"+script_+".");
    }
    
    @Override
    protected List<File> processGroup(final Group<File> group)  throws InterruptedException {
            IJ.log("ProcessGroup: "+group.elements.size()+" files");
            List<File> modFiles=new ArrayList<File>(group.elements.size());
            for (File f:group.elements) {
                IJ.log("    "+f.getAbsolutePath());
                modFiles.add(createCopy(f));
                
            }
            if (execute(modFiles)) {
                if (saveRT_ ) {
                    saveResultsTable(modFiles);
                }
                processResults(modFiles);
            }
            return modFiles;
    }

    @Override
    protected boolean isPoison(File f) {
        return ImageFileQueue.isPoison(f);
    }
     
    @Override
    public void makeConfigurationGUI() {
        JPanel optionPanel = new JPanel();
        BoxLayout layout = new BoxLayout(optionPanel,BoxLayout.Y_AXIS);
        optionPanel.setLayout(layout);
        
        JLabel l=new JLabel("Separate Images for Analysis by:");
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
        framesCB.setSelected(criteriaKeys.contains(MMTags.Image.FRAME) || criteriaKeys.contains(MMTags.Image.FRAME_INDEX));
        positionsCB.setSelected(criteriaKeys.contains(MMTags.Image.POS_NAME) || criteriaKeys.contains(MMTags.Image.POS_INDEX));
        clustersCB.setSelected(criteriaKeys.contains(ExtImageTags.CLUSTER_INDEX));
        areasCB.setSelected(criteriaKeys.contains(ExtImageTags.AREA_NAME) || criteriaKeys.contains(ExtImageTags.AREA_INDEX));
        commentsCB.setSelected(criteriaKeys.contains(ExtImageTags.AREA_COMMENT));           
            
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
    protected long determineMaxGroupSize(JSONObject meta) throws JSONException {
        if (!processOnTheFly || criteriaKeys.contains(ExtImageTags.AREA_COMMENT)) {
            IJ.log("Not processing on the fly, or group size unknown");
            return -1;
        } else {
            JSONObject summary=meta.getJSONObject(MMTags.Root.SUMMARY);
            int channels=(criteriaKeys.contains(MMTags.Image.CHANNEL_INDEX) ? 1 : summary.getInt(MMTags.Summary.CHANNELS));
            int slices=(criteriaKeys.contains(MMTags.Image.SLICE_INDEX) ? 1 : summary.getInt(MMTags.Summary.SLICES));
            int frames=(criteriaKeys.contains(MMTags.Image.FRAME_INDEX) ? 1 : summary.getInt(MMTags.Summary.FRAMES));
            int sitesInArea=meta.getInt(ExtImageTags.SITES_IN_AREA);
            int clustersInArea=meta.getInt(ExtImageTags.CLUSTERS_IN_AREA);
            if (criteriaKeys.contains(MMTags.Image.POS_INDEX)) {
                return channels*slices*frames;
            }
            if (criteriaKeys.contains(ExtImageTags.CLUSTER_INDEX)) {
                return channels*slices*frames*Math.round((sitesInArea/clustersInArea));
            }
            if (criteriaKeys.contains(ExtImageTags.AREA_INDEX)) {
                return channels*slices*frames*sitesInArea;
            } else {
                int positions=summary.getInt(MMTags.Summary.POSITIONS);
                return channels*slices*frames*positions;
            }
        }
    }

    @Override
    public JSONObject updateTagValue(JSONObject meta, String newDir, String newPrefix, boolean updateSummary) throws JSONException {
        JSONObject summary=meta.getJSONObject(MMTags.Root.SUMMARY);
        if (newDir!=null && updateSummary)
            summary.put(MMTags.Summary.DIRECTORY,newDir);
        if (newPrefix!=null && updateSummary)
            summary.put(MMTags.Summary.PREFIX,newPrefix);
        return meta;
    }

/*
    @Override
    protected void cleanUp() {
        super.cleanUp();
        if (storage!=null) {
            storage.close(); 
            //set to null so a new storage will be created when processor runs again
            storage=null;
        }

    }
*/
    
}

