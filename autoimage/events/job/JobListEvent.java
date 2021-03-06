package autoimage.events.job;

import autoimage.data.acquisition.Job;
import java.util.List;

/**
 *
 * @author Karsten Siller
 */
public abstract class JobListEvent {
        
    private final List<Job> jobs;

    public JobListEvent(List<Job> j) {
        jobs=j;
    }

    public List<Job> getJobs() {
        return jobs;
    }
    
}
