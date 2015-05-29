package autoimage.dataprocessors;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Basic extension of FilterProcessor class that allows user to select filter 
 * values from a predefined list.
 * In the configuration GUI, filter value options are displayed as an array of 
 * checkboxes.
 * 
 * @author Karsten Siller
 * @param <E> element type in BlockingQueue (e.g. TaggedImage or File)
 * @param <T> type of option value, e.g. String for channel names, Long for slice index
 */
public abstract class ImageTagFilterOpt<E,T> extends FilterProcessor<E,T> implements IDataProcessorOption<String> {

    List<String> options_;
        
    public ImageTagFilterOpt() {
        this("",null);
    }
    
    public ImageTagFilterOpt(String key, List<T> values) {
        super(key,values);
        options_=new ArrayList<String>();
    }
    
    @Override
    public void setOptions(List<String> options) {
        options_=options;
    }
    
    @Override
    public List<String> getOptions() {
        return options_;
    }
    
    @Override
    public void makeConfigurationGUI() {
        JPanel guiPanel = new JPanel();
        BoxLayout layout = new BoxLayout(guiPanel,BoxLayout.Y_AXIS);

        guiPanel.setLayout(layout); 
        guiPanel.setSize(300, 300);

        JLabel label = new JLabel(key_);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        guiPanel.add(label);

        //create a JPanel with an array of checkboxes representing selectable 
        //options, and place checkbox array in JScrollPane
        float n=options_.size();
        int columns=(int)Math.floor(Math.sqrt(n)/1.5);
        if (columns<1)
            columns=1;
        int rows=(int)Math.ceil(n/columns);
        JPanel cbPanel = new JPanel();
        cbPanel.setLayout(new GridLayout(rows,columns));
        
        JCheckBox[] optCB = new JCheckBox[(int)options_.size()];
        int i=0;
        for (String opt:options_) {
            optCB[i] = new JCheckBox(opt);
            if (values_!=null)
                optCB[i].setSelected(values_.contains(valueOf(opt)));
            cbPanel.add(optCB[i]);
            i++;
        }      

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.getViewport().add(cbPanel);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        guiPanel.add(scrollPane,BorderLayout.WEST);

        JOptionPane pane = new JOptionPane(guiPanel,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
        JDialog dlg=pane.createDialog(null, "Filter: "+key_);
        dlg.setSize(new Dimension(500,300));
        dlg.setVisible(true);
        if ((Integer)pane.getValue() == JOptionPane.OK_OPTION) {
            values_.clear();
            for (i=0; i<optCB.length; i++) {
                if (optCB[i].isSelected()) {
                    values_.add(valueOf(optCB[i].getText()));
                }    
            }
        }
    }
}
