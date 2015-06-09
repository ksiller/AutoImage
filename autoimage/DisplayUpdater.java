/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import i5d.ChannelImagePlus;
import i5d.Image5D;
import ij.IJ;
import ij.ImagePlus;
import static ij.ImagePlus.GRAY16;
import ij.ImageStack;
import ij.measure.Calibration;
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

/**
 *
 * @author Karsten
 */

public class DisplayUpdater extends SwingWorker<Void, TaggedImage> implements ImageCacheListener {

    private boolean finished;
    private final List<ImagePlus> impList;
    private Image5D i5d = null;
    private final ImageCache imageCache;
    private double pixelSize;
    private boolean isFirstImage=true;

    public DisplayUpdater(ImageCache ic, List<Channel> channels, Double pixelSize) {
        super();
        finished = false;
        impList = new ArrayList<ImagePlus>();
        imageCache=ic;
        imageCache.addImageCacheListener(this);
        this.pixelSize=pixelSize;
    }

    @Override
    protected Void doInBackground() {
        while (!finished) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    @Override
    protected void process(List<TaggedImage> images) {
        if (!finished) {
            for (TaggedImage lastImage:images) {
                //TaggedImage lastImage = images.get(images.size() - 1);

                final JSONObject metadata = lastImage.tags;
                final Object pixel = lastImage.pix;
                ImageProcessor[] ipArray = MMCoreUtils.createImageProcessor(lastImage, MMCoreUtils.SCALE_NONE); //SCALE_IMAGES);
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
                    ImagePlus imp=null;
                    try {
                        int index = metadata.getInt(MMTags.Image.CHANNEL_INDEX);
                        if (index>=impList.size() || impList.get(index)==null) {
                            imp=MMCoreUtils.createImagePlus(lastImage, MMCoreUtils.SCALE_NONE); //SCALE_IMAGES);
                            impList.add(imp);
                            Calibration cal=imp.getCalibration();
                            cal.setUnit("um");
                            cal.pixelWidth = pixelSize;
                            cal.pixelHeight = pixelSize;                
                            imp.setCalibration(cal);
                        } else {
                            imp = impList.get(index);
                        }    
                        JSONArray color=metadata.getJSONObject(MMTags.Root.SUMMARY).getJSONArray(MMTags.Summary.COLORS);
                        if (color!=null&& !(ipArray[0] instanceof ColorProcessor)) {
                            //set LUT for 8-bit and 16-bit grayscale
                            ipArray[0].setLut(LUT.createLutFromColor(new Color(color.getInt(index))));
                        }    
                    } catch (JSONException je) {
                        IJ.log("DisplayUpdater.process: JSONException - cannot parse image metadata title");
                    }
                    if (ipArray.length==1) {
                        imp.setProcessor(ipArray[0]);
                    } else if (imp.isComposite()) {
                        for (int i=0; i<ipArray.length; i++) {
                            imp.getStack().setProcessor(ipArray[i],i+1);
                        }
                    }
                    try {
                        imp.setTitle(metadata.getString(MMTags.Image.CHANNEL) + ": Area " + metadata.getString(ExtImageTags.AREA_NAME) + ", t" + (metadata.getInt(MMTags.Image.FRAME_INDEX)) + ", z" + (metadata.getInt(MMTags.Image.SLICE_INDEX)));
                    } catch (JSONException je) {
                        IJ.log("DisplayUpdater.process: JSONException - cannot parse image title");
                    }
                    imp.show();

/*                    final TaggedImage image=lastImage;
                    final int w=ipArray[0].getWidth();
                    final int h=ipArray[0].getHeight();
                    
                        int noOfChannels;
                                try {
                                    noOfChannels=metadata.getJSONObject(MMTags.Root.SUMMARY).getInt(MMTags.Summary.CHANNELS);
                                    if (i5d == null) {
                                        i5d = new Image5D("5D", GRAY16, w, h, noOfChannels, 1, 1, true);
                                        ImageStack stack=new ImageStack(w,h);
                                        for (int s=0; s<noOfChannels; s++) {
                                            stack.addSlice(ipArray[0].duplicate());
                                        }
                                        i5d.setStack(stack,noOfChannels,1,1);
                                    }
                                    //i5d.setC(metadata.getInt(MMTags.Image.CHANNEL_INDEX)+1);
//                                       i5d.setPosition(metadata.getInt(MMTags.Image.CHANNEL_INDEX)+1, 1, 1);
                                    i5d.setPixels(image.pix, metadata.getInt(MMTags.Image.CHANNEL_INDEX)+1, 1, 1);
//          //                              i5d.setProcessor(ipArray[0]);
//                                        ImageStack stack=i5d.getImageStack();
                                    ChannelImagePlus cImp=i5d.getChannelImagePlus(metadata.getInt(MMTags.Image.CHANNEL_INDEX)+1);
                                    cImp.setProcessor(ipArray[0]);
//                                        cImp.setTitle(metadata.getString(MMTags.Image.CHANNEL));
                                    JSONArray color=metadata.getJSONObject(MMTags.Root.SUMMARY).getJSONArray(MMTags.Summary.COLORS);
//                                        cImp.setColor(new Color(color.getInt(metadata.getInt(MMTags.Image.CHANNEL_INDEX))));

//                                        IJ.log("StackSize : "+Integer.toString(i5d.getStackSize())
//                                                +"Channel index: " +Integer.toString(metadata.getInt(MMTags.Image.CHANNEL_INDEX))
//                                                +"Stack index: " +Integer.toString(i5d.getImageStackIndex(metadata.getInt(MMTags.Image.CHANNEL_INDEX)+1,1,1)));
                     //               stack.setProcessor(ipArray[0], i5d.getImageStackIndex(metadata.getInt(MMTags.Image.CHANNEL_INDEX)+1,1,1));
                                    i5d.show();
                                    i5d.updateImageAndDraw();
                                } catch (JSONException je) {
                                    IJ.log("PROBLEM");
                                }
//                            }});*/
                    
/*                        ImageWindow window=imp.getWindow();
                    if (window!=null && newWindow) {
                        window.setSize((int)window.getSize().getWidth()/2,(int)window.getSize().getHeight()/2);
                        imp.getCanvas().fitToWindow();
                    }*/
                }
            }
        }
    }

    @Override
    protected void done() {
        imageCache.removeImageCacheListener(this);
        for (int i = impList.size() - 1; i >= 0; i--) {
            impList.get(i).close();
        }
        impList.clear();
        i5d.close();
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

