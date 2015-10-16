package autoimage;

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
public abstract class AcqBasicLayout implements IAcqLayout {
    
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
            Logger.getLogger(AcqBasicLayout.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AcqBasicLayout.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            Logger.getLogger(AcqBasicLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
        IAcqLayout layout=AcqBasicLayout.createFromJSONObject(layoutObj, file);
        return layout;
    }  
    
    public static IAcqLayout createFromJSONObject(JSONObject obj, File f) {
        AcqBasicLayout layout=null;
        if (obj!=null) {
            String className;
            try {
                //dynamic class loading
                className = obj.getString(TAG_CLASS_NAME);
                Class clazz=Class.forName(className);
                layout=(AcqBasicLayout) clazz.newInstance();
                layout.initializeFromJSONObject(obj, f);
                try {
                    layout.calcStageToLayoutTransform();
                } catch (Exception ex) {
                    Logger.getLogger(AcqBasicLayout.class.getName()).log(Level.SEVERE, null, ex);
                }        
            } catch (ClassNotFoundException ex) {
                layout=null;
                Logger.getLogger(AcqBasicLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                layout=null;
                Logger.getLogger(AcqBasicLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                layout=null;
                Logger.getLogger(AcqBasicLayout.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JSONException ex) {
                layout=null;
                Logger.getLogger(AcqBasicLayout.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return layout;
    }

}
