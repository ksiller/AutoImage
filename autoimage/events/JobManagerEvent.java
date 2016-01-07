package autoimage.events;

import autoimage.JobManager;

/**
 *
 * @author Karsten Siller
 */
public interface JobManagerEvent<T> {
    
    public JobManager getManager();
    
}
