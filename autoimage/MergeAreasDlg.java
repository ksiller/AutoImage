/**
 *
 * @author Karsten, modified from PositionListDlg.java authored by Nenad Amodaj and Nico Stuurman
 */


package autoimage;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

import mmcorej.CMMCore;
import org.micromanager.utils.MMDialog;

import ij.IJ;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.GUIUtils;
import org.micromanager.utils.ReportingUtils;


public class MergeAreasDlg extends MMDialog implements MouseListener, ChangeListener, TableModelListener {

   private JTable areaTable;
   private SpringLayout springLayout;
   private AcqFrame acqFrame;
   private AcquisitionLayout acqLayout;
   private CMMCore core_;
   private ScriptInterface gui_;

   public JButton markButtonRef;

    @Override
    public void tableChanged(TableModelEvent tme) {
        acqFrame.setMergeAreasBounds(calcMergeAreasBounds());
    }


   private class AreaTableModel extends AbstractTableModel {
      private static final long serialVersionUID = 1L;
      public final String[] COLUMN_NAMES = new String[] {
            "Area",
            "Origin X",
            "Origin Y",
            "Width",
            "Height"
      };
      private List<Area> areas;

      public void setData(List<Area> areas) {
         this.areas = areas;
      }

      public List<Area> getAreaList() {
         return areas;
      }

      public Area getArea(int row) {
          return areas.get(row);
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
      public String getColumnName(int columnIndex) {
         return COLUMN_NAMES[columnIndex];
      }
      @Override
      public Object getValueAt(int rowIndex, int colIndex) {
         Area a;
         if (areas!=null & rowIndex < areas.size()) {
             a = areas.get(rowIndex);
             switch (colIndex) {
                 case 0: return a.getName();
                 case 1: return a.getTopLeftX();
                 case 2: return a.getTopLeftY();
                 case 3: return a.getWidth();
                 case 4: return a.getHeight();
             }
             return false;
         } else
             return null;
      }

      @Override
      public boolean isCellEditable(int rowIndex, int columnIndex) {
         return false;
      }
      
      @Override
      public void setValueAt(Object value, int rowIndex, int colIndex) {
         Area a;
         if (areas!=null & rowIndex < areas.size()) {
             a = areas.get(rowIndex);
             switch (colIndex) {
                 case 0: a.setName((String)value);
                 case 1: a.setTopLeftX((Double)value);
                 case 2: a.setTopLeftY((Double)value);
                 case 3: a.setWidth((Double)value);
                 case 4: a.setHeight((Double)value);
             }
             fireTableCellUpdated(rowIndex,colIndex);
         }
      }
      
      public void removeRow(int index) {
          areas.remove(index);
          fireTableRowsDeleted(index,index);
      }

      public void removeAll() {
          int rows=getRowCount();
          for (int i=areas.size()-1; i>=0;i--) {
              areas.remove(i);
          }
          fireTableDataChanged();
      }

      public void addRow(Object value) {
          Area a=(Area)value;
          areas.add(a);
          fireTableRowsInserted(getRowCount()-1,getRowCount()-1);
      }
   }


   /**
    * Create the dialog
    */
   public MergeAreasDlg(AcqFrame aFrame, AcquisitionLayout al, CMMCore core, ScriptInterface gui) {
      super();
      addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent arg0) {
            savePosition();
//            IJ.showMessage("window closing");
            removeAllAreas();
         }
      });
      acqFrame=aFrame;
      acqLayout=al;
      core_ = core;
      gui_ = gui;
      setTitle("Merge Areas");
      springLayout = new SpringLayout();
      getContentPane().setLayout(springLayout);
      setBounds(100, 100, 362, 595);

      /*Preferences root = Preferences.userNodeForPackage(this.getClass());
      prefs_ = root.node(root.absolutePath() + "/XYPositionListDlg");
      setPrefsNode(prefs_);

      Rectangle r = getBounds();
      GUIUtils.recallPosition(this);
*/
      setBackground(gui_.getBackgroundColor());
      gui_.addMMBackgroundListener(this);

      final JScrollPane scrollPane = new JScrollPane();
      getContentPane().add(scrollPane);

      areaTable = new JTable();// {
      areaTable.setFont(new Font("Arial", Font.PLAIN, 10));
      AreaTableModel model = new AreaTableModel();
      model.setData(new ArrayList<Area>());
      areaTable.setModel(model);
      model.addTableModelListener(this);
      CellEditor cellEditor_ = new CellEditor();
      areaTable.setDefaultEditor(Object.class, cellEditor_);
      areaTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      scrollPane.setViewportView(areaTable);
      areaTable.addMouseListener(this);

      JButton removeButton = new JButton();
      removeButton.setFont(new Font("Arial", Font.PLAIN, 10));
      removeButton.setMaximumSize(new Dimension(0,0));
      removeButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent arg0) {
             removeAreas();
         }
      });
      removeButton.setText("Remove");
      removeButton.setToolTipText("Remove Area from list");
      getContentPane().add(removeButton);
      
      int northConstraint = 17;
      final int buttonHeight = 27;      
      springLayout.putConstraint(SpringLayout.NORTH, removeButton, northConstraint, SpringLayout.NORTH, getContentPane());     
      springLayout.putConstraint(SpringLayout.SOUTH, removeButton, northConstraint+=buttonHeight, SpringLayout.NORTH, getContentPane());
      springLayout.putConstraint(SpringLayout.EAST, removeButton, -9, SpringLayout.EAST, getContentPane());
      springLayout.putConstraint(SpringLayout.WEST, removeButton, -105, SpringLayout.EAST, getContentPane());

      springLayout.putConstraint(SpringLayout.NORTH, scrollPane, +9, SpringLayout.NORTH, getContentPane());
      springLayout.putConstraint(SpringLayout.SOUTH, scrollPane, -9, SpringLayout.SOUTH, getContentPane());
      springLayout.putConstraint(SpringLayout.EAST, scrollPane, -9, SpringLayout.WEST, removeButton);
      springLayout.putConstraint(SpringLayout.WEST, scrollPane, 10, SpringLayout.WEST, getContentPane());

      final JButton cancelButton = new JButton();
      cancelButton.setFont(new Font("Arial", Font.PLAIN, 10));
      cancelButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent arg0) {
            removeAllAreas();
            savePosition();
            dispose();
         }
      });
      cancelButton.setText("Cancel");
      cancelButton.setToolTipText("Exit without merging Areas");
      getContentPane().add(cancelButton);
      springLayout.putConstraint(SpringLayout.NORTH, cancelButton, northConstraint, SpringLayout.NORTH, getContentPane());
      springLayout.putConstraint(SpringLayout.SOUTH, cancelButton, northConstraint+buttonHeight, SpringLayout.NORTH, getContentPane());
      springLayout.putConstraint(SpringLayout.EAST, cancelButton, 0, SpringLayout.EAST, removeButton);
      springLayout.putConstraint(SpringLayout.WEST, cancelButton, 0, SpringLayout.WEST, removeButton);
      
        final JButton mergeButton = new JButton();
        mergeButton.setFont(new Font("Arial", Font.PLAIN, 10));
        mergeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                mergeAreas();
                savePosition();
                dispose();
            }
        });
        mergeButton.setText("Merge");
        mergeButton.setToolTipText("Merge all Areas in list");
        getContentPane().add(mergeButton);
        springLayout.putConstraint(SpringLayout.NORTH, mergeButton, northConstraint+=buttonHeight, SpringLayout.NORTH, getContentPane());
        springLayout.putConstraint(SpringLayout.SOUTH, mergeButton, northConstraint+=buttonHeight, SpringLayout.NORTH, getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, mergeButton, 0, SpringLayout.EAST, removeButton);
        springLayout.putConstraint(SpringLayout.WEST, mergeButton, 0, SpringLayout.WEST, removeButton);      
      
        setAlwaysOnTop(true);
    }
   
    public void setAcquisitionLayout(AcquisitionLayout acqL) {
        acqLayout=acqL;
    }    
   
    public void addAreas(List<Area> al) {
        if (al!=null) {
            AreaTableModel atm = (AreaTableModel)areaTable.getModel();
            List<Area> areas=atm.getAreaList();
            for (int i=0; i<al.size(); i++){
               boolean unique=true;
               for (int j=0; j<areas.size(); j++) {
                  if (al.get(i).getId()==areas.get(j).getId()) {
                      unique=false;
                      break;
                  }
               }
               if (unique) {
                   atm.addRow(al.get(i));
               }    
            }
        }
    }
   
   
    public void removeAllAreas() {
        AreaTableModel atm=(AreaTableModel)areaTable.getModel();
        atm.removeAll();
    }
   
    public void removeAreas() {
        int[] rows=areaTable.getSelectedRows();
        if (rows.length>0) {
            List<Area> layoutAreas=acqLayout.getAreaArray();
            AreaTableModel atm=(AreaTableModel)areaTable.getModel();
            for (int i=rows[rows.length-1]; i>=rows[0]; i--) {
                Area a=atm.getArea(i);
                acqLayout.getAreaById(a.getId()).setSelectedForMerge(false);
                atm.removeRow(i);
            }    
        }
    }
   
    
   @Override
   public void stateChanged(ChangeEvent e) {
        try {
            GUIUtils.invokeLater(new Runnable() {
                @Override
                public void run() {
                    areaTable.revalidate();
                }
            });
        } catch (Exception ex) {
            ReportingUtils.logError(ex);
        }
    }


   public void updateAreaData() {
      AreaTableModel ptm = (AreaTableModel)areaTable.getModel();
      ptm.fireTableDataChanged();
   }
   

   private String createNewAreaName() {
       String s="";
       boolean exists=true;
       List<Area> areas=acqLayout.getAreaArray();
       int n=1;
       while (exists) {
            s="Merged_Area_"+Integer.toString(n);
            exists=false;
            for (int i=0; i<areas.size(); i++) {
                if (s.equals(areas.get(i).getName())) {
                    exists=true;
                }
            }
            n++;
       }
       return s;
   }   
   
   
   private Rectangle2D.Double calcMergeAreasBounds() { 
       AreaTableModel atm=(AreaTableModel)areaTable.getModel();
       List<Area> mergingAreas=atm.getAreaList();
       Rectangle2D.Double r;
       if (mergingAreas!=null & mergingAreas.size()>1) {
            double minX=(Double)atm.getValueAt(0,1);
            double maxX=minX+(Double)atm.getValueAt(0,3);
            double minY=(Double)atm.getValueAt(0,2);
            double maxY=minY+(Double)atm.getValueAt(0,4);
            double z=0;
            for (int row=1;row<atm.getRowCount();row++) {
                if ((Double)atm.getValueAt(row,1)<minX)
                    minX=(Double)atm.getValueAt(row,1);
                if ((Double)atm.getValueAt(row,2)<minY)
                    minY=(Double)atm.getValueAt(row,2);
                if ((Double)atm.getValueAt(row,1)+(Double)atm.getValueAt(row,3)>maxX)
                    maxX=(Double)atm.getValueAt(row,1)+(Double)atm.getValueAt(row,3);
                if ((Double)atm.getValueAt(row,2)+(Double)atm.getValueAt(row,4)>maxY)
                    maxY=(Double)atm.getValueAt(row,2)+(Double)atm.getValueAt(row,4);    
            }
           r=new Rectangle2D.Double(minX,minY,maxX-minX,maxY-minY);
       } else
           r=null;
       return r;
   }
   
   
   private void mergeAreas() {
       Rectangle2D.Double r=calcMergeAreasBounds();
       if (r!=null) {
           List<Area> layoutAreas=acqLayout.getAreaArray();
            Area mergedArea=new RectArea(createNewAreaName(),acqLayout.createUniqueAreaId(),r.x, r.y, 0, r.width, r.height, false, "");
            AreaTableModel atm=(AreaTableModel)areaTable.getModel();
            layoutAreas.removeAll(atm.getAreaList());
            layoutAreas.add(mergedArea);
            removeAllAreas();
            acqLayout.setModified(true);
       } else 
           IJ.showMessage("empty");
   } 
   

   /**
    * Editor component for the position list table
    */
   public class CellEditor extends AbstractCellEditor implements TableCellEditor, FocusListener {
      private static final long serialVersionUID = 3L;
      // This is the component that will handle editing of the cell's value
      JTextField text_ = new JTextField();
      int editingCol_;

      public CellEditor() {
         super();
         text_.addFocusListener(this);
      }

      @Override
      public void focusLost(FocusEvent e) {
         fireEditingStopped();
      }

      @Override
      public void focusGained(FocusEvent e) {
 
      }

      // This method is called when a cell value is edited by the user.
      @Override
      public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int rowIndex, int colIndex) {

        editingCol_ = colIndex;

         // Configure the component with the specified value
         if (colIndex == 0) {
            text_.setText((String)value);
            return text_;
         }

         return null;
      }
                                                                             
      // This method is called when editing is completed.
      // It must return the new value to be stored in the cell. 
      @Override
      public Object getCellEditorValue() {
         if (editingCol_ == 0) {
               return text_.getText();
         }
         return null;
      }
   }

   private List<Area> getAreaList() {
      AreaTableModel ptm = (AreaTableModel)areaTable.getModel();
      return ptm.getAreaList();

   }

   private void handleError(Exception e) {
      ReportingUtils.showError(e);
   }


   /*
    * Implementation of MouseListener
    * Sole purpose is to be able to unselect rows in the positionlist table
    */
   @Override
   public void mousePressed (MouseEvent e) {}
   @Override
   public void mouseReleased (MouseEvent e) {}
   @Override
   public void mouseEntered (MouseEvent e) {}
   @Override
   public void mouseExited (MouseEvent e) {}
   /*
    * This event is fired after the table sets its selection
    * Remember where was clicked previously so as to allow for toggling the selected row
    */
   private static int lastRowClicked_;
   @Override
   public void mouseClicked (MouseEvent e) {
      java.awt.Point p = e.getPoint();
      int rowIndex = areaTable.rowAtPoint(p);
      if (rowIndex >= 0) {
         if (rowIndex == areaTable.getSelectedRow() && rowIndex == lastRowClicked_) {
               areaTable.clearSelection();
               lastRowClicked_ = -1;
         } else
            lastRowClicked_ = rowIndex;
      }
   }
}