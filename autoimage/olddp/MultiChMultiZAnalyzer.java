/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.olddp;

import autoimage.MMCoreUtils;
import autoimage.Utils;
import autoimage.dataprocessors.BranchedProcessor;
import ij.IJ;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import mmcorej.TaggedImage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.api.MMTags;


/** Caches group of images of selected channel and selected z-position (slice) that belong to same timepoint and position.
 * Image group is analyzed in "processElement(Element E)" method. A single modified image can be returned, which will be pushed to analysis_output queue 
 
 handles TaggedImage or File
 * 
 * @author Karsten
 * @param <E> 
 */ 
public abstract class MultiChMultiZAnalyzer<E> extends BranchedProcessor<E> {
    
    protected List<String> selChannels_;
    protected List<Long> selSlices_;
//    private final int frameIndex_;
//    private final int totalImages_;
//    private int imagesAcquired_;
    private long currentPos=-1;
    private long currentFrame=-1;
    protected List<E> imageList;
   // protected JSONObject meta; 
    
    public MultiChMultiZAnalyzer(final String pName, final String path,final List<String> channels, final List<Long> slices) {
        super(pName, path);
        selChannels_=channels;
        selSlices_=slices;
        imageList=new ArrayList<E>();
    }
   
    @Override
    public JSONObject getParameters() throws JSONException {
        JSONObject obj=super.getParameters();
        JSONArray channels=new JSONArray();
        for (String ch:selChannels_) {
            channels.put(ch);
        }
        obj.put("Channels", channels);
        JSONArray slices=new JSONArray();
        for (Long sl:selSlices_) {
            slices.put(sl);
        }
        obj.put("Slices", slices);
        return obj;
    }
    
    @Override
    public void setParameters(JSONObject obj) throws JSONException {
        super.setParameters(obj);
        JSONArray channels=obj.getJSONArray("Channels");
        selChannels_=new ArrayList<String>();
        for (int i=0; i<channels.length(); i++) {
            selChannels_.add(channels.getString(i));
        }
        JSONArray slices=obj.getJSONArray("Slices");
        selSlices_=new ArrayList<Long>();
        for (int i=0; i<slices.length(); i++) {
            selSlices_.add(slices.getLong(i));
        }
    }
    
    protected JSONObject readMetadata(E element) {
        if (element instanceof TaggedImage)
            return ((TaggedImage)element).tags;
        else if (element instanceof File) {
            try {
                return MMCoreUtils.parseMetadataFromFile((File)element);
            } catch (JSONException ex) {
                Logger.getLogger(MultiChMultiZAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
                 return null;
            }    
        }
        else
            return null;
    }

    
    //called when Poison element is received. this is called before poison element is passed on to analysisOutput queue
    @Override
    protected void cleanUp() {
        IJ.log("cleanUp");
        List<E> modifiedElements=analyzeGroup(imageList);
        if (modifiedElements!=null) { //image analysis was successful
                //analyzers that form nodes need to place processed image in 
                //modifiedOutput_ queue
                //for last analyzer in tree branch (='leaf') modifiedOutput_ = null,
                //which is handled in produceModified method
            for (E modElement:modifiedElements)    
                produceModified(modElement);
        }
        imageList.clear();
        currentPos=-1;
        currentFrame=-1;

    }
    
    @Override
    protected boolean acceptElement(E element) {
        JSONObject meta=readMetadata(element);
        if (meta==null) {
            IJ.log("readdata: problem metadata");            
            return false;
        }    
        try {
            boolean acceptChannel = selChannels_==null || selChannels_.contains(meta.getString(MMTags.Image.CHANNEL_NAME));
            boolean acceptSlice ;
            if (element instanceof TaggedImage) {
                acceptSlice = selSlices_==null || selSlices_.contains(meta.getLong(MMTags.Image.SLICE_INDEX));
            } else {
                acceptSlice = selSlices_==null || selSlices_.contains(meta.getLong(MMTags.Image.SLICE_INDEX));
            }
            IJ.log("acceptElement: "+Boolean.toString(acceptSlice&&acceptChannel)
                    +", "+meta.getString(MMTags.Image.CHANNEL_NAME)
                    +", "+Long.toString(meta.getLong(MMTags.Image.SLICE_INDEX)));
            return acceptSlice && acceptChannel;
        } catch (JSONException e) {
            IJ.log("acceptElement: problem metadata");
            return false;
        }
    }

    //if elements are modified, working copies need to be created first
    protected abstract List<E> analyzeGroup(List<E> elements);
    
    @Override
    protected List<E> processElement(E element) { 
        IJ.log("  analyze...");
        JSONObject meta=readMetadata(element);
        if (meta!=null) {
             long pos;
             long frame;
             try {
                List<E> returnValue;
                if (element instanceof TaggedImage) {
                    pos = meta.getLong(MMTags.Image.POS_INDEX);
                    frame = meta.getLong(MMTags.Image.FRAME_INDEX);
                } 
                else {
                    pos = meta.getLong(MMTags.Image.POS_INDEX);
                    frame = meta.getLong(MMTags.Image.FRAME_INDEX);
                }
                if ((currentPos!=-1 && pos!=currentPos) || (currentFrame!=-1 && frame!=currentFrame)) {//element group set complete
                    List<E> modifiedElements=analyzeGroup(imageList);
                    imageList.clear();
//                    currentPos=-1;
//                    currentFrame=-1;
                    returnValue= modifiedElements;
                } else
                    returnValue=null;
                currentPos=pos;
                currentFrame=frame;
                imageList.add(element);
                return returnValue;
             } catch (JSONException ex) {
                 Logger.getLogger(MultiChMultiZAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
                 IJ.log("  analyze: problem parsing metadata" );
                 return null;
             }
         } else
             IJ.log("  analyze: metadata=null");
             return null;
    }

    
}

