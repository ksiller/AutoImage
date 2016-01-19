package autoimage.events.job;

import autoimage.events.job.JobListManagerEvent;
import autoimage.data.acquisition.Job;
import autoimage.services.JobManager;
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

