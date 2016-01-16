package autoimage.gui.views;

import autoimage.events.IRuleListener;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JScrollPane;

/**
 *
 * @author Karsten Siller
 */
public class LayoutScrollPane extends JScrollPane implements IRuleListener {

    @Override
    public void resizeRequested(int newSize, Component source) {
        Component colView=this.getColumnHeader().getView();
        colView.setPreferredSize(new Dimension(colView.getPreferredSize().width,newSize));
        Component rowView=this.getRowHeader().getView();
        rowView.setPreferredSize(new Dimension(newSize,rowView.getPreferredSize().height));
        revalidate();
//        repaint();
    }
    
}
