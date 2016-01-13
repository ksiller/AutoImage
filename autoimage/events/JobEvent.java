package autoimage.events;

import autoimage.Job;

/**
 *
 * @author Karsten
 */
public abstract class JobEvent {
   
    protected final Job job;

    public JobEvent(Job j) {
        job=j;
    }

    public Job getJob() {
        return job;
    }        

}