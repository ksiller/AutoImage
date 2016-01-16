package autoimage.data.layout;

import ij.IJ;

/**
 *
 * @author Karsten Siller
 */
public class WellPlateConfig {
    private final String name;
    private final String fileLocation;
    private final double width;
    private final double length;
    private final double height;
    private final int columns;
    private final int rows;
    private final double distLeftEdgeToA1;
    private final double distTopEdgeToA1;
    private final double wellDiameter;
    private final double wellToWellDistance;
    private final double wellDepth;
    private final double bottomThickness;
    private final String bottomMaterial;
    private final String wellShape;
    
    private WellPlateConfig(String n, String file, double w, double l, double h, int col, int rws, double distLeftEdge, double distTopEdge, double wellDiam, double wellDist, double wellDpth, double bttmThick, String bttmMat, String shape) {
        name=n;
        fileLocation=file;
        width=w;
        length=l;
        height=h;
        columns=col;
        rows=rws;
        distLeftEdgeToA1=distLeftEdge;
        distTopEdgeToA1=distTopEdge;
        wellDiameter=wellDiam;
        wellToWellDistance=wellDist;
        wellDepth=wellDpth;
        bottomThickness=bttmThick;
        bottomMaterial=bttmMat;
        wellShape=shape;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WellPlateConfig)) {
            return false;
        }
        WellPlateConfig config = (WellPlateConfig) obj;
        if (!name.equals(config.name)) {
            return false;
        }
        if (!fileLocation.equals(config.fileLocation)) {
            return false;
        }
        if (width!=config.width) {
            return false;
        } 
        if (length!=config.length) {
            return false;
        } 
        if (height!=config.height) {
            return false;
        } 
        if (columns!=config.columns) {
            return false;
        } 
        if (rows!=config.rows) {
            return false;
        } 
        if (distLeftEdgeToA1!=config.distLeftEdgeToA1) {
            return false;
        } 
        if (distTopEdgeToA1!=config.distTopEdgeToA1) {
            return false;
        } 
        if (wellDiameter!=config.wellDiameter) {
            return false;
        } 
        if (wellToWellDistance!=config.wellToWellDistance) {
            return false;
        } 
        if (wellDepth!=config.wellDepth) {
            return false;
        } 
        if (bottomThickness!=config.bottomThickness) {
            return false;
        } 
        if (!bottomMaterial.equals(config.bottomMaterial)) {
            return false;
        }
        if (!wellShape.equals(config.wellShape)) {
            return false;
        }
        return true;
    } 

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 59 * hash + (this.fileLocation != null ? this.fileLocation.hashCode() : 0);
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.width) ^ (Double.doubleToLongBits(this.width) >>> 32));
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.length) ^ (Double.doubleToLongBits(this.length) >>> 32));
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.height) ^ (Double.doubleToLongBits(this.height) >>> 32));
        hash = 59 * hash + this.columns;
        hash = 59 * hash + this.rows;
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.distLeftEdgeToA1) ^ (Double.doubleToLongBits(this.distLeftEdgeToA1) >>> 32));
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.distTopEdgeToA1) ^ (Double.doubleToLongBits(this.distTopEdgeToA1) >>> 32));
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.wellDiameter) ^ (Double.doubleToLongBits(this.wellDiameter) >>> 32));
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.wellToWellDistance) ^ (Double.doubleToLongBits(this.wellToWellDistance) >>> 32));
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.wellDepth) ^ (Double.doubleToLongBits(this.wellDepth) >>> 32));
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.bottomThickness) ^ (Double.doubleToLongBits(this.bottomThickness) >>> 32));
        hash = 59 * hash + (this.bottomMaterial != null ? this.bottomMaterial.hashCode() : 0);
        hash = 59 * hash + (this.wellShape != null ? this.wellShape.hashCode() : 0);
        return hash;
    }

    public Builder copy() {
        Builder builder = new Builder()
                .name(this.name)
                .fileLocation(this.fileLocation)
                .width(this.width)
                .length(this.length)
                .height(this.height)
                .columns(this.columns)
                .rows(this.rows)
                .distLeftEdgeToA1Center(this.distLeftEdgeToA1)
                .distTopEdgeToA1Center(this.distTopEdgeToA1)
                .wellDiameter(this.wellDiameter)
                .wellToWellDistance(this.wellToWellDistance)
                .wellDepth(this.wellDepth)
                .bottomThickness(this.bottomThickness)
                .bottomMaterial(this.bottomMaterial)
                .wellShape(this.wellShape);
        return builder;
    }

    public String getName() {
        return name;
    }

    public String getFileLocation() {
        return fileLocation;
    }

    public double getWidth() {
        return width;
    }

    public double getLength() {
        return length;
    }

    public double getHeight() {
        return height;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public double getDistLeftEdgeToA1Center() {
        return distLeftEdgeToA1;
    }

    public double getDistTopEdgeToA1Center() {
        return distTopEdgeToA1;
    }

    public String getBottomMaterial() {
        return bottomMaterial;
    }

    public double getBottomThickness() {
        return bottomThickness;
    }

    public String getWellShape() {
        return wellShape;
    }

    public double getWellDepth() {
        return wellDepth;
    }

    public double getWellDiameter() {
        return wellDiameter;
    }

    public double getWellToWellDistance() {
        return wellToWellDistance;
    }
    
    public static class Builder {
        
        private String name = "New_Plate.txt";
        private String fileLocation = "";
        private double width = 127760; //in um; ANSI/SLAS-1 2004; former ANSI/SBS-1 20014;
        private double length = 85480; //in um; ANSI/SLAS-1 2004; former ANSI/SBS-1 20014;
        private double height = 14350; //in um; ANSI/SLAS-2 2004; former ANSI/SBS-2 20014;
        private int columns = 12;
        private int rows = 8;
        private double distLeftEdgeToA1 = 0; //in um
        private double distTopEdgeToA1 = 0; //in um
        private double wellDiameter = 0; //in um
        private double wellDistance = 0; //in um
        private double wellDepth = 0; //in um
        private double bottomThickness = 170; //in um
        private String bottomMaterial = "Glass";
        private String wellShape = "Circle";
        
        public Builder() {
        }
        
        public Builder name(String n) {
            name=n;
            return this;
        }
        
        public Builder fileLocation(String path) {
            fileLocation=path;
            return this;
        }
        
        public Builder width(double w) {
            width=w;
            return this;
        }
        
        public Builder length(double l) {
            length=l;
            return this;
        }
        
        public Builder height(double h) {
            height=h;
            return this;
        }
        
        public Builder columns(int col) {
            columns=col;
            return this;
        }
        
        public Builder rows(int rws) {
            rows=rws;
            return this;
        }
        
        public Builder distLeftEdgeToA1Center(double dist) {
            distLeftEdgeToA1=dist;
            return this;
        }

        public Builder distTopEdgeToA1Center(double dist) {
            distTopEdgeToA1=dist;
            return this;
        }
        
        public Builder wellDiameter(double diam) {
            wellDiameter=diam;
            return this;
        }
        
        public Builder wellToWellDistance(double dist) {
            wellDistance=dist;
            return this;
        }
        
        public Builder wellDepth(double depth) {
            wellDepth=depth;
            return this;
        }
        
        public Builder bottomThickness(double thickness) {
            bottomThickness=thickness;
            return this;
        }
        
        public Builder bottomMaterial(String material) {
            bottomMaterial=material;
            return this;
        }
        
        public Builder wellShape(String shape) {
            wellShape=shape;
            return this;
        }
        
        public WellPlateConfig build() {
            WellPlateConfig config = new WellPlateConfig(
                name,
                fileLocation,
                width,
                length,
                height,
                columns,
                rows,
                distLeftEdgeToA1,
                distTopEdgeToA1,
                wellDiameter,
                wellDistance,
                wellDepth,
                bottomThickness,
                bottomMaterial,
                wellShape
            );
            return config;
        }
             
    }
}

