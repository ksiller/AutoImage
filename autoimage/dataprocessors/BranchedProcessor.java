package autoimage.dataprocessors;

import autoimage.ImageFileQueue;
import autoimage.MMCoreUtils;
import ij.IJ;
import java.io.File;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import mmcorej.TaggedImage;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.acquisition.TaggedImageStorageDiskDefault;
import org.micromanager.api.MMTags;
import org.micromanager.utils.MMScriptException;
import org.micromanager.utils.ReportingUtils;



/**
 * A BranchedProcessor thread allows for on-the-fly analysis and modification of image
 * data during acquisition.
 * 
 * Extend this class and use the AcquisitionEngine functions
 * addImageProcessor and removeImageProcessor to insert your code into the
 * acquisition pipeline

 * The incoming element is polled in process() method and pushed through unmodified 
 * via the output_ queue (functionality inherited from DataProcessor).
 * 
 * The Element flow can be branched to create complex analysis trees.
 * A modified element (copy of input_ element) is pushed through to the modifiedOutput_ queue. 
 * This copied element can be modified, for example a GroupProcessor could collect
 * all slices of a z-stack and create a single image z-projection that gets pushed
 * to the queue of the next DataProcessor.
 * 
 * The tree architecture has nodes (with modifiedOutput_!=null) and leafs (
 * terminal processor in branch: analysisOutput==null)
 * 
 * @author Karsten Siller
 * @param <E> element type in BlockingQueue (e.g. TaggedImage or java.io.File)
 */
public abstract class BranchedProcessor<E> extends ExtDataProcessor<E> {

    private BlockingQueue<E> modifiedOutput_;
    protected TaggedImageStorageDiskDefault storage=null;
    protected boolean savesImageCopy=true;
    protected boolean modifiesMetaData=false;

    public BranchedProcessor () {
        super("","");
    }
    
    
    /**
     * 
     * @param pName label of processor used in GUI
     */
    public BranchedProcessor (String pName) {
        super(pName);
    }     
   
    
    /** 
    * 
    * @param pName label of processor used in GUI
    * @param path assigned path that processor can use to store results (images or data) 
    */
    public BranchedProcessor(String pName, String path) {
        super(pName, path);
    }
   
    
    @Override
    protected void initialize() {
        storage=null;
    }

    
    @Override
    protected void cleanUp() {
         if (storage!=null) {
             storage.close(); 
             //set to null so a new storage will be created when processor runs again
             storage=null;
         }
     }

   
   /**
    * Sets the output queue for objects that have been analyzed
    * exit the DataProcessor. This is a branch parallel to the output_ branch 
    * defined in the DataProcessor class, allowing creation of 
    * a tree analysis architecture. Unmodified objects (same as received via 
    * input queue) are passed through via 'produce()'  
    * 
    * @param aOutput BlockingQueue for modified element output
    */
   public void setAnalysisOutput(BlockingQueue<E> aOutput) {
      modifiedOutput_ = aOutput;
   }

   
   /**
    * Places element into modifiedOutput_ queue
    * 
    * @param element 
    */
   protected void produceModified(E element) {
      if (modifiedOutput_!=null) {           
         try {
            modifiedOutput_.put(element);
         } catch (InterruptedException ex) {
            ReportingUtils.logError(ex);
         }    
      } 
   }

   
   /**
    * Has to reject TaggedImageQueue.Poison or ImageFileQueue.Poison.
    * 
    * @param element incoming element
    * @return should be true if element should be processed and placed in modifiedOutput_ queue
    */ 
   protected abstract boolean acceptElement(E element);
   
   
   /** 
    * If element is modified, a copy needs to be created;
    * createModifiedOutput should handle that.
    * Copy should be passed to modifiedOutput_ queue via call of produceModified(modifiedElement); 
    * @param element incoming element from input_ queue
    * @return list of elements created during processing step
    * @throws java.lang.InterruptedException
    */
   protected abstract List<E> processElement(E element) throws InterruptedException;
   
   
   /**
    * Does not do anything by default.
    * Overwrite if metadata update is needed, e.g. after filtering 
    * @param meta metadata of modified element
    * @param newDir path to be set for "Directory" entry in summary metadata
    * @param newPrefix name to be set for "Prefix" entry in summary metadata
    * @param isTaggedImage if true, newDir and newPrefix values should be ignored since they will be defined by ImageCache
    * @return modified metadata
    * @throws org.json.JSONException if meta JSONObject cannot be parsed or modified
    */
   public JSONObject updateTagValue(JSONObject meta,String newDir, String newPrefix, boolean isTaggedImage) throws JSONException {
       return meta;
   }

   /**
    * Creates the output of the incoming element.
    * Calls updateTagValue with metadata tags of copied element
    * Should create/save a modified element as a copy of incoming element
    * 
    * @param element unmodified element from input_queue
    * @return modified (copied) version of incoming element
    */
    protected E createModifiedOutput(E element) {
        E copy=null;
        if (element instanceof File) {
            try {
                String newPrefix=new File(workDir).getName();
                String newDir=new File(workDir).getParentFile().getAbsolutePath();
                TaggedImage ti=MMCoreUtils.openAsTaggedImage(((File)element).getAbsolutePath());
                //update metadata
                if (savesImageCopy) {
                    //this is the current default
                    ti.tags=updateTagValue(ti.tags, newDir, newPrefix, false);
                } else {
                    //create TaggedImage without any pixel data
                    //--> subsequent call of storage.putImage(ti) will save the metadata.txt without TIF files
                    ti=new TaggedImage(null,updateTagValue(ti.tags, null, null, false));
                }
                if (storage==null) {
                    storage = new TaggedImageStorageDiskDefault (workDir,true,ti.tags.getJSONObject(MMTags.Root.SUMMARY));
                }
                storage.putImage(ti);

                String posName="";
                File copiedFile=new File(new File(new File(newDir,newPrefix),ti.tags.getString("PositionName")),
                                            ti.tags.getString("FileName"));
                copy=(E)copiedFile;
            } catch (JSONException ex) {
                 IJ.log(this.getClass().getName()+ ": Cannot retrieve 'Info' metadata from file. "+ex);
                 Logger.getLogger(FilterProcessor.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NullPointerException ex) {
                 IJ.log(this.getClass().getName()+ ": Error opening file: "+((File)element).getAbsolutePath()+"; "+ex);
            } catch (Exception ex) {
                 IJ.log(this.getClass().getName()+ ": Error writing file to storage. "+ex);
                 Logger.getLogger(FilterProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }
       } else if (element instanceof TaggedImage) {
            try {                
                TaggedImage ti=MMCoreUtils.duplicateTaggedImage((TaggedImage)element);
                ti.tags=updateTagValue(ti.tags,null,null, true);
                copy=(E)ti;
            } catch (JSONException ex) {
                IJ.log(this.getClass().getName()+ ": Cannot retrieve 'Info' metadata from file. "+ex);
                Logger.getLogger(FilterProcessor.class.getName()).log(Level.SEVERE, null, ex);
            } catch (MMScriptException ex) {
                IJ.log("Problem: copying TaggedImage: "+this.getClass().getName());
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
       }
       return copy;
    }  
    
   /**
    * Convenience method to identify last element ("Poison") of queue
    * @param element e.g. TaggedImage or java.io.File
    * @return true if element is TaggedImageQueue.isPoison or ImageFileQueue.isPoison
    */
   protected boolean isPoison(E element) {
        if (element instanceof TaggedImage)
            return TaggedImageQueue.isPoison((TaggedImage)element);
        else if (element instanceof File)
            return ImageFileQueue.isPoison((File)element);
        else
            return false;
   }


   /** 
    * Extends logic of DataProcessor.process() design.
    * - places polled element from input_ queue into output_ queue
    * - 
    */
   @Override
   protected void process() {
      E element = poll();
      try {
         produce(element);
      } catch (NullPointerException e) {//this means output_ queue is set to null because this is last sibling node in branch 
          //no action required
      } finally {//still need to place in analysisOutput_ queue 
         if (isPoison(element)) { //needs to be passed on unmodified
             cleanUp();
             produceModified(element);
             done=true;
             return;
         }
         if (acceptElement(element)) { 
             try {
                 List<E> modifiedElements=processElement(element);
                 if (modifiedElements!=null) { //processing was successful and processed element should be passed to next processor
                     /* processors that form nodes need to place processed image in
                     * modifiedOutput_ queue
                     * for last processor in tree branch (='leaf') modifiedOutput_ == null,
                     * which is handled in produceModified method
                     */
                     for (E modElement:modifiedElements)
                         produceModified(modElement);
                 }
             } catch (InterruptedException ex) {
                 //exit gracefully
                 cleanUp();
                 done=true;
                 IJ.log(this.getClass().getName()+": InterruptedException caught");
             }
         }   
      }   
   }   
}   