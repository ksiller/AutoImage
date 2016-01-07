package autoimage.events;

import autoimage.Job;
import autoimage.JobManager;

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
