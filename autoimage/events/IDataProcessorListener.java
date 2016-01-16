package autoimage.events;

import org.json.JSONObject;
import org.micromanager.api.DataProcessor;

/**
 *
 * @author Karsten Siller
 */
public interface IDataProcessorListener {
    
    public void imageProcessed(JSONObject metadata, DataProcessor source);
    
}
