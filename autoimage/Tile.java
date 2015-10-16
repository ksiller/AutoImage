package autoimage;

/**
 *
 * @author Karsten Siller
 */
public class Tile {
    static final String TAG_TILE_OVERLAP = "TILE_OVERLAP";
    static final String TAG_UNITS = "TILE_UNITS";
    static final String TAG_TILECONFIG = "TILE_CONFIG";
    //static final String TAG_SELECTED = "SELECTED";
    static final String TAG_PIX_SIZE = "TILE_PIX_SIZE";
    static final String TAG_TILE_HEIGHT = "TILE_HEIGHT";
    static final String TAG_TILE_WIDTH = "TILE_WIDTH";
    static final String TAG_IS_ABSOLUTE = "IS_ABSOLUTE";
        
    public String name;
    public double centerX;
    public double centerY;
    public double zPos;
    public boolean isAbsolute;
    
    public Tile(String n, double x, double y, double z, boolean absolute) {
        name=n;
        centerX=x;
        centerY=y;
        zPos=z;
        isAbsolute=absolute;
    }
             
    public static Tile createTile(String n, double x, double y, double z, boolean absolute) {
        return new Tile(n,x,y,z,absolute);
    }
      
}
