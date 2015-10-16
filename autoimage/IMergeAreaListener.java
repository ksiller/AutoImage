package autoimage;

import autoimage.api.SampleArea;
import java.util.List;

/**
 *
 * @author Karsten
 */
public interface IMergeAreaListener {
    
    public void mergeAreaSelectionChanged(List<SampleArea> areas);
    public void mergeAreas(List<SampleArea> areas);
}
