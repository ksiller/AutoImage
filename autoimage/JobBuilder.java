package autoimage;

import com.google.common.eventbus.EventBus;

/**
 *
 * @author Karsten
 */
public abstract class JobBuilder<T> {
        private final String identifier;
        private long scheduledTimeMS = -1;
        private EventBus jeventBus = null;
        private Runnable preInitCallback = null;
        private Runnable postInitCallback = null;
        private Runnable preRunCallback = null;
        private Runnable postRunCallback = null;
        private long callbackTimeoutMS = 0;
        private T properties = null;
        
        public JobBuilder(String id) {
            identifier=id;
        }
        
        public JobBuilder eventBus(EventBus eb) {
            jeventBus=eb;
            return this;
        }
        
        public JobBuilder scheduleFor(long timeMS) {
            scheduledTimeMS=timeMS;
            return this;
        }
        
        public JobBuilder preInitcallback(Runnable preInit) {
            preInitCallback=preInit;
            return this;
        }
        
        public JobBuilder postInitcallback(Runnable postInit) {
            postInitCallback=postInit;
            return this;
        }
        
        public JobBuilder preRuncallback(Runnable preRun) {
            preRunCallback=preRun;
            return this;
        }
        
        public JobBuilder postRuncallback(Runnable postRun) {
            postRunCallback=postRun;
            return this;
        }
        
        public JobBuilder callbackTimeout(long timeout) {
            callbackTimeoutMS=timeout;
            return this;
        }
        
        public JobBuilder properties(T props) {
            properties=props;
            return this;
        }
                
        public abstract Job<T> build();
        
    }
