package autoimage.events;

import autoimage.api.BasicArea;
import autoimage.api.IAcqLayout;

/**
 *
 * @author Karsten Siller
 */
public class AreaDeletedEvent extends SingleAreaEvent {
    
    public AreaDeletedEvent(IAcqLayout layout, BasicArea area) {
        super(layout,area);
    }
    
}
