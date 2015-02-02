/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.RGBStackMerge;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import mmcorej.CMMCore;
import mmcorej.StrVector;

/**
 *
 * @author Karsten
 */
public class MMCoreUtils {
    
    public static List<String> availableChannelList = new ArrayList<String>();

    public static String loadAvailableChannelConfigs(JFrame parent,String cGroupStr, CMMCore core) {
//        availableCh = core.getAvailableConfigs(channelGroupStr);
        StrVector availableCh = core.getAvailableConfigs(cGroupStr);
        availableChannelList = new ArrayList<String>();
        if (availableCh != null && !availableCh.isEmpty()) {
            for (String ch:availableCh) {
                availableChannelList.add(ch);
            }
        } else {
            StrVector configs = core.getAvailableConfigGroups();
            String[] options = new String[(int) configs.size()];
            for (int i = 0; i < configs.size(); i++) {
                options[i] = configs.get(i);
            }
            String s = (String) JOptionPane.showInputDialog(
                    parent,
                    "Could not find Configuration Group '" + cGroupStr + "'.\n\nChoose Channel Configuration Group:",
                    "Error",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    configs.get(0));
            if ((s != null) && (s.length() > 0)) {
//                channelGroupStr = s;
                cGroupStr=s;
                availableCh = core.getAvailableConfigs(cGroupStr);
                if ((availableCh == null) || availableCh.isEmpty()) {
                    JOptionPane.showMessageDialog(parent, "No Channel configurations found.\n\nPresets need to be defined in the Channel group!");
                } else {
                    for (String ch:availableCh) {
                        availableChannelList.add(ch);
                    }
                }
                return cGroupStr;
            } else {
                return null;
            }
        }
        String c="";
        for (String ch:availableChannelList)
            c=c+ch+", ";         
//        IJ.log("MMCoreUtils.loadAvailableChannelConfigs: Channels found: cGroupStr="+cGroupStr+"; "+c);

        return cGroupStr;
    }

/*    
    public static ImageProcessor snapImage(CMMCore core, String chGroupStr, String ch, double exp) {
        ImageProcessor ip = null;
        try {
            core.setConfig(chGroupStr, ch);
            core.setExposure(exp);
            core.waitForSystem();
            core.snapImage();
            Object imgArray=core.getImage();
            int w = (int)core.getImageWidth();
            int h = (int)core.getImageHeight();
            switch ((int)core.getBytesPerPixel()) {
                case 1: {
                    // 8-bit grayscale pixels
                    byte[] img = (byte[]) imgArray;
                    ip = new ByteProcessor(w, h, img, null);
                    break;
                } 
                case 2: {
                    // 16-bit grayscale pixels
                    short[] img = (short[]) imgArray;
                    ip = new ShortProcessor(w, h, img, null);
                    break;
                } 
                case 4: {
                    // color pixels: RGB32
                    int type=ImagePlus.COLOR_RGB;
                    if (imgArray instanceof byte[]) {
                        //convert byte[] to int[] 
                        byte[] byteArray=(byte[])imgArray;
                        int[] intArray = new int[byteArray.length/4];
                        for (int i=0; i<intArray.length; ++i) {
                            intArray[i] =  byteArray[4*i]
                  	                 + (byteArray[4*i + 1] << 8)
                  	                 + (byteArray[4*i + 2] << 16);
                  	}
	                imgArray = intArray;
	            }
	            ip=new ColorProcessor(w, h, (int[]) imgArray);
                    break;
                }               
                case 8: {
                    // color pixels
                    IJ.log("MMCoreUtils.snapImage: "+Long.toString(core.getBytesPerPixel())+" bytes/pixel) not suppported");
                    break;
                }
                default: {
                    IJ.log("MMCoreUtils.snapImage: Unknown image type ("+Long.toString(core.getBytesPerPixel())+" bytes/pixel)");        
                    break;
                }
            }
        } catch (Exception ex) {
            IJ.log("MMCoreUtils.snapImage: Exception."+ex.getMessage());
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            ip = null;
        }
        return ip;
    }
    */
    public static ImageProcessor[] snapImage(CMMCore core, String chGroupStr, String ch, double exp) {
        ImageProcessor[] ipArray = null;
        try {
            core.setConfig(chGroupStr, ch);
            core.setExposure(exp);
            core.waitForSystem();
            core.snapImage();
            Object imgArray=core.getImage();
            int w = (int)core.getImageWidth();
            int h = (int)core.getImageHeight();
            switch ((int)core.getBytesPerPixel()) {
                case 1: {
                    // 8-bit grayscale pixels
                    byte[] img = (byte[]) imgArray;
                    ipArray=new ImageProcessor[1];
                    ipArray[0] = new ByteProcessor(w, h, img, null);
                    break;
                } 
                case 2: {
                    // 16-bit grayscale pixels
                    short[] img = (short[]) imgArray;
                    ipArray=new ImageProcessor[1];
                    ipArray[0] = new ShortProcessor(w, h, img, null);
                    break;
                } 
                case 4: {
                    // color pixels: RGB32
                    if (imgArray instanceof byte[]) {
                        //convert byte[] to int[] 
                        byte[] byteArray=(byte[])imgArray;
                        int[] intArray = new int[byteArray.length/4];
                        for (int i=0; i<intArray.length; ++i) {
                            intArray[i] =  byteArray[4*i]
                  	                 + (byteArray[4*i + 1] << 8)
                  	                 + (byteArray[4*i + 2] << 16);
                  	}
	                imgArray = intArray;
	            }
                    ipArray=new ImageProcessor[1];
	            ipArray[0]=new ColorProcessor(w, h, (int[]) imgArray);
                    break;
                }               
                case 8: {
                    // color pixels: RGB64
                    if (imgArray instanceof short[]) {
                        short[] shortArray=(short[])imgArray;
                        IJ.log("shortArray: "+shortArray.length);
                        ipArray=new ImageProcessor[3];
                        for (int i=0; i<3; ++i) {//iterate over B, G, R channels
                            short[] channelArray=new short[shortArray.length/4];
                            for (int pixelPos=0; pixelPos<channelArray.length; pixelPos++) {
                                channelArray[pixelPos] =  shortArray[4*pixelPos+i];
                            }
                            //create new ShortProcessor for this channel and add to array
                            //return in R, G, B order
                            ipArray[2-i]=new ShortProcessor(w, h, channelArray, null);
                  	}
                    }
                    break;
                }
                default: {
                    IJ.log("MMCoreUtils.snapImage: Unknown image type ("+Long.toString(core.getBytesPerPixel())+" bytes/pixel)");        
                    break;
                }
            }
        } catch (Exception ex) {
            IJ.log("MMCoreUtils.snapImage: Exception."+ex.getMessage());
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            ipArray = null;
        }
        return ipArray;
    } 
    
    public static ImageProcessor[] snapImageWithZOffset(CMMCore core, String groupStr, String ch, double exp, double zOffs) {
        ImageProcessor[] ip = null;
        try {
            String zStageLabel=core.getFocusDevice();
            if (zOffs!=0) {
                core.setRelativePosition(zStageLabel, zOffs);
                core.waitForDevice(zStageLabel);
            }
            ip=MMCoreUtils.snapImage(core, groupStr, ch, exp);
            if (zOffs!=0) {
                core.setRelativePosition(zStageLabel, -zOffs);
                core.waitForDevice(zStageLabel);
            }            
        } catch (Exception ex) {
            IJ.log("AcqFrame.snapAndDisplayImage: Exception - "+ex.getMessage()+".");
            ip = null;
        }
        return ip;
    }
    
    public static ImagePlus snapImagePlus(CMMCore core, String chGroupStr, String ch, double exp, double zOffset, boolean scale) {
        ImagePlus imp=null;
        ImageProcessor[] ipArray=snapImageWithZOffset(core, chGroupStr, ch, exp, zOffset);
        if (ipArray==null) {
            return null;
        }
        if (ipArray.length == 1) {
            imp=new ImagePlus(ch, ipArray[0]);
            if (!scale) {
                if (core.getBytesPerPixel() == 1 || core.getBytesPerPixel() == 2) {
                    //8bit or 16 bit grayscale
                    ipArray[0].setMinAndMax(0, Math.pow(2,ipArray[0].getBitDepth()));
                }
            }    
        } else {
            //RGB64    
            ImagePlus[] impArray=new ImagePlus[ipArray.length]; 
            for (int i=0; i<ipArray.length; i++) {
                impArray[i]=new ImagePlus(ch+"("+Integer.toString(i)+")",ipArray[i]);
                if (!scale)
                    ipArray[i].setMinAndMax(0, Math.pow(2,ipArray[0].getBitDepth()));
            }
            RGBStackMerge merger=new RGBStackMerge();
            imp=merger.mergeHyperstacks(impArray, false);
            imp.setTitle(ch);
        }
        Calibration cal = imp.getCalibration();
        cal.setUnit("um");
        cal.pixelWidth = core.getPixelSizeUm();
        cal.pixelHeight = core.getPixelSizeUm();
        imp.setCalibration(cal);        
        return imp;
    }
    
}
