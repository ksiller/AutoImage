package autoimage.events.landmark;

import autoimage.events.landmark.SingleLandmarkEvent;
import autoimage.api.IAcqLayout;
import autoimage.data.layout.Landmark;

/**
 *
 * @author Karsten Siller
 */
public class LandmarkUpdatedEvent extends SingleLandmarkEvent {
    
    public LandmarkUpdatedEvent(IAcqLayout layout, Landmark landmark, int index) {
        super(layout,landmark, index);
    }
 
}
