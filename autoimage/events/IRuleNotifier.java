package autoimage.events;

/**
 *
 * @author Karsten Siller
 */
public interface IRuleNotifier {
    
    public void addListener(IRuleListener l);
    public void removeListener(IRuleListener l);
    
}