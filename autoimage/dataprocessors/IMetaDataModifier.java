package autoimage.dataprocessors;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This interface should be implemented for ExtDataProcessors that modify metadata, 
 * e.g. FilterProcessor, GroupProcessor
 * 
 * @author Karsten Siller
 */
public interface IMetaDataModifier {
    
    public JSONObject updateTagValue(JSONObject meta,String newDir, String newPrefix, boolean updateSummary) throws JSONException;
}
