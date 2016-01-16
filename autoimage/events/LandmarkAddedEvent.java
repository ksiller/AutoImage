package autoimage.events;

import autoimage.api.IAcqLayout;
import autoimage.data.layout.Landmark;

/**
 *
 * @author Karsten Siller
 */
public class LandmarkAddedEvent extends SingleLandmarkEvent {
    
    public LandmarkAddedEvent(IAcqLayout layout, Landmark landmark, int index) {
        super(layout,landmark, index);
    }
 
}