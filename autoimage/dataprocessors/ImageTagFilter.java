/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.dataprocessors;

import autoimage.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import mmcorej.TaggedImage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.acquisition.TaggedImageStorageDiskDefault;
import org.micromanager.api.MMTags;
import org.micromanager.utils.ImageUtils;

/**
 *
 * @author Karsten
 */

//E: element type in BlockingQueue (either TaggedImage or File)
//T: tag value type
public abstract class ImageTagFilter<E,T> extends BranchedProcessor<E>{
    
    protected String key_; //key to retrieve property of JSONObject in TaggedImages that will be filtered
    protected List<T> values_; //accepted values for property retrieved with key_
    protected long imageNumberForThisDim=0; //used to adjust dimension properties of TaggedImage (# of 'CHANNELS', 'FRAMES', 'SLICES')
    protected TaggedImageStorageDiskDefault storage=null;
            
    public ImageTagFilter() {
        this("Filter","",null);
    }
    
    public ImageTagFilter(String key, List<T> values) {
        this("Filter",key,values);
    }
    
    public abstract boolean equalValue(T t1, T t2);
    
    public ImageTagFilter(String pName, String key, List<T> values) {
        super(pName);
        this.key_=key;
        if (values!=null)
            this.values_=values;
        else 
            this.values_=new ArrayList<T>();
    }

    
    @Override
    public void setParameters(JSONObject obj) throws JSONException {
        super.setParameters(obj);
        key_=obj.getString("FilterKey");
        JSONArray vals=obj.getJSONArray("FilterValues");
//        String valueClassName=obj.getString("ValueClassName");
        values_=new ArrayList<T>(vals.length());
        for (int i=0; i<vals.length(); i++) {
            T v;
//            IJ.log(this.getClass().getSimpleName()+".setParameters: set vals.get(i): "+vals.get(i).getClass().getName());
            if (vals.get(i) instanceof Integer){ 
                v=(T)new Long(vals.getInt(i)); 
            }else {
                v=(T)vals.get(i);
            }    
//            IJ.log(this.getClass().getSimpleName()+".setParameters: set (T)v: "+v.getClass().getName());
            values_.add(v);
        } 
//        IJ.log("---");
    }
    
    @Override
    public JSONObject getParameters() throws JSONException {
        JSONObject obj=super.getParameters();
        obj.put("FilterKey", key_);
        JSONArray vals=new JSONArray(values_);
/*        for (T v:values_) {
            IJ.log(this.getClass().getSimpleName()+".getParameters: get "+v.getClass().getName());
            if (v instanceof Integer)
                vals.put((Long)v);
            else
                vals.put(v);
        }*/
        obj.put("FilterValues", vals);
//        IJ.log("---");
        return obj;
    } 
    
    @Override
    public String getProcName() {
        return procName+" "+key_+" "+values_.toString();
    }
    
    public String getKey() {
        return key_;
    }
    
    public void setKey(String key) {
        key_=key;
    }
    
    public abstract T valueOf(String v);
    
    public void setValuesFromStr(List<String> values) {
//        this.values_=values;
        values_.clear();
        for (String value:values)
            values_.add(valueOf(value));
    }
    
    public List<T> getValues() {
        return values_;
    }
   
    protected boolean listContainsElement(T value) {
        if (value instanceof Integer) {
//            IJ.log(this.getClass().getName()+": Integer found, converting to Long");
            return values_.contains(new Long((Integer)value));   
        } else {
            return values_.contains(value);
        }    
    }
    
    //if parsing fails, element is rejected
    @Override
    protected boolean acceptElement(E element) {
        JSONObject tags;
        if (element instanceof TaggedImage) {
            IJ.log(this.getClass().getName()+".acceptElement - IMAGE: ");
            tags=((TaggedImage)element).tags;
        } else if (element instanceof File) {
            IJ.log(this.getClass().getName()+".acceptElement - FILE: "+((File)element).getAbsolutePath());
            try {
                tags=Utils.parseMetadata((File)element);
            } catch (JSONException ex) {
                IJ.log("    "+this.getClass().getName()+".acceptElement: problem parsing 1");
                Logger.getLogger(ImageTagFilter.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        } else
            return false;
        if (tags!=null) {
            try {
                T entry = (T)tags.get(key_);
                if (listContainsElement(entry)) {
                    IJ.log(getProcName()+"   accepted "+entry.getClass().getSimpleName()+": "+entry);
                    return true;
                } else {
                    IJ.log(getProcName()+"   dropped "+entry.getClass().getSimpleName()+": "+entry);
                    return false;
                }    
            } catch (JSONException ex) {
                IJ.log("    "+this.getClass().getName()+".acceptElement: problem parsing 2");
                Logger.getLogger(ImageTagFilter.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
         } else
            return false; 
    }
   
    @Override
    public void run() {
        imageNumberForThisDim=0;
        super.run();
    }
    
    protected JSONObject updateTagValue(JSONObject meta,String newDir, String newPrefix, boolean isTaggedImage) throws JSONException {
        JSONObject summary=meta.getJSONObject(MMTags.Root.SUMMARY);
        if (!isTaggedImage && newDir!=null)
            summary.put(MMTags.Summary.DIRECTORY,newDir);
        if (!isTaggedImage && newPrefix!=null)
            summary.put(MMTags.Summary.PREFIX,newPrefix);
        if (key_.equals(MMTags.Image.CHANNEL_INDEX) 
                        || key_.equals(MMTags.Image.CHANNEL_NAME)
                        || key_.equals(MMTags.Image.CHANNEL)) {
//                IJ.log(this.getClass().getName()+": updating channel info");
                    String thisChannel=meta.getString(MMTags.Image.CHANNEL_NAME);
                    JSONArray chNames=summary.getJSONArray(MMTags.Summary.NAMES);
                    JSONArray chColor=summary.getJSONArray(MMTags.Summary.COLORS);
                    JSONArray chMin=summary.getJSONArray("ChContrastMin");//MMTags.Summary.CHANNEL_MINS);
                    JSONArray chMax=summary.getJSONArray("ChContrastMax");//MMTags.Summary.CHANNEL_MAXES);
                    JSONArray foundCh=new JSONArray();
                    JSONArray foundChColor=new JSONArray();
                    JSONArray foundChMin=new JSONArray();
                    JSONArray foundChMax=new JSONArray();
                    int newNoOfChannels=0;
                    for (int i=0; i<chNames.length(); i++) {
                        if (((List<String>)values_).contains((String)chNames.get(i))) {
                            foundCh.put(chNames.get(i));
                            foundChColor.put(chColor.get(i));
                            foundChMin.put(chMin.get(i));
                            foundChMax.put(chMax.get(i));
                            newNoOfChannels++;
                        }
                    }    
                    int newChIndex=0;
                    for (newChIndex=0; newChIndex<foundCh.length();newChIndex++) {
                        if (thisChannel.equals(foundCh.get(newChIndex))) {
                           meta.put(MMTags.Image.CHANNEL_INDEX, newChIndex);
                           break;
                        }
                    }
                    //update total # of channels, channel name array, channel color array, channel min contrast array, channel max contrast array
                    summary.put(MMTags.Summary.CHANNELS, newNoOfChannels);//can't use values_.size() since filter may contain values not used during acqusition
                    summary.put(MMTags.Summary.NAMES, foundCh);
                    summary.put(MMTags.Summary.COLORS, foundChColor);
                    summary.put("ChContrastMin",foundChMin);//MMTags.Summary.CHANNEL_MINS, foundChMin);
                    summary.put("ChContrastMax",foundChMax);//MMTags.Summary.CHANNEL_MAXES, foundChMax);
        } if (key_.equals(MMTags.Image.FRAME_INDEX) 
                        || key_.equals(MMTags.Image.FRAME)) {
//                IJ.log(this.getClass().getName()+": updating frame info");
                    if (imageNumberForThisDim==0) {
                        long currentNoOfFrames=summary.getLong(MMTags.Summary.FRAMES);                        
                        for (T value : values_) {
                            if ((Long)value < currentNoOfFrames) {
                                imageNumberForThisDim++;
                            }
                        }    
                    }
                    Long oldIdx = meta.getLong(MMTags.Image.FRAME_INDEX);
                    for (int i=0; i<values_.size(); i++) {
                        if (oldIdx.equals((Long)values_.get(i))) {
//                            IJ.log("WRITING FRAMEINDEX: "+i);
                            meta.put(MMTags.Image.FRAME_INDEX,i);
                            meta.put(MMTags.Image.FRAME,i);
                            break;
                        } else {
//                            IJ.log("LEAVING FRAMEINDEX: "+oldIdx);                            
                        }
                    }    
                    summary.put(MMTags.Summary.FRAMES, imageNumberForThisDim);
        } if (key_.equals(MMTags.Image.SLICE_INDEX) 
                        || key_.equals(MMTags.Image.SLICE)) {
//            IJ.log(this.getClass().getName()+": updating slice info");
//                    if (isTaggedImage) {
                        if (imageNumberForThisDim==0) {
                            long currentNoOfSlices=summary.getLong(MMTags.Summary.SLICES);
//                            IJ.log("CURRENTNOOFSLICES: "+currentNoOfSlices);
                            for (T value:values_) {
                                if ((Long)value < currentNoOfSlices) {
                                    imageNumberForThisDim++;
                                }
                            }    
/*                            for (int i=0; i<values_.size(); i++) {
                                T v=(T)values_.get(i);
                                if ((Long)v < currentNoOfSlices) {
                                    imageNumberForThisDim++;
                                }
                            }    */
                        }
//                        IJ.log("IMAGENNOFORTHISDIM: "+imageNumberForThisDim);
                        Long oldIdx = meta.getLong(MMTags.Image.SLICE_INDEX);
                        for (int i=0; i<values_.size(); i++) {
                            if (oldIdx.equals((Long)values_.get(i))) {
//                                IJ.log("WRITING SLICEINDEX: "+i);
                                meta.put(MMTags.Image.SLICE,i);
                                meta.put(MMTags.Image.SLICE_INDEX,i);
                                break;
                            } 
                        }    
/*                    } else {
                        if (imageNumberForThisDim==0) {
                            long currentNoOfSlices=summary.getLong(MMTags.Summary.SLICES);
                            for (int i=0; i<values_.size(); i++) {
                                T v=(T)values_.get(i);
                                if ((Long)v < currentNoOfSlices) {
                                    imageNumberForThisDim++;
                                } 
                            }
                        }    
                        Long oldIdx = meta.getLong(MMTags.Image.SLICE_INDEX);
                        for (int i=0; i<values_.size(); i++) {
                            if (oldIdx.equals((Long)values_.get(i))) {
                                meta.put(MMTags.Image.SLICE_INDEX,i);
                                break;
                            } 
                        }        
                    }    */
//                        IJ.log("WRITING SUMMARY SLICES: "+imageNumberForThisDim);
                    summary.put(MMTags.Summary.SLICES, imageNumberForThisDim);
        }
        return meta;
    }
    
   
    //creates copy of element
    //if meta!=null, metadata in copied element will be replaced by meta, otherwise keep original metadata
    @Override
    protected E createCopy(E element) {
        JSONObject meta=null;
        E copy=null;
        if (element instanceof File) {
            TaggedImage ti=null;
            ImagePlus imp=null;
            imp=IJ.openImage(((File)element).getAbsolutePath());
            if (imp.getProperty("Info") != null) {
//                TaggedImageStorageDiskDefault storage=null;
                try {
                    meta = new JSONObject((String)imp.getProperty("Info"));
                    String newDir=new File(workDir).getParentFile().getAbsolutePath();
//                    String newDir=new File(workDir).getAbsolutePath();
                    ImageProcessor ip=imp.getProcessor();
                    ti=ImageUtils.makeTaggedImage(ip);
                    ti.tags=new JSONObject(meta.toString());
//                    String newPrefix=ti.tags.getString("PositionName");
                    String newPrefix=new File(workDir).getName();
//                    ti.tags=updateTagValue(ti.tags, workDir, newPrefix, false);
                    ti.tags=updateTagValue(ti.tags, newDir, newPrefix, false);
                    if (storage==null) {
                        storage = new TaggedImageStorageDiskDefault (workDir,true,ti.tags.getJSONObject(MMTags.Root.SUMMARY));
                    }
                    storage.putImage(ti);
/*                    JSONObject summary=meta.getJSONObject(MMTags.Root.SUMMARY);
                    File copiedFile=new File(new File(new File(summary.getString(MMTags.Summary.DIRECTORY), 
                                                summary.getString(MMTags.Summary.PREFIX)),
                                                meta.getString("PositionName")),
                                                    ((File)element).getName());*/
//                    File copiedFile=new File(new File(new File(summary.getString(MMTags.Summary.DIRECTORY), 
//                                                summary.getString(MMTags.Summary.PREFIX)),
//                                                meta.getString("PositionName")),
//                                                    meta.getString("FileName"));

//                    File copiedFile=new File(new File(workDir,newPrefix),
//                                                    ti.tags.getString("FileName"));
                    String posName="";
                    File copiedFile=new File(new File(new File(newDir,newPrefix),meta.getString("PositionName")),
                                                    ti.tags.getString("FileName"));
                    copy=(E)copiedFile;
                } catch (Exception ex) {
                    IJ.log(this.getClass().getName()+ ": problem: create copy. "+ex);
                    Logger.getLogger(ImageTagFilter.class.getName()).log(Level.SEVERE, null, ex);
                    copy=super.createCopy(element);
                }
//                if (storage!=null) {
//                    storage.close();           
//                }
            } else {
                copy=super.createCopy(element);
            }
       } else if (element instanceof TaggedImage) {
            copy=super.createCopy(element);
            meta=((TaggedImage)element).tags;
            try {
                meta=updateTagValue(meta,null,null, true);
                ((TaggedImage)copy).tags=meta;
            } catch (JSONException ex) {
                Logger.getLogger(ImageTagFilter.class.getName()).log(Level.SEVERE, null, ex);
            }
       }
       return copy;
    }    
        
    @Override
    protected List<E> processElement(E element) {
        // create copy, update metadata and pass accepted elements through
        List<E> list=new ArrayList<E>(1);
        list.add(createCopy(element));
        return list;
    }

    @Override
    public void makeConfigurationGUI() {
        JPanel optionPanel = new JPanel();
        BoxLayout layout = new BoxLayout(optionPanel,BoxLayout.Y_AXIS);

        optionPanel.setLayout(layout);
        JLabel l=new JLabel("Tag:");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.add(l);   
        JComboBox tagComboBox= new JComboBox(Utils.getAvailableImageTags());
//        if (itf!=null)
            tagComboBox.setSelectedItem(key_);
        tagComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.add(tagComboBox);
                        
        l=new JLabel("Filter Values:");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.add(l);
        JTextArea argField = new JTextArea();
        argField.setColumns(30);
        argField.setRows(10);
        argField.setLineWrap( true );
        argField.setWrapStyleWord( true );
        argField.setSize(argField.getPreferredSize().width, 1);
        argField.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (values_!=null && values_.size()>0) {
            //argField.setText(itf.getValues().toString());
            for (Object o:values_) {
                argField.append(o.toString()+"\n");
            }
        }    
        optionPanel.add(argField);
            
        int result = JOptionPane.showConfirmDialog(null, optionPanel, 
               "Filter: Image Tags", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            key_=(String)tagComboBox.getSelectedItem();
            String[] argArray=argField.getText().split("\\n");
            setValuesFromStr(Arrays.asList(argArray));
        } else {
            key_="";
            values_.clear();
        }   
    }
    
    @Override
    protected void cleanUp() {
        if (storage!=null) {
            storage.close(); 
            //set to null so a new storage will be created when processor runs again
            storage=null;
        }
        imageNumberForThisDim=0;
    }
}
