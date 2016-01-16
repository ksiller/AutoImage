package autoimage.events;

import autoimage.api.BasicArea;
import java.util.List;

/**
 *
 * @author Karsten
 */
public interface IMergeAreaListener {
    
    public void mergeAreaSelectionChanged(List<BasicArea> areas);
    public void mergeAreas(List<BasicArea> areas);
}
