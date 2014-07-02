/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

/**
 *
 * @author Karsten
 */
public class Tile {
    static final String TAG_TILE_OVERLAP = "TILE_OVERLAP";
    static final String TAG_UNITS = "TILE_UNITS";
    static final String TILECONFIG_TAG = "TILE_CONFIG";
    //static final String TAG_SELECTED = "SELECTED";
    static final String TAG_PIX_SIZE = "TILE_PIX_SIZE";
    static final String TAG_TILE_HEIGHT = "TILE_HEIGHT";
    static final String TAG_TILE_WIDTH = "TILE_WIDTH";
        
    public String name;
    public double centerX;
    public double centerY;
    public double relZPos;
    
    public Tile(String n, double x, double y, double z) {
        name=n;
        centerX=x;
        centerY=y;
        relZPos=z;
    }
             
    public static Tile createTile(String n, double x, double y, double z) {
        return new Tile(n,x,y,z);
    }
      
}
