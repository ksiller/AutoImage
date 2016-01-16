package autoimage.gui.views;

import autoimage.gui.dialogs.TimeChooserDlg;
import autoimage.data.acquisition.AcqSetting;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author Karsten Siller
 */
public class StartTimeEditor extends AbstractCellEditor
                         implements TableCellEditor, ActionListener {
    AcqSetting.ScheduledTime startTime;
    JButton button;//component in table
    TimeChooserDlg timeChooser;
    protected static final String EDIT = "edit";
 
    public StartTimeEditor() {
        button = new JButton();
        button.setActionCommand(EDIT);//when cell is pressed in table
        button.addActionListener(this);
        button.setBorderPainted(false);
 
        timeChooser = new TimeChooserDlg(null,true);
        timeChooser.addOkCancelListener(this);
    }
 
    @Override
    public boolean isCellEditable(EventObject evt) {
        if (evt instanceof MouseEvent) { 
            return ((MouseEvent)evt).getClickCount() >= 2;
        }
        return true;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (EDIT.equals(e.getActionCommand())) {
            //bring up TimeChooserDlg.
            timeChooser.setStartTime(new AcqSetting.ScheduledTime(startTime.type,startTime.startTimeMS));
            timeChooser.setVisible(true);

            //Make the renderer reappear.
            fireEditingStopped();
        } else if (e.getActionCommand().equals("Ok")) {
            startTime = timeChooser.getStartTime();
        }
        if (e.getActionCommand().equals("Cancel")) {
            //ignore changes
        }
    }
    
    @Override
    public Object getCellEditorValue() {
        return startTime;
    }
 
    @Override
    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        startTime = (AcqSetting.ScheduledTime)value;
        return button;
    }
}    

