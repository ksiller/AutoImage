/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage;

import ij.IJ;
import ij.ImagePlus;
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
import org.micromanager.utils.ImageUtils;

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
                    ByteProcessor bp = new ByteProcessor(w, h, img, null);
                    ip = bp;
                    break;
                } 
                case 2: {
                    // 16-bit grayscale pixels
                    short[] img = (short[]) imgArray;
                    ShortProcessor sp = new ShortProcessor(w, h, img, null);
                    ip = sp;
                    break;
                } 
                case 4: {
                    // color pixels
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
/*                    int type=ImagePlus.COLOR_RGB;
                    if (imgArray instanceof short[]) {
                        //convert byte[] to int[] 
                        short[] shortArray=(short[])imgArray;
                        int[] intArray = new int[shortArray.length/2];
                        for (int i=0; i<intArray.length; ++i) {
                            intArray[i] =  shortArray[2*i]
                  	                 + (shortArray[2*i + 1] << 16);
                  	}
	                imgArray = intArray;
	            }
                    IJ.log("before Color ip");
	            ip=new ColorProcessor(w, h, (int[]) imgArray);
                    IJ.log("after Color ip");*/
                    ip=ImageUtils.makeProcessor(core, imgArray);
                    IJ.log("after ImageUtils.makeProcessor "+ Boolean.toString(ip!=null));
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
}
