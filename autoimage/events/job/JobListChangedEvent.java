package autoimage.events.job;

import autoimage.data.acquisition.Job;
import autoimage.services.JobManager;
import java.util.List;

/**
 *
 * @author Karsten Siller
 */
public class JobListChangedEvent extends JobListManagerEvent {
    
    public JobListChangedEvent(JobManager mgr, List<Job> j) {
        super(mgr,j);
    }
    
}
