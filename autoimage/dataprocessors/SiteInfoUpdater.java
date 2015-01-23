/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.dataprocessors;

import autoimage.IDataProcessorListener;
import autoimage.IDataProcessorNotifier;
import ij.IJ;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import mmcorej.TaggedImage;
import org.json.JSONObject;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.DataProcessor;
import org.micromanager.utils.MDUtils;
import org.micromanager.utils.ReportingUtils;


/**
 * Notification of listeners adapted from MMImageCache design
 * 
 * @author Karsten
 */
public class SiteInfoUpdater extends ExtDataProcessor<TaggedImage> implements IDataProcessorNotifier{
    
    private ArrayList<JSONObject> siteInfo;
    private final List<IDataProcessorListener> listeners = Collections.synchronizedList(new ArrayList<IDataProcessorListener>());
    private final ExecutorService listenerExecutor;
    
    public SiteInfoUpdater() {
        this("Acq Engine",null);
    }
    
    public SiteInfoUpdater(String pName, ArrayList<JSONObject> sInfoList) {
        super(pName,"");
        siteInfo=sInfoList;
        listenerExecutor = Executors.newFixedThreadPool(1);
    }
    
    @Override
    public boolean isSupportedDataType(Class<?> clazz) {
        if (clazz==TaggedImage.class)
            return true;
        else
            return false;
    }
    
    public void setPositionInfoList(ArrayList<JSONObject> sInfoList) {
        siteInfo=sInfoList;
    }

    @Override
    public synchronized void addListener(IDataProcessorListener l) {
        if (!listeners.contains(l))
            listeners.add(l);
    }

    @Override
    public synchronized void removeListener(IDataProcessorListener l) {
        listeners.remove(l);
    }
/**
* Polls for tagged images and adds image-specific Area comments.
*
*/
    @Override
    public void process() {
            TaggedImage nextImage = poll();
            if (nextImage != TaggedImageQueue.POISON) {
                try {
                    JSONObject tags=nextImage.tags;
                    int pos=MDUtils.getPositionIndex(tags);
                    if (siteInfo!=null & siteInfo.size()>pos) {
                        JSONObject si=siteInfo.get(pos);
                        Iterator<?> keys = si.keys();
                        while( keys.hasNext() ){
                            String key = (String)keys.next();
                            tags.put(key, si.get(key));
                        }
                    }
                } catch (Exception ex) {
                    ReportingUtils.logError(ex);
                } finally {
                    try {
                        produce(nextImage);
                    } catch (NullPointerException e) {
                    }    
                }    
            } else {
                // Must produce Poison image to terminate tagged image pipeline
                IJ.log(getClass().getSimpleName()+" "+procName+" : Poison");                
                try {
                    produce(nextImage);
                } catch (NullPointerException e) {     
                }
                done=true;
//                requestStop();
            }
            
            final DataProcessor dp=this;
            final JSONObject metadata=nextImage.tags;
            synchronized (listeners) {
                for (final IDataProcessorListener l : listeners) {
                   listenerExecutor.submit(
                           new Runnable() {
                              @Override
                              public void run() {
                                 l.imageProcessed(metadata,dp);
                              }
                           });
                }
            }
    }    
}
