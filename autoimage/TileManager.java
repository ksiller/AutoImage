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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Karsten
 */
public class TileManager {
    
    protected Map<String,List<Tile>> areaMap;
    protected AcquisitionLayout acqLayout;
    protected AcqSetting acqSetting;
//    protected boolean finalized;
    
    public TileManager (AcquisitionLayout aLayout, AcqSetting aSetting) {
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
    public void setAcquisitionLayout(AcquisitionLayout aLayout) {
        acqLayout=aLayout;
    }
    
    public void setAcquisitionSetting(AcqSetting aSetting) {
        acqSetting=aSetting;
    }
    
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
    
    public synchronized Map<String,List<Tile>> getAreaMap() {
        return areaMap;
    }
    
    public synchronized void clearAllSeeds() {
        areaMap.clear();
    }
    /*
    public void convertStagePosMap(AcquisitionLayout acqLayout) {
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

}
