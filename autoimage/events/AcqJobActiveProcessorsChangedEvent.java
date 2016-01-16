package autoimage.events;

import autoimage.data.acquisition.AcquisitionJob;
import java.util.List;

/**
 *
 * @author Karsten Siller
 */
public class AcqJobActiveProcessorsChangedEvent<T> {
    
    private final AcquisitionJob job;
    private final List<T> processors;
    
    public AcqJobActiveProcessorsChangedEvent(AcquisitionJob j, List<T> procs) {
        job=j;
        processors=procs;
    }
    
    public List<T> getProcessors() {
        return processors;
    }
    
    public AcquisitionJob getJob() {
        return job;
    }
}
