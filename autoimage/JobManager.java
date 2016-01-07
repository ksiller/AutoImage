package autoimage;

import autoimage.Job.Status;
import autoimage.events.JobStatusChangedEvent;
import autoimage.events.AllJobsCompletedEvent;
import autoimage.events.JobSwitchEvent;
import autoimage.events.JobListReorderedEvent;
import autoimage.events.JobPlacedInQueueEvent;
import autoimage.gui.JobStatusWindow;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import ij.IJ;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
//import org.micromanager.events.AcquisitionEndedEvent;
//import org.micromanager.events.AcquisitionStartedEvent;

/**
 *
 * @author Karsten Siller
 */
public abstract class JobManager {
    
    /**
     *
     */
    protected final EventBus jobEventBus;
    private ListeningScheduledExecutorService executor;
    private final List<Job> jobs;
    private final FutureCallback<Job> callback;
    private JobStatusWindow statusWindow;
    private Job currentJob;
    private boolean sortByScheduledTime=false;
    //protected boolean jobQueueClosed;
    private boolean cancellingAllJobs;
    
    /**
     *
     */
    public class ScheduledTimeComparator implements Comparator<Job> {

        @Override
        public int compare(Job job1, Job job2) {
            if (job1==null) {
                return -1;
            }
            if (job2==null) {
                return 1;
            }
            if (job1.scheduledTimeMS < job2.scheduledTimeMS) {
                return -1;
            }
            if (job1.scheduledTimeMS > job2.scheduledTimeMS){
                return 1;
            }
            return 0;
        }
    }

    //----------------------
    
    public JobManager() {
        jobEventBus= new EventBus();        
        cancellingAllJobs=false;
        currentJob=null;
        executor = JobManager.ExecutorFactory();
        jobs=new ArrayList<Job>();
        callback = new FutureCallback<Job> () {

            @Override
            public void onSuccess(Job job) {
//                job.updateAndBroadcastStatus(Job.COMPLETED);
                IJ.log("onSuccess: "+job.toString());
            }

            @Override
            public void onFailure(Throwable thrwbl) {
                if (thrwbl instanceof JobException) {
                    JobException jobException=(JobException)thrwbl;
                    IJ.log("onFailure: "+jobException.toString());
                }
            }    
        };        
    }
    
    /**
     * Factory method to create an instance of ListeningScheduledExecutorService
     * @return instance that can be used as executor object
     */
    protected static ListeningScheduledExecutorService ExecutorFactory() {
        return MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
    }
    
    /**
     * Determines whether jobs are reordered based on each job's scheduled execution time. Used in the scheduleJobs() method.
     * @param b false (default), no reordering
     */
    public void setSortByScheduledTime(boolean b) {
        sortByScheduledTime=b;
    }
    
/*    
    @Subscribe
    public void acquisitionStarted(AcquisitionStartedEvent e) {
        IJ.log(this.getClass().getName()+".acquisitonStarted: "+e.getDatastore().getSavePath());
    }
    
    @Subscribe
    public void acquisitionStopped(AcquisitionEndedEvent e) {
        IJ.log(this.getClass().getName()+".acquisitonEnded: "+e.getStore().getSavePath());
    }
*/  
    
    /**
     * Handler to job status events posted by Job objects.
     * @param e describes the event, including job source (poster), previous job status, and new job status.
     */
    @Subscribe
    public void jobStatusUpdated(JobStatusChangedEvent e) {
        int index=jobs.indexOf(e.getJob());
        if (index<0) {
            //this means job is not in job list, do nothing
            return;
        }
        Status newStatus=e.getNewStatus();
        Status oldStatus=e.getOldStatus();
        IJ.log(this.getClass().getName()+".jobStatusUpdated: "
                + e.getJob().getId()
                + ", old status=" + oldStatus.toString()
                + ", new status=" + newStatus.toString());
        synchronized (this) {
            IJ.log("CURRENTJOB="+(currentJob==null ? "null" : currentJob.getId()));
            if (currentJob==null && !Job.JobFinished(newStatus)) {
                //first job and it is not finished 
                currentJob=e.getJob();
                IJ.log("POSTING SWITCH JOB");
                jobEventBus.post(new JobSwitchEvent(this,null,e.getJob()));
            }
            else if (Job.JobFinished(newStatus) && !Job.JobFinished(oldStatus)) {
                //this job just finished
                if (getActiveJobs().isEmpty()) {
                    //all jobs done
                    currentJob=null;
                    jobEventBus.post(new AllJobsCompletedEvent(this,jobs));
                } else if (!cancellingAllJobs) {    
                    //more jobs, go to next in queue
                    Job previousJob=currentJob;
                    //set current job to next in job list
                    currentJob=jobs.get(index+1);
                    jobEventBus.post(new JobSwitchEvent(this,previousJob,currentJob));
                }
            }
        }
    }

    /**
     * Handler for event posted by this manager.
     * @param e describes the event, including a list of all completed jobs.
     */
    @Subscribe
    public void allJobsCompleted(AllJobsCompletedEvent e) {
        IJ.log(this.getClass().getName()+".allJobsCompleted:");
        for (Job j:e.getJobs()) {
            IJ.log("    "+j.getId()+", status="+j.getStatus());
        }
//        jobEventBus.post(new AllJobsCompletedEvent(this, e.getJobs()));
//        eventbus.post(new AllJobsCompletedEvent(e.getJobs()));
    }
    
    
    /**
     * Register this object as a listener of the jobEventBus. Both jobs and this manager post to the jobEventBus. Listeners need to add the @Subscribe annotation to a single argument public method to handle events posted on the jobEventBus 
     * @param listener Object to be added as listener
     */
    public void registerForEvents(Object listener) {
        jobEventBus.register(listener);
//        eventbus.registerForEvents(listener);
    }
    
    /**
     * Remove this object as listener of the jobEventBus.  
     * @param listener 
     */
    public void unregisterEventListener(Object listener) {
        jobEventBus.unregister(listener);
//        eventbus.unregisterForEvents(listener);
    }
    
/*    private String calculateWaitTime(long time) {
        long deltaMS=time - System.currentTimeMillis();

        int seconds = (int)Math.floor(deltaMS / 1000) % 60 ;
        int minutes = (int) Math.floor(deltaMS / (1000*60)) % 60;
        int hours   = (int) Math.floor(deltaMS / (1000*60*60)) % 24;
        int days = (int) Math.floor(deltaMS / (1000*60*60*24));                                    
        String d="";
        if (days > 0) {
            d=d+Integer.toString(days)+" d, ";
        }
        if (hours > 0 | days > 0) {
            d=d+Integer.toString(hours)+" h, ";
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            d=d+Integer.toString(minutes)+" min, ";
        }
        return d+Integer.toString(seconds)+" s";
    }
*/  
    /**
     * Places a new job at the end of the job queue
     * @param newJob The job object to be added to the job queue
     * @throws JobException is thrown if the job executor has been shutdown and cannot accept new jobs. The job executor's shutDown() method is invoked at the end of scheduleJobs() method.
     */
    public void putJob(Job newJob) throws JobException {
        if (executor.isShutdown()) {
            throw new JobException(newJob, "AcquisitionManager cannot accept new jobs. Reset AcquisitonManager first");
        } 
        if (newJob!=null) {
            jobs.add(newJob);
//            eventbus.post(new JobPlacedInQueueEvent(newJob));
            jobEventBus.post(new JobPlacedInQueueEvent(this,newJob));
        }
    }
    
    /**
     * Schedules all jobs. If sortedByScheduledTime == true, job queue is resorted based on each job's scheduled time. If sortedByScheduleTime == false, jobs are not reordered based on scheduled time
     * @throws JobException is thrown if sortedByScheduleTime == false and the scheduled times for all jobs is out of order.
     */
    public void scheduleJobs() throws JobException {
        if (jobs==null || jobs.isEmpty()) {
            throw new JobException(null,"No jobs in queue.");
        }
        long lastStartTimeMS=-1;
        if (sortByScheduledTime) {
            Collections.sort(jobs, new ScheduledTimeComparator());
            for (Job job:jobs) {
                job.schedule(executor, callback);
            }    
            this.jobEventBus.post(new JobListReorderedEvent(this,jobs));
        } else {
            Job previousJob=null;
            for (Job job:jobs) {
                if (previousJob!=null && job.getScheduledTimeMS() < previousJob.getScheduledTimeMS()) {
                    cancelAllJobs();
                    throw new JobException(job,"Job scheduling time is out of order. Check scheduled start time for jobs");
                } else {   
                    job.schedule(executor, callback);
                }
                previousJob=job;
            }
        }
        executor.shutdown();
    }
    
    /**
     * Attempts to cancel all jobs in job queue. Completed jobs are unaffected. Scheduled but not executed jobs are cancelled. Running jobs are interrupted.
     * @return List of Runnables that have not been executed
     */
    public List<Runnable> cancelAllJobs() {
        cancellingAllJobs=true;
        List<Runnable> interruptedtasks=executor.shutdownNow();
        try {
            executor.awaitTermination(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(AcqJobManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (Job j:jobs) {
            if (j.futureresult!=null) {
                if (j.futureresult.isCancelled()) {
                    //Does not appear to return appropriate value
                    //IJ.log("    isCancelled: "+j.toString());
                } else if (j.futureresult.isDone()) {
                    //IJ.log("    isDone: "+j.toString());
                } else {
                    try {
                        j.futureresult.get(100,TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ex) {
                        IJ.log("    IE: "+j.toString());
                    } catch (ExecutionException ex) {
                        IJ.log("    EE: "+j.toString());
                    } catch (CancellationException ex) {
                        IJ.log("    CE: "+j.toString());
                    } catch (TimeoutException ex) {
                        IJ.log("    TE: "+j.toString());
                        j.updateAndBroadcastStatus(Job.Status.CANCELLED);
                    } catch (Exception ex) {
                        if (ex instanceof JobException) {
                            IJ.log("    Job E: "+((JobException)ex).getMessage()+", "+j.toString());                            
                        } else {
                            IJ.log("    Other E: "+j.toString());
                        }
                    }
                }
            } else {
                IJ.log("    futureresult=null: "+j.toString());
                j.updateAndBroadcastStatus(Job.Status.CANCELLED);
            }
        }
//        getActiveJobs();//for debugging
        return interruptedtasks;
    }
    
    /**
     * Jumps to execute the next scheduled job in the queue. The next job's execution schedule is honored.
     */
    public void skipToNextJob() {
        if (!hasActiveJobs() || currentJob==null) {
            return;
        }
        if (currentJob!=null) {
            currentJob.requestCancel(true);
        }
    }
    
    /**
     * 
     * @return reference to job that is currently executing or is the next in the job queue 
     */
    public synchronized Job getCurrentJob() {
        return currentJob;
    }
    
    /**
     * 
     * @return list of jobs in queue that have run and completed. Completeness is determined by job's isFinished() method 
     */
    public synchronized List<Job> getCompletedJobs() {
        List completed=new ArrayList();
        for (Job job:jobs) {
            if (job.isFinished()) {
                completed.add(job);
            }
        }
        return completed;
    }
    
    /**
     * 
     * @return true if there are any jobs in queue regardless of status. 
     */
    public synchronized boolean hasJobsInQueue() {
        return !jobs.isEmpty();
    }
    
    /**
     * 
     * @return list of jobs in queue that have not run yet.
     */
    public synchronized List<Job> getActiveJobs() {
        List active=new ArrayList();
        for (Job job:jobs) {
            IJ.log("Job: "+job.getId()+", "+job.getStatus().toString());
            if (!job.isFinished()) {
                IJ.log("    active Job: "+job.getId()+", "+job.getStatus().toString());
                active.add(job);
            }
        }
        return active;
    }
    
    /**
     * 
     * @return true if jobs are in queue that have been scheduled but not run yet. 
     */
    public boolean hasActiveJobs() {
        return !getActiveJobs().isEmpty();
    }
    
    /**
     * 
     * @return list of all jobs in queue regardless of status. 
     */
    public synchronized List<Job> getJobs() {
        return jobs;
    }
    
    /**
     * 
     * @return true is executor is running a job 
     */
    public synchronized boolean hasJobRunning() {
        return getCurrentJob()!=null;
    }    
    
    /**
     * Shuts down the job executor. From this point onward no new jobs can be created/scheduled. Call reset first to create/schedule new jobs.
     */
    public void shutdown() {
        executor.shutdown();
    }
    
    /**
     * Tries to clear all jobs in queue.
     * @throws JobException if job executor is not terminated (active jobs in queue)
     */
    public void clearJobs() throws JobException {
        if (!executor.isTerminated()) {
            throw new JobException(null,"Error in JobManager: cannot clear active jobs.");
        } else {
            jobs.clear();
            currentJob=null;
        }
    }
    
    /**
     *
     * @param parent
     * @return
     */
    public JobStatusWindow createJobStatusWindow(Frame parent) {
        if (statusWindow==null) {
            statusWindow=new JobStatusWindow(parent,this,jobs);
            jobEventBus.register(statusWindow);
            statusWindow.addWindowListener(new WindowAdapter() {
            
                @Override
                public void windowClosing(WindowEvent evt) {
                    IJ.log("STATUSWINDOW==null: "+Boolean.toString(statusWindow==null));
                    jobEventBus.unregister(statusWindow);
                    statusWindow=null;
                }

            });
        }        
        return statusWindow;
    }
    
    /**
     * Cancels all active jobs, clears all jobs from queue, and creates a new job executor instance.
     * @return List of all unfinished Runnables
     */
    public List<Runnable> reset() {
        List<Runnable> unfinishedJobs=cancelAllJobs();
        executor=JobManager.ExecutorFactory();
        cancellingAllJobs=false;
        jobs.clear();
        currentJob=null;
        return unfinishedJobs;
    }

    /**
     * 
     * @param force if true, all active tasks will be cancelled
     * @return true if close was successful. Returns false if force==true and hasActiveJobs==true
     */
    public boolean close(boolean force) {
        if (!force && hasActiveJobs()) {
            return false;
        }
        reset();
        return true;
    }
}
