/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import java.awt.geom.Point2D;

/**
 *
 * @author Karsten
 */
public class Vec3d {
    public double x;
    public double y;
    public double z;
    
    public Vec3d(double x, double y, double z) {
        this.x=x;
        this.y=y;
        this.z=z;
    }
    
    public Vec3d() {
        this.x=0;
        this.y=0;
        this.z=0;
    }
    
    public static Vec3d cross(Vec3d v1, Vec3d v2) {
        return new Vec3d(v1.y*v2.z - v1.z*v2.y,
                v1.z*v2.x - v1.x*v2.z,
                v1.x*v2.y - v1.y*v2.x);
    }
    
    public static double dot(Vec3d v1, Vec3d v2) {
        return v1.x*v2.x + v1.y*v2.y + v1.z*v2.z;
    }
    
    @Override
    public String toString() {
        return "("+Double.toString(x)+"/"+Double.toString(y)+"/"+Double.toString(z)+")";
    }
    
    public double length() {
        return Math.sqrt(x*x+y*y+z*z);
    }
    
    public void negate() {
        x=x*(-1);
        y=y*(-1);
        z=z*(-1);
    }
    
    public void scale(double s) {
        x=x*s;
        y=y*s;
        z=z*s;
    }
    
    public Double angle(Vec3d v2) {
        return Math.acos(Vec3d.dot(this, v2)/(this.length()*v2.length()))/Math.PI*180;
    }
    
    public Vec3d minus(Vec3d v) throws Exception {
        if (v!=null)
            return new Vec3d(x-v.x,y-v.y,z-v.z);
        else 
            throw new Exception("Vec3d: argument is null");        
    }
 
}
