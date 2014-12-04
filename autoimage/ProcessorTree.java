package autoimage;

import autoimage.dataprocessors.ExtDataProcessor;
import autoimage.dataprocessors.BranchedProcessor;
import ij.IJ;
import java.io.File;
import java.util.Enumeration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.tree.DefaultMutableTreeNode;
import mmcorej.TaggedImage;
import org.micromanager.api.DataProcessor;
import org.micromanager.api.TaggedImageAnalyzer;
import org.micromanager.utils.ReportingUtils;

/**
 * Sets up a queue of DataProcessors
 * The static method "run" will chain the given list of DataProcessors to 
 * inputqueue, and return an output queue.  The net result is that each 
 * DataProcessor will modify the image and pass it along to the next 
 * DataProcessor
 * 
 * DataProcessor : node with children but no downstream siblings
 *                 passes on modified image to children
 * ImageAnalyzer : leaf (no children)
 *                 passes unmodified image to sibling, runs analysis
 * SeqAnalyer    : node or leaf with children and possible downstream siblings
 *                 passes unmodified image to sibling
 *                 creates copy of image that can be modified and passed on to
 *                 children
 *
 * @author Karsten
 */
public class ProcessorTree<E> {

   private final DefaultMutableTreeNode root_;
   
   private final BlockingQueue<E> input_;
   private final BlockingQueue<E> output_;
   
   public final static String PROC_NAME_ACQ_ENG = "Acquisition Engine";
   public final static String PROC_NAME_IMAGE_STORAGE = "Image Storage";
   public final static String PROC_NAME_TAG_FILTER = "Tags";
   public final static String PROC_NAME_CHANNEL_FILTER = "Channel";
   public final static String PROC_NAME_Z_FILTER = "Z-Position";
   public final static String PROC_NAME_AREA_FILTER = "Area";
   public final static String PROC_NAME_TIMEPOINT_FILTER = "Timepoint";


   
   public ProcessorTree(BlockingQueue<E> input, DefaultMutableTreeNode root) {

      /* TaggedImageAnalyzers are linear processors and should 
       * not show up as nodes with children in tree.
       */
      root_=root;
      input_ = input;
      
      /* - create analysisOutput_ queues
       * - set up analysisOutput_ to input_ connections between parent and first child
       */
      BlockingQueue<E> dataIn = input_;
      BlockingQueue<E> dataOut = input_;
      BlockingQueue<E> analysisOut = null;
      DataProcessor currentProc;
      DataProcessor lastProc=null;
      DefaultMutableTreeNode currentNode;
      DefaultMutableTreeNode lastNode=null;
      BlockingQueue<E> finalOut = null;
      
 
      Enumeration<DefaultMutableTreeNode> en = root_.preorderEnumeration();
      while (en.hasMoreElements()) {
         currentNode = en.nextElement();
         currentProc=(DataProcessor)currentNode.getUserObject();
         if (currentNode==root) {
             currentProc.setInput(input_);
            IJ.log("    setting input for  "+currentProc.getClass().getSimpleName());
         }
       //  System.out.println(currentProc.getClass().getName());
         //connect inputQueue
         if (lastNode!=null && lastNode.isNodeRelated(currentNode) && lastNode.isNodeChild(currentNode)) {
            if (lastProc instanceof BranchedProcessor) {// && currentNode.isNodeChild(lastNode)) {
                dataIn=analysisOut;
                if (analysisOut==null)
                    IJ.log("Error: cannot connect input to prevSib analysisOut");
            } else {
                dataIn=dataOut; 
            }
            if (currentProc instanceof ExtDataProcessor 
                 && ((ExtDataProcessor)currentProc).getProcName().equals(PROC_NAME_IMAGE_STORAGE)) {
                finalOut=dataIn;
                dataIn=null;//make sure that placeholder ImageStorageProcessor does not pull images
                dataOut=null;
            }
            IJ.log("    setting input for  "+currentProc.getClass().getSimpleName());
            currentProc.setInput(dataIn);
         }

         //create OutputQueue for currentNode
         if (currentProc instanceof BranchedProcessor) {
            IJ.log("current=BranchedProcessor: "+currentProc.getClass().getSimpleName());
            if (currentNode.getChildCount()>0)
                analysisOut=new LinkedBlockingQueue(1);
            else 
                analysisOut=null;
            ((BranchedProcessor)currentProc).setAnalysisOutput(analysisOut);
            IJ.log("    setting analysisOutput for  "+currentProc.getClass().getSimpleName());
            DefaultMutableTreeNode sibNode=currentNode.getNextSibling();
            if (sibNode != null) {
                dataOut=new LinkedBlockingQueue(1);
                DataProcessor nextSibProc=(DataProcessor)sibNode.getUserObject();
                nextSibProc.setInput(dataOut);
                IJ.log("    setting input for  "+nextSibProc.getClass().getSimpleName());
                if (nextSibProc instanceof ExtDataProcessor 
                 && ((ExtDataProcessor)nextSibProc).getProcName().equals(PROC_NAME_IMAGE_STORAGE)) {
                    finalOut=dataOut;
                    nextSibProc.setInput(null);
                }
            } else 
                dataOut=null;
            currentProc.setOutput(dataOut);
            IJ.log("    setting output for  "+currentProc.getClass().getSimpleName());
         } else if (currentProc instanceof TaggedImageAnalyzer) {
            IJ.log("current=TaggedImageAnalyzer: "+currentProc.getClass().getSimpleName());
            if (currentNode.getChildCount()>0)
                IJ.log("Error: TaggedImageAnalyzer with child node");
            DefaultMutableTreeNode sibNode=currentNode.getNextSibling();
            if (sibNode != null) {
                dataOut=new LinkedBlockingQueue(1);
                DataProcessor nextSibProc=(DataProcessor)sibNode.getUserObject();
                nextSibProc.setInput(dataOut);
                IJ.log("    setting input for  "+nextSibProc.getClass().getSimpleName());
                if (nextSibProc instanceof ExtDataProcessor 
                    && ((ExtDataProcessor)nextSibProc).getProcName().equals(PROC_NAME_IMAGE_STORAGE)) {
                    finalOut=dataOut;
                    nextSibProc.setInput(null);
                }
            } else 
                dataOut=null;
            currentProc.setOutput(dataOut);
            IJ.log("    setting output for  "+currentProc.getClass().getSimpleName());
            analysisOut=null;
                
         } else if (currentProc instanceof DataProcessor) {
            IJ.log("current=DataProcessor: "+currentProc.getClass().getSimpleName());
            if (currentNode.getChildCount()>0) {
                dataOut=new LinkedBlockingQueue(1);
            } else 
                dataOut=null;
            currentProc.setOutput(dataOut);
            IJ.log("    setting output for  "+currentProc.getClass().getSimpleName());
            DefaultMutableTreeNode sibNode=currentNode.getNextSibling();
            if (sibNode != null)
                IJ.log("Error: DataProcessor with downstream sibling node");
            analysisOut=null;
         }
         
         IJ.log(dataIn==null?"   dataIn=null":"   dataIn=ok");
         IJ.log(dataOut==null?"   dataOut=null":"   dataOut=ok");
         IJ.log(analysisOut==null?"   analysisOut=null":"   analysisOut=ok");
         
         lastProc=currentProc;
         lastNode=currentNode;
      }   
      output_=finalOut;
      IJ.log(output_==null?"   output_=null":"   output_=ok");
   }

   
   public BlockingQueue<E> begin() {
      start();
      return output_;
   }

   
   public void start() {
      /* note: the first and last processor are just placeholders to visualize the
       * hubs of the acqEngine and TaggedImageStorage
       */ 
       
      Enumeration<DefaultMutableTreeNode> en = root_.preorderEnumeration();
      while (en.hasMoreElements()) {
         DefaultMutableTreeNode node = en.nextElement();
         DataProcessor proc=(DataProcessor)node.getUserObject();
         if (proc.isAlive())
             IJ.log(proc.getName()+" is already alive");
         if (!(proc instanceof ExtDataProcessor 
                 && ((ExtDataProcessor)proc).getProcName().equals(PROC_NAME_IMAGE_STORAGE)) 
                 && !proc.isAlive()) {
            if (proc.isStarted()) {
               ReportingUtils.showError("Analyzer: " + proc.getName()
                       + " is no longer running. Remove and re-insert to get it to go again");
            } else {
               proc.start();
            }
         }
      }
   }

   
   /**
    * Sets up the DataProcessor<TaggedImage> sequence
    * @param inputQueue
    * @param imageProcessors
    * @return 
    */
   public static BlockingQueue<TaggedImage> runImage(BlockingQueue<TaggedImage> inputQueue,DefaultMutableTreeNode imageProcessorTree) {
      ProcessorTree<TaggedImage> processorTree = 
              new ProcessorTree<TaggedImage>(inputQueue, imageProcessorTree);
      return processorTree.begin();
   }
   
   public static BlockingQueue<File> runFile(BlockingQueue<File> inputQueue, DefaultMutableTreeNode imageProcessorTree) {
      ProcessorTree<File> processorTree = 
              new ProcessorTree<File>(inputQueue, imageProcessorTree);
      return processorTree.begin();
   }
   
   
}
