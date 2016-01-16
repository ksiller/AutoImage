package autoimage.events;

import autoimage.data.acquisition.Job;
import autoimage.services.JobManager;

/**
 *
 * @author Karsten Siller
 */
public class JobPlacedInQueueEvent extends SingleJobManagerEvent {
    
    public JobPlacedInQueueEvent(JobManager mgr, Job j) {
        super(mgr,j);
    }
    
}
