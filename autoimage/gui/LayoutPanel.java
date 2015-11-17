package autoimage.gui;

import autoimage.api.AcqSetting;
import autoimage.FieldOfView;
import autoimage.Tile;
import autoimage.api.IStageMonitorListener;
import autoimage.api.RefArea;
import autoimage.api.TilingSetting;
import autoimage.Vec3d;
import autoimage.api.IAcqLayout;
import autoimage.api.BasicArea;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 *
 * @author Karsten Siller
 */


class LayoutPanel extends JPanel implements Scrollable, IStageMonitorListener {

    //private Cursor zoomCursor;
    //private static final Cursor moveCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
    //private static final Cursor normCursor = new Cursor(Cursor.DEFAULT_CURSOR);
    
    private static int LAYOUT_MAX_DIM = 20000;//in um
    private static double MAX_ZOOM = 64;
    
    private static Color COLOR_PANEL_BACKGR = Color.WHITE;
    private static Color COLOR_UNSELECTED_AREA = Color.GRAY;
    private static Color COLOR_ACQUIRING_AREA = new Color(180,180,0);
    private static Color COLOR_SELECTED_AREA = new Color(51,115,188);
    private static Color COLOR_AREA_BORDER = Color.LIGHT_GRAY;
    private static Color COLOR_SELECTED_AREA_BORDER = new Color(220,220,0);
    private static Color COLOR_MERGE_AREA_BORDER = Color.RED; //new Color(51,115,188);
    private static Color COLOR_AREA_LABEL = Color.WHITE;
    private static Color COLOR_FOV = Color.CYAN;
    private static Color COLOR_TILE_GRID = Color.RED;
    private static Color COLOR_LANDMARK = Color.GREEN;
    private static Color COLOR_SELECTED_LANDMARK = Color.MAGENTA;
    private static Color COLOR_LAYOUT_BACKGR = Color.BLACK;
    private static Color COLOR_STAGE_GRID = Color.DARK_GRAY;
    private static Color COLOR_Z_TILT_LOW = Color.RED;
    private static Color COLOR_Z_TILT_HIGH = Color.BLUE;

    private static final int LABEL_FONT_SIZE = 16;
    private static final float STROKE_WIDTH = 1.5f;
    private static Stroke SOLID_STROKE = new BasicStroke(1.0f);
    private static Stroke DASHED_STROKE = new BasicStroke(1.0f, // line width
      /* cap style */BasicStroke.CAP_BUTT,
      /* join style, miter limit */BasicStroke.JOIN_BEVEL, 1.0f,
      /* dash pattern */new float[] { 8.0f, 8.0f},0.0f);
    
    private IAcqLayout acqLayout;
    private AcqSetting cAcqSetting;//reference to current AcqSetting
    private Point anchorMousePos;
    private Point prevMousePos;
    private double currentStagePos_X;
    private double currentStagePos_Y;
    private boolean stageXYPosUpdated;
    private boolean showZProfile;
    private boolean showAreaLabels;
    private Rectangle2D mergeAreasBounds; 

    private double zoom;
    private double scale;

    public LayoutPanel() {
        this(null,null);
    }
    
    public LayoutPanel(IAcqLayout al, AcqSetting as) {
        super(true);
        setOpaque(true);
        
/*        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image cursorImage = toolkit.getImage(getClass().getClassLoader().getResource("autoimage/resources/ZoomCursor.png"));
        Point cursorHotSpot = new Point(11,11);
        zoomCursor = toolkit.createCustomCursor(cursorImage, cursorHotSpot, "Zoom Cursor");*/
        showZProfile=false;
        showAreaLabels=true;
        mergeAreasBounds=null;
        cAcqSetting=as;        
        setAcquisitionLayout(al);
    }

    
    //IStageMonitorListener
    @Override
    public void stagePositionChanged(Double[] stagePos) {
        setCurrentXYStagePos(stagePos[0], stagePos[1]);
        repaint();
    }
    //end IStageMonitorListener
    

    public IAcqLayout getAcqLayout() {
        return acqLayout;
    }
    
    public void setAcquisitionLayout(IAcqLayout al) {
        this.acqLayout=al;
        zoom=1;
        scale=1;
        setPreferredSize(getPreferredLayoutSize());
        revalidate();
        repaint();
    }

    public void setMergeAreasBounds(Rectangle2D.Double rect) {
        mergeAreasBounds=rect;
        revalidate();
        repaint();
    }
        
    static void setMaxZoom(double mZoom) {
        MAX_ZOOM=mZoom;
    }

    //receives screen coordinate
    public double convertPixToLayoutCoord(int pix) {
        return (pix/scale/zoom);
    }
    
    //receives screen coordinate
    public Point2D convertPixToLayoutCoord(Point p) {
        return new Point2D.Double(convertPixToLayoutCoord(p.x),convertPixToLayoutCoord(p.y));
    }
    
    //w, h: width and height of JScrollPane.getVisibleRect()
    public void calculateScale(int w, int h) { 
        Dimension d=new Dimension(getPreferredLayoutWidth(),getPreferredLayoutLength());
        if (zoom==1) {
            scale=Math.min(((double)w-2)/d.width,((double)h-2)/d.height);
            revalidate();
            repaint();
        }
    }
    
    private Dimension scaleDimension(Dimension dim, double factor) {
        return new Dimension((int)(factor*dim.width), (int)(factor*dim.height));
    }

   
    private Dimension getPreferredLayoutSize() {
        return new Dimension(
                    getPreferredLayoutWidth(),
                    getPreferredLayoutLength());
    }

    
    private int getPreferredLayoutWidth() {
        if (acqLayout!=null) {
            return (int)Math.ceil(acqLayout.getWidth());
        } else
            return LAYOUT_MAX_DIM;
    }
    
    
    private int getPreferredLayoutLength() {
        if (acqLayout!=null) {
            return (int)Math.ceil(acqLayout.getLength());
        } else
            return Math.round(LAYOUT_MAX_DIM/2);
    }
    
    
    @Override
    public Dimension getPreferredSize() {
        return scaleDimension(getPreferredLayoutSize(),scale*zoom);
    }
    
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return scaleDimension(getPreferredLayoutSize(),scale);
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

    
    public double zoomIn() {
        if (zoom < MAX_ZOOM) {
            zoom=zoom*2; 
        } else {
            zoom=MAX_ZOOM;
        }  
        setSize(scaleDimension(getPreferredLayoutSize(),zoom));
        revalidate();
        repaint();
        return zoom;
    }
    
    public double zoomOut(int w, int h) {
        zoom=zoom/2;
        if (zoom < 1) {
            zoom=1;
            calculateScale(w,h);
        } else {   
            setSize(scaleDimension(getPreferredLayoutSize(),zoom));
            revalidate();
            repaint();
        }
        return zoom;
    }
    
    public double getZoom() {
        return zoom;
    }
       
    
    public void drawTileGridForAllSelectedAreas(Graphics2D g2d) {
        if ((acqLayout!=null) && (acqLayout.getAreaArray().size()>0)) {
            List areas=acqLayout.getAreaArray();
            if (cAcqSetting!=null) {
                double tileWidth=cAcqSetting.getTileWidth_UM();
                double tileHeight=cAcqSetting.getTileHeight_UM();
                TilingSetting tSetting=cAcqSetting.getTilingSetting();
                
                g2d.setColor(COLOR_TILE_GRID);
                Composite oldComposite=g2d.getComposite();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.25f));
                //get tile shape, centered at 0,0; considers camera field rotation
                Shape tileMaster=cAcqSetting.getFieldOfView().getEffectiveRoiPath();
                for (int i=0; i<areas.size(); i++) {
                    BasicArea a=acqLayout.getAreaArray().get(i);
                    if (a.isSelectedForAcq() && a.getTilePositions()!=null) {
                        for (Tile t : a.getTilePositions()) {
                            AffineTransform affine=AffineTransform.getTranslateInstance(t.centerX, t.centerY);
                            affine.concatenate(AffineTransform.getRotateInstance(acqLayout.getStageToLayoutRot()));
                            Shape tileShape=affine.createTransformedShape(tileMaster);
                            g2d.fill(tileShape);                            
                        }  
                    }
                }
                g2d.setComposite(oldComposite);
            }      
        }
    }

    protected Color getFillColor(BasicArea area, boolean showRelZPos) {
        Color color=COLOR_UNSELECTED_AREA;
        if (area.isAcquiring()) {
            color=COLOR_ACQUIRING_AREA;
        } else {
            if (showRelZPos) {
                double relativeZPos=area.getRelativeZPos();
                if (Math.round(relativeZPos)==0) {
                    color=new Color(128,128,128);
                } else if (Math.round(relativeZPos) < 0) {
                    color=new Color(64,64,64);
                } else if (Math.round(relativeZPos) > 0) {
                    color=new Color(192,192,192);
                }
            }    
        }
        return color;
    }

    protected Color getBorderColor(BasicArea area) {
        if (area.isSelectedForMerge())
            return COLOR_MERGE_AREA_BORDER;
        else {
            if (area.isSelectedForAcq())
                return COLOR_SELECTED_AREA_BORDER;
            else    
                return COLOR_AREA_BORDER;
        }   
    }
    
    private void drawAreaLabel(Graphics2D g2d, Font font, BasicArea area) {
        g2d.setColor(COLOR_AREA_LABEL);
        g2d.setFont(font);
        FontMetrics fm=g2d.getFontMetrics(font);
        g2d.drawString(area.getName(),
                (int)(area.getCenterXYPos().getX() - fm.getStringBounds(area.getName(), g2d).getWidth()/2),
                (int)(area.getCenterXYPos().getY() + fm.getAscent() - (fm.getAscent() + fm.getDescent()) / 2));
    }  
    
    private void drawAllAreas(Graphics2D g2d, Font font) {
        if (acqLayout!=null) {
            List<BasicArea> areas=acqLayout.getAreaArray();
            if (cAcqSetting!=null) {
                for (BasicArea area:areas) {
                    g2d.setColor(getFillColor(area,showZProfile));
                    g2d.fill(area);
                    g2d.setColor(getBorderColor(area));
                    g2d.draw(area); 
                }
                if (showAreaLabels) {
                    for (BasicArea a:areas) {
                        drawAreaLabel(g2d, font, a);
                    }
                }
                drawTileGridForAllSelectedAreas(g2d);
            }    
        }
    }
    
    private void drawLandmarks(Graphics2D g2d) {
        if ((acqLayout!=null) && (acqLayout.getLandmarks()!=null)) {
            List<RefArea> landmarks=acqLayout.getLandmarks();
            for (RefArea lm:landmarks) {
                g2d.setColor(lm.isSelected() ? COLOR_SELECTED_LANDMARK : COLOR_LANDMARK);
                g2d.setStroke(lm.isStagePosFound() ? SOLID_STROKE : DASHED_STROKE);
                AffineTransform lmTransform=AffineTransform.getRotateInstance(
                        acqLayout.getStageToLayoutRot(),
                        lm.getLayoutCoordX(),
                        lm.getLayoutCoordY());
                Shape landmark=lmTransform.createTransformedShape(new Rectangle2D.Double(
                        lm.getLayoutCoordX() - lm.getPhysWidth()/2,
                        lm.getLayoutCoordY() - lm.getPhysHeight()/2 , 
                        lm.getPhysWidth(), 
                        lm.getPhysHeight()));
                g2d.draw(landmark);
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
        Graphics2D g2d = (Graphics2D) getGraphics();
        Rectangle r=createRectangle(anchorMousePos, prevMousePos);
        g2d.setXORMode(getBackground());
        g2d.drawRect(r.x, r.y, r.width, r.height);        
        if (redraw) {
            r=createRectangle(anchorMousePos, mp);
            g2d.setXORMode(getBackground());
            g2d.drawRect(r.x, r.y, r.width, r.height);
            g2d.setPaintMode();  
        } else
            g2d.drawLine(anchorMousePos.x,anchorMousePos.y,anchorMousePos.x,anchorMousePos.y);
        prevMousePos = mp;
    }
    
    public void updateSelectionPath(SelectionPath selection) {
        if (selection!=null && selection.getPath()!=null) {
            Path2D selPath=selection.getPath();
            Graphics2D g = (Graphics2D) getGraphics();
            AffineTransform at=g.getTransform();
            Path2D path=new Path2D.Double(selPath);
            g.scale(zoom*scale, zoom*scale);
            selPath.transform(at);
            g.setXORMode(getBackground());
            g.draw(selPath);        
            g.setPaintMode();  
            g.setTransform(at);
        }
    }
    
    public void drawFovAtCurrentStagePos(Graphics2D g2d) {
        Point2D fullChipCenter=acqLayout.convertStageToLayoutPos_XY(new Point2D.Double(
                currentStagePos_X ,
                currentStagePos_Y ));            
        FieldOfView fov=cAcqSetting.getFieldOfView();
        AffineTransform fovTransform=AffineTransform.getTranslateInstance(fullChipCenter.getX(),fullChipCenter.getY());
        fovTransform.concatenate(AffineTransform.getRotateInstance(acqLayout.getStageToLayoutRot()));
        fov.createRoiPath(cAcqSetting.getObjPixelSize());
        
        Shape roi=fovTransform.createTransformedShape(fov.getEffectiveRoiPath());
//        fov.createFullChipPath(objPixSize);
//        Shape fullChip=fovTransform.createTransformedShape(fov.getFullChipPath());
        g2d.setColor(COLOR_FOV);
//        g2d.draw(fullChip);
        
        Composite oldComposite=g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.5f));     
        g2d.fill(roi);

        g2d.setComposite(oldComposite);
    }
    
    public void setCurrentXYStagePos(double xPos, double yPos) {
        stageXYPosUpdated=currentStagePos_X!=xPos || currentStagePos_Y!=yPos;
        currentStagePos_X=xPos;
        currentStagePos_Y=yPos;
    }
    
    private double calcDist(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));
    }
    
    private Point2D[] calcGradientVector() {
        Vec3d n=null;
        try {
            n = new Vec3d(acqLayout.getNormalVector());
            //ensure that normalvector used for tilt always points in same direction
            if (n.z < 0) {
                n.negate();
            }
            n.z=0;
            //scale so that length of n = 1
            n = Vec3d.normalized(n);
        } catch (Exception ex) {
//            Logger.getLogger(LayoutPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (acqLayout.getLandmarks()!=null & acqLayout.getLandmarks().size()>1 & calcDist(0,0,n.x,n.y)>0) {
            //gradient center positioned in center of layout
            Rectangle2D layoutRect=new Rectangle2D.Double(0,0,acqLayout.getWidth(),acqLayout.getLength());
            java.awt.geom.Area stage = new java.awt.geom.Area(layoutRect);
            stage.transform(acqLayout.getLayoutToStageTransform());
            Rectangle2D stageRect=stage.getBounds();

            double lmx=stageRect.getWidth()/2;
            double lmy=stageRect.getHeight()/2;
            double radius=calcDist(0,0,lmx,lmy)*n.length();
            
            Point2D[] stagep=new Point2D.Double[2];
            stagep[0] = new Point2D.Double(stageRect.getX()+(lmx+n.x*radius),stageRect.getY()+(lmy+n.y*radius));
            stagep[1] = new Point2D.Double(stageRect.getX()+(lmx-n.x*radius),stageRect.getY()+(lmy-n.y*radius));
            
            Point2D[] layoutp=new Point2D.Double[2];
            layoutp[0] = acqLayout.convertStageToLayoutPos_XY(stagep[0]);
            layoutp[1] = acqLayout.convertStageToLayoutPos_XY(stagep[1]);

            Point2D[] screenp=new Point2D.Double[2];
            screenp[0] = new Point2D.Double(layoutp[0].getX(), layoutp[0].getY());
            screenp[1] = new Point2D.Double(layoutp[1].getX(), layoutp[1].getY());
            return screenp;
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
    
    public boolean isShowZProfile() {
        return showZProfile;
    }
    
    public void enableShowAreaLabels(boolean b) {
        showAreaLabels=b;
    }
    
    public boolean isShowAreaLabels() {
        return showAreaLabels;
    }
    
    private void drawStageGrid(Graphics2D g2d) {
        g2d.setStroke(SOLID_STROKE);
        Rectangle2D layoutRect=new Rectangle2D.Double(0,0,acqLayout.getWidth(),acqLayout.getLength());
        java.awt.geom.Area a = new java.awt.geom.Area(layoutRect);
        a.transform(acqLayout.getLayoutToStageTransform());
        Rectangle2D stageRect=a.getBounds();
        
        //vertical stage grid lines
        Point2D stageStart=new Point2D.Double(stageRect.getX(),stageRect.getY());
        Point2D stageEnd=new Point2D.Double(stageRect.getX(),stageRect.getY()+stageRect.getHeight());
        while (stageStart.getX() < stageRect.getX()+stageRect.getWidth()) {
            Point2D layoutStart=acqLayout.convertStageToLayoutPos_XY(stageStart);
            Point2D layoutEnd=acqLayout.convertStageToLayoutPos_XY(stageEnd);
            g2d.setColor(COLOR_STAGE_GRID);
            g2d.drawLine(
                    (int)Math.round(layoutStart.getX()),
                    (int)Math.round(layoutStart.getY()),
                    (int)Math.round(layoutEnd.getX()),
                    (int)Math.round(layoutEnd.getY()));
            stageStart=new Point2D.Double(stageStart.getX()+1000,stageStart.getY());
            stageEnd=new Point2D.Double(stageStart.getX(),stageEnd.getY());
        }
        //horizontal stage grid lines
        stageStart=new Point2D.Double(stageRect.getX(),stageRect.getY());
        stageEnd=new Point2D.Double(stageRect.getX()+stageRect.getWidth(),stageRect.getY());
        while (stageStart.getY() < stageRect.getY()+stageRect.getHeight()) {
            Point2D layoutStart=acqLayout.convertStageToLayoutPos_XY(stageStart);
            Point2D layoutEnd=acqLayout.convertStageToLayoutPos_XY(stageEnd);
            g2d.drawLine(
                    (int)Math.round(layoutStart.getX()),
                    (int)Math.round(layoutStart.getY()),
                    (int)Math.round(layoutEnd.getX()),
                    (int)Math.round(layoutEnd.getY()));
            stageStart=new Point2D.Double(stageStart.getX(),stageStart.getY()+1000);
            stageEnd=new Point2D.Double(stageEnd.getX(),stageStart.getY());
        }        
    }
    
    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
//        Graphics2D g2d = (Graphics2D)g2d.create();
        g2d.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON));
        //save original g2d settings
        AffineTransform oldTransform=g2d.getTransform();
        Composite oldComposite=g2d.getComposite();
        Color oldColor=g2d.getColor();
        Stroke oldStroke=g2d.getStroke();

        //ensure that BasicArea and RefArea work with current layout rotation value
        BasicArea.setStageToLayoutRot(acqLayout.getStageToLayoutRot());
        RefArea.setStageToLayoutRot(acqLayout.getStageToLayoutRot());

        //bounding rectangle of panel;
        Rectangle panelR=getBounds();

        //clear: fill panel with Border color
        g2d.setColor(COLOR_PANEL_BACKGR);
        g2d.fillRect(panelR.x, panelR.y, panelR.width, panelR.height); 
        
        Dimension dim=getPreferredLayoutSize();

        g2d.scale(scale*zoom, scale*zoom);
        if (g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING)==RenderingHints.VALUE_ANTIALIAS_ON) {
            float scale=(float)(1d/g2d.getTransform().getScaleX());
            SOLID_STROKE=new BasicStroke(scale*STROKE_WIDTH);
            DASHED_STROKE = new BasicStroke(scale, // line width
                /* cap style */BasicStroke.CAP_BUTT,
                /* join style, miter limit */BasicStroke.JOIN_BEVEL, scale,
                /* the dash pattern */new float[] { scale*4, scale*4},0.0f);
        }

        //draw background
        if (showZProfile) {
            //uses stageToLayoutTransform to calculate gradient screen start and end points
            Point2D[] p=calcGradientVector();
            if (p!=null) {
                Point2D start = new Point2D.Double(p[0].getX(),p[0].getY());
                Point2D end = new Point2D.Double(p[1].getX(),p[1].getY());
                float[] dist = {0.0f, 0.5f, 1.0f};
                Color[] colors = {COLOR_Z_TILT_HIGH, Color.BLACK, COLOR_Z_TILT_LOW};
                LinearGradientPaint lg =new LinearGradientPaint(start, end, dist, colors);            
                g2d.setPaint(lg);
                g2d.fill (new Rectangle2D.Double(0, 0, dim.width, dim.height));
            } else {
                g2d.setColor(COLOR_LAYOUT_BACKGR);
                g2d.fillRect(0, 0, dim.width, dim.height); //getWidth(), getHeight());                
            }
        } else {
            g2d.setColor(COLOR_LAYOUT_BACKGR);
            g2d.fillRect(0, 0, dim.width, dim.height); //getWidth(), getHeight());
        }
        
        //draw stage grid
        drawStageGrid(g2d);

        if ((acqLayout!=null) && !acqLayout.isEmpty()) {
            //the g2d object is scaled by the calling Layout --> apply inverse scaling to keep constant font size
            Font font=new Font("Arial",Font.PLAIN,(int)Math.round(LABEL_FONT_SIZE/g2d.getTransform().getScaleX()));
            drawAllAreas(g2d, font);
            drawLandmarks(g2d);
            if (mergeAreasBounds!=null) {
                g2d.setColor(COLOR_SELECTED_AREA);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.3f));
                g2d.fillRect((int)Math.round(mergeAreasBounds.getX()),
                        (int)Math.round(mergeAreasBounds.getY()),
                        (int)Math.round(mergeAreasBounds.getWidth()),
                        (int)Math.round(mergeAreasBounds.getHeight()));
                g2d.setComposite(oldComposite);
                g2d.setStroke(SOLID_STROKE);
                g2d.draw(new Rectangle2D.Double(mergeAreasBounds.getX(),
                        mergeAreasBounds.getY(),
                        mergeAreasBounds.getWidth(),
                        mergeAreasBounds.getHeight()));
            }
                
        } else {
            g2d.setColor(Color.GRAY);
            g2d.setFont(new Font("Arial", Font.BOLD, 72));
            String s = "No Layout File Loaded";
            Rectangle2D bounds=g2d.getFontMetrics().getStringBounds(s, g2d);
            double scaledSize=72*(dim.width/(3*bounds.getWidth()));
            g2d.setFont(new Font("Arial", Font.BOLD, (int)Math.ceil(scaledSize)));
            bounds=g2d.getFontMetrics().getStringBounds(s, g2d);
            g2d.drawString(s, Math.round((dim.width-bounds.getWidth())/2), Math.round((dim.height-bounds.getHeight())/2));
        }    
        if (acqLayout.getNoOfMappedStagePos() > 0) {
            drawFovAtCurrentStagePos(g2d);
        }
        
        //restore original g2d parameters
        g2d.setComposite(oldComposite);
        g2d.setTransform(oldTransform);
        g2d.setColor(oldColor);
        g2d.setStroke(oldStroke);
//        g2d.dispose();
    }
    
    public double getScale() {
        return scale;
    }
}

