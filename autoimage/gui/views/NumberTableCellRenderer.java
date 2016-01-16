/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.gui.views;

import java.awt.Component;
import java.text.DecimalFormat;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Karsten
 */
    public class NumberTableCellRenderer extends DefaultTableCellRenderer {
        
        DecimalFormat df;
        
        public NumberTableCellRenderer(DecimalFormat format) {
            super();
            df = format;
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable jTable, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(jTable, value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel && value instanceof Number) {
                JLabel label = (JLabel) c;
                label.setHorizontalAlignment(JLabel.RIGHT);
                String text = df.format((Number) value);
                label.setText(text);
            }
            return c;
        }
    }
