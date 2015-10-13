package autoimage;

/**
 *
 * @author Karsten
 */
public interface IDataProcessorNotifier {
    
    public void addListener(IDataProcessorListener l);
    public void removeListener(IDataProcessorListener l);
    
}
