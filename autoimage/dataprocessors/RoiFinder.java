/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.dataprocessors;

import autoimage.ExtImageTags;
import autoimage.TileManager;
import autoimage.Utils;
import autoimage.Vec3d;
import bsh.EvalError;
import bsh.Interpreter;
import ij.IJ;
import ij.Prefs;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
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
    
    protected List<Point2D> roiList;
    protected List<String> options_;
    protected List<String> selectedSeq;
    private final List<TileManager> tileManagerList;
    private final ExecutorService listenerExecutor;

    public RoiFinder () {
        this("","","",null,false);
    }
    
    public RoiFinder (final String script, final String args, String path, TileManager tileManager) {
        this(script, args, path, tileManager,false);
    }

    public RoiFinder (final String script, final String args, String path, TileManager tileManager, boolean saveRT) {
        super(script, args, path, saveRT);
        tileManagerList=new ArrayList<TileManager>();
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
        roiList= new ArrayList<Point2D>();
        interpreter.set("roiList",roiList);
    }

    public void addTileManager(TileManager tileManager) {
        tileManagerList.add(tileManager);
    }
    
    public void removeTileManager(TileManager tileManager) {
        tileManagerList.remove(tileManager);
    }
    
    public void removeAllTileManagers() {
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
            JSONObject summary=meta.getJSONObject(MMTags.Root.SUMMARY);
            double pixSize=summary.getDouble(MMTags.Summary.PIXSIZE);
            double detectorAngle=summary.getDouble(ExtImageTags.DETECTOR_ROTATION);
            double stageCenterX=meta.getDouble(MMTags.Image.XUM);
            double stageCenterY=meta.getDouble(MMTags.Image.YUM);
            double stageZ=meta.getDouble(MMTags.Image.ZUM);
            final String area=meta.getString(ExtImageTags.AREA_NAME);
            double cosinus=Math.cos(detectorAngle);
            double sinus=Math.sin(detectorAngle);
            if (roiList.size()>0){
                final List<Vec3d> stagePosList=new ArrayList<Vec3d>(roiList.size());
                for (final Point2D point:roiList) {
                    //correct for detector rotation relative to stage
                    //1. translate to center -> dx/dy
                    double dx=point.getX()-pixSize*imgWidth/2;
                    double dy=point.getY()-pixSize*imgHeight/2;
                    //2. rotate
                    double x = (cosinus * dx) - (sinus * dy);
                    double y = (sinus * dx) + (cosinus * dy);
                    //3. translate to stage position of image center
                    stagePosList.add(new Vec3d(stageCenterX + x,stageCenterY + y,stageZ));
                }
                roiList.clear();
                    
                synchronized (tileManagerList) {
                    for (final TileManager rtm : tileManagerList) {
                        listenerExecutor.submit(
                           new Runnable() {
                              @Override
                              public void run() {
                                rtm.stagePosListAdded(area,stagePosList,this);
                              }
                           });
                    }
                }
      
                IJ.log("    "+Integer.toString(stagePosList.size())+" ROIs added for area "+area);
            } else {
                IJ.log("    no ROIs found");
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
