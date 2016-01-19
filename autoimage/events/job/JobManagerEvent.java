package autoimage.events.job;

import autoimage.services.JobManager;

/**
 *
 * @author Karsten Siller
 */
public interface JobManagerEvent<T> {
    
    public JobManager getManager();
    
}
