/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.dataprocessors;

import autoimage.api.ImageFileQueue;
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
 * @param <E> image data type (e.g. TaggedImage or java.io.File)
 */
public class ExtDataProcessor<E> extends DataProcessor<E>{

    protected String procName;//used as abel in GUI
    protected String toolTipText;//used in GUI
    protected String workDir;//path where processor can save results/images
    protected volatile boolean done;
    protected volatile boolean stopRequested;
    
    public ExtDataProcessor () {
        this("","");
    }
    
    /**
     * 
     * @param pName label of processor used in GUI
     */
    public ExtDataProcessor (String pName) {
        this(pName,"");
    }
    
    /** 
    * 
    * @param pName label of processor used in GUI
    * @param path assigned path that processor can use to store results (images or data) 
    */
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

    /**
     * 
     * @return JSONObject describing processor label and configurable parameters
     * @throws JSONException 
     */
    public JSONObject getParameters() throws JSONException {
        JSONObject obj=new JSONObject();
        obj.put("ClassName", this.getClass().getName());
        obj.put("ProcName", procName);
        return obj;
    }
    
    /**
     * call this method to specify processors label and parameters
     * 
     * @param obj contains processor name used as label in GUI and configurable parameters
     * @throws org.json.JSONException if "ProcName" cannot be parsed from obj 
     */
    public void setParameters(JSONObject obj) throws JSONException {
        procName=obj.getString("ProcName");
    }
    
    /** 
     * override this if processor can handle data types other than TaggedImage or java.io.File
     * 
     * @param clazz class type in question 
     * @return true if class type is supported
     */
    public boolean isSupportedDataType(Class<?> clazz) {
        return clazz==TaggedImage.class || clazz==java.io.File.class;
    }
    
    /**
     * 
     * @return label of processor to be shown in GUI
     */
    public String getProcName() {
        return procName;
    }
    
    /**
     * 
     * @param pn new label for processor to be shown in GUI
     */
    public void setProcName(String pn) {
        procName=pn;
    }
    
    /**
     * 
     * @param path assigned path that processor can use to store results (images or data) 
     */
    public void setWorkDir(String path) {
        workDir=path;
    }
    
    /**
     * 
     * @return path of directory that processor is using to store results (images or data)
     */
    public String getWorkDir() {
        return workDir;
    }
    
    /**
     * 
     * @param text label to describe processor function in GUI
     */
    public void setToolTipText(String text) {
        toolTipText=text;
    }
    
    /**
     * 
     * @return label that describes processor's function
     */
    public String getToolTipText() {
        return toolTipText;
    }
    
    /**
     * should launch a GUI dialog to set processing parameters
     * 
     */ 
    @Override
    public void makeConfigurationGUI() {}
    
    /**
     * close configuration dialog
     * should be overridden when using makeConfigurationGUI
     * 
     */ 
    @Override
    public void dispose() {}
    
    /**
     * handles polling elements from BlockingQueue and places them in output queue
     * 
     */ 
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
    
    /**
     * called immediately before "run()" --> use to initialize processor's fields before each run
     *
     */ 
    protected void initialize() {}
    
    /** 
     * call after last element (Poison) has been received to free up resources, close storage streams etc.
     * 
     */ 
    protected void cleanUp() {}
    
    /**
     * had to override to set "done" flag and call initialize() before starting "run"
     * 
     */
    @Override
    public void run() {
        done=false;
        initialize();
        //super.run() will check if stopRequested==true;
        super.run();
//        cleanUp();
        done=true;
    }
    
    /**
     * 
     * @return "done" flag (should be true after receiving "Poison" or abort   
     */
    public boolean isDone() {
        return done;
    }

    /**
     * 
     * @param b set "done" flag to true or false
     */
    public void setDone(boolean b) {
        done=b;
    }
    
    /**
     * had to override to prevent stopping thread when it's already done with processing 
     * 
     */
    @Override
    public synchronized void requestStop() {
        IJ.log(this.getClass().getName()+": requestStop()");
        if (!done)
            stopRequested=true;
    }
    
 }
