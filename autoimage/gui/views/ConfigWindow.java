package autoimage.gui.views;

import autoimage.data.acquisition.AcqSetting;
import autoimage.data.acquisition.MMConfig;
import autoimage.gui.models.MMConfigTableModel;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;

/**
 *
 * @author Karsten Siller
 */
public class ConfigWindow extends JDialog {
    
    private AcqSetting acqSetting;
    private final JTable configTable;
    
    
    
    public ConfigWindow (Frame p, AcqSetting setting, List<MMConfig> allConfigs) {
        super(p);
        acqSetting=setting;
        setTitle("Configurations");
        setMinimumSize(new Dimension(500,250));
        SpringLayout springLayout = new SpringLayout();
        getContentPane().setLayout(springLayout);
        
        //job table
        configTable = new JTable(MMConfigTableModel.createInstance(allConfigs,setting));
        configTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane=new JScrollPane(configTable);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        springLayout.putConstraint(SpringLayout.NORTH, scrollPane, 10, SpringLayout.NORTH, getContentPane());     
        springLayout.putConstraint(SpringLayout.SOUTH, scrollPane, -10, SpringLayout.SOUTH, getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, scrollPane, -10, SpringLayout.EAST, getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, scrollPane, +10, SpringLayout.WEST, getContentPane());
                 
        getContentPane().add(scrollPane);
        pack();
    }    
    
}
