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
public class RuntimeTileManager {
    
    protected Map<String,List<Tile>> roiMap;
    protected AcquisitionLayout acqLayout;
//    protected boolean finalized;
    
    public RuntimeTileManager(AcquisitionLayout aLayout) {
        roiMap=new HashMap<String,List<Tile>>();
        acqLayout=aLayout;
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
    
    //expects Tile with stage coordinates and places them in map binned by area name
    public synchronized void addStageROI(String area, Tile sTile) {
        if (acqLayout==null) {
            IJ.log("RuntimeTileManager==null: area: "+area);
            return;
        }    
        RefArea rp = acqLayout.getLandmark(0);
        if (rp!=null) {
            //convert stage pos to layout coord and place in map
            Tile lTile=new Tile(sTile.name,
                            rp.convertStagePosToLayoutCoord_X(sTile.centerX),
                            rp.convertStagePosToLayoutCoord_Y(sTile.centerY),
                            0);
            List tileList=roiMap.get(area);
            if (tileList!=null) {
                IJ.log("add to tileList for area: "+area);
                tileList.add(lTile);
            } else {
                IJ.log("new tileList for area: "+area);
                tileList=new ArrayList();
                tileList.add(lTile);
                roiMap.put(area,tileList);
            }
        } else {
            IJ.log("RuntimeTileManager: refArea==null");
        }
    }
    
    public synchronized Map<String,List<Tile>> getROIs() {
        return roiMap;
    }
    
    public synchronized void clearAllROIs() {
        roiMap.clear();
    }
    /*
    public void convertStagePosMap(AcquisitionLayout acqLayout) {
        RefArea rp = acqLayout.getLandmark(0);
        if (rp!=null) {
            Iterator it = roiMap.entrySet().iterator();
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
                roiMap.put((String)pairs.getKey(), lList);
                it.remove(); // avoids a ConcurrentModificationException
            }
        }
    }*/

}
