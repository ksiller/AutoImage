package autoimage;

import autoimage.area.Area;
import ij.IJ;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import org.micromanager.api.ScriptInterface;



public class MergeAreasDlg extends javax.swing.JDialog implements TableModelListener {

    private JTable areaTable;
//    private AcqFrame acqFrame;
    private AcqLayout acqLayout;
    private final List<IMergeAreaListener> listeners;
    private ExecutorService listenerExecutor;

   //implementation of TableModelListener
    @Override
    public void tableChanged(TableModelEvent tme) {
//        acqFrame.setMergeAreasBounds(calcMergeAreasBounds());
//        IJ.showMessage("tableChanged: "+Boolean.toString(SwingUtilities.isEventDispatchThread()));
        synchronized (listeners) {
/*            for (final IMergeAreaListener l : listeners) {
               listenerExecutor.submit(new Runnable (){
                    @Override
                    public void run() {*/
                        for (IMergeAreaListener l:listeners)
                            l.mergeAreaSelectionChanged(((AreaTableModel)areaTable.getModel()).getAreaList());
/*                    }
                });
            }*/
        }    
    }

/*    
   @Override
   public void stateChanged(ChangeEvent e) {
        try {
            IJ.showMessage("stateChanged");
            GUIUtils.invokeLater(new Runnable() {
                @Override
                public void run() {
                    areaTable.revalidate();
                }
            });
        } catch (InterruptedException ex) {
            ReportingUtils.logError(ex);
        } catch (InvocationTargetException ex) {
            ReportingUtils.logError(ex);
       }
    }
*/

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

   public MergeAreasDlg(AcqFrame aFrame, AcqLayout al, ScriptInterface gui) {
      super(aFrame);
      listeners=new ArrayList<IMergeAreaListener>();
      listenerExecutor = Executors.newFixedThreadPool(1);
      addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent arg0) {
            removeAllAreas();
         }
      });
      acqLayout=al;
      setTitle("Merge Areas");
      SpringLayout springLayout = new SpringLayout();
      getContentPane().setLayout(springLayout);
      setBounds(100, 100, 362, 595);
      setMinimumSize(new Dimension(362,595));

//      setBackground(gui.getBackgroundColor());
//      gui.addMMBackgroundListener(this);

      final JScrollPane scrollPane = new JScrollPane();
      getContentPane().add(scrollPane);

      areaTable = new JTable();// {
      areaTable.getTableHeader().setReorderingAllowed(false);
      areaTable.setFont(new Font("Arial", Font.PLAIN, 10));
      AreaTableModel model = new AreaTableModel();
      model.setData(new ArrayList<Area>());
      areaTable.setModel(model);
      model.addTableModelListener(this);
      areaTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      scrollPane.setViewportView(areaTable);

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
      removeButton.setToolTipText("Remove selected Area(s) from list");
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
         @Override
         public void actionPerformed(ActionEvent arg0) {
            removeAllAreas();
//            savePosition();
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
            @Override
            public void actionPerformed(ActionEvent e) {
//                mergeAreas();
                List<Area> selectedAreas=((AreaTableModel)areaTable.getModel()).getAreaList();
                final List<Area> mergingAreas=new ArrayList<Area>(selectedAreas.size());
                for (Area area:selectedAreas)
                    mergingAreas.add(area);
                synchronized (listeners) {
	            for (final IMergeAreaListener l : listeners) {
/*                        listenerExecutor.submit(new Runnable (){
                            @Override
                            public void run() {*/
                                l.mergeAreas(mergingAreas);
/*                            }
                        });*/
                    }
                }    
                removeAllAreas();
//                savePosition();
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
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
   
    synchronized public void addListener(IMergeAreaListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
        IJ.log(getClass().getName()+": "+listeners.size()+" listeners");
    }
        
    synchronized public void removeListener(IMergeAreaListener listener) {
        if (listeners != null)
            listeners.remove(listener);
        IJ.log(getClass().getName()+": "+listeners.size()+" listeners");
    }

    public void setAcquisitionLayout(AcqLayout acqL) {
        acqLayout=acqL;
    }    
   
    public void addAreas(List<Area> additionalAreaList) {
        if (additionalAreaList!=null) {
            AreaTableModel atm = (AreaTableModel)areaTable.getModel();
            List<Area> areas=atm.getAreaList();
            for (Area additionalArea : additionalAreaList) {
                boolean unique=true;
                for (Area area : areas) {
                    if (additionalArea.getId() == area.getId()) {
                        unique=false;
                        break;
                    }
                }
                if (unique) {
                    atm.addRow(additionalArea);
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
   
/*
   private String createNewAreaName() {
       String s="";
       boolean exists=true;
       List<Area> areas=acqLayout.getAreaArray();
       int n=1;
       while (exists) {
            s="Merged_Area_"+Integer.toString(n);
            exists=false;
           for (Area area : areas) {
               if (s.equals(area.getName())) {
                   exists=true;
               }
           }
            n++;
       }
       return s;
   }   
*/
    
/*   
   private Rectangle2D.Double calcMergeAreasBoundss() { 
       AreaTableModel atm=(AreaTableModel)areaTable.getModel();
       List<Area> mergingAreas=atm.getAreaList();
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
           return new Rectangle2D.Double(minX,minY,maxX-minX,maxY-minY);
       } else
           return null;
   }
   
   
   private void mergeAreas() {
       Rectangle2D.Double r=calcMergeAreasBounds();
       if (r!=null) {
           List<Area> layoutAreas=acqLayout.getAreaArray();
            Area mergedArea=new RectArea(createNewAreaName(),acqLayout.createUniqueAreaId(),r.x, r.y, 0, r.width, r.height, false, "");
            AreaTableModel atm=(AreaTableModel)areaTable.getModel();
            layoutAreas.removeAll(atm.getAreaList());
            layoutAreas.add(mergedArea);
            removeAllAreas();//in MergeAreasDlg
            acqLayout.setModified(true);
       } else 
           IJ.log(getClass().getName()+": mergeAreaBounds == null");
   } 
*/   

}