package autoimage;

import ij.IJ;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.text.DecimalFormat;
import javax.swing.JComponent;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 *
 * @author Karsten Siller
 */
public class AcqRule extends JComponent implements Scrollable {
        
    private final static DecimalFormat df = new DecimalFormat("#.###");
    private final static int SIZE = 30;
    private static double MAX_ZOOM = 64;
    private final static int[] increments={10,20,50,100,200,500,1000,2000,5000,10000,20000,50000,100000,200000,500000,1000000};
    
    private int orientation;
    private double scale;
    private double zoom=1;
    private double totalUnits;//in um
    
    public AcqRule (int orientation) {
        this.orientation=orientation;
    }
    
    public void setTotalUnits(double units) {
        totalUnits=units;
    }

    public void setZoom(double z) {
        if (zoom!=z) {
            zoom=z;
            revalidate();
            repaint();
        }
    }
    
    public void setScale(double s) {
        if (scale!=s) {
            scale=s;
            revalidate();
            repaint();
        }
    }
    
    public double getZoom() {
        return zoom;
    }
   
    
    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g.create();
        Rectangle clipRect = g2d.getClipBounds();  
        IJ.log((orientation==SwingConstants.HORIZONTAL ? "horizontal" : "vertical") +" clipping: "+clipRect.toString());
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(clipRect.x, clipRect.y, clipRect.width, clipRect.height);
        g.setFont(new Font("Arial", Font.PLAIN,10));
        int tickLength = 0;
        String text = null;
        
        int min;
        int max;
        double end;
        if (orientation == SwingConstants.HORIZONTAL) {
            end=this.getWidth();
            min=(int)Math.floor(clipRect.width / zoom / scale / 10);
            max=(int)Math.ceil(clipRect.width / zoom / scale / 5);
        } else {
            //VERTICAL
            end=this.getHeight();
            min=(int)Math.floor(clipRect.height / zoom / scale / 10);
            max=(int)Math.ceil(clipRect.height / zoom / scale / 5);
        }
        double half=(max+min)/2;
        int deltalabelTick=increments[increments.length-1];
        for (int i:increments) {
            if (Math.abs(i-half) < Math.abs(deltalabelTick-half)) {
                deltalabelTick=i;
            } else {
                break;
            }
        }
        double minTickUnit;//um
        switch (Integer.parseInt(Integer.toString(deltalabelTick).substring(0, 1))) {
            case 1: {minTickUnit=deltalabelTick/10; break;}
            case 2: {minTickUnit=deltalabelTick/2; break;}
            case 5: {minTickUnit=deltalabelTick/5; break;}
            default: {minTickUnit=1000;}
        }
        double labelSpacing=deltalabelTick/minTickUnit;
        double minTickPix = minTickUnit * zoom * scale;
        IJ.log("final MinTickPix:"+Double.toString(minTickPix));            
        IJ.log(orientation==SwingConstants.HORIZONTAL ? "Horizontal Increment: "+Double.toString(minTickPix) : "Vertical Increment: "+Double.toString(minTickPix));
        int run=0;
        for (double i = 0; i < end; i += minTickPix) {
            double value=Math.round(i/zoom/scale);
            if (run % Math.round(labelSpacing) == 0)  {
                tickLength = 10;
                text = df.format(value/1000);
            } else {
                tickLength = 5;
                text = null;
            }
            if (orientation == SwingConstants.HORIZONTAL) {
                g.drawLine((int)Math.round(i), SIZE-1, (int)Math.round(i), SIZE-tickLength-1);
                if (text != null)
                    g.drawString(text, value==0 ? 1 : (int)Math.round(i-g.getFontMetrics().stringWidth(text)/2), SIZE-tickLength-3);
            } else {
                //VERTICAL
                g.drawLine(SIZE-1, (int)Math.round(i), SIZE-tickLength-1, (int)Math.round(i));
                if (text != null)
                    g.drawString(text, SIZE-tickLength-3-g.getFontMetrics().stringWidth(text), value==0 ? (int)Math.round(i)+11 : (int)Math.round(i)+4);
            }
            run++;
        }
        g2d.dispose();
    }
    
    public void setPreferredSize(int size) {
        switch (orientation) {
            case SwingConstants.HORIZONTAL: {
                this.setPreferredSize(new Dimension(size,SIZE));
                break;
            }
            case SwingConstants.VERTICAL: {
                this.setPreferredSize(new Dimension(SIZE,size));
                break;
            }
        }
        revalidate();
        repaint();
    }
    
    public double getScale() {
        return scale;
    }
    
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension((int)(getPreferredSize().width),(int)(getPreferredSize().height));
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return Math.round(visibleRect.width/10);
        } else {
            return Math.round(visibleRect.height/10);
        }    
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return (int) Math.round(visibleRect.width*0.9);
        } else {
            return (int) Math.round(visibleRect.height*0.9);
        }    
    }    

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return (zoom == 1);
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return (zoom == 1);
    }

}
