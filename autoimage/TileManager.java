/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import ij.IJ;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Karsten Siller
 */
public class TileManager implements IStagePosListener {
    
    private Map<String,List<Tile>> areaMap; //tile positions stored as layout coordinates
    private AcqLayout acqLayout;
    private AcqSetting acqSetting;
//    protected boolean finalized;
    
    public TileManager (AcqLayout aLayout, AcqSetting aSetting) {
        areaMap=new HashMap<String,List<Tile>>();
        acqLayout=aLayout;
        acqSetting=aSetting;
//        finalized=false;
    }
    
/*    public void setFinalized(boolean b) {
        finalized=b;
    }
    
    public boolean isFinalized() {
        return finalized;
    }
*/    
    public void setAcquisitionLayout(AcqLayout aLayout) {
        acqLayout=aLayout;
    }
    
    public void setAcquisitionSetting(AcqSetting aSetting) {
        acqSetting=aSetting;
    }
/*    
    //expects Tile with stage coordinates and places them in map binned by area name
    public synchronized void addStagePosToTileList(String areaName, Tile sTile) {
//        RefArea rp = acqLayout.getLandmark(0);
//        if (rp!=null) {
            //convert stage pos to layout coord and place in map
            //needs to be updated to use affine transform
            Vec3d lCoord = acqLayout.convertStagePosToLayoutPos(sTile.centerX, sTile.centerY, sTile.relZPos);
            Tile lTile=new Tile(sTile.name,lCoord.x,lCoord.y,lCoord.z);
            List tileList=areaMap.get(areaName);
            if (tileList!=null) {
                IJ.log("add to tileList for area: "+areaName+", stage:"+sTile.centerX+","+sTile.centerY+", "+sTile.relZPos+" layout:"+lCoord.x+", "+lCoord.y+", "+lCoord.z);
                tileList.add(lTile);
            } else {
                IJ.log("new tileList for area: "+areaName+", stage:"+sTile.centerX+","+sTile.centerY+", "+sTile.relZPos+" layout:"+lCoord.x+", "+lCoord.y+", "+lCoord.z);
                tileList=new ArrayList();
                tileList.add(lTile);
                areaMap.put(areaName,tileList);
            }
//            IJ.log("RuntimeTileManager: refArea==null");
//        }
    }
*/    
    public synchronized Map<String,List<Tile>> getAreaMap() {
        return areaMap;
    }
    
    public synchronized List<Tile> getTiles(String area) {
        if (areaMap!=null && areaMap.containsKey(area))
            return areaMap.get(area);
        else
            return null;
    }    
    
    public synchronized void clearList() {
        areaMap.clear();
    }
    /*
    public void convertStagePosMap(AcqLayout acqLayout) {
        RefArea rp = acqLayout.getLandmark(0);
        if (rp!=null) {
            Iterator it = seedMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry)it.next();
                List<Tile> sList=(List<Tile>)pairs.getValue();
                List<Tile> lList=new ArrayList<Tile>(sList.size());
                for (Tile sTile:sList) {
                    Tile lTile=new Tile(sTile.name,
                            rp.convertStagePosToLayoutCoord_X(sTile.centerX),
                            rp.convertStagePosToLayoutCoord_Y(sTile.centerY),
                            0);
                    lList.add(lTile);
                }
                seedMap.put((String)pairs.getKey(), lList);
                it.remove(); // avoids a ConcurrentModificationException
            }
        }
    }*/

    public synchronized void consolidateTiles(double percOfEdge) {
        if (percOfEdge <0 || percOfEdge >1) {
            return;
        }
        if (acqSetting!=null) {
            FieldOfView fov=acqSetting.getFieldOfView();
            double maxDeltaX=(1-percOfEdge)*fov.getRoiWidth_UM(acqSetting.getObjPixelSize());
            double maxDeltaY=(1-percOfEdge)*fov.getRoiHeight_UM(acqSetting.getObjPixelSize());
//            List<List<Tile>> groups=new ArrayList<List<Tile>>();
            Iterator it=areaMap.keySet().iterator();
            while (it.hasNext()) {
                List<Tile> newTileListForArea = new ArrayList<Tile>();
                String areaName=(String)it.next();
                List<Tile> areaTileList=areaMap.get(areaName);
                IJ.log(areaName.toUpperCase());
                for (Tile t:areaTileList) {
                    IJ.log(t.name+", "+Double.toString(t.centerX) +", "+Double.toString(t.centerY)+", "+Double.toString(t.relZPos));
                }
                while (areaTileList.size()>0) {
                    double shortestDist=-1;
                    List<Tile> consolidatedList=new ArrayList<Tile>();
                    Tile firstTile=null;
                    Tile secondTile=null;
                    //iterate through list and find two tiles closest to each other
                    for (int j=0; j<areaTileList.size(); j++) {
                        Tile t1=areaTileList.get(j);
                        for (int i=j+1; i<areaTileList.size(); i++) {
                            Tile t2=areaTileList.get(i);
                            double deltax=t2.centerX-t1.centerX;
                            double deltay=t2.centerY-t1.centerY;
                            double dist=Math.sqrt(deltax*deltax * deltay*deltay);
                            if ((deltax < maxDeltaX && deltay < maxDeltaY) && (shortestDist==-1 || dist < shortestDist)) {
                                shortestDist=dist;
                                firstTile=t1;
                                secondTile=t2;
                            }
                        }
                    }
                    if (firstTile!=null)
                        IJ.log("   FirstTile: "+firstTile.name+", "+Double.toString(firstTile.centerX) +", "+Double.toString(firstTile.centerY)+", "+Double.toString(firstTile.relZPos));
                    else 
                        IJ.log("   FirstTile==null");
                    if (secondTile!=null)
                        IJ.log("   SecondTile: "+secondTile.name+", "+Double.toString(secondTile.centerX) +", "+Double.toString(secondTile.centerY)+", "+Double.toString(secondTile.relZPos));
                    else 
                        IJ.log("   SecondTile==null");
                    //if first and second tile don't fit in single FOV, then further consolidation impossible
                    if (firstTile==null || secondTile==null 
                            || firstTile.relZPos!=secondTile.relZPos
                            || Math.abs(firstTile.centerX-secondTile.centerX) > maxDeltaX
                            || Math.abs(firstTile.centerY-secondTile.centerY) > maxDeltaY) {
                        IJ.log("   Cannot fit into single FOV");    
                        for (int i=areaTileList.size()-1; i>=0; i--) {
                            newTileListForArea.add(areaTileList.get(i));
                            areaTileList.remove(i);
                        }
                    } else {
                        IJ.log("   Fit into single FOV");    
                        consolidatedList.add(firstTile);
                        consolidatedList.add(secondTile);
                        double minX=Math.min(firstTile.centerX,secondTile.centerX);
                        double maxX=Math.max(firstTile.centerX,secondTile.centerX);
                        double minY=Math.min(firstTile.centerY,secondTile.centerY);
                        double maxY=Math.max(firstTile.centerY,secondTile.centerY);
                        IJ.log("ConsolidatedList.size()="+Integer.toString(consolidatedList.size()));
                        for (Tile t:areaTileList) {
                            //only add unique tiles that fit into single FOV
                            if (!consolidatedList.contains(t)
                                        && t.centerX < minX+maxDeltaX 
                                        && t.centerX > maxX-maxDeltaX 
                                        && t.centerY < minY+maxDeltaY
                                        && t.centerY > maxY-maxDeltaY
                                        && t.relZPos == firstTile.relZPos) { 
                                consolidatedList.add(t);
                                //find new minx, maxx, miny, maxy in consolidated list
                                for (int i=0; i<consolidatedList.size(); i++) {
                                    Tile temp=consolidatedList.get(i);
                                    minX=Math.min(minX,temp.centerX);
                                    maxX=Math.max(maxX,temp.centerX);
                                    minY=Math.min(minY,temp.centerY);
                                    maxY=Math.max(maxY,temp.centerY);
                                }
                                IJ.log("ConsolidatedList.size()="+Integer.toString(consolidatedList.size()));
                            }    
                        }
                        newTileListForArea.add(new Tile(areaName,(minX+maxX)/2, (minY+maxY)/2, firstTile.relZPos));
                        //remove consolidated tiles from original list
                        for (int i=consolidatedList.size()-1; i>=0; i--) {
                            areaTileList.remove(consolidatedList.get(i));
                        }
                    }
                }
                areaMap.put(areaName, newTileListForArea);
                IJ.log(areaName.toUpperCase()+" Consolidated");
                for (Tile t:areaMap.get(areaName)) {
                    IJ.log(t.name+", "+Double.toString(t.centerX) +", "+Double.toString(t.centerY)+", "+Double.toString(t.relZPos));
                }
            }
        }        
    }
    
    //receives stage positions, converts to layout position
    @Override
    public void stagePosAdded(String areaName, Vec3d stagePos, Object source) {
        Vec3d lCoord;
        try {
            lCoord = acqLayout.convertStageToLayoutPos(stagePos.x, stagePos.y, stagePos.z);
            Tile lTile=new Tile(areaName,lCoord.x,lCoord.y,lCoord.z);
            List roiList=areaMap.get(areaName);
            if (roiList!=null) {
                roiList.add(lTile);
            } else {
                roiList=new ArrayList();
                roiList.add(lTile);
                areaMap.put(areaName,roiList);
            }    
            IJ.log(this.getClass().getName()+": add to tileList for area: "+areaName+", stage:"+stagePos.x+","+stagePos.y+", "+stagePos.z+" layout:"+lCoord.x+", "+lCoord.y+", "+lCoord.z);
        } catch (Exception ex) {
            IJ.log(this.getClass().getName()+": error converting stage position to layout position for area: "+areaName+", stage:"+stagePos.x+","+stagePos.y+", "+stagePos.z);
            Logger.getLogger(TileManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void stagePosListAdded(String area, List<Vec3d> stagePosList, Object source) {
        for (Vec3d stagePos:stagePosList) {
            stagePosAdded(area, stagePos,source);
        }
    }

}
