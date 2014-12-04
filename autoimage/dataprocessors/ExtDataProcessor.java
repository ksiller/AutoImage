/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.dataprocessors;

import autoimage.ImageFileQueue;
import autoimage.Utils;
import ij.IJ;
import java.io.File;
import mmcorej.TaggedImage;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.DataProcessor;
import org.micromanager.api.MMTags;

/**
 *
 * @author Karsten
 * @param <E>
 */
public class ExtDataProcessor<E> extends DataProcessor<E>{

    protected String procName;
    protected String toolTipText;
    protected String workDir;
    protected boolean done;
    
    public JSONObject getParameters() throws JSONException {
        JSONObject obj=new JSONObject();
        obj.put("ClassName", this.getClass().getName());
        obj.put("ProcName", procName);
        return obj;
    }
    
    public void setParameters(JSONObject obj) throws JSONException {
        procName=obj.getString("ProcName");
    }
    
    public ExtDataProcessor () {
        this("","");
    }
    
    public ExtDataProcessor (String pName) {
        this(pName,"");
    }
    
    public ExtDataProcessor (String pName, String path) {
        super();
        setName(this.getClass().getSimpleName());
        if (pName.equals(""))
            pName=this.getClass().getSimpleName();
        procName=pName;
        workDir=path;
        done=false;
        toolTipText="no information";
    }
    
    public String getProcName() {
        return procName;
    }
    
    public void setProcName(String pn) {
        procName=pn;
    }
    
    public void setWorkDir(String path) {
        workDir=path;
    }
    
    public String getWorkDir() {
        return workDir;
    }
    
    public void setToolTipText(String text) {
        toolTipText=text;
    }
    public String getToolTipText() {
        return toolTipText;
    }
    
    @Override
    public void makeConfigurationGUI() {}
    
   //should be overridden when using makeConfigurationGUI
    @Override
    public void dispose() {}
    
    @Override
    protected void process() {
        try {
            E element = poll();
            produce(element);
            if (element instanceof TaggedImage && TaggedImageQueue.isPoison((TaggedImage)element)) {
                IJ.log(getClass().getSimpleName()+" "+procName+" : Poison");
                done=true;
            }    
            if (element instanceof File && ImageFileQueue.isPoison((File)element)) {
                IJ.log(getClass().getSimpleName()+" "+procName+" : Poison");
                done=true;
            }    
        } catch (NullPointerException e) {
            IJ.log(getClass().getSimpleName()+" "+procName+" : "+e);
        }    
    }
    
    @Override
    public void run() {
        done=false;
        super.run();
        done=true;
    }
    
    public boolean isDone() {
        return done;
    }
    
    public void setDone(boolean b) {
        done=b;
    }
}
