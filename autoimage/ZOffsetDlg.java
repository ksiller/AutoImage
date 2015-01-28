/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage;

import ij.IJ;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import mmcorej.CMMCore;
import mmcorej.StrVector;
import org.micromanager.api.ScriptInterface;

/**
 *
 * @author Karsten
 */
public class ZOffsetDlg extends javax.swing.JDialog implements ILiveListener, IStageMonitorListener, TableModelListener {

    private CMMCore core;
    private Frame parent;
    private ScriptInterface gui;
    private String channelGroupStr;
    private String xyStageName;
    private String focusDeviceName;
    private Map<String,List<ChannelData>> groupMap;
//   private List<ChannelData> result;
    
    public final static double DEFAULT_EXPOSURE = 100.0;
    public final static double DEFAULT_Z_OFFSET = 0.0;
    
    public void dispose() {
        super.dispose();
        core=null;
        parent=null;
        gui=null;
        channelGroupStr=null;
        xyStageName=null;
        focusDeviceName=null;
        groupMap=null;
    }

    @Override
    public void stagePositionChanged(final Double[] stagePos) {
        SwingUtilities.invokeLater(new Runnable() {
                
            @Override
            public void run() {    
                stagePosLabel.setText(
                  (stagePos[0]!=null ? String.format("%1$,.2f",stagePos[0]) : "???") + "; "
                + (stagePos[1]!=null ? String.format("%1$,.2f",stagePos[1]) : "???") + "; "
                + (stagePos[2]!=null ? String.format("%1$,.2f",stagePos[2]) : "???")); 
            }
        });        
    }

    @Override
    public void tableChanged(TableModelEvent e) {
//        IJ.log(Integer.toString(e.getColumn())+", "+Integer.toString(e.getFirstRow())+", "+Integer.toString(e.getLastRow()));
    }

    
    protected class ChannelData {
        private boolean reference;
        private String configName;
        private double exposure;
        private double zOffset;
        private double stageZPos;
        private boolean isSet;
//        private JButton liveButton;
//        private JButton setButton;
    
        public ChannelData (boolean ref, String name, double exp, double offset, double stagePos, boolean set) {
            reference=ref;
            configName=name;
            exposure=exp;
            zOffset=offset;
            stageZPos=stagePos;
            isSet=set;
        }
        
        public ChannelData (String name, double exp, double zOffset) {
            this(false, name, exp, zOffset, 0, false);
        }
        
        public void setReference(boolean ref) {
            reference=ref;
        }
        
        public boolean isReference() {
            return reference;
        }
        
        public void setConfigName(String name) {
            configName=name;
        }
        
        public String getConfigName() {
            return configName;
        }
        
        public void setExposure(Double exp) {
            exposure=exp;
        }
        
        public double getExposure() {
            return exposure;
        }
        
        public void setZOffset(Double offset) {
            zOffset=offset;
        }
        
        public double getZOffset() {
            return zOffset;
        }
        
        public void setStageZPos(Double zpos) {
            stageZPos=zpos;
        }
        
        public double getStageZPos() {
            return stageZPos;
        }
        
        public boolean isSet() {
            return isSet;
        }
        
        public void isSet(boolean b) {
            isSet=b;
        }
    }
    
    private class ConfigTableModel extends AbstractTableModel {

        public final String[] COLUMN_NAMES = new String[]{"Reference","Configuration", "Exposure", "z-Offset", "Stage Z-Position", "View", "Focus"};
        private List<ChannelData> channelData;

        public ConfigTableModel(List<ChannelData> cl) {
            super();
            setData(cl);
        }

        private boolean hasReferenceSet() {
            boolean refSet=false;
            for (ChannelData cd:channelData) {
                if (cd.isReference()) {
                    refSet=true;
                    break;
                }
            }
            return refSet;
        }
        
        //returns 0 if no ref position is set
        private double getRefStageZPosition() {
            double refSet=0;
            for (ChannelData cd:channelData) {
                if (cd.isReference()) {
                    refSet=cd.getStageZPos();
                    break;
                }
            }
            return refSet;    
        }

        public void setData(List<ChannelData> cl) {
            if (cl == null) {
                cl = new ArrayList<ChannelData>();
            }
            this.channelData = cl;
        }

        public List<ChannelData> getData() {
            return channelData;
        }
        
        @Override
        public int getRowCount() {
            return channelData.size();
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
        public Object getValueAt(int rowIndex, int colIndex) {
            ChannelData c;
            if (channelData != null & rowIndex < channelData.size()) {
                c = channelData.get(rowIndex);
                if (colIndex == 0) {
                    return c.isReference();
                }else if (colIndex == 1) {
                    return c.getConfigName();
                } else if (colIndex == 2) {
                    return c.getExposure();
                } else if (colIndex == 3) {
//                    return (c.isSet() && referenceSet ? Double.toString(c.getZOffset()) : "?");
                    return (Double.toString(c.getZOffset()));
                } else if (colIndex == 4) {
                    return (c.isSet() ? Double.toString(c.getStageZPos()) : "?");
                } else if (colIndex == 5) {
                    return new String(gui.isLiveModeOn() ? "Stop Live" : "Live");
                } else if (colIndex == 6) {
                    return new String("Set");
                } else if (colIndex == 7) {
                    return c.isSet();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int colIndex) {
            return (colIndex == 0 || colIndex==2 || colIndex == 5 || colIndex==6);
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int colIndex) {
            ChannelData c;
            if (channelData != null & rowIndex < channelData.size()) {
                c = channelData.get(rowIndex);
                if (colIndex == 0) {
                    if ((Boolean)value) {
                        if (c.isSet()) {
                            int row=0;
                            for (ChannelData cd: channelData) {
                                cd.setReference(false);
    //                            fireTableCellUpdated(row, colIndex);
                                if (cd.isSet()) {
                                    cd.setZOffset(cd.getStageZPos()-c.getStageZPos());
     //                             fireTableCellUpdated(row, 3);
                                }
                                row++;
                            }    
                            c.setReference((Boolean) value);
     //                       fireTableCellUpdated(rowIndex, colIndex);
                        } else {
                            JOptionPane.showMessageDialog(null,"A stage Z-position has to be set first.");
                        }
                    } else {  
                        c.setReference((Boolean) value);
     //                   fireTableCellUpdated(rowIndex, colIndex);
                    }
                    this.fireTableRowsUpdated(0,getRowCount());
                } else if (colIndex == 1) {
                    c.setConfigName((String) value);
                    fireTableCellUpdated(rowIndex, colIndex);
                } else if (colIndex == 2) {
                    c.setExposure(new Double((Double) value));
                    fireTableCellUpdated(rowIndex, colIndex);
                } else if (colIndex == 3) {
                    c.setZOffset(new Double((Double) value));
                    fireTableCellUpdated(rowIndex, colIndex);
                } else if (colIndex == 4) {
                    if (hasReferenceSet()) {
                        c.setZOffset(c.getStageZPos()-getRefStageZPosition());
                        fireTableCellUpdated(rowIndex, 3);
                    }
                    c.setStageZPos((Double) value);
                    fireTableCellUpdated(rowIndex, colIndex);
                } else if (colIndex == 5) {
                    //Live mode
//                    c.setLiveButton((JButton) value);
                    fireTableCellUpdated(rowIndex, colIndex);
                } else if (colIndex == 6) {
                    //Set as reference position
                    /*int setPos=0;
                    for (ChannelData cd:channelData) {
                        if (cd.isSet()) {
                            setPos++;
                        }
                    }
                    
                    if (setPos == 0) {//first one to be set will be reference
                        c.setReference(true);
                        fireTableCellUpdated(rowIndex,0);
                    } 
                    */
                    fireTableCellUpdated(rowIndex, colIndex);
                } else if (colIndex == 7) {
                    c.isSet(new Boolean((Boolean) value));
                    fireTableCellUpdated(rowIndex, colIndex);
                } 
            }
        }

        public void addRow(Object value) {
            ChannelData c = (ChannelData) value;
            channelData.add(c);
            fireTableRowsInserted(getRowCount(), getRowCount());
        }

        public ChannelData getRowData(int rowIdx) {
            if (rowIdx >= 0 && rowIdx < channelData.size()) {
                return channelData.get(rowIdx);
            } else {
                return null;
            }
        }

        public void removeRow(Object element) {
            for (int i = 0; i < channelData.size(); i++) {
                if (((ChannelData) element).equals(channelData.get(i))) {
                    channelData.remove(i);
                    fireTableRowsDeleted(i, i);
                }
            }
        }

        public void removeRows(int[] rowIdx) {
            for (int i = rowIdx[rowIdx.length - 1]; i >= rowIdx[0]; i--) {
                channelData.remove(i);
            }
            fireTableRowsDeleted(rowIdx[0], rowIdx[rowIdx.length - 1]);
        }

        public void removeAllRows() {
            for (int i =channelData.size()-1; i>=0; i--) {
                channelData.remove(i);
            }
            fireTableRowsDeleted(0,channelData.size()-1);
        }
    /*    
        public int rowDown(int rowIdx) {
            if (rowIdx >= 0 && rowIdx < channels.size() - 1) {
                Channel channel = channels.get(rowIdx);
                channels.remove(rowIdx);
                channels.add(rowIdx + 1, channel);
                return rowIdx + 1;
            }
            return rowIdx;
        }

        public int rowUp(int rowIdx) {
            if (rowIdx >= 1 && rowIdx < channels.size()) {
                Channel channel = channels.get(rowIdx);
                channels.remove(rowIdx);
                channels.add(rowIdx - 1, channel);
                return rowIdx - 1;
            }
            return rowIdx;
        }*/
    }
    // end ConfigTableModel


    public class ButtonRenderer extends JButton implements TableCellRenderer {

        public ButtonRenderer() {
          setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                         boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
              setForeground(table.getForeground());
              setBackground(table.getSelectionBackground());
            } else{
              setForeground(table.getForeground());
              setBackground(UIManager.getColor("Button.background"));
            }
            setText( (value ==null) ? "" : value.toString() );
            return this;
        }
    }
    
    public class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String    label;
        private boolean   isPushed;

        public ButtonEditor(JCheckBox checkBox) {
          super(checkBox);
          button = new JButton();
          button.setOpaque(true);
          button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              fireEditingStopped();
            }
          });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                         boolean isSelected, int row, int column) {
          if (isSelected) {
            button.setForeground(table.getSelectionForeground());
            button.setBackground(table.getSelectionBackground());
          } else{
            button.setForeground(table.getForeground());
            button.setBackground(table.getBackground());
          }
          label = (value ==null) ? "" : value.toString();
          button.setText( label );
          isPushed = true;
          return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed)  {
                channelTable.getCellEditor().stopCellEditing();
                ConfigTableModel model=(ConfigTableModel)channelTable.getModel();
                if (channelTable.getSelectedColumn() == 5) {
                    if (!gui.isLiveModeOn()) {
                        ChannelData cd=model.getRowData(channelTable.getSelectedRow());
                        try {
                            core.setExposure(cd.getExposure());
                            //                            core.setConfig(channelGroupStr, c.getName());
                            core.setConfig((String)groupComboBox.getSelectedItem(), cd.getConfigName());
                            //gui.setChannelExposureTime(channelGroupStr, c.getName(), c.getExposure());
                            gui.refreshGUI();
                            //gui.setConfigChanged(true);
                        } catch (Exception e) {
                        }
                        gui.enableLiveMode(true);
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                gui.getSnapLiveWin().toFront();
//                                gui.getSnapLiveWin().repaint();
                            }
                        });
                    } else {
                        gui.enableLiveMode(false);
                    }
                } else if (channelTable.getSelectedColumn() == 6) {
                    try {
                        double zPos=core.getPosition(focusDeviceName);
                        int row=channelTable.getSelectedRow();
                        ChannelData cd=model.getRowData(row);
                        cd.setStageZPos(zPos);
                        cd.isSet(true);
/*                        if (!model.hasReferenceSet()) {
                            //set isReference --> indirectly sets new zOffset
                            model.setValueAt(true,row,0);
                        } else {
                            //set new zOffset*/
/*                        }*/
                        if (model.hasReferenceSet()) {
                            if (cd.isReference()) {
                                //this is the reference channel, 
                                //so all channels with a set stage position need to be updated
                                row=0;
                                for (ChannelData c:model.getData()) {
                                    if (c.isSet()) {
                                        model.setValueAt(c.getStageZPos()-cd.getStageZPos(), row,3);
                                    }
                                    row++;
                                }
                            } else {
                                //set zOffset=0 for this channel nly
                                model.setValueAt(cd.getStageZPos()-model.getRefStageZPosition(), row,3); 
                            }
                        }    
                    } catch (Exception ex) {
                        Logger.getLogger(ZOffsetDlg.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    gui.enableLiveMode(false);
                }
            }
            isPushed = false;
            return new String( label ) ;
        }

        @Override
        public boolean stopCellEditing() {
          isPushed = false;
          return super.stopCellEditing();
        }

        @Override
        protected void fireEditingStopped() {
          super.fireEditingStopped();
        }
        
    }
    
    public ZOffsetDlg(java.awt.Frame parent, final ScriptInterface gui, String chGroupStr, boolean modal) {
        super(parent, modal);
        initComponents();
        channelTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        channelTable.getTableHeader().setReorderingAllowed(false);
        this.parent=parent;
        this.gui=gui;
        core=gui.getMMCore();
        //load available config groups
        channelGroupStr="";
        
        try {
            xyStageName=core.getXYStageDevice();
            focusDeviceName=core.getFocusDevice();
            double stageX=core.getXPosition(xyStageName);
            double stageY=core.getYPosition(xyStageName);
            double stageZ=core.getPosition(focusDeviceName);
            stagePositionChanged(new Double[]{stageX, stageY, stageZ});     
        } catch (Exception e) {
            
        }  
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
//                IJ.log("zOffsetDlg.windowClosing");
                AbstractCellEditor ace = (AbstractCellEditor) channelTable.getCellEditor();
                if (ace != null) {
                    ace.stopCellEditing();
                }
                int result=JOptionPane.showConfirmDialog(null, "Do you want to discard the z-offset settings?", "Z-Offset Settings", JOptionPane.YES_NO_OPTION);
                if (result==JOptionPane.YES_OPTION) {
//                    setVisible(false);
                    dispose();                
                }
//                result=null;
/*                if (oldRoi!=null && core!=null) {
                    try {
                        core.setROI(oldRoi.x, oldRoi.y, oldRoi.width, oldRoi.height);
                    } catch (Exception ex) {
                        Logger.getLogger(CameraRotDlg.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }    */            
            }

            @Override
            public void windowActivated(WindowEvent e) {
/*                try {
                    oldRoi=core.getROI();
                } catch (Exception ex) {
                    Logger.getLogger(CameraRotDlg.class.getName()).log(Level.SEVERE, null, ex);
                }*/
                liveModeChanged(gui.isLiveModeOn());
            }
        });
        ConfigTableModel model=new ConfigTableModel(null);
/*        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
//                IJ.log("tableChanged");
                ConfigTableModel model=(ConfigTableModel)channelTable.getModel();
                if (e.getType() == TableModelEvent.UPDATE && (e.getColumn() == 0)) {
                    ChannelData cd=(ChannelData)groupMap.get(channelGroupStr);
                    IJ.showMessage("setting new reference");        
                }
            }});*/
        channelTable.setModel(model);
        updateGroupMap();
/*        StrVector configs = core.getAvailableConfigGroups();
        groupComboBox.setModel(new DefaultComboBoxModel(configs.toArray()));*/
//        setChannelConfigs(chGroupStr,new ArrayList<ChannelData>());
        groupComboBox.setSelectedItem(chGroupStr);   

    }
    
    @Override
    public void liveModeChanged(final boolean isLive) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {

                ConfigTableModel model=(ConfigTableModel)channelTable.getModel();
                for (int row=0; row<model.getRowCount(); row++) {
                    model.setValueAt((isLive ? "Stop Live" : "Live"),row,5);
                }
            }
        });    
    }
    
    public void addApplyListener(ActionListener listener) {
        applyButton.addActionListener(listener);
    }
    
    public String getGroupName() {
        return (String)groupComboBox.getSelectedItem();
    }
    
    public List<ChannelData> getConfigData() {
        return ((ConfigTableModel)channelTable.getModel()).getData();
    }
    
    public List<ChannelData> convertChannelList(List<Channel> chList) {
        List<ChannelData> cdList = new ArrayList<ChannelData>();
        for (Channel ch:chList) {
            cdList.add(new ChannelData(ch.getName(),ch.getExposure(),ch.getZOffset()));
        }
        return cdList;
    }
    
    public void selectGroup(String groupName) {
        groupComboBox.setSelectedItem(groupName);
    }
    
    //replaces ChannelData list for specfic map entry groupName
    //NOTE: does not add ChannelData list to map unless groupName is in map [call updateGroupMap() first]
    //if no config is found by core under groupName, groupName key is removed from map
    public void updateConfigData(String groupName, List<ChannelData> cdList) {
        IJ.log(getClass().getName()+".updateConfigData: "+groupName+" ....cdList.size="+cdList.size());
        if (!groupMap.containsKey(groupName)) {
            return;
        }
        StrVector configNameList=core.getAvailableConfigs(groupName);
        if (configNameList==null || configNameList.size() == 0) {
            groupMap.remove(groupName);
            return;
        }
        List<ChannelData> newcdList=new ArrayList<ChannelData>();
        for (String configName:configNameList) {
            //create default ChannelData entry
            ChannelData newcd=null;
            if (cdList!=null && cdList.size()>0) {
                boolean found=false;
                for (ChannelData cd:cdList) {
                    if (cd.getConfigName().equals(configName)) {
                        found=true;
                        newcd = cd;
                        break;
                    }
                }
                if (!found) {
                    newcd = new ChannelData(configName, 100,0);            
                }            
            }
            else {    
                newcd = new ChannelData(configName, 100,0);
            }
            newcdList.add(newcd);
        }        
        groupMap.put(groupName, newcdList);
//        IJ.log("updateConfigData "+groupName+" ...completed. cdList.size()="+newcdList.size());
    }
    /*  
        - loads available ConfigGroups from core
        - removes non-existent groups from groupMap
        - adds additional available groups to groupMap 
          with default ChannelData for each available config within group
    */
    public boolean updateGroupMap() {
        String currentGroup=(String)groupComboBox.getSelectedItem();
        List<String> keysToRemove=new ArrayList<String>();
//        IJ.log("updateGroupMap....");
        StrVector availableGroups=core.getAvailableConfigGroups();
        List<String> availableGroupList=new ArrayList<String>();
        for (String group:availableGroups) {
            availableGroupList.add(group);
        }
        boolean currentGroupRemoved=false;
        if (groupMap!=null) {
            Iterator<Map.Entry<String, List<ChannelData>>> entries = groupMap.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, List<ChannelData>> entry = entries.next();
                String mapKey = entry.getKey();
                if (!availableGroupList.contains(mapKey)) {//not found
//                    IJ.log("updateGroupMap.before remove mapKey "+mapKey);
                    //groupMap.remove(mapKey);
                    keysToRemove.add(mapKey);
//                    IJ.log("updateGroupMap.mapKey "+mapKey+" removed.");
                } else {//found
                    //remove group from list --> remaining list items need to be added to map
                    availableGroupList.remove(mapKey);
                }
            }
            for (String key:keysToRemove) {
                if (key.equals(channelGroupStr)) {
                    currentGroupRemoved=true;
                }    
                groupMap.remove(key);
            }
        } else {
            groupMap=new HashMap<String,List<ChannelData>>();
        }
        //availableGroups now contains those groups not present in groupMap;
        //these need to be added to groupMap
        for (String groupName:availableGroupList) {
            StrVector configNameList=core.getAvailableConfigs(groupName);
            List<ChannelData> cdList=new ArrayList<ChannelData>();
            for (String configName:configNameList) {
                //create default ChannelData entry
                ChannelData cd=new ChannelData(configName, 100,0);
                cdList.add(cd);
                groupMap.put(groupName, cdList);
            }        
        }
        //update groupComboBox
        groupComboBox.setModel(new DefaultComboBoxModel(availableGroups.toArray()));
        if (currentGroupRemoved)
            groupComboBox.setSelectedIndex(0);
        else {
            groupComboBox.setSelectedItem(currentGroup);
        }
//        IJ.log("updateGroupMap....completed: "+groupMap.size()+" groups");
        return (keysToRemove.size() > 0 || availableGroupList.size() > 0);
    }
    
    public void setGroupData(String groupStr,List<ChannelData> channelList, boolean reloadGroups) {
//        IJ.log("setGroupData....");
        if (groupMap==null) {
            groupMap=new HashMap<String,List<ChannelData>>();
        }
        StrVector availableGroups=core.getAvailableConfigGroups();
        boolean groupMapModified=false;
        if (reloadGroups) {
            groupMapModified=updateGroupMap();
        }
        updateConfigData(groupStr, channelList);
        groupComboBox.setModel(new DefaultComboBoxModel(groupMap.keySet().toArray()));
        if (groupMapModified) {
            groupComboBox.setSelectedIndex(0);
        }
//        IJ.log("setGroupData....completed: "+groupMap.size()+" groups");
    }

    
    private void setChannelConfigs(String group, List<ChannelData> channelList) {
        ConfigTableModel dm = (ConfigTableModel)channelTable.getModel();
//        IJ.log("setChannelConfigs... "+dm.getRowCount()+" rows");
        channelTable.setModel(new ConfigTableModel(channelList));
        
        ButtonRenderer br=new ButtonRenderer();
        ButtonEditor be=new ButtonEditor(new JCheckBox());
        channelTable.getColumnModel().getColumn(5).setCellRenderer(br);
        channelTable.getColumnModel().getColumn(5).setCellEditor(be);
        channelTable.getColumnModel().getColumn(6).setCellRenderer(br);
        channelTable.getColumnModel().getColumn(6).setCellEditor(be);
        //set right justification for z-Offset and stage position column
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
        channelTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);        
        channelTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);        


        channelTable.getModel().addTableModelListener(this);
//        IJ.log("setChannelConfigs... completed. "+dm.getRowCount()+" rows");
        
    }

    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        groupComboBox = new javax.swing.JComboBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        channelTable = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        stagePosLabel = new javax.swing.JLabel();
        refreshButton = new javax.swing.JButton();
        applyButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Z-Offset");
        setMinimumSize(new java.awt.Dimension(480, 200));

        jLabel1.setText("Channel group:");

        groupComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        groupComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                groupComboBoxActionPerformed(evt);
            }
        });

        channelTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4", "Title 5", "Title 6", "Title 7", "Title 8"
            }
        ));
        jScrollPane1.setViewportView(channelTable);

        jLabel2.setText("Stage position:");

        stagePosLabel.setText("jLabel3");

        refreshButton.setText("Refresh");
        refreshButton.setToolTipText("Reload available Configuration Groups");
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });

        applyButton.setText("Apply");
        applyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 548, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createSequentialGroup()
                                .add(jLabel1)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(groupComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 236, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(refreshButton))
                            .add(layout.createSequentialGroup()
                                .add(jLabel2)
                                .add(6, 6, 6)
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(layout.createSequentialGroup()
                                        .add(applyButton)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                        .add(cancelButton))
                                    .add(stagePosLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 233, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                        .add(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(groupComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(refreshButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(stagePosLabel))
                .add(12, 12, 12)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(applyButton)
                    .add(cancelButton))
                .add(12, 12, 12))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void groupComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_groupComboBoxActionPerformed
        if (groupComboBox.getSelectedItem() != null) {
            ConfigTableModel model=(ConfigTableModel)channelTable.getModel();
//            IJ.log("groupComboBoxActionPerformed.... "+model.getRowCount()+" rows");
            channelGroupStr=(String)groupComboBox.getSelectedItem();
            StrVector availableGroups=core.getAvailableConfigGroups();
            if (!Arrays.asList(availableGroups.toArray()).contains(channelGroupStr)) {
                JOptionPane.showMessageDialog(this, "Group "+channelGroupStr+" is not available anymore");
                updateGroupMap();
            } else {
//                IJ.log("groupComboBoxActionPerformed. before updateConfigData");
                updateConfigData(channelGroupStr,groupMap.get(channelGroupStr));
//                IJ.log("groupComboBoxActionPerformed. after updateConfigData");
                List<ChannelData> cdList=groupMap.get(channelGroupStr);
//                IJ.log("groupComboBoxActionPerformed. before setChannelConfigs. "+model.getRowCount()+" rows");
                setChannelConfigs(channelGroupStr,cdList);
//                IJ.log("groupComboBoxActionPerformed. after setChannelConfigs. "+model.getRowCount()+" rows");

            }    
//            IJ.log("groupComboBoxActionPerformed....completed. "+model.getRowCount()+" rows");
        }
    }//GEN-LAST:event_groupComboBoxActionPerformed

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed
        updateGroupMap();
    }//GEN-LAST:event_refreshButtonActionPerformed

    private void applyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_applyButtonActionPerformed
//        result=((ConfigTableModel)channelTable.getModel()).getData();
//        setVisible(false);
        AbstractCellEditor ace = (AbstractCellEditor) channelTable.getCellEditor();
        if (ace != null) {
            ace.stopCellEditing();
        }
        if (!((ConfigTableModel)channelTable.getModel()).hasReferenceSet()) {
           JOptionPane.showMessageDialog(this, "A reference channel needs to be selected.");
           return;
        }
        dispose();
    }//GEN-LAST:event_applyButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
//        result=null;
//        setVisible(false);
        AbstractCellEditor ace = (AbstractCellEditor) channelTable.getCellEditor();
        if (ace != null) {
            ace.stopCellEditing();
        }
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton applyButton;
    private javax.swing.JButton cancelButton;
    private javax.swing.JTable channelTable;
    private javax.swing.JComboBox groupComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton refreshButton;
    private javax.swing.JLabel stagePosLabel;
    // End of variables declaration//GEN-END:variables
}
