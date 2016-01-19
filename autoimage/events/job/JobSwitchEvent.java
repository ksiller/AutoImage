package autoimage.events.job;

import autoimage.data.acquisition.Job;
import autoimage.services.JobManager;

/**
 *
 * @author Karsten
 */
public class JobSwitchEvent implements JobManagerEvent {
       
    private final Job currentJob;
    private final Job previousJob;
    private final JobManager manager;

    public JobSwitchEvent(JobManager mgr, Job previous, Job current) {
        currentJob=current;
        previousJob=previous;
        manager=mgr;
    }

    public Job getCurrentJob() {
        return currentJob;
    }        

    public Job getPreviousJob() {
        return previousJob;
    }        

    @Override
    public JobManager getManager() {
        return manager;
    }
        
}
