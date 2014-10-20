package autoimage;

import java.util.List;

/**
 *
 * @author Karsten
 */
public interface IMergeAreaListener {
    
    public void mergeAreaSelectionChanged(List<Area> areas);
    public void mergeAreas(List<Area> areas);
}