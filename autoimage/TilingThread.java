package autoimage;

import autoimage.api.TilingSetting;
import autoimage.api.BasicArea;

/**
 *
 * @author Karsten Siller
 */
public class TilingThread implements Runnable {//Callable<List<Area.Tile>> {
    
    private BasicArea area;
    private double fovX;
    private double fovY;
    private TilingSetting setting;
    private DoxelManager tileManager;
    
    public TilingThread(DoxelManager tManager, BasicArea a, double w, double h, TilingSetting s) {
        area=a;
        setting=s;
        fovX=w;
        fovY=h;
        tileManager=tManager;
    }

    @Override
    public void run() {
        try {
            if (area!=null) {
//                for (BasicArea a:areas) {
                    area.calcTilePositions(tileManager,fovX, fovY, setting);
//                }  
            }    
        } catch (InterruptedException ie) {
        }    
    }

/*    @Override
    public List<Area.Tile> call() throws Exception {
        IJ.log(Thread.currentThread().getName()+", BasicArea:"+area.getName()+" ...started");
        List<Area.Tile> tl=area.calcTilePositionsNew(fovX, fovY, setting);
        IJ.log(Thread.currentThread().getName()+", BasicArea:"+area.getName()+" ...ended");
        return tl;
    }
 */   
   
}
