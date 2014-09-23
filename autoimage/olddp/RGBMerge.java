/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.olddp;

import autoimage.Utils;
import autoimage.dataprocessors.MultiChMultiZAnalyzer;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.RGBStackMerge;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten
 */
public class RGBMerge extends MultiChMultiZAnalyzer<File> {


    public RGBMerge(String path, List<String> channels, List<Long> slices) {
        super("RGB-Merge",path,channels, slices);
    }
    
    @Override
    protected List<File> analyzeGroup(List<File> elements) {
        IJ.log("    Analyze group...");
        if (elements!=null && elements.size()>0) {
            try {
                Map <Long,List<File>> sliceMap = Utils.getSliceIndexMap(elements);
                Iterator it = sliceMap.entrySet().iterator();
                String path=Utils.createPathForSite(elements.get(0),workDir, false);
                List<File> destFile = new ArrayList<File>();
//                ImagePlus stackImp=IJ.createHyperStack("Merged_Stack",512,512,0,0,1,16);

                while (it.hasNext()) {
                    Map.Entry pairs = (Map.Entry)it.next();
                    List<File> chList=(List<File>)pairs.getValue();

                    ImagePlus[] impArray=new ImagePlus[chList.size()];
                    int i=0;
                    JSONObject meta=null;
                    for (File f:chList) {
                        meta=Utils.parseMetadata(f);
                        impArray[i]=IJ.openImage(f.getAbsolutePath());
                        IJ.log("       "+f.getName());
                        i++;
                    }
                    RGBStackMerge merger=new RGBStackMerge();
                    ImagePlus resultImp=merger.mergeHyperstacks(impArray, false);
//                    for (int j=0;j<mergedImp.getStackSize();j++)
//                        stackImp.getImageStack().addSlice(mergedImp.getImageStack().getProcessor(j));
                    Utils.calibrateImage(resultImp, meta);

                    destFile.add(new File (path,"RGB-Merge-z"+Long.toString((Long)pairs.getKey())+".tif"));
//                    if (!IJ.saveAsTiff(resultImp, destFile.getAbsolutePath()))
//                        destFile=null;
                    it.remove(); // avoids a ConcurrentModificationException
                }
//                destFile=new File (path,"RGB-Merge-stack.tif");
//                if (!IJ.saveAsTiff(stackImp, destFile.getAbsolutePath()))
//                    destFile=null;
                return destFile;
            } catch (JSONException ex) {
                Logger.getLogger(RGBMerge.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        } else
            return null;
    }

}
