/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.dataprocessors;

import java.util.logging.Level;
import java.util.logging.Logger;
import mmcorej.TaggedImage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten
 */
public class ImageTagModifier extends ExtDataProcessor<TaggedImage> {

    private String key_;
    private Object value_;

    public ImageTagModifier() {
        this("",null);
    }
    

    public ImageTagModifier(String key, Object value) {
        super ("Image Tag Modifier: ["+key+"="+value.toString()+"]","");
        key_=key;
        value_=value;
    }
    
        @Override
    public void setParameters(JSONObject obj) throws JSONException {
        super.setParameters(obj);
        key_=obj.getString("ModifiedKey");
        value_=obj.get("ModifiedValue");
    }
    
    @Override
    public JSONObject getParameters() throws JSONException {
        JSONObject obj=super.getParameters();
        obj.put("ModifiedKey", key_);
        obj.put("ModifiedValue", value_);
        return obj;
    } 
    
    @Override
    public void process() {
        TaggedImage ti=poll();
        try {
            ti.tags.put(key_, value_);
        } catch (JSONException ex) {
            Logger.getLogger(ImageTagModifier.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullPointerException ex) {    
            Logger.getLogger(ImageTagModifier.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                produce(ti);
            } catch (NullPointerException e) {
                
            }    
        }    
    }
    
}
