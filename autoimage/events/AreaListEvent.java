package autoimage.events;

import autoimage.api.BasicArea;
import autoimage.api.IAcqLayout;
import java.util.List;

/**
 *
 * @author Karsten Siller
 */
public class AreaListEvent {

    private final IAcqLayout layout;
    private final List<BasicArea> areaList;
    
    public AreaListEvent(IAcqLayout layout, List<BasicArea> list) {
        this.layout=layout;
        this.areaList=list;
    }
    
    public IAcqLayout getLayout() {
        return this.layout;
    }
    
    public List<BasicArea> getAreaList() {
        return this.areaList;
    }
    
}
