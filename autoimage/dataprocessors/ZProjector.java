package autoimage.dataprocessors;

import autoimage.api.ExtImageTags;
import autoimage.MMCoreUtils;
import autoimage.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
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
import javax.swing.JCheckBox;
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

/**
 * Executes Z-projections using ImageJ's ZProjector class
 * 
 * @author Karsten Siller
 */
public class ZProjector<E> extends GroupProcessor<E> {
    
    private String projMethod;
    private boolean allSlices;
    private long startSlice;
    private long endSlice;

    private static ExecutorService executor=Executors.newSingleThreadExecutor();
//    protected TaggedImageStorageDiskDefault storage=null;

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
                + "Slices: All<br>"
                + "Process 'on-the-fly': "+(processOnTheFly ? "yes": "no")+"</html>";
        } else {
            return "<html>Method: "+projMethod+"<br>"
                + "Slices: "+Long.toString(startSlice)+"-"+Long.toString(endSlice)+"<br>"
                + "Process 'on-the-fly': "+(processOnTheFly ? "yes": "no")+"</html>";
        }
    }
    
    @Override
    public List<E> processGroup(final Group<E> group) {
        IJ.log("-----");
        IJ.log(this.getClass().getName()+".processGroup: ");
        IJ.log("   Criteria: "+group.groupCriteria.toString());
        IJ.log("   Images: "+group.elements.size()+" images");
        final String newDir=new File(workDir).getParentFile().getAbsolutePath();                        
        final String newPrefix=new File(workDir).getName();
        
        if (group!=null && group.elements!=null) {

            Callable projectionTask=new Callable<List<E>>() {

                @Override
                public List<E> call() {
                    List<E> results=new ArrayList<E>();
                    ImageStack stack=null;
                    //LUT lut=null;
                    JSONObject meta=null;
//                    boolean isRGB=false;
                    try {
                        //read elements and place in single stack
                        double zPos=0;
                        ImagePlus resultImp;
                        if (group.elements.size() > 1) {
                            for (E image:group.elements) {
                                ImagePlus imp;
    //                            if (image instanceof java.io.File) {
                                    File file=(File)image;
                                    imp=IJ.openImage(file.getAbsolutePath());
                                    meta=MMCoreUtils.parseMetadataFromFile(file);
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
//                                    JSONObject summary=meta.getJSONObject(MMTags.Root.SUMMARY);
    //                                String pixType=summary.getString(MMTags.Summary.PIX_TYPE);
    //                                isRGB=pixType.equals("RGB32") || pixType.equals("RGB64");
    //                                int bitDepth=(pixType.equals("Gray16") || pixType.equals("RGB64")) ? 16 : 8;
    //                                IJ.log("bitDepth="+Integer.toString(summary.getInt(MMTags.Summary.BIT_DEPTH)));
                                    //isRGB=imp.getImageStack().isRGB();
    //                                IJ.log("isRGB="+Boolean.toString(isRGB));
                                    //impHyperStack=imp.createHyperStack("hyperstack", isRGB ? 3 : 1 ,group.elements.size(), 1, bitDepth);
                                }
                                //slices in imp refer to channels here
                                //RGB64 --> 3 slices (R, G, B)
                                for (int z=1; z<=imp.getStackSize(); z++) {
                                    ImageProcessor ip=imp.getImageStack().getProcessor(z);
    //                                lut=ip.getLut();
                                    stack.addSlice(ip.duplicate());
                                }
                            }
                            zPos/=group.elements.size();
                            //IJ.log(lut.toString());
                            ImagePlus stackImp=new ImagePlus("Hyperstack");
                            stackImp.setStack(stack,stack.getSize() / group.elements.size(),group.elements.size(), 1);
    //                        IJ.run(stackImp,"Re-order Hyperstack ...", "channels=[Channels (c)] slices=[Slices (z)] frames=[Frames (t)]");
                            stackImp.setOpenAsHyperStack(true);
                            IJ.save(stackImp,"/Users/Karsten/Desktop/hyperstack.tif");
                            ij.plugin.ZProjector projector=new ij.plugin.ZProjector(stackImp);
                            projector.setStartSlice(0);
                            projector.setStopSlice(group.elements.size()-1);
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
                            IJ.log("isHyperStack="+Boolean.toString(stackImp.isHyperStack()));
                            projector.doHyperStackProjection(true);
                            resultImp=projector.getProjection();
                            //update metadata
                            meta.put(ExtImageTags.SLICE_INDEX_ORIG, 0);
                            meta.put(MMTags.Image.ZUM, zPos);
                        } else {
                            IJ.log("SINGLE FILE");
                            File file=(File)group.elements.get(0);
                            resultImp=IJ.openImage(file.getAbsolutePath());
                            meta=MMCoreUtils.parseMetadataFromFile(file);
                        }
                        IJ.save(resultImp,"/Users/Karsten/Desktop/proj.tif");

                        //update metadata
                        meta.put(MMTags.Image.SLICE_INDEX, 0);
                        meta.put(MMTags.Image.SLICE, 0);
                        JSONObject summary = new JSONObject(meta.getJSONObject(MMTags.Root.SUMMARY).toString());
                        summary.put(MMTags.Summary.SLICES, 1);
                        meta.put(MMTags.Root.SUMMARY, summary);

                        //create TaggedImage
                        TaggedImage ti=MMCoreUtils.createTaggedImage(resultImp,meta);
/*
                        if (group.elements.get(0) instanceof TaggedImage) {
                            results.add((E)ti);
                            IJ.log("ZProject result (TAGGEDIMAGE): "+(new File(new File(workDir,meta.getString(MMTags.Image.POS_NAME)),
                                                    meta.getString("FileName"))).getAbsolutePath());
                        } else */
                        if (group.elements.get(0) instanceof java.io.File) {
                            if (storage==null) {
                                summary.put(MMTags.Summary.DIRECTORY, newDir);
                                summary.put(MMTags.Summary.PREFIX, newPrefix);
                                storage = new TaggedImageStorageDiskDefault(workDir,true,summary);
                            }
                            storage.putImage(ti);
                            results.add((E)new File(new File(workDir,meta.getString(MMTags.Image.POS_NAME)),
                                                    meta.getString("FileName")));                    
                            IJ.log("ZProject result (FILE): "+((File)results.get(0)).getAbsolutePath());
                        }
                    } catch (JSONException ex) {
                        IJ.log(this.getClass().getName()+"Problem with processing group");
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                    } finally {
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
    
    //filters image elements based on user defined slice index parameters
    @Override
    protected boolean acceptElement(E element) {
        try {
            JSONObject meta=MMCoreUtils.parseMetadataFromFile((File)element);
            long slice=meta.getLong(MMTags.Image.SLICE_INDEX);
            IJ.log(this.getClass().getName()+": current="+slice+", start="+startSlice+", end="+endSlice);
            if (allSlices || (slice >= startSlice && slice <=endSlice)) {
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

/*    
    @Override
    public JSONObject updateTagValue(JSONObject meta, String newDir, String newPrefix, boolean updateSummary) throws JSONException {
        return meta;
    }

    @Override
    public void cleanUp() {
        super.cleanUp();
        //close image storage
        if (storage != null) {
            storage.close();
            storage=null;
        } else {
            //if storage == null --> TaggedImage
            //need to modify 'SLICES' in all metadata summary files
        }
    }
*/
    @Override
    protected long determineMaxGroupSize(JSONObject meta) throws JSONException {
        if (!processOnTheFly) {
            return -1;
        }
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
        
        JCheckBox procCB = new JCheckBox("Process On-the-Fly"); 
        procCB.setSelected(processOnTheFly);
        optionPanel.add(procCB);
        
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
            processOnTheFly=procCB.isSelected();
        }    
    }

    @Override
    public boolean isSupportedDataType(Class<?> clazz) {
        return clazz==java.io.File.class;
    }
}
