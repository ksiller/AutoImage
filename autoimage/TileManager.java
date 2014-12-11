/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import ij.IJ;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Karsten
 */
public class TileManager implements IStagePosListener {
    
    private Map<String,List<Tile>> areaMap;
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

    @Override
    public void stagePosAdded(String areaName, Vec3d stagePos, Object source) {
        Vec3d lCoord = acqLayout.convertStagePosToLayoutPos(stagePos.x, stagePos.y, stagePos.z);
        Tile lTile=new Tile(areaName,lCoord.x,lCoord.y,lCoord.z);
        List roiList=areaMap.get(areaName);
        if (roiList!=null) {
            roiList.add(lTile);
        } else {
            roiList=new ArrayList();
            roiList.add(lTile);
            areaMap.put(areaName,roiList);
        }    
        IJ.log("add to tileList for area: "+areaName+", stage:"+stagePos.x+","+stagePos.y+", "+stagePos.z+" layout:"+lCoord.x+", "+lCoord.y+", "+lCoord.z);
    }

    @Override
    public void stagePosListAdded(String area, List<Vec3d> stagePosList, Object source) {
        for (Vec3d stagePos:stagePosList) {
            stagePosAdded(area, stagePos,source);
        }
    }

}
