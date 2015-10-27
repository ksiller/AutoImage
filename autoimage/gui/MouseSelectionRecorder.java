package autoimage.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 *
 * @author Karsten Siller
 */
public class MouseSelectionRecorder implements MouseListener {
      
    public final int PATH_BOUNDING_RECTANGLE = 1;
    public final int PATH_POLYGON = 2;
    
    public final int MODE_CLICK = 1;
    public final int MODE_DRAG = 2;
    
    private Path2D selectionPath;
    private int mode;
    private int pathType;
    private int mouseButton;
    private Point2D start;
    private Point2D end;
    private boolean running;

    
    public MouseSelectionRecorder() {
        
    }
    
    public void setPathType(int pType) {
        
    }
    
    public void setMouseButton(int mButton) {
        mouseButton=mButton;
    }
    
    public void setMode(int m) {
        mode=m;
    }
    
    public void start() {
        running=true;
    }
    
    public void stop() {
        running=false;
    }
    
    public Path2D getSelectionPath() {
        return selectionPath;
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mousePressed(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mouseExited(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
