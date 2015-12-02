/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import autoimage.api.Channel;
import autoimage.api.ExtImageTags;
import autoimage.gui.AcqFrame;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.measure.Calibration;
import ij.plugin.HyperStackConverter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import mmcorej.TaggedImage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.api.ImageCache;
import org.micromanager.api.ImageCacheListener;
import org.micromanager.api.MMTags;
import org.micromanager.utils.ImageUtils;

/**
 *
 * @author Karsten Siller
 */
public class DisplayUpdater extends SwingWorker<Void, TaggedImage> implements ImageCacheListener {

        private boolean finished;
        private final List<ImagePlus> impList;
        private CompositeImage hyperstack;
        private final ImageCache imageCache;
//        private double pixelSize;
        private boolean isFirstImage=true;

        public DisplayUpdater(ImageCache ic) {
            super();
            finished = false;
            impList = new ArrayList<ImagePlus>();
            imageCache=ic;
            imageCache.addImageCacheListener(this);
            hyperstack=null;
//            pixelSize=pixSize;
        }

        @Override
        protected Void doInBackground() {
            while (!finished) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return null;
        }

        @Override
        protected void process(List<TaggedImage> images) {
            if (!finished) {
//                for (TaggedImage lastImage:images) {
                    TaggedImage lastImage = images.get(images.size() - 1);

                    final JSONObject metadata = lastImage.tags;
                    final Object pixel = lastImage.pix;
//                    ImageProcessor[] ipArray = Utils.createImageProcessor(lastImage, SCALE_IMAGES);
                    ImageProcessor[] ipArray = ImgUtils.createImageProcessor(lastImage, MMCoreUtils.SCALE_AUTO);
                    int x=(int)(Math.random()*ipArray[0].getWidth()/2);
                    int y=(int)(Math.random()*ipArray[0].getHeight()/2);
                    ipArray[0].setColor(Color.black);
                    ipArray[0].drawRect(x, y, 100, 200);
                    if (ipArray==null && isFirstImage) {
                        isFirstImage=false;
                        SwingUtilities.invokeLater(new Runnable() {                          
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(null,"Cannot display this image format.\n"
                                + "The acquisition, image storage, and image processsing will not be affected.");
                            }
                        });
                    } else if (ipArray!=null) {    
//                        boolean newWindow=false;
                        ImagePlus imp=null;
                        try {
                            int index = metadata.getInt(MMTags.Image.CHANNEL_INDEX);
                            String channel = metadata.getString(MMTags.Image.CHANNEL_NAME);
                            JSONObject summary = metadata.getJSONObject(MMTags.Root.SUMMARY);
                            int channels=summary.getInt(MMTags.Summary.CHANNELS);
/*                            if (hyperstack==null) {
                                ImageStack stack=new ImageStack(ipArray[0].getWidth(), ipArray[0].getHeight());
                                stack.addSlice(channel, ipArray[0]);
                                hyperstack=new ImagePlus("Stack",stack);
                            } else {
                                if (index>=hyperstack.getStackSize()) {
                                    hyperstack.getStack().addSlice(channel, ipArray[0]);
                                } else {
                                    hyperstack.getStack().setProcessor(ipArray[0], index+1);
                                }    
                            }*/
//                            int lastIndex=1;
                            if (hyperstack==null || hyperstack.getStackSize() < channels) {
                                if (hyperstack!=null) {
                                    hyperstack.close();
                                }
                                ImagePlus old=new ImagePlus("Stack",ipArray[0]).createHyperStack(
                                        "Stack", 
                                        channels, 
                                        1, 
                                        1, 
                                        ipArray[0].getBitDepth());
                                JSONArray color=summary.getJSONArray(MMTags.Summary.COLORS);
                                for (int c=0;c<channels;c++) {
                                    old.getImageStack().setProcessor(
                                            ipArray[0].createProcessor(ipArray[0].getWidth(), ipArray[0].getHeight()), c+1);                                    
                                }
                                hyperstack=new CompositeImage(old);
                                old.close();
                                for (int c=0;c<channels;c++) {
                                    if (color!=null&& !(ipArray[0] instanceof ColorProcessor)) {
                                        //set LUT for 8-bit and 16-bit grayscale
                                        ipArray[0].setLut(LUT.createLutFromColor(new Color(color.getInt(index))));
                                        hyperstack.setChannelLut(LUT.createLutFromColor(new Color(color.getInt(c))), c+1);
                                    }    
                                }
                                double pixSize = summary.getDouble((MMTags.Summary.PIXSIZE));
                                Calibration cal=hyperstack.getCalibration();
                                cal.setUnit("um");
                                cal.pixelWidth = pixSize;
                                cal.pixelHeight = pixSize;                
                                hyperstack.setCalibration(cal);
                                hyperstack.setMode(CompositeImage.COLOR);
                                WindowManager.toFront(hyperstack.getWindow());
                            } /*else {
                                lastIndex=hyperstack.getSlice();
                            }*/
                            hyperstack.getImageStack().setProcessor(ipArray[0], index+1);
                            hyperstack.getImageStack().setSliceLabel(channel, index+1);
                            hyperstack.setTitle("Area " + metadata.getString(ExtImageTags.AREA_NAME) + ", t" + (metadata.getInt(MMTags.Image.FRAME_INDEX)) + ", z" + (metadata.getInt(MMTags.Image.SLICE_INDEX)));
                            hyperstack.setSlice(index+1);
                        } catch (JSONException je) {
                            IJ.log("DisplayUpdater.process: JSONException - cannot parse image metadata title");
                        } catch (Exception e) {
                            IJ.log(e.toString());
                        }
                        if (hyperstack!=null) {
                            hyperstack.show();
                        }
/*                        ImageWindow window=imp.getWindow();
                        if (window!=null && newWindow) {
                            window.setSize((int)window.getSize().getWidth()/2,(int)window.getSize().getHeight()/2);
                            imp.getCanvas().fitToWindow();
                        }*/
                    }
//                }
            }
        }

        @Override
        protected void done() {
            imageCache.removeImageCacheListener(this);
            for (int i = impList.size() - 1; i >= 0; i--) {
                impList.get(i).close();
            }
            impList.clear();
            IJ.log("DisplayUpdater.done.");

        }

        @Override
        public void imageReceived(TaggedImage ti) {
            if (!finished) {
                publish(ti);
            }
        }

        @Override
        public void imagingFinished(String string) {
            finished = true;
        }
    }

