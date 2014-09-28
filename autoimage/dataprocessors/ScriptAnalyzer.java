/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.dataprocessors;

import autoimage.ExtImageTags;
import autoimage.ImageFileQueue;
import autoimage.Utils;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.TargetError;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.measure.ResultsTable;
import java.awt.Component;
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
 *
 * @author Karsten
 */
public class ScriptAnalyzer extends BranchedProcessor<File>  {
    
    protected String script_;
    protected String args_;
    protected boolean saveRT_;
    protected ResultsTable rTable_; //new table for each image
    protected static String scriptDir = "";
    
    
    public ScriptAnalyzer() {
        this("","","",false);
    }
    
    public ScriptAnalyzer(final String script, final String args, final String path) {
        this(script,args,path, false);
    }
    
    
    public ScriptAnalyzer(String script, String args, String path, boolean saveRT) {
        super("ScriptAnalyzer: "+new File(script).getName()+" ["+args+"]", path);
        script_=script;
        args_=args;
        saveRT_=saveRT;
        rTable_=null;
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
    
    protected boolean saveResultsTable(File modFile) {
        File rtFile=null;
        try {
            JSONObject meta=Utils.parseMetadata(modFile);
            String area = meta.getString(ExtImageTags.AREA_NAME);
            String cluster=Long.toString(meta.getLong(ExtImageTags.CLUSTER_INDEX));
            String site=Long.toString(meta.getLong(ExtImageTags.SITE_INDEX));
            String channel=meta.getString(MMTags.Image.CHANNEL_NAME);
            String cFrame=Long.toString(meta.getLong(MMTags.Image.FRAME_INDEX));
            String cSlice=Long.toString(meta.getLong(MMTags.Image.SLICE_INDEX));
            IJ.log("cframe: "+cFrame);
            IJ.log("cSlice: "+cSlice);
            File path;
            if (!cluster.equals("-1"))
                path=new File(workDir,area+"-Cluster"+cluster+"-Site"+site);
            else
                path=new File(workDir,area+"-Site"+site);
            try {
                if (!path.exists()) {
                    path.mkdirs(); 
                }
            } catch (Exception e) {
                IJ.log("  Cannot create directory");
            }    

            String scriptName=new File(script_).getName();
            IJ.log("  script: "+scriptName);
            rtFile=new File(path.getPath(),
                "Results-"+scriptName.substring(0, scriptName.indexOf("."))
                +"-"+cFrame//String.format("%09d", cFrame)
                +"-"+channel
                +"-"+cSlice//String.format("%03d", cSlice)
                +".txt");
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
    
    private boolean executePy(File f) {
        IJ.log(    "Excuting Py script:"+script_+"; args="+args_);

        try {
           Process process = Runtime.getRuntime().exec("python "+script_+" "+args_);
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
        } catch (Exception ex) {
            IJ.log("exception: "+ex);
        }
        return true;
    }
    
    private boolean executeBsh(File f) {
        ImagePlus imp=IJ.openImage(f.getAbsolutePath());    
        if (imp!=null) {
            try {
                Interpreter interpreter=new bsh.Interpreter();
                interpreter.set("imp",imp);
                interpreter.set("args",args_);
                interpreter.set("sourceFile",f.getAbsolutePath());
                setScriptVariables(interpreter);
                interpreter.source(script_);
                /*
                JSONObject meta=Utils.parseMetadata(f);
                String area=meta.getString("Area");
                String cluster=meta.getString("Cluster");
                String site=meta.getString("Site");
                String subDir=area+"-Cluster"+cluster+"-Site"+site;

                IJ.saveAsTiff(imp, new File(workDir,new File(,f.getName()).getAbsolutePath()));
                */
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
            } finally {
                imp.close();
            }    
        }    
        return true;
    }
        
    private boolean execute(File modFile) {
        if (modFile!=null) {
            if (script_.indexOf(".bsh")!=-1)
                return executeBsh(modFile);
            else if (script_.indexOf(".py")!=-1)
                return executePy(modFile);
            else
               return false;
        } else {
            return false;
        }    
    }
        
    protected void processResults(File modFile) {
        IJ.log("    Processed File:"+modFile.getAbsolutePath()+".");
        IJ.log("    Processed Results:"+script_+".");
    }
    
    @Override
    protected List<File> analyze(File f) {
/*        JSONObject meta=Utils.parseMetadata(f);
        try {
            String area = meta.getString("Area");
            String cluster=Long.toString(meta.getLong("ClusterIndex"));
            String site=Long.toString(meta.getLong("SiteIndex"));
            File path;
            if (!cluster.equals("-1"))
                path=new File(workDir,area+"-Cluster"+cluster+"-Site"+site);
            else
                path=new File(workDir,area+"-Site"+site);
         //   ImagePlus imp=IJ.openImage(f.getAbsolutePath());
         //   IJ.saveAsTiff(imp, tempDir.getAbsolutePath());
            if (!path.exists()) {
                path.mkdirs(); 
            }    
            File modFile=new File(path,f.getName());
            try {
                Utils.copyFile(f,modFile);
            } catch (IOException ex) {
                Logger.getLogger(ScriptAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
                modFile=f;
                IJ.log("Problem: copying file for "+script_);
            }*/
            File modFile=createCopy(f);
            if (execute(modFile)) {
                if (saveRT_ && rTable_!=null) {
                    saveResultsTable(modFile);
                }
                processResults(modFile);
            }
            List<File> list = new ArrayList<File>(1);
            list.add(modFile);
            return list;
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
        JLabel l=new JLabel("Script File:");
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
            
        l=new JLabel("Save Numeric Results");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.add(l);
        JCheckBox saveCB = new JCheckBox();
        saveCB.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveCB.setSelected(saveRT_);
        optionPanel.add(saveCB);
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
            script_=scriptField.getText();
            args_=argField.getText().replaceAll("\n", " ");
            saveRT_=saveCB.isSelected();
        }
    }


}
