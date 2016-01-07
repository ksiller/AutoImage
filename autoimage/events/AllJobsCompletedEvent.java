package autoimage.events;

import autoimage.Job;
import autoimage.JobManager;
import java.util.List;

/**
 *
 * @author Karsten Siller
 */
public class AllJobsCompletedEvent extends JobListManagerEvent {

    public AllJobsCompletedEvent(JobManager mgr, List<Job> j) {
        super(mgr,j);
    }

}

