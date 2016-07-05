package autoimage.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Doxel: Dynamic Voxel
 * 
 * This class is used to define a 4d coordinate (3 spatial + 1 temporal). 
 * The properties field can be used to provide additional annotation.
 * 
 * @author Karsten Siller
 */
public class Doxel {
    
    //unknown unit def
    public static final String IS_UNKNOWN = "is_unknown";
    //unit def for spatial dim: xPos, yPos, zPos
    public static final String IS_PIXEL = "is_Pixel";
    public static final String IS_METER = "m";
    public static final String IS_CENTIMETER = "cm";
    public static final String IS_MILLIMETER = "mm";
    public static final String IS_MICROMETER = "um";
    public static final String IS_NANOMETER = "nm";
    public static final String IS_PICOMETER = "pm";
    //unit def for temporal dim: time
    public static final String IS_MICROSEC = "us";
    public static final String IS_MILLISEC = "ms";
    public static final String IS_SEC = "s";

    public static final String IDENTIFIER = "id";

    public double xPos;
    public double yPos;
    public double zPos;
    public double time;
    public String unitX;
    public String unitY;
    public String unitZ;
    public String unitTime;
    public Map<String,Object> properties;
    
    
    //initialize without additional properties
    public Doxel(double x, String uX, double y, String uY, double z, String uZ, double t, String uT) {
        this(x,uX,y,uY,z,uZ,t,uT,null);
    }
    
    //initialize using identical spatial unit dimension, no additional properties
    public Doxel(double x, double y, double z, String posUnit, double t, String uT) {
        this(x,posUnit,y,posUnit,z,posUnit,t,uT,null);
    }
    
    //initialize using identical spatial unit dimension,
    public Doxel(double x, double y, double z, String posUnit, double t, String uT, Map<String,Object> props) {
        this(x,posUnit,y,posUnit,z,posUnit,t,uT,props);
    }
    
    public Doxel(double x, String uX, double y, String uY, double z, String uZ, double t, String uT, Map<String, Object> props) {
        xPos=x;
        yPos=y;
        zPos=z;
        unitX=uX;
        unitY=uY;
        unitZ=uZ;
        time=t;
        unitTime=uT;
        if (props==null) {
            props=new HashMap<String,Object>();
        }
        properties=props;
    }
    
    public static Doxel duplicate(Doxel doxel) {
        Map<String,Object> props=new HashMap<String,Object>();
        if (doxel.properties!=null) {
            for (String key:doxel.properties.keySet()) {
                props.put(key, doxel.properties.get(key));
            }
        }
        return new Doxel(doxel.xPos,doxel.unitX, doxel.yPos, doxel.unitY, doxel.zPos, doxel.unitZ, doxel.time, doxel.unitTime,props);
    }
    
    @Override
    public String toString() {
        return toString(true);
    }
    
    public String toString(boolean includeProps) {
        String s= Double.toString(this.xPos) + " " + this.unitX+", "
                + Double.toString(this.yPos)+  " " + this.unitY+", "
                + Double.toString(this.zPos) +  " " + this.unitZ+", "
                + Double.toString(this.time) +  " " + this.unitTime;
        if (properties!=null && includeProps) {
            for (String prop:properties.keySet()) {
                s+=", "+prop+": "+this.properties.get(prop).toString();
            }    
        }
        return s;
    }
    
    private Double convertToMicron(double value, String unit) throws IllegalArgumentException {
        if (IS_PICOMETER.equals(unit)) {
            return value/=1000000;
        } else if (IS_NANOMETER.equals(unit)) {
            return value/=1000;
        } else if (IS_MILLIMETER.equals(unit)) {
            return value*=1000;
        } else if (IS_CENTIMETER.equals(unit)) {
            return value*=10000;
        } else if (IS_METER.equals(unit)) {
            return value*=1000000;
        }
        throw new IllegalArgumentException(unit+" cannot be converted");
    }

    public void put(String key, Object obj) {
        if (key==null || obj==null) { 
            return;
        }    
        if (properties==null) {
            properties=new HashMap<String,Object>();
        }
        properties.put(key, obj);
    }

    /**
     *
     * @param key
     * @return Object defined under key
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public Object get(String key) throws NullPointerException, IllegalArgumentException {
        if (properties==null) {
            throw new NullPointerException();
        }
        if (key==null || !properties.containsKey(key)) {
            throw new IllegalArgumentException ();
        }
        return properties.get(key);
    }
    
    /**
     *
     * @throws IllegalArgumentException
     */
    public void convertToMicron() throws IllegalArgumentException {
        convertToMicron(this.xPos, this.unitX);
        this.unitX=IS_MICROMETER;
        convertToMicron(this.yPos, this.unitY);
        this.unitY=IS_MICROMETER;
        convertToMicron(this.zPos, this.unitZ);
        this.unitZ=IS_MICROMETER;
    }
}
