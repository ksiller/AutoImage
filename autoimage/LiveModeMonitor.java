/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage;

import ij.IJ;
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
    private final List<ILiveListener> listeners;
    private final ExecutorService listenerExecutor;
    
        
        public LiveModeMonitor(ScriptInterface gui, int interval) {
            this.gui=gui;
            listeners=new ArrayList<ILiveListener>();
            interval_ms=interval;
            currentMode=!gui.isLiveModeOn();
            listenerExecutor = Executors.newFixedThreadPool(1);
            publish(currentMode);
        }
        
        public LiveModeMonitor(ScriptInterface gui) {
            this(gui,100);
        }

        synchronized
        public void addListener(ILiveListener listener) {
            if (!listeners.contains(listener))
                listeners.add(listener);
            IJ.log("LiveModeMonitor: "+listeners.size()+" listeners");
        }
        
        synchronized
        public void removeListener(ILiveListener listener) {
            if (listeners != null)
                listeners.remove(listener);
            IJ.log("LiveModeMonitor: "+listeners.size()+" listeners");
        }
        
        public int getNoOfListeners() {
            if (listeners != null)
                return listeners.size();
            else
                return 0;
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
/*                      listenerExecutor.submit(new Runnable() {
	                    @Override
	                    public void run() {*/
                                try {
	                            l.liveModeChanged(currentMode);
                                } catch (RuntimeException e) {
                                    IJ.log(getClass().getName()+": Error in listener. "+e.getMessage());
                                    listeners.remove(l);
                                }    
/*	                    }
	                });*/
	            }
	         }
            }    
        }


        @Override
        public void done() {
        }
    }
