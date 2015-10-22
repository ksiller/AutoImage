/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import javax.swing.JPanel;

/**
 *
 * @author Karsten Siller
 */
public class PreviewPanel extends JPanel {

    protected Path2D path;
    private double pathDiagonale;
    
    public PreviewPanel (Path2D path, double pathD) {
        //create a copy to ensure that original path is not modifie by transforms in paintComponent method
        this.path=path;
        this.pathDiagonale=pathD;
    }
    
    public void setPath(Path2D path, double pathD, boolean refresh) {
        //create a copy to ensure that original path is not modifie by transforms in paintComponent method
        this.path=path;
        this.pathDiagonale=pathD;
        if (refresh) {
            revalidate();
            repaint();
        }
    }
    
    @Override
    public void paintComponent(Graphics g) {
        int border = 10;
        Graphics2D g2d=(Graphics2D)g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.black);
        g2d.fill(new Rectangle(0,0,this.getWidth(),this.getHeight()));
        g2d.setColor(Color.yellow);
        g2d.drawLine((int)(this.getWidth()/2), 0, (int)(this.getWidth()/2), (int)this.getHeight());
        g2d.drawLine(0, (int)(this.getHeight()/2), (int)this.getWidth(), (int)(this.getHeight()/2));
        if (this.path!=null) {
            //create a copy to ensure that original path is not modifie by transforms in paintComponent method
            Path2D path=new Path2D.Double(this.path);
            AffineTransform at=new AffineTransform();
            at.translate(-path.getBounds2D().getCenterX(), -path.getBounds2D().getCenterY());
            double scale;
            if (pathDiagonale > 0) {
                scale=Math.min((this.getWidth()-2*border)/pathDiagonale, (this.getHeight()-2*border)/pathDiagonale);
            } else {
                scale=Math.min((this.getWidth()-2*border)/path.getBounds2D().getWidth(), (this.getHeight()-2*border)/path.getBounds2D().getHeight());
            }
            path.transform(at);
            at=new AffineTransform();
            at.scale(scale,scale);
            path.transform(at);
            at=new AffineTransform();
            at.translate(this.getWidth()/2,this.getHeight()/2);
            path.transform(at);
            g2d.setColor(Color.GRAY);
            g2d.fill(path);
            g2d.setColor(Color.white);
            g2d.draw(path);
        }
        g2d.setColor(Color.yellow);
        g2d.drawLine((int)(this.getWidth()/2)-5, (int)(this.getHeight()/2), (int)(this.getWidth()/2)+5, (int)(this.getHeight()/2));
        g2d.drawLine((int)(this.getWidth()/2), (int)(this.getHeight()/2)-5, (int)(this.getWidth()/2), (int)(this.getHeight()/2)+5);
    }

}
