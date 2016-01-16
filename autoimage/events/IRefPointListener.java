package autoimage.events;

import autoimage.data.layout.Landmark;
import java.util.List;

/**
 *
 * @author Karsten Siller
 */
public interface IRefPointListener {
    
    public void referencePointsUpdated(List<Landmark> refAreas);
    public void referencePointSelectionChanged(Landmark refArea);
}
