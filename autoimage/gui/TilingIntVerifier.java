package autoimage.gui;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 *
 * @author Karsten Siller
 */
public class TilingIntVerifier extends InputVerifier {

    int minVal;
    int maxVal;

    public TilingIntVerifier(int min, int max) {
        super();
        minVal = min;
        maxVal = max;
    }

    @Override
    public boolean verify(JComponent jc) {
        JTextField field = (JTextField) jc;
        boolean passed = false;
        try {
            int n = Integer.parseInt(field.getText());
            passed = (n >= minVal && n <= maxVal);
        } catch (NumberFormatException nfe) {
        }
        return passed;
    }

    @Override
    public boolean shouldYieldFocus(JComponent input) {
        boolean inputOk = verify(input);
        if (!inputOk) {
            JOptionPane.showMessageDialog(null, "Number is out of range (" + Integer.toString(minVal) + "-" + Integer.toString(maxVal) + ")");
            ((JTextField) input).selectAll();
        }
        return inputOk;
    }
    
}
