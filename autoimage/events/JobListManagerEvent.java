package autoimage.events;

import autoimage.data.acquisition.Job;
import autoimage.services.JobManager;
import java.util.List;

/**
 *
 * @author Karsten
 */
public abstract class JobListManagerEvent implements JobManagerEvent {
    
    private final List<Job> jobs;
    private final JobManager manager;

    public JobListManagerEvent(JobManager mgr, List<Job> j) {
        manager=mgr;
        jobs=j;
    }

    @Override
    public JobManager getManager() {
        return manager;
    }
    
    public List<Job> getJobs() {
        return jobs;
    } 
    
}
