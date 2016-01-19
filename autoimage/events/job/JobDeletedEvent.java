package autoimage.events.job;

import autoimage.data.acquisition.Job;

/**
 *
 * @author Karsten Siller
 */
public class JobDeletedEvent extends JobEvent {

    public JobDeletedEvent(Job job) {
        super(job);
    }
}
