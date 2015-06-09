/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.olddp;

import autoimage.ExtImageTags;
import autoimage.MMCoreUtils;
import autoimage.Utils;
import autoimage.Vec3d;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import java.awt.Component;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import mmcorej.TaggedImage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.acquisition.TaggedImageStorageDiskDefault;
import org.micromanager.api.MMTags;
import org.micromanager.api.TaggedImageStorage;

/**
 *
 * @author Karsten
 */
public class StitchCluster_NoCamRot extends ClusterProcessor {
    
    private String fusionMethod;
    private double regressionTh;
    private double maxAvgDisplaceTh;
    private double absDisplaceTh;
    private boolean addTilesAsROIs;
    private boolean computeOverlap;
    private boolean subpixelAccuracy;
    private String compParams;
    private static PlugIn stitch_grid=null;
    
    private static boolean stitchingInProgress=false;
    private static String outputDir="";
    
    public StitchCluster_NoCamRot() {
        super ("Stitching");
        fusionMethod="Linear Blending";
        regressionTh=0.7;
        maxAvgDisplaceTh=2.5;
        absDisplaceTh=3.5;
        addTilesAsROIs=false;
        computeOverlap=true;
        subpixelAccuracy=true;
        compParams="Save memory (but be slower)";
        Class<? extends PlugIn> stitchClass;
        try {
            stitchClass = (Class<? extends PlugIn>)Class.forName("plugin.Stitching_Grid");
            stitch_grid = stitchClass.newInstance();
        } catch (ClassNotFoundException ex) {
            IJ.log("StitchCluster.constructor: Class not found");
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            IJ.log("StitchCluster.constructor: Instantiation Error");
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            IJ.log("StitchCluster.constructor: Illegal Access");
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }

    }
        
    @Override
    public JSONObject getParameters() throws JSONException {
        JSONObject obj=super.getParameters();
        obj.put("FusionMethod", fusionMethod);
        obj.put("RegressionThreshold", regressionTh);
        obj.put("MaxAvgDisplaceTh", maxAvgDisplaceTh);
        obj.put("AbsDisplaceTh", absDisplaceTh);
        obj.put("AddTilesAsROIs", addTilesAsROIs);
        obj.put("ComputeOverlap", computeOverlap);
        obj.put("SubpixelAccuracy", subpixelAccuracy);
        obj.put("ComputationParams", compParams);
        return obj;
    }
            
    @Override
    public void setParameters(JSONObject obj) throws JSONException {
        super.setParameters(obj);
        fusionMethod=obj.getString("FusionMethod");
        regressionTh=obj.getDouble("RegressionThreshold");
        maxAvgDisplaceTh=obj.getDouble("MaxAvgDisplaceTh");
        absDisplaceTh=obj.getDouble("AbsDisplaceTh");
        addTilesAsROIs=obj.getBoolean("AddTilesAsROIs");
        computeOverlap=obj.getBoolean("ComputeOverlap");
        subpixelAccuracy=obj.getBoolean("SubpixelAccuracy");
        compParams=obj.getString("ComputationParams");
    }
            
    /*        
    public void renameMetadataFile(String path, String originalName, String newName) {
                    try {
                        File oldfile = new File(path,originalName);
                        if (!oldfile.exists())
                            return;
//                        IJ.log("        old file: "+oldfile.getAbsolutePath());
                      // File (or directory) with new name
                        File newfile;
                        int i=1;
                        do {
                            newfile = new File(path, newName+Integer.toString(i)+".txt");
                            i++;
                      } while (newfile.exists());
//                        IJ.log("        new file: "+newfile.getAbsolutePath());
                        boolean success = oldfile.renameTo(newfile);
                        if (!success) {
//                            IJ.log("renaming failed");
                        }        
                    } catch (Exception e) {
                    }
    }
    */
    /*
    private void restoreMetadataFile(String path, String currentName, String newName) {
                        int i=0; 
                        try {
                            File oldfile;
                            do {
                                i++;
                                oldfile = new File(path, currentName+Integer.toString(i)+".txt");
                            } while (!oldfile.exists() && i<10);
                            if (!oldfile.exists())
                                return;
//                            IJ.log("        old file: "+oldfile.getAbsolutePath());

                            // File (or directory) with new name
                            File newfile = new File(path,newName);
                            //i++;
//                            IJ.log("        new file: "+newfile.getAbsolutePath());
                            boolean success = oldfile.renameTo(newfile);
                            if (!success) {
//                                IJ.log("restoring filename failed");
                         }
                        } catch (Exception e) {                
                        }
    }
    */

    private List<Vec3d> readCoordinates(File file) throws FileNotFoundException, IOException {
        List<Vec3d> list = new ArrayList<Vec3d>();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line="";
            while (!line.contains("# Define the image coordinates")) {
                line=br.readLine();
            }
            line=br.readLine();
            int i=0;
            Vec3d refVec=null;
            while (line!=null) {
                String xStr=line.substring(line.indexOf("(")+1);
                String yStr=xStr.substring(xStr.indexOf(",")+1);
                if (yStr.contains(","))
                    yStr=yStr.substring(0,yStr.indexOf(","));
                else
                    yStr=yStr.substring(0,yStr.indexOf(")"));
                xStr=xStr.substring(0,xStr.indexOf(","));
                if (i==0) {
                    refVec=new Vec3d(Double.parseDouble(xStr),Double.parseDouble(yStr),0);
                } else
                    list.add(new Vec3d(Double.parseDouble(xStr)-refVec.x,Double.parseDouble(yStr)-refVec.y,0));
                
                line=br.readLine();
                i++;
            }
            br.close();  
        return list;
    }
    
/*    
    private double calculateRotation(File configFile, File registerFile) {
        double angle=0;
        try {
            List<Vec3d> original=readCoordinates(configFile);
            List<Vec3d> registered=readCoordinates(registerFile);
            List<Double> angles=new ArrayList<Double>(original.size());
            for (int i=0; i<original.size(); i++) {
                Vec3d orig=original.get(i);
                Vec3d reg=registered.get(i);
                try {
                    angle=orig.angle(reg);
                } catch (Exception ex) {
                    Logger.getLogger(StitchCluster_NoCamRot.class.getName()).log(Level.SEVERE, null, ex);
                }
                IJ.log("orig: "+orig.toString()+" --> "+reg.toString()+", Angle: "+Double.toString(angle));
                angles.add(angle);
            }
        } catch (FileNotFoundException ex) {
            IJ.log(this.getClass().getName()+": FileNotFound");
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            IJ.log(this.getClass().getName()+": IOException");
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return angle;
    }
    */
    
    @Override
    public List<File> analyzeGroup(List<File> elements) {
        List<File> stitchedFiles=new ArrayList<File>();
        IJ.log("-----");
        IJ.log("analyzeGroup");
        if (elements!=null && elements.size()>1) {
            while (stitchingInProgress) {
                IJ.log("waiting for stitch to finish");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }
            stitchingInProgress=true;
            String sourceImagePath=elements.get(0).getParent();
            sourceImagePath=new File (sourceImagePath).getParent();
            try {
                meta=MMCoreUtils.parseMetadataFromFile(elements.get(0));
                JSONObject summary=meta.getJSONObject(MMTags.Root.SUMMARY);
                String prefix = meta.getString(ExtImageTags.AREA_NAME);
                long clusterIdx=meta.getLong(ExtImageTags.CLUSTER_INDEX);
                if (clusterIdx != -1)
                    prefix=prefix+"-Cluster"+Long.toString(clusterIdx);
                IJ.log("StitchCluster: "+prefix);
                IJ.log("StitchCluster element(0) FileName: "+meta.getString("FileName"));
             
                String frame="t"+Long.toString(meta.getLong(MMTags.Image.FRAME_INDEX))+"-";
                String channel=meta.getString(MMTags.Image.CHANNEL_NAME)+"-";
                Long noOfCh=summary.getLong(MMTags.Summary.CHANNELS);
                Long noOfSlices=summary.getLong(MMTags.Summary.SLICES);
                Long noOfFrames=summary.getLong(MMTags.Summary.FRAMES);
                
                int dim=noOfSlices < 2 ? 2 : 3;
                
                List<String> chNames=new ArrayList<String>();
                JSONArray chN=meta.getJSONObject(MMTags.Root.SUMMARY).getJSONArray(MMTags.Summary.NAMES);
                for (int i=0; i<chN.length(); i++)
                    chNames.add(chN.getString(i));
                String slice="z"+Long.toString(meta.getLong(MMTags.Image.SLICE_INDEX));
                Double pixSize=meta.getJSONObject(MMTags.Root.SUMMARY).getDouble(MMTags.Summary.PIXSIZE);
                File configFile = new File(sourceImagePath, "StitchConfig-"+prefix+"-"+frame+channel+slice+".txt");
                File registerFile = new File(sourceImagePath, "StitchConfig-"+prefix+"-"+frame+channel+slice+".registered.txt");

                double xMin=0;
                double xMax=0;
                double yMin=0;
                double yMax=0;
                List<String> foundCh=new ArrayList<String>();
                try {
                    BufferedWriter writer;
                    
                    writer = new BufferedWriter(new FileWriter(configFile));
                    String line = "# Define the number of dimensions we are working on\n"
                            + "dim = " + Integer.toString(dim)
                            + "\n\n"
                            + "# Define the image coordinates\n";
                    writer.write(line);
                    int i=1;
                    for (File e:elements) {
                        IJ.log(Integer.toString(i)+": "+e.getAbsolutePath());
                        meta=MMCoreUtils.parseMetadataFromFile(e);
                        //need to convert from um into pixel coords if using grid stitching plugin

                        double xUM=meta.getDouble(MMTags.Image.XUM);
                        double yUM=meta.getDouble(MMTags.Image.YUM);
                        double zUM=meta.getDouble(MMTags.Image.ZUM);
                        String ch=meta.getString((MMTags.Image.CHANNEL_NAME));
                        if (!foundCh.contains(ch))
                            foundCh.add(ch);
                        if (i==1) {
                            xMin=xUM;
                            xMax=xUM;
                            yMin=yUM;
                            yMax=yUM;
                        } else {
                            xMin=xUM < xMin?xUM:xMin;
                            xMax=xUM > xMax?xUM:xMax;
                            yMin=yUM < yMin?yUM:yMin;
                            yMax=yUM > yMax?yUM:yMax;
                        }
                        double x=xUM/pixSize;
                        double y=yUM/pixSize;
                        double z=zUM/pixSize;
                        String path=e.getParent();
                      //  renameMetadataFile(path,"metadata.txt","metadata_backup");
                        path=new File(path).getName();
                        if (dim==2) {
                            line = new File(path,e.getName()).getPath() + "; ; (" + Double.toString(x) + ", " + Double.toString(y) + ")\n";
                        } else {
                            line = new File(path,e.getName()).getPath() + "; ; (" + Double.toString(x) + ", " + Double.toString(y) + ", " + Double.toString(z)+ ")\n";
                            
                        }
                        writer.write(line);
                        i++;

                    }    
                    writer.close();
                } catch (IOException ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                    IJ.log(this.getClass().getName()+": Error creating stitch config file");
                }
                
                IJ.log("sourceImagePath: "+sourceImagePath);
                IJ.log("workDir: "+workDir);

                    //this is a hack to change the output directory for saving fused image and ROIs
                String defaultDirectory="";
                if (outputDir==null)
                        outputDir=sourceImagePath;
                if (stitch_grid!=null) {
                    try {
                        Field f=stitch_grid.getClass().getDeclaredField("defaultOutputDirectory");
                        String lastDefaultOutputDirectory=(String)f.get(stitch_grid);
                        IJ.log("defaultOutputDirectory: "+lastDefaultOutputDirectory);
                        f.set(null,"");
                        f=stitch_grid.getClass().getDeclaredField("defaultDirectory");
                        defaultDirectory=(String)f.get(stitch_grid);
                        IJ.log("defaultDirectory: "+defaultDirectory);
                        f.set(null, outputDir);

                    } catch (NoSuchFieldException ex) {
                        IJ.showMessage("cannot find field"+ex);
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                    } catch (SecurityException ex) {
                        IJ.showMessage("security field"+ex);
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalArgumentException ex) {
                        IJ.showMessage("illegal arg field"+ex);
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalAccessException ex) {
                        IJ.showMessage("illegal access field"+ex);
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                    }                     
                } else {
                    IJ.log("stitch_grid = null");  
                }    
             //   IJ.runMacroFile("scripts/setOutput.bsh", sourceImagePath);
                try {
                    String compOverlap=computeOverlap?"compute_overlap ":""; 
                    String subpix=subpixelAccuracy?"subpixel_accuracy ":"";
                    String rois=addTilesAsROIs?"add_tiles_as_rois ":"";
/*                    Thread thread=Thread.currentThread();
                    Macro.setOptions(thread, "type=[Positions from file] "
                                    + "order=[Defined by TileConfiguration] "
                                    + "directory=["+sourceImagePath+"] "
                                    + "layout_file=["+configFile.getName()+"] "
                                    + "fusion_method=["+fusionMethod+"] "
                                    + "regression_threshold="+Double.toString(regressionTh)+" "
                                    + "max/avg_displacement_threshold="+Double.toString(maxAvgDisplaceTh)+ " "
                                    + "absolute_displacement_threshold="+Double.toString(absDisplaceTh)+ " "
                                    + rois
                                    + subpix
                                    + compOverlap
                                    + "ignore_z_stage"
                                    + "computation_parameters=["+compParams+"] "
                                    + "image_output=[Write to disk]");
                    Object runPlugIn = IJ.runPlugIn("Grid/Collection stitching","");
                    IJ.log("after call runPluin");
                    Macro.setOptions(thread,null);*/
                    IJ.run("Grid/Collection stitching",
                    "type=[Positions from file] "
                    + "order=[Defined by TileConfiguration] "
                    + "directory=["+sourceImagePath+"] "
                    + "layout_file=["+configFile.getName()+"] "
                    + "fusion_method=["+fusionMethod+"] "
                    + "regression_threshold="+Double.toString(regressionTh)+" "
                    + "max/avg_displacement_threshold="+Double.toString(maxAvgDisplaceTh)+ " "
                    + "absolute_displacement_threshold="+Double.toString(absDisplaceTh)+ " "
                    + rois
                    + subpix
                    + compOverlap
                    + "ignore_z_stage"
                    + "computation_parameters=["+compParams+"] "
                    + "image_output=[Write to disk]");
                } catch (Exception ex) {
                    IJ.log("Stitching exception: "+ex);
                }
                for (File e:elements) {
                    String path=e.getParent();
                   // restoreMetadataFile(path,"metadata_backup","metadata.txt");                    
//                    path=new File(path).getName();
                }
                
                try {
                    //move Stitch configFile and update file handle 
                    Utils.copyFile(configFile, new File(workDir,configFile.getName()));
                    configFile.delete();
                    configFile=new File(workDir,configFile.getName());

                    //if compute_overlap, registered tile file should have been created
                    //move registered Stitch configFile and update file handle 
                    if (registerFile.exists()) {
                        Utils.copyFile(registerFile, new File(workDir,registerFile.getName()));
                        registerFile.delete();
                        registerFile=new File(workDir,registerFile.getName());
                    }
                    
                    TaggedImageStorage storage = null;

                    for (int i=0; i<foundCh.size(); i++) {
                    //wait for result stitched image file
                        String name="img_t1_z1_c"+Integer.toString(i+1);
                        IJ.log("processing result "+name);
//                        File resultFile=new File(outputDir, name);
                        File resultFile=new File(sourceImagePath, name);
                        File resultFile2=null;
                        if (!"".equals(outputDir))
                            resultFile2=new File(outputDir, name);
//                      File resultFile=new File(new File(workDir,prefix),name);                    
                        //wait for result file to be saved
                        while (!resultFile.exists()) {
                            if (resultFile2!=null)
                                if (resultFile2.exists()) {
                                    IJ.log("STITCHED FILE FOUND IN outputDir "+ outputDir);
                                    resultFile=resultFile2;
                                }    
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                            }
                        } 

                    
                        ImagePlus imp=IJ.openImage(resultFile.getAbsolutePath());
                        ImageProcessor proc=imp.getProcessor();
                        IJ.log("got imageProcessor");
                        //if compute_overlap, tiles may have been rotated (if camera rotation was not correctly set) 
            /*            if (registerFile.exists()) {
                            double angle=calculateRotation(configFile, registerFile);
                            proc.rotate(angle);
                        }*/
                        TaggedImage ti=null;
                        try {
                            //these are used to create display settings 
                            //newMeta.put(MMTags.Summary.HEIGHT, imp.getHeight());
                            if (storage==null) {
                                JSONObject newSummary = meta.getJSONObject(MMTags.Root.SUMMARY);
                                newSummary.put(MMTags.Summary.SLICES, 1); 
                                newSummary.put(MMTags.Summary.POSITIONS, 1); 
                                newSummary.put(MMTags.Summary.CHANNELS, noOfCh); 
                                newSummary.put(MMTags.Summary.FRAMES, 1); 
                                newSummary.put(MMTags.Summary.WIDTH,imp.getWidth()); 
                                newSummary.put(MMTags.Summary.HEIGHT,imp.getHeight()); 
                                newSummary.put(MMTags.Summary.PREFIX,prefix); 
                                newSummary.put(MMTags.Summary.DIRECTORY, workDir);
                                // ??newSummary.put("PositionName", prefix);
                                newSummary.remove("InitialPositionList");
                                storage = new TaggedImageStorageDiskDefault (
                                    workDir,true,newSummary);
                            }
                            IJ.log("after summary update");
                            meta.put(MMTags.Image.HEIGHT, imp.getHeight());
                            //newMeta.put(MMTags.Summary.WIDTH, imp.getWidth());
                            meta.put(MMTags.Image.WIDTH, imp.getWidth());
                            meta.put(MMTags.Image.POS_NAME,prefix);
                            meta.put(MMTags.Image.POS_INDEX,0);
                            meta.put(MMTags.Image.CHANNEL_NAME, chNames.get(i));
                            meta.put(MMTags.Image.CHANNEL_INDEX,i);
                            meta.put(MMTags.Image.XUM,xMin+(xMax-xMin)/2);
                            meta.put(MMTags.Image.YUM,yMin+(yMax-yMin)/2);
                            meta.put("SliceIndex",0);
                            meta.put("ClusterIndex",0);
                            IJ.log("xMin: "+Double.toString(xMin)+", xMax: "+Double.toString(xMax)+", yMin: "+Double.toString(yMin)+", yMax: "+Double.toString(yMax));
                            IJ.log("xMax-xMin: "+Double.toString(xMax-xMin)+", yMax-yMin: "+Double.toString(yMax-yMin)+", imp.getWidth()-->um="+Double.toString(imp.getWidth()*pixSize)+", imp.getHeight()-->um="+Double.toString(imp.getHeight()*pixSize));
                            IJ.log("trying to create new TaggedImage");
                            ti=new TaggedImage(proc.getPixels(),meta);
                            //ti.tags.put("FileName", stitchedFile.getName());
                            //JSONObject summary=newMeta.getJSONObject(MMTags.Root.SUMMARY);
                            IJ.log("new TaggedImage created");
                            storage.putImage(ti);
            /*                stitchedFile=new File(new File(workDir,prefix),ti.tags.getString("FileName"));
                            IJ.log("        stitchedFile: "+stitchedFile.getAbsolutePath());*/

                            stitchedFiles.add(new File(new File(workDir,prefix), 
                                    ti.tags.getString("FileName")));
                            IJ.log(stitchedFiles.get(stitchedFiles.size()-1).getAbsolutePath());
                        } catch (Exception ex) {
                            IJ.log("Problem saving to storage: "+ti.tags.getString(MMTags.Image.CHANNEL_NAME));
                            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                        }
                        resultFile.delete();
                    
                    }
                    storage.close();
                    IJ.log("Stitching successful");
                    IJ.log("----");
                    stitchingInProgress=false;
                } catch (IOException ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                    IJ.log(this.getClass().getName()+": Error saving stitched results");
                    stitchingInProgress=false;
                }
            } catch (JSONException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                IJ.log(this.getClass().getName()+": Error parsing metadata");
                stitchingInProgress=false;
            } 
        }
        return stitchedFiles;     
    }
    
    @Override
    public void makeConfigurationGUI() {
        JPanel optionPanel = new JPanel();
        GridLayout layout = new GridLayout(0,2);

        optionPanel.setLayout(layout);
        JLabel l=new JLabel("Fusion method:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        String[] fusionOptions=new String[]
                {"Linear Blending",
                "Average",
                "Median",
                "Max. Intensity",
                "Min. Intensity",
                "Do not fuse images (only write TileConig File)"};
        
        JComboBox fusionMethodCombo=new JComboBox(fusionOptions);
        fusionMethodCombo.setSelectedItem(fusionMethod);
        optionPanel.add(fusionMethodCombo);
        
        l=new JLabel("Regression threshold:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        JFormattedTextField regressionThField = new JFormattedTextField();
        regressionThField.setColumns(10);
        regressionThField.setValue(new Double(regressionTh));
        optionPanel.add(regressionThField);
            
        l=new JLabel("Max/Avg displacement threshold:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        JFormattedTextField maxAvgDisplaceThField = new JFormattedTextField();
        maxAvgDisplaceThField.setColumns(10);
        maxAvgDisplaceThField.setValue(new Double(maxAvgDisplaceTh));
        optionPanel.add(maxAvgDisplaceThField);
            
        l=new JLabel("Absolute displacement threshold:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        JFormattedTextField absDisplaceThField = new JFormattedTextField();
        absDisplaceThField.setColumns(10);
        absDisplaceThField.setValue(new Double(absDisplaceTh));
        optionPanel.add(absDisplaceThField);
            
        l=new JLabel("Compute overlap:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        JCheckBox computeOverlapCB = new JCheckBox();
        computeOverlapCB.setAlignmentX(Component.RIGHT_ALIGNMENT);
        computeOverlapCB.setSelected(computeOverlap);
        optionPanel.add(computeOverlapCB);

        l=new JLabel("Subpixel accuracy:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        JCheckBox subpixelCB = new JCheckBox();
        subpixelCB.setAlignmentX(Component.RIGHT_ALIGNMENT);
        subpixelCB.setSelected(subpixelAccuracy);
        optionPanel.add(subpixelCB);
        
        l=new JLabel("Computation Parameters:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        String[] compParamOptions=new String[]
                {"Save memory (but be slower)",
                "Save computation time (but use more RAM)"};
        
        JComboBox compParamCombo=new JComboBox(compParamOptions);
        compParamCombo.setSelectedItem(compParams);
        optionPanel.add(compParamCombo);

        
        int result = JOptionPane.showConfirmDialog(null, optionPanel, 
                this.getClass().getName(), JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            fusionMethod=(String)fusionMethodCombo.getSelectedItem();
            try {
                regressionThField.commitEdit();
            } catch (ParseException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
            regressionTh=(Double)regressionThField.getValue();
            try {
                maxAvgDisplaceThField.commitEdit();
            } catch (ParseException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
            maxAvgDisplaceTh=(Double)maxAvgDisplaceThField.getValue();
            try {
                absDisplaceThField.commitEdit();
            } catch (ParseException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
            absDisplaceTh=(Double)absDisplaceThField.getValue();
            computeOverlap=computeOverlapCB.isSelected();
            subpixelAccuracy=subpixelCB.isSelected();
            compParams=(String)compParamCombo.getSelectedItem();
        }    
    }
    
}
