package autoimage;

import autoimage.events.JobScheduleChangedEvent;
import autoimage.events.JobStatusChangedEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.Callable;
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
    protected ListenableFuture<Job> futureresult;
        
    public enum Status {
        UNDEFINED, CREATED, INITIALIZING, INITIALIZED, SCHEDULED, RUNNING, ACQUIRING, ACQUISITION_DONE, PROCESSING, PROCESSING_DONE, COMPLETED, ERROR, CANCEL_REQUESTED, CANCELLED, INTERRUPTED;
        
        @Override
        public String toString() {
            String name=this.name();
            name=name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
            return name.replaceAll("_", " ");
        }
    };
    
    public static final int DONE=0;
    public static final int CANCEL_REQUESTED=1;
    public static final int CANCELLED=2;
    public static final int CREATED=3;
    public static final int INITIALIZING=4;
    public static final int INITIALIZED=5;
    public static final int SCHEDULED=6;
    public static final int RUNNING=7;
    public static final int ACQUIRING=8;
    public static final int PROCESSING=9;
    public static final int COMPLETED=10;
    public static final int ERROR=11;
    public static final int UNDEFINED=12;
    public static final int INTERRUPTED=13;
    


    protected Job (EventBus evtbus, String id, T config, Runnable preInit, Runnable postInit, Runnable preRun, Runnable postRun, long timeoutMS) {
        status = Status.UNDEFINED;
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
//        updateAndBroadcastStatus(CREATED);
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

    protected final void updateAndBroadcastStatus(Status newStatus) {
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
        schedule(executor, callback, scheduledTimeMS);
    }

    public synchronized final void schedule(ListeningScheduledExecutorService executor, FutureCallback callback, long timeMS) {
        scheduledTimeMS=timeMS;
        long delay=scheduledTimeMS-System.currentTimeMillis();
        futureresult=executor.schedule(this, delay, TimeUnit.MILLISECONDS);   
        Futures.addCallback(futureresult, callback);
        updateAndBroadcastStatus(Status.SCHEDULED);
    }

    public T getConfiguration() {
        return configuration;
    }

    public Status getStatus() {
        return status;
    }

    public synchronized void requestScheduledTimeMS(long timeMS) {
        if (status==Status.CREATED) {
            long previousSchedule=scheduledTimeMS;
            scheduledTimeMS=timeMS;
            jeventBus.post(new JobScheduleChangedEvent(this,previousSchedule, timeMS));
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

    protected synchronized final void requestCancel(boolean mayInterrupt) {
        updateAndBroadcastStatus(Job.Status.CANCEL_REQUESTED);
        if (isFinished()) {
           //do nothing 
           return; 
        }    
        if (futureresult!=null) {
            //after scheduled
            boolean cancelled=futureresult.cancel(mayInterrupt);
            if (cancelled) {
                if (isRunning()) {
                    updateAndBroadcastStatus(Status.INTERRUPTED);
                } else {
                    //created, initialized, scheduled, but not running
                    updateAndBroadcastStatus(Status.CANCELLED);
                }
            } else {
                //after finished, should not happen
//                updateAndBroadcastStatus(CANCELLED);
            }
        } else {
            //before scheduled
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
        try {
            executeCallback(preInitCallback);
            updateAndBroadcastStatus(Status.INITIALIZING);
            initialize();
            updateAndBroadcastStatus(Status.INITIALIZED);
            executeCallback(postInitCallback);
            executeCallback(preRunCallback);
            updateAndBroadcastStatus(Status.RUNNING);
            actualTimeMS=System.currentTimeMillis();
            run();
            runtimeMS=actualTimeMS-System.currentTimeMillis();
            executeCallback(postRunCallback);
            updateAndBroadcastStatus(Status.COMPLETED);
            return this;
        } catch (InterruptedException ie) {
            updateAndBroadcastStatus(Status.INTERRUPTED);                        
            throw new JobException(this,JobException.INTERRUPT);
        } catch (Exception e) {
            updateAndBroadcastStatus(Status.ERROR);
            throw new JobException(this,JobException.RUNTIME_ERROR);
        } finally {
            cleanUp();
        }
    }

    protected abstract void initialize() throws JobException, InterruptedException;

    protected abstract Job run() throws JobException, InterruptedException;
        
//    protected abstract Job finishProcessing() throws JobException, InterruptedException;

    protected abstract void cleanUp();
     
}

