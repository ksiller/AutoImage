package autoimage;

import autoimage.area.Area;

/**
 *
 * @author Karsten
 */
public class TilingThread implements Runnable {//Callable<List<Area.Tile>> {
    
    private Area area;
    private double fovX;
    private double fovY;
    private TilingSetting setting;
    private TileManager tileManager;
    
    public TilingThread(TileManager tManager, Area a, double w, double h, TilingSetting s) {
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
//                for (Area a:areas) {
                    area.calcTilePositions(tileManager,fovX, fovY, setting);
//                }  
            }    
        } catch (InterruptedException ie) {
        }    
    }

/*    @Override
    public List<Area.Tile> call() throws Exception {
        IJ.log(Thread.currentThread().getName()+", Area:"+area.getName()+" ...started");
        List<Area.Tile> tl=area.calcTilePositionsNew(fovX, fovY, setting);
        IJ.log(Thread.currentThread().getName()+", Area:"+area.getName()+" ...ended");
        return tl;
    }
 */   
   
}
