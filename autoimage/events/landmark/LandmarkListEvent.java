package autoimage.events.landmark;

import autoimage.api.IAcqLayout;
import autoimage.data.layout.Landmark;
import java.util.List;

/**
 *
 * @author Karsten
 */
public class LandmarkListEvent {

    private final IAcqLayout layout;
    private final List<Landmark> refAreaList;
    
    public LandmarkListEvent(IAcqLayout layout, List<Landmark> list) {
        this.layout=layout;
        this.refAreaList=list;
    }
    
    public IAcqLayout getLayout() {
        return this.layout;
    }
    
    public List<Landmark> getRefAreaList() {
        return this.refAreaList;
    }
    
}
