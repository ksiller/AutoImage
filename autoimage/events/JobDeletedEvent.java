package autoimage.events;

import autoimage.Job;

/**
 *
 * @author Karsten Siller
 */
public class JobDeletedEvent extends JobEvent {

    public JobDeletedEvent(Job job) {
        super(job);
    }
}
