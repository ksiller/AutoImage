package autoimage.gui;

//import autoimage.AcqCustomLayout;
import autoimage.IMergeAreaListener;
import autoimage.api.IAcqLayout;
import autoimage.api.SampleArea;
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
    private IAcqLayout acqLayout;
    private final List<IMergeAreaListener> listeners;
//    private ExecutorService listenerExecutor;

   //implementation of TableModelListener
    @Override
    public void tableChanged(TableModelEvent tme) {
        synchronized (listeners) {
            for (IMergeAreaListener l:listeners)
                l.mergeAreaSelectionChanged(((AreaTableModel)areaTable.getModel()).getAreaList());
        }    
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
      private List<SampleArea> areas;

      public void setData(List<SampleArea> areas) {
         this.areas = areas;
      }

      public List<SampleArea> getAreaList() {
         return areas;
      }

      public SampleArea getArea(int row) {
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
         SampleArea a;
         if (areas!=null & rowIndex < areas.size()) {
             a = areas.get(rowIndex);
             switch (colIndex) {
                 case 0: return a.getName();
                 case 1: return a.getTopLeftX();
                 case 2: return a.getTopLeftY();
                 case 3: return a.getBounds().getWidth();
                 case 4: return a.getBounds().getHeight();
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
         SampleArea a;
         if (areas!=null & rowIndex < areas.size()) {
             a = areas.get(rowIndex);
             switch (colIndex) {
                 case 0: a.setName((String)value);
                 case 1: a.setTopLeftX((Double)value);
                 case 2: a.setTopLeftY((Double)value);
//                 case 3: a.setWidth((Double)value);
//                 case 4: a.setHeight((Double)value);
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
          SampleArea a=(SampleArea)value;
          areas.add(a);
          fireTableRowsInserted(getRowCount()-1,getRowCount()-1);
      }
      
   }

   public MergeAreasDlg(AcqFrame aFrame, IAcqLayout al, ScriptInterface gui) {
      super(aFrame);
      listeners=new ArrayList<IMergeAreaListener>();
//      listenerExecutor = Executors.newFixedThreadPool(1);
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
      model.setData(new ArrayList<SampleArea>());
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
                List<SampleArea> selectedAreas=((AreaTableModel)areaTable.getModel()).getAreaList();
                final List<SampleArea> mergingAreas=new ArrayList<SampleArea>(selectedAreas.size());
                for (SampleArea area:selectedAreas)
                    mergingAreas.add(area);
                synchronized (listeners) {
	            for (final IMergeAreaListener l : listeners) {
                                l.mergeAreas(mergingAreas);
                    }
                }    
                removeAllAreas();
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

    public void setAcquisitionLayout(IAcqLayout acqL) {
        acqLayout=acqL;
    }    
   
    public void addAreas(List<SampleArea> additionalAreaList) {
        if (additionalAreaList!=null) {
            AreaTableModel atm = (AreaTableModel)areaTable.getModel();
            List<SampleArea> areas=atm.getAreaList();
            for (SampleArea additionalArea : additionalAreaList) {
                boolean unique=true;
                for (SampleArea area : areas) {
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
            List<SampleArea> layoutAreas=acqLayout.getAreaArray();
            AreaTableModel atm=(AreaTableModel)areaTable.getModel();
            for (int i=rows[rows.length-1]; i>=rows[0]; i--) {
                SampleArea a=atm.getArea(i);
                acqLayout.getAreaById(a.getId()).setSelectedForMerge(false);
                atm.removeRow(i);
            }    
        }
    }
   

}