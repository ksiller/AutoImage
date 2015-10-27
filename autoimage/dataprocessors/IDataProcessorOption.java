package autoimage.dataprocessors;

import java.util.List;

/**
 * This interface should be implemented by a FilterProcessor that provides the 
 * user with a list of defined options for selection.
 * For example: a List of String that contains available channel names, 
 * see class ImageTagFilterOptString
 * 
 * @author Karsten Siller
 * @param <T> type of selectable option
 */
public interface IDataProcessorOption<T> {
        
    public void setOptions(List<T> options);
    
    public List<T> getOptions();
    
}
