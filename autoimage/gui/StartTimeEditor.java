package autoimage.gui;

import autoimage.api.AcqSetting;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author Karsten
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
    
    //AbstractCellEditor
    @Override
    public Object getCellEditorValue() {
        return startTime;
    }
 
    //AbstractCellEditor
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

