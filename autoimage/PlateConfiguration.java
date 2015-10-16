package autoimage;

/**
 *
 * @author Karsten Siller
 */
public class PlateConfiguration {
    public String name;
    public String fileLocation;
    public double width;
    public double length;
    public double height;
    public int columns;
    public int rows;
    public double distLeftEdgeToA1;
    public double distTopEdgeToA1;
    public double wellDiameter;
    public double wellDistance;
    public double wellDepth;
    public double bottomThickness;
    public String bottomMaterial;
    public String wellShape;

    public PlateConfiguration() {
        name="NewPlate";
        fileLocation="";
        width=127760; //ANSI/SLAS-1 2004; former ANSI/SBS-1 20014
        length=85480; //ANSI/SLAS-1 2004; former ANSI/SBS-1 20014
        height=14350; //ANSI/SLAS-2 2004; former ANSI/SBS-2 20014
        columns=1;
        rows=1;
        distLeftEdgeToA1=0;
        distTopEdgeToA1=0;
        wellDiameter=0;
        wellDistance=0;
        wellDepth=0;
        bottomThickness=0.17;
        bottomMaterial="Glass";
        wellShape="Circle";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PlateConfiguration)) {
            return false;
        }
        PlateConfiguration config = (PlateConfiguration) obj;
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
        if (wellDistance!=config.wellDistance) {
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
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.wellDistance) ^ (Double.doubleToLongBits(this.wellDistance) >>> 32));
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.wellDepth) ^ (Double.doubleToLongBits(this.wellDepth) >>> 32));
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.bottomThickness) ^ (Double.doubleToLongBits(this.bottomThickness) >>> 32));
        hash = 59 * hash + (this.bottomMaterial != null ? this.bottomMaterial.hashCode() : 0);
        hash = 59 * hash + (this.wellShape != null ? this.wellShape.hashCode() : 0);
        return hash;
    }
}

