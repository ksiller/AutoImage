package autoimage.dataprocessors;

import autoimage.ImageFileQueue;
import autoimage.Utils;
import ij.IJ;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import mmcorej.TaggedImage;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.utils.MMScriptException;
import org.micromanager.utils.ReportingUtils;



/**
 * A BranchedProcessor thread allows for on-the-fly analysis and modification of image
 * data during acquisition.
 * 
 * Inherit from this class and use the AcquisitionEngine functions
 * addImageProcessor and removeImageProcessor to insert your code into the
 * acquisition pipeline

 * The incoming element will polled in process() method and pushed through unmodified 
 * via the output_ queue (functionality inherited from DataProcessor.
 * 
 * The Element flow can be branched to create complex analysis pipelines.
 * A copy of the element will be pushed through to the modifiedOutput_ queue. 
 * This copied element can be modified, for example the seqAnalyzer could collect
 * all slices of a z-stack and create a single image z-projection that gets pushed
 * to the queue of the next DataProcessor.
 * 
 * The tree architecture has nodes (with modifiedOutput_!=null) and leafs (
 * terminal processor in branch: analysisOutput==null)
 * 
 *
 */
public abstract class BranchedProcessor<E> extends ExtDataProcessor<E> {

   private BlockingQueue<E> modifiedOutput_;

   public BranchedProcessor () {
        super("","");
   }
    
   public BranchedProcessor (String pName) {
        super(pName);
   }     
   
   public BranchedProcessor(String pName, String path) {
       super(pName, path);
   }
   
   
   
   /*
    * The processElement method should be overridden by classes implementing
  BranchedProcessoryzer, to provide a analysis and processing function.
    *
    * For example, an "Identity" DataProcessor (where nothing is
    * done to the data) would override process() thus:
    *
    * @Override
    * public void process() {
    *    produce(poll());
    * }
    * TaggedImageQueue.POISON will be the last object
    * received by polling -- the process method should pass this
    * object on unchanged.
    */
 
   
   /*
    * Sets the input queue where objects to be processed
    * are received by the DataProcessor.
    */
   
   /*
    * Sets the output queue for objects that have been analyzed
    * exit the DataProcessor. This is a parallel branch that allows creation of 
    * a tree analysis architecture. Unmodified objects (same as received via 
    * input queue) are passed through via 'produce()'  
    */
   
 
   
   public void setAnalysisOutput(BlockingQueue<E> aOutput) {
      modifiedOutput_ = aOutput;
   }

   
   protected void produceModified(E element) {
      if (modifiedOutput_!=null) {           
         try {
            modifiedOutput_.put(element);
         } catch (InterruptedException ex) {
            ReportingUtils.logError(ex);
         }    
      } 
   }

/*    protected JSONObject readMetadata(E element) throws JSONException {
        JSONObject meta=null;
        if (element instanceof File) {
            meta=Utils.parseMetadata((File)element);
        } else if (element instanceof TaggedImage) {
            meta=((TaggedImage)element).tags;
        }    
        return meta;
    }
*/
   //has to reject TaggedImageQueue.Poison
   protected abstract boolean acceptElement(E element);
   
   /* if element is modified, a copy needs to be created and saved in workDir
    * createCopy can handle that
    * copy will be passed to analysisOutput_ 
    */
   protected abstract List<E> processElement(E element);
   
   /* creates copy of element
    * if meta!=null, metadata in copied element will be replaced by meta, otherwise keep original metadata
    */
   protected E createCopy(E element) {
        if (element instanceof File) {
            File f =(File)element;
            try {
                JSONObject meta=Utils.parseMetadata(f);
                String path=Utils.createPathForSite(f,workDir,true);
                if (path==null)
                    path=workDir;
                File modFile=new File(path,f.getName());
                Utils.copyFile(f,modFile);
                return (E)modFile;
            } catch (JSONException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                IJ.log("Problem: parsing File metadata: "+this.getClass().getName());
                return null;
            } catch (IOException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                IJ.log("Problem: copying File: "+f.getAbsolutePath());
                return null;
            }    
        } else if (element instanceof TaggedImage) {
            try {
                TaggedImage modTI=Utils.duplicateTaggedImage((TaggedImage)element);
                return (E)modTI;
            } catch (JSONException ex) {
                IJ.log("Problem: parsing TaggedImage metadata: "+this.getClass().getName());
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                return null;
            } catch (MMScriptException ex) {
                IJ.log("Problem: copying TaggedImage: "+this.getClass().getName());
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        } else {
            return null;
        } 
   }
   
   protected boolean isPoison(E element) {
        if (element instanceof TaggedImage)
            return TaggedImageQueue.isPoison((TaggedImage)element);
        else if (element instanceof File)
            return ImageFileQueue.isPoison((File)element);
        else
            return false;
   }

   
   protected void cleanUp() {
       //empty
       //can be used to freeup allocated resources or complete processing on cached elements (see MutliChannelImageStackAnalyzer)
   }

   @Override
   protected void process() {
//      IJ.log(getClass().getName()+": "+getProcName()+", process");
      E element = poll();
/*        if (element instanceof TaggedImage && TaggedImageQueue.isPoison((TaggedImage)element))
            IJ.log(getClass().getSimpleName()+" "+getProcName()+" : Poison");
        if (element instanceof File && ImageFileQueue.isPoison((File)element))
            IJ.log(getClass().getSimpleName()+" "+getProcName()+" : Poison");*/
      try {
         produce(element);
      } catch (NullPointerException e) {//this means output_ queue is set to null because this is last sibling node in branch 
          //no action required
      } finally {//still need to place in analysisOutput_ queue 
         if (isPoison(element)) { //needs to be passed on unmodified
             cleanUp();
             produceModified(element);
             IJ.log(this.getClass().getName()+" received POISON");
             done=true;
             return;
         }
         if (acceptElement(element)) { 
            List<E> modifiedElements=processElement(element);
            if (modifiedElements!=null) { //image analysis was successful and processed element should be passed to next processor
            /* analyzers that form nodes need to place processed image in 
             * modifiedOutput_ queue
             * for last analyzer in tree branch (='leaf') modifiedOutput_ == null,
             * which is handled in produceModified method
             */
                for (E modElement:modifiedElements)
                    produceModified(modElement);
            }
         }   
      }   
   }   
}   