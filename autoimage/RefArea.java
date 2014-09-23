/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import ij.IJ;
import org.json.JSONException;
import org.json.JSONObject;


/**
 *
 * @author Karsten Siller
 */
class RefArea {

    private double stageX; // center of RefArea
    private double stageY; // center of RefArea
    private double stageZ; // center of RefArea
    private double physWidth;
    private double physHeight;
    private double layoutCoordX; //center of RefArea
    private double layoutCoordY; //center of RefArea
    private double layoutCoordOrigX;
    private double layoutCoordOrigY;
    private double layoutCoordZ; //offset from layout flat bottom plane
    private boolean zDefined;
    private String name;
    private String refImageFile;
    private boolean changed;
    private boolean stagePosMapped; //false unless stagePos mapped to Layout; currently via RefPointListDialog
    private boolean selected; //used to highlight landmark in LayoutPanel
    private static double cameraRot; //in radians relative to x-y staga axis, NOT layout 
    
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
    public final static String TAG_Z_POS_DEFINED="Z_POS_DEFINED";
    public final static String TAG_LANDMARK_ARRAY="LANDMARK_ARRAY";

    public RefArea(String n, double sX, double sY, double sZ, double lX, double lY, double lZ, double physW, double physH, String rImage) {
        name=n;
        stageX=sX;
        stageY=sY;
        stageZ=sZ;
        physWidth=physW;
        physHeight=physH;
        setLayoutCoordX(lX);
        setLayoutCoordY(lY);
        layoutCoordZ=lZ;
        zDefined=true;
        refImageFile=rImage;
        changed=false;
        stagePosMapped=false;
        selected=false;
        cameraRot=0;
    }
    
    public RefArea(RefArea rp) {
        name=rp.getName();
        stageX=rp.getStageCoordX();
        stageY=rp.getStageCoordY();
        stageZ=rp.getStageCoordZ();
        physWidth=rp.getPhysWidth();
        physHeight=rp.getPhysHeight();
        setLayoutCoordX(rp.getLayoutCoordX());
        setLayoutCoordY(rp.getLayoutCoordY());
        layoutCoordZ=rp.getLayoutCoordZ();
        zDefined=rp.isZPosDefined();
        refImageFile=rp.getRefImageFile();
        changed=rp.isChanged();
        selected=rp.isSelected();
        stagePosMapped=rp.isStagePosFound();
        cameraRot=0;
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
        obj.put(TAG_Z_POS_DEFINED,zDefined);
        return obj;
    }

    public void initializeFromJSONObject(JSONObject obj) throws JSONException {
        IJ.log("initializing: "+this.getClass().getName());
        name=obj.getString(TAG_NAME);
        stageX=obj.getDouble(TAG_STAGE_X);
        stageY=obj.getDouble(TAG_STAGE_Y);
        stageZ=obj.getDouble(TAG_STAGE_Z);
        physWidth=obj.getDouble(TAG_WIDTH);
        physHeight=obj.getDouble(TAG_HEIGHT);
        setLayoutCoordX(obj.getDouble(TAG_LAYOUT_X));
        setLayoutCoordY(obj.getDouble(TAG_LAYOUT_Y));
        layoutCoordZ=obj.getDouble(TAG_LAYOUT_Z);
        refImageFile=obj.getString(TAG_IMAGE_FILE);
        zDefined=obj.getBoolean(TAG_Z_POS_DEFINED);
        changed=false;
        selected=false;
        stagePosMapped=false;
        cameraRot=0;
        IJ.log("initialization completed: "+this.getClass().getName());
    }
    
    public RefArea (JSONObject obj) throws JSONException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        initializeFromJSONObject(obj);
    }

    public static void setCameraRot(double rot) {
        cameraRot=rot;
    }
    
    public static double getCameraRot() {
        return cameraRot;
    }
    
    public void setStagePosMapped(boolean b) {
        stagePosMapped=b;
    }
    
    public boolean isStagePosFound() {
        return stagePosMapped;
    }
    
    public void setSelected(boolean b) {
        selected=b;
    }
    
    public boolean isSelected() {
        return selected;
    }

    public void setStageCoord(double sX, double sY, double sZ) {
        stageX=sX;
        stageY=sY;
        stageZ=sZ;
        changed=true;
    }
 
    public String getName() {
        return name;
    }
    
    public void setName(String n) {
        name=n;
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

    public double getLayoutCoordOrigX() {
        return layoutCoordOrigX;
    }
    
    public double getLayoutCoordOrigY() {
        return layoutCoordOrigY;
    }
    
    public void setLayoutCoordX(double lx) {
        layoutCoordX=lx;
        layoutCoordOrigX=lx-physWidth/2;
    }
    

    public void setLayoutCoordY(double ly) {
        layoutCoordY=ly;
        layoutCoordOrigY=ly-physHeight/2;
    }
    

    public void setLayoutCoordZ(double lz) {
        layoutCoordZ=lz;
    }
    
    public void setLayoutCoord(double lX, double lY, double lZ) {
        setLayoutCoordX(lX);
        setLayoutCoordY(lY);
        layoutCoordZ=lZ;
        changed=true;
    }
    
    public double getPhysWidth() {
        return physWidth;
    }
    
    public double getPhysHeight() {
        return physHeight;
    }
    
    public void setDimension(double w, double h) {
        physWidth=w;
        physHeight=h;
    }
    
    public String getRefImageFile() {
        return refImageFile;
    }
    
    public void setRefImageFile(String rif) {
        refImageFile=rif;
    }
    
    /*
    public double convertLayoutCoordToStageCoord_X (double lX) {
        return stageX+lX-layoutCoordX;
    }
    
    public double convertLayoutCoordToStageCoord_Y (double lY) {
        return stageY+lY-layoutCoordY;
    }
    
    public double convertStagePosToLayoutCoord_X (double sX) {
        return sX-stageX+layoutCoordX;
    }
    
    public double convertStagePosToLayoutCoord_Y (double sY) {
        return sY-stageY+layoutCoordY;
    }
*/
    public boolean isZPosDefined() {
        return zDefined;
    }
    
    public void setZDefined(boolean b) {
        zDefined=b;
    }
    
    public void setChanged(boolean b) {
        changed=b;
    }
    
    public boolean isChanged() {
        return changed;
    }
}
