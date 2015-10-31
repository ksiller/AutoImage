/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.gui;

import ij.IJ;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 *
 * @author Karsten Siller
 */
public class PreviewPanel extends JPanel {

    protected List<Path2D> paths;
    private List<Color> colors;
    private List<Color> bgColors;
    private double pathDiagonale;
    
    public PreviewPanel (Path2D path, double pathD) {
        //create a copy to ensure that original path is not modifie by transforms in paintComponent method
        this.paths=new ArrayList<Path2D>();
        this.paths.add(new Path2D.Double(path));
        this.pathDiagonale=pathD;
    }
    
    public void setPath(Path2D path, double pathD) {
        //create a copy to ensure that original path is not modifie by transforms in paintComponent method
        this.setPath(path, Color.WHITE, Color.GRAY, pathD);
    }
    
    public void setPath(Path2D path, Color color, Color bgColor, double pathD) {
        //create a copy to ensure that original path is not modifie by transforms in paintComponent method
        paths=new ArrayList<Path2D>();
        paths.add(new Path2D.Double(path));
        colors=new ArrayList<Color>();
        colors.add(color);
        bgColors=new ArrayList<Color>();
        bgColors.add(bgColor);
        this.pathDiagonale=pathD;
    }
    
    public void addPath(Path2D path, Color color, Color bgColor) {
        if (paths==null) {
            paths=new ArrayList<Path2D>();
        }
        paths.add(new Path2D.Double(path));
        if (colors==null) {
            colors=new ArrayList<Color>();
        }
        colors.add(color);
        if (bgColors==null) {
            bgColors=new ArrayList<Color>();
        }
        bgColors.add(bgColor);
    }
    
    @Override
    public void paintComponent(Graphics g) {
        int border = 10;
        Graphics2D g2d=(Graphics2D)g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.black);
        g2d.fill(new Rectangle(0,0,this.getWidth(),this.getHeight()));
        g2d.setColor(Color.yellow);
        g2d.drawLine((int)(this.getWidth()/2), 0, (int)(this.getWidth()/2), border);
        g2d.drawLine((int)(this.getWidth()/2), (int)this.getHeight(), (int)(this.getWidth()/2), (int)this.getHeight()-border);
        g2d.drawLine(0, (int)(this.getHeight()/2), border, (int)(this.getHeight()/2));
        g2d.drawLine((int)this.getWidth()-border, (int)(this.getHeight()/2), (int)this.getWidth(), (int)(this.getHeight()/2));
        if (this.paths!=null && !this.paths.isEmpty()) {
            //create a copy to ensure that original path is not modifie by transforms in paintComponent method
            int index=0;
            double scale=1;
            if (pathDiagonale > 0) {
                scale=Math.min((this.getWidth()-2*border)/pathDiagonale, (this.getHeight()-2*border)/pathDiagonale);
            } else {
                scale=Math.min((this.getWidth()-2*border)/paths.get(0).getBounds2D().getWidth(), (this.getHeight()-2*border)/paths.get(0).getBounds2D().getHeight());
            }
            double translatex=paths.get(0).getBounds2D().getCenterX();
            double translatey=paths.get(0).getBounds2D().getCenterY();
            for (Path2D p:paths) {
                Path2D path=new Path2D.Double(p);
                AffineTransform at=new AffineTransform();
                at.translate(-translatex, -translatey);
                path.transform(at);
                at=new AffineTransform();
                at.scale(scale,scale);
                path.transform(at);
                at=new AffineTransform();
                at.translate(this.getWidth()/2,this.getHeight()/2);
                path.transform(at);
                if (bgColors!=null && index<bgColors.size()) {
                    if (bgColors.get(index)!=null) {
                        g2d.setColor(bgColors.get(index));
                        g2d.fill(path);
                    }
                } else {
                    g2d.setColor(Color.GRAY);
                    g2d.fill(path);
                }
                if (colors!=null && index<colors.size()) {
                    if (colors.get(index)!=null) {
                        g2d.setColor(colors.get(index));
                        g2d.draw(path);
                    }
                } else {
                    g2d.setColor(Color.white);
                    g2d.draw(path);
                }
                index++;
            }    
        }
        g2d.setColor(Color.yellow);
        g2d.drawLine((int)(this.getWidth()/2)-5, (int)(this.getHeight()/2), (int)(this.getWidth()/2)+5, (int)(this.getHeight()/2));
        g2d.drawLine((int)(this.getWidth()/2), (int)(this.getHeight()/2)-5, (int)(this.getWidth()/2), (int)(this.getHeight()/2)+5);
    }

}
