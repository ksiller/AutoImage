package autoimage.api;

import java.util.List;

/**
 *
 * @author Karsten Siller
 */
public interface IDoxelListener {
    
    public void doxelAdded(Doxel docxel, Object source);
    public void doxelListAdded(List<Doxel> doxelList, Object source);
}
