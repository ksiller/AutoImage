/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.gui.utils;

import autoimage.data.acquisition.MMConfig;
import autoimage.gui.models.MMConfigTableModel;
import ij.IJ;
import java.awt.Component;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTable;

/**
 *
 * @author Karsten
 */
public class PresetEditor extends DefaultCellEditor {

    public PresetEditor(JComboBox comboBox) {
        super(comboBox);
    }
    
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        MMConfigTableModel model = (MMConfigTableModel)table.getModel();
        MMConfig config = model.getRowData(row);
        ((JComboBox)this.editorComponent).setModel(new DefaultComboBoxModel(config.getAvailablePresets().toArray()));
        IJ.log("TABLECELL EDITOR:"+model.getAcqSetting().getName()+", "+config.getName()+", "+config.getSelectedPreset());
        ((JComboBox)this.editorComponent).setSelectedItem(config.getSelectedPreset());
        return super.getTableCellEditorComponent(table, value, isSelected, row, column); 
    }
}
