/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.dataprocessors;

import autoimage.RuntimeTileManager;
import autoimage.Tile;
import autoimage.Utils;
import bsh.EvalError;
import bsh.Interpreter;
import ij.IJ;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.api.MMTags;

    
/**
 *
 * @author Karsten
 */
public class RoiFinder extends ScriptAnalyzer implements IDataProcessorOption<String> {
    
    protected List<Tile> tileList;
    protected List<String> options_;
    protected List<String> selectedSeq;
    private final List<RuntimeTileManager> tileManagerList;
    private final ExecutorService listenerExecutor;

    public RoiFinder () {
        this("","","",null,false);
    }
    
    public RoiFinder (final String script, final String args, String path, RuntimeTileManager tileManager) {
        this(script, args, path, tileManager,false);
    }

    public RoiFinder (final String script, final String args, String path, RuntimeTileManager tileManager, boolean saveRT) {
        super(script, args, path, saveRT);
        tileManagerList=new ArrayList<RuntimeTileManager>();
        if (tileManager!=null)
            tileManagerList.add(tileManager);
        selectedSeq=new ArrayList<String> ();
        listenerExecutor = Executors.newFixedThreadPool(1);
    }
    
    @Override
    public JSONObject getParameters() throws JSONException {
        JSONObject obj=super.getParameters();
/*        JSONArray opts=new JSONArray();
        for (String opt:options_) {
            opts.put(opt);
        }
        obj.put("Options", opts);*/
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
/*        JSONArray opts=obj.getJSONArray("Options");
        options_=new ArrayList<String>();
        for (int i=0; i<opts.length(); i++) {
            options_.add(opts.getString(i));
        }*/
        JSONArray names=obj.getJSONArray("SettingNames");
        selectedSeq=new ArrayList<String>();
        for (int i=0; i<names.length(); i++) {
            selectedSeq.add(names.getString(i));
        }
    }
    
    @Override
    public String getProcName() {
        return "ROI-Finder: "+new File(script_).getName()+" ["+args_+"]";
    }
    @Override
    public void setScriptVariables(Interpreter interpreter) throws EvalError {
        tileList= new ArrayList<Tile>();
        interpreter.set("tileList",tileList);
    }

    public void addRuntimeTileManager(RuntimeTileManager tileManager) {
        tileManagerList.add(tileManager);
    }
    
    public void removeRuntimeTileManager(RuntimeTileManager tileManager) {
        tileManagerList.remove(tileManager);
    }
    
    public void removeAllRuntimeTimeManagers() {
        tileManagerList.clear();
    }
    
    //File f is reference to working copy
    @Override
    protected void processResults(File f) {
        IJ.log(    "    Processed Results:"+script_+".");
        try {
            JSONObject meta=Utils.parseMetadata(f);
            String name=meta.getString(MMTags.Image.POS_NAME);
            int imgWidth=meta.getInt(MMTags.Image.WIDTH);
            int imgHeight=meta.getInt(MMTags.Image.HEIGHT);
            double stageX=meta.getDouble(MMTags.Image.XUM);
            double stageY=meta.getDouble(MMTags.Image.YUM);
            double stageZ=meta.getDouble(MMTags.Image.ZUM);
            final String area=meta.getString("Area");
            if (tileList.size()>0){
                for (final Tile tile:tileList) {
                    tile.name=name;
                    tile.centerX=stageX-imgWidth/2+tile.centerX;
                    tile.centerY=stageY-imgWidth/2+tile.centerY;
                    tile.relZPos=stageZ;
//                    for (RuntimeTileManager rtm:tileManagerList)
//                        rtm.addStageROI(area,tile);
                    
                    synchronized (tileManagerList) {
                        for (final RuntimeTileManager rtm : tileManagerList) {
                            listenerExecutor.submit(
	                       new Runnable() {
	                          @Override
	                          public void run() {
                                    rtm.addStageROI(area,tile);
	                          }
	                       });
                        }
                    }
      
                    IJ.log("         ROIs for "+area+", "+tile.name+", "+tile.centerX+", "+tile.centerY+", "+tile.relZPos);
                }    
            } else {
                IJ.log("         no ROIs found");
            }    
        } catch (JSONException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    @Override
    public void makeConfigurationGUI() {
        JPanel optionPanel = new JPanel();
        BoxLayout layout = new BoxLayout(optionPanel,BoxLayout.Y_AXIS);
        optionPanel.setLayout(layout);

        JLabel l=new JLabel("Select acquisition sequences that utilize the ROIs");
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
        
        l=new JLabel("Script File:");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.add(l);     
        JPanel filePanel = new JPanel();
        BoxLayout flayout = new BoxLayout(filePanel,BoxLayout.X_AXIS);
        final JFormattedTextField scriptField = new JFormattedTextField();
        scriptField.setColumns(30);
//        if (script_!=null)
        scriptField.setValue(script_);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        filePanel.add(scriptField);
        JButton fileButton=new JButton("Browse");
        filePanel.add(fileButton);
        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                JFileChooser fc=new JFileChooser();
                fc.setCurrentDirectory(new File(scriptField.getText()).getParentFile());
                int result=fc.showSaveDialog(null);
                if (result == JFileChooser.APPROVE_OPTION)
                    scriptField.setValue(fc.getSelectedFile().getAbsolutePath());
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
//        if (sa!=null)
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
            selectedSeq.clear();
            for (int i=0; i<optCB.length; i++) {
                if (optCB[i].isSelected()) {
                    selectedSeq.add(optCB[i].getText());
                }    
            }script_=scriptField.getText();
            args_=argField.getText().replaceAll("\n", " ");
            saveRT_=saveCB.isSelected();
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
