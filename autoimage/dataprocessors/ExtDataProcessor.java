/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.dataprocessors;

import autoimage.ImageFileQueue;
import ij.IJ;
import java.io.File;
import mmcorej.TaggedImage;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.DataProcessor;

/**
 * Basic class that extends of DataProcessor interface
 * - manages a "workDir" where the processor can store results/modified images
 * - processsing parameters are configured as JSONObject representation
 * - provides label for display and tooltip function for GUI
 * - added processing status variables
 * - NOTE: ExtDataProcessor handles UNBRANCHED ELEMENT BlockingQueue
 * 
 * @author Karsten Siller
 * @param <E>
 */
public class ExtDataProcessor<E> extends DataProcessor<E>{

    protected String procName;//used as abel in GUI
    protected String toolTipText;//used in GUI
    protected String workDir;//path where processor can save results/images
    protected volatile boolean done;
    protected volatile boolean stopRequested;
    
    public JSONObject getParameters() throws JSONException {
        JSONObject obj=new JSONObject();
        obj.put("ClassName", this.getClass().getName());
        obj.put("ProcName", procName);
        return obj;
    }
    
    //override this if processor can handle data types other than TaggedImage or java.io.File
    public boolean isSupportedDataType(Class<?> clazz) {
        return clazz==TaggedImage.class || clazz==java.io.File.class;
    }
    
    //JSONObject contains processor label and configurable parameters
    public void setParameters(JSONObject obj) throws JSONException {
        procName=obj.getString("ProcName");
    }
    
    public ExtDataProcessor () {
        this("","");
    }
    
    public ExtDataProcessor (String pName) {
        this(pName,"");
    }
    
    //name: label of processor
    //path: assigned working directory where processor should save reults/images
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
    
    //path: assigned working directory where processor should save reults/images
    public void setWorkDir(String path) {
        workDir=path;
    }
    
    public String getWorkDir() {
        return workDir;
    }
    
    //used in GUI
    public void setToolTipText(String text) {
        toolTipText=text;
    }
    
    public String getToolTipText() {
        return toolTipText;
    }
    
    //should launch a GUI dialog to set processing parameters
    @Override
    public void makeConfigurationGUI() {}
    
   //should be overridden when using makeConfigurationGUI
    @Override
    public void dispose() {}
    
    //handles polling elements from BlockingQueue and
    @Override
    protected void process() {
        try {
            //retrieve element from upstream BlockingQueue
            E element = poll();
            //push element to output queue
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
    
    //called immediately before "run()" --> allows variable initialization before each run
    protected void initialize() {}
    
    //
    protected void cleanUp() {}
    
    @Override
    public void run() {
        done=false;
        initialize();
        //super.run() will check if stopRequested==true;
        super.run();
        done=true;
    }
    
    public boolean isDone() {
        return done;
    }
    
    public void setDone(boolean b) {
        done=b;
    }
    
    @Override
    public synchronized void requestStop() {
        IJ.log(this.getClass().getName()+": requestStop()");
        if (!done)
            stopRequested=true;
    }
    
 }
