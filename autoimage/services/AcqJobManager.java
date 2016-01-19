package autoimage.services;

import autoimage.data.acquisition.SequenceConfig;
import autoimage.data.acquisition.AcquisitionJob;
import autoimage.data.acquisition.Job;
import autoimage.services.JobManager;
import autoimage.data.acquisition.AcqSetting;
import autoimage.events.job.AcqJobActiveProcessorsChangedEvent;
import autoimage.events.job.JobStatusChangedEvent;
import com.google.common.eventbus.EventBus;
import ij.IJ;
import java.util.Calendar;
import java.util.GregorianCalendar;
import mmcorej.CMMCore;
import org.micromanager.api.IAcquisitionEngine2010;
import org.micromanager.utils.AutofocusManager;
//import org.micromanager.data.DataManager;

/**
 *
 * @author Karsten Siller
 */
public class AcqJobManager extends JobManager {
    
    protected IAcquisitionEngine2010 engine;
    protected CMMCore core;
    protected AutofocusManager afManager;
    //protected DataManager dataManager;
    
    public static class JobBuilder { 

        private final AcqJobManager manager;
        private final SequenceConfig acqConfig;
        private final String identifier;
        private long scheduledTimeMS = -1;
        private EventBus jeventBus = null;
        private Runnable preInitCallback = null;
        private Runnable postInitCallback = null;
        private Runnable preRunCallback = null;
        private Runnable postRunCallback = null;
        private long callbackTimeoutMS = 0;
        private String imageStorageRoot = "";
        private boolean showAcquisitionDisplay = false;
              
        public JobBuilder(AcqJobManager mgr, String id, SequenceConfig config) {
            manager=mgr;
            identifier=id;
            acqConfig=config;
        }
        
        public JobBuilder imageStorageRoot(String imgRoot) {
            imageStorageRoot=imgRoot;
            return this;
        }
        
        public JobBuilder acquisitionDisplay(boolean show) {
            showAcquisitionDisplay=show;
            return this;
        }
        
        public JobBuilder preInitCallback(Runnable preInit) {
            preInitCallback=preInit;
            return this;
        }
        
        public JobBuilder postInitCallback(Runnable postInit) {
            postInitCallback=postInit;
            return this;
        }
        
        public JobBuilder preRunCallback(Runnable preRun) {
            preRunCallback=preRun;
            return this;
        }
        
        public JobBuilder postRunCallback(Runnable postRun) {
            postRunCallback=postRun;
            return this;
        }
        
        public JobBuilder callbackTimeout(long timeout) {
            callbackTimeoutMS=timeout;
            return this;
        }

        public JobBuilder scheduleFor(long timeMS) {
            scheduledTimeMS=timeMS;
            return this;
        }
                     
        public AcquisitionJob build() {
            if (manager==null) {
                throw new IllegalArgumentException("AcqJobManager object cannot be null.");
            }
            AcquisitionJob newJob=new AcquisitionJob.AcqJobBuilder(identifier)
                .eventBus(manager.jobEventBus)
                .acqEngine(manager.engine)
                .cmmcore(manager.core)
                .autofocusManager(manager.afManager)
//                .dataManager(manager.dataManager)
                .imageStorageRoot(imageStorageRoot)
                .acquisitionDisplay(showAcquisitionDisplay)
                .preInitCallback(preInitCallback)
                .postInitCallback(postInitCallback)
                .preRunCallback(preRunCallback)
                .postRunCallback(postRunCallback)
                .callbackTimeout(callbackTimeoutMS)
                .configure(acqConfig)
                .build();
            return newJob;
        } 
    }
    
    private AcqJobManager(IAcquisitionEngine2010 eng, CMMCore c, AutofocusManager am) {
        super();
        engine=eng;
        core=c;
        IJ.log(this.getClass().getName()+": "+(core==null ? "core=null" : "core API "+core.getAPIVersionInfo()));
//        dataManager=dm;
        afManager=am;
    }

    public static AcqJobManager ManagerFactory (IAcquisitionEngine2010 eng, CMMCore c, AutofocusManager am) {
        AcqJobManager manager=new AcqJobManager(eng, c, am);
        IJ.log("AcqJobManager.ManagerFactory: "+(c==null ? "core=null" : "core API "+c.getAPIVersionInfo()));
        manager.registerForEvents(manager);
        return manager;
    }
        
    public void processorUpdated(AcqJobActiveProcessorsChangedEvent e) {
        IJ.log("ACTIVE PROCESSORS");
        for (Object o:e.getProcessors()) {
            IJ.log("    active: "+o.toString());
        }
    }

    public void statusUpdated(JobStatusChangedEvent e) {
        IJ.log("JOB STATUS changed: "+e.getNewStatus().toString());
    }    
/*    
    public void createAndScheduleJob(AcqSetting setting, IAcqLayout layout, long startTimeMS) throws MMException {
        if (executor.isShutdown() || jobQueueClosed) {
            throw new MMException("AcqJobManager cannot accept new jobs. Reset AcquisitonManager first");
        }
        AcquisitionJob newJob=new AcquisitionJob(jobEventBus,studio,setting,layout,imageDestPath);
        if (sortByScheduledTime) {
            jobs.add(newJob);
            Collections.sort(jobs, new ScheduledTimeComparator());
        } else {
            if (jobs.isEmpty()) {
                jobs.add(newJob);
            } else {
                List<Job> copy=new ArrayList<Job>(jobs);
                Collections.sort(copy, new ScheduledTimeComparator());
                long lastStartTimeMS=copy.get(copy.size()-1).getScheduledTimeMS();
                copy.clear();
                if (lastStartTimeMS <= startTimeMS) {
                    jobs.add(newJob);
                } else {
                    cancelAllJobs();
                    throw new MMException("Job scheduling time is out of order. Check scheduled start time for jobs");
                }
            }
        }
        eventbus.post(new JobCreatedEvent(newJob));
//        newJob.initialize();
        newJob.schedule(executor,callback,startTimeMS);
    }
*/        

    public void updateStartTimes(long reference) throws JobException {
        Calendar refTime=new GregorianCalendar();
        refTime.setTimeInMillis(reference);
        IJ.log("Acquisition reference start time: "+refTime.getTime().toString());
        long lastms=reference;
        long ms=0;
        for (Job job:getJobs()) {
            if (job.getStatus()!=Job.Status.CREATED) {
                throw new JobException(job,"Job has been scheduled alreaydy. Scheduled time cannot be changed.");
            }
            if (!(job.getConfiguration() instanceof SequenceConfig)) {
                throw new JobException(job,"Job is not instance of AquisitionJob class");
            }
            AcqSetting setting=((SequenceConfig)job.getConfiguration()).getAcqSetting();
            switch (setting.getStartTime().type) {
                case AcqSetting.ScheduledTime.ASAP: {
                    //convert to absolute time 
                    ms=lastms;
                    break;
                }
                case AcqSetting.ScheduledTime.DELAY: {
                    //convert to absolute time    
                    ms=lastms + setting.getStartTime().startTimeMS;
//                    IJ.log("lastms:"+Long.toString(lastms)+", ms:"+Long.toString(ms)+", startTime: "+Long.toString(setting.getStartTime().startTimeMS));
                    break;
                }
                case AcqSetting.ScheduledTime.ABSOLUTE: {
                    ms=setting.getStartTime().startTimeMS;
                    break;
                }
            }    
            job.requestRescheduling(ms);
            Calendar scheduled=new GregorianCalendar();
            scheduled.setTimeInMillis(ms);
            IJ.log(setting.getName() +" scheduled for: "+scheduled.getTime().toString());
            
            lastms=ms;
        }        
    }
    /*    
    public void createAndScheduleJobs(final List<AcqSetting> settings, List<IAcqLayout> layouts) throws MMException, InvalidParameterException {
        if (settings==null || layouts==null || settings.isEmpty() || settings.size()!=layouts.size()) {
            throw new InvalidParameterException("Invalid acquisition settings or layout(s).");
        } 
        List<Long> startTimes=calculateStartTimes(settings, System.currentTimeMillis());
        for (int i=0; i<settings.size(); i++) {
            createAndScheduleJob(settings.get(i), layouts.get(i), startTimes.get(i));
        }
    }
    
    public void createAndScheduleJobs(final List<AcqSetting> settings, IAcqLayout layout) throws MMException, InvalidParameterException {
        if (settings==null || settings.isEmpty() || layout==null) {
            throw new InvalidParameterException("Invalid acquisition settings or layout(s).");
        } 
        List<Long> startTimes=calculateStartTimes(settings, System.currentTimeMillis());
        for (int i=0; i<settings.size(); i++) {
            createAndScheduleJob(settings.get(i), layout, startTimes.get(i));
        }
    }
      */  
    


}
