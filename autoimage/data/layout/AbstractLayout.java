package autoimage.data.layout;

import autoimage.api.IAcqLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten Siller
 */
public abstract class AbstractLayout implements IAcqLayout {
    
    public static IAcqLayout loadLayout(File file) {
        BufferedReader br;
        StringBuilder sb=new StringBuilder();
        JSONObject layoutObj=null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) !=null) {
                sb.append(line);
            }
            JSONObject obj=new JSONObject(sb.toString());
            layoutObj=obj.getJSONObject(IAcqLayout.TAG_LAYOUT);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AbstractLayout.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AbstractLayout.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            Logger.getLogger(AbstractLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
        IAcqLayout layout=AbstractLayout.createFromJSONObject(layoutObj, file);
        return layout;
    }  
    
    public static IAcqLayout createFromJSONObject(JSONObject obj, File f) {
        AbstractLayout layout=null;
        if (obj!=null) {
            String className;
            try {
                //dynamic class loading
                className = obj.getString(TAG_CLASS_NAME);
                Class<?> clazz=Class.forName(className);
                layout=(AbstractLayout) clazz.newInstance();
                layout.setFile(f);
                layout.initializeFromJSONObject(obj, f);
            } catch (ClassNotFoundException ex) {
                layout=null;
                Logger.getLogger(AbstractLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                layout=null;
                Logger.getLogger(AbstractLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                layout=null;
                Logger.getLogger(AbstractLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JSONException ex) {
                layout=null;
                Logger.getLogger(AbstractLayout.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return layout;
    }
    
}
