package autoimage.events;

import autoimage.data.acquisition.Job;

/**
 *
 * @author Karsten Siller
 */
public class JobCreatedEvent extends JobEvent{

    public JobCreatedEvent(Job job) {
        super(job);
    }
}

