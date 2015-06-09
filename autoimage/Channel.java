package autoimage;

import java.awt.Color;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten Siller
 */
public class Channel {
    
    private String name;
    private double exposure;
    private double zOffset;
    private Color color;
    
    public final static String TAG_CHANNEL_ARRAY = "CHANNEL_ARRAY";
    public final static String TAG_CHANNEL = "CHANNEL";
    public final static String TAG_NAME = "NAME";
    public final static String TAG_EXPOSURE = "EXPOSURE";
    public final static String TAG_Z_OFFSET = "Z_OFFSET";
    public final static String TAG_COLOR = "COLOR";
    
    
    public Channel (String ch, double exp, double zo, Color c) {
        name=ch;
        exposure=exp;
        zOffset=zo;
        color=c;
    }
    
    public Channel(JSONObject obj) throws JSONException {
        if (obj!=null) {
            name=obj.getString(TAG_NAME);
            exposure=obj.getDouble(TAG_EXPOSURE);
            zOffset=obj.getDouble(TAG_Z_OFFSET);
            color=new Color(obj.getInt(TAG_COLOR));
        }
    }
    
    public JSONObject toJSONObject() throws JSONException {
        JSONObject obj=new JSONObject();
        obj.put(TAG_NAME, name);
        obj.put(TAG_EXPOSURE, exposure);
        obj.put(TAG_Z_OFFSET, zOffset);
        obj.put(TAG_COLOR, color.getRGB());
        return obj;
    }
    
    public Channel duplicate() {
        Channel c = new Channel(this.getName(),this.getExposure(),this.getZOffset(), this.getColor());
        return c;
    }
    
    public static String colorToHexString(Color c) throws NullPointerException {
        String hexColStr = Integer.toHexString(c.getRGB() & 0xffffff);
        if (hexColStr.length() < 6) {
            hexColStr = "000000".substring(0, 6 - hexColStr.length()) + hexColStr;
        }
        return "#" + hexColStr;
    } 
    
    public String getName() {
        return name;
    }
    
    public boolean equals(Channel c) {
        return name.equals(c.getName());
    }
    
    public double getExposure() {
        return exposure;
    }
    
    public double getZOffset() {
        return zOffset;
    }
    
    public Color getColor() {
        return color;
    }
    
    public void setName(String ch) {
        name=ch;
    }

    public void setExposure(double exp) {
        exposure=exp;
    }

    public void setZOffset(double zo) {
        zOffset=zo;
    }

    public void setColor(Color c) {
        color=c;
    }
}
