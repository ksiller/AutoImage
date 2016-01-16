package autoimage.data.layout;

/**
 *
 * @author Karsten Siller
 */
public class Tile {
    public static final String TAG_TILE_OVERLAP = "TILE_OVERLAP";
    public static final String TAG_UNITS = "TILE_UNITS";
    public static final String TAG_TILECONFIG = "TILE_CONFIG";
    //static final String TAG_SELECTED = "SELECTED";
    public static final String TAG_PIX_SIZE = "TILE_PIX_SIZE";
    public static final String TAG_TILE_HEIGHT = "TILE_HEIGHT";
    public static final String TAG_TILE_WIDTH = "TILE_WIDTH";
    public static final String TAG_IS_ABSOLUTE = "IS_ABSOLUTE";
        
    private final String name;
    private final double centerX;
    private final double centerY;
    private final double zPos;
    private final boolean isAbsolute;
    
    
    private Tile(String n, double x, double y, double z, boolean absolute) {
        name=n;
        centerX=x;
        centerY=y;
        zPos=z;
        isAbsolute=absolute;
    }
    
    public Builder copy() {
        Builder builder=new Builder (this.name)
                .center(this.centerX, this.centerY)
                .zPosition(this.zPos)
                .absoluteCoordinates(this.isAbsolute);
        return builder;
    }
    
    @Override
    public String toString() {
        return "Name: "+name+", centerX="+Double.toString(centerX)+", centerY="+Double.toString(centerY)+", z="+Double.toString(zPos)+", isAbsolute="+Boolean.toString(isAbsolute);
    }
    
    public String getName() {
        return name;
    }
    
    public double getCenterXPos() {
        return centerX;
    }
    
    public double getCenterYPos() {
        return centerY;
    }
    
    public double getZPos() {
        return zPos;
    }
    
    public boolean isAbsolute() {
        return isAbsolute;
    }
            
    
    /**
     * Factory to create Tile objects
     */
    public static class Builder {
        
        private final String name;
        private double centerX=0;
        private double centerY=0;
        private double zPos=0;
        private boolean isAbsolute=true;
    
    
        public Builder (String name) {
            this.name=name;
        }
        
        public Builder center(double x, double y) {
            this.centerX=x;
            this.centerY=y;
            return this;
        }
        
        public Builder zPosition(double zPos) {
            this.zPos=zPos;
            return this;
        }
        
        public Builder absoluteCoordinates(boolean absolute) {
            this.isAbsolute=absolute;
            return this;
        }

        public Tile build() {
            return new Tile(this.name,this.centerX,this.centerY,this.zPos,this.isAbsolute);
        }
    }
     
      
}
