package autoimage.events;

import autoimage.api.BasicArea;
import autoimage.api.IAcqLayout;

/**
 *
 * @author Karsten Siller
 */
public class SingleAreaEvent {
    
    private final IAcqLayout layout;
    private final BasicArea area;
    
    public SingleAreaEvent(IAcqLayout layout, BasicArea area) {
        this.layout=layout;
        this.area=area;
    }
    
    public IAcqLayout getLayout() {
        return this.layout;
    }
    
    public BasicArea getArea() {
        return this.area;
    }
    
}
