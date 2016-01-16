package autoimage.events;

import autoimage.api.IAcqLayout;
import autoimage.data.layout.Landmark;

/**
 *
 * @author Karsten
 */
public abstract class SingleLandmarkEvent {
    
    private final IAcqLayout layout;
    private final Landmark landmark;
    private final int listIndex;
    
    public SingleLandmarkEvent(IAcqLayout layout, Landmark landmark, int index) {
        this.layout=layout;
        this.landmark=landmark;
        this.listIndex=index;
    }
 
    public IAcqLayout getLayout() {
        return layout;
    }
    
    public Landmark getLandmark() {
        return landmark;
    }
    
    public int getListIndex() {
        return listIndex;
    }
}
