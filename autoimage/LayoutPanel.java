/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import ij.IJ;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 *
 * @author Karsten
 */


class LayoutPanel extends JPanel implements Scrollable {

    private Cursor zoomCursor;
    private static final Cursor moveCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
    private static final Cursor normCursor = new Cursor(Cursor.DEFAULT_CURSOR);
    
    private static int LAYOUT_MAX_DIM = 20000;
    private static int MAX_ZOOM = 32;
    private static Color COLOR_BORDER = Color.WHITE;
    private static Color COLOR_UNSELECTED_AREA = Color.GRAY;
    private static Color COLOR_ACQUIRING_AREA = Color.YELLOW;
    private static Color COLOR_SELECTED_AREA = new Color(51,115,188);
    private static Color COLOR_FOV = Color.CYAN;
    private static Color COLOR_TILE_GRID = Color.RED;
    private static Color COLOR_LANDMARK = Color.GREEN;
    private static Color COLOR_SEL_LANDMARK = Color.MAGENTA;
    private static Color COLOR_LAYOUT_BACKGR = Color.BLACK;
    private static Stroke SOLID_STROKE = new BasicStroke(1.0f);
    private static Stroke DASHED_STROKE = new BasicStroke(1.0f, // line width
      /* cap style */BasicStroke.CAP_BUTT,
      /* join style, miter limit */BasicStroke.JOIN_BEVEL, 1.0f,
      /* the dash pattern */new float[] { 8.0f, 8.0f},0.0f);
    private AcquisitionLayout acqLayout;
    private AcqSetting cAcqSetting;//reference to current AcqSetting
    private Point anchorMousePos;
    private Point prevMousePos;
    private double currentStagePos_X;
    private double currentStagePos_Y;
    private boolean stageXYPosUpdated;
    private boolean showLandmark;
    private boolean showZProfile;
    private boolean landmarkFound;
    private Rectangle2D.Double mergeAreasBounds; 

    
    private double borderDim; //in physDim;
    private double physToPixelRatio;

    private double zoom;
    private double scale;

    public LayoutPanel() {
        this(null,null);
    }
    
    public LayoutPanel(AcquisitionLayout al, AcqSetting as) {
        super(true);
//        IJ.log("LayoutPanel.constructor");
        setOpaque(true);
        
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image cursorImage = toolkit.getImage(getClass().getClassLoader().getResource("autoimage/resources/ZoomCursor.png"));
        Point cursorHotSpot = new Point(11,11);
        zoomCursor = toolkit.createCustomCursor(cursorImage, cursorHotSpot, "Zoom Cursor");
        showLandmark=true;
        showZProfile=false;
        landmarkFound=false;
        mergeAreasBounds=null;
        cAcqSetting=as;
        
        setAcquisitionLayout(al, 0);
      
        this.addComponentListener(new ComponentHandler());
    }

    public AcquisitionLayout getAcqLayout() {
        return acqLayout;
    }
    
    public void setMergeAreasBounds(Rectangle2D.Double rect) {
        mergeAreasBounds=rect;
        revalidate();
        repaint();
    }
    
    public void showLandmark(boolean b) {
        showLandmark=b;
    }
    
    public void setLandmarkFound(boolean b) {
        landmarkFound=b;
    }
    
    public boolean getLandmarkFound() {
        return landmarkFound;
    }

    public void setShowZProfile(boolean b) {
        showZProfile=b;
    }
    
    public boolean getShowZProfile() {
        return showZProfile;
    }
    
    public void setAcquisitionLayout(AcquisitionLayout al, double borderD) {
 //       IJ.log("LayoutPanel.setAcquisitionLayout: start");
        this.acqLayout=al;
        borderDim=borderD;
        zoom=1;
        scale=1;
//        IJ.log("LayoutPanel.setAcquisitionLayout: before calculatePhysToPixelRatio");
        calculatePhysToPixelRatio();
//        IJ.log("LayoutPanel.setAcquisitionLayout: after calculatePhysToPixelRatio");
//        calculateScale(0,0);
        setPreferredSize(new Dimension(getPreferredLayoutWidth(),getPreferredLayoutHeight()));
        revalidate();
        repaint();
//        IJ.log("LayoutPanel.setAcquisitionLayout: end");
    }

    
    public double convertPixToPhysCoord(int pix) {
        return (pix/physToPixelRatio/scale/zoom)-borderDim;
    }
    
    public Point2D.Double convertPixToPhysCoord(Point p) {
        return new Point2D.Double(convertPixToPhysCoord(p.x),convertPixToPhysCoord(p.y));
    }
    
    public int getBorderDimPix() {
        int bdPix;
        if ((borderDim>0) && (physToPixelRatio!=1))
            bdPix=(int)Math.round(borderDim*physToPixelRatio);
        else
            bdPix=10;
        return bdPix;
     }
    
    public void calculateScale(int w, int h) {
        // w and h of JScrollPane
/*
        JViewport vp=((JScrollPane)getParent()).getViewport();
        Rectangle vpRect=vp.getViewRect();
        w=vpRect.width;
        h=vpRect.height;
*/
        
        Dimension d=new Dimension(getPreferredLayoutWidth(),getPreferredLayoutHeight());
        if (zoom==1) {
            scale=Math.min(((double)w-2)/d.width,((double)h-2)/d.height);
            revalidate();
            repaint();
        }
//        IJ.log("LayoutPanel.calculateScale: w="+Integer.toString(w)+", h="+Integer.toString(h)+", "+Double.toString(d.getWidth())+", "+Double.toString(d.getHeight()));
    }
    
    public void calculatePhysToPixelRatio() {
        if (acqLayout!=null) {
//            IJ.log("LayoutPanel.calculatePhyToPixelRatio: acqLayout.getWidth()="+Double.toString(acqLayout.getWidth()));
//            IJ.log("LayoutPanel.calculatePhyToPixelRatio: acqLayout.getHeight()="+Double.toString(acqLayout.getHeight()));
            physToPixelRatio=Math.min((double)LAYOUT_MAX_DIM/acqLayout.getWidth(),(double)LAYOUT_MAX_DIM/acqLayout.getHeight());
        } else
            physToPixelRatio=1;
//        IJ.log("LayoutPanel.calculatePhysToPixelRatio: physToPixelRatio="+Double.toString(physToPixelRatio));
    }

    
    private int getPreferredLayoutWidth() {
        if (acqLayout!=null) {
            //return (int)Math.round(2*getBorderDimPix()+(int)Math.ceil(acqLayout.getWidth()*physToPixelRatio)*zoom);
            return 2*getBorderDimPix()+(int)Math.ceil(acqLayout.getWidth()*physToPixelRatio);
        } else
            return LAYOUT_MAX_DIM;
    }
    
    
    private int getPreferredLayoutHeight() {
        if (acqLayout!=null) {
            //return (int)Math.round(2*getBorderDimPix()+(int)Math.ceil(acqLayout.getHeight()*physToPixelRatio)*zoom);
            return 2*getBorderDimPix()+(int)Math.ceil(acqLayout.getHeight()*physToPixelRatio);
        } else
            return Math.round(LAYOUT_MAX_DIM/2);
    }
    
    @Override
    public Dimension getPreferredSize() {    
    
        int w = (int)(getPreferredLayoutWidth()*scale*zoom);  
        int h = (int)(getPreferredLayoutHeight()*scale*zoom);  
        return new Dimension(w, h);
     
    //    return super.getPreferredSize();
    }
    
    @Override
    public Dimension getPreferredScrollableViewportSize() {
//        IJ.log("LayoutPanel.getPreferredScrollableViewportSize()");
//        return getPreferredSize();
        return new Dimension((int)Math.round(getPreferredLayoutWidth()*scale),(int)Math.round(getPreferredLayoutHeight()*scale));
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
        if (zoom == 1)
            return true;
        else
            return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
       if (zoom == 1)
            return true;
        else
            return false;
    }
    
    private class ComponentHandler extends ComponentAdapter {

        @Override
        public void componentResized(ComponentEvent e) {
//            IJ.log("LayoutPanel private class ComponentHandler.componentResized");
//            calculatePhysToPixelRatio();
//            revalidate();
        }
    }
    
    
    public double zoomIn() {
        if (zoom < MAX_ZOOM)
            zoom=zoom*2;
        setSize(new Dimension((int)(getPreferredLayoutWidth()*zoom),(int)(getPreferredLayoutHeight()*zoom)));
//        setSize(new Dimension((int)(getPreferredLayoutWidth()),(int)(getPreferredLayoutHeight())));
        revalidate();
        repaint();
        return zoom;
    }
    
    public double zoomOut(int w, int h) {
        zoom=zoom/2;
        if (zoom < 1) {
            zoom=1;
//            calculateScale(getParent().getWidth(),getParent().getHeight());
            calculateScale(w,h);
        } else {   
            setSize(new Dimension((int)(getPreferredLayoutWidth()*zoom),(int)(getPreferredLayoutHeight()*zoom)));
            revalidate();
            repaint();
        }
        return zoom;
    }
    
    public double getZoom() {
        return zoom;
    }
   
    
    public void drawTileGridForAllSelectedAreas(Graphics2D g2d) {
//        IJ.log("LayoutPanel.drawTileGridForAllAreas: start");
        int totalTiles=0;
        int tileNum=0;
        if ((acqLayout!=null) && (acqLayout.getAreaArray().size()>0)) {
            List areas=acqLayout.getAreaArray();
            if (cAcqSetting!=null) {
                double tileWidth=cAcqSetting.getTileWidth();
                double tileHeight=cAcqSetting.getTileHeight();
                TilingSetting tSetting=cAcqSetting.getTilingSetting();
                for (int i=0; i<areas.size(); i++) {
                    Area a=acqLayout.getAreaArray().get(i);
                    if (a.isSelectedForAcq())
                        a.drawTiles(g2d, getBorderDimPix(), physToPixelRatio, tileWidth, tileHeight,tSetting);
                }
            }      
        }
//        IJ.log("LayoutPanel.drawTileGridForAllAreas: end");
    }

    
    private void drawAllAreas(Graphics2D g2d) {
//        IJ.log("LayoutPanel.drawAllAreas");
        if (acqLayout!=null) {
            List areas=acqLayout.getAreaArray();
            if (cAcqSetting!=null) {
                double tileWidth=cAcqSetting.getTileWidth();
                double tileHeight=cAcqSetting.getTileHeight();
                TilingSetting tSetting=cAcqSetting.getTilingSetting();
                for (int i=0; i<areas.size(); i++) {
                    Area a=acqLayout.getAreaArray().get(i);
//                    a.draw(g2d, getBorderDimPix(), physToPixelRatio, tileWidth, tileHeight,tSetting,true);
                    a.drawArea(g2d, getBorderDimPix(), physToPixelRatio);
                }
                drawTileGridForAllSelectedAreas(g2d);
            }    
        }
    }
    
    private void drawLandmarks(Graphics g) {
//        IJ.log("drawLandmark");
        if ((acqLayout!=null) && (acqLayout.getLandmarks()!=null)) {
            Graphics2D g2d = (Graphics2D) g;
            ArrayList<RefArea> landmarks=acqLayout.getLandmarks();
            int bdPix=getBorderDimPix();
            for (int i=0; i<landmarks.size(); i++) {
                RefArea lm=landmarks.get(i);
                int x = bdPix + (int) Math.round(lm.getLayoutCoordOrigX()*physToPixelRatio);
                int y = bdPix + (int) Math.round(lm.getLayoutCoordOrigY()*physToPixelRatio);
                int w = (int) Math.round(lm.getPhysWidth()*physToPixelRatio);
                int h = (int) Math.round(lm.getPhysHeight()*physToPixelRatio);
//                IJ.log("drawLandmark "+Integer.toString(i)+": "+Integer.toString(x)+" "+Integer.toString(y)+" "+Integer.toString(w)+" "+Integer.toString(h));
                if (lm.isSelected())
                    g2d.setColor(COLOR_SEL_LANDMARK);
                else
                    g2d.setColor(COLOR_LANDMARK);       
                if (i==0) {
                    g2d.setStroke(SOLID_STROKE);
                } else {
                    g2d.setStroke(DASHED_STROKE);
                } 
                g2d.drawRect(x,y,w,h);
            }
        }
    }
    
    @Override
    public boolean isOptimizedDrawingEnabled() {
        return true;
    }
    
    public void setAnchorMousePos(Point mp) {
        anchorMousePos=mp;
        prevMousePos=mp;
    }
    
    private Rectangle createRectangle(Point start, Point end) {
        int x=Math.min(start.x, end.x);
        int y=Math.min(start.y, end.y);
        int w=Math.abs(start.x-end.x);
        int h=Math.abs(start.y-end.y);
        
        Rectangle r = new Rectangle(x,y,w,h);
        return r;
    }
    
    public void updateSelRect(Point mp, boolean redraw) {
        Graphics2D g = (Graphics2D) getGraphics();
        Rectangle r=createRectangle(anchorMousePos, prevMousePos);
        g.setXORMode(getBackground());
        g.drawRect(r.x, r.y, r.width, r.height);        
        if (redraw) {
            r=createRectangle(anchorMousePos, mp);
            g.setXORMode(getBackground());
            g.drawRect(r.x, r.y, r.width, r.height);
            g.setPaintMode();  
        } else
            g.drawLine(anchorMousePos.x,anchorMousePos.y,anchorMousePos.x,anchorMousePos.y);
        prevMousePos = mp;
    }
    
    public void drawFovAtCurrentStagePos(Graphics2D g2d) {
//        IJ.log("LayoutPanel.drawFovAtCurrentStagePos: start");
      //  if (stageXYPosUpdated) {
            int bdPix=getBorderDimPix();
            RefArea lm=acqLayout.getLandmark(0);
            double tileWidth=cAcqSetting.getTileWidth();
            double tileHeight=cAcqSetting.getTileHeight();
            int x=bdPix+(int)Math.floor(physToPixelRatio*(lm.convertStagePosToLayoutCoord_X(currentStagePos_X)-tileWidth/2));
            int y=bdPix+(int)Math.floor(physToPixelRatio*(lm.convertStagePosToLayoutCoord_Y(currentStagePos_Y)-tileHeight/2));
            int w=(int)Math.ceil(tileWidth*physToPixelRatio);
            int h=(int)Math.ceil(tileHeight*physToPixelRatio);
            g2d.setColor(COLOR_FOV);
            Composite oldComposite=g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.5f));
            g2d.fillRect(x,y,w,h);
            g2d.setComposite(oldComposite);
            g2d.setStroke(SOLID_STROKE);
            g2d.drawRect(x,y,w,h);
//            IJ.log("LayoutPanel.drawFovAtCurrentStagePos: "+Integer.toString(x)+", "+Integer.toString(y)+", "+Integer.toString(w)+", "+Integer.toString(h));
       // }
    }
    
    public void setCurrentXYStagePos(double xPos, double yPos) {
        stageXYPosUpdated=currentStagePos_X!=xPos || currentStagePos_Y!=yPos;
        currentStagePos_X=xPos;
        currentStagePos_Y=yPos;
    }
    
    private double calcDist(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));
    }
    
    private Point2D.Double[] calcGradientVector() {
        Vec3d n=acqLayout.getNormalVector();
        if (acqLayout.getLandmarks()!=null & acqLayout.getLandmarks().size()>1 & calcDist(0,0,n.x,n.y)>0) {
            //gradient center positioned in center of layout
            double lmx=acqLayout.getWidth()/2;
            double lmy=acqLayout.getHeight()/2;
            double radius=calcDist(0,0,lmx,lmy);
            
            //gradient center positioned in center of first landmark
            //acqLayout.getLandmark(0).getLayoutCoordX();
            //acqLayout.getLandmark(0).getLayoutCoordY();
            //double d1=calcDist(lmx,lmy, 0, 0);
            //double d2=calcDist(lmx,lmy, acqLayout.getWidth(), 0);
            //double d3=calcDist(lmx,lmy, acqLayout.getWidth(), acqLayout.getHeight());
            //double d4=calcDist(lmx,lmy, 0, acqLayout.getHeight());
            //double radius=Math.max(Math.max(Math.max(d1, d2),d3),d4);
            Point2D.Double[] p=new Point2D.Double[2];
            p[0] = new Point2D.Double(physToPixelRatio*(lmx+n.x*radius), physToPixelRatio*(lmy+n.y*radius));
            p[1] = new Point2D.Double(physToPixelRatio*(lmx-n.x*radius), physToPixelRatio*(lmy-n.y*radius));
//            IJ.log("LayoutPanel.calGradientVector: radius: "+Double.toString(radius));
//            IJ.log("LayoutPanel.calGradientVector: p[0]: ("+Double.toString(p[0].x)+"/"+p[0].y+"), p[1]: ("+Double.toString(p[1].x)+"/"+p[1].y+")");
            return p;
        } else
            return null;
    }
    
    public void setAcqSetting(AcqSetting setting, boolean redraw) {
        cAcqSetting=setting;
        if (redraw)
            repaint();
    }
    
    public void enableShowZProfile(boolean b) {
        showZProfile=b;
    }
    
    @Override
    public void paintComponent(final Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Composite oldComposite=g2d.getComposite();
        Color oldColor=g2d.getColor();
        Stroke oldStroke=g2d.getStroke();
        
        Rectangle r=getBounds();
        g2d.setColor(COLOR_BORDER);
        g2d.fillRect(r.x, r.y, r.width, r.height); 
        g2d.scale(scale*zoom, scale*zoom);
        
        int w=getPreferredLayoutWidth();
        int h=getPreferredLayoutHeight();
        int bdPix=getBorderDimPix();
        
        Point2D.Double[] p=calcGradientVector();
        if (showZProfile & p!=null) {
            Point2D start = new Point2D.Double(bdPix+p[0].x,bdPix+p[0].y);
            Point2D end = new Point2D.Double(bdPix+p[1].x,bdPix+p[1].y);
            float[] dist = {0.0f, 0.5f, 1.0f};
            Color[] colors = {Color.RED, Color.BLACK, Color.BLUE};
            LinearGradientPaint lg =new LinearGradientPaint(start, end, dist, colors);            
            g2d.setPaint(lg);
            g2d.fill (new Rectangle2D.Double(bdPix, bdPix, w-2*bdPix, h-2*bdPix));
        } else {
            g2d.setColor(COLOR_LAYOUT_BACKGR);
            g2d.fillRect(bdPix, bdPix, w-2*bdPix, h-2*bdPix); //getWidth(), getHeight());
        }
        
        if ((acqLayout!=null) && !acqLayout.isEmpty()) {
            drawAllAreas(g2d);
            if (showLandmark)
                drawLandmarks(g);
            if (mergeAreasBounds!=null) {
                g2d.setColor(COLOR_SELECTED_AREA);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.3f));
                g2d.fillRect((int)Math.round(bdPix+mergeAreasBounds.x*physToPixelRatio),
                        (int)Math.round(bdPix+mergeAreasBounds.y*physToPixelRatio),
                        (int)Math.round(mergeAreasBounds.width*physToPixelRatio),
                        (int)Math.round(mergeAreasBounds.height*physToPixelRatio));
                g2d.setComposite(oldComposite);
                g2d.setStroke(SOLID_STROKE);
                g2d.draw(new Rectangle2D.Double(bdPix+mergeAreasBounds.x*physToPixelRatio,
                        bdPix+mergeAreasBounds.y*physToPixelRatio,
                        mergeAreasBounds.width*physToPixelRatio,
                        mergeAreasBounds.height*physToPixelRatio));
            }
                
        } else {
            g2d.setColor(Color.GRAY);
            g2d.setFont(new Font("Calibri", Font.BOLD, 72));
            String s = "No Layout File Loaded";
            Rectangle2D bounds=g2d.getFontMetrics().getStringBounds(s, g2d);
            double scaledSize=72*(w/(3*bounds.getWidth()));
            g2d.setFont(new Font("Calibri", Font.BOLD, (int)Math.ceil(scaledSize)));
            bounds=g2d.getFontMetrics().getStringBounds(s, g2d);
            g2d.drawString(s, Math.round((w-bounds.getWidth())/2), Math.round((h-bounds.getHeight())/2));
        }    
        if (landmarkFound)
            drawFovAtCurrentStagePos(g2d);
        g2d.setComposite(oldComposite);
        g2d.setColor(oldColor);
        g2d.setStroke(oldStroke);
    }
}

