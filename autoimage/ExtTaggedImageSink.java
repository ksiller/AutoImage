/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import mmcorej.TaggedImage;
import org.json.JSONException;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.ImageCache;
import org.micromanager.utils.MMException;
import org.micromanager.utils.ReportingUtils;

/**
 *
 * @author Karsten
 * design based on DefaultTaggedImageSink
 */
public class ExtTaggedImageSink {
    
   private final BlockingQueue<TaggedImage> imageProducingQueue_;
   private ImageCache imageCache_ = null;
   private final BlockingQueue<File> fileOutputQueue_;
   
   public ExtTaggedImageSink(BlockingQueue<TaggedImage> imageProducingQueue,
                  ImageCache imageCache, BlockingQueue<File> oq) {
      imageProducingQueue_ = imageProducingQueue;
      imageCache_ = imageCache;
      fileOutputQueue_ = oq;
   }
   
   
   public void start() {
      Thread savingThread = new Thread("ExtTaggedImageSink thread") {
         @Override
         public void run() {
            long t1 = System.currentTimeMillis();
            int imageCount = 0;
            try {
               while (true) {
                  TaggedImage image = imageProducingQueue_.poll(1, TimeUnit.SECONDS);
                  if (image != null) {
                     if (!TaggedImageQueue.isPoison(image)) {
                         ++imageCount;
                         imageCache_.putImage(image);
                         if (fileOutputQueue_!=null)
                            fileOutputQueue_.put(MMCoreUtils.getImageAsFileObject(image));
                     } else {
                         if (fileOutputQueue_!=null)
                            fileOutputQueue_.put(ImageFileQueue.POISON);
                         break;
                     }    
                  }
               }
            } catch (IOException ex2) {
               ReportingUtils.logError(ex2);
            } catch (InterruptedException ex2) {
                ReportingUtils.logError(ex2);
            } catch (JSONException ex2) {
                 ReportingUtils.logError(ex2);
            } catch (MMException ex2) {
                 ReportingUtils.logError(ex2);
            }
            long t2 = System.currentTimeMillis();
            ReportingUtils.logMessage(imageCount + " images stored in " + (t2 - t1) + " ms.");
            imageCache_.finished();
         }
      };
      savingThread.start();
   }
   public ImageCache getImageCache() {
      return imageCache_;
   }
}
    
    
