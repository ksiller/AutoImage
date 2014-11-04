/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import autoimage.dataprocessors.BranchedProcessor;
import autoimage.dataprocessors.ExtDataProcessor;
import autoimage.dataprocessors.ImageTagFilter;
import autoimage.dataprocessors.MultiChMultiZAnalyzer;
import autoimage.dataprocessors.RoiFinder;
import autoimage.dataprocessors.ScriptAnalyzer;
import ij.IJ;
import java.awt.Component;
import java.util.EventObject;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import org.micromanager.api.DataProcessor;
import org.micromanager.api.TaggedImageAnalyzer;

/**
 *
 * @author Karsten
 */
public class ProcessorTreeCellRenderer extends DefaultTreeCellRenderer {
    
    final Icon dpIcon=new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/dp.png"));
    final Icon baIcon=new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/ba.png"));
    final Icon taIcon=new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/ta.png"));
    final Icon imageStorageIcon=new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/harddisk.png"));
    final Icon fChannelIcon=new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Filter_Channel.png"));
    final Icon fTagIcon=new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Filter_Tag.png"));
    final Icon fAreaIcon=new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Area.png"));
    final Icon fZPosIcon=new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Filter_Z.png"));
    final Icon fFrameIcon=new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Filter_Frame.png"));
    final Icon scriptIcon=new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/script.png"));
    final Icon acqEngIcon=new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/microscope.png"));
    final Icon roiIcon=new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/roi.png"));
    final Icon aMC_MZ_Icon=new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/MC-MZ-Analysis.png"));
    
    
    @Override
    public Component getTreeCellRendererComponent(
            JTree tree, Object value, 
            boolean sel, boolean expanded, 
            boolean leaf, int row, boolean hasFocus) {
        
        Component cell=super.getTreeCellRendererComponent(
            tree, value, sel, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode node=(DefaultMutableTreeNode)value;
        DataProcessor dp=(DataProcessor)node.getUserObject();
        if (dp instanceof DataProcessor) {
            setIcon(dpIcon);
        }
        if (dp instanceof TaggedImageAnalyzer) {
            setIcon(taIcon);
        }
        if (dp instanceof BranchedProcessor) {
            setIcon(baIcon);
        }
        if (dp instanceof ExtDataProcessor) {
            String pName=((ExtDataProcessor)dp).getProcName();
            setText(pName);
            if (cell instanceof JComponent) {
                ((JComponent)cell).setToolTipText(((ExtDataProcessor)dp).getToolTipText());
            } else
                IJ.log("NOT JCOMPONENT");
            if (pName.contains(ProcessorTree.PROC_NAME_ACQ_ENG))
                setIcon(acqEngIcon);
            if (pName.contains(ProcessorTree.PROC_NAME_IMAGE_STORAGE))
                setIcon(imageStorageIcon);
            if (dp instanceof ImageTagFilter) {
                String tagName=((ImageTagFilter)dp).getKey();
                if (tagName.toLowerCase().contains("channel"))
                    setIcon(fChannelIcon);
                else if (tagName.toLowerCase().contains("area"))
                    setIcon(fAreaIcon);            
                else if (tagName.toLowerCase().contains("slice"))
                    setIcon(fZPosIcon);
                else if (tagName.toLowerCase().contains("frame"))
                    setIcon(fFrameIcon);
                else
                    setIcon(fTagIcon);
            }
            if (dp instanceof ScriptAnalyzer)// || dp instanceof MC_ScriptAnalyzer)
                setIcon(scriptIcon);
            if (dp instanceof MultiChMultiZAnalyzer)
                setIcon(aMC_MZ_Icon);
            if (dp instanceof RoiFinder)
                setIcon(roiIcon);
        } else {
            setText(dp.getClass().getSimpleName());
        }    
        return cell;
    }
    
    public boolean isCellEditable(EventObject evt) {
/*    if (super.isCellEditable(evt) && 
        lastValue instanceof DefaultMutableTreeNode) { 
      Object userObject =
        ((DefaultMutableTreeNode)lastValue).
                     getUserObject();
       for (int i = 0; i < values.length; i++) {
        if (userObject.equals(values[i])) {
          return true;
        }
      }
    }
    return false;*/
        return true;
  } 
    
}
