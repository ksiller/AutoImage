/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage.dataprocessors;

import autoimage.ExtImageTags;
import autoimage.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.LUT;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import mmcorej.TaggedImage;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.acquisition.TaggedImageStorageDiskDefault;
import org.micromanager.api.MMTags;
import org.micromanager.utils.ImageUtils;

/**
 *
 * @author Karsten
 */
public class ZProjector<E> extends GroupProcessor<E> {
    
    private String projMethod;
    private boolean allSlices;
    private long startSlice;
    private long endSlice;

    private static ExecutorService executor=Executors.newSingleThreadExecutor();
    protected TaggedImageStorageDiskDefault storage=null;

    public ZProjector() {
        super("Z-Projector");
        List<String> criteria=new ArrayList<String>();
        criteria.add(MMTags.Image.POS_INDEX);
        criteria.add(MMTags.Image.CHANNEL_INDEX);
        criteria.add(MMTags.Image.FRAME_INDEX);
        criteria.add(ExtImageTags.AREA_INDEX);
        criteria.add(ExtImageTags.CLUSTER_INDEX);
        setCriteria(criteria);
        processIncompleteGrps=true;   
        
        projMethod="Max Intensity";
        allSlices=true;
        startSlice=0;
        endSlice=0;
    }
@Override
    public JSONObject getParameters() throws JSONException {
        JSONObject obj=super.getParameters();
        obj.put("ProjectionMethod", projMethod);
        obj.put("AllSlices", allSlices);
        obj.put("StartSlice", startSlice);
        obj.put("EndSlice", endSlice);
        return obj;
    }
            
    @Override
    public void setParameters(JSONObject obj) throws JSONException {
        super.setParameters(obj);
        projMethod=obj.getString("ProjectionMethod");
        allSlices=obj.getBoolean("AllSlices");
        startSlice=obj.getLong("StartSlice");
        endSlice=obj.getLong("EndSlice");        
    }
         
    @Override
    public String getToolTipText() {
        if (allSlices) {
            return "<html>Method: "+projMethod+"<br>"
                + "Slices: All</html>";
        } else {
            return "<html>Method: "+projMethod+"<br>"
                + "Slices: "+Long.toString(startSlice)+"-"+Long.toString(endSlice)+"</html>";
        }
    }
    
    @Override
    public List<E> processGroup(final Group<E> group) {
        IJ.log("-----");
        IJ.log(this.getClass().getName()+".processGroup: ");
        IJ.log("   Criteria: "+group.groupCriteria.toString());
        IJ.log("   Images: "+group.images.size()+" images");
        
        if (group!=null && group.images!=null && group.images.size()>1) {

            Callable projectionTask=new Callable<List<E>>() {

                @Override
                public List<E> call() {
                    List<E> results=new ArrayList<E>();
                    ImageStack stack=null;
                    LUT lut=null;
                    JSONObject meta=null;
                    try {
                        //read images and place in single stack
                        double zPos=0;
                        for (E image:group.images) {
                            ImagePlus imp;
//                            if (image instanceof java.io.File) {
                                File file=(File)image;
                                imp=IJ.openImage(file.getAbsolutePath());
                                IJ.log("ZProject result (File): opened "+file.getAbsolutePath());
                                meta=Utils.parseMetadata(file);
                                IJ.log("ZProject result (File): parsed metadata for "+file.getAbsolutePath());
//                            } 
                                /*else if (image instanceof TaggedImage) {
                                imp=Utils.createImagePlus((TaggedImage)image);
                                //important: create copy of metadata otherwise saving fails
                                meta=new JSONObject(((TaggedImage)image).tags.toString());
                            } else {//unknown image type
                                return results;
                            }*/
                            zPos+=meta.getDouble(MMTags.Image.ZUM);
                            if (stack==null) {
                                stack=imp.createEmptyStack();
                            }
                            ImageProcessor ip=imp.getProcessor();
                            lut=ip.getLut();
                            stack.addSlice(ip.duplicate());
                        }
                        zPos/=group.images.size();
                        IJ.log(lut.toString());
                        ImagePlus zProjImp=new ImagePlus("Z-Proj",stack);
                        ij.plugin.ZProjector projector=new ij.plugin.ZProjector(zProjImp);
                        if (projMethod.equals("Max Intensity")) {
                            projector.setMethod(ij.plugin.ZProjector.MAX_METHOD);
                        } else if (projMethod.equals("Min Intensity")) {
                            projector.setMethod(ij.plugin.ZProjector.MIN_METHOD);
                        } else if (projMethod.equals("Average Intensity")) {
                            projector.setMethod(ij.plugin.ZProjector.AVG_METHOD);
                        } else if (projMethod.equals("Sum Slices")) {
                            projector.setMethod(ij.plugin.ZProjector.SUM_METHOD);
                        } else if (projMethod.equals("Standard Deviation")) {
                            projector.setMethod(ij.plugin.ZProjector.SD_METHOD);
                        } else if (projMethod.equals("Median")) {
                            projector.setMethod(ij.plugin.ZProjector.MEDIAN_METHOD);
                        }    
                        projector.doProjection();
                        ImagePlus resultImp=projector.getProjection();

                        //create TaggedImage
//                        IJ.log("before create TaggedImage");
                        TaggedImage ti=ImageUtils.makeTaggedImage(resultImp.getProcessor());
//                        IJ.log("after create TaggedImage");

                        //update metadata
                        String newDir=new File(workDir).getParentFile().getAbsolutePath();                        
                        String newPrefix=new File(workDir).getName();
                        meta.put(MMTags.Image.SLICE_INDEX, 0);
                        meta.put(MMTags.Image.SLICE, 0);
//                        IJ.log("after updating summary data");
                        ti.tags=meta;
/*
                        if (group.images.get(0) instanceof TaggedImage) {
                            JSONObject summary = new JSONObject(meta.getJSONObject(MMTags.Root.SUMMARY).toString());
                            summary.put(MMTags.Summary.SLICES, 1); 
                            meta.put(MMTags.Root.SUMMARY, summary);
                            results.add((E)ti);
                            IJ.log("ZProject result (TAGGEDIMAGE): "+(new File(new File(workDir,meta.getString(MMTags.Image.POS_NAME)),
                                                    meta.getString("FileName"))).getAbsolutePath());
                        } else */
                        if (group.images.get(0) instanceof java.io.File) {
                            JSONObject summary = new JSONObject(meta.getJSONObject(MMTags.Root.SUMMARY).toString());
                            IJ.log(this.getClass().getName()+": Positions="+summary.getLong(MMTags.Summary.POSITIONS));
                            meta.put(MMTags.Image.ZUM, zPos);
                            summary.put(MMTags.Summary.SLICES, 1);
                            meta.put(MMTags.Root.SUMMARY, summary);
                            if (storage==null) {
                                summary.put(MMTags.Summary.DIRECTORY, newDir);
                                summary.put(MMTags.Summary.PREFIX, newPrefix);
//                                meta.put(MMTags.Root.SUMMARY, summary);
                                storage = new TaggedImageStorageDiskDefault(workDir,true,summary);
                            }
                            IJ.log("before putImage");
                            storage.putImage(ti);
                            IJ.log("after putImage");
                            results.add((E)new File(new File(workDir,meta.getString(MMTags.Image.POS_NAME)),
                                                    meta.getString("FileName")));                    
                            IJ.log("ZProject result (FILE): "+((File)results.get(0)).getAbsolutePath());
                        }
                        
                    } catch (JSONException ex) {
                        IJ.log(this.getClass().getName()+"Problem with processing group");
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                    } finally {
//                        group.processed=true;
                        return results;
                    }
                }
            };    
            Future<List<E>> future=executor.submit(projectionTask);
            try {
                List<E> returnList=future.get();
                return returnList;
            } catch (InterruptedException ex) {
                return new ArrayList<E>();
            } catch (ExecutionException ex) {
                return new ArrayList<E>();
            }
        } else {
            return new ArrayList<E>();
        }
    }
    
    @Override
    protected boolean acceptElement(E element) {
        try {
            JSONObject meta=Utils.parseMetadata((File)element);
            long slice=meta.getLong(MMTags.Image.SLICE_INDEX);
            IJ.log(this.getClass().getName()+": "+slice+", "+startSlice+", "+endSlice);
            if (startSlice==-1 || (slice >= startSlice && slice <=endSlice)) {
                IJ.log(this.getClass().getName()+": element accepted");
                return true;
            } else {
                IJ.log(this.getClass().getName()+": element rejected");
                return false;
            }    
        } catch (JSONException ex) {
            IJ.log(this.getClass().getName()+": error");
            return false;
        }
    }

    @Override
    public JSONObject updateTagValue(JSONObject meta, String newDir, String newPrefix, boolean updateSummary) throws JSONException {
        return meta;
    }

    @Override
    public void cleanUp() {
/*        IJ.log(this.getClass().getName()+".cleanUp: "+groupList.size()+" groups");
        for (Group<E> grp:groupList) {
            if (processIncompleteGrps && !grp.processed) {
                List<E> modifiedElements=processGroup(grp);
                if (modifiedElements!=null) { //image processing was successful
                        //analyzers that form nodes need to place processed image in 
                        //modifiedOutput_ queue
                        //for last analyzer in tree branch (='leaf') modifiedOutput_ = null,
                        //which is handled in produceModified method

                    for (E element:modifiedElements)
                        produceModified(element);
                }
            }
            grp.images.clear();
        }
        clearGroups();*/
        super.cleanUp();
        if (storage != null) {
            storage.close();
            storage=null;
        } else {
            //if storage == null --> TaggedImage
            //need to modify 'SLICES' in all metadata summary files
        }
    }

    @Override
    protected long determineMaxGroupSize(JSONObject meta) throws JSONException {
        long max=-1;
        try {
            JSONObject summary=meta.getJSONObject(MMTags.Root.SUMMARY);
            max=summary.getLong(MMTags.Summary.SLICES);
            max=1+Math.min(max,endSlice)-startSlice;
            if (max<-1)
                max=-1;
        } catch (JSONException jse) {
            max=(long)meta.getInt(MMTags.Summary.SLICES);
        }
        return max;
    }

    @Override
    public void makeConfigurationGUI() {
        JPanel optionPanel = new JPanel();
        GridLayout layout = new GridLayout(0,2);
        optionPanel.setLayout(layout);
        
        final JLabel startLabel=new JLabel("Start slice:",JLabel.RIGHT);
        startLabel.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        final JSpinner startSpinner = new JSpinner(new SpinnerNumberModel(
            startSlice, //initial value
            0, //min
            1000, //max
            1));
        final JLabel endLabel=new JLabel("End slice:",JLabel.RIGHT);
        endLabel.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        final JSpinner endSpinner = new JSpinner(new SpinnerNumberModel(
            endSlice, //initial value
            0, //min
            1000, //max
            1));
           
        JRadioButton allSlicesRb=new JRadioButton("All slices");
        allSlicesRb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startLabel.setEnabled(false);
                startSpinner.setEnabled(false);
                endLabel.setEnabled(false);
                endSpinner.setEnabled(false);
            }
        });
        optionPanel.add(allSlicesRb, JComponent.RIGHT_ALIGNMENT);
        optionPanel.add(new JLabel());
        JRadioButton selectedSlicesRb=new JRadioButton("Selected slices");
        selectedSlicesRb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startLabel.setEnabled(true);
                startSpinner.setEnabled(true);
                endLabel.setEnabled(true);
                endSpinner.setEnabled(true);
            }
        });
        optionPanel.add(selectedSlicesRb, JComponent.RIGHT_ALIGNMENT);
        optionPanel.add(new JLabel());
        ButtonGroup group = new ButtonGroup();
        group.add(allSlicesRb);
        group.add(selectedSlicesRb);
        
        optionPanel.add(startLabel);
        optionPanel.add(startSpinner);
            
        optionPanel.add(endLabel);
        optionPanel.add(endSpinner);
        if (allSlices) {
            allSlicesRb.setSelected(true);
            startLabel.setEnabled(false);
            startSpinner.setEnabled(false);
            endLabel.setEnabled(false);
            endSpinner.setEnabled(false);            
        } else 
            selectedSlicesRb.setSelected(true);
            
        JLabel l=new JLabel("Projection method:",JLabel.RIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        optionPanel.add(l);
        String[] methodOptions=new String[]
                {"Average Intensity",
                "Max Intensity",
                "Min Intensity",
                "Sum Slices",
                "Standard Deviation",
                "Median"};
        
        JComboBox projMethodCombo=new JComboBox(methodOptions);
        projMethodCombo.setSelectedItem(projMethod);
        optionPanel.add(projMethodCombo);
        
        int result = JOptionPane.showConfirmDialog(null, optionPanel, 
                this.getClass().getName(), JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            projMethod=(String)projMethodCombo.getSelectedItem();
            try {
                startSpinner.commitEdit();
            } catch (ParseException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
            allSlices=allSlicesRb.isSelected();
            startSlice=((Number)startSpinner.getValue()).longValue();
            try {
                endSpinner.commitEdit();
            } catch (ParseException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
            endSlice=((Number)endSpinner.getValue()).longValue();
            if (endSlice < startSlice) {
                long temp=endSlice;
                endSlice=startSlice;
                startSlice=temp;
            }    
        }    
    }
}
