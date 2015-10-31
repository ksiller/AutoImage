/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.gui;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 *
 * @author Karsten Siller
 */
public class SelectionPath {
    
    private Point2D start;
    private Path2D path;
    
    public SelectionPath(Point2D start) {
        this.start=start;
        this.path=new Path2D.Double();
    }
    
    public void setPath(Path2D p) {
        path=p;
    }

    public Path2D getPath() {
        return path;
    }
    
    public void setStart(Point2D s) {
        start=s;
    }
    
    public Point2D getStart() {
        return start;
    }
}
