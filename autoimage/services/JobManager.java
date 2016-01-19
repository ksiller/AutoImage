package autoimage.services;

import autoimage.data.acquisition.Job;
import autoimage.data.acquisition.Job.Status;
import autoimage.events.job.JobStatusChangedEvent;
import autoimage.events.job.AllJobsCompletedEvent;
import autoimage.events.job.JobSwitchEvent;
import autoimage.events.job.JobListChangedEvent;
import autoimage.events.job.JobPlacedInQueueEvent;
import autoimage.gui.dialogs.JobStatusWindow;
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
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Karsten Siller
 */
public abstract class JobManager {
    
    public enum Order {FIFO, LIFO, TIME_SORTED};
    /**
     *
     */
    protected final EventBus jobEventBus;
    private ListeningScheduledExecutorService executor;
    private final List<Job> jobs;
    private final List<Job> jobsForPublic;
    private final Map<String, Job> jobMap;
    private final FutureCallback<Job> callback;
    private JobStatusWindow statusWindow;
    private volatile Job currentJob;
    private Order schedulingOrder;
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
            if (job1.getScheduledTimeMS() < job2.getScheduledTimeMS()) {
                return -1;
            }
            if (job1.getScheduledTimeMS() > job2.getScheduledTimeMS()){
                return 1;
            }
            return 0;
        }
    }

    //----------------------
    
    public JobManager() {
        jobEventBus= new EventBus(); 
        schedulingOrder=Order.FIFO;
        cancellingAllJobs=false;
        currentJob=null;
        executor = JobManager.ExecutorFactory();
        jobs=Collections.synchronizedList(new ArrayList<Job>());
        jobsForPublic=Collections.unmodifiableList(jobs);
        jobMap=new ConcurrentHashMap<String, Job>();
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
    public void setSchedulingOrder(Order order) {
        schedulingOrder=order;
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
        int index=-1;
        synchronized (jobs) {
            index=jobs.indexOf(e.getJob());
        }
        if (index<0) {
            //this means job is not in job list, do nothing
            return;
        }
        Status newStatus=e.getNewStatus();
        Status oldStatus=e.getOldStatus();
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
                    //more jobs, go to next in job list
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
    
    /**
     * Places a new job at the end of the job list
     * @param newJob The job object to be added to the job list
     * @throws JobException is thrown if the job executor has been shutdown and cannot accept new jobs. The job executor's shutDown() method is invoked at the end of scheduleJobs() method.
     */
    public boolean putJob(Job newJob) throws JobException {
        if (executor.isShutdown()) {
            throw new JobException(newJob, "AcquisitionManager cannot accept new jobs. Reset AcquisitonManager first");
        } 
        boolean addJob=false;
        if (newJob!=null) {
            synchronized (this) {
                addJob=!jobMap.containsKey(newJob.getId());
                if (addJob) {
                    jobs.add(newJob);
                    jobMap.put(newJob.getId(),newJob);
                }
            }
            if (addJob) {
                jobEventBus.post(new JobPlacedInQueueEvent(this,newJob));
            }
        }
        return addJob;
    }
    
    /**
     * Removes job from job list. Jobs can only be removed if not active (isActive()==false).
     * @param id identifier of job (is compared to field identifier of all Job objects in job list)
     * @return true is job was removed
     */
    public boolean removeJob(String id) {
        Job toRemove=null;
        Job previousJob;
        synchronized (this) {
            previousJob=currentJob;
            toRemove=jobMap.get(id);
            if (toRemove==null) {
                //not in job list --> do nothing
                return false;
            }
            if (toRemove.isActive()) {
                //cannot remove active job
                toRemove=null;
            } else {
                if (currentJob==toRemove) {
                    previousJob=currentJob;
                    currentJob=null;
                }
                jobMap.remove(id, toRemove);
                jobs.remove(toRemove);
            }
        }
        if (toRemove!=null) {
            jobEventBus.post(new JobListChangedEvent(this,jobsForPublic));
        }
        if (currentJob!=previousJob) {
            jobEventBus.post(new JobSwitchEvent(this,previousJob,currentJob));
        }
        return toRemove!=null;
    }

    /**
     * Searches job list and returns Job based on identifier
     * @param id identifier of job (is compared to field identifier of all Job objects in job list)
     * @return Job for which getId()==id
     */
    public Job getJob(String id) {
        return jobMap.get(id);
    }
    
    /**
     * Schedules all jobs. If sortedByScheduledTime == true, job list is resorted based on each job's scheduled time. If sortedByScheduleTime == false, jobs are not reordered based on scheduled time
     * @throws JobException is thrown if sortedByScheduleTime == false and the scheduled times for all jobs is out of order.
     */
    public synchronized void scheduleJobs() throws JobException {
        if (jobs==null || jobs.isEmpty()) {
            throw new JobException(null,"No jobs in queue.");
        }
        long lastStartTimeMS=-1;
        switch (schedulingOrder) {
            case TIME_SORTED: {
                Collections.sort(jobs, new ScheduledTimeComparator());
                for (Job job:jobs) {
                    job.schedule(executor, callback);
                }    
                jobEventBus.post(new JobListChangedEvent(this,jobsForPublic));
//                jobEventBus.post(new JobListChangedEvent(this,jobs));
                break;
            }
            case LIFO: {
                Collections.reverse(jobs);
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
                jobEventBus.post(new JobListChangedEvent(this,jobsForPublic));
//                jobEventBus.post(new JobListChangedEvent(this,jobs));
                break;
            }
            default: {
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
        }
        executor.shutdown();
    }
    
    /**
     * Attempts to cancel all jobs in job list. Completed jobs are unaffected. Scheduled but not executed jobs are cancelled. Running jobs are interrupted.
     * @return List of Runnables that have not been executed
     */
    public List<Runnable> cancelAllJobs() {
        if (cancellingAllJobs) {
            //prevent reentrance
            return null;
        }
        cancellingAllJobs=true;
        List<Runnable> interruptedtasks=executor.shutdownNow();
        try {
            executor.awaitTermination(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            IJ.log("Executor await termination interrupted Exception ");
            Logger.getLogger(AcqJobManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (Job j:jobs) {
            Future result=j.getResult();
            if (j.getResult()!=null) {
                if (result.isCancelled()) {
                    //Does not appear to return appropriate value
                    //IJ.log("    isCancelled: "+j.toString());
                } else if (result.isDone()) {
                    //IJ.log("    isDone: "+j.toString());
                } else {
                    try {
                        result.get(100,TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ex) {
                        IJ.log("    IE: "+j.toString());
                    } catch (ExecutionException ex) {
                        IJ.log("    EE: "+j.toString());
                    } catch (CancellationException ex) {
                        IJ.log("    CE: "+j.toString());
                        j.updateAndBroadcastStatus(Job.Status.CANCELLED);
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
        cancellingAllJobs=false;
        return interruptedtasks;
    }
    
    /**
     * Jumps to execute the next scheduled job in the list. The next job's execution schedule is honored.
     */
    public synchronized void skipToNextJob() {
        if (!hasActiveJobs() || currentJob==null) {
            return;
        }
        if (currentJob!=null) {
            currentJob.requestCancel(true);
        }
    }
    
    /**
     * 
     * @return reference to job that is currently executing or is the next in the job list 
     */
    public synchronized Job getCurrentJob() {
        return currentJob;
    }
    
    
    /**
     * 
     * @return list of jobs in list that have run and completed. Completeness is determined by job's isFinished() method 
     */
    public List<Job> getCompletedJobs() {
        List completed=new ArrayList();
        synchronized(jobs) {
            for (Job job:jobs) {
                if (job.isFinished()) {
                    completed.add(job);
                }
            }
        }
        return completed;
    }
    
    /**
     * 
     * @return true if there are any jobs in list regardless of status. 
     */
    public synchronized boolean hasJobsInQueue() {
        return !jobs.isEmpty();
    }
    
    /**
     * 
     * @return list of jobs in list that are scheduled and have not run yet.
     */
    public List<Job> getActiveJobs() {
        List active=new ArrayList();
        synchronized (jobs) {
            for (Job job:jobs) {
                IJ.log("Job: "+job.getId()+", "+job.getStatus().toString());
                if (Job.JobIsActive(job.getStatus())) {
                    IJ.log("    active Job: "+job.getId()+", "+job.getStatus().toString());
                    active.add(job);
                }
            }
        }
        return active;
    }
    
    /**
     * 
     * @return true if jobs are in list that have been scheduled but not run yet. 
     */
    public boolean hasActiveJobs() {
        return !getActiveJobs().isEmpty();
    }
    
    /**
     * 
     * @return list of all jobs in list regardless of status. 
     */
    public synchronized List<Job> getJobs() {
        return jobsForPublic;
//        return jobs;
    }
    
    /**
     * 
     * @return true is executor is running a job 
     */
    public boolean hasJobRunning() {
        return getCurrentJob()!=null;
    }    
    
    /**
     * Shuts down the job executor. From this point onward no new jobs can be created/scheduled. Call reset first to create/schedule new jobs.
     */
    public void shutdown() {
        executor.shutdown();
    }
    
    /**
     * Tries to clear all jobs in list.
     * @throws JobException if job executor is not terminated (active jobs in list)
     */
    public void clearJobs() throws JobException {
        Job previousJob=null;
        if (!executor.isTerminated()) {
            throw new JobException(null,"Error in JobManager: cannot clear active jobs.");
        } else {
            synchronized (this) {
                if (jobs.isEmpty()) {
                    //ensures that repeated (reentrant) calls do not fire events again
                    return;
                }
                jobs.clear();
                jobMap.clear();
                previousJob=currentJob;
                currentJob=null;
            }
            jobEventBus.post(new JobListChangedEvent(this,jobsForPublic));
            jobEventBus.post(new JobSwitchEvent(this,previousJob,null));
        }
    }
    
    /**
     *
     * @param parent
     * @return
     */
    public JobStatusWindow createJobStatusWindow(Frame parent) {
        if (statusWindow==null) {
            statusWindow=new JobStatusWindow(parent,this);
//            statusWindow=new JobStatusWindow(parent,this,jobs);
            jobEventBus.register(statusWindow);
            statusWindow.addWindowListener(new WindowAdapter() {
            
                @Override
                public void windowClosing(WindowEvent evt) {
                    jobEventBus.unregister(statusWindow);
                    statusWindow=null;
                }

            });
        }        
        return statusWindow;
    }
    
    /**
     * Cancels all active jobs, clears all jobs from list, and creates a new job executor instance.
     * @return List of all unfinished Runnables
     */
    public List<Runnable> reset() {
        List<Runnable> unfinishedJobs;
        Job previousJob=null;
        synchronized (this) {
            unfinishedJobs=cancelAllJobs();
            executor=JobManager.ExecutorFactory();
            cancellingAllJobs=false;
            previousJob=currentJob;
            jobs.clear();
            jobMap.clear();
            currentJob=null;
        }
        jobEventBus.post(new JobListChangedEvent(this,jobsForPublic));
        jobEventBus.post(new JobSwitchEvent(this,previousJob,null));
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
