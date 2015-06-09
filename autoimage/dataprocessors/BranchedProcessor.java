package autoimage.dataprocessors;

import autoimage.ImageFileQueue;
import autoimage.MMCoreUtils;
import autoimage.Utils;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
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
import org.micromanager.acquisition.TaggedImageStorageDiskDefault;
import org.micromanager.api.MMTags;
import org.micromanager.utils.ImageUtils;
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
 * A copy of the element will be pushed through to the modifiedOutput_ queue. 
 * This copied element can be modified, for example a GroupProcessor could collect
 * all slices of a z-stack and create a single image z-projection that gets pushed
 * to the queue of the next DataProcessor.
 * 
 * The tree architecture has nodes (with modifiedOutput_!=null) and leafs (
 * terminal processor in branch: analysisOutput==null)
 * 
 * @author Karsten Siller
 * @param <E> element type in BlockingQueue (e.g. TaggedImage or File)
 */
public abstract class BranchedProcessor<E> extends ExtDataProcessor<E> {

   private BlockingQueue<E> modifiedOutput_;
   protected TaggedImageStorageDiskDefault storage=null;

   public BranchedProcessor () {
        super("","");
   }
    
   public BranchedProcessor (String pName) {
        super(pName);
   }     
   
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
   
   /*
    * The processElement method should be overridden by classes extending
  BranchedProcessorzer to provide a analysis and processing function.
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

   //has to reject TaggedImageQueue.Poison or ImageFileQueue.Poison
   protected abstract boolean acceptElement(E element);
   
   /* if element is modified, a copy needs to be created and saved in workDir
    * createCopy can handle that
    * copy will be passed to analysisOutput_ 
    */
   protected abstract List<E> processElement(E element) throws InterruptedException;
   
   /**
    * Does not do anything by default.
    * Overwrite if metadata update is needed, e.g. after filtering 
    */
   public JSONObject updateTagValue(JSONObject meta,String newDir, String newPrefix, boolean isTaggedImage) throws JSONException {
       return meta;
   }

   /**
    * creates copy of element
    * calls updateTagValue with metadata tags of copied element
    * 
    */
    protected E createCopy(E element) {
    //    JSONObject meta=null;
        E copy=null;
        if (element instanceof File) {
            TaggedImage ti=null;
            ImagePlus imp=IJ.openImage(((File)element).getAbsolutePath());
            if (imp!=null && imp.getProperty("Info") != null) {
                try {
                    JSONObject meta = new JSONObject((String)imp.getProperty("Info"));
                    String newDir=new File(workDir).getParentFile().getAbsolutePath();
                    ImageProcessor ip=imp.getProcessor();
                    if (meta.getJSONObject(MMTags.Root.SUMMARY).getString(MMTags.Summary.PIX_TYPE).equals("RGB32")) {
                        //RGB32 images hold pixel data in int[] --> convert to byte[]
                        ti=new TaggedImage(MMCoreUtils.convertIntToByteArray((int[])ip.getPixels()),new JSONObject(meta.toString()));
                    }
                    else if (meta.getJSONObject(MMTags.Root.SUMMARY).getString(MMTags.Summary.PIX_TYPE).equals("RGB64")) {
                        if (imp.isComposite()) {
                            CompositeImage ci=(CompositeImage)imp;
                            short[] totalArray=new short[ci.getWidth()*ci.getHeight()*4];
                            for (int channel=0; channel< ci.getNChannels(); channel++) {
                                ImageProcessor proc=imp.getStack().getProcessor(channel+1);
                                short[] chPixels=(short[])proc.getPixels();
                                for (int i=0;i<chPixels.length;i++) {
                                    totalArray[(2-channel) + 4*i] = chPixels[i]; // B,G,R
                                }
                            }
                            ti=new TaggedImage(totalArray,new JSONObject(meta.toString()));
                        }
                        
                    } else {//8-bit or 16-bit grayscale
                        ti=ImageUtils.makeTaggedImage(ip);
                        ti.tags=new JSONObject(meta.toString());                        
                    }
                    String newPrefix=new File(workDir).getName();
                    //update metadata
                    ti.tags=updateTagValue(ti.tags, newDir, newPrefix, false);
                    if (storage==null) {
                        storage = new TaggedImageStorageDiskDefault (workDir,true,ti.tags.getJSONObject(MMTags.Root.SUMMARY));
                    }
                    storage.putImage(ti);

                    String posName="";
                    File copiedFile=new File(new File(new File(newDir,newPrefix),meta.getString("PositionName")),
                                                    ti.tags.getString("FileName"));
                    copy=(E)copiedFile;
                } catch (JSONException ex) {
                    IJ.log(this.getClass().getName()+ ": Cannot retrieve 'Info' metadata from file. "+ex);
                    Logger.getLogger(FilterProcessor.class.getName()).log(Level.SEVERE, null, ex);
//                    copy=super.createCopy(element);
                } catch (Exception ex) {
                    IJ.log(this.getClass().getName()+ ": Error writing file to storage. "+ex);
                    Logger.getLogger(FilterProcessor.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                IJ.log(this.getClass().getName()+": Cannot open image");
            }
       } else if (element instanceof TaggedImage) {
            try {                
                TaggedImage modTI=MMCoreUtils.duplicateTaggedImage((TaggedImage)element);
                modTI.tags=updateTagValue(modTI.tags,null,null, true);
                copy=(E)modTI;
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
    
    /*
   protected E createCopy(E element) {
        if (element instanceof File) {
            File f =(File)element;
            try {
                JSONObject meta=Utils.parseMetadataFromFile(f);
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
                modTI.tags=updateTagValue(modTI.tags,null,null, true);
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
   */
   
   protected boolean isPoison(E element) {
        if (element instanceof TaggedImage)
            return TaggedImageQueue.isPoison((TaggedImage)element);
        else if (element instanceof File)
            return ImageFileQueue.isPoison((File)element);
        else
            return false;
   }


   // extends logic of DataProcessor.process() design
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
//             IJ.log(this.getClass().getName()+" received POISON");
             done=true;
             return;
         }
         if (acceptElement(element)) { 
             try {
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