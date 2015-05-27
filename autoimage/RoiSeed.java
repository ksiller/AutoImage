/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage;

/**
 *
 * @author Karsten
 */
public class RoiSeed {
    
    public static final String IS_PIXEL = "Is_Pixel";
    public static final String IS_CENTIMETER = "Is_CM";
    public static final String IS_MILLIMETER = "Is_MM";
    public static final String IS_MIROMETER = "Is_UM";
    public static final String IS_NANOMETER = "Is_NM";
    //public String areaName;
    //public long areaIndex;
    //public Long clusterIndex;
    //public Long siteIndex;
    //public Long posIndex;
    //public double voxelX_UM;
    public double xPos;
    public double yPos;
    public double zPos;
    public String unitXY;
    public String unitZ;
    
    public RoiSeed(double x, double y, String uXY, double z, String uZ) {
        xPos=x;
        yPos=y;
        zPos=z;
        unitXY=uXY;
        unitZ=uZ;
    }
}
