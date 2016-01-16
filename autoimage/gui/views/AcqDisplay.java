/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.gui.views;

import autoimage.gui.views.AcqFrame;
import ij.IJ;
import java.awt.Color;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import mmcorej.TaggedImage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.api.ImageCache;
import org.micromanager.api.ImageCacheListener;
import org.micromanager.api.MMTags;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.MMScriptException;

/**
 *
 * @author Karsten Siller
 */
public class AcqDisplay extends SwingWorker<Void, TaggedImage> implements ImageCacheListener {

        private boolean finished;
        private final ImageCache imageCache;
        String acqName;
        ScriptInterface app;

        public AcqDisplay(ScriptInterface si, ImageCache ic, String name) {
            super();
            finished = false;
            imageCache=ic;
            imageCache.addImageCacheListener(this);
            acqName=name;
            app=si;
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
                TaggedImage lastImage = images.get(images.size() - 1);
                JSONObject metadata = lastImage.tags;
                try {
                    int sliceIdx=0;
//                    int sliceIdx = metadata.getInt(MMTags.Image.SLICE_INDEX);
                    int channelIdx = metadata.getInt(MMTags.Image.CHANNEL_INDEX);
                    String channel = metadata.getString(MMTags.Image.CHANNEL_NAME);
                    JSONObject summary = metadata.getJSONObject(MMTags.Root.SUMMARY);
                    int channels=summary.getInt(MMTags.Summary.CHANNELS);
                    int slices=1;
//                    int slices=summary.getInt(MMTags.Summary.SLICES);
                    JSONArray color=summary.getJSONArray(MMTags.Summary.COLORS);
                    JSONArray channelNames=summary.getJSONArray(MMTags.Summary.NAMES);

                    if (!app.acquisitionExists(acqName)) {
                        acqName=app.getUniqueAcquisitionName(acqName);
                        try {
                            app.openAcquisition(acqName, acqName, 1, channels, slices, 1, true, false);
                            for (int i=0; i<channels; i++) {
                                app.setChannelName(acqName, i, channelNames.getString(i));
                                app.setChannelColor(acqName, i, new Color(color.getInt(i)));
                            }
//                            app.autostretchCurrentWindow();
                        } catch (MMScriptException ex) {
                            Logger.getLogger(AcqDisplay.class.getName()).log(Level.SEVERE, null, ex);
                            IJ.log(ex.toString());
                        }
                    }
//                    app.setContrastBasedOnFrame(acqName, 0, 0);
                    app.addImageToAcquisition(acqName, 0, channelIdx, sliceIdx, 0, lastImage);
                } catch (JSONException je) {
                    IJ.log("AcqDisplay.process: JSONException - cannot parse image metadata title");
                } catch (Exception e) {
                    IJ.log(e.toString());
                }
            }
        }

        @Override
        protected void done() {
            imageCache.removeImageCacheListener(this);
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
            try {
//                app.closeAcquisition(acqName);
                app.closeAcquisitionWindow(acqName);
            } catch (MMScriptException ex) {
                Logger.getLogger(AcqDisplay.class.getName()).log(Level.SEVERE, null, ex);
            }
            finished = true;
        }
    }

