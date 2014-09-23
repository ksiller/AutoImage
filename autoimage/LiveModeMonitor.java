/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import org.micromanager.api.ScriptInterface;

/**
 *
 * @author Karsten
 */
class LiveModeMonitor extends SwingWorker<Void, Boolean> {

    private ScriptInterface gui;
    private int interval_ms;//interval for polling live mode in millisecond
    private boolean currentMode;
    private List<ILiveListener> listeners;
    private final ExecutorService listenerExecutor;
    
        
        public LiveModeMonitor(ScriptInterface gui, int interval) {
            this.gui=gui;
            interval_ms=interval;
            currentMode=!gui.isLiveModeOn();
            listenerExecutor = Executors.newFixedThreadPool(1);
            publish(currentMode);
        }
        
        public LiveModeMonitor(ScriptInterface gui) {
            this(gui,100);
        }

        public void addListener(ILiveListener listener) {
            if (listeners==null)
                listeners=new ArrayList<ILiveListener>();
            listeners.add(listener);
        }
        
        public void removeListener(ILiveListener listener) {
            if (listeners != null)
                listeners.remove(listener);
        }
        
        @Override
        public Void doInBackground() {
            while (!this.isCancelled()) {
                try {
                    Thread.sleep(interval_ms);
                    publish(gui.isLiveModeOn());
                } catch (InterruptedException ex) {
                    Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return null;
        }

        public boolean isLive() {
            return gui.isLiveModeOn();
        }
        
        @Override
        protected void process(final List<Boolean> newMode) {
            //get last live mode
            boolean b = newMode.get(newMode.size() - 1);
            // notify listeners if live mode has changed
            if (currentMode != b) {
                currentMode=b;
                synchronized (listeners) {
	            for (final ILiveListener l : listeners) {
	               listenerExecutor.submit(
	                       new Runnable() {
	                          @Override
	                          public void run() {
	                             l.liveModeChanged(currentMode);
	                          }
	                       });
	            }
	         }
            }    
        }


        @Override
        public void done() {
        }
    }
