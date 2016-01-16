package autoimage.events;

import java.awt.Component;

/**
 *
 * @author Karsten Siller
 */
public interface IRuleListener {
    
    public void resizeRequested(int newSize, Component source);
    
}
