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
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import mmcorej.CMMCore;
import mmcorej.TaggedImage;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.ImageUtils;

/**
 *
 * @author Karsten
 */
public class AutoExposureTool implements Callable<Double> {

    private final String channelGroup;
    private final String channel;
    private double exposure;
    private CMMCore core;
    private boolean showImages;
    ScriptInterface gui;
//    boolean done=false;
    
    
    private static double MAX_EXPOSURE = 2000; //ceiling for exposure time (to make sure it stops for dim samples
    private static double MAX_SATURATION = 0.015;//fraction of pixel allowed to have max intensity 
    
    public AutoExposureTool (ScriptInterface gui, String chGroup, String ch, double startExp, boolean show) {
        channelGroup=chGroup;
        channel=ch;
        exposure=startExp;
        this.core=gui.getMMCore();
        showImages=show;
    }
    /*  
    private ImageProcessor snapImage(double exp) {
        ImageProcessor ip = null;
        try {
            core.setConfig(channelGroup, channel);
            core.setExposure(exp);
            core.waitForSystem();
            core.snapImage();

            Object imgArray = core.getImage();
            long w = core.getImageWidth();
            long h = core.getImageHeight();
            
            switch ((int)core.getBytesPerPixel()) {
                case 1: {
                    // 8-bit grayscale pixels
                    byte[] img = (byte[]) imgArray;
                    ByteProcessor bp = new ByteProcessor((int) w, (int) h, img);
                    ip = bp;
                    break;
                } case 2: {
                    // 16-bit grayscale pixels
                    short[] img = (short[]) imgArray;
                    ShortProcessor sp = new ShortProcessor((int) w, (int) h, img, null);
                    ip = sp;
                    break;
                } case 4: {
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
	            ip=new ColorProcessor((int)w, (int)h, (int[]) imgArray);
                    break;
                }
                default: {
                    IJ.log(this.getClass().getName()+": Unknown image type ("+Long.toString(core.getBytesPerPixel())+" bytes/pixel)");        
                    break;
                }
            }
        } catch (Exception ex) {
            IJ.log(this.getClass().getName()+" snapImage: Exception.");
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            ip = null;
        }
        return ip;
    }
    */
    @Override
    public Double call() {
        final JFrame frame=new JFrame("Auto-Exposure");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setPreferredSize(new Dimension(400,120));
        frame.setResizable(false);
        frame.getContentPane().setLayout(new GridLayout(0,1));

        JLabel label=new JLabel("Channel '"+channel+"': Testing exposure");
        final JLabel expLabel=new JLabel("");

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(10, 0)));
        panel.add(expLabel);
        panel.add(Box.createHorizontalGlue());

        frame.getContentPane().add(panel);

        final SwingWorker<Double, Double> worker = new SwingWorker<Double, Double>() {

            private double optimalExp;
            private ImageProcessor ip=null;
            private ImagePlus imp = null;

            @Override
            protected Double doInBackground() {

                double newExp=exposure;
                publish(newExp);
                double maxExp=-1;
                double minExp=1;
                boolean optimalExpFound=false;
                while (!optimalExpFound && !isCancelled()) {
                    ip = MMCoreUtils.snapImage(core, channelGroup, channel, newExp);
                    ImageStatistics stats=ip.getStatistics();
                    if ((stats.max < Math.pow(2,core.getImageBitDepth())-1)
                        || stats.maxCount < MAX_SATURATION*ip.getWidth()*ip.getHeight()){
                        //underexposed
                        minExp=newExp;
                        if (maxExp==-1)
                            newExp*=2;
                        else
                            newExp=(minExp+maxExp)/2;
                        if (newExp >= MAX_EXPOSURE) {
                            newExp=MAX_EXPOSURE;
                            maxExp=MAX_EXPOSURE;
                        }
                    }  else {
                        //overexposed
                        maxExp=newExp;
                        newExp=(minExp+maxExp)/2;
                        if (newExp <=0) {
                            break;
                        }
                    }   
                    publish(newExp);// Notify progress
                    optimalExpFound=Math.abs(maxExp/minExp - 1) < 0.025; 
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (!isCancelled()) {
                    optimalExp=(maxExp+minExp)/2;
                } else
                    optimalExp=-1;
                return optimalExp;
            }

            @Override
            protected void process(List<Double> exp) {
                String msg=String.format("%1$,.1f", exp.get(exp.size()-1))+" ms";
                expLabel.setText(msg);
            }

            @Override
            protected void done() {
                frame.dispose();
                if (showImages && ip!=null) {
                    if (imp==null) {
                        imp=new ImagePlus("Auto-Exposure: "+channel,ip);
                    } else {
                        imp.setProcessor(ip);
                    }
                    imp.show();
                }
                if (!isCancelled()) {
                    if (optimalExp==MAX_EXPOSURE) {
                        JOptionPane.showMessageDialog(null, "Reached maximum exposure ("+MAX_EXPOSURE+" ms).");
                    }
                }
//                done=true;
            }

        };

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                worker.cancel(true);
            }
        });

        JButton cancelButton=new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                worker.cancel(true);
            }
        });
        JPanel buttonPanel=new JPanel();
        buttonPanel.add(cancelButton);
        frame.getContentPane().add(buttonPanel);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        worker.execute();
        while (worker.isDone()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(AutoExposureTool.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return new Double(100);
    }
    
/*
    public boolean isDone() {
        return done;
    }
*/    
    public double getMaxExposure() {
        return MAX_EXPOSURE;
    }
    
    public void setMaxExposure(double exp) {
        if (exp>0)
            MAX_EXPOSURE=exp;
    }
    
    public double getMaxSaturation() {
        return MAX_SATURATION;
    }
    
    public void setMaxSaturation(double saturation) {
        if (saturation>0 && saturation <1) {
            MAX_SATURATION=saturation;
        }
    }
    
    public double getOptimalExposure() {
        return exposure;
    }
    

}
