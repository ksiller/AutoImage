/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import javax.swing.JFrame;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

/**
 *
 * @author Karsten
 */
public class TileCalculator {
    

    public TileCalculator(JFrame acqFrame){
        final ProgressMonitor pm = new ProgressMonitor(acqFrame, "checking", "...", 0, 100);
        final SwingWorker sw = new SwingWorker<Integer, Integer>()
        {
            protected Integer doInBackground() throws Exception 
            {
                int i = 0;
                //While still doing work and progress  monitor wasn't canceled
                 while (i++ < 100 && !pm.isCanceled()) {
                     //System.out.println(i);
                     publish(i);
                     Thread.sleep(100);
                 }
                 return null;
            }


             @Override
             protected void process(java.util.List<Integer> chunks) {
                 for (int number : chunks) {
                     pm.setProgress(number);
                 }
             }

             @Override
             protected void done() {
             }
        };
        
        pm.setMillisToDecideToPopup(10);
        pm.setMillisToPopup(20);
        sw.execute();
    }

    
}
