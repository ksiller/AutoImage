package autoimage.events;

import autoimage.api.BasicArea;
import autoimage.api.IAcqLayout;
import java.util.List;

/**
 *
 * @author Karsten Siller
 */
public class AreasDeletedEvent extends AreaListEvent {

    
    public AreasDeletedEvent(IAcqLayout layout, List<BasicArea> list) {
        super(layout,list);
    }
    
}
