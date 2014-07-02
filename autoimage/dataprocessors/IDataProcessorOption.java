/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.dataprocessors;

import java.util.List;

/**
 *
 * @author Karsten
 */
public interface IDataProcessorOption<T> {
        
    public void setOptions(List<T> options);
    
    public List<T> getOptions();
    
}
