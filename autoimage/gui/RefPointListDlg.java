package autoimage.gui;

import autoimage.IRefPointListener;
import autoimage.api.IStageMonitorListener;
import autoimage.api.RefArea;
import autoimage.Vec3d;
import autoimage.api.IAcqLayout;
import autoimage.api.SampleArea;
//import ij.IJ;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import mmcorej.CMMCore;
import org.micromanager.api.ScriptInterface;

/**
 *
 * @author Karsten Siller
 */
public class RefPointListDlg extends javax.swing.JDialog implements IStageMonitorListener {

    private CMMCore core;
    private IAcqLayout acqLayout;
    private List<RefArea> rpList;
    private List<RefArea> rpBackup;
    private String xyStageName;
    private String zStageName;
    private boolean modified;
    private List<IRefPointListener> listeners;
       
    private final static double TOLERANCE_SCALE_FACTOR = 0.05;
    
    private class RefAreaTableModel extends AbstractTableModel {
        public final String[] COLUMN_NAMES = new String[]{"Name", "Stage X", "Stage Y", "Stage Z", "Layout X", "Layout Y", "Layout Z", "Status"};
        private List<RefArea> refAreas;

        public RefAreaTableModel(List<RefArea> al) {
            super();
            if (al == null) {
                al = new ArrayList<RefArea>();
            }
            this.refAreas = al;
        }

        public List<RefArea> getAreaList() {
            return refAreas;
        }

        @Override
        public int getRowCount() {
            return refAreas.size();
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
            RefArea a;
            if (refAreas != null & rowIndex < refAreas.size()) {
                a = refAreas.get(rowIndex);
                switch (colIndex) {
                    case 0: {
                        return a.getName();
                    }
                    case 1: {
                        return a.getStageCoordX();
                    }
                    case 2: {
                        return a.getStageCoordY();
                    }
                    case 3: {
                        return a.getStageCoordZ();
                    }
                    case 4: {
                        return a.getLayoutCoordX();
                    }
                    case 5: {
                        return a.getLayoutCoordY();
                    }
                    case 6: {
                        return a.getLayoutCoordZ();
                    }
                    case 7: {
                        return (a.isStagePosFound() ? "mapped" : "not mapped");
                    }
                } 
                return null;
            } else {
                return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int colIndex) {
            return (colIndex == 0 || colIndex == 4 || colIndex == 5 || colIndex == 6);
        }

        public void setStagePos(int row, double x, double y, double z) {
            if (row < getRowCount()) {
                refAreas.get(row).setStageCoord(x, y, z);
                refAreas.get(row).setStagePosMapped(true);
                fireTableRowsUpdated(row,row);
            }
        }
        
        @Override
        public void setValueAt(Object value, int rowIndex, int colIndex) {
            RefArea area;
            if (refAreas != null & rowIndex < refAreas.size()) {
                area = refAreas.get(rowIndex);
                switch (colIndex) {
                    case 0: {
                        area.setName((String) value);
                        fireTableCellUpdated(rowIndex, colIndex);
                        break;
                    }
                    case 1: {
                        area.setStageCoordX((Double) value);
                        fireTableCellUpdated(rowIndex, colIndex);
                        break;
                    }
                    case 2: {
                        area.setStageCoordY((Double) value);
                        fireTableCellUpdated(rowIndex, colIndex);
                        break;
                    }
                    case 3: {
                        area.setStageCoordZ((Double) value);
                        fireTableCellUpdated(rowIndex, colIndex);
                        break;
                    }
                    case 4: {
                        area.setLayoutCoordX((Double) value);
                        fireTableCellUpdated(rowIndex, colIndex);
                        break;
                    }
                    case 5: {
                        area.setLayoutCoordY((Double) value);
                        fireTableCellUpdated(rowIndex, colIndex);
                        break;
                    }
                    case 6: {
                        area.setLayoutCoordZ((Double) value);
                        fireTableCellUpdated(rowIndex, colIndex);
                        break;
                    }
                    case 7: {
                        area.setStagePosMapped(((String) value).equals("mapped"));
                        fireTableCellUpdated(rowIndex, colIndex);
                        break;
                    }
                }
            }
        }

        public void addRow(Object value) {
            RefArea a = (RefArea) value;
            refAreas.add(a);
            fireTableRowsInserted(getRowCount(), getRowCount());
        }

        public RefArea getRowData(int rowIdx) {
            if (rowIdx >= 0 && rowIdx < refAreas.size()) {
                return refAreas.get(rowIdx);
            } else {
                return null;
            }
        }

        public void removeRow(int index) {
            if (index < getRowCount()) {
                refAreas.remove(index);
                fireTableRowsDeleted(index, index);
            }
        }

        public void removeRows(int[] rowIdx) {
            for (int i = rowIdx[rowIdx.length - 1]; i >= rowIdx[0]; i--) {
                refAreas.remove(i);
            }
            fireTableRowsDeleted(rowIdx[0], rowIdx[rowIdx.length - 1]);
        }

    }
    // end RefAreaTableModel
        

    
    public RefPointListDlg(java.awt.Frame parent, ScriptInterface gui, IAcqLayout aLayout) {
        super(parent, false);
        listeners = new ArrayList<IRefPointListener>();
        this.core=gui.getMMCore();
        acqLayout=aLayout;
        initComponents();
        messageLabel.setText("");
        setRefPointList(acqLayout.getLandmarks());
        refPointTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);        
        refPointTable.getTableHeader().setReorderingAllowed(false);        
        ListSelectionModel lsm = refPointTable.getSelectionModel();
        lsm.addListSelectionListener(new SharedListSelectionHandler());
        refPointTable.setSelectionModel(lsm);

        try {
            xyStageName=core.getXYStageDevice();
            zStageName=core.getFocusDevice();
            double stageX=core.getXPosition(xyStageName);
            double stageY=core.getYPosition(xyStageName);
            double stageZ=core.getPosition(zStageName);
            stagePositionChanged(new Double[]{stageX, stageY, stageZ});

            updateStageRotAndTilt();
        } catch (Exception ex) {
            Logger.getLogger(RefPointListDlg.class.getName()).log(Level.SEVERE, null, ex);
        } 

        int mappedPos = acqLayout.getNoOfMappedStagePos();
        moveToButton.setEnabled(mappedPos > 0 );

        RefAreaTableModel model=(RefAreaTableModel) refPointTable.getModel();
        model.addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                RefAreaTableModel model=(RefAreaTableModel)refPointTable.getModel();
                if (e.getType() == TableModelEvent.UPDATE && (e.getColumn() >= 4 || e.getColumn() <= 6)) {
                    int row=e.getFirstRow();
//                    rpList.get(row).setLayoutCoord((Double)model.getValueAt(row,4),(Double)model.getValueAt(row,5),(Double)model.getValueAt(row, 6));
                }
                updateStageRotAndTilt();    
                int mappedPos = acqLayout.getNoOfMappedStagePos();
                moveToButton.setEnabled(mappedPos > 0 );
                modified=true;
                notifyRefAreaListeners(rpList);
            }});
        modified=false;
        
    }

    synchronized public void addListener(IRefPointListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
//        IJ.log(getClass().getName()+": "+listeners.size()+" total listeners");
    }
        
    synchronized public void removeListener(IRefPointListener listener) {
        if (listeners != null)
            listeners.remove(listener);
//        IJ.log(getClass().getName()+": "+listeners.size()+" listeners");
    }
    
    private void notifySelectionListeners(RefArea refArea) {
        synchronized (listeners) {
            for (IRefPointListener l:listeners) {
                l.referencePointSelectionChanged(refArea);
            }
        }
    }
    
    private void notifyRefAreaListeners(List<RefArea> refAreas) {
        synchronized (listeners) {
            for (IRefPointListener l:listeners) {
                l.referencePointsUpdated(refAreas);
            }
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        refPointTable = new javax.swing.JTable();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        stagePosLabel = new javax.swing.JLabel();
        moveToButton = new javax.swing.JButton();
        updateStagePosButton = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        tiltLabel = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        rotationLabel = new javax.swing.JLabel();
        messageLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Set Landmarks");
        setMinimumSize(new java.awt.Dimension(527, 300));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        refPointTable.setModel(new RefAreaTableModel(null));
        jScrollPane1.setViewportView(refPointTable);

        addButton.setText("+");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        removeButton.setText("-");
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("Stage position:");

        stagePosLabel.setText("jLabel2");

        moveToButton.setText("Move to");
        moveToButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveToButtonActionPerformed(evt);
            }
        });

        updateStagePosButton.setText("Update Stage Pos");
        updateStagePosButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateStagePosButtonActionPerformed(evt);
            }
        });

        jLabel4.setText("Tilt (z-axis):");

        tiltLabel.setText("jLabel5");

        jLabel2.setText("Rotation (x-y plane):");

        rotationLabel.setText("jLabel3");

        messageLabel.setForeground(new java.awt.Color(255, 0, 0));
        messageLabel.setText("jLabel3");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(addButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(removeButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(moveToButton))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel4)
                                    .addComponent(jLabel2))
                                .addGap(6, 6, 6)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(rotationLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 207, Short.MAX_VALUE)
                                    .addComponent(tiltLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(6, 6, 6)
                                .addComponent(stagePosLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(messageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(okButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(updateStagePosButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addGap(6, 6, 6))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addButton)
                    .addComponent(removeButton)
                    .addComponent(moveToButton)
                    .addComponent(updateStagePosButton))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(okButton)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(stagePosLabel)))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(rotationLabel)
                    .addComponent(cancelButton))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(tiltLabel))
                .addGap(6, 6, 6)
                .addComponent(messageLabel)
                .addGap(27, 27, 27))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void updateStageRotAndTilt() {
        int mappedPos=acqLayout.getNoOfMappedStagePos();
        if (mappedPos == 0) {
            tiltLabel.setText("unknown");
            rotationLabel.setText("unknown");     
        } else {
            try {    
                acqLayout.calcStageToLayoutTransform();
                tiltLabel.setText(String.format("%1$,.1f",acqLayout.getTilt())+" degree");
                AffineTransform at=acqLayout.getStageToLayoutTransform();
                double angle=acqLayout.getStageToLayoutRot();
                
                
                //move to listener?
                SampleArea.setStageToLayoutRot(angle);
                RefArea.setStageToLayoutRot(angle);
                
                
                angle=angle/Math.PI*180;
                if (angle > 180) angle=angle-360;
                rotationLabel.setText(String.format("%1$,.1f",angle)+" degree");
                String message="";
                List<RefArea> mappedList=acqLayout.getMappedLandmarks();
                if (mappedPos == 2) {
                    RefArea r0=mappedList.get(0);
                    RefArea r1=mappedList.get(1);
                    double sx=r1.getStageCoordX()-r0.getStageCoordX();
                    double sy=r1.getStageCoordY()-r0.getStageCoordY();
                    double sl=Math.sqrt(sx*sx+sy*sy);
                    double dx=r1.getLayoutCoordX()-r0.getLayoutCoordX();
                    double dy=r1.getLayoutCoordY()-r0.getLayoutCoordY();
                    double dl=Math.sqrt(dx*dx+dy*dy);
                    if (Math.abs(dl/sl - 1) > TOLERANCE_SCALE_FACTOR) {
                        message="Warning: Uniform scaling factor is "+String.format("%1$,.2f",dl/sl)+" (exceeding "+(100*TOLERANCE_SCALE_FACTOR)+"% tolerance)";
                    }
                } else if (mappedPos==3) {
                    RefArea r0=mappedList.get(0);
                    RefArea r1=mappedList.get(1);
                    RefArea r2=mappedList.get(2);
                    double s01x=r1.getStageCoordX()-r0.getStageCoordX();
                    double s01y=r1.getStageCoordY()-r0.getStageCoordY();
                    double s01l=Math.sqrt(s01x*s01x+s01y*s01y);
                    double d01x=r1.getLayoutCoordX()-r0.getLayoutCoordX();
                    double d01y=r1.getLayoutCoordY()-r0.getLayoutCoordY();
                    double d01l=Math.sqrt(d01x*d01x+d01y*d01y);

                    double s02x=r2.getStageCoordX()-r0.getStageCoordX();
                    double s02y=r2.getStageCoordY()-r0.getStageCoordY();
                    double s02l=Math.sqrt(s02x*s02x+s02y*s02y);
                    double d02x=r2.getLayoutCoordX()-r0.getLayoutCoordX();
                    double d02y=r2.getLayoutCoordY()-r0.getLayoutCoordY();
                    double d02l=Math.sqrt(d02x*d02x+d02y*d02y);

                    double s21x=r1.getStageCoordX()-r2.getStageCoordX();
                    double s21y=r1.getStageCoordY()-r2.getStageCoordY();
                    double s21l=Math.sqrt(s21x*s21x+s21y*s21y);
                    double d21x=r1.getLayoutCoordX()-r2.getLayoutCoordX();
                    double d21y=r1.getLayoutCoordY()-r2.getLayoutCoordY();
                    double d21l=Math.sqrt(d21x*d21x+d21y*d21y);
                    if (Math.abs(d01l/s01l - 1) > TOLERANCE_SCALE_FACTOR
                            || Math.abs(d21l/s21l - 1) > TOLERANCE_SCALE_FACTOR
                            ||Math.abs(d02l/s02l - 1) > TOLERANCE_SCALE_FACTOR) {
                        if (Math.abs(d01l/s01l - d21l/s21l) < 0.01
                               && Math.abs(d01l/s01l - d02l/s02l) < 0.01
                               && Math.abs(d02l/s02l - d21l/s21l) < 0.01) {
                            message="Warning: Uniform scaling factor is "+String.format("%1$,.2f",d01l/s01l)+" (exceeding "+(100*TOLERANCE_SCALE_FACTOR)+"% tolerance)";
                        } else {
                            message="Warning: Non-uniform scaling and shearing.";                              
                        }
                    }
                }
                messageLabel.setText(message);
            } catch (Exception ex) {
                tiltLabel.setText("Error");
                rotationLabel.setText("Error");
                Logger.getLogger(RefPointListDlg.class.getName()).log(Level.SEVERE, null, ex);
//                IJ.log(getClass().getName()+": affineTransform: "+ex.getMessage());
                JOptionPane.showMessageDialog(null, ex.getMessage());
            }
        }
    }
    
    private static List<RefArea> cloneRefPointList(List<RefArea> rpl) {
        ArrayList<RefArea> clonedList = new ArrayList<RefArea>(rpl.size());
        for (RefArea rp : rpl) {
            clonedList.add(new RefArea(rp));
        }
        return clonedList;
    }
    
    private void updateStagePosButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateStagePosButtonActionPerformed
        int row=refPointTable.getSelectedRow();
        if (row>=0) {
            RefAreaTableModel model=(RefAreaTableModel)refPointTable.getModel();
            try {
                double stageX=core.getXPosition(xyStageName);
                double stageY=core.getYPosition(xyStageName);
                double stageZ=core.getPosition(zStageName);
                for (int i=0; i<model.getRowCount(); i++) {
                    RefArea rp=model.getRowData(i);
                    if (i!=row 
                            && rp.isStagePosFound() 
                            && Math.round(rp.getStageCoordX())==Math.round(stageX)
                            && Math.round(rp.getStageCoordY())==Math.round(stageY)
                            && Math.round(rp.getStageCoordZ())==Math.round(stageZ)) {
//                    if (i!=row & Math.round((Double)model.getValueAt(i,1))==Math.round(stageX) & Math.round((Double)model.getValueAt(i,2))==Math.round(stageY)) {
                        JOptionPane.showMessageDialog(this, "A Reference Point is already defined for this stage position!");   
                        return;
                    }
                }
                model.setStagePos(row, stageX, stageY, stageZ);
                modified=true;
                notifyRefAreaListeners(rpList);
            } catch (Exception ex) {
                Logger.getLogger(RefPointListDlg.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else
            JOptionPane.showMessageDialog(this, "No Reference Point selected.");
    }//GEN-LAST:event_updateStagePosButtonActionPerformed

    private void moveToButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveToButtonActionPerformed
        int row=refPointTable.getSelectedRow();
        if (row>=0) {
            RefAreaTableModel model=(RefAreaTableModel)refPointTable.getModel();
            RefArea refArea=model.getRowData(row);
            if (!refArea.isStagePosFound()) {
                JOptionPane.showMessageDialog(this, "Stage position not mapped. Cannot move to this landmark.");
                return;
            }
            try {
                double stageX=(Double)model.getValueAt(row,1);
                double stageY=(Double)model.getValueAt(row,2);
                double stageZ=(Double)model.getValueAt(row,3);
                core.waitForDevice(zStageName);
                core.setPosition(zStageName,acqLayout.getEscapeZPos());
                core.waitForDevice(xyStageName);
                core.setXYPosition(xyStageName, stageX, stageY);
                core.waitForDevice(zStageName);
                core.setPosition(zStageName,stageZ);
            } catch (Exception ex) {
                Logger.getLogger(RefPointListDlg.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        } else
            JOptionPane.showMessageDialog(this, "No Reference Point selected.");
    }//GEN-LAST:event_moveToButtonActionPerformed

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        RefAreaTableModel model=(RefAreaTableModel)refPointTable.getModel();
        if (model.getRowCount()>=3) {
            JOptionPane.showMessageDialog(this, "Cannot create more than three Reference Points.");
            return;
        }    
        try {
            double stageX=core.getXPosition(xyStageName);
            double stageY=core.getYPosition(xyStageName);
            double stageZ=core.getPosition(zStageName);
            for (RefArea rp:rpList) {
                if (stageX==rp.getStageCoordX() && stageY==rp.getStageCoordY() && stageZ==rp.getStageCoordZ()) {
                    JOptionPane.showMessageDialog(this, "Stage position already in list.");
                    return;
                }
            }
            if (rpList.size()>1) {
                Vec3d rp1=new Vec3d(rpList.get(0).getStageCoordX(), rpList.get(0).getStageCoordY(), 0);
                Vec3d rp2=new Vec3d(rpList.get(1).getStageCoordX(), rpList.get(1).getStageCoordY(), 0);
                Vec3d newrp=new Vec3d(stageX,stageY,0);
                if (Vec3d.cross(newrp.minus(rp1),newrp.minus(rp2)).length() == 0) {
                    JOptionPane.showMessageDialog(this,"Cannot add current stage position as new Reference Point!\nThe new x-y stage position is colinear with x-y stage positions of existing Reference Points.");
                    return;
                }    
            }       
            RefArea newRP;
            if (model.getRowCount()>0) {
                //convert xy stage pos to xy layout pos; z pos in layout will be initialized with '0'
                Point2D lPoint=acqLayout.convertStageToLayoutPos_XY(new Point2D.Double(stageX, stageY));
                newRP=new RefArea("Landmark", stageX, stageY, stageZ, lPoint.getX(), lPoint.getY(), 0,core.getPixelSizeUm()*core.getImageWidth(),core.getPixelSizeUm()*core.getImageHeight(), "" );
            } else {
                //first landmark: initialize x-y-z layout pos with (0/0/0)
                newRP=new RefArea("Landmark", stageX, stageY, stageZ, 0,0,0, core.getPixelSizeUm()*core.getImageWidth(),core.getPixelSizeUm()*core.getImageHeight(), "" );    
            }
            newRP.setStagePosMapped(true);
            model.addRow(newRP);
//            updateStageRotAndTilt();
            int mappedPos = acqLayout.getNoOfMappedStagePos();
            moveToButton.setEnabled(mappedPos > 0 );
            modified=true;
            notifyRefAreaListeners(rpList);
        } catch (Exception ex) {
//            IJ.log(getClass().getName()+": addLandmark: "+ex.getMessage());
            Logger.getLogger(RefPointListDlg.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_addButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        acqLayout.setLandmarks(rpBackup);
        rpList=rpBackup;
        updateStageRotAndTilt();
        int mappedPos = acqLayout.getNoOfMappedStagePos();
        moveToButton.setEnabled(mappedPos > 0 );
        acqLayout.deselectAllLandmarks();
        notifyRefAreaListeners(rpList);
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        try {
            acqLayout.calcStageToLayoutTransform();
        } catch (Exception ex) {
            Logger.getLogger(RefPointListDlg.class.getName()).log(Level.SEVERE, null, ex);
        }
        acqLayout.deselectAllLandmarks();
        notifyRefAreaListeners(rpList);
        if (modified) 
            acqLayout.setModified(true);
        dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        int row=refPointTable.getSelectedRow();
        if (row>=0) {
            try {
                RefAreaTableModel model=(RefAreaTableModel)refPointTable.getModel();
                model.removeRow(row);
                modified=true;
            } catch (Exception ex) {
//                IJ.log(getClass().getName()+": removeLandmark: "+ex.getMessage());
                Logger.getLogger(RefPointListDlg.class.getName()).log(Level.SEVERE, null, ex);
            }
        } 
        moveToButton.setEnabled(acqLayout.getNoOfMappedStagePos() > 0 );
        notifyRefAreaListeners(rpList);
    }//GEN-LAST:event_removeButtonActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        int answer = JOptionPane.showConfirmDialog(this, "Do you want to keep the modified Landmark definitions?","", JOptionPane.YES_NO_OPTION);
        if (answer==JOptionPane.NO_OPTION) {
            acqLayout.setLandmarks(rpBackup);
            notifyRefAreaListeners(rpBackup);
        }
        acqLayout.deselectAllLandmarks();
        notifySelectionListeners(null);
    }//GEN-LAST:event_formWindowClosing

    
    public void closeDialog() {
        acqLayout.deselectAllLandmarks();
        WindowEvent wev = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
        this.dispatchEvent(wev);
    }    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel messageLabel;
    private javax.swing.JButton moveToButton;
    private javax.swing.JButton okButton;
    private javax.swing.JTable refPointTable;
    private javax.swing.JButton removeButton;
    private javax.swing.JLabel rotationLabel;
    private javax.swing.JLabel stagePosLabel;
    private javax.swing.JLabel tiltLabel;
    private javax.swing.JButton updateStagePosButton;
    // End of variables declaration//GEN-END:variables

    public void setRefPointList(List<RefArea> rpl) {
        rpList=rpl;
        rpBackup=cloneRefPointList(rpl);    

        RefAreaTableModel model=new RefAreaTableModel(rpList);
        refPointTable.setModel(model);
        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                RefAreaTableModel model=(RefAreaTableModel)refPointTable.getModel();
/*                if (e.getType() == TableModelEvent.UPDATE && (e.getColumn() >= 4 || e.getColumn() <= 6)) {
                    int row=e.getFirstRow();
//                    rpList.get(row).setLayoutCoord((Double)model.getValueAt(row,4),(Double)model.getValueAt(row,5),(Double)model.getValueAt(row, 6));
                }
*/
                updateStageRotAndTilt();    
                int mappedPos = acqLayout.getNoOfMappedStagePos();
                moveToButton.setEnabled(mappedPos > 0 );
                modified=true;
                notifyRefAreaListeners(rpList);
            }});
        modified=false;
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

    
    class SharedListSelectionHandler implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {  
            ListSelectionModel lsm = (ListSelectionModel)e.getSource();
 
            for (RefArea rp : rpList) {
                rp.setSelected(false);
            }
            RefArea selectedRefArea=null;
            if (!lsm.isSelectionEmpty()) {
                selectedRefArea=rpList.get(lsm.getMinSelectionIndex());
                selectedRefArea.setSelected(true);
            }
            notifySelectionListeners(selectedRefArea);
         }   
    }
}
