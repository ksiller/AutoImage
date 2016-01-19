package autoimage.data.acquisition;

import autoimage.services.JobException;
import autoimage.events.job.JobScheduleChangedEvent;
import autoimage.events.job.JobStatusChangedEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumMap;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Karsten Siller
 */
public abstract class Job<T> implements Callable<Job> {

    protected final String identifier;
    protected final EventBus jeventBus;
    protected final T configuration;
    protected final Runnable preInitCallback;
    protected final Runnable postInitCallback;
    protected final Runnable preRunCallback;
    protected final Runnable postRunCallback;
    protected final long callbackTimeoutMS;

    protected volatile Status status;
    protected long scheduledTimeMS;
    protected long actualTimeMS;
    protected long runtimeMS;
    protected final Map<Status,Float> progressMap;
    protected final Map<Status,Float> publicProgressMap;
    protected volatile ListenableFuture<Job> futureresult;
    
    private volatile boolean isCalled;
        
    public enum Status {
        UNDEFINED, CREATED, INITIALIZING, INITIALIZED, SCHEDULED, RUNNING, ACQUIRING, ACQUISITION_DONE, PROCESSING, PROCESSING_DONE, COMPLETED, ERROR, CANCEL_REQUESTED, CANCELLED, INTERRUPTED;
        
        @Override
        public String toString() {
            String name=this.name();
            name=name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
            return name.replaceAll("_", " ");
        }
    };
       

    protected Job (EventBus evtbus, String id, T config, Runnable preInit, Runnable postInit, Runnable preRun, Runnable postRun, long timeoutMS) {
        status = Status.UNDEFINED;
        isCalled=false;
        jeventBus=evtbus;
        identifier=id;
        configuration=config;
        preInitCallback=preInit;
        postInitCallback=postInit;
        preRunCallback=preRun;
        postRunCallback=postRun;
        callbackTimeoutMS=timeoutMS;
        scheduledTimeMS = -1;
        actualTimeMS = -1;
        runtimeMS=0;
        futureresult=null;
        progressMap=Collections.synchronizedMap(new EnumMap<Status,Float>(Status.class));
        publicProgressMap=Collections.unmodifiableMap(progressMap);
//        updateAndBroadcastStatus(CREATED);
    }

    public synchronized Future getResult() {
        return futureresult;
    }
    
    public String getId() {
        return identifier;
    }
    
    @Override
    public String toString() {
        Calendar cal=new GregorianCalendar();
        cal.setTimeInMillis(scheduledTimeMS);
        return "Id="+identifier+", Scheduled time="+cal.getTime().toString()+", Status="+status.toString();
    }

    public final void updateAndBroadcastStatus(Status newStatus) {
        Status oldStatus;
        synchronized (this) {
            oldStatus=status;
            status=newStatus;
        }
        if (jeventBus!=null && newStatus!=oldStatus) {
            jeventBus.post(new JobStatusChangedEvent(this, oldStatus, newStatus));
        }          
    }

    public synchronized final void schedule(ListeningScheduledExecutorService executor, FutureCallback callback) {
        if (status==Status.CREATED) {
            long delay=scheduledTimeMS-System.currentTimeMillis();
            futureresult=executor.schedule(this, delay, TimeUnit.MILLISECONDS);   
            Futures.addCallback(futureresult, callback);
            updateAndBroadcastStatus(Status.SCHEDULED);
        } else {
            //ignore
        }
    }

    public synchronized final void schedule(ListeningScheduledExecutorService executor, FutureCallback callback, long timeMS) throws IllegalStateException {
        if (status==Status.CREATED) {
            scheduledTimeMS=timeMS;
            schedule(executor, callback);
        } else {
            //already scheduled or executed
            if (timeMS==scheduledTimeMS) {
                //same time --> do nothing
                return;
            } else {
                //rescheduling unsuccessful
                throw new IllegalStateException("Job is already scheduled or executed and cannot be rescheduled.");
            }
        }
    }

    /*public synchronized void setProgress(Status st, float p) {
        if (p>=0 && p<=1) {
            progressMap.put(st, p);
        }    
    }
    */
    
    public synchronized Float getProgress(Status st) {
        if (progressMap.containsKey(st)) {
            return progressMap.get(st);
        } else {
            return null;
        }
    }
    
    public T getConfiguration() {
        return configuration;
    }

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized void requestRescheduling(long timeMS) throws IllegalStateException {
        if (status==Status.CREATED) {
            long previousSchedule=scheduledTimeMS;
            scheduledTimeMS=timeMS;
            jeventBus.post(new JobScheduleChangedEvent(this,previousSchedule, timeMS));
        } else {
            throw new IllegalStateException("Job is already scheduled or executed.");
        }
    }
    
    public synchronized long getScheduledTimeMS() {
        return scheduledTimeMS;
    }

    public synchronized long getActualTimeMS() {
        return actualTimeMS;
    }

    public synchronized long getRuntimeMS() {
        return runtimeMS;
    }

    public synchronized boolean isActive() {
        return Job.JobIsActive(status);
    }
    
    public static boolean JobIsActive(Status status) {
        return status!=Status.CREATED && !Job.JobFinished(status);
    }
    
    public synchronized boolean isRunning() {
        return JobRunning(status);
    }
    
    public static boolean JobRunning(Status status) {
       return status!=Status.CREATED
               && status!=Status.SCHEDULED
               && status!=Status.INITIALIZING
               && status!=Status.INITIALIZED
               && !Job.JobFinished(status);
/*       return status==Status.ACQUIRING
                || status==Status.PROCESSING; */
    }

    public synchronized boolean isFinished() {
       return Job.JobFinished(status); 
    }

    public static boolean JobFinished(Status status) {
       return status==Status.COMPLETED 
                || status==Status.INTERRUPTED 
                || status==Status.ERROR 
                || status==Status.CANCELLED; 
    }

    public synchronized final void requestCancel(boolean mayInterrupt) {
        if (isFinished()) {
           //do nothing 
           return; 
        }    
        updateAndBroadcastStatus(Job.Status.CANCEL_REQUESTED);
        if (futureresult!=null) {
            //after scheduled
            boolean cancelled=futureresult.cancel(mayInterrupt);
            if (cancelled) {
                if (isRunning()) {
                    updateAndBroadcastStatus(Status.INTERRUPTED);
                } else {
                    //created and scheduled, but not running
                    updateAndBroadcastStatus(Status.CANCELLED);
                }
            } else {
                //after finished, should not happen
            }
        } else {
            //craated but not scheduled
            updateAndBroadcastStatus(Status.CANCELLED);
        }
    }

    private void executeCallback (Runnable callback) throws JobException {
        if (callback!=null) {
            Thread thread=new Thread(callback);
            try {
                thread.start();
                thread.join(callbackTimeoutMS);
            } catch (Exception ex) {
                throw new JobException(this,JobException.RUNTIME_ERROR);
            }
        }
    }
    
    @Override
    public final Job call() throws JobException {
        synchronized (this) {
            if (isCalled) {
                throw new JobException(this, "Job has been executed or is currently running");
            }
        }
        isCalled=true;
        try {
            executeCallback(preInitCallback);
            updateAndBroadcastStatus(Status.INITIALIZING);
            progressMap.put(Status.INITIALIZED, 0f);
            initialize();
            progressMap.put(Status.INITIALIZED, 1f);
            updateAndBroadcastStatus(Status.INITIALIZED);
            executeCallback(postInitCallback);
            executeCallback(preRunCallback);
            updateAndBroadcastStatus(Status.RUNNING);
            progressMap.put(Status.RUNNING, 0f);
            actualTimeMS=System.currentTimeMillis();
            run();
            progressMap.put(Status.RUNNING, 1f);
            runtimeMS=actualTimeMS-System.currentTimeMillis();
            executeCallback(postRunCallback);
            updateAndBroadcastStatus(Status.COMPLETED);
        } catch (InterruptedException ie) {
            updateAndBroadcastStatus(Status.INTERRUPTED);                        
            throw new JobException(this,JobException.INTERRUPT);
        } catch (Exception e) {
            updateAndBroadcastStatus(Status.ERROR);
            throw new JobException(this,JobException.RUNTIME_ERROR);
        } finally {
            cleanUp();
        }
        return this;
    }

    protected abstract void initialize() throws JobException, InterruptedException;

    protected abstract Job run() throws JobException, InterruptedException;
        
    protected abstract void cleanUp();
     
}

