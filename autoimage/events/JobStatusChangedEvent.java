package autoimage.events;

import autoimage.Job;
import autoimage.Job.Status;

/**
 *
 * @author Karsten Siller
 */
public class JobStatusChangedEvent extends JobEvent {

    private final Status oldStatus;
    private final Status newStatus;
    
    public JobStatusChangedEvent(Job j, Status oldSt, Status newSt) {
        super(j);
        oldStatus=oldSt;
        newStatus=newSt;
    }

    @Override
    public Job getJob() {
        return job;
    }

    public Status getNewStatus () {
        return newStatus;
    }

    public Status getOldStatus() {
        return oldStatus;
    }

}