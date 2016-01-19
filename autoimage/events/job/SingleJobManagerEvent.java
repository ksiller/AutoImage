package autoimage.events.job;

import autoimage.data.acquisition.Job;
import autoimage.services.JobManager;

/**
 *
 * @author Karsten Siller
 */
public abstract class SingleJobManagerEvent extends JobEvent implements JobManagerEvent{

    private final JobManager manager;
    
    public SingleJobManagerEvent(JobManager mgr, Job j) {
        super(j);
        manager=mgr;
    }
    
    @Override
    public JobManager getManager() {
        return manager;
    }
}
