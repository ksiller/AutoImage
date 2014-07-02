/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

/**
 *
 * @author Karsten
 */
public interface IDataProcessorNotifier {
    
    public void addListener(IDataProcessorListener l);
    public void removeListener(IDataProcessorListener l);
    
}
