/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage.dataprocessors;

import autoimage.Utils;
import ij.IJ;
import ij.ImagePlus;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import mmcorej.TaggedImage;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.acquisition.TaggedImageStorageDiskDefault;
import org.micromanager.api.MMTags;
import org.micromanager.utils.ImageUtils;
import org.micromanager.utils.MMException;

/**
 *
 * @author Karsten
 */
public class ImageStorage<E> extends BranchedProcessor<E> {
    
    protected TaggedImageStorageDiskDefault storage=null;

    @Override
    protected boolean acceptElement(E element) {
        return true;
    }

    @Override
    protected List<E> processElement(E element) {
        JSONObject summary;
        try {
            summary = Utils.readMetadataSummary(element,true);
            if (element instanceof File) {
                TaggedImage ti=Utils.openAsTaggedImage(((File)element).getAbsolutePath());
                if (storage==null) {
                    summary.put(MMTags.Summary.DIRECTORY, new File(workDir).getParent());
                    summary.put(MMTags.Summary.PREFIX, new File(workDir).getName());
                    storage = new TaggedImageStorageDiskDefault(workDir,true,summary);
                }
                storage.putImage(ti);
            } else if (element instanceof TaggedImage) {
                if (storage==null) {
                    storage = new TaggedImageStorageDiskDefault(workDir,true,summary);
                }
                storage.putImage((TaggedImage)element);
            }
        } catch (JSONException ex) {
            IJ.log(this.getClass().getName()+": Cannot parse summary data");
            Logger.getLogger(ImageStorage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MMException ex) {
            IJ.log(this.getClass().getName()+": Cannot save data");
            Logger.getLogger(ImageStorage.class.getName()).log(Level.SEVERE, null, ex);    
        } catch (Exception ex) {
            IJ.log(this.getClass().getName()+": Cannot initialize storage");
            Logger.getLogger(ImageStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new ArrayList<E>();
    }
    
    @Override
    protected void cleanUp() {
        if (storage!=null) {
            storage.close();
            storage=null;
        }
    }
}
