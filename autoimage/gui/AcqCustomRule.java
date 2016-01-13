package autoimage.gui;

//import ij.IJ;
import autoimage.IRuleListener;
import autoimage.IRuleNotifier;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 *
 * @author Karsten Siller
 */
public class AcqCustomRule extends JComponent implements Scrollable, IRuleNotifier {
        
    private final static DecimalFormat df = new DecimalFormat("#.###");
    private static double MAX_ZOOM = 64;
    private static int DEFAULT_SIZE = 30;
    private final static int[] increments={10,20,50,100,200,500,1000,2000,5000,10000,20000,50000,100000,200000,500000,1000000};
    
    protected int orientation;
    protected int size;
    protected double scale;
    protected double zoom=1;
    protected List<IRuleListener> listeners;
    
    private double totalUnits;//in um

    public AcqCustomRule (int orientation) {
        this.orientation=orientation;
        listeners=new ArrayList<IRuleListener>();
        size=DEFAULT_SIZE;
    }
    
    public void resetToDefaultSize() {
        size=DEFAULT_SIZE;
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
    
    public double getZoom() {
        return zoom;
    }
   
    public void setScale(double s) {
        if (scale!=s) {
            scale=s;
            revalidate();
            repaint();
        }
    }
    
    public double getScale() {
        return scale;
    }
    
    public void setScaleAndZoom(double s, double z) {
        scale=s;
        zoom=z;
        revalidate();
        repaint();
    }
    
    
    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g.create();
        Rectangle clipRect = g2d.getClipBounds();  
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(clipRect.x, clipRect.y, clipRect.width, clipRect.height);
        g.setFont(new Font("Arial", Font.PLAIN,10));
        int tickLength = 0;
        
        int minSpacing;
        int maxSpacing;
        double end;
        if (orientation == SwingConstants.HORIZONTAL) {
            end=this.getWidth();
            minSpacing=(int)Math.floor(3 * g.getFontMetrics().stringWidth(df.format(totalUnits/1000)) / zoom / scale);
            maxSpacing=(int)Math.ceil(6 * g.getFontMetrics().stringWidth(df.format(totalUnits/1000)) / zoom / scale);
        } else {
            //VERTICAL
            end=this.getHeight();
            minSpacing=(int)Math.floor(4 * g.getFontMetrics().getHeight() / zoom / scale);
            maxSpacing=(int)Math.ceil(8 * g.getFontMetrics().getHeight() / zoom / scale);
        }
        double half=(maxSpacing+minSpacing)/2;
        int labelUnit=increments[increments.length-1];
        for (int i:increments) {
            if (Math.abs(i-half) < Math.abs(labelUnit-half)) {
                labelUnit=i;
            } else {
                break;
            }
        }
        double minTickUnit;//um
        switch (Integer.parseInt(Integer.toString(labelUnit).substring(0, 1))) {
            case 1: {minTickUnit=labelUnit/10; break;}
            case 2: {minTickUnit=labelUnit/2; break;}
            case 5: {minTickUnit=labelUnit/5; break;}
            default: {minTickUnit=1000;}
        }
        double labelSpacing=labelUnit/minTickUnit;
        double minTickPix = minTickUnit * zoom * scale;
        int run=0;
        int maxLabelWidth=0;
        for (double i = 0; i < end; i += minTickPix) {
            double value=Math.round(i/zoom/scale);
            if (value>totalUnits) {
                break;
            }
            String label;
            if (run % labelSpacing == 0)  {
                tickLength = 10;
                label = df.format(value/1000);
            } else {
                tickLength = 5;
                label = null;
            }
            int coord=(int)Math.round(i);
            if (orientation == SwingConstants.HORIZONTAL) {
                g.drawLine(coord, size-1, coord, size-tickLength-1);
                if (label != null)
                    g.drawString(label, value==0 ? 1 : coord-(g.getFontMetrics().stringWidth(label)/2), size-tickLength-3);
            } else {
                //VERTICAL
                g.drawLine(size-1, coord, size-tickLength-1, coord);
                if (label != null) {
                    maxLabelWidth=(int)Math.max(maxLabelWidth, tickLength+5+g.getFontMetrics().stringWidth(label));
                    g.drawString(label, size-tickLength-3-g.getFontMetrics().stringWidth(label), value==0 ? coord+11 : coord+4);
                }
            }    
            run++;
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
    
    public void setPreferredSize(int newSize) {
        switch (orientation) {
            case SwingConstants.HORIZONTAL: {
                this.setPreferredSize(new Dimension(newSize,size));
                break;
            }
            case SwingConstants.VERTICAL: {
                this.setPreferredSize(new Dimension(size, newSize));
                break;
            }
        }
        revalidate();
//        repaint();
    }
    
    @Override
    public void setPreferredSize(Dimension dim) {
        super.setPreferredSize(dim);
        if (orientation==SwingConstants.HORIZONTAL) {
            size=dim.height;
        } else {
            size=dim.width;         
        }
//        repaint();
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

    @Override
    public void addListener(IRuleListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    @Override
    public void removeListener(IRuleListener l) {
        if (listeners.contains(l)) {
            listeners.remove(l);
        }
    }


}
