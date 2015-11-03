package autoimage.tools;

import autoimage.FieldOfView;
import autoimage.api.ILiveListener;
import autoimage.MMCoreUtils;
import autoimage.Utils;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.table.DefaultTableModel;
import mmcorej.CMMCore;
import mmcorej.StrVector;
import org.micromanager.api.MMListenerInterface;
import org.micromanager.api.ScriptInterface;

/**
 *
 * @author Karsten Siller
 */
public class CameraRotDlg extends javax.swing.JDialog implements ILiveListener, MMListenerInterface {

    private CMMCore core;
    private final ScriptInterface gui;
    private String lastMeasuredDetector;
    private static String configGroupStr ="";
    private static String channelName = "";
    private static double exposure;
    private static double stageStepSize; // stage movement in um
    private static int iterations;
    private static boolean showImages=false;
    private boolean isMeasuring;
    private CameraRotationTask rotMeasureTask;
    private Map<String, Measurement> measurements;//stores camera rotation(in rad) and pixelsize
    
    private static int maxIterations=10;
    private static double toleratedAngleDisp=2;//degree
    private static double toleratedPixSizeDisp=0.05; //%

    

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
    }

    @Override
    public void pixelSizeChangedAlert(double d) {
        if (isShowing()) {
            JOptionPane.showMessageDialog(this,"Pixel size has changed. Ensure that the selected 'step size'\nis smaller than the width and height of a single field of view.","Camera Rotation Measurement",JOptionPane.OK_OPTION);
        }
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

    @Override
    public void slmExposureChanged(String string, double d) {
    }
    
    public class Measurement {
        double cameraAngle;
        double pixelSize;
        
        public Measurement() {
            this (0,-1);
        }
        
        public Measurement(double ca, double ps) {
            cameraAngle=ca;
            pixelSize=ps;
        }
        
        public double getCameraAngle() {
            return cameraAngle;
        }
        
        public double getPixelSize() {
            return pixelSize;
        }
    }



    //LiveListener
    @Override
    public void liveModeChanged(boolean isLive) {
        liveButton.setText(isLive ? "Stop Live" : "Live");
        measureButton.setEnabled(!isLive);
    }
    
    private class CameraRotationTask extends SwingWorker<Boolean,Object[]> {

        private String detectorName="";
        
        @Override
        protected Boolean doInBackground() {
            try {
                exposureField.commitEdit();
            } catch (ParseException ex) {
                Logger.getLogger(CameraRotDlg.class.getName()).log(Level.SEVERE, null, ex);
            }
            exposure=(Double)exposureField.getValue();
            String xyStageName=core.getXYStageDevice();
            double xPos;
            double yPos;
            ImagePlus imp1=null;
            ImagePlus imp2=null;
            ImagePlus vimp=null;
            ImagePlus himp=null;
            try {
                xPos = core.getXPosition(xyStageName);
                yPos = core.getYPosition(xyStageName);
                DefaultTableModel model=(DefaultTableModel)resultTable.getModel();
                int rows=model.getRowCount();
                List<Point2D> stagePosList = new ArrayList<Point2D>();
                int iterations=(Integer)iterationsComboBox.getSelectedItem();
                for (int i=0; i<iterations; i++) {
                    double x=xPos+(Math.random()-0.5)*2*stageStepSize;
                    double y=yPos+(Math.random()-0.5)*2*stageStepSize;
                    stagePosList.add(new Point2D.Double(x,y));
                }
                int index=rows+1;

                ImageStack hStack=null;
                ImageStack vStack=null;
                int maxWidth_H=0;
                int maxHeight_H=0;
                int maxWidth_V=0;
                int maxHeight_V=0;
                int iteration=0;
                Overlay overlay_h=new Overlay();
                Overlay overlay_v=new Overlay();
                imp1=ij.WindowManager.getImage("Camera_Rotation:_image_1");
                if (imp1!=null) {
                    imp1.close();
                    imp1=null;
                }    
                imp2=ij.WindowManager.getImage("Camera_Rotation:_image_2");
                if (imp2!=null) {
                    imp2.close();
                    imp2=null;
                }    
                for (Point2D stagePos:stagePosList) {
                    if (isCancelled()) {
                        if (imp1!=null)
                            imp1.close();
                        if (imp2!=null)
                            imp2.close();
                        return null;
                    }    
                    //horizontal step along x-axis to right
                    core.setXYPosition(xyStageName, stagePos.getX(),stagePos.getY());
                    imp1=MMCoreUtils.snapImagePlus(core, configGroupStr, channelName, exposure,0,MMCoreUtils.SCALE_NONE);
                    detectorName=core.getCameraDevice();
                    if (imp1==null) {
                        return false;
                    }
                    if (imp1.getProcessor() instanceof ColorProcessor) {
                        imp1=new CompositeImage(imp1, CompositeImage.COMPOSITE);
                    }
                    imp1.setTitle("Camera_Rotation:_image_1"); 
                    if (hStack==null) {
                        hStack=new ImageStack(imp1.getWidth()*2, imp1.getHeight()*2);
                    }
                    imp1.show();
                    imp1.getCanvas().zoomOut(0, 0);
                    imp1.getCanvas().zoomOut(0, 0);
                    if (isCancelled()) {
                        if (imp1!=null)
                            imp1.close();
                        if (imp2!=null)
                            imp2.close();
                        return null;
                    }    
                    core.setXYPosition(xyStageName, stagePos.getX()+stageStepSize,stagePos.getY());
                    imp2=MMCoreUtils.snapImagePlus(core, configGroupStr, channelName, exposure,0,MMCoreUtils.SCALE_NONE);
                    if (imp2==null) {
                        return false;
                    }                    
                    if (imp2.getProcessor() instanceof ColorProcessor) {
                        imp2=new CompositeImage(imp2, CompositeImage.COMPOSITE);
                    }                    
                    imp2.setTitle("Camera_Rotation:_image_2");  
                    imp2.show();
                    imp2.getCanvas().zoomOut(0,0);
                    imp2.getCanvas().zoomOut(0,0);
                    if (isCancelled()) {
                        if (imp1!=null)
                            imp1.close();
                        if (imp2!=null)
                            imp2.close();
                        return null;
                    }   
                    IJ.run("Pairwise stitching", "first_image=Camera_Rotation:_image_1 second_image=Camera_Rotation:_image_2 fusion_method=[Linear Blending] check_peaks=5 compute_overlap subpixel_accuracy x=0.0000 y=0.0000 registration_channel_image_1=[Only channel 1] registration_channel_image_2=[Only channel 1]");
                    ImagePlus stitched=ij.WindowManager.getCurrentImage();
                    if (showImages) {
                        int channels=1;
                        if (stitched.isComposite()) {
                            channels=((CompositeImage)stitched).getNChannels();
                        }
                        for (int ch=0; ch<channels; ch++) {
                            ImageProcessor stitchedIp;
                            if (stitched.isComposite()) {
                                stitchedIp=((CompositeImage)stitched).getProcessor(ch+1);
                            } else {
                                stitchedIp=stitched.getProcessor();
                            }
                            ImageProcessor stackIp=null;
                            if (stitchedIp instanceof ByteProcessor) {
                                stackIp=new ByteProcessor(imp1.getWidth()*2,imp1.getHeight()*2);
                            } else if (stitchedIp instanceof ShortProcessor) {
                                stackIp=new ShortProcessor(imp1.getWidth()*2,imp1.getHeight()*2);
                            } else if (stitchedIp instanceof ColorProcessor) {
                                stackIp=new ColorProcessor(imp1.getWidth()*2,imp1.getHeight()*2);
                            }
                            if (stackIp!=null) {
                                int width=stitchedIp.getWidth();
                                maxWidth_H=Math.max(width,maxWidth_H);
                                maxHeight_H=Math.max(stitchedIp.getHeight(),maxHeight_H);
                                for (int row=0; row<stitchedIp.getHeight(); row++) {
                                    int[] rowPixel=new int[width];
                                    stitchedIp.getRow(0, row, rowPixel, width);
                                    stackIp.putRow(0, row, rowPixel, width);
                                }
                                hStack.addSlice("Iteration "+Integer.toString(index),stackIp);   
                            }
                        }
                    }
                    stitched.close();
                    
                    //parse log for translation vector
                    String log=IJ.getLog();
                    int pos=log.lastIndexOf("shift (second relative to first)")+35;
                    String transl=log.substring(pos,log.indexOf(")", pos));
                    String xStr=transl.substring(0,transl.indexOf(","));
                    String yStr=transl.substring(transl.indexOf(",")+1);
                    double deltaX=Double.parseDouble(xStr);
                    double deltaY=Double.parseDouble(yStr);
                    double pixSizeH=stageStepSize/Math.sqrt(deltaX*deltaX + deltaY*deltaY);
                    //calculate angle in degree for table
                    double angleH = Utils.angle2D_Rad(new Point2D.Double(deltaX,deltaY), new Point2D.Double(stageStepSize,0))/Math.PI*180;

                    double roi1x;
                    double roi1y;
                    double roi2x;
                    double roi2y;
                    if (deltaX>=0) {
                        roi1x = 0;
                        roi2x=deltaX;
                    } else {
                        roi1x = -deltaX;
                        roi2x = 0;
                    }    
                    if (deltaY>=0) {
                        roi1y = 0;
                        roi2y=deltaY;
                    } else {
                        roi1y = -deltaY;
                        roi2y = 0;
                        angleH=-angleH;
                    }    
                    Roi roi1=new Roi(roi1x,roi1y,imp1.getWidth(),imp1.getHeight());
                    roi1.setPosition(iteration);
                    roi1.setStrokeColor(Color.CYAN);
                    roi1.setName("Roi 1");
                    overlay_h.add(roi1);
                    Roi roi2=new Roi(roi2x,roi2y,imp2.getWidth(),imp2.getHeight());
                    roi2.setPosition(iteration);
                    roi2.setStrokeColor(Color.MAGENTA);
                    roi2.setName("Roi 2");
                    overlay_h.add(roi2);

                    //store values in resultTable
                    Object[] data=new Object[9];
                    data[0]=new Integer(index);
                    data[1]=new Double(angleH);
                    data[3]=new Double(pixSizeH);
                    data[5]=new Double(deltaX);
                    data[6]=new Double(deltaY);

                    if (imp1!=null) 
                        imp1.close();
                    if (imp2!=null) 
                        imp2.close();                    
                    if (isCancelled()) {
                        return null;
                    }    
                   //vertical step along y-axis down
                    core.setXYPosition(xyStageName, stagePos.getX(),stagePos.getY());
                    imp1=MMCoreUtils.snapImagePlus(core, configGroupStr, channelName, exposure,0,MMCoreUtils.SCALE_NONE);
                    if (imp1==null) {//ipArray1==null || ipArray1.length<1) {
                        return false;
                    }                    
                    if (imp1.getProcessor() instanceof ColorProcessor) {
                        imp1=new CompositeImage(imp1, CompositeImage.COMPOSITE);
                    }                    
                    imp1.setTitle("Camera_Rotation:_image_1");
                    imp1.show();
                    imp1.getCanvas().zoomOut(0, 0);
                    imp1.getCanvas().zoomOut(0, 0);
                    if (vStack==null) {
                        vStack=new ImageStack(imp1.getWidth()*2, imp1.getHeight()*2);
                    }                
                    if (isCancelled()) {
                        if (imp1!=null)
                            imp1.close();
                        if (imp2!=null)
                            imp2.close();
                        return null;
                    }    
                    core.setXYPosition(xyStageName, stagePos.getX(),stagePos.getY()+stageStepSize);
                    imp2=MMCoreUtils.snapImagePlus(core, configGroupStr, channelName, exposure,0,MMCoreUtils.SCALE_NONE);
                    if (imp2==null) {
                        return false;
                    }                    
                    if (imp2.getProcessor() instanceof ColorProcessor) {
                        imp2=new CompositeImage(imp2, CompositeImage.COMPOSITE);
                    }                    
                    imp2.setTitle("Camera_Rotation:_image_2"); 
                    imp2.show();
                    imp2.getCanvas().zoomOut(0, 0);
                    imp2.getCanvas().zoomOut(0, 0);
                    if (isCancelled()) {
                        if (imp1!=null)
                            imp1.close();
                        if (imp2!=null)
                            imp2.close();
                        return null;
                    }    
                    IJ.run("Pairwise stitching", "first_image=Camera_Rotation:_image_1 second_image=Camera_Rotation:_image_2 fusion_method=[Linear Blending] check_peaks=5 compute_overlap subpixel_accuracy x=0.0000 y=0.0000 registration_channel_image_1=[Only channel 1] registration_channel_image_2=[Only channel 1]");
                    
                    stitched=ij.WindowManager.getCurrentImage();
                    if (showImages) {
                        int channels=1;
                        if (stitched.isComposite()) {
                            channels=((CompositeImage)stitched).getNChannels();
                        }
                        for (int ch=0; ch<channels; ch++) {
                            ImageProcessor stitchedIp;
                            if (stitched.isComposite()) {
                                stitchedIp=((CompositeImage)stitched).getProcessor(ch+1);
                            } else {
                                stitchedIp=stitched.getProcessor();
                            }
                            ImageProcessor stackIp=null;
                            if (stitchedIp instanceof ByteProcessor) {
                                stackIp=new ByteProcessor(imp1.getWidth()*2,imp1.getHeight()*2);
                            } else if (stitchedIp instanceof ShortProcessor) {
                                stackIp=new ShortProcessor(imp1.getWidth()*2,imp1.getHeight()*2);
                            } else if (stitchedIp instanceof ColorProcessor) {
                                stackIp=new ColorProcessor(imp1.getWidth()*2,imp1.getHeight()*2);
                            }
                            if (stackIp!=null) {
                                int width=stitchedIp.getWidth();
                                maxWidth_V=Math.max(width,maxWidth_V);
                                maxHeight_V=Math.max(stitchedIp.getHeight(),maxHeight_V);
                                for (int row=0; row<stitchedIp.getHeight(); row++) {
                                    int[] rowPixel=new int[width];
                                    stitchedIp.getRow(0, row, rowPixel, width);
                                    stackIp.putRow(0, row, rowPixel, width);
                                }
                                vStack.addSlice("Iteration "+Integer.toString(index),stackIp);   
                            }
                        }
                    }
                    stitched.close();


                    //parse log for translation vector
                    log=IJ.getLog();
                    pos=log.lastIndexOf("shift (second relative to first)")+35;
                    transl=log.substring(pos,log.indexOf(")", pos));
                    xStr=transl.substring(0,transl.indexOf(","));
                    yStr=transl.substring(transl.indexOf(",")+1);
                    deltaX=Double.parseDouble(xStr);
                    deltaY=Double.parseDouble(yStr);
                    double pixSizeV=stageStepSize/Math.sqrt(deltaX*deltaX + deltaY*deltaY);
                    //calculate angle in degree for table
                    double angleV = Utils.angle2D_Rad(new Point2D.Double(deltaX,deltaY), new Point2D.Double(0,stageStepSize))/Math.PI*180;
                    if (deltaX>=0) {
                        roi1x = 0;
                        roi2x=deltaX;
                        angleV=-angleV;
                    } else {
                        roi1x = -deltaX;
                        roi2x = 0;
                    }    
                    if (deltaY>=0) {
                        roi1y = 0;
                        roi2y=deltaY;
                    } else {
                        roi1y = -deltaY;
                        roi2y = 0;
                    }    
                    roi1=new Roi(roi1x,roi1y,imp1.getWidth(),imp1.getHeight());
                    roi1.setPosition(iteration);
                    roi1.setStrokeColor(Color.CYAN);
                    roi1.setName("Roi 1");
                    overlay_v.add(roi1);
                    roi2=new Roi(roi2x,roi2y,imp2.getWidth(),imp2.getHeight());
                    roi2.setPosition(iteration);
                    roi2.setStrokeColor(Color.MAGENTA);
                    roi2.setName("Roi 2");
                    overlay_v.add(roi2);

                    //store values in resultTable
                    data[2]=new Double(angleV);
                    data[4]=new Double(pixSizeV);
                    data[7]=new Double(deltaX);
                    data[8]=new Double(deltaY);
                    if (imp1!=null)
                        imp1.close();
                    if (imp2!=null)
                        imp2.close();
                    publish(data);
                    index++;
                    iteration++;
                }
                if (showImages) {
                    ImagePlus impH=new ImagePlus("Horizontal",hStack);
                    impH.setDimensions(hStack.getSize()/iterations, 1, iterations);
                    himp=new CompositeImage(impH,CompositeImage.COMPOSITE);                  
                    himp.setRoi(0, 0, maxWidth_H, maxHeight_H);
                    IJ.run(himp,"Crop","");
                    himp.setOverlay(overlay_h);
                    himp.show();
                    himp.getCanvas().zoomOut(0, 0);
                    himp.getCanvas().zoomOut(0, 0);

                    ImagePlus impV=new ImagePlus("Vertical",vStack);
                    impV.setDimensions(vStack.getSize()/iterations, 1, iterations);
                    vimp=new CompositeImage(impV,CompositeImage.COMPOSITE);
                    vimp.setRoi(0, 0, maxWidth_V, maxHeight_V);
                    IJ.run(vimp,"Crop","");
                    vimp.setOverlay(overlay_v);
                    vimp.show();
                    vimp.getCanvas().zoomOut(0, 0);
                    vimp.getCanvas().zoomOut(0, 0);
                }
                lastMeasuredDetector=core.getCameraDevice();
            } catch (Exception ex) {
                IJ.log(CameraRotDlg.class.getName()+": Exception - "+ex.getMessage());
                isMeasuring=false;
                progressBar.setValue(0);
                progressBar.setString("");
                measureButton.setText("Measure");
                liveButton.setEnabled(true);
                if (imp1!=null)
                    imp1.close();
                if (imp2!=null)
                    imp2.close();
                if (vimp!=null) 
                    vimp.close();
                if (himp!=null)
                    himp.close();
                if (!measurements.containsKey(detectorName)) {
                    measurements.put(detectorName,new Measurement(FieldOfView.ROTATION_UNKNOWN,-1));
                }
                return false;
            }
            return true;
        }
    
        @Override
        protected void process(List<Object[]> measurements) {
            DefaultTableModel model=(DefaultTableModel)resultTable.getModel();
            for (Object[] row:measurements) {
                model.addRow(row);
            }    
            int progress=progressBar.getValue()+1;
            progressBar.setValue(progress);
            progressBar.setString(Integer.toString(progress)+"/"+Integer.toString(progressBar.getMaximum()));
            
            double minH=0;
            double minV=0;
            double maxH=0;
            double maxV=0;
            List<Double> angleHList=new ArrayList<Double>();
            List<Double> angleVList=new ArrayList<Double>();
            for (int row=0; row<model.getRowCount(); row++) {
                double angleH=(Double)model.getValueAt(row, 1);
                double angleV=(Double)model.getValueAt(row, 2);        
                if (row==0) {
                    minH=angleH;
                    maxH=angleH;
                    minV=angleV;
                    maxV=angleV;
                } else {
                    minH=Math.min(minH,angleH);
                    maxH=Math.max(maxH,angleH);
                    minV=Math.min(minV,angleV);
                    maxV=Math.max(maxV,angleV);
                }
                angleHList.add(angleH);
                angleVList.add(angleV);
            }
            List<Double> radHList=new ArrayList<Double>();
            for (Double angle:angleHList) {
                radHList.add(new Double(angle/180*Math.PI));
            }
            List<Double> radVList=new ArrayList<Double>();
            for (Double angle:angleVList) {
                radVList.add(new Double(angle/180*Math.PI));
            }
            double medianAngleH=Utils.MedianDouble(radHList)/Math.PI*180;
            if (medianAngleH>180) medianAngleH=medianAngleH-360;
            double medianAngleV=Utils.MedianDouble(radVList)/Math.PI*180;
            if (medianAngleV>180) medianAngleV=medianAngleV-360;
            minAngleLabel.setText(String.format("%1$,.1f",minH)+" (H); "+String.format("%1$,.1f",minV)+" (V)");
            maxAngleLabel.setText(String.format("%1$,.1f",maxH)+" (H); "+String.format("%1$,.1f",maxV)+" (V)");
            medianAngleLabel.setText(String.format("%1$,.1f",medianAngleH)+" (H); "+String.format("%1$,.1f",medianAngleV)+" (V)");
            
            String result="";
            //currently, we're ignoring pixel size
            double difference = Math.abs((medianAngleH - medianAngleV + 180) % 360) - 180;
            if (Math.abs(difference) > toleratedAngleDisp || Utils.isNaN(medianAngleH) || Utils.isNaN(medianAngleV)) {
                CameraRotDlg.this.measurements.put(detectorName,new Measurement(FieldOfView.ROTATION_UNKNOWN, -1));
                result="<html><p>Warning: Angles not defined or measured angles using horizontal and vertical stage displacement show more than "+toleratedAngleDisp+" degree disparity.</p></html>";
            } else {
                double angle=((medianAngleH + medianAngleV) / 2)/180*Math.PI;
                CameraRotDlg.this.measurements.put(detectorName,new Measurement(angle,-1));
                double cameraAngleDeg=(medianAngleH + medianAngleV) / 2;
                if (cameraAngleDeg > 180) cameraAngleDeg=cameraAngleDeg-360;
                result = "<html><p>Camera rotation angle: "+String.format("%1$,.1f",cameraAngleDeg)+" degree.</p></html>";
            }
            messageLabel.setText(result);
        }
        
        @Override
        protected void done() {
            isMeasuring=false;
            progressBar.setValue(0);
            progressBar.setString("");
            measureButton.setText("Measure");
            liveButton.setEnabled(true);
        }
    }
    
    public CameraRotDlg(java.awt.Frame parent, final ScriptInterface gui, String chGroupStr, double stepSize, boolean modal) {
        super(parent, modal);
        initComponents();
        lastMeasuredDetector="";
        resultTable.getTableHeader().setReorderingAllowed(false);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        measurements=new HashMap<String,Measurement>();
        rotMeasureTask=null;
        this.gui=gui;
        core=gui.getMMCore();

        channelComboBox.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    IJ.log("itemchanged");
                    channelName=(String)channelComboBox.getSelectedItem();
                    configGroupStr=(String)configGroupComboBox.getSelectedItem();
                    try {
                        core.setConfig(configGroupStr, channelName);
                        detectorLabel.setText(core.getCameraDevice());
                    } catch (Exception ex) {
                        Logger.getLogger(CameraRotDlg.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });

        //load available config groups
        configGroupStr="";
        configGroupComboBox.removeAllItems();
        StrVector configs = core.getAvailableConfigGroups();
        for (int i = 0; i < configs.size(); i++) {
            configGroupComboBox.addItem(configs.get(i));
        }
        configGroupComboBox.setSelectedItem(chGroupStr);  
        
        setStageStepSize(stepSize);
        exposure=100;
        exposureField.setValue(exposure);
        iterationsComboBox.removeAllItems();
        progressBar.setString("");
        progressBar.setStringPainted(true);
        for (int i=1; i<=maxIterations; i++)
            iterationsComboBox.addItem(i);
        clearList();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                liveModeChanged(gui.isLiveModeOn());
            }
        });
    }

    public CameraRotDlg(java.awt.Frame parent, ScriptInterface gui, boolean modal) {
        this(parent,gui,configGroupStr,0, modal);
    }

    
    public void setTolerance(double angle) {
        toleratedAngleDisp=angle;
    }
    
    public void addOkButtonListener(ActionListener listener) {
        okButton.addActionListener(listener);
    }
    
    public void addCancelButtonListener(ActionListener listener) {
        cancelButton.addActionListener(listener);
    }
    
    public Map<String,Measurement> getResults() {
        return measurements;
    }
    
    public void setStageStepSize(double stepSize) {
        stageStepSize=stepSize;
        stepSizeField.setValue(stepSize);
    }
    
    public void setIterations(int it) {
        if (it>0 && it<maxIterations) {
            iterations=it;
            iterationsComboBox.setSelectedItem(it);
        }
    }
    
    public void setChannelGroup(String groupName) {
        if (Arrays.asList(core.getAvailableConfigGroups().toArray()).contains(groupName)) {
            configGroupStr=groupName;
            configGroupComboBox.setSelectedItem(groupName);
        }
    }
    
    private void setChannelList(StrVector chList) {
        channelComboBox.removeAllItems();
        if (chList!=null) {
            for (String ch:chList) {
                channelComboBox.addItem(ch);
            }    
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

        channelComboBox = new javax.swing.JComboBox();
        exposureField = new javax.swing.JFormattedTextField();
        configGroupComboBox = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        rotationAngleTable = new javax.swing.JScrollPane();
        resultTable = new javax.swing.JTable();
        measureButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        minAngleLabel = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        maxAngleLabel = new javax.swing.JLabel();
        medianAngleLabel = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        label = new javax.swing.JLabel();
        messageLabel = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        clearButton = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        iterationsComboBox = new javax.swing.JComboBox();
        jLabel9 = new javax.swing.JLabel();
        stepSizeField = new javax.swing.JFormattedTextField();
        progressBar = new javax.swing.JProgressBar();
        liveButton = new javax.swing.JButton();
        showImagesCB = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        detectorLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Camera Rotation");
        setMinimumSize(new java.awt.Dimension(603, 380));
        setResizable(false);

        channelComboBox.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        channelComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        exposureField.setText("jFormattedTextField1");
        exposureField.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N

        configGroupComboBox.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        configGroupComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        configGroupComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configGroupComboBoxActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel1.setText("Config Group:");

        jLabel2.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel2.setText("Channel/Preset:");

        jLabel3.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel3.setText("Exposure (ms):");

        resultTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "#", "Angle hor.", "Angle vert.", "Pix size hor.", "Pix size vert."
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        rotationAngleTable.setViewportView(resultTable);

        measureButton.setText("Measure");
        measureButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                measureButtonActionPerformed(evt);
            }
        });

        okButton.setText("Ok");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Calculated camera rotation"));

        minAngleLabel.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        minAngleLabel.setText("jLabel5");

        jLabel7.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel7.setText("Max:");

        maxAngleLabel.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        maxAngleLabel.setText("jLabel5");

        medianAngleLabel.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        medianAngleLabel.setText("jLabel6");

        jLabel8.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel8.setText("Median:");

        label.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        label.setText("Min:");

        messageLabel.setText("jLabel5");

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(label)
                    .addComponent(jLabel7)
                    .addComponent(jLabel8))
                .addGap(6, 6, 6)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(maxAngleLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)
                    .addComponent(minAngleLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(medianAngleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(6, 6, 6)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(messageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(label)
                                    .addComponent(minAngleLabel))
                                .addGap(6, 6, 6)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(maxAngleLabel)
                                    .addComponent(jLabel7))
                                .addGap(6, 6, 6)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(medianAngleLabel)
                                    .addComponent(jLabel8)))
                            .addComponent(messageLabel))
                        .addGap(6, 6, 6)))
                .addContainerGap())
        );

        clearButton.setText("Clear");
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel4.setText("Step size (um):");

        iterationsComboBox.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        iterationsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel9.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel9.setText("Iterations:");

        stepSizeField.setText("jFormattedTextField1");
        stepSizeField.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N

        liveButton.setText("Live");
        liveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                liveButtonActionPerformed(evt);
            }
        });

        showImagesCB.setText("Show result images");
        showImagesCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showImagesCBActionPerformed(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel5.setText("Detector:");

        detectorLabel.setText("---");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING))
                            .addComponent(jLabel4))
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(channelComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(configGroupComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(stepSizeField, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addComponent(jLabel3)
                            .addComponent(jLabel9))
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(exposureField, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(detectorLabel)
                            .addComponent(iterationsComboBox, 0, 1, Short.MAX_VALUE))
                        .addGap(0, 0, 0)
                        .addComponent(showImagesCB))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(rotationAngleTable, javax.swing.GroupLayout.DEFAULT_SIZE, 492, Short.MAX_VALUE)
                            .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(liveButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(measureButton, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                            .addComponent(clearButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(okButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(configGroupComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel5)
                    .addComponent(detectorLabel))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exposureField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel2)
                    .addComponent(channelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(iterationsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(stepSizeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(showImagesCB))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(liveButton)
                        .addGap(3, 3, 3)
                        .addComponent(measureButton)
                        .addGap(6, 6, 6)
                        .addComponent(clearButton)
                        .addGap(6, 6, 6)
                        .addComponent(okButton)
                        .addGap(6, 6, 6)
                        .addComponent(cancelButton))
                    .addComponent(rotationAngleTable, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(6, 6, 6)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void measureButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_measureButtonActionPerformed
        if (isMeasuring && rotMeasureTask!=null) {
            //request abort
            measureButton.setText("Measure");
            isMeasuring=false;
            rotMeasureTask.cancel(true);
            return;
        } else {
            channelName=(String)channelComboBox.getSelectedItem();
            configGroupStr=(String)configGroupComboBox.getSelectedItem();
            iterations=(Integer)iterationsComboBox.getSelectedItem();
            stageStepSize=(Double)stepSizeField.getValue();
            try {
                if (channelName == null) {
                    JOptionPane.showMessageDialog(this, "No channel selected.");
                    return;
                }
                if (configGroupStr == null) {
                    JOptionPane.showMessageDialog(this, "No channel group selected.");
                    return;
                }
                if (stageStepSize==0) {
                    JOptionPane.showMessageDialog(this, "Select step size larger than 0.");
                    return;                    
                }
                if (stageStepSize==0) {
                    JOptionPane.showMessageDialog(this, "Stage step size not set.");
                }
                
                isMeasuring=true;
                measureButton.setText("Stop Measure");
                liveButton.setEnabled(false);
                progressBar.setMaximum(iterations);
                progressBar.setValue(0);
                progressBar.setString("0/"+Integer.toString(iterations));
                if (!lastMeasuredDetector.equals(core.getCameraDevice())) {
                    this.clearList();
                }

                rotMeasureTask=new CameraRotationTask();
                rotMeasureTask.addPropertyChangeListener(new PropertyChangeListener() {

                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (StateValue.DONE == rotMeasureTask.getState()) {
                            try {
                                Boolean success=rotMeasureTask.get();
                                //success==true: do nothing
                                //success==null: cancelled, do nothing
                                if (success!=null && !success) {
                                    //problem snapping images or stitching library not installed
                                    JOptionPane.showMessageDialog(null,"Cannot snap images or image stitching library not found.");
                                }
                            } catch (InterruptedException ex) {
                                Logger.getLogger(CameraRotDlg.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (ExecutionException ex) {
                                Logger.getLogger(CameraRotDlg.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                });
                rotMeasureTask.execute();
                
            } catch (Exception ex) {
                Logger.getLogger(CameraRotDlg.class.getName()).log(Level.SEVERE, null, ex);
            }
        }    
    }//GEN-LAST:event_measureButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
//        setVisible(false);
        dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    private void configGroupComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configGroupComboBoxActionPerformed
        if (configGroupComboBox.getSelectedItem() != null) {
            configGroupStr=(String)configGroupComboBox.getSelectedItem();
            /* load available presets for the current config group.
               If the config group does not exist, user can select valid one from
               valid list. The newly selected config group is returned as String.
               The available presets for the config group are stored in public static variable in MMCoreUtils 
            */ 
            String newGroupStr=MMCoreUtils.verifyAndSelectConfigGroup(null, core, configGroupStr);
            if (!configGroupStr.equals(newGroupStr)) {
                //reload all available config groups
                configGroupComboBox.removeAllItems();
                StrVector configs = core.getAvailableConfigGroups();
                for (int i = 0; i < configs.size(); i++) {
                    configGroupComboBox.addItem(configs.get(i));
                }        
                configGroupComboBox.setSelectedItem(newGroupStr);
            }
            configGroupStr=(String)configGroupComboBox.getSelectedItem();
            StrVector presets=core.getAvailableConfigs(configGroupStr);
            setChannelList(presets);
        }
    }//GEN-LAST:event_configGroupComboBoxActionPerformed

    private void clearList() {
        DefaultTableModel model=(DefaultTableModel)resultTable.getModel();
        for (int row=model.getRowCount()-1; row>=0; row--) {
            model.removeRow(row);
        }
        minAngleLabel.setText("---");
        maxAngleLabel.setText("---");
        medianAngleLabel.setText("---");
        messageLabel.setText("");
    }
    
    private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
        clearList();
    }//GEN-LAST:event_clearButtonActionPerformed

    private void liveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_liveButtonActionPerformed
        if (gui.isAcquisitionRunning()) {
            JOptionPane.showMessageDialog(this, "Acquisition is running. Cannot switch to Live mode until acquisition is completed or stopped.", "Live Mode", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (liveButton.getText().equals("Live")) {
            if (gui.isLiveModeOn()) {
                JOptionPane.showMessageDialog(this, "Live mode is already running.", "Live Mode", JOptionPane.ERROR_MESSAGE);
            } else {
                configGroupStr=(String)configGroupComboBox.getSelectedItem();
                channelName=(String)channelComboBox.getSelectedItem();
                try {
                    exposureField.commitEdit();
                    exposure=(Double)exposureField.getValue();
                    try {
                        core.setExposure(exposure);
                        core.setConfig(configGroupStr, channelName);
                        gui.refreshGUI();
                    } catch (Exception e) {
                    }
                    //try to place snap window in front; OS-dependent: may or may not work 
                    if (gui.getSnapLiveWin()!=null)
                        gui.getSnapLiveWin().close();
                    gui.enableLiveMode(true);
/*                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            gui.getSnapLiveWin().toFront();
                            gui.getSnapLiveWin().repaint();
                        }
                    });*/
                } catch (ParseException ex) {
                    Logger.getLogger(CameraRotDlg.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            liveButton.setText("Stop");
        } else {
            gui.enableLiveMode(false);
        }
    }//GEN-LAST:event_liveButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
//        setVisible(false);
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void showImagesCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showImagesCBActionPerformed
        showImages=showImagesCB.isSelected();
    }//GEN-LAST:event_showImagesCBActionPerformed

    public void setShowImages(boolean b) {
        showImages=b;
        if (showImagesCB!=null)
            showImagesCB.setEnabled(b);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JComboBox channelComboBox;
    private javax.swing.JButton clearButton;
    private javax.swing.JComboBox configGroupComboBox;
    private javax.swing.JLabel detectorLabel;
    private javax.swing.JFormattedTextField exposureField;
    private javax.swing.JComboBox iterationsComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel label;
    private javax.swing.JButton liveButton;
    private javax.swing.JLabel maxAngleLabel;
    private javax.swing.JButton measureButton;
    private javax.swing.JLabel medianAngleLabel;
    private javax.swing.JLabel messageLabel;
    private javax.swing.JLabel minAngleLabel;
    private javax.swing.JButton okButton;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JTable resultTable;
    private javax.swing.JScrollPane rotationAngleTable;
    private javax.swing.JCheckBox showImagesCB;
    private javax.swing.JFormattedTextField stepSizeField;
    // End of variables declaration//GEN-END:variables
}
