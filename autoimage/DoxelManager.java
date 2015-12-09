package autoimage;

import autoimage.api.IDoxelListener;
import autoimage.api.AcqSetting;
import autoimage.api.Doxel;
import autoimage.api.ExtImageTags;
import autoimage.api.IAcqLayout;
import ij.IJ;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Karsten Siller
 */
public class DoxelManager implements IDoxelListener {
    
    private Map<String,List<Doxel>> areaMap; //tile positions stored as layout coordinates
//    private IAcqLayout acqLayout;
    private AcqSetting acqSetting;
    
    public DoxelManager (IAcqLayout aLayout, AcqSetting aSetting) {
        areaMap=new HashMap<String,List<Doxel>>();
//        acqLayout=aLayout;
        acqSetting=aSetting;
    }
    
/*    public void setAcquisitionLayout(IAcqLayout aLayout) {
        acqLayout=aLayout;
    }
*/    
    public void setAcquisitionSetting(AcqSetting aSetting) {
        acqSetting=aSetting;
    }

    public synchronized Map<String,List<Doxel>> getAreaMap() {
        return areaMap;
    }
    
    public synchronized List<Doxel> getDoxels(String area) {
        if (areaMap!=null && areaMap.containsKey(area))
            return areaMap.get(area);
        else
            return null;
    }    
    
    public synchronized void clearList() {
        areaMap.clear();
    }
    
    public synchronized void consolidateDoxelsIn2D(double percOverlap, boolean ignoreZ) {
        if (percOverlap <0 || percOverlap >1) {
            return;
        }
        if (acqSetting!=null) {
            FieldOfView fov=acqSetting.getFieldOfView();
            double maxDeltaX=(1-percOverlap)*fov.getRoiWidth_UM(acqSetting.getObjPixelSize());
            double maxDeltaY=(1-percOverlap)*fov.getRoiHeight_UM(acqSetting.getObjPixelSize());
//            List<List<Tile>> groups=new ArrayList<List<Tile>>();
            Iterator it=areaMap.keySet().iterator();
            while (it.hasNext()) {
                List<Doxel> newDoxelListForArea = new ArrayList<Doxel>();
                String areaName=(String)it.next();
                List<Doxel> areaDoxelList=areaMap.get(areaName);
                IJ.log(areaName.toUpperCase());
                for (Doxel d:areaDoxelList) {
                    IJ.log(d.properties.get(Doxel.IDENTIFIER)+", "+Double.toString(d.xPos) +", "+Double.toString(d.yPos)+", "+Double.toString(d.zPos));
                }
                int number=0;
                while (areaDoxelList.size()>0) {
                    double shortestDist=-1;
                    List<Doxel> consolidatedList=new ArrayList<Doxel>();
                    Doxel firstDoxel=null;
                    Doxel secondDoxel=null;
                    //iterate through list and find two tiles closest to each other
                    for (int j=0; j<areaDoxelList.size(); j++) {
                        Doxel d1=areaDoxelList.get(j);
                        for (int i=j+1; i<areaDoxelList.size(); i++) {
                            Doxel d2=areaDoxelList.get(i);
                            double deltax=d2.xPos-d1.xPos;
                            double deltay=d2.yPos-d1.yPos;
                            double dist=Math.sqrt(deltax*deltax * deltay*deltay);
                            if ((deltax < maxDeltaX && deltay < maxDeltaY) && (shortestDist==-1 || dist < shortestDist)) {
                                shortestDist=dist;
                                firstDoxel=d1;
                                secondDoxel=d2;
                            }
                        }
                    }
                    if (firstDoxel!=null)
                    IJ.log("   First Doxel: "+(firstDoxel!=null ? firstDoxel.toString(false) : "null"));
                    IJ.log("   Second Doxel: "+(secondDoxel!=null ? secondDoxel.toString(false) : "null"));
                    //if first and second tile don'd fit in single FOV, then further consolidation impossible
                    if (firstDoxel==null || secondDoxel==null 
                            || firstDoxel.zPos!=secondDoxel.zPos
                            || Math.abs(firstDoxel.xPos-secondDoxel.xPos) > maxDeltaX
                            || Math.abs(firstDoxel.yPos-secondDoxel.yPos) > maxDeltaY) {
                        IJ.log("   Cannot fit into single FOV");    
                        for (int i=areaDoxelList.size()-1; i>=0; i--) {
                            newDoxelListForArea.add(areaDoxelList.get(i));
                            areaDoxelList.remove(i);
                        }
                    } else {
                        IJ.log("   Fit into single FOV");    
                        consolidatedList.add(firstDoxel);
                        consolidatedList.add(secondDoxel);
                        double minX=Math.min(firstDoxel.xPos,secondDoxel.xPos);
                        double maxX=Math.max(firstDoxel.xPos,secondDoxel.xPos);
                        double minY=Math.min(firstDoxel.yPos,secondDoxel.yPos);
                        double maxY=Math.max(firstDoxel.yPos,secondDoxel.yPos);
                        IJ.log("ConsolidatedList.size()="+Integer.toString(consolidatedList.size()));
                        for (Doxel d:areaDoxelList) {
                            //only add unique tiles that fit into single FOV
                            if (!consolidatedList.contains(d)
                                        && d.xPos < minX+maxDeltaX 
                                        && d.xPos > maxX-maxDeltaX 
                                        && d.yPos < minY+maxDeltaY
                                        && d.yPos > maxY-maxDeltaY
                                        && d.zPos == firstDoxel.zPos) { 
                                consolidatedList.add(d);
                                //find new minx, maxx, miny, maxy in consolidated list
                                for (int i=0; i<consolidatedList.size(); i++) {
                                    Doxel temp=consolidatedList.get(i);
                                    minX=Math.min(minX,temp.xPos);
                                    maxX=Math.max(maxX,temp.xPos);
                                    minY=Math.min(minY,temp.yPos);
                                    maxY=Math.max(maxY,temp.yPos);
                                }
                                IJ.log("ConsolidatedList.size()="+Integer.toString(consolidatedList.size()));
                            }    
                        }
                        Doxel d=new Doxel((minX+maxX)/2, firstDoxel.unitX,(minY+maxY)/2, firstDoxel.unitY,firstDoxel.zPos,firstDoxel.unitX, firstDoxel.time, firstDoxel.unitTime);
                        d.put(Doxel.IDENTIFIER,areaName+"-"+Integer.toString(number++));
                        d.put(ExtImageTags.AREA_NAME, areaName);
                        newDoxelListForArea.add(d);
                        //remove consolidated tiles from original list
                        for (int i=consolidatedList.size()-1; i>=0; i--) {
                            areaDoxelList.remove(consolidatedList.get(i));
                        }
                    }
                }
                areaMap.put(areaName, newDoxelListForArea);
                IJ.log(areaName.toUpperCase()+" Consolidated");
                for (Doxel d:areaMap.get(areaName)) {
                    IJ.log(d.toString(true));
                }
            }
        }        
    }
    
    
    //receives 4D coordinate (doxel) and groups them by area name if possible
    @Override
    public void doxelAdded(Doxel doxel, Object source) {
//        Vec3d lCoord;
/*        
        try {
        */
            //convert spatial doxel coordinates into micron
            try {
                doxel.convertToMicron();
            } catch (IllegalArgumentException ie) {
                
            }
            
            String areaName="all";//default in case it cannot be parsed
            try {
                areaName=(String)doxel.get(ExtImageTags.AREA_NAME);
            } catch (Exception e) {
            }
            List doxelList=areaMap.get(areaName);

            /*           
            //convert absolute doxel coordinates into logical layout coordinates
            lCoord = acqLayout.convertStageToLayoutPos(doxel.xPos, doxel.yPos, doxel.zPos);            
            doxel.xPos=lCoord.x;
            doxel.yPos=lCoord.y;
            doxel.zPos=lCoord.z;
            */
            
            if (doxelList!=null) {
                doxelList.add(doxel);
            } else {
                doxelList=new ArrayList<Doxel>();
                doxelList.add(doxel);
                areaMap.put(areaName,doxelList);
            }    
            //add annotation
            doxel.put(ExtImageTags.AREA_NAME, areaName);
            doxel.put(Doxel.IDENTIFIER,areaName+"-Doxel "+Integer.toString(doxelList.size()));
            IJ.log(this.getClass().getName()+": add to doxelList for area "+areaName+": "+doxel.toString(false));//+", layout:"+lCoord.x+", "+lCoord.y+", "+lCoord.z);
/*        
        } catch (Exception ex) {
            IJ.log(this.getClass().getName()+": error converting stage position to layout position for area "+doxel.toString(false));
            Logger.getLogger(DoxelManager.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }

    @Override
    public void doxelListAdded(List<Doxel> doxelList, Object source) {
        for (Doxel doxel:doxelList) {
            doxelAdded(doxel,source);
        }
    }

}
