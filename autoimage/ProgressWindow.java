/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 *
 * @author Karsten
 */
public class ProgressWindow extends JPanel {
	
	   private static final long serialVersionUID = 1L;
	   private JProgressBar progressBar;
	   private JFrame frame;

	
	   public ProgressWindow (String windowName, int start, int end) {
	      super(new BorderLayout());
	     
	      frame = new JFrame(windowName);
	      frame.setDefaultCloseOperation (JFrame.DISPOSE_ON_CLOSE);
	      frame.setBounds(0,0,150 + 6 * windowName.length() ,100);
	
	      progressBar = new JProgressBar(start,end);
	      progressBar.setValue(0);
	      JPanel panel = new JPanel(new BorderLayout());
	      panel.add(progressBar, BorderLayout.CENTER);
	      add(panel, BorderLayout.CENTER);
	      panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
	
	      JComponent newContentPane = panel;
	      newContentPane.setOpaque(true);
	      frame.setContentPane(newContentPane);
	
	      frame.setLocationRelativeTo(null);
              //frame.setAlwaysOnTop(true);
	      frame.setVisible(true);
	   }
	
	   public void setProgress(int progress) {
	      progressBar.setValue(progress);
	      progressBar.repaint();
	   }
	
	   @Override
	   public void setVisible(boolean visible) {
	      frame.setVisible(visible);
	   }
	
	   public void dispose() {
	      frame.dispose();
	   }
	    public void setRange(int min, int max) {
	        progressBar.setMinimum(min);
	        progressBar.setMaximum(max);
	    }    
}
