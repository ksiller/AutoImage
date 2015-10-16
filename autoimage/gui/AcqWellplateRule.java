package autoimage.gui;

import autoimage.IRuleListener;
import autoimage.gui.AcqCustomRule;
import autoimage.api.SampleArea;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 *
 * @author Karsten Siller
 */
public class AcqWellplateRule extends AcqCustomRule {

    protected double offset;//from edge in um
    protected double wellDistance;//between wells in um
    protected int items;//i.e. columns or rows in plate
    
    private String[] colAlphabet = SampleArea.PLATE_COL_ALPHABET;
    private String[] rowAlphabet = SampleArea.PLATE_ROW_ALPHABET;
    
    public AcqWellplateRule(int orientation) {
        super(orientation);
    }
    
    public void setColumnAlphabet(String[] colAlpha) {
        colAlphabet=colAlpha;
    }
    
    public void setRowAlphabet(String[] rowAlpha) {
        rowAlphabet=rowAlpha;
    }
    
    public void setOffset(double offs) {
        offset=offs;
    }
    
    public void setWellDistance(double dist) {
        wellDistance=dist;
    }
    
    public void setNoOfItems(int i) {
        items=i;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g.create();
        Rectangle clipRect = g2d.getClipBounds();  
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(clipRect.x, clipRect.y, clipRect.width, clipRect.height);
        g.setFont(new Font("Arial", Font.PLAIN,14));
        
        int tickLength = 5;
        double minTickPix = wellDistance * zoom * scale;
        double pixPos=offset * zoom * scale;
        int maxLabelWidth=0;
        if (orientation == SwingConstants.HORIZONTAL) {
            for (int i = 0; i < items; i++) {
                int coord=(int)Math.round(pixPos);
                g.drawLine(coord, size-1, coord, size-tickLength-1);
                String label=colAlphabet.length>i ? colAlphabet[i] : "...";
                g.drawString(label, coord-(g.getFontMetrics().stringWidth(label)/2), size-tickLength-3);
                pixPos+=minTickPix;
            }
        } else {
            //VERTICAL
            for (int i = 0; i < items; i++) {
                int coord=(int)Math.round(pixPos);
                g.drawLine(size-1, coord, size-tickLength-1, coord);
                String label=rowAlphabet.length>i ? rowAlphabet[i] : "...";
                g.drawString(label, size-tickLength-3-g.getFontMetrics().stringWidth(label), coord+6);
                maxLabelWidth=(int)Math.max(maxLabelWidth, tickLength+5+g.getFontMetrics().stringWidth(label));
                pixPos+=minTickPix;
            }
        }
        if (orientation==SwingConstants.VERTICAL && maxLabelWidth > this.getWidth()) {
            final int size=maxLabelWidth; 
            final Component source=this;
            SwingUtilities.invokeLater(new Runnable () {
                @Override
                public void run() {
                    for (IRuleListener l:listeners) {
                        l.resizeRequested(size, source);
                    }
                }
            });
        }
        g2d.dispose();
        
    }
}
