package autoimage.gui;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;


public class NumberFilter extends DocumentFilter {

    private final char[] LEGAL_CHAR;
    private int digits;
    private boolean negative;

    public NumberFilter (){
        this(1,false);
    }
    
    public NumberFilter (int l, boolean neg){
        super();
        negative=neg;
        digits=l;
        if (negative) {
            LEGAL_CHAR = new char[]{'-','0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};            
        } else {
            LEGAL_CHAR = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};   
        }           
    }
    
    @Override
    public void insertString (DocumentFilter.FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
        if ((digits<=0) 
                || (fb.getDocument().getLength() + text.length() <= digits) 
                || (fb.getDocument().getText(0,1).equals("-") && fb.getDocument().getLength() + text.length() <= digits+1)) {
            StringBuffer buffer = new StringBuffer(text);
            for (int i = buffer.length() - 1; i >= 0; i--) {
                char ch = buffer.charAt(i);
                if ((new String(LEGAL_CHAR).indexOf(ch))==-1)
                    buffer.deleteCharAt(i);
                if (ch=='-' && (i!=0 || offset!=0))
                    buffer.deleteCharAt(i);
            }
            super.insertString(fb, offset, buffer.toString(), attr);
        }
    }

    @Override
    public void replace (DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attr) throws BadLocationException {
        if (length > 0) 
            fb.remove(offset, length);
        insertString(fb, offset, text, attr);
    }

}
