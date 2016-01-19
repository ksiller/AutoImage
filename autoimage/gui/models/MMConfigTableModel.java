package autoimage.gui.models;

import autoimage.data.acquisition.AcqSetting;
import autoimage.data.acquisition.MMConfig;
import autoimage.events.config.MMConfigAddedEvent;
import autoimage.events.config.MMConfigChGroupChangedEvent;
import autoimage.events.config.MMConfigDeletedEvent;
import autoimage.events.config.MMConfigListChangedEvent;
import autoimage.events.config.MMConfigUpdatedEvent;
import com.google.common.eventbus.Subscribe;
import ij.IJ;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.micromanager.conf2.ConfigPreset;

/**
 *
 * @author Karsten Siller
 */
public class MMConfigTableModel extends AbstractTableModel {
    public final String[] COLUMN_NAMES = new String[]{"Use","Channel", "Name", "Preset"};
    private AcqSetting acqSetting;
    private List<MMConfig> allConfigs;

    public static MMConfigTableModel createInstance(final List<MMConfig> configs, final AcqSetting setting) {
        MMConfigTableModel model=new MMConfigTableModel(configs, setting);
        //to receive notification from layout when landmarks change
        setting.registerForEvents(model);
        return model;
    }
    
    private MMConfigTableModel(List<MMConfig> configs, AcqSetting setting) {
        super();
        if (setting==null) {
            throw new IllegalArgumentException("Layout object cannot be null");
        }
        acqSetting=setting;
        allConfigs=configs;
        mmConfigListChanged(new MMConfigListChangedEvent(setting,setting.getUsedMMConfigs()));
    }

    public List<MMConfig> getMMConfigs() {
        return allConfigs;
    }

    @Override
    public int getRowCount() {
        return allConfigs.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return COLUMN_NAMES[columnIndex];
    }

    @Override
    public Class getColumnClass(int colIndex) {
        return getValueAt(0, colIndex).getClass();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int colIndex) {
        return (colIndex != 2);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int colIndex) {
        if (allConfigs != null & rowIndex < allConfigs.size()) {
            MMConfig config=allConfigs.get(rowIndex);
            switch (colIndex) {
                case 0: {
                    if ((Boolean)value) {
                        //selected, add to used config list
                        config = config
                                .copy()
                                .use(true)
                                .build();
                        acqSetting.putMMConfig(config);
                    } else {
                        //deselected, remove form used confog list
                        acqSetting.deleteMMConfig(config.getName());
                    }
                    return;
                }
                case 1: {
                    config = config
                        .copy()
                        .controlChannel((Boolean) value)
                        .build();
                    acqSetting.putMMConfig(config);
                    break;
                }
                case 2: {
                    config = config
                        .copy()
                        .name((String) value)
                        .build();
                    acqSetting.putMMConfig(config);
                    break;
                }
                case 3: {
                    config = config
                        .copy()
                        .selectPreset((String) value)
                        .build();
                    acqSetting.putMMConfig(config);
                    break;
                }
            }
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int colIndex) {
        MMConfig config;
        if (allConfigs != null && rowIndex < allConfigs.size()) {
            config = allConfigs.get(rowIndex);
            switch (colIndex) {
                case 0: {
                    return config.isUsed();
                }
                case 1: {
                    return config.isChannel();
                }
                case 2: {
                    return config.getName() + (!config.isValid() ? " !" : "");
                }
                case 3: {
                    return !config.isChannel() ? config.getSelectedPreset() : "see Channel Configuration";
                }
            } 
            return null;
        } else {
            return null;
        }
    }

    public MMConfig getRowData(int rowIdx) {
        if (rowIdx >= 0 && rowIdx < allConfigs.size()) {
            return allConfigs.get(rowIdx);
        } else {
            return null;
        }
    }

    public void setAcqLayout(AcqSetting setting) {
        if (setting==null) {
            throw new IllegalArgumentException("Layout object cannot be null");
        }
        acqSetting=setting;
        setting.registerForEvents(this);
        allConfigs=setting.getUsedMMConfigs();
        fireTableDataChanged();
    }
    
    @Subscribe
    public void addedMMConfig(MMConfigAddedEvent e) {
        if (e.getAcqSetting()==acqSetting) {
            for (int index=0; index<allConfigs.size(); index++) {
                if (e.getMMConfig().getName().equals(allConfigs.get(index).getName())) {
                    allConfigs.set(index, e.getMMConfig());
                    fireTableRowsUpdated(index, index);
                    return;
                }   
            }
            allConfigs.add(e.getMMConfig());
            fireTableRowsInserted(allConfigs.size()-1, allConfigs.size()-1);
        }
    }

    @Subscribe
    public void deletedMMConfig(MMConfigDeletedEvent e) {
        if (e.getAcqSetting()==acqSetting) {
            for (int index=0; index<allConfigs.size(); index++) {
                if (e.getMMConfig().getName().equals(allConfigs.get(index).getName())) {
                    MMConfig unusedConfig=allConfigs.get(index)
                           .copy()
                           .use(false)
                           .build();
                    allConfigs.set(index, unusedConfig);
                    fireTableRowsUpdated(index, index);
                }    
            }
        }
    }

    @Subscribe
    public void updatedMMConfig(MMConfigUpdatedEvent e) {
        if (e.getAcqSetting()==acqSetting) {
            for (int index=0; index<allConfigs.size(); index++) {
                if (e.getMMConfig().getName().equals(allConfigs.get(index).getName())) {
                    allConfigs.set(index, e.getMMConfig());
                    fireTableRowsUpdated(index, index);
                    return;
                }    
            }
            allConfigs.add(e.getMMConfig());
            fireTableRowsInserted(allConfigs.size()-1, allConfigs.size()-1);
        }
    }

    @Subscribe
    public void mmConfigListChanged(MMConfigListChangedEvent e) {
        if (e.getAcqSetting()==acqSetting) {
            for (MMConfig usedConfig:e.getMMConfigList()) {
                boolean found=false;
                for (MMConfig allConfig:allConfigs) {
                    if (usedConfig.getName().equals(allConfig.getName())) {
                        //found it, replace
                        allConfig=usedConfig;
                        found=true;
                        break;
                    }
                }
                if (!found) {
                    MMConfig invalidConfig=usedConfig.copy()
                            .validate(false)
                            .build();
                    allConfigs.add(invalidConfig);
                    acqSetting.updateUsedMMConfig(invalidConfig);
                }
            }
            fireTableDataChanged();
        }
    }
   
    @Subscribe 
    public void channelGroupChanged(MMConfigChGroupChangedEvent e) {
        if (e.getAcqSetting()==acqSetting) {
            for (int index=0; index<allConfigs.size(); index++) {
                MMConfig config=allConfigs.get(index);
                if (e.getPreviousChannelGroup().equals(config.getName())
                        && config.isChannel()) {
                    //need to deselect "isChannel" in previous group
                    config = config.copy()
                            .controlChannel(false)
                            .build();
                    if (acqSetting.updateUsedMMConfig(config)==null) {
                        //this may occur when channel is not used in acqSetting
                        //and therefore cannot be found and updated
                        //--> do it explicitely here
                        allConfigs.set(index, config);
                        fireTableRowsUpdated(index, index);
                    }
                } 
                if (e.getCurrentChannelGroup().equals(config.getName())
                        && !config.isChannel()) {
                    fireTableRowsUpdated(index, index);
                }
            }
            IJ.log("ChannelGroup="+acqSetting.getChannelGroupStr());
        }
    }
    
    public void addMMConfig(MMConfig lm) {
        acqSetting.putMMConfig(lm);
    }

    public MMConfig deleteMMConfig(String name) {
        return acqSetting.deleteMMConfig(name);
    }

    public MMConfig updateMMConfig(MMConfig config) {
        return acqSetting.updateUsedMMConfig(config);
    }

    public void setMMConfigs(List<MMConfig> list) {
        acqSetting.setUsedMMConfigs(list);
    }
    
}
