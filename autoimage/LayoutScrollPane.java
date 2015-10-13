package autoimage;

import ij.IJ;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JScrollPane;

/**
 *
 * @author Karsten Siller
 */
public class LayoutScrollPane extends JScrollPane implements IRuleListener {

    @Override
    public void resizeRequested(int newSize) {
        IJ.log("RESIZE: "+newSize);
        Component colView=this.getColumnHeader().getView();
        colView.setPreferredSize(new Dimension(colView.getPreferredSize().width,newSize));
        Component rowView=this.getRowHeader().getView();
        rowView.setPreferredSize(new Dimension(newSize,rowView.getPreferredSize().height));
        revalidate();
//        repaint();
    }
    
}
