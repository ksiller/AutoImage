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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Karsten Siller
 */
public class MMConfigTableModel extends AbstractTableModel {
    public final String[] COLUMN_NAMES = new String[]{"Use","Is Channel", "Group", "Preset"};
    private volatile AcqSetting acqSetting;
    private final List<MMConfig> allConfigs = Collections.synchronizedList(new ArrayList<MMConfig>());
    private final Map<String, MMConfig> usedConfigs = new ConcurrentHashMap<String,MMConfig>();

    public static MMConfigTableModel createInstance(final List<MMConfig> configs, final AcqSetting setting) {
        MMConfigTableModel model=new MMConfigTableModel(configs, setting);
        //to receive notification from layout when landmarks change
        if (setting!=null) {
            setting.registerForEvents(model);
        }
        if (setting!=null) {
            model.mmUsedConfigListChanged(new MMConfigListChangedEvent(setting,setting.getUsedMMConfigs()));
        }
        return model;
    }
    
    private MMConfigTableModel(List<MMConfig> configs, AcqSetting setting) {
        super();
        acqSetting=setting;
        if (configs!=null) {
            for (MMConfig config:configs) {
                allConfigs.add(config);
            }
        }
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
                        if (acqSetting!=null) {
                            acqSetting.putMMConfig(config);
                        }
                    } else {
                        if (acqSetting!=null) {
                            //deselected, remove form used confog list
                            acqSetting.deleteMMConfig(config.getName());
                        }
                    }
                    return;
                }
                case 1: {
                    config = config
                        .copy()
                        .controlChannel((Boolean) value)
                        .build();
                    if (acqSetting!=null) {
                        acqSetting.putMMConfig(config);
                    }
                    break;
                }
                case 3: {
                    config = config
                        .copy()
                        .selectPreset((String) value)
                        .build();
                    if (acqSetting!=null) {
                        IJ.log("SET PRESET: "+acqSetting.getName()+", "+config.getName()+", "+config.getSelectedPreset());
                        acqSetting.putMMConfig(config);
                    } else {
                        IJ.log("SET PRESET: NULL, "+config.getName()+", "+config.getSelectedPreset());
                    }
                    break;
                }
            }
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int colIndex) {
        MMConfig config=null;
        synchronized (allConfigs) {
            if (rowIndex >=0 && rowIndex < allConfigs.size()) {
                config = allConfigs.get(rowIndex);
            }
        }
        if (config!=null) {
            switch (colIndex) {
                case 0: {
                    return usedConfigs.containsKey(config.getName());
                }
                case 1: {
                    return config.isChannel();
                }
                case 2: {
                    return config.getName();
                }
                case 3: {
                    return config.getSelectedPreset();
                }
            } 
            return null;
        } else {
            return null;
        }
    }

    public synchronized MMConfig getRowData(int rowIdx) {
        if (rowIdx >= 0 && rowIdx < allConfigs.size()) {
            return allConfigs.get(rowIdx);
        } else {
            return null;
        }
    }

    public void setAvailableMMConfigs(List<MMConfig> configs) {
        if (configs==null) {
            return;
        }
        allConfigs.clear();
        synchronized (allConfigs) {
            for (MMConfig config:configs) {
                allConfigs.add(config);
            }
        }
        if (acqSetting!=null) {
            mmUsedConfigListChanged(new MMConfigListChangedEvent(acqSetting,acqSetting.getUsedMMConfigs()));
        }
    }

    public void setAcqSetting(AcqSetting setting) {
        if (setting==null) {
            throw new IllegalArgumentException("AcqSetting object cannot be null");
        }
        acqSetting=setting;
        setting.registerForEvents(this);
        mmUsedConfigListChanged(new MMConfigListChangedEvent(setting,setting.getUsedMMConfigs()));
    }
    
    public AcqSetting getAcqSetting() {
        return acqSetting;
    }
    
    @Subscribe
    public void addedUsedMMConfig(final MMConfigAddedEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() { 
                IJ.log("ADDED USED MMCONFIG: "+e.getMMConfig().getName()+", "+e.getMMConfig().getSelectedPreset());
                if (e.getAcqSetting()==acqSetting) {
                    int size=0;
                    int index;
                    boolean update=false;
                    boolean insert=false;
                    synchronized (allConfigs) {
                        for (index=0; index<allConfigs.size(); index++) {
                            if (e.getMMConfig().getName().equals(allConfigs.get(index).getName())) {
                                allConfigs.set(index, e.getMMConfig());
                                update=true;
                                break;
                            }   
                        }
                        if (!update) {
                            allConfigs.add(e.getMMConfig());
                            size=allConfigs.size();
                        }
                    }
                    usedConfigs.put(e.getMMConfig().getName(), e.getMMConfig());
                    if (update) {
                        fireTableRowsUpdated(index, index);
                    } else {
                        fireTableRowsInserted(size-1, size-1);
                    }
                }
            };    
        });
    }

    @Subscribe
    public void deletedUsedMMConfig(final MMConfigDeletedEvent e) {
        if (e.getAcqSetting()==acqSetting) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {            
                    IJ.log("DELETED USED MMCONFIG: "+e.getMMConfig().getName()+", "+e.getMMConfig().getSelectedPreset());
                    boolean update=false;
                    boolean delete=false;
                    int index;
                    synchronized (allConfigs) {
                        for (index=0; index<allConfigs.size(); index++) {
                            MMConfig allConfig=allConfigs.get(index);
                            if (e.getMMConfig().getName().equals(allConfig.getName())) {
                                //disable "isChannel"
                                allConfigs.set(index, allConfig.copy().controlChannel(false).build());
                                //remove from usedConfigs map
                                usedConfigs.remove(e.getMMConfig().getName());
                                if (!e.getMMConfig().isValid()) {
                                    //if an invalid MMconfig was removed from used list, then remove it from available allConfigs as well
                                    allConfigs.remove(index);
                                    delete=true;
                                } else {
                                    update=true;
                                }
                                break;
                            }    
                        }
                    }
                    if (delete) {
                        fireTableRowsDeleted(index,index);               
                    } else if (update) {
                        fireTableRowsUpdated(index, index);
                    }
                }
            });    
        }
    }

    @Subscribe
    public void updatedUsedMMConfig(final MMConfigUpdatedEvent e) {
        if (e.getAcqSetting()==acqSetting) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {            
                IJ.log("UPDATED USED MMCONFIG: "+e.getMMConfig().getName()+", "+e.getMMConfig().getSelectedPreset());
                    synchronized (this) {
                        for (int index=0; index<allConfigs.size(); index++) {
                            if (e.getMMConfig().getName().equals(allConfigs.get(index).getName())) {
                                allConfigs.set(index, e.getMMConfig());
                                usedConfigs.put(e.getMMConfig().getName(), e.getMMConfig());
                                fireTableRowsUpdated(index, index);
                                return;
                            }    
                        }
                    }
                    allConfigs.add(e.getMMConfig());
                    usedConfigs.put(e.getMMConfig().getName(), e.getMMConfig());
                    fireTableRowsInserted(allConfigs.size()-1, allConfigs.size()-1);
                }
            });
        }
    }

    @Subscribe
    public void mmUsedConfigListChanged(final MMConfigListChangedEvent e) {
        if (e.getAcqSetting()==null) {
        IJ.log("MMConfigTableModel.mmConfigListChanged: e.getAcqSetting()=NULL");
            return;
        }
        IJ.log("MMConfigTableModel.mmConfigListChanged: e.getAcqSetting()="+e.getAcqSetting().getName()+", acqSetting="+(acqSetting==null ? "NULL" : acqSetting.getName()));
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {        
                if (e.getAcqSetting()==acqSetting) {
                    List<MMConfig> invalidConfigs=new ArrayList<MMConfig>();
                    synchronized (allConfigs) {
                        //remove all invalid MMConfigs and disable "isChannel"
                        for (int index=allConfigs.size()-1; index>=0; index--) {
                            MMConfig allConfig=allConfigs.get(index);
                            if (!allConfig.isValid()) {
                                allConfigs.remove(allConfig);
                            } else {
                                allConfigs.set(index, allConfig.copy().controlChannel(false).build());
                            }
                        }
                        //clear usedConfigs map
                        usedConfigs.clear();
                        //repopulate allConfigs list and usedConfigs map
                        for (MMConfig usedConfig:e.getMMConfigList()) {
                            boolean foundUsedConfig=false;
                            for (int index=0; index<allConfigs.size(); index++) {
                                MMConfig allConfig=allConfigs.get(index);
                                if (allConfig.getName().equals(usedConfig.getName())) {
                                    //found it, replace
                                    //no defensive copy required since MMConfig objects are immutable
                                    allConfigs.set(index,usedConfig);
                                    foundUsedConfig=true;
                                    break;
                                } else {
                                    //not foundUsedConfig
                                }
                            }
                            if (!foundUsedConfig) {
                                //usedConfig not in list of available configs
                                //mark config as invalid and add to allConfigs 
                                MMConfig invalidConfig=usedConfig.copy()
                                        .validate(false)
                                        .build();
        //                        allConfigs.add(invalidConfig);
                                invalidConfigs.add(invalidConfig);
                            }
                            usedConfigs.put(usedConfig.getName(), usedConfig);
                        }
                    }
                    for (MMConfig invalid:invalidConfigs) {
                        acqSetting.updateUsedMMConfig(invalid);
                    }
                    fireTableDataChanged();
                }
            }
        });
    }
   
    @Subscribe 
    public void channelGroupChanged(final MMConfigChGroupChangedEvent e) {
        if (e.getAcqSetting()==null) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (e.getAcqSetting()==acqSetting) {
                    List<Integer> rowsToUpdate = new ArrayList<Integer>();
                    synchronized (allConfigs) {
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
                                    rowsToUpdate.add(index);
                                }
                            } 
                            if (e.getCurrentChannelGroup().equals(config.getName())
                                    && !config.isChannel()) {
                                rowsToUpdate.add(index);
                            }
                        }
                    }
                    for (int row:rowsToUpdate) {
                        fireTableRowsUpdated(row, row);
                    }            
                    IJ.log("ChannelGroup="+acqSetting.getChannelGroupStr());
                }
            }
        });
    }
    
}
