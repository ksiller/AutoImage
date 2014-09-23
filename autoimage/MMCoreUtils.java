/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage;

import ij.process.ByteProcessor;
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
        return cGroupStr;
        /*
         for (int j=0; j<availableChannelList.size(); j++) {
         try {
         PropertySetting s = cdata.getSetting(j);
         System.out.println(" " + s.getDeviceLabel() + ", " + s.getPropertyName() + ", " + s.getPropertyValue());
         } catch (Exception ex) {
         Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
         }
         }
         }
         StrVector channelList=core.getAvailableConfigs("Channels");
         String channelList = core.getChannelGroup();
         for (int i=0; i<channelList.size(); i++)
         IJ.showMessage("core.getChannelGroup",channelList.get(i));

         */
    }

    public static ImageProcessor snapImage(CMMCore core, String chGroupStr, String ch, double exp) {
        ImageProcessor ip = null;
        try {
//            core.setConfig(channelGroupStr, ch);
            core.setConfig(chGroupStr, ch);
            core.setExposure(exp);
            core.waitForSystem();
            core.snapImage();
            if (core.getBytesPerPixel() == 1) {
                // 8-bit grayscale pixels
                byte[] img = (byte[]) core.getImage();
                long w = core.getImageWidth();
                long h = core.getImageHeight();
                ByteProcessor bp = new ByteProcessor((int) w, (int) h, img);
                ip = bp;
            } else if (core.getBytesPerPixel() == 2) {
                // 16-bit grayscale pixels
                short[] img = (short[]) core.getImage();
                long w = core.getImageWidth();
                long h = core.getImageHeight();
                ShortProcessor sp = new ShortProcessor((int) w, (int) h, img, null);
                ip = sp;
            } else {
                ip = null;
            }
        } catch (Exception ex) {
//            IJ.log("MMCoreUtils.snapImage: Exception.");
            Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            ip = null;
        }
        return ip;
    } 
}
