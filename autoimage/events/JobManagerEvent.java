package autoimage.events;

import autoimage.services.JobManager;

/**
 *
 * @author Karsten Siller
 */
public interface JobManagerEvent<T> {
    
    public JobManager getManager();
    
}
