package autoimage.events.job;

import autoimage.data.acquisition.Job;

/**
 *
 * @author Karsten Siller
 */
public class JobScheduleChangedEvent extends JobEvent {
    
    private final long oldScheduledTimeMS;
    private final long newScheduledTimeMS;
    
    public JobScheduleChangedEvent(Job j, long oldSt, long newSt) {
        super(j);
        oldScheduledTimeMS=oldSt;
        newScheduledTimeMS=newSt;
    }

    @Override
    public Job getJob() {
        return job;
    }

    public long getNewScheduledTimeMS () {
        return newScheduledTimeMS;
    }

    public long getOldScheduledTimeMS() {
        return oldScheduledTimeMS;
    }

}