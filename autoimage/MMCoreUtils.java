package autoimage;

import autoimage.gui.AcqFrame;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.RGBStackMerge;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import mmcorej.CMMCore;
import mmcorej.StrVector;
import org.micromanager.utils.ReportingUtils;

/**
 *
 * @author Karsten Siller
 */
public class MMCoreUtils {
    
//    public static List<String> availableChannelList = new ArrayList<String>();
    public final static int SCALE_CAMERA = -2;
    public final static int SCALE_AUTO = -1;
    public final static int SCALE_NONE = 0;

    public static String[] getPropertyNamesForAFDevice(String devName_, CMMCore core_){
        Vector<String> propNames = new Vector<String>();
        try {
            core_.setAutoFocusDevice(devName_);
            StrVector propNamesVect = core_.getDevicePropertyNames(devName_);
            for (int i = 0; i < propNamesVect.size(); i++)
                if (!core_.isPropertyReadOnly(devName_, propNamesVect.get(i))
	                  && !core_.isPropertyPreInit(devName_,
	                        propNamesVect.get(i)))
	               propNames.add(propNamesVect.get(i));
        } catch (Exception e) {
           ReportingUtils.logError(e);
        }
        return propNames.toArray(new String[propNames.size()]);
    }
    
    
    public static String selectDeviceStr(JFrame parent, CMMCore core, String device) {
        StrVector configs = core.getLoadedDevices();
        String[] options = new String[(int) configs.size()];
        //populate array
        for (int i = 0; i < configs.size(); i++) {
            options[i] = configs.get(i);
        }
        String s = (String) JOptionPane.showInputDialog(
                    parent,
                    "Select property group for "+device+":",
                    "Property Group",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    configs.get(0));
        if ((s != null) && (s.length() > 0)) { 
            return s;
        } else {
            return null;
        }
    } 
    
    public static String changeConfigGroupStr(JFrame parent, CMMCore core, String groupName, String groupStr) {
        StrVector configs = core.getAvailableConfigGroups();
        String[] options = new String[(int) configs.size()];
        for (int i = 0; i < configs.size(); i++) {
            options[i] = configs.get(i);
        }
        String s = (String) JOptionPane.showInputDialog(
                    parent,
                    "Select "+groupName+" configuration group:",
                    "Configuration Group",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    configs.get(0));
        if ((s != null) && (s.length() > 0)) {
            groupStr = s;
            configs = core.getAvailableConfigs(groupStr);
            if ((configs == null) || configs.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "No configurations found for "+groupStr+".\n\nPresets need to be defined for this group!");
            }
            return groupStr;
        } else {
            return null;
        }
    } 

    public static String verifyConfigGroupOld(JFrame parent, CMMCore core, String cGroupStr) {
        if (cGroupStr==null) {
            return null;
        }
        StrVector availableCh = core.getAvailableConfigs(cGroupStr);
        if (availableCh != null) {// && !availableCh.isEmpty()) {
            return cGroupStr;
        } else {      
            StrVector groups = core.getAvailableConfigGroups();
            String[] options;
            if (groups==null) {
                options=new String[0];
            } else {
                options=groups.toArray();
            }
            String s = (String) JOptionPane.showInputDialog(parent,
                    "Could not find Configuration Group '" + cGroupStr + "'.\n\nChoose Channel Configuration Group:",
                    "Error",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    groups.get(0));
            if ((s != null) && (s.length() > 0)) {
                cGroupStr=s;
/*                if ((availableCh == null) || availableCh.isEmpty()) {
                    JOptionPane.showMessageDialog(parent, "No Channel configurations found.\n\nPresets need to be defined in the Channel group!");
                }*/
                return cGroupStr;
            } else {
                return null;
            }
        }
    }

    public static String verifyAndSelectConfigGroup(JFrame parent, CMMCore core, String cGroupStr) {
        StrVector groups = core.getAvailableConfigGroups();
        String[] options;
        if (groups==null) {
            options=new String[0];
        } else {
            options=groups.toArray();
        }
        boolean found=false;
        for (String o:options) {
            if (o.equals(cGroupStr)) {
                found=true;
                break;
            }
        }
        if (found) {
            return cGroupStr;
        } else {
            cGroupStr = (String) JOptionPane.showInputDialog(parent,
                    "Could not find Configuration Group '" + cGroupStr + "'.\n\nChoose Channel Configuration Group:",
                    "Error",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    groups.get(0));
            if ((cGroupStr != null) && (cGroupStr.length() > 0)) {
                return cGroupStr;
            } else {
                return null;
            }
        }    
    }
    
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
                //return to pre-snap z-position position
                core.setRelativePosition(zStageLabel, -zOffs);
                core.waitForDevice(zStageLabel);
            }            
        } catch (Exception ex) {
            IJ.log("AcqFrame.snapAndDisplayImage: Exception - "+ex.getMessage()+".");
            ip = null;
        }
        return ip;
    }
    
    public static ImagePlus snapImagePlus(CMMCore core, String chGroupStr, String channel, double exp, double zOffset, int scaleMode) {
        ImagePlus imp=null;
        long fullBitRange;
        ImageProcessor[] ipArray=snapImageWithZOffset(core, chGroupStr, channel, exp, zOffset);
        if (ipArray==null) {
            return null;
        }
        if (ipArray.length == 1) {
            imp=new ImagePlus(channel, ipArray[0]);
            if (ipArray[0] instanceof ColorProcessor) {
                //RGB32
                fullBitRange=8;
            } else {
                //8bit or 16 bit grayscale
                fullBitRange=8*core.getBytesPerPixel();
            }    
            switch (scaleMode) {
                case SCALE_CAMERA: {
                        for (ImageProcessor ip:ipArray)
                            ip.setMinAndMax(0, Math.pow(2, core.getImageBitDepth()));
                        break;
                    }
                case SCALE_AUTO: {  
                        for (ImageProcessor ip:ipArray) {
                            ImageStatistics stats=ip.getStatistics();
                            ip.setMinAndMax(0, stats.max);
                        }
                        break; 
                    }
                case SCALE_NONE: {
                        for (ImageProcessor ip:ipArray)
                            ip.setMinAndMax(0, Math.pow(2, fullBitRange));
                        break; 
                    } 
                default: {
                        for (ImageProcessor ip:ipArray)
                            ip.setMinAndMax(0, Math.pow(2, scaleMode)); 
                    }
            }  
        } else {
            //RGB64
            fullBitRange=16;
            ImagePlus[] impArray=new ImagePlus[ipArray.length]; 
            for (int i=0; i<ipArray.length; i++) {
                impArray[i]=new ImagePlus(channel+"("+Integer.toString(i)+")",ipArray[i]);
                impArray[i].getProcessor().setMinAndMax(0, 65535);
            }
            RGBStackMerge merger=new RGBStackMerge();
            imp=merger.mergeHyperstacks(impArray, false);
            imp.setTitle(channel);
        }
        Calibration cal = imp.getCalibration();
        cal.setUnit("um");
        cal.pixelWidth = core.getPixelSizeUm();
        cal.pixelHeight = core.getPixelSizeUm();
        imp.setCalibration(cal);        
        return imp;
    }

    public static String[] getAvailableConfigs(CMMCore core, String groupStr) {
        StrVector configs=core.getAvailableConfigs(groupStr);     
        if (configs==null)
            return new String[0];
        else
            return configs.toArray();
    }
    

    
}
