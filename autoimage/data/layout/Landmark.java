package autoimage.data.layout;

import autoimage.data.Vec3d;
//import ij.IJ;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONException;
import org.json.JSONObject;


/**
 *
 * @author Karsten Siller
 */
public class Landmark {

    private final long serialNo;
    private final String name;
    private final double stageX; // center of Landmark
    private final double stageY; // center of Landmark
    private final double stageZ; // center of Landmark
    private final double layoutCoordX; //center of Landmark
    private final double layoutCoordY; //center of Landmark
    private final double layoutCoordZ; //offset from layout reference plane
    private final double physWidth;
    private final double physHeight;
    private final String refImageFile;
    private final Object image;
    //operational
    private final boolean stagePosMapped; //false unless stagePos mapped to Layout
//    private final boolean selected; //used to highlight landmark in LayoutPanel
//    private final boolean zDefined;
//    private final boolean changed;
//    private static double stageToLayoutRot=0;//in radians
    
    public final static String TAG_NAME="NAME";
    public final static String TAG_STAGE_X="STAGE_X";
    public final static String TAG_STAGE_Y="STAGE_Y";
    public final static String TAG_STAGE_Z="STAGE_Z";
    public final static String TAG_WIDTH="WIDTH";
    public final static String TAG_HEIGHT="HEIGHT";
    public final static String TAG_LAYOUT_X="LAYOUT_X";
    public final static String TAG_LAYOUT_Y="LAYOUT_Y";
    public final static String TAG_LAYOUT_Z="LAYOUT_Z";
    public final static String TAG_IMAGE_FILE="IMAGE_FILE_NAME";
    public final static String TAG_IMAGE_DATA="IMAGE_DATA";
    public final static String TAG_Z_POS_DEFINED="Z_POS_DEFINED";
    public final static String TAG_LANDMARK_ARRAY="LANDMARK_ARRAY";

    private Landmark(long no, String n, double sX, double sY, double sZ, double lX, double lY, double lZ, double physW, double physH, String rImage, Object img, boolean mapped) {
        serialNo=no;
        name=n;
        stageX=sX;
        stageY=sY;
        stageZ=sZ;
        layoutCoordX=lX;
        layoutCoordY=lY;
        layoutCoordZ=lZ;
        physWidth=physW;
        physHeight=physH;
        refImageFile=rImage;
        image=img;
        
        stagePosMapped=mapped;
//        selected=false;
//      zDefined=true;
//        changed=false;
//        cameraRot=FieldOfView.ROTATION_UNKNOWN;
    }
    
    public Builder copy () {
        Builder builder = new Builder()
            .setName(name)
            .setStageCoord(stageX, stageY, stageZ)
            .setLayoutCoord(layoutCoordX, layoutCoordY, layoutCoordZ)
            .setPhysDimension(physWidth, physHeight)
            .setRefImageFile(refImageFile)
            .setImage(image)
            .setStagePosMapped(stagePosMapped);
//            .setSelected(selected);
        return builder;
//        zDefined=rp.isZPosDefined();
//        changed=rp.isChanged();
//        cameraRot=FieldOfView.ROTATION_UNKNOWN;
    }
    
    public JSONObject toJSONObject() throws JSONException {
        JSONObject obj=new JSONObject();
        obj.put(TAG_NAME,name);
        obj.put(TAG_STAGE_X,stageX);
        obj.put(TAG_STAGE_Y,stageY);
        obj.put(TAG_STAGE_Z,stageZ);
        obj.put(TAG_LAYOUT_X,layoutCoordX);
        obj.put(TAG_LAYOUT_Y,layoutCoordY);
        obj.put(TAG_LAYOUT_Z,layoutCoordZ);
        obj.put(TAG_WIDTH,physWidth);
        obj.put(TAG_HEIGHT,physHeight);
        obj.put(TAG_IMAGE_FILE, refImageFile);
        obj.put(TAG_IMAGE_DATA, image);
//        obj.put(TAG_Z_POS_DEFINED,zDefined);
        return obj;
    }
    

/*
    public static void setCameraRot(double rot) {
        cameraRot=rot;
    }
    
    public static double getCameraRot() {
        return cameraRot;
    }
*/   
    
    public long getSerialNo() {
        return serialNo;
    }
    
    public boolean isStagePosMapped() {
        return stagePosMapped;
    }

/*    
    public boolean isSelected() {
        return selected;
    }
*/
    public String getName() {
        return name;
    }
    
    public double getStageCoordX() {
        return stageX;
    }
    
    public double getStageCoordY() {
        return stageY;
    }
    
    public double getStageCoordZ() {
        return stageZ;
    }
    
    public Vec3d getStageVector() {
        return new Vec3d(stageX+physWidth/2, stageY+physHeight/2, stageZ-layoutCoordZ);
    }
    
    public double getLayoutCoordX() {
        return layoutCoordX;
    }
    
    public double getLayoutCoordY() {
        return layoutCoordY;
    }
    
    public double getLayoutCoordZ() {
        return layoutCoordZ;
    }
        
    public double getPhysWidth() {
        return physWidth;
    }
    
    public double getPhysHeight() {
        return physHeight;
    }
    
    public String getRefImageFile() {
        return refImageFile;
    }
    
    public Object getImage() {
        return image;
    }
    
/*    public double getStageToLayoutRot() {
        return stageToLayoutRot;
    }*/
    
/*    public boolean isZPosDefined() {
        return zDefined;
    }
*/    
    
/*    public void setChanged(boolean b) {
        changed=b;
    }
    
    public boolean isChanged() {
        return changed;
    }
*/
    
    
    public static class Builder {
        
        private static AtomicLong serialNo=new AtomicLong(0);
        
        private long no;
        private double stageX=0; // center of Landmark
        private double stageY=0; // center of Landmark
        private double stageZ=0; // center of Landmark
        private double physWidth=0;
        private double physHeight=0;
        private double layoutCoordX=0; //center of Landmark
        private double layoutCoordY=0; //center of Landmark
        private double layoutCoordZ=0; //offset from layout flat bottom plane
        private boolean zDefined=true;
        private String name="Landmark";
        private String refImageFile="";
        private Object image=null;
    //    private final boolean changed;
        private boolean stagePosMapped=false; //false unless stagePos mapped to Layout
    //    private boolean selected=false; //used to highlight landmark in LayoutPanel
        private static double stageToLayoutRot=0;//in radians        
        
        public Builder () {
        }
        
        public Builder(JSONObject obj) throws JSONException {
            String jsonExceptionStr="";
            try {
                name=obj.getString(TAG_NAME);
            } catch (JSONException ex) {
                jsonExceptionStr+=TAG_NAME+",";
            }
            try {
                stageX=obj.getDouble(TAG_STAGE_X);
            } catch (JSONException ex) {
                jsonExceptionStr+=TAG_STAGE_X+",";
            }
            try {
                stageY=obj.getDouble(TAG_STAGE_Y);
            } catch (JSONException ex) {
                jsonExceptionStr+=TAG_STAGE_Y+",";
            }
            try {
                stageZ=obj.getDouble(TAG_STAGE_Z);
            } catch (JSONException ex) {
                jsonExceptionStr+=TAG_STAGE_Z+",";
            }
            try {
                layoutCoordX=obj.getDouble(TAG_LAYOUT_X);
            } catch (JSONException ex) {
                jsonExceptionStr+=TAG_LAYOUT_X+",";
            }
            try {
                layoutCoordY=obj.getDouble(TAG_LAYOUT_Y);
            } catch (JSONException ex) {
                jsonExceptionStr+=TAG_LAYOUT_Y+",";
            }
            try {
                layoutCoordZ=obj.getDouble(TAG_LAYOUT_Z);
            } catch (JSONException ex) {
                jsonExceptionStr+=TAG_LAYOUT_Z+",";
            }
            try {
                physWidth=obj.getDouble(TAG_WIDTH);
            } catch (JSONException ex) {
                jsonExceptionStr+=TAG_WIDTH+",";
            }
            try {
                physHeight=obj.getDouble(TAG_HEIGHT);
            } catch (JSONException ex) {
                jsonExceptionStr+=TAG_HEIGHT+",";
            }
            try {
                refImageFile=obj.getString(TAG_IMAGE_FILE);
            } catch (JSONException ex) {
                jsonExceptionStr+=TAG_IMAGE_FILE+",";
            }
            try {
                image=obj.get(TAG_IMAGE_DATA);
            }
            catch (JSONException ex) {
                jsonExceptionStr+=TAG_IMAGE_DATA+",";
            }
            if (!"".equals(jsonExceptionStr)) {
                throw new JSONException(jsonExceptionStr);
            }
       }        

        public Landmark build() {
            no=serialNo.getAndIncrement();
            Landmark r=new Landmark(
                no,
                name,
                stageX,
                stageY,
                stageZ,
                layoutCoordX,
                layoutCoordY,
                layoutCoordZ,
                physWidth,
                physHeight,
                refImageFile,
                image,
                stagePosMapped    
            );
            return r;
        }

        public Builder setName(String n) {
            name=n;
            return this;
        }
        
        public Builder setStageCoord(double sX, double sY, double sZ) {
            stageX=sX;
            stageY=sY;
            stageZ=sZ;
//            changed=true;
            return this;
        }

        public Builder setStageCoordX(double x) {
            stageX=x;
//            changed=true;
            return this;
        }

        public Builder setStageCoordY(double y) {
            stageY=y;
//            changed=true;
            return this;
        }

        public Builder setStageCoordZ(double z) {
            stageZ=z;
//            changed=true;
            return this;
        }

        public Builder setLayoutCoordX(double lx) {
            layoutCoordX=lx;
//            changed=true;
            return this;
        }

        public Builder setLayoutCoordY(double ly) {
            layoutCoordY=ly;
//            changed=true;
            return this;
        }

        public Builder setLayoutCoordZ(double lz) {
            layoutCoordZ=lz;
//            changed=true;
            return this;
        }

        public Builder setLayoutCoord(double lX, double lY, double lZ) {
            layoutCoordX=lX;
            layoutCoordY=lY;
            layoutCoordZ=lZ;
//            changed=true;
            return this;
        }

        public Builder setPhysDimension(double w, double h) {
            physWidth=w;
            physHeight=h;
            return this;
        }

        public Builder setRefImageFile(String rif) {
            refImageFile=rif;
            return this;
        }

        public Builder setImage(Object img) {
            image=img;
            return this;
        }
        
        public Builder setZDefined(boolean b) {
            zDefined=b;
            return this;
        }    

        public static void setStageToLayoutRot(double rot) {
            stageToLayoutRot=rot;
        }

        public Builder setStagePosMapped(boolean mapped) {
            stagePosMapped=mapped;
            return this;
        }

/*        public Builder setSelected(boolean b) {
            selected=b;
            return this;
        }
*/

    }
}
