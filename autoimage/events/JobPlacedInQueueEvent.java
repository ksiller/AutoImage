package autoimage.events;

import autoimage.Job;
import autoimage.JobManager;

/**
 *
 * @author Karsten Siller
 */
public class JobPlacedInQueueEvent extends SingleJobManagerEvent {
    
    public JobPlacedInQueueEvent(JobManager mgr, Job j) {
        super(mgr,j);
    }
    
}
