package autoimage.events;

import autoimage.Job;
import autoimage.JobManager;
import java.util.List;

/**
 *
 * @author Karsten Siller
 */
public class JobListReorderedEvent extends JobListManagerEvent {
    
    public JobListReorderedEvent(JobManager mgr, List<Job> j) {
        super(mgr,j);
    }
    
}
