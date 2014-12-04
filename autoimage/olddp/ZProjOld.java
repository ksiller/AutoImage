/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.olddp;

import autoimage.Utils;
import autoimage.dataprocessors.MultiChMultiZAnalyzer;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;
import ij.process.LUT;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import mmcorej.TaggedImage;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.acquisition.TaggedImageStorageDiskDefault;
import org.micromanager.api.MMTags;
import org.micromanager.api.TaggedImageStorage;
import org.micromanager.utils.ImageUtils;

/**
 *
 * @author Karsten
 */
public class ZProjOld extends MultiChMultiZAnalyzer<File> {

    public ZProjOld() {
        this("",null,null);
    }

    public ZProjOld(String path, List<String> channels, List<Long> slices) {
        super("Z-Project",path,channels, slices);
    }
    
    @Override
    protected List<File> analyzeGroup(List<File> elements) {
        IJ.log("    Analyze group...");
        if (elements!=null && elements.size()>0) {
            try {
                
                Map <String,List<File>> channelMap = Utils.getChannelMap(elements);
                Iterator it = channelMap.entrySet().iterator();
                String path = Utils.createPathForSite(elements.get(0),workDir, false);
                List<File> zProjFiles=new ArrayList<File>();
//                ImagePlus stackImp=IJ.createHyperStack("Merged_Stack",512,512,0,0,1,16);

                while (it.hasNext()) {
                    Map.Entry pairs = (Map.Entry)it.next();
                    List<File> zList=(List<File>)pairs.getValue();

                    ImagePlus[] impArray=new ImagePlus[zList.size()];
                    int i=0;
                    //ImagePlus stackImp=new ImagePlus("Z-stack",0);
                    ImageStack stack=null;
                    LUT lut=null;
                    JSONObject meta=null;
                    for (File file:zList) {
                        impArray[i]=IJ.openImage(file.getAbsolutePath());
                        if (i==0) {
                            stack=impArray[0].createEmptyStack();
                            meta=readMetadata(file);
                        }
                        ImageProcessor ip=impArray[i].getProcessor();
                        lut=ip.getLut();
                        //ip.rotateLeft();
                        stack.addSlice(ip.duplicate());
                        //stack.addSlice(impArray[i].getProcessor());
                        IJ.log("       "+file.getName()+", stack size:"+Integer.toString(stack.getSize()));
                        i++;
                    }
                    IJ.log(lut.toString());
                    ImagePlus zProjImp=new ImagePlus("Z-Proj",stack);
                    ZProjector projector=new ZProjector(zProjImp);
                    projector.setMethod(ZProjector.MAX_METHOD);
                    projector.doProjection();
                    ImagePlus resultImp=projector.getProjection();

                    TaggedImage ti=ImageUtils.makeTaggedImage(resultImp.getProcessor());
                    JSONObject summary=meta.getJSONObject(MMTags.Root.SUMMARY);
                    meta.put(MMTags.Image.SLICE_INDEX, 0);
                    meta.put(MMTags.Image.SLICE, 0);
                    summary.put(MMTags.Summary.SLICES, 1);
                    String area = meta.getString("Area")+"-";
                    String cluster="Cluster"+Long.toString(meta.getLong("ClusterIndex"))+"-";
                    String site="Site"+
//                    String prefix=area+cluster;
//                    prefix=prefix.substring(0,prefix.lastIndexOf("-"));
                    summary.put(MMTags.Summary.PREFIX,meta.getString("PositionName")); 
                    summary.put(MMTags.Summary.DIRECTORY, workDir);

                    ti.tags=meta;
                    TaggedImageStorage storage=null;
                    try {
                        storage = new TaggedImageStorageDiskDefault(workDir,true,summary);
                        storage.putImage(ti);
                    } catch (Exception ex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                    }
                    if (storage!=null)
                        storage.close();
                    
//                    resultImp.getProcessor().setLut(lut);
//                    Utils.calibrateImage(resultImp, meta);
                    //resultImp.getProcessor().setLut(impArray[0].getProcessor().getLut());
                    zProjFiles.add(new File(new File(workDir,meta.getString("PositionName")),
                                                    meta.getString("FileName")));                    
                    IJ.log("ZProject result: "+zProjFiles.get(zProjFiles.size()-1).getAbsolutePath());
//                    if (!IJ.saveAsTiff(resultImp, destFile.getAbsolutePath()))
//                        destFile=null;
                    it.remove(); // avoids a ConcurrentModificationException
                }
//                destFile=new ZProjOld(path,"ZProj-stack.tif");
//                if (!IJ.saveAsTiff(stackImp, destFile.getAbsolutePath()))
//                    destFile=null;
                return zProjFiles;
            } catch (JSONException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        } else
            return null;
    }
    
}
