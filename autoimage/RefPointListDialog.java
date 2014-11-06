/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import ij.IJ;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
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
 * @author Karsten
 */
public class RefPointListDialog extends javax.swing.JDialog implements IStageMonitorListener {

    private CMMCore core;
    private AcquisitionLayout acqLayout;
    private List<RefArea> rpList;
    private List<RefArea> rpBackup;
//    private AffineTransform layoutTransform;
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
        

    
    public RefPointListDialog(java.awt.Frame parent, ScriptInterface gui, AcquisitionLayout aLayout) {
        super(parent, false);
        listeners = new ArrayList<IRefPointListener>();
        this.core=gui.getMMCore();
        acqLayout=aLayout;
//        acqLayoutPanel=acqLP;
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
//            updateStagePosLabel(stageX,stageY,stageZ);

            updateStageRotAndTilt();
//            stagePosLabel.setText(Double.toString(stageX)+"; "+Double.toString(stageY)+"; "+Double.toString(stageZ));            
        } catch (Exception ex) {
            Logger.getLogger(RefPointListDialog.class.getName()).log(Level.SEVERE, null, ex);
        } 

        int mappedPos = acqLayout.getNoOfMappedStagePos();
        moveToButton.setEnabled(mappedPos > 0 );
        ((AcqFrame)getParent()).setLandmarkFound(mappedPos > 0);

        RefAreaTableModel model=(RefAreaTableModel) refPointTable.getModel();
        model.addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                RefAreaTableModel model=(RefAreaTableModel)refPointTable.getModel();
/*                if (e.getType() == TableModelEvent.UPDATE && e.getColumn()==4 && e.getFirstRow()==0) {
                    double refLayoutX=(Double)model.getValueAt(0, 4);
                    double refStageX=(Double)model.getValueAt(0, 1);
                    for (int i=1; i<model.getRowCount(); i++) {
                        double sX=(Double)model.getValueAt(i, 1);
                        model.setValueAt(refLayoutX+sX-refStageX,i,4);
                    }
                    for (int i=0; i<model.getRowCount(); i++)
                        rpList.get(i).setLayoutCoordX((Double)model.getValueAt(i,4));
                } else if (e.getType() == TableModelEvent.UPDATE & e.getColumn()==5 & e.getFirstRow()==0) {
                    double refLayoutY=(Double)model.getValueAt(0, 5);
                    double refStageY=(Double)model.getValueAt(0, 2);
                    for (int i=1; i<model.getRowCount(); i++) {
                        double sY=(Double)model.getValueAt(i, 2);
                        model.setValueAt(refLayoutY+sY-refStageY,i,5);
                    }
                    for (int i=0; i<model.getRowCount(); i++)
                        rpList.get(i).setLayoutCoordY((Double)model.getValueAt(i,5));
                } else if (e.getType() == TableModelEvent.UPDATE & e.getColumn()==6) {
                    rpList.get(e.getFirstRow()).setLayoutCoordZ((Double)model.getValueAt(e.getFirstRow(),6));
                }
*/
                if (e.getType() == TableModelEvent.UPDATE && (e.getColumn() >= 4 || e.getColumn() <= 6)) {
                    int row=e.getFirstRow();
//                    rpList.get(row).setLayoutCoord((Double)model.getValueAt(row,4),(Double)model.getValueAt(row,5),(Double)model.getValueAt(row, 6));
                }
                updateStageRotAndTilt();    
                int mappedPos = acqLayout.getNoOfMappedStagePos();
                moveToButton.setEnabled(mappedPos > 0 );
                ((AcqFrame)getParent()).setLandmarkFound(mappedPos > 0);
                modified=true;
                                
//                acqLayoutPanel.repaint();
                notifyRefAreaListeners(rpList);
            }});
        modified=false;
        
    }

    synchronized public void addListener(IRefPointListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
        IJ.log(getClass().getName()+": "+listeners.size()+" listeners");
    }
        
    synchronized public void removeListener(IRefPointListener listener) {
        if (listeners != null)
            listeners.remove(listener);
        IJ.log(getClass().getName()+": "+listeners.size()+" listeners");
    }
    
    private void notifySelectionListeners(RefArea refArea) {
        synchronized (listeners) {
            for (IRefPointListener l:listeners) {
                l.selectedRefPointChanged(refArea);
            }
        }
    }
    
    private void notifyRefAreaListeners(List<RefArea> refAreas) {
        synchronized (listeners) {
            for (IRefPointListener l:listeners) {
                l.referencePointsChanged(refAreas);
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
        setMaximumSize(new java.awt.Dimension(2147483647, 300));
        setMinimumSize(new java.awt.Dimension(527, 300));
        setPreferredSize(new java.awt.Dimension(527, 300));
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

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(6, 6, 6)
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 515, Short.MAX_VALUE))
                    .add(layout.createSequentialGroup()
                        .add(20, 20, 20)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(layout.createSequentialGroup()
                                .add(addButton)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(removeButton)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(moveToButton))
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                    .add(jLabel4)
                                    .add(jLabel2))
                                .add(6, 6, 6)
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                    .add(rotationLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 207, Short.MAX_VALUE)
                                    .add(tiltLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                                .add(jLabel1)
                                .add(6, 6, 6)
                                .add(stagePosLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 207, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(messageLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, okButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, updateStagePosButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(cancelButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .add(6, 6, 6))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .add(6, 6, 6)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 98, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(6, 6, 6)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(addButton)
                    .add(removeButton)
                    .add(moveToButton)
                    .add(updateStagePosButton))
                .add(6, 6, 6)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(okButton)
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jLabel1)
                        .add(stagePosLabel)))
                .add(6, 6, 6)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(rotationLabel)
                    .add(cancelButton))
                .add(6, 6, 6)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(tiltLabel))
                .add(6, 6, 6)
                .add(messageLabel)
                .add(27, 27, 27))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void updateStageRotAndTilt() {
//        IJ.log(getClass().getName()+".updateStagePosAndTilt");
        int mappedPos=acqLayout.getNoOfMappedStagePos();
        if (mappedPos == 0) {
            tiltLabel.setText("unknown");
            rotationLabel.setText("unknown");     
        } else {
            try {    
                acqLayout.calcStageToLayoutTransform();
                tiltLabel.setText(String.format("%1$,.1f",acqLayout.getTilt())+" degree");
                AffineTransform at=acqLayout.getStageToLayoutTransform();
                //double angle=Math.atan2(at.getShearY(), at.getScaleY());
                double angle=acqLayout.getStageToLayoutRot();
                
                
                //move to listener?
                Area.setStageToLayoutRot(angle);
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
/*                    if (Math.abs(at.getScaleX()/at.getScaleY() - 1) > TOLERANCE_SCALE_FACTOR) {
                        message="Warning: Non-uniform scaling (x:y ratio="+String.format("%1$,.2f",at.getScaleX()/at.getScaleY());
                    }*/
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
/*                            message="Warning: Uniform scaling "+String.format("%1$,.2f",d01l/s01l)
                            +", "+String.format("%1$,.2f",d02l/s02l)
                            +", "+String.format("%1$,.2f",d21l/s21l);    */
                        } else {
                            message="Warning: Non-uniform scaling and shearing.";                              
                        }
                    }
/*                    if (Math.abs(at.getScaleX()/at.getScaleY() - 1) > TOLERANCE_SCALE_FACTOR
                            && at.getShearX() == -at.getShearY()) {
                        message="Warning: Non-uniform scaling (x:y ratio="+String.format("%1$,.2f",at.getScaleX()/at.getScaleY());
                    }
                    if (Math.abs(at.getScaleX()/at.getScaleY() - 1) <= TOLERANCE_SCALE_FACTOR
                            && at.getShearX() != -at.getShearY()) {
                        message="Warning: Shearing (x:y ratio="+String.format("%1$,.2f",at.getShearX()/at.getShearY());
                    }
                    if (Math.abs(at.getScaleX()/at.getScaleY() - 1) > TOLERANCE_SCALE_FACTOR
                            && at.getShearX() != -at.getShearY()) {
                        message="Warning: Non-uniform scaling (x:y ratio="+String.format("%1$,.2f",at.getScaleX()/at.getScaleY())+"Shearing (x:y ratio="+String.format("%1$,.2f",at.getShearX()/at.getShearY());
                    }*/

                }
/*                messageLabel.setText("Shear X: "+String.format("%1$,.2f",at.getShearX())
                        +", Shear Y: "+String.format("%1$,.2f",at.getShearY())
                        +", Scale X: "+String.format("%1$,.2f",at.getScaleX())
                        +", Scale Y: "
                        +message);*/
                messageLabel.setText(message);
            } catch (Exception ex) {
                tiltLabel.setText("Error");
                rotationLabel.setText("Error");
                Logger.getLogger(RefPointListDialog.class.getName()).log(Level.SEVERE, null, ex);
                IJ.log(getClass().getName()+": affineTransform: "+ex.getMessage());
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
    
/*    public void updateStagePosLabel(double x, double y, double z) {
        stagePosLabel.setText(
                  String.format("%1$,.2f",x)+"; "
                + String.format("%1$,.2f",y)+"; "
                + String.format("%1$,.2f",z));
    }
*/    
    private void updateStagePosButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateStagePosButtonActionPerformed
        int row=refPointTable.getSelectedRow();
        if (row>=0) {
            RefAreaTableModel model=(RefAreaTableModel)refPointTable.getModel();
            try {
                double stageX=core.getXPosition(xyStageName);
                double stageY=core.getYPosition(xyStageName);
                double stageZ=core.getPosition(zStageName);
                for (int i=0; i<model.getRowCount(); i++) {
                    if (i!=row & Math.round((Double)model.getValueAt(i,1))==Math.round(stageX) & Math.round((Double)model.getValueAt(i,2))==Math.round(stageY)) {
                        JOptionPane.showMessageDialog(this, "A Reference Point is already defined for this stage position!");   
                        return;
                    }
                }
                RefArea rp=rpList.get(row);
                //set coordinates and stagePosFound=true;
//                rp.setStageCoord(stageX, stageY, stageZ);
//                rp.setStagePosMapped(true);

                model.setStagePos(row, stageX, stageY, stageZ);

                /*                model.setValueAt(new Double(stageX),row,1);
                model.setValueAt(new Double(stageY),row,2);
                model.setValueAt(new Double(stageZ),row,3);
                model.setValueAt(new String("mapped"), row,7);*/
                //leads to call of tableChanged() --> updateRotAndTilt()
                
//                acqLayoutPanel.repaint();
                modified=true;
                notifyRefAreaListeners(rpList);
            } catch (Exception ex) {
                Logger.getLogger(RefPointListDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else
            JOptionPane.showMessageDialog(this, "No Reference Point selected.");
    }//GEN-LAST:event_updateStagePosButtonActionPerformed

    private void moveToButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveToButtonActionPerformed
        int row=refPointTable.getSelectedRow();
        if (row>=0) {
            RefAreaTableModel model=(RefAreaTableModel)refPointTable.getModel();
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
//                stageXPosLabel.setText(Double.toString(stageX));
//                stageYPosLabel.setText(Double.toString(stageY));
//                stageZPosLabel.setText(Double.toString(stageZ));            
//                acqLayoutPanel.repaint();
            } catch (Exception ex) {
                Logger.getLogger(RefPointListDialog.class.getName()).log(Level.SEVERE, null, ex);
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
                Vec3d lPoint=acqLayout.convertStagePosToLayoutPos(stageX, stageY, stageZ);
                newRP=new RefArea("Landmark", stageX, stageY, stageZ, lPoint.x, lPoint.y, lPoint.z,core.getPixelSizeUm()*core.getImageWidth(),core.getPixelSizeUm()*core.getImageHeight(), "" );
            } else {
                newRP=new RefArea("Landmark", stageX, stageY, stageZ, 0,0,0, core.getPixelSizeUm()*core.getImageWidth(),core.getPixelSizeUm()*core.getImageHeight(), "" );    
            }
            newRP.setStagePosMapped(true);
            model.addRow(newRP);
/*            rpList.add(newRP);
            model.addRow(new Object[]{
                    newRP.getName(),
                    newRP.getStageCoordX(),
                    newRP.getStageCoordY(),
                    newRP.getStageCoordZ(),
                    newRP.getLayoutCoordX(),
                    newRP.getLayoutCoordY(),
                    newRP.getLayoutCoordZ(),
                    new String(newRP.isStagePosFound() ? "mapped":"not mapped")
            });*/
            
//            updateStagePosLabel(stageX, stageY, stageZ);
            //change setLandmarkFound flag in AcqFrame and acqLayout 
            
//            updateStageRotAndTilt();
            int mappedPos = acqLayout.getNoOfMappedStagePos();
            moveToButton.setEnabled(mappedPos > 0 );
            ((AcqFrame)getParent()).setLandmarkFound(mappedPos > 0);

//            acqLayoutPanel.repaint();
            modified=true;
            notifyRefAreaListeners(rpList);
        } catch (Exception ex) {
            IJ.log(getClass().getName()+": addLandmark: "+ex.getMessage());
            Logger.getLogger(RefPointListDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_addButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        acqLayout.setLandmarks(rpBackup);
        rpList=rpBackup;
        updateStageRotAndTilt();
        int mappedPos = acqLayout.getNoOfMappedStagePos();
        moveToButton.setEnabled(mappedPos > 0 );
        ((AcqFrame)getParent()).setLandmarkFound(mappedPos > 0);
        acqLayout.deselectAllLandmarks();
//        acqLayoutPanel.repaint();
        notifyRefAreaListeners(rpList);
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        try {
            acqLayout.calcStageToLayoutTransform();
        } catch (Exception ex) {
            Logger.getLogger(RefPointListDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
        acqLayout.deselectAllLandmarks();
//        acqLayoutPanel.repaint();
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
//                RefArea removedRefArea = rpList.remove(row);
                model.removeRow(row);
//                acqLayoutPanel.repaint();
                modified=true;
            } catch (Exception ex) {
                IJ.log(getClass().getName()+": removeLandmark: "+ex.getMessage());
                Logger.getLogger(RefPointListDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        } 
        int mappedPos = acqLayout.getNoOfMappedStagePos();
        moveToButton.setEnabled(mappedPos > 0 );
        notifyRefAreaListeners(rpList);
//        ((AcqFrame)getParent()).setLandmarkFound(mappedPos > 0);

//        ((AcqFrame)getParent()).setLandmarkFound(acqLayout.getNoOfMappedStagePos() > 0);
    }//GEN-LAST:event_removeButtonActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        int answer = JOptionPane.showConfirmDialog(this, "Do you want to keep the modified Landmark definitions?","", JOptionPane.YES_NO_OPTION);
        if (answer==JOptionPane.NO_OPTION) {
            acqLayout.setLandmarks(rpBackup);
            notifyRefAreaListeners(rpBackup);
        }
        acqLayout.deselectAllLandmarks();
        notifySelectionListeners(null);
//        acqLayoutPanel.repaint();
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

    protected void setRefPointList(List<RefArea> rpl) {
        rpList=rpl;
        rpBackup=cloneRefPointList(rpl);    
/*        RefAreaTableModel model=(RefAreaTableModel) refPointTable.getModel();
        int r=model.getRowCount();
        for (int i=r-1; i>=0; i--)
            model.removeRow(i);
        if (rpl!=null) {
            for (RefArea rp : rpl) {
                model.addRow(new Object[]{
                    rp.getName(),
                    rp.getStageCoordX(),
                    rp.getStageCoordY(),
                    rp.getStageCoordZ(),
                    rp.getLayoutCoordX(),
                    rp.getLayoutCoordY(),
                    rp.getLayoutCoordZ(),
                    new String(rp.isStagePosFound() ? "mapped":"not mapped")
                });
            }            
        }*/
        RefAreaTableModel model=new RefAreaTableModel(rpList);
        refPointTable.setModel(model);
        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                RefAreaTableModel model=(RefAreaTableModel)refPointTable.getModel();
/*                if (e.getType() == TableModelEvent.UPDATE && e.getColumn()==4 && e.getFirstRow()==0) {
                    double refLayoutX=(Double)model.getValueAt(0, 4);
                    double refStageX=(Double)model.getValueAt(0, 1);
                    for (int i=1; i<model.getRowCount(); i++) {
                        double sX=(Double)model.getValueAt(i, 1);
                        model.setValueAt(refLayoutX+sX-refStageX,i,4);
                    }
                    for (int i=0; i<model.getRowCount(); i++)
                        rpList.get(i).setLayoutCoordX((Double)model.getValueAt(i,4));
                } else if (e.getType() == TableModelEvent.UPDATE & e.getColumn()==5 & e.getFirstRow()==0) {
                    double refLayoutY=(Double)model.getValueAt(0, 5);
                    double refStageY=(Double)model.getValueAt(0, 2);
                    for (int i=1; i<model.getRowCount(); i++) {
                        double sY=(Double)model.getValueAt(i, 2);
                        model.setValueAt(refLayoutY+sY-refStageY,i,5);
                    }
                    for (int i=0; i<model.getRowCount(); i++)
                        rpList.get(i).setLayoutCoordY((Double)model.getValueAt(i,5));
                } else if (e.getType() == TableModelEvent.UPDATE & e.getColumn()==6) {
                    rpList.get(e.getFirstRow()).setLayoutCoordZ((Double)model.getValueAt(e.getFirstRow(),6));
                }
*/
                if (e.getType() == TableModelEvent.UPDATE && (e.getColumn() >= 4 || e.getColumn() <= 6)) {
                    int row=e.getFirstRow();
//                    rpList.get(row).setLayoutCoord((Double)model.getValueAt(row,4),(Double)model.getValueAt(row,5),(Double)model.getValueAt(row, 6));
                }
                updateStageRotAndTilt();    
                int mappedPos = acqLayout.getNoOfMappedStagePos();
                moveToButton.setEnabled(mappedPos > 0 );
                ((AcqFrame)getParent()).setLandmarkFound(mappedPos > 0);
                modified=true;
                                
//                acqLayoutPanel.repaint();
                notifyRefAreaListeners(rpList);
            }});
        modified=false;
    }

    @Override
    public void stagePositionChanged(final Double[] stagePos) {
//        updateStagePosLabel(stageX,stageY,stageZ);
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
//                    int maxIndex = lsm.getMaxSelectionIndex();
            }
            notifySelectionListeners(selectedRefArea);
//            acqLayoutPanel.repaint();
         }   
    }
}
