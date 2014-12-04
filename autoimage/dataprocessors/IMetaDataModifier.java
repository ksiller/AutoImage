/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage.dataprocessors;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten
 */
public interface IMetaDataModifier {
    
    public JSONObject updateTagValue(JSONObject meta,String newDir, String newPrefix, boolean updateSummary) throws JSONException;
}
