package autoimage.events.landmark;

import autoimage.api.IAcqLayout;
import autoimage.data.layout.Landmark;
import java.util.List;

/**
 *
 * @author Karsten
 */
public class LandmarkSelectionChangedEvent {
    
    private final IAcqLayout layout;
    private final List<Landmark> selected;
    
    public LandmarkSelectionChangedEvent(IAcqLayout layout, List<Landmark> selected) {
        this.layout=layout;
        this.selected=selected;
    }
    
    public IAcqLayout getLayout() {
        return layout;
    }
    
    public List<Landmark> getSelectedRefAreas() {
        return selected;
    }
        
}
