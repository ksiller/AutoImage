/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage;

import ij.IJ;
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
//    JDialog dialog;
    protected static final String EDIT = "edit";
 
    public StartTimeEditor() {
        IJ.log("StartTimeEditor.constructor");
        button = new JButton();
        button.setActionCommand(EDIT);//when cell is pressed in table
        button.addActionListener(this);
/*
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IJ.log("StartTimeEditor.actionPerformed");
                if (EDIT.equals(e.getActionCommand())) {
                    //The user has clicked the cell, so
                    //bring up the dialog.
                    IJ.log("before setStartTime");    
                    timeChooser.setStartTime(new AcqSetting.ScheduledTime(startTime.type,startTime.startTimeMS));
                    IJ.log("after setStartTime");    
                    timeChooser.setVisible(true);
                    IJ.log("after setVisible");    

                    //Make the renderer reappear.
                    fireEditingStopped();
                }
            }
        });
            */
        button.setBorderPainted(false);
 
        timeChooser = new TimeChooserDlg(null,true);
        timeChooser.addOkCancelListener(this);
    }
 
 
    @Override
    public void actionPerformed(ActionEvent e) {
        if (EDIT.equals(e.getActionCommand())) {
            //The user has clicked the cell, so
            //bring up the dialog.
            timeChooser.setStartTime(new AcqSetting.ScheduledTime(startTime.type,startTime.startTimeMS));
            timeChooser.setVisible(true);

            //Make the renderer reappear.
            fireEditingStopped();
        } else if (e.getActionCommand().equals("Ok")) {
            startTime = timeChooser.getStartTime();
        }
        if (e.getActionCommand().equals("Cancel")) {
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

