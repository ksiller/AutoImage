package autoimage.events;

import java.util.List;
import java.util.concurrent.Callable;

/**
 *
 * @author Karsten Siller
 */
    public class JobsCancelledEvent {

        private final List<Callable> cancelledJobs;

        public JobsCancelledEvent(List<Callable> jobs) {
            cancelledJobs=jobs;
        }
        
        public List<Callable> getTasks() {
            return cancelledJobs;
        }
    }
    
