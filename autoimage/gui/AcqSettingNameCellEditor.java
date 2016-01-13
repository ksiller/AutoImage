package autoimage.gui;

import autoimage.Utils;
import autoimage.api.AcqSetting;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;

/**
 *
 * @author Karsten Siller
 */
public class AcqSettingNameCellEditor extends DefaultCellEditor implements FocusListener {
    
    private List<AcqSetting> acqSettings;
    private String cachedValue;
    private final InputVerifier verifier = new InputVerifier() {

        @Override
        public boolean verify(JComponent input) {
            String name = ((JTextField) input).getText();
            int count=0;
            for (AcqSetting setting:acqSettings) {
                if (!name.equals(cachedValue) && name.equals(setting.getName())) {
                    //only count if name (new) != value (old)
                    count++;
                }
            }
            return count<1;
        }

        @Override
        public boolean shouldYieldFocus(JComponent input) {
            return verify(input);
        }    
    };
    
    public AcqSettingNameCellEditor(List<AcqSetting> settings) {
        super(new JTextField());
        acqSettings=settings;
        JTextField textField=getComponent();
        textField.addFocusListener(this);
        textField.setInputVerifier(verifier);
        textField.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
    }
    
    public void resetAcqSettingName(JTextField textField) {
        String uniqueName=Utils.createUniqueAcqSettingName(acqSettings, textField.getText());
        JOptionPane.showMessageDialog(null, "The name for the acquisition setting is already in use.\n"
                + "Suggested name: "+uniqueName);
        textField.setText(uniqueName);
    }
    
    @Override
    public boolean stopCellEditing() {   
        JTextField textField=getComponent();
        if (!verifier.shouldYieldFocus(textField)) {
            resetAcqSettingName(textField);
        }
        cachedValue=textField.getText();
        return super.stopCellEditing();
    }
    
    @Override
    public Component getTableCellEditorComponent(JTable table,
        Object value, boolean isSelected, int row, int column) {
        cachedValue=value.toString();
        return super.getTableCellEditorComponent(
                table, value, isSelected, row, column);
    }

    @Override
    public JTextField getComponent() {
        return (JTextField)super.getComponent();
    }
    
    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (!stopCellEditing()) {
            getComponent().requestFocusInWindow();
        }
    }
}
