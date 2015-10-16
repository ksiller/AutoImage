package autoimage;

import autoimage.api.RefArea;
import java.util.List;

/**
 *
 * @author Karsten Siller
 */
public interface IRefPointListener {
    
    public void referencePointsUpdated(List<RefArea> refAreas);
    public void referencePointSelectionChanged(RefArea refArea);
}
