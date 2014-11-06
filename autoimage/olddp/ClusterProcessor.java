/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.olddp;

import autoimage.ExtImageTags;
import autoimage.Utils;
import autoimage.dataprocessors.BranchedProcessor;
import ij.IJ;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;


/** Caches group of images of selected channel and selected z-position (slice) that belong to same timepoint and position.
 * Image group is analyzed in "processElement(Element E)" method. A single modified image can be returned, which will be pushed to analysis_output queue 
 
 handles TaggedImage or File
 * 
 * @author Karsten
 */ 
public class ClusterProcessor extends BranchedProcessor<File> {
    
//    protected final List<String> selAreas_;
//    private final int frameIndex_;
//    private final int totalImages_;
//    private int imagesAcquired_;
    protected String currentArea="";
    protected long currentClusterIndex=-1;
//    private long currentFrame=-1;
    protected List<File> imageList;
    protected JSONObject meta; 
    
    public ClusterProcessor(final String pName) {
        this (pName,"",null);
    }
    
    public ClusterProcessor(final String pName, final String path,final List<String> areas) {
        super(pName, path);
        //selAreas_=areas;
        imageList=new ArrayList<File>();
    }
    
    /*
    public void setOptions(JSONObject obj) {
        super.setOptions(obj);
    }
    
    public JSONObject asJSONObject() {
        JSONObject obj=super.asJSONObject();
        return obj;
    } 
    */
/*
    protected void readMetadata(File element) {
        IJ.log("AreaAnalyzer: readMetadata: "+element.getAbsolutePath());
        try {
            meta=Utils.parseMetadata((File)element);
        } catch (JSONException ex) {
            meta=null;
            Logger.getLogger(ClusterProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
*/    
    //called when Poison element is received. this is called before poison element is passed on to analysisOutput queue
    @Override
    protected void cleanUp() {
        IJ.log("cleanUp");
        List<File> modifiedElements=analyzeGroup(imageList);
        if (modifiedElements!=null) { //image analysis was successful
                //analyzers that form nodes need to place processed image in 
                //modifiedOutput_ queue
                //for last analyzer in tree branch (='leaf') modifiedOutput_ = null,
                //which is handled in produceModified method
            
            for (File element:modifiedElements)
            produceModified(element);
        }
        imageList.clear();
        currentArea="";
        currentClusterIndex=-1;

    }
    
    @Override
    protected boolean acceptElement(File element) {
        try {
            readMetadata(element);
        } catch (JSONException ex) {
            Logger.getLogger(ClusterProcessor.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
/*        if (meta==null) {
            IJ.log("readdata: problem metadata");            
            return false;
        }    
        try {
            return selAreas_==null || selAreas_.contains(meta.getString("Area"));
        } catch (JSONException e) {
            IJ.log("acceptElement: problem metadata");
            return false;
        }*/
        return true;
    }

    protected List<File> analyzeGroup(List<File> elements) {
        IJ.log("    analyzing area group: "+currentArea+", "+Long.toString(currentClusterIndex)+", files:"+elements.size());
        for (File f:elements)
            IJ.log("      File: "+f.getAbsolutePath());
        return elements;
    }
    
    @Override
    protected List<File> processElement(File element) { 
        IJ.log("  analyze..."); 
        if (meta!=null) {
             String area;
             Long clusterIndex;
             try {
                List<File> returnValue;
                area = meta.getString(ExtImageTags.AREA_NAME);
                clusterIndex = meta.getLong(ExtImageTags.CLUSTER_INDEX);
                if ((!currentArea.equals("") && !area.equals(currentArea)) || (currentClusterIndex!=-1 && clusterIndex!=currentClusterIndex)) {//element group set complete
                    List<File> modifiedElements=analyzeGroup(imageList);
                    imageList.clear();
//                    currentPos=-1;
//                    currentFrame=-1;
                    returnValue= modifiedElements;
                } else
                    returnValue=null;
                currentArea=area;
                currentClusterIndex=clusterIndex;
                imageList.add(element);
                return returnValue;
             } catch (JSONException ex) {
                 Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                 IJ.log("  analyze: problem parsing metadata" );
                 return null;
             }
         } else
             IJ.log("  analyze: metadata=null");
             return null;
    }

    
}
