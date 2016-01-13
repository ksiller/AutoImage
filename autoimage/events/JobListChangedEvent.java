package autoimage.events;

import autoimage.Job;
import autoimage.JobManager;
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
