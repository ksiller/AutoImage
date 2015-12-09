package autoimage.tools;

import autoimage.AcqBasicLayout;
import autoimage.AcqCustomLayout;
import autoimage.AcqWellplateLayout;
import autoimage.api.BasicArea;
import autoimage.Utils;
import autoimage.api.IAcqLayout;
import autoimage.gui.NumberTableCellRenderer;
import ij.IJ;
import ij.Prefs;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten Siller
 */
public class LayoutManagerDlg extends javax.swing.JDialog {

    private IAcqLayout startUpLayout;
    private static String lastFileLocation="";
    private static Map<String, String> availableAreaClasses=null;
    private String lastAreaType=null;

    
    
        private class AreaTableModel extends AbstractTableModel {

        public final String[] COLUMN_NAMES = new String[]{"Area Id", "Area Name", "Type", "Center (mm)", "Width (mm)", "Height (mm)", "Relative Z (mm)"};
        private List<BasicArea> areas;

        public AreaTableModel(List<BasicArea> al) {
            super();
            setData(al,false);
        }

        public void setData(List<BasicArea> al, boolean updateView) {
            if (al == null) {
                al = new ArrayList<BasicArea>();
            }
            this.areas = al;
            if (updateView) {
                fireTableDataChanged();
            }
        }

        public List<BasicArea> getAreaList() {
            return areas;
        }
        
        @Override
        public int getRowCount() {
            return areas.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int colIdx) {
            return COLUMN_NAMES[colIdx];
        }

        @Override
        public Class getColumnClass(int colIdx) {
            return getValueAt(0, colIdx).getClass();
        }

        @Override
        public Object getValueAt(int rowIndex, int colIndex) {
            BasicArea a;
            if (areas != null & rowIndex < areas.size()) {
                a = areas.get(rowIndex);
                switch (colIndex) {
                    case 0: return a.getId();
                    case 1: return a.getName();
                    case 2: return a.getShapeType();
                    case 3: {//center
                                DecimalFormat df = new DecimalFormat("###,###,##0.000");
                                return df.format(a.getCenterXYPos().getX()/1000)+" / "+df.format(a.getCenterXYPos().getY()/1000);
                            } 
                    case 4: return a.getBounds2D().getWidth()/1000;
                    case 5: return a.getBounds2D().getHeight()/1000;
                    case 6: return a.getRelativeZPos()/1000;
                    default: return null;
                }
            } else {
                return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int colIndex) {
            return (colIndex == 1);
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int colIndex) {
            BasicArea area;
            if (areas != null & rowIndex < areas.size()) {
                area = areas.get(rowIndex);
                switch (colIndex) {
                    case 1: {
                        area.setName((String) value);
                        fireTableCellUpdated(rowIndex, colIndex);
                        break;
                    }
                }
            }
        }

        public void addRow(Object value) {
            BasicArea a = (BasicArea) value;
            areas.add(a);
            fireTableRowsInserted(getRowCount(), getRowCount());
        }

        public BasicArea getRowData(int rowIdx) {
            if (rowIdx >= 0 && rowIdx < areas.size()) {
                return areas.get(rowIdx);
            } else {
                return null;
            }
        }

        /*
        @Param rowIdx: array of indices in model
        @Param lastPlusIndex: model rowindex that corresponds to 1 below selection in view
        */
        public int rowDown(int[] rowIdx, int lastPlusOneIndex) {
            BasicArea temp=areas.get(lastPlusOneIndex);
            //move last entry in selection down one
            areas.set(lastPlusOneIndex, areas.get(rowIdx[rowIdx.length-1]));
            for (int i=rowIdx.length-1; i>0; i--) {
                areas.set(rowIdx[i], areas.get(rowIdx[i-1]));
            }
            areas.set(rowIdx[0],temp);
            fireTableRowsUpdated(0,getRowCount()-1);
            return 0;
        }

        /*
        @Param rowIdx: array of indices in model
        @Param firstMinusOneIndex: model rowindex that corresponds to 1 below selection in view
        */
        public int rowUp(int[] rowIdx, int firstMinusOneIndex) {
            BasicArea temp=areas.get(firstMinusOneIndex);
            //move first entry in selection up one
            areas.set(firstMinusOneIndex, areas.get(rowIdx[0]));
            for (int i=0; i<rowIdx.length-1; i++) {
                areas.set(rowIdx[i], areas.get(rowIdx[i+1]));
            }
            areas.set(rowIdx[rowIdx.length-1],temp);
            fireTableRowsUpdated(0,getRowCount()-1);
            return 0;
        }

        public void removeRow(Object element) {
            for (int i = 0; i < areas.size(); i++) {
                if (((BasicArea) element).getId() == areas.get(i).getId()) {
                    areas.remove(i);
                    fireTableRowsDeleted(i, i);
                }
            }
        }

        public void removeRows(int[] rowIdx) {
            for (int i = rowIdx[rowIdx.length - 1]; i >= rowIdx[0]; i--) {
                areas.remove(i);
            }
            fireTableRowsDeleted(rowIdx[0], rowIdx[rowIdx.length - 1]);
        }

        private void setRowData(int rowIdx, BasicArea area) {
            if (rowIdx >=0 && rowIdx < areas.size()) { 
                areas.set(rowIdx, area);
                fireTableRowsUpdated(rowIdx,rowIdx);
            }    
        }


    }
    // end AreaTableModel

    
    
    
    public LayoutManagerDlg(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        areaTable.setDefaultRenderer(Double.class, new NumberTableCellRenderer(new DecimalFormat("###,###,##0.000")));
        if (lastFileLocation.equals(""))
            lastFileLocation=Prefs.getHomeDir();
        if (availableAreaClasses == null || availableAreaClasses.isEmpty())
            loadAvailableAreaClasses();
    }

    private void loadAvailableAreaClasses() {
        Class clazz = BasicArea.class;
        URL location = clazz.getResource('/'+clazz.getName().replace('.', '/')+".class");
        String locationStr=location.toString().substring(0, location.toString().indexOf(".jar!")+6);
        String jarFileStr=locationStr.substring(locationStr.indexOf("file:")+5,locationStr.length()-2);
        //replace forward slash with OS specific path separator
        jarFileStr.replace("/",File.pathSeparator);
        URLClassLoader classLoader;
        JarFile jarFile;
        try {
            jarFile = new JarFile(jarFileStr);
            Enumeration e = jarFile.entries();

            URL[] urls = { new URL(locationStr) };
            classLoader = URLClassLoader.newInstance(urls,
                //    this.getClass().getClassLoader());
            BasicArea.class.getClassLoader());

            int i=0;
            availableAreaClasses=new HashMap<String,String>();
            while (e.hasMoreElements()) {
                JarEntry je = (JarEntry) e.nextElement();
                if(je.isDirectory() || !je.getName().endsWith(".class")){
                    continue;
                }
                 // -6 to remove ".class"
                String className = je.getName().substring(0,je.getName().length()-6);
                className = className.replace('/', '.');
                try {
                    clazz=Class.forName(className);
                    //only add non-abstract BasicArea classes that support custom layouts
                    if (BasicArea.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                        BasicArea area=(BasicArea)clazz.newInstance();
                        if ((area.supportedLayouts() & BasicArea.SUPPORT_CUSTOM_LAYOUT) == BasicArea.SUPPORT_CUSTOM_LAYOUT) {
                            availableAreaClasses.put(area.getShapeType(), className);
                        }   
                    }
                } catch (ClassNotFoundException ex) {
                    IJ.log(ex.toString());
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                } catch (InstantiationException ex) {
                    IJ.log(ex.toString());
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    IJ.log(ex.toString());
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                } catch (Exception ex) {
                    //catch all other exceptions so we can continue with remaining class definitions
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                    IJ.log(ex.toString());
                    for (StackTraceElement s:ex.getStackTrace()) {
                        IJ.log(s.getClassName()+", "+s.getMethodName()+", "+s.getLineNumber());
                    }
                }                       
            }
            if (availableAreaClasses.isEmpty()) {
                JOptionPane.showMessageDialog(this,"No 'Area' classes found.");
                return;
            }
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
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

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        plateWidthField = new javax.swing.JFormattedTextField();
        plateHeightField = new javax.swing.JFormattedTextField();
        closeButton = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        openButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        newButton = new javax.swing.JButton();
        fileLocationLabel = new javax.swing.JLabel();
        wellDistanceField = new javax.swing.JFormattedTextField();
        wellDepthField = new javax.swing.JFormattedTextField();
        bottomThicknessField = new javax.swing.JFormattedTextField();
        jSeparator1 = new javax.swing.JSeparator();
        platenameField = new javax.swing.JFormattedTextField();
        jLabel15 = new javax.swing.JLabel();
        plateLengthField = new javax.swing.JFormattedTextField();
        jSeparator2 = new javax.swing.JSeparator();
        jScrollPane1 = new javax.swing.JScrollPane();
        areaTable = new javax.swing.JTable();
        newAreaButton = new javax.swing.JButton();
        removeAreaButton = new javax.swing.JButton();
        moveUpButton = new javax.swing.JButton();
        moveDownButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        editAreaButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Custom Layout Manager");
        setMinimumSize(new java.awt.Dimension(557, 486));

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel1.setText("Width (mm):");

        jLabel2.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel2.setText("Height (mm):");

        plateWidthField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0.000"))));
        plateWidthField.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        plateWidthField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                plateWidthFieldPropertyChange(evt);
            }
        });

        plateHeightField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0.000"))));
        plateHeightField.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        plateHeightField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                plateHeightFieldPropertyChange(evt);
            }
        });

        closeButton.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        closeButton.setText("Close");
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        jLabel9.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel9.setText("Layout name:");

        jLabel10.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel10.setText("Location:");

        openButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/openDoc.png"))); // NOI18N
        openButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openButtonActionPerformed(evt);
            }
        });

        saveButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/saveDoc.png"))); // NOI18N
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        newButton.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        newButton.setText("New");
        newButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newButtonActionPerformed(evt);
            }
        });

        fileLocationLabel.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        fileLocationLabel.setText("jLabel11");

        wellDistanceField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0.000"))));
        wellDistanceField.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N

        wellDepthField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0.000"))));
        wellDepthField.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N

        bottomThicknessField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0.000"))));
        bottomThicknessField.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N

        platenameField.setText("jFormattedTextField11");
        platenameField.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N

        jLabel15.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel15.setText("Length (mm):");

        plateLengthField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0.000"))));
        plateLengthField.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        plateLengthField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                plateLengthFieldPropertyChange(evt);
            }
        });

        areaTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(areaTable);

        newAreaButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/add2.png"))); // NOI18N
        newAreaButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newAreaButtonActionPerformed(evt);
            }
        });

        removeAreaButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/delete.png"))); // NOI18N
        removeAreaButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeAreaButtonActionPerformed(evt);
            }
        });

        moveUpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Up.png"))); // NOI18N
        moveUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveUpButtonActionPerformed(evt);
            }
        });

        moveDownButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/Down.png"))); // NOI18N
        moveDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveDownButtonActionPerformed(evt);
            }
        });

        jLabel3.setText("Layout dimensions:");

        editAreaButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/autoimage/resources/edit.png"))); // NOI18N
        editAreaButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editAreaButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator1)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel9)
                                    .addComponent(jLabel10))
                                .addGap(6, 6, 6)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(fileLocationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 352, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addComponent(platenameField)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(newButton)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(openButton)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(saveButton))))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(wellDistanceField, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                            .addComponent(wellDepthField, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                            .addComponent(bottomThicknessField, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 493, Short.MAX_VALUE)
                                        .addGap(6, 6, 6)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(newAreaButton, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(removeAreaButton, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(moveUpButton, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(moveDownButton, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(editAreaButton, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                        .addComponent(jLabel1)
                                        .addGap(11, 11, 11)
                                        .addComponent(plateWidthField, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(jLabel15)
                                        .addGap(6, 6, 6)
                                        .addComponent(plateLengthField, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(jLabel2)
                                        .addGap(6, 6, 6)
                                        .addComponent(plateHeightField, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, Short.MAX_VALUE)))
                                .addGap(4, 4, 4)))
                        .addGap(12, 12, 12))))
            .addGroup(layout.createSequentialGroup()
                .addGap(235, 235, 235)
                .addComponent(closeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {bottomThicknessField, wellDepthField, wellDistanceField});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel9)
                    .addComponent(newButton)
                    .addComponent(openButton)
                    .addComponent(saveButton)
                    .addComponent(platenameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(fileLocationLabel))
                .addGap(6, 6, 6)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(jLabel3)
                .addGap(2, 2, 2)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(plateWidthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15)
                    .addComponent(plateLengthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(plateHeightField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(wellDistanceField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(wellDepthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(bottomThicknessField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(newAreaButton, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(3, 3, 3)
                                .addComponent(removeAreaButton, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(3, 3, 3)
                                .addComponent(moveUpButton, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(3, 3, 3)
                                .addComponent(moveDownButton, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(3, 3, 3)
                                .addComponent(editAreaButton, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE))))
                .addGap(6, 6, 6)
                .addComponent(closeButton)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public void setCustomLayout(IAcqLayout layout) {
        if (layout!=null) {
            platenameField.setValue(layout.getName());
            if (layout.getFile()==null) {
                layout.setFile(new File(lastFileLocation,layout.getName()));
            }
            fileLocationLabel.setText(layout.getFile().getParent());
            plateWidthField.setValue(layout.getWidth()/1000);
            plateLengthField.setValue(layout.getLength()/1000);
            plateHeightField.setValue(layout.getHeight()/1000);
            areaTable.setModel(new AreaTableModel(layout.getAreaArray()));
        }
        lastFileLocation=layout.getFile().getParent();
        startUpLayout=layout;
    }
    
    private IAcqLayout getCustomLayout() {
        startUpLayout.setName((String)platenameField.getValue());
        startUpLayout.setFile(new File(fileLocationLabel.getText(),startUpLayout.getName()));
        //convert dimensions from mm to um
        startUpLayout.setWidth(((Number)plateWidthField.getValue()).doubleValue() * 1000);
        startUpLayout.setLength(((Number)plateLengthField.getValue()).doubleValue() * 1000);
        startUpLayout.setHeight(((Number)plateHeightField.getValue()).doubleValue() * 1000);
        return startUpLayout;
    }

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        //check if modified and if yes, open save dialog
        if (startUpLayout.isModified()) {
            int result=JOptionPane.showConfirmDialog(this, "Save current layout configuration?", "Custom Layout Manager", JOptionPane.YES_NO_CANCEL_OPTION);
            switch (result) {
                case JOptionPane.CANCEL_OPTION: {
                    break;
                }
                case JOptionPane.NO_OPTION: {
                    dispose();
                    break;
                }
                case JOptionPane.YES_OPTION: {
                    saveLayout();
                    break;
                }
            }
        } else {
            dispose();
        }
    }//GEN-LAST:event_closeButtonActionPerformed

    private void saveLayout() {
        JFileChooser jfc = new JFileChooser();
        if (startUpLayout.getFile()==null) {
            startUpLayout.setFile(new File(Prefs.getHomeDir(),startUpLayout.getName()));
        }
        jfc.setCurrentDirectory(startUpLayout.getFile().getParentFile());
        jfc.setSelectedFile(new File(startUpLayout.getFile().getName()));
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setMultiSelectionEnabled(false);
        jfc.ensureFileIsVisible(startUpLayout.getFile());
        if (jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File f = jfc.getSelectedFile();
            if (!Utils.getExtension(f).toLowerCase().equals(".txt")) {
                JOptionPane.showMessageDialog(this,"Not a txt file");
                f = new File(f.getAbsolutePath() + ".txt");
            }
            if (f.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(null, "Replace " + f.getName(), "", JOptionPane.YES_NO_OPTION);
                if (overwrite != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            startUpLayout=getCustomLayout();
            startUpLayout.setName(f.getName());
            startUpLayout.setFile(f);

            FileWriter fw;
            try {
                fw = new FileWriter(f);
                JSONObject layoutObj=new JSONObject();
                try {
                    JSONObject obj=startUpLayout.toJSONObject();
                    if (obj!=null) {
                        layoutObj.put(IAcqLayout.TAG_LAYOUT,obj);
                        fw.write(layoutObj.toString(4));
                    }
//                    startUpLayout=config;
                } catch (JSONException ex) {
                    JOptionPane.showMessageDialog(null,"Error parsing Acquisition Layout as JSONObject.");
                    Logger.getLogger(LayoutManagerDlg.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null,"Error saving Acquisition Layout as JSONObject.");
                    Logger.getLogger(LayoutManagerDlg.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    fw.close();
                }        
            } catch (IOException ex) {
                Logger.getLogger(LayoutManagerDlg.class.getName()).log(Level.SEVERE, null, ex);
            }
            platenameField.setText(startUpLayout.getName());
            lastFileLocation=startUpLayout.getFile().getParent();
            fileLocationLabel.setText(lastFileLocation);
            startUpLayout.setModified(false);
        }       
    }
    
    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        saveLayout();
    }//GEN-LAST:event_saveButtonActionPerformed

    private void openButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openButtonActionPerformed
        //check if modified and if yes, open save dialog
        if (startUpLayout.isModified()) {
            int result=JOptionPane.showConfirmDialog(this, "Save current layout configuration?", "Custom Layout Manager", JOptionPane.YES_NO_CANCEL_OPTION);
            switch (result) {
                case JOptionPane.CANCEL_OPTION: {
                    return;
                }
                case JOptionPane.NO_OPTION: {
                    break;
                }
                case JOptionPane.YES_OPTION: {
                    saveLayout();
                    break;
                }
            }
        }
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(fileLocationLabel.getText()));
        int returnVal = fc.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!Utils.getExtension(f).toLowerCase().equals(".txt")) {
                JOptionPane.showMessageDialog(null, "Layout files have to be in txt format.\nLayout has not been loaded.", "", JOptionPane.ERROR_MESSAGE);
                return;
            }
            IAcqLayout layout=AcqBasicLayout.loadLayout(fc.getSelectedFile());
            if (layout==null) {
                JOptionPane.showMessageDialog(this, "Layout file '"+f.getName()+"' could not be found or read!");
                return;
            }
            if ((layout instanceof AcqWellplateLayout)) {
                JOptionPane.showMessageDialog(this, "Layout file '"+f.getName()+"' does not encode a custom layout");
                return;
            }
            startUpLayout=layout;
            setCustomLayout(startUpLayout);
        }
    }//GEN-LAST:event_openButtonActionPerformed

    private void newButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newButtonActionPerformed
        //check if modified and if yes, open save dialog
        if (startUpLayout.isModified()) {
            int result=JOptionPane.showConfirmDialog(this, "Save current layout configuration?", "Custom Layout Manager", JOptionPane.YES_NO_CANCEL_OPTION);
            switch (result) {
                case JOptionPane.CANCEL_OPTION: {
                    return;
                }
                case JOptionPane.NO_OPTION: {
                    break;
                }
                case JOptionPane.YES_OPTION: {
                    saveLayout();
                    break;
                }
            }
        }
        startUpLayout=new AcqCustomLayout();
 //       if (lastFileLocation.equals("")) {
 //           lastFileLocation=Prefs.getHomeDir();
 //       }
        startUpLayout.setFile(new File(lastFileLocation, startUpLayout.getName()));
        setCustomLayout(startUpLayout);
    }//GEN-LAST:event_newButtonActionPerformed

    private void removeAreaButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeAreaButtonActionPerformed
        List<BasicArea> areas = startUpLayout.getAreaArray();
        if (areas.size() > 0) {
            int[] rows = areaTable.getSelectedRows();
            if (rows.length > 0) {
                AreaTableModel atm = (AreaTableModel) areaTable.getModel();
                for (int i=0; i<rows.length; i++) {
                    rows[i]=areaTable.convertRowIndexToModel(rows[i]);
                }
                atm.removeRows(rows);
                startUpLayout.setModified(true);
            }
        }
    }//GEN-LAST:event_removeAreaButtonActionPerformed

    private void moveUpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveUpButtonActionPerformed
        AreaTableModel atm = (AreaTableModel) areaTable.getModel();
        if (atm != null) {
            int[] selRows = areaTable.getSelectedRows();
            if (selRows.length > 0 & selRows[0] > 0) {
                int newSelRowInView=selRows[0]-1;
                //
                int firstMinusOneIndex=areaTable.convertRowIndexToModel(selRows[0]-1);
                //convert view row indices retrived from table to corresponding indices in model
                for (int i=0; i<selRows.length; i++) {
                    selRows[i]=areaTable.convertRowIndexToModel(selRows[i]);
                }
                atm.rowUp(selRows, firstMinusOneIndex);
                areaTable.setRowSelectionInterval(newSelRowInView, newSelRowInView + selRows.length - 1);
                startUpLayout.setModified(true);
            }
        }
    }//GEN-LAST:event_moveUpButtonActionPerformed

    private void moveDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveDownButtonActionPerformed
        AreaTableModel atm = (AreaTableModel) areaTable.getModel();
        if (atm != null) {
            int[] selRows = areaTable.getSelectedRows();
            if (selRows.length > 0 & selRows[selRows.length - 1] < areaTable.getRowCount()) {
                int newSelRowInView=selRows[0]+1;
                //
                int lastPlusOneIndex=areaTable.convertRowIndexToModel(selRows[selRows.length-1]+1);
                //convert view row indices retrived from table to corresponding indices in model
                for (int i=0; i<selRows.length; i++) {
                    selRows[i]=areaTable.convertRowIndexToModel(selRows[i]);
                }
                atm.rowDown(selRows, lastPlusOneIndex);
                areaTable.setRowSelectionInterval(newSelRowInView, newSelRowInView + selRows.length - 1);
                startUpLayout.setModified(true);
            }
        }
    }//GEN-LAST:event_moveDownButtonActionPerformed

    private void editAreaButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editAreaButtonActionPerformed
        if (areaTable.getSelectedRowCount() != 1) {
            JOptionPane.showMessageDialog(this, "Select single area.");
            return;
        }
        int index=areaTable.getSelectedRow();
        AreaTableModel model=(AreaTableModel)areaTable.getModel();
        int rowInModel=areaTable.convertRowIndexToModel(index);
        BasicArea area=model.getRowData(rowInModel);
        try {
            BasicArea areaCopy=BasicArea.createFromJSONObject(area.toJSONObject());
            //pass copy, so area can remain unchanged if user clicks 'Cancel'
            BasicArea modArea=areaCopy.showConfigDialog(new Rectangle2D.Double(0,0,startUpLayout.getWidth(), startUpLayout.getLength()));
            if (modArea!=null) {// user clicked OK in dialog
                model.setRowData(rowInModel,modArea);
                startUpLayout.setModified(true);
            }            
        } catch (JSONException ex) {
            Logger.getLogger(LayoutManagerDlg.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(LayoutManagerDlg.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(LayoutManagerDlg.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(LayoutManagerDlg.class.getName()).log(Level.SEVERE, null, ex);
        }
            
    }//GEN-LAST:event_editAreaButtonActionPerformed

    private void newAreaButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newAreaButtonActionPerformed
        if (availableAreaClasses==null || availableAreaClasses.size() == 0) {
            JOptionPane.showMessageDialog(this, "No area class definitions found. New areas cannot be created.");
            return;
        }
        String[] classDescr=new String[availableAreaClasses.size()];
        Iterator it=availableAreaClasses.keySet().iterator();
        int i=0;
        while (it.hasNext()) {
            classDescr[i]=(String)it.next();
            i++;
        }
        Arrays.sort(classDescr);
        String selOption;
        if ("".equals(lastAreaType)) {
            selOption=classDescr[0];
        } else
            selOption=lastAreaType;
        String selectedType=(String)JOptionPane.showInputDialog(this, "Area type:", "Select Area Type", JOptionPane.OK_CANCEL_OPTION, null, classDescr, selOption);
        if (selectedType!=null) {
            Class clazz;
            try {
                clazz = Class.forName(availableAreaClasses.get(selectedType));
                BasicArea newArea=((BasicArea)clazz.newInstance());
                
//                double width=((Number)plateWidthField.getValue()).doubleValue();
//                double length=((Number)plateLengthField.getValue()).doubleValue();
//                newArea=newArea.showConfigDialog(new Rectangle2D.Double(0,0,width, length));
                newArea=newArea.showConfigDialog(new Rectangle2D.Double(0,0,startUpLayout.getWidth(), startUpLayout.getLength()));
                if (newArea!=null) {
                    AreaTableModel model=(AreaTableModel)areaTable.getModel();
                    newArea.setId(startUpLayout.createUniqueAreaId());
                    model.addRow(newArea);
                    lastAreaType=selectedType;
                    startUpLayout.setModified(true);
                }
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(LayoutManagerDlg.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(LayoutManagerDlg.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(LayoutManagerDlg.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_newAreaButtonActionPerformed

    private void plateWidthFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_plateWidthFieldPropertyChange
        if (plateWidthField.getValue()!=null && startUpLayout!=null) {
            double newValue=((Number)plateWidthField.getValue()).doubleValue() * 1000;
            if (newValue != startUpLayout.getWidth()) {
                startUpLayout.setWidth(newValue);
            }    
        }
    }//GEN-LAST:event_plateWidthFieldPropertyChange

    private void plateLengthFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_plateLengthFieldPropertyChange
        if (plateLengthField.getValue()!=null && startUpLayout!=null) {
            double newValue=((Number)plateLengthField.getValue()).doubleValue() * 1000;
            if (newValue != startUpLayout.getLength()) {
                startUpLayout.setLength(newValue);
            }    
        }
    }//GEN-LAST:event_plateLengthFieldPropertyChange

    private void plateHeightFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_plateHeightFieldPropertyChange
        if (plateHeightField.getValue()!=null && startUpLayout!=null) {
            double newValue=((Number)plateHeightField.getValue()).doubleValue() * 1000;
            if (newValue != startUpLayout.getHeight()) {
                startUpLayout.setHeight(newValue);
            }    
        }
    }//GEN-LAST:event_plateHeightFieldPropertyChange

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable areaTable;
    private javax.swing.JFormattedTextField bottomThicknessField;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton closeButton;
    private javax.swing.JButton editAreaButton;
    private javax.swing.JLabel fileLocationLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JButton moveDownButton;
    private javax.swing.JButton moveUpButton;
    private javax.swing.JButton newAreaButton;
    private javax.swing.JButton newButton;
    private javax.swing.JButton openButton;
    private javax.swing.JFormattedTextField plateHeightField;
    private javax.swing.JFormattedTextField plateLengthField;
    private javax.swing.JFormattedTextField plateWidthField;
    private javax.swing.JFormattedTextField platenameField;
    private javax.swing.JButton removeAreaButton;
    private javax.swing.JButton saveButton;
    private javax.swing.JFormattedTextField wellDepthField;
    private javax.swing.JFormattedTextField wellDistanceField;
    // End of variables declaration//GEN-END:variables
}
