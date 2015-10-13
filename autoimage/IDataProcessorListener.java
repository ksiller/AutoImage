package autoimage;

import org.json.JSONObject;
import org.micromanager.api.DataProcessor;

/**
 *
 * @author Karsten
 */
public interface IDataProcessorListener {
    
    public void imageProcessed(JSONObject metadata, DataProcessor source);
    
}
