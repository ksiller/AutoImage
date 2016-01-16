package autoimage.services;

import autoimage.data.acquisition.Job;

/**
 *
 * @author Karsten
 */
public class JobException extends Exception {
    
    public static final String INTERRUPT = "Interrupted Execution";
    public static final String RUNTIME_ERROR = "Runtime Execution Error";
    public static final String ILLEGAL_CONFIGURATION = "Illegal Configuration";
    
    private Job job;
    private String message;
    
    public JobException (Job j, String msg) {
        job=j;
        message=msg;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
    
    public Job getJob() {
        return job;
    }
    
    @Override
    public String toString() {
        return "Job: "+job.toString()+", "+message;
    }
}
