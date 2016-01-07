package autoimage.events;

import autoimage.Job;

/**
 *
 * @author Karsten Siller
 */
public class JobImageStoredEvent<T extends Object> extends JobEvent {
    
    private final T image;
    
    public JobImageStoredEvent (Job j, T ti) {
        super (j);
        image=ti;
    }
    
    public T getImage() {
        return image;
    } 
    
}
