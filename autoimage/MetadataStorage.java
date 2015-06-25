/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import java.io.File;
import java.io.IOException;
import org.json.JSONObject;
import org.micromanager.MMStudio;
import org.micromanager.acquisition.TaggedImageStorageDiskDefault;
import org.micromanager.utils.JavaUtils;
import org.micromanager.utils.MDUtils;
import org.micromanager.utils.MMException;
import org.micromanager.utils.ReportingUtils;

/**
 *
 * @author Karsten
 */
public class MetadataStorage extends TaggedImageStorageDiskDefault {
    
    MetadataStorage (String dir) throws Exception {
        super(dir);
    }
    
    MetadataStorage (String dir, Boolean newDataSet,JSONObject summaryMetadata) throws Exception {
        super(dir, newDataSet, summaryMetadata);
    }
    
/*    public void putImage(TaggedImage taggedImg) throws MMException {
        try {
            if (!newDataSet_) {
                throw new MMException("This ImageFileManager is read-only.");
            }
            if (!metadataStreams_.containsKey(MDUtils.getPositionIndex(taggedImg.tags))) {
                try {
                    openNewDataSet(taggedImg);
                } catch (Exception ex) {
                    ReportingUtils.logError(ex);
                }
            }
            JSONObject md = taggedImg.tags;
            Object img = taggedImg.pix;
            String tiffFileName = createFileName(md);
            MDUtils.setFileName(md, tiffFileName);
            String posName;
            String fileName = tiffFileName;
            try {
                posName = positionNames_.get(MDUtils.getPositionIndex(md));
                if (posName != null && posName.length() > 0 && 
                    !posName.contentEquals("null")) {
                    JavaUtils.createDirectory(dir_ + "/" + posName);
                    fileName = posName + "/" + tiffFileName;
                } else {
                    fileName = tiffFileName;
                }
            } catch (Exception ex) {
                ReportingUtils.logError(ex);
            }         

            File saveFile = new File(dir_, fileName);
            if (saveFile.exists()) {
                MMStudio.getInstance().stopAllActivity();
                throw new IOException("Image saving failed: " + saveFile.getAbsolutePath());
            }
            
            saveImageFile(img, md, dir_, fileName); 
            writeFrameMetadata(md);
            String label = MDUtils.getLabel(md);
            filenameTable_.put(label, fileName);
        } catch (Exception ex) {
            ReportingUtils.showError(ex);
        }
    }*/
}
