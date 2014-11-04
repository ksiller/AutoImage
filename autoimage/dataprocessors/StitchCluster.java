/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage.dataprocessors;

import autoimage.ExtImageTags;
import autoimage.FieldOfView;
import autoimage.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
public class StitchCluster extends GroupProcessor<File> {

    private String fusionMethod;
    private double regressionTh;
    private double maxAvgDisplaceTh;
    private double absDisplaceTh;
//    private boolean addTilesAsROIs;
    private boolean computeOverlap;
    private boolean subpixelAccuracy;
    private boolean downSample;
    private boolean invertX;
    private boolean invertY;
    private String compParams;
    private boolean rotateAfterStitching;
    private static PlugIn stitch_grid=null;
    
//    private static boolean stitchingInProgress=false;
    private static long jobNumber = 0;
    private static String outputDir="";
    private static ExecutorService executor=Executors.newSingleThreadExecutor();
    
    public StitchCluster() {
        super ("Stitching Cluster");
        List<String> criteria=new ArrayList<String>();
        criteria.add(ExtImageTags.AREA_INDEX);
        criteria.add(ExtImageTags.CLUSTER_INDEX);
        setCriteria(criteria);
        processIncompleteGrps=true;
        
        fusionMethod="Linear Blending";
        regressionTh=0.7;
        maxAvgDisplaceTh=2.5;
        absDisplaceTh=3.5;
//        addTilesAsROIs=false;
        computeOverlap=true;
        invertX=false;
        invertY=false;
        subpixelAccuracy=true;
        downSample=false;
        compParams="Save memory (but be slower)";
        rotateAfterStitching=true;
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
//        obj.put("AddTilesAsROIs", addTilesAsROIs);
        obj.put("ComputeOverlap", computeOverlap);
        obj.put("InvertX", invertX);
        obj.put("InvertY", invertY);
        obj.put("SubpixelAccuracy", subpixelAccuracy);
        obj.put("DownSample", downSample);
        obj.put("ComputationParams", compParams);
        obj.put("RotateAfterStitching", rotateAfterStitching);
        return obj;
    }
            
    @Override
    public void setParameters(JSONObject obj) throws JSONException {
        super.setParameters(obj);
        fusionMethod=obj.getString("FusionMethod");
        regressionTh=obj.getDouble("RegressionThreshold");
        maxAvgDisplaceTh=obj.getDouble("MaxAvgDisplaceTh");
        absDisplaceTh=obj.getDouble("AbsDisplaceTh");
//        addTilesAsROIs=obj.getBoolean("AddTilesAsROIs");
        computeOverlap=obj.getBoolean("ComputeOverlap");
        invertX=obj.getBoolean("InvertX");
        invertY=obj.getBoolean("InvertY");
        subpixelAccuracy=obj.getBoolean("SubpixelAccuracy");
        downSample=obj.getBoolean("DownSample");
        compParams=obj.getString("ComputationParams");
        rotateAfterStitching=obj.getBoolean("RotateAfterStitching");
    }
         
    @Override
    public String getToolTipText() {
        String text="<html>Fusion method: " + fusionMethod+"<br>"
                + "Regression threshold: " + regressionTh + "<br>"
                + "Max average displacement threshold: " + maxAvgDisplaceTh + "<br>"
                + "Absolute displacement threshold: " + absDisplaceTh + "<br>"
                + "Compute overlap: " + computeOverlap + "<br>"
                + "Invert X coordinates: " + invertX + "<br>"
                + "Invert Y coordinates: " + invertY + "<br>"
                + "Subpixel accuracy: " + subpixelAccuracy + "<br>"
                + "Downsample: " + downSample + "<br>"
                + "Computation: " + compParams + "<br>"
                + "Rotate after stitching: " + rotateAfterStitching + "</html>";
        return text;
}
    
    private Rectangle2D getTileBounds(Rectangle2D.Double rect, double rad) {
        java.awt.geom.Area a= new java.awt.geom.Area(rect);
        AffineTransform rot=new AffineTransform();
        rot.rotate(rad);
        a.transform(rot);
        return a.getBounds2D();
    }
    
    @Override
    public List<File> processGroup(final Group<File> group) {
        IJ.log("-----");
        IJ.log(this.getClass().getName()+".processGroup: ");
        IJ.log("   Criteria: "+group.groupCriteria.toString());
        IJ.log("   Images: "+group.images.size()+" images");
        if (group.images!=null && group.images.size()>1) {

            Callable stitchingTask=new Callable<List<File>>() {

                @Override
                public List<File> call() {
                    jobNumber++;
                    IJ.log(    "processGroup: starting job # "+jobNumber);
            
                    List<File> stitchedFiles=new ArrayList<File>();
//                    stitchingInProgress=true;
                    String sourceImagePath=group.images.get(0).getParent();
                    sourceImagePath=new File (sourceImagePath).getParent();
                    try {
                        JSONObject meta=Utils.parseMetadata(group.images.get(0));
                        JSONObject summary=meta.getJSONObject(MMTags.Root.SUMMARY);
                        double cameraRot=summary.getDouble(ExtImageTags.DETECTOR_ROTATION);
                        String prefix = meta.getString(ExtImageTags.AREA_NAME);
                        long clusterIdx=meta.getLong(ExtImageTags.CLUSTER_INDEX);
                        if (clusterIdx != -1)
                            prefix=prefix+"-Cluster"+Long.toString(clusterIdx);
                        IJ.log("    StitchCluster: "+prefix);
//                        IJ.log("StitchClusterOld element(0) FileName: "+meta.getString("FileName"));

                        String frame="t"+Long.toString(meta.getLong(MMTags.Image.FRAME_INDEX))+"-";
                        String channel=meta.getString(MMTags.Image.CHANNEL_NAME)+"-";
                        Long noOfCh=summary.getLong(MMTags.Summary.CHANNELS);
                        Long noOfSlices=summary.getLong(MMTags.Summary.SLICES);
                        Long noOfFrames=summary.getLong(MMTags.Summary.FRAMES);

//                        int dim=2;
//                        long selectedSliceIndex=0;
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
                        double expectedWidth=0;
                        double expectedHeight=0;
                        try {
                            BufferedWriter writer;

                            writer = new BufferedWriter(new FileWriter(configFile));
                            String line = "# Define the number of dimensions we are working on\n"
                                    + "dim = " + Integer.toString(dim)
                                    + "\n\n"
                                    + "# Define the image coordinates\n";
                            writer.write(line);
                            int i=1;
                            Point2D.Double refPoint=null;
                            double minXPx=0;
                            double minYPx=0;
                            double maxXPx=0;
                            double maxYPx=0;
                            double widthPx=0;
                            double heightPx=0;
                            Rectangle2D boundsPx=null;
                            for (File e:group.images) {
                                IJ.log("    "+Integer.toString(i)+": "+e.getAbsolutePath());
                                meta=Utils.parseMetadata(e);
                                //need to convert from um into pixel coords if using grid stitching plugin
                                Long sliceIndex=meta.getLong(MMTags.Image.SLICE_INDEX);
//                                if (selectedSliceIndex == sliceIndex) {
                                    double xUM=meta.getDouble(MMTags.Image.XUM)/pixSize;
                                    double yUM=meta.getDouble(MMTags.Image.YUM)/pixSize;
                                    double zUM=meta.getDouble(MMTags.Image.ZUM)/pixSize;
                                    
                                    if (rotateAfterStitching) {
                                        widthPx=meta.getDouble(MMTags.Image.WIDTH);
                                        heightPx=meta.getDouble(MMTags.Image.HEIGHT);
                                        if (boundsPx==null) {
                                            boundsPx=getTileBounds(new Rectangle2D.Double(0,0,widthPx,heightPx), cameraRot);
                                            IJ.log("BOUNDS: "+boundsPx.toString());
                                            minXPx=xUM - (double)boundsPx.getWidth()/2;
                                            minYPx=yUM - (double)boundsPx.getHeight()/2;
                                            maxXPx=xUM + (double)boundsPx.getWidth()/2;
                                            maxYPx=yUM + (double)boundsPx.getHeight()/2;
                                        } else {
//                                                    boundsPx=getTileBounds(new Rectangle2D.Double(0,0,widthPx,heightPx),cameraRot);
                                            minXPx=Math.min(minXPx,xUM - (double)boundsPx.getWidth()/2);
                                            minYPx=Math.min(minYPx,yUM - (double)boundsPx.getHeight()/2);
                                            maxXPx=Math.max(maxXPx,xUM + (double)boundsPx.getWidth()/2);
                                            maxYPx=Math.max(maxYPx,yUM + (double)boundsPx.getHeight()/2);             
                                        }
                                    }   
                                    
                                    String ch=meta.getString((MMTags.Image.CHANNEL_NAME));
                                    if (!foundCh.contains(ch))
                                        foundCh.add(ch);
                                    if (i==1) {
                                        xMin=xUM;
                                        xMax=xUM;
                                        yMin=yUM;
                                        yMax=yUM;
                                        refPoint=new Point2D.Double(xUM,yUM);
                                    } else {
                                        if (cameraRot != FieldOfView.ROTATION_UNKNOWN) {
                                            Point2D p=Utils.rotatePoint(new Point2D.Double(xUM,yUM), refPoint, cameraRot);
                                            xUM=p.getX();
                                            yUM=p.getY();
                                        }
                                        xMin=xUM < xMin?xUM:xMin;
                                        xMax=xUM > xMax?xUM:xMax;
                                        yMin=yUM < yMin?yUM:yMin;
                                        yMax=yUM > yMax?yUM:yMax;
                                    }
            //                        double x=xUM/pixSize;
            //                        double y=yUM/pixSize;
            //                        double z=zUM/pixSize;
                                    String path=e.getParent();
                                  //  renameMetadataFile(path,"metadata.txt","metadata_backup");
                                    path=new File(path).getName();
                                    if (dim==2) {
                                        line = new File(path,e.getName()).getPath() + "; ; (" + Double.toString(xUM) + ", " + Double.toString(yUM) + ")\n";
                                    } else {
                                        line = new File(path,e.getName()).getPath() + "; ; (" + Double.toString(xUM) + ", " + Double.toString(yUM) + ", " + Double.toString(zUM)+ ")\n";

                                    }
                                    writer.write(line);
                                    i++;
//                                }

                            }    
                            writer.close();
                            boundsPx=getTileBounds(new Rectangle2D.Double(0,0,widthPx,heightPx), cameraRot);

                            expectedWidth=maxXPx-minXPx;
                            expectedHeight=maxYPx-minYPx;
                            
                        } catch (IOException ex) {
                            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                            IJ.log("    Error creating stitch config file");
                            return new ArrayList<File>();
                        }

                        IJ.log("    sourceImagePath: "+sourceImagePath);
                        IJ.log("    workDir: "+workDir);

                            //this is a hack to change the output directory for saving fused image and ROIs
                        String defaultDirectory="";
                        if (outputDir==null)
                            outputDir=sourceImagePath;
                        if (stitch_grid!=null) {
                            try {
                                Field f=stitch_grid.getClass().getDeclaredField("defaultOutputDirectory");
                                String lastDefaultOutputDirectory=(String)f.get(stitch_grid);
                                IJ.log("    defaultOutputDirectory: "+lastDefaultOutputDirectory);
                                f.set(null,"");
                                f=stitch_grid.getClass().getDeclaredField("defaultDirectory");
                                defaultDirectory=(String)f.get(stitch_grid);
                                IJ.log("    defaultDirectory: "+defaultDirectory);
                                f.set(null, outputDir);

                            } catch (NoSuchFieldException ex) {
                                IJ.log("    cannot find field"+ex);
                                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                            } catch (SecurityException ex) {
                                IJ.log("    security field"+ex);
                                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                            } catch (IllegalArgumentException ex) {
                                IJ.log("    illegal arg field"+ex);
                                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                            } catch (IllegalAccessException ex) {
                                IJ.log("    illegal access field"+ex);
                                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                            }                     
                        } else {
                            IJ.log("    stitch_grid == null");  
                        }    
                     //   IJ.runMacroFile("scripts/setOutput.bsh", sourceImagePath);
                        try {
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
                            IJ.log("after call runPlugin");
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
//                            + (addTilesAsROIs?"add_tiles_as_rois ":"")
                            + (subpixelAccuracy?"subpixel_accuracy ":"")
                            + (computeOverlap?"compute_overlap ":"")
                            + (invertX?"invert_x ":"")
                            + (invertY?"invert_y ":"")
                            + (downSample ? "downsample_tiles " : "")
                            + "ignore_z_stage "
                            + "computation_parameters=["+compParams+"] "
                            + "image_output=[Write to disk] ");
//                            + "output_directory="+workDir);
                            
                        } catch (Exception ex) {
                            IJ.log("    Stitching exception: "+ex);
                            return new ArrayList<File>();
                        }
/*                        for (File e:elements) {
                            String path=e.getParent();
                           // restoreMetadataFile(path,"metadata_backup","metadata.txt");                    
        //                    path=new File(path).getName();
                        }*/

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
                            for (int t=0; t<noOfFrames; t++) {
                            for (int j=0; j<noOfSlices; j++) {
                            for (int i=0; i<foundCh.size(); i++) {
                            //wait for result stitched image file
                                String name="img_t"+Integer.toString(t+1)+"_z"+Integer.toString(j+1)+"_c"+Integer.toString(i+1);
                                IJ.log("    processing result "+name);
//                                File resultFile=new File(outputDir, name);
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
//                                IJ.log("Retrieved imageProcessor");
                                //if compute_overlap, tiles may have been rotated (if camera rotation was not correctly set) 
                    /*            if (registerFile.exists()) {
                                    double angle=calculateRotation(configFile, registerFile);
                                    proc.rotate(angle);
                                }*/
                                if (rotateAfterStitching && cameraRot != FieldOfView.ROTATION_UNKNOWN) {
//                                    proc.rotate(-cameraRot);
                                    IJ.run(imp,"Rotate... ", "angle="+Double.toString(-cameraRot/Math.PI*180)+" grid=0 interpolation=Bicubic fill enlarge");
                                    int newWidth=imp.getWidth();
                                    int newHeight=imp.getHeight();
                                    IJ.log(newWidth+", "+newHeight+", "+expectedWidth + ", "+expectedHeight);
                                    imp.setRoi(new Roi((int)(newWidth-expectedWidth)/2,(int)(newHeight-expectedHeight)/2,(int)expectedWidth,(int)expectedHeight));
                                    IJ.run(imp, "Crop","");
                                }
                                ImageProcessor proc=imp.getProcessor();
                                TaggedImage ti=null;
                                try {
                                    //these are used to create display settings 
                                    //newMeta.put(MMTags.Summary.HEIGHT, imp.getHeight());
                                    if (storage==null) {
                                        JSONObject newSummary = new JSONObject(meta.getJSONObject(MMTags.Root.SUMMARY).toString());
                                        if (rotateAfterStitching && cameraRot != FieldOfView.ROTATION_UNKNOWN) {
                                            newSummary.put(ExtImageTags.DETECTOR_ROTATION, new Double(0));
                                        }
                                        newSummary.put(MMTags.Summary.SLICES, noOfSlices); 
                                        newSummary.put(MMTags.Summary.POSITIONS, 1); 
                                        newSummary.put(MMTags.Summary.CHANNELS, noOfCh); 
                                        newSummary.put(MMTags.Summary.FRAMES, noOfFrames); 
                                        newSummary.put(MMTags.Summary.WIDTH,imp.getWidth()); 
                                        newSummary.put(MMTags.Summary.HEIGHT,imp.getHeight()); 
                                        newSummary.put(MMTags.Summary.PREFIX,prefix); 
                                        newSummary.put(MMTags.Summary.DIRECTORY, workDir);
                                        newSummary.put(ExtImageTags.AREAS, 1);
                                        newSummary.put(ExtImageTags.CLUSTERS, 1);
                                        // ??newSummary.put(MMTags.POS_NAME, prefix);
                                        newSummary.remove("InitialPositionList");
                                        storage = new TaggedImageStorageDiskDefault (
                                            workDir,true,newSummary);
                                    }
        //                            IJ.log("after summary update");
                                    meta.put(MMTags.Image.HEIGHT, imp.getHeight());
                                    //newMeta.put(MMTags.Summary.WIDTH, imp.getWidth());
                                    meta.put(MMTags.Image.WIDTH, imp.getWidth());
                                    meta.put(MMTags.Image.POS_NAME,prefix);
                                    meta.put(MMTags.Image.POS_INDEX,0);
                                    meta.put(MMTags.Image.CHANNEL_NAME, chNames.get(i));
                                    meta.put(MMTags.Image.CHANNEL_INDEX,i);
                                    meta.put(MMTags.Image.XUM,pixSize * (xMin+(xMax-xMin)/2));
                                    meta.put(MMTags.Image.YUM,pixSize * (yMin+(yMax-yMin)/2));
                                    meta.put(MMTags.Image.SLICE_INDEX,j);
                                    meta.put(MMTags.Image.SLICE,j);
                                    meta.put(MMTags.Image.FRAME_INDEX,t);
                                    meta.put(MMTags.Image.FRAME,t);
                                    meta.put(ExtImageTags.AREA_INDEX,0);
//                                    meta.put(ExtImageTags.SITE_INDEX,currentClusterIndex);
                                    meta.put(ExtImageTags.SITE_INDEX,0);
//                                    meta.put(ExtImageTags.CLUSTER_INDEX,currentClusterIndex);
                                    meta.put(ExtImageTags.CLUSTER_INDEX,0);
                                    meta.put(ExtImageTags.CLUSTERS_IN_AREA, 1);
                                    meta.put(ExtImageTags.SITES_IN_AREA, 1);
        //                            IJ.log("xMin(pix): "+Double.toString(xMin)+", xMax(pix): "+Double.toString(xMax)+", yMin(pix): "+Double.toString(yMin)+", yMax(pix): "+Double.toString(yMax));
        //                            IJ.log("xMax(pix)-xMin(pix): "+Double.toString(xMax-xMin)+", yMax(pix)-yMin(pix): "+Double.toString(yMax-yMin)+", imp.getWidth()-->um="+Double.toString(imp.getWidth()*pixSize)+", imp.getHeight()-->um="+Double.toString(imp.getHeight()*pixSize));
        //                            IJ.log("Creating new TaggedImage");
                                    ti=new TaggedImage(proc.getPixels(),meta);
                                    //ti.tags.put("FileName", stitchedFile.getName());
                                    //JSONObject summary=newMeta.getJSONObject(MMTags.Root.SUMMARY);
                                    storage.putImage(ti);
                                    stitchedFiles.add(new File(new File(workDir,prefix), ti.tags.getString("FileName")));
                                    IJ.log("    "+stitchedFiles.get(stitchedFiles.size()-1).getAbsolutePath());
                                } catch (Exception ex) {
                                    IJ.log("    Problem saving to storage: "+ti.tags.getString("FileName"));
                                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                                }
                                resultFile.delete();

                            }
                            }
                            }
                            storage.close();
                            IJ.log("    Stitching successful");
//                            stitchingInProgress=false;
                        } catch (IOException ex) {
                            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                            IJ.log(this.getClass().getName()+"    Error saving stitched results");
//                            stitchingInProgress=false;
                        }
                    } catch (JSONException ex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                        IJ.log("    Error parsing metadata");
//                        stitchingInProgress=false;
                    } 
                    IJ.log("    "+this.getClass().getName()+": finished job # "+jobNumber);
                    IJ.log("----");
                    return stitchedFiles;
                }
            };
            Future<List<File>> future=executor.submit(stitchingTask);
            try {
                List<File> returnList=future.get();
                return returnList;
            } catch (InterruptedException ex) {
                return new ArrayList<File>();
            } catch (ExecutionException ex) {
                return new ArrayList<File>();
            }
        } else {
            return new ArrayList<File>();
        }
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
/*            
        l=new JLabel("Add tiles as ROIs:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        JCheckBox addTilesAsRoisCB = new JCheckBox();
        addTilesAsRoisCB.setAlignmentX(Component.RIGHT_ALIGNMENT);
        addTilesAsRoisCB.setSelected(addTilesAsROIs);
        optionPanel.add(addTilesAsRoisCB);
*/
        l=new JLabel("Compute overlap:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        JCheckBox computeOverlapCB = new JCheckBox();
        computeOverlapCB.setAlignmentX(Component.RIGHT_ALIGNMENT);
        computeOverlapCB.setSelected(computeOverlap);
        optionPanel.add(computeOverlapCB);

        l=new JLabel("Invert X coordinates:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        JCheckBox invertXCB = new JCheckBox();
        invertXCB.setAlignmentX(Component.RIGHT_ALIGNMENT);
        invertXCB.setSelected(invertX);
        optionPanel.add(invertXCB);

        l=new JLabel("Invert Y coordinates:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        JCheckBox invertYCB = new JCheckBox();
        invertYCB.setAlignmentX(Component.RIGHT_ALIGNMENT);
        invertYCB.setSelected(invertY);
        optionPanel.add(invertYCB);

        l=new JLabel("Subpixel accuracy:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        JCheckBox subpixelCB = new JCheckBox();
        subpixelCB.setAlignmentX(Component.RIGHT_ALIGNMENT);
        subpixelCB.setSelected(subpixelAccuracy);
        optionPanel.add(subpixelCB);
        
        l=new JLabel("Downsample tiles:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        JCheckBox downsampleCB = new JCheckBox();
        downsampleCB.setAlignmentX(Component.RIGHT_ALIGNMENT);
        downsampleCB.setSelected(downSample);
        optionPanel.add(downsampleCB);

        l=new JLabel("Computation Parameters:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        String[] compParamOptions=new String[]
                {"Save memory (but be slower)",
                "Save computation time (but use more RAM)"};
        JComboBox compParamCombo=new JComboBox(compParamOptions);
        compParamCombo.setSelectedItem(compParams);
        optionPanel.add(compParamCombo);
        
        l=new JLabel("Rotate after stitching:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        JCheckBox rotateAfterStitchCB = new JCheckBox();
        rotateAfterStitchCB.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rotateAfterStitchCB.setSelected(rotateAfterStitching);
        optionPanel.add(rotateAfterStitchCB);
        
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
//            addTilesAsROIs=addTilesAsRoisCB.isSelected();
            computeOverlap=computeOverlapCB.isSelected();
            invertX=invertXCB.isSelected();
            invertY=invertYCB.isSelected();
            subpixelAccuracy=subpixelCB.isSelected();
            downSample=downsampleCB.isSelected();
            compParams=(String)compParamCombo.getSelectedItem();
            rotateAfterStitching=rotateAfterStitchCB.isSelected();
        }    
    }

    @Override
    protected boolean acceptElement(File element) {
        return true;
    }
    
}
