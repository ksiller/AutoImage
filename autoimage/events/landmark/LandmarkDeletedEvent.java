package autoimage.events.landmark;

import autoimage.events.landmark.SingleLandmarkEvent;
import autoimage.api.IAcqLayout;
import autoimage.data.layout.Landmark;

/**
 *
 * @author Karsten Siller
 */
public class LandmarkDeletedEvent extends SingleLandmarkEvent {
    
    public LandmarkDeletedEvent(IAcqLayout layout, Landmark landmark, int index) {
        super(layout,landmark, index);
    }
 
}
