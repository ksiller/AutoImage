package autoimage.gui.views;

import autoimage.data.acquisition.AcqSetting;
import autoimage.data.acquisition.MMConfig;
import autoimage.events.acqsetting.AcqSettingSelectionChangedEvent;
import autoimage.gui.models.MMConfigTableModel;
import autoimage.utils.MMCoreUtils;
import com.google.common.eventbus.Subscribe;
import ij.IJ;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import mmcorej.CMMCore;
import org.micromanager.api.events.PropertiesChangedEvent;
import org.micromanager.api.events.SystemConfigurationLoadedEvent;

/**
 *
 * @author Karsten Siller
 */
public class ConfigWindow extends JDialog {
    
    private AcqSetting acqSetting;
    private final JTable configTable;
    private final CMMCore core;
    
    private class MMConfigGroupRenderer extends DefaultTableCellRenderer {
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,boolean isSelected, boolean hasFocus, int row, int column) {
            MMConfig config=((MMConfigTableModel)table.getModel()).getRowData(row);
            String valueText=config.isValid() ? (String)value : "! "+(String)value;
            Component component=super.getTableCellRendererComponent(table, valueText, isSelected, hasFocus, row, column);
            component.setForeground(config.isValid() ? Color.black :Color.red);
            return component;
        }
    }
    
    private class MMConfigPresetRenderer extends DefaultTableCellRenderer {
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,boolean isSelected, boolean hasFocus, int row, int column) {
            MMConfig config=((MMConfigTableModel)table.getModel()).getRowData(row);
            String valueText=config.isChannel() ? "See Channel Config" : (String)value;
            Component component=super.getTableCellRendererComponent(table, valueText, isSelected, hasFocus, row, column);
            component.setForeground(config.isValid() ? Color.black : Color.red);
//            component.setBackground(table.getBackground());
            return component;    
        }
    }
    
    public ConfigWindow (Frame p, CMMCore core, AcqSetting setting, List<MMConfig> allConfigs) {
        super(p);
        this.core=core;
        acqSetting=setting;
        setTitle("Configurations");
        setMinimumSize(new Dimension(500,250));
        SpringLayout springLayout = new SpringLayout();
        getContentPane().setLayout(springLayout);
        
        //job table inside JScrollPane
        configTable = new JTable(MMConfigTableModel.createInstance(allConfigs,setting));
        configTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configTable.getColumnModel().getColumn(2).setCellRenderer(new MMConfigGroupRenderer());
        configTable.getColumnModel().getColumn(3).setCellRenderer(new MMConfigPresetRenderer());
        configTable.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(new JComboBox()) {
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                MMConfig config = ((MMConfigTableModel)table.getModel()).getRowData(row);
                ((JComboBox)this.editorComponent).setModel(new DefaultComboBoxModel(config.getAvailablePresets().toArray()));
                ((JComboBox)this.editorComponent).setSelectedItem(config.getSelectedPreset());
                return super.getTableCellEditorComponent(table, value, isSelected, row, column); 
            }
        });
        configTable.getModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                MMConfigTableModel model=(MMConfigTableModel)configTable.getModel();
                setTitle("Configurations: "+model.getAcqSetting().getName()+", Channel Group "+model.getAcqSetting().getChannelGroupStr());
            }
        });
        JScrollPane scrollPane=new JScrollPane(configTable);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        springLayout.putConstraint(SpringLayout.NORTH, scrollPane, 10, SpringLayout.NORTH, getContentPane());     
        springLayout.putConstraint(SpringLayout.SOUTH, scrollPane, -10, SpringLayout.SOUTH, getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, scrollPane, -10, SpringLayout.EAST, getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, scrollPane, +10, SpringLayout.WEST, getContentPane());
                 
        getContentPane().add(scrollPane);
        pack();
    }    
    
    @Subscribe
    public void mmSystemConfigurationChanged(SystemConfigurationLoadedEvent e) {
        IJ.log("New system configuration loaded");
        MMConfigTableModel model=(MMConfigTableModel)configTable.getModel();
        model.setAvailableMMConfigs(MMCoreUtils.getAvailableMMConfigs(core));
    }
    
    @Subscribe
    public void mmPropertiesChanged(PropertiesChangedEvent e) {
        IJ.log("Properties changed");
    }
    
    @Subscribe
    public void acqSettingSelectionChanged(AcqSettingSelectionChangedEvent e) {
        MMConfigTableModel model=(MMConfigTableModel)configTable.getModel();
        model.setAcqSetting(e.getCurrentAcqSetting());
    }
    
}
