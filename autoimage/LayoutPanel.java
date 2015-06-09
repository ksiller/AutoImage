package autoimage;

//import ij.IJ;
import autoimage.area.Area;
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
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 *
 * @author Karsten
 */


class LayoutPanel extends JPanel implements Scrollable, IStageMonitorListener {

    private Cursor zoomCursor;
    private static final Cursor moveCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
    private static final Cursor normCursor = new Cursor(Cursor.DEFAULT_CURSOR);
    
    private static int LAYOUT_MAX_DIM = 20000;
    private static int MAX_ZOOM = 64;
    private static Color COLOR_BORDER = Color.WHITE;
    private static Color COLOR_UNSELECTED_AREA = Color.GRAY;
    private static Color COLOR_ACQUIRING_AREA = Color.YELLOW;
    private static Color COLOR_SELECTED_AREA = new Color(51,115,188);
    private static Color COLOR_FOV = Color.CYAN;
    private static Color COLOR_TILE_GRID = Color.RED;
    private static Color COLOR_LANDMARK = Color.GREEN;
    private static Color COLOR_SEL_LANDMARK = Color.MAGENTA;
    private static Color COLOR_LAYOUT_BACKGR = Color.BLACK;
    private static Color COLOR_STAGE_GRID = Color.DARK_GRAY;
    private static Color COLOR_TILT_LOW = Color.RED;
    private static Color COLOR_TILT_HIGH = Color.BLUE;
    private static Stroke SOLID_STROKE = new BasicStroke(1.0f);
    private static Stroke DASHED_STROKE = new BasicStroke(1.0f, // line width
      /* cap style */BasicStroke.CAP_BUTT,
      /* join style, miter limit */BasicStroke.JOIN_BEVEL, 1.0f,
      /* the dash pattern */new float[] { 8.0f, 8.0f},0.0f);
    private AcqLayout acqLayout;
    private AcqSetting cAcqSetting;//reference to current AcqSetting
    private Point anchorMousePos;
    private Point prevMousePos;
    private double currentStagePos_X;
    private double currentStagePos_Y;
    private boolean stageXYPosUpdated;
//    private boolean showLandmark;
    private boolean showZProfile;
    private Rectangle2D.Double mergeAreasBounds; 
    private AffineTransform layoutTransform;

    
    private double borderDim; //in physDim;
    private double physToPixelRatio;

    private double zoom;
    private double scale;

    public LayoutPanel() {
        this(null,null);
    }
    
    public LayoutPanel(AcqLayout al, AcqSetting as) {
        super(true);
//        IJ.log("LayoutPanel.constructor");
        setOpaque(true);
        
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image cursorImage = toolkit.getImage(getClass().getClassLoader().getResource("autoimage/resources/ZoomCursor.png"));
        Point cursorHotSpot = new Point(11,11);
        zoomCursor = toolkit.createCustomCursor(cursorImage, cursorHotSpot, "Zoom Cursor");
        showZProfile=false;
        mergeAreasBounds=null;
        cAcqSetting=as;
        
        setAcquisitionLayout(al, 0);
      
//        this.addComponentListener(new ComponentHandler());

/*        JPopupMenu menuPopup = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Move to default position");
        menuPopup.add(menuItem);
        this.setComponentPopupMenu(menuPopup);*/
    }

    
    //IStageMonitorListener
    @Override
    public void stagePositionChanged(Double[] stagePos) {
        setCurrentXYStagePos(stagePos[0], stagePos[1]);
        repaint();
    }
    //end IStageMonitorListener
    

    public AcqLayout getAcqLayout() {
        return acqLayout;
    }
    
    public void setMergeAreasBounds(Rectangle2D.Double rect) {
        mergeAreasBounds=rect;
        revalidate();
        repaint();
    }
    
    public void setShowZProfile(boolean b) {
        showZProfile=b;
    }
    
    public boolean getShowZProfile() {
        return showZProfile;
    }
    
    public void setAcquisitionLayout(AcqLayout al, double borderD) {
 //       IJ.log("LayoutPanel.setAcquisitionLayout: start");
        this.acqLayout=al;
        borderDim=borderD;
        zoom=1;
        scale=1;
//        IJ.log("LayoutPanel.setAcquisitionLayout: before calculatePhysToPixelRatio");
        calculatePhysToPixelRatio();
//        IJ.log("LayoutPanel.setAcquisitionLayout: after calculatePhysToPixelRatio");
//        calculateScale(0,0);
//        setPreferredSize(new Dimension(getPreferredLayoutWidth(),getPreferredLayoutHeight()));
        setPreferredSize(getPreferredLayoutSize());
        revalidate();
        repaint();
//        IJ.log("LayoutPanel.setAcquisitionLayout: end");
    }

    public void setlayoutRotation(double rad) {
        layoutTransform.setToRotation(rad);
    }
    
    public void setLayoutTransform(AffineTransform at) {
        layoutTransform=at;
    }
    
    //receives screen coordinate
    public double convertPixToLayoutCoord(int pix) {
        return (pix/physToPixelRatio/scale/zoom)-borderDim;
    }
    
    //receives screen coordinate
    public Point2D.Double convertPixToLayoutCoord(Point p) {
        return new Point2D.Double(convertPixToLayoutCoord(p.x),convertPixToLayoutCoord(p.y));
    }
    
    public int getBorderDimPix() {
        int bdPix;
        if ((borderDim>0) && (physToPixelRatio!=1))
            bdPix=(int)Math.round(borderDim*physToPixelRatio);
        else
            bdPix=10;
        return bdPix;
     }
    
    //w, h: width and height of JScrollPane.getVisibleRect()
    public void calculateScale(int w, int h) { 
        Dimension d=new Dimension(getPreferredLayoutWidth(),getPreferredLayoutLength());
        if (zoom==1) {
            scale=Math.min(((double)w-2)/d.width,((double)h-2)/d.height);
            revalidate();
            repaint();
        }
//        IJ.log("LayoutPanel.calculateScale: w="+Integer.toString(w)+", h="+Integer.toString(h)+", "+Double.toString(d.getWidth())+", "+Double.toString(d.getHeight()));    }
    }
    
    public void calculatePhysToPixelRatio() {
        if (acqLayout!=null) {
//            physToPixelRatio=Math.min((double)LAYOUT_MAX_DIM/acqLayout.getWidth(),(double)LAYOUT_MAX_DIM/acqLayout.getHeight());
            physToPixelRatio=1;
        } else
            physToPixelRatio=1;
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
//            return (int)Math.round(2*getBorderDimPix()+(int)Math.ceil(acqLayout.getWidth()*physToPixelRatio)*zoom);
            return 2*getBorderDimPix()+(int)Math.ceil(acqLayout.getWidth()*physToPixelRatio);
        } else
            return LAYOUT_MAX_DIM;
    }
    
    
    private int getPreferredLayoutLength() {
        if (acqLayout!=null) {
//            return (int)Math.round(2*getBorderDimPix()+(int)Math.ceil(acqLayout.getHeight()*physToPixelRatio)*zoom);
            return 2*getBorderDimPix()+(int)Math.ceil(acqLayout.getLength()*physToPixelRatio);
        } else
            return Math.round(LAYOUT_MAX_DIM/2);
    }
    
    
    @Override
    public Dimension getPreferredSize() {
    //    int w = (int)(getPreferredLayoutWidth()*scale*zoom);  
    //    int h = (int)(getPreferredLayoutHeight()*scale*zoom);  
    //    return new Dimension(w, h);
        return scaleDimension(getPreferredLayoutSize(),scale*zoom);
    //    return super.getPreferredSize();
    }
    
    @Override
    public Dimension getPreferredScrollableViewportSize() {
//        IJ.log("LayoutPanel.getPreferredScrollableViewportSize()");
//        return getPreferredSize();
//        IJ.log("LayoutPanel.getPreferredScrollableViewPortSize:" +scaleDimension(getPreferredLayoutSize(),scale).toString());
//        return new Dimension((int)Math.round(getPreferredLayoutWidth()*scale),(int)Math.round(getPreferredLayoutHeight()*scale));
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

    
    public double zoomIn() {
//        IJ.log("zoom"+Double.toString(zoom)+", scale:"+Double.toString(scale));
//        IJ.log("LayoutPanel.zoomIn: scaled getPreferredLayoutSize (zoom:"+Double.toString(zoom)+":" +scaleDimension(getPreferredLayoutSize(),zoom*scale).toString());
        if (zoom < MAX_ZOOM)
            zoom=zoom*2;
//        setSize(new Dimension((int)(getPreferredLayoutWidth()*zoom),(int)(getPreferredLayoutHeight()*zoom)));
        setSize(scaleDimension(getPreferredLayoutSize(),zoom));
        revalidate();
        repaint();
//        IJ.log("zoom"+Double.toString(zoom)+", scale:"+Double.toString(scale));
//        IJ.log("LayoutPanel.zoomIn: scaled getPreferredLayoutSize (zoom:"+Double.toString(zoom)+":" +scaleDimension(getPreferredLayoutSize(),zoom*scale).toString());
        return zoom;
    }
    
    public double zoomOut(int w, int h) {
//        IJ.log("zoom"+Double.toString(zoom)+", scale:"+Double.toString(scale));
//        IJ.log("LayoutPanel.zoomOut: scaled getPreferredLayoutSize (zoom:"+Double.toString(zoom)+":" +scaleDimension(getPreferredLayoutSize(),zoom*scale).toString());
        zoom=zoom/2;
        if (zoom < 1) {
            zoom=1;
//            calculateScale(getParent().getWidth(),getParent().getHeight());
            calculateScale(w,h);
        } else {   
//            setSize(new Dimension((int)(getPreferredLayoutWidth()*zoom),(int)(getPreferredLayoutHeight()*zoom)));
            setSize(scaleDimension(getPreferredLayoutSize(),zoom));
            revalidate();
            repaint();
        }
//        IJ.log("zoom"+Double.toString(zoom)+", scale:"+Double.toString(scale));
//        IJ.log("LayoutPanel.zoomOut: scaled getPreferredLayoutSize (zoom:"+Double.toString(zoom)+":" +scaleDimension(getPreferredLayoutSize(),zoom*scale).toString());
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
                double tileWidth=cAcqSetting.getTileWidth_UM();
                double tileHeight=cAcqSetting.getTileHeight_UM();
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
                double tileWidth=cAcqSetting.getTileWidth_UM();
                double tileHeight=cAcqSetting.getTileHeight_UM();
                TilingSetting tSetting=cAcqSetting.getTilingSetting();
                for (int i=0; i<areas.size(); i++) {
                    Area a=acqLayout.getAreaArray().get(i);
//                    a.draw(g2d, getBorderDimPix(), physToPixelRatio, tileWidth, tileHeight,tSetting,true);
                    a.drawArea(g2d, getBorderDimPix(), physToPixelRatio, showZProfile);
                }
                drawTileGridForAllSelectedAreas(g2d);
            }    
        }
    }
    
    private void drawLandmarks(Graphics2D g2d) {
//        IJ.log("drawLandmark");
        if ((acqLayout!=null) && (acqLayout.getLandmarks()!=null)) {
            List<RefArea> landmarks=acqLayout.getLandmarks();
            int bdPix=getBorderDimPix();
            for (int i=0; i<landmarks.size(); i++) {
                RefArea lm=landmarks.get(i);
                int xo = bdPix + (int) Math.round(lm.getLayoutCoordOrigX()*physToPixelRatio);
                int yo = bdPix + (int) Math.round(lm.getLayoutCoordOrigY()*physToPixelRatio);
                int xCenter = bdPix + (int) Math.round(lm.getLayoutCoordX()*physToPixelRatio);
                int yCenter = bdPix + (int) Math.round(lm.getLayoutCoordY()*physToPixelRatio);
                int w = (int) Math.round(lm.getPhysWidth()*physToPixelRatio);
                int h = (int) Math.round(lm.getPhysHeight()*physToPixelRatio);
//                IJ.log("drawLandmark "+Integer.toString(i)+": "+Integer.toString(x)+" "+Integer.toString(y)+" "+Integer.toString(w)+" "+Integer.toString(h));
                if (lm.isSelected())
                    g2d.setColor(COLOR_SEL_LANDMARK);
                else
                    g2d.setColor(COLOR_LANDMARK);       
                if (lm.isStagePosFound()) {
                    g2d.setStroke(SOLID_STROKE);
                } else {
                    g2d.setStroke(DASHED_STROKE);
                } 
                AffineTransform at=g2d.getTransform();
                g2d.translate(xCenter,yCenter);
//                    double stageRotAngle=Math.atan2(acqLayout.getStageToLayoutTransform().getShearY(), acqLayout.getStageToLayoutTransform().getScaleY());
                g2d.rotate(RefArea.getStageToLayoutRot());
                if (RefArea.getCameraRot() != FieldOfView.ROTATION_UNKNOWN) {
                    g2d.rotate(-RefArea.getCameraRot());                        
                }
                g2d.translate(-xCenter,-yCenter);
                g2d.drawRect(xo,yo,w,h);
                g2d.setTransform(at);
            /*    
                Point2D p=acqLayout.convertStageToLayoutPos_XY(new Point2D.Double(lm.getStageCoordX(),lm.getStageCoordY()));
                xo = bdPix + (int) Math.round((p.getX()-lm.getPhysWidth()/2)*physToPixelRatio);
                yo = bdPix + (int) Math.round((p.getY()-lm.getPhysHeight()/2)*physToPixelRatio);
                xCenter = bdPix + (int) Math.round(p.getX()*physToPixelRatio);
                yCenter = bdPix + (int) Math.round(p.getY()*physToPixelRatio);
                w = (int) Math.round(lm.getPhysWidth()*physToPixelRatio);
                h = (int) Math.round(lm.getPhysHeight()*physToPixelRatio);
//                IJ.log("drawLandmark "+Integer.toString(i)+": "+Integer.toString(x)+" "+Integer.toString(y)+" "+Integer.toString(w)+" "+Integer.toString(h));
                g2d.setColor(Color.BLUE);       
                at=g2d.getTransform();
                g2d.translate(xCenter,yCenter);
                g2d.rotate(RefArea.getCameraRot());                        
                g2d.translate(-xCenter,-yCenter);
                g2d.drawRect(xo,yo,w,h);
                g2d.setTransform(at);*/
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
            int binning=cAcqSetting.getBinning();
            double objPixSize=cAcqSetting.getObjPixelSize();
            
            FieldOfView fov=cAcqSetting.getFieldOfView();
            AffineTransform oldTransform=g2d.getTransform();

            Point2D fullChipOrigin=acqLayout.convertStageToLayoutPos_XY(new Point2D.Double(
                    currentStagePos_X ,
                    currentStagePos_Y ));            
            int x=bdPix+(int)Math.round(physToPixelRatio*(fullChipOrigin.getX()- fov.getFullWidth_UM(objPixSize)/2));
            int y=bdPix+(int)Math.round(physToPixelRatio*(fullChipOrigin.getY()- fov.getFullHeight_UM(objPixSize)/2));
            int w=(int)Math.ceil(fov.getFullWidth_UM(objPixSize)*physToPixelRatio);
            int h=(int)Math.ceil(fov.getFullHeight_UM(objPixSize)*physToPixelRatio);

            g2d.translate(x+w/2, y+h/2);
//                double stageRotAngle=Math.atan2(acqLayout.getStageToLayoutTransform().getShearY(), acqLayout.getStageToLayoutTransform().getScaleY());
            g2d.rotate(Area.getStageToLayoutRot());
            if (fov.getFieldRotation() != FieldOfView.ROTATION_UNKNOWN) {
                g2d.rotate(-fov.getFieldRotation());
//                g2d.rotate(fov.getFieldRotation()/180*Math.PI);
            }
            g2d.translate(-x-w/2, -y-h/2);
            g2d.setColor(COLOR_FOV);
            g2d.drawRect(x,y,w,h);
            
            Point2D offset_UM=fov.getRoiOffset_UM(objPixSize);
            Point2D roiOrigin=acqLayout.convertStageToLayoutPos_XY(new Point2D.Double(
                    currentStagePos_X ,
                    currentStagePos_Y ));
            Composite oldComposite=g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.5f));
            Point2D lpoint=acqLayout.convertStageToLayoutPos_XY(new Point2D.Double(currentStagePos_X- fov.getFullWidth_UM(objPixSize)/2 + offset_UM.getX()
                    ,currentStagePos_Y- fov.getFullHeight_UM(objPixSize)/2 + offset_UM.getY()));
            x=bdPix+(int)Math.round(physToPixelRatio*(roiOrigin.getX()- fov.getFullWidth_UM(objPixSize)/2 + offset_UM.getX()));
            y=bdPix+(int)Math.round(physToPixelRatio*(roiOrigin.getY()- fov.getFullHeight_UM(objPixSize)/2 + offset_UM.getY()));
            w=(int)Math.ceil(fov.getRoiWidth_UM(objPixSize) * physToPixelRatio);
            h=(int)Math.ceil(fov.getRoiHeight_UM(objPixSize) * physToPixelRatio);

            g2d.fillRect(x,y,w,h);
            g2d.setComposite(oldComposite);
            g2d.setTransform(oldTransform);
            g2d.setStroke(SOLID_STROKE);
//            g2d.drawRect(x,y,w,h);
            //g2d.rotate(-45);
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

            IJ.log("layoutRect: "+layoutRect.toString()+"; stageRect: "+stageRect.toString());
            double lmx=stageRect.getWidth()/2;
            double lmy=stageRect.getHeight()/2;
            double radius=calcDist(0,0,lmx,lmy)*n.length();
            
            Point2D[] stagep=new Point2D.Double[2];
//            stagep[0] = new Point2D.Double(stageRect.getX()+(lmx+n.x*radius),stageRect.getY()+(lmy+n.y*radius));
//            stagep[1] = new Point2D.Double(stageRect.getX()+(lmx-n.x*radius),stageRect.getY()+(lmy-n.y*radius));
            stagep[0] = new Point2D.Double(stageRect.getX()+(lmx+n.x*radius),stageRect.getY()+(lmy+n.y*radius));
            stagep[1] = new Point2D.Double(stageRect.getX()+(lmx-n.x*radius),stageRect.getY()+(lmy-n.y*radius));
            
            Point2D[] layoutp=new Point2D.Double[2];
            layoutp[0] = acqLayout.convertStageToLayoutPos_XY(stagep[0]);
            layoutp[1] = acqLayout.convertStageToLayoutPos_XY(stagep[1]);

            Point2D[] screenp=new Point2D.Double[2];
            screenp[0] = new Point2D.Double(physToPixelRatio * layoutp[0].getX(), physToPixelRatio * layoutp[0].getY());
            screenp[1] = new Point2D.Double(physToPixelRatio * layoutp[1].getX(), physToPixelRatio * layoutp[1].getY());
            IJ.log("stagep[0]: "+stagep[0].toString()+", stagep[1]: "+stagep[1].toString());
            IJ.log("layoutp[0]: "+layoutp[0].toString()+", layoutp[1]: "+layoutp[1].toString());
            IJ.log("screenp[0]: "+screenp[0].toString()+", screenp[1]: "+screenp[1].toString());
/*            double lmx=acqLayout.getWidth()/2;
            double lmy=acqLayout.getHeight()/2;
            double radius=calcDist(0,0,lmx,lmy);
            
            Point2D.Double[] p=new Point2D.Double[2];
            p[0] = new Point2D.Double(physToPixelRatio*(lmx+n.x*radius), physToPixelRatio*(lmy+n.y*radius));
            p[1] = new Point2D.Double(physToPixelRatio*(lmx-n.x*radius), physToPixelRatio*(lmy-n.y*radius));
//            IJ.log("LayoutPanel.calGradientVector: radius: "+Double.toString(radius));
//            IJ.log("LayoutPanel.calGradientVector: p[0]: ("+Double.toString(p[0].x)+"/"+p[0].y+"), p[1]: ("+Double.toString(p[1].x)+"/"+p[1].y+")");*/
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
    
    private void drawStageGrid(Graphics2D g2d) {
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
        //vertical stage grid lines
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
//        Graphics2D g2d = (Graphics2D) g;
        Graphics2D g2d = (Graphics2D)g.create();
        //save original g2d settings
        AffineTransform oldTransform=g2d.getTransform();
        Composite oldComposite=g2d.getComposite();
//        Color oldColor=g2d.getColor();
//        Stroke oldStroke=g2d.getStroke();

        //ensure that Area and RefArea work with current layout rotation value
        Area.setStageToLayoutRot(acqLayout.getStageToLayoutRot());
        RefArea.setStageToLayoutRot(acqLayout.getStageToLayoutRot());

        //bounding rectangle of panel;
        Rectangle panelR=getBounds();

        //clear: fill panel with Border color
        g2d.setColor(COLOR_BORDER);
        g2d.fillRect(panelR.x, panelR.y, panelR.width, panelR.height); 
        
        borderDim=10;

        int bdPix=getBorderDimPix();
        Dimension dim=getPreferredLayoutSize();
        int w=dim.width;//+2*bdPix;
        int h=dim.height;//+2*bdPix;
/*        Dimension dimRot=getPreferredLayoutSizeRot();
        int wRot=dimRot.width;//+2*bdPix;
        int hRot=dimRot.height;//+2*bdPix;
*/
//        IJ.log("layoutPanel.paintComponent: before translate. PreferredLayoutSize: "+w+", "+h+", scale: "+scale+", zoom: "+zoom);

        g2d.scale(scale*zoom, scale*zoom);

/*        g2d.translate(dimRot.width/2,dimRot.height/2);
        IJ.log(layoutTransform.toString());
        AffineTransform newTransform=g2d.getTransform();
        newTransform.concatenate(layoutTransform);
        IJ.log("layoutPanel.paintComponent:oldTransform: "+oldTransform.toString());
        IJ.log("layoutPanel.paintComponent:layoutTransform: "+layoutTransform.toString());
        IJ.log("layoutPanel.paintComponent:newTransform: "+newTransform.toString());
        g2d.setTransform(newTransform);
        //g2d.rotate(Math.PI/180*10);
        g2d.translate(-dimRot.width/2,-dimRot.height/2);
*/ 
        //draw background
        if (showZProfile) {
            //uses stageToLayoutTransform to calculate gradient screen start and end points
            Point2D[] p=calcGradientVector();

//            Rectangle2D layoutRect=new Rectangle2D.Double(0,0,acqLayout.getWidth(),acqLayout.getHeight());
//            java.awt.geom.Area a = new java.awt.geom.Area(layoutRect);
//            a.transform(acqLayout.getLayoutToStageTransform());
//            Rectangle2D stageRect=a.getBounds();

            if (p!=null) {
            Point2D start = new Point2D.Double(bdPix+p[0].getX(),bdPix+p[0].getY());
            Point2D end = new Point2D.Double(bdPix+p[1].getX(),bdPix+p[1].getY());
            float[] dist = {0.0f, 0.5f, 1.0f};
            Color[] colors = {COLOR_TILT_HIGH, Color.BLACK, COLOR_TILT_LOW};
            LinearGradientPaint lg =new LinearGradientPaint(start, end, dist, colors);            
            g2d.setPaint(lg);
            g2d.fill (new Rectangle2D.Double(bdPix, bdPix, w-2*bdPix, h-2*bdPix));
            } else {
                g2d.setColor(COLOR_LAYOUT_BACKGR);
                g2d.fillRect(bdPix, bdPix, w-2*bdPix, h-2*bdPix); //getWidth(), getHeight());                
            }
        } else {
            g2d.setColor(COLOR_LAYOUT_BACKGR);
            g2d.fillRect(bdPix, bdPix, w-2*bdPix, h-2*bdPix); //getWidth(), getHeight());
        }
        
        //draw stage grid
        drawStageGrid(g2d);

        if ((acqLayout!=null) && !acqLayout.isEmpty()) {
            drawAllAreas(g2d);
            drawLandmarks(g2d);
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
            g2d.setFont(new Font("Arial", Font.BOLD, 72));
            String s = "No Layout File Loaded";
            Rectangle2D bounds=g2d.getFontMetrics().getStringBounds(s, g2d);
            double scaledSize=72*(w/(3*bounds.getWidth()));
            g2d.setFont(new Font("Calibri", Font.BOLD, (int)Math.ceil(scaledSize)));
            bounds=g2d.getFontMetrics().getStringBounds(s, g2d);
            g2d.drawString(s, Math.round((w-bounds.getWidth())/2), Math.round((h-bounds.getHeight())/2));
        }    
        if (acqLayout.getNoOfMappedStagePos() > 0) {
//            g2d.setTransform(newTransform);
            drawFovAtCurrentStagePos(g2d);
        }
        
        //restore original g2d parameters
        g2d.setComposite(oldComposite);
//        g2d.setColor(oldColor);
//        g2d.setStroke(oldStroke);
        g2d.setTransform(oldTransform);
        g2d.dispose();

    }
}

