/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.olddp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import mmcorej.TaggedImage;
import org.json.JSONException;
import org.micromanager.api.DataProcessor;
import org.micromanager.api.MMTags;

/**
 *
 * @author Karsten
 */
public abstract class MC_ScriptAnalyzer extends DataProcessor<TaggedImage> {
    
    private final List<String> channels_;
    private final int sliceIndex_;
    private final int frameIndex_;
    private Map<String,TaggedImage> imageMap;
    
    MC_ScriptAnalyzer(final List<String> channels, int sliceIndex, int frameIndex) {
        super();
        channels_=channels;
        sliceIndex_=sliceIndex;
        frameIndex_=frameIndex;
        imageMap=new HashMap<String,TaggedImage>();
    }
    
    @Override
    protected void process() {
        final TaggedImage taggedImage = poll();
        produce(taggedImage);
        try {
            boolean acceptSlice = sliceIndex_==-1 || taggedImage.tags.getInt(MMTags.Image.SLICE_INDEX) == sliceIndex_;
            boolean acceptFrame = frameIndex_==-1 || taggedImage.tags.getInt(MMTags.Image.FRAME_INDEX) == frameIndex_;
            String channel=taggedImage.tags.getString(MMTags.Image.CHANNEL_NAME);
            if (channels_.contains(channel) && acceptSlice && acceptFrame) {
                imageMap.put(channel, taggedImage);
                if (imageMap.size() == channels_.size()) {
                    analyze();
                    imageMap.clear();
                }    
            }    
        } catch (JSONException ex) {
            Logger.getLogger(MC_ScriptAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
     protected abstract void analyze();
    
}
