package autoimage;

import ij.IJ;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import mmcorej.CMMCore;

/**
 *
 * @author Karsten Siller
 */
class StagePosMonitor extends SwingWorker<Void, Double[]> {

    private CMMCore core;
    private String xyStageName;
    private String focusDeviceName;
    private int interval_ms;//interval for polling live mode in millisecond
    private Double[] currentPos;
    private boolean readZPos;
    private final List<IStageMonitorListener> listeners;
    private final ExecutorService listenerExecutor;
    
        
    public StagePosMonitor(CMMCore core, int interval, boolean readZ) {
        listeners=new ArrayList<IStageMonitorListener>();
        setCore(core);
        interval_ms=interval;
        readZPos=readZ;
        
        listenerExecutor = Executors.newFixedThreadPool(1);
        currentPos=readStagePosition();
        publish(currentPos);
    }

    public StagePosMonitor(CMMCore core, int interval) {
        this (core, interval, true);
    }
    
    public StagePosMonitor(CMMCore core) {
        this(core,100, true);
    }

    public final void setCore(CMMCore core) {
        this.core=core;
        xyStageName=core.getXYStageDevice();
        focusDeviceName=core.getFocusDevice();
    }
    
    public void setXYStageDevice(String name) {
        xyStageName=name;
    }
    
    public void setFocusDeviceName(String name) {
        focusDeviceName=name;
    }
    
    public void enableReadZPosition(boolean b) {
        readZPos=b;
    }
    
    //read xy position, z position if readZPos==true
    //returns null if position cannot be read
    private Double[] readStagePosition() {
        Double x=null;
        Double y=null;
        Double z=null;
        if (xyStageName!=null) {
            try {
                Point2D.Double xy = core.getXYStagePosition(xyStageName);
                x=xy.x;
                y=xy.y;
            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (readZPos && focusDeviceName!=null) {
            try {
                z=core.getPosition(focusDeviceName);
            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }    
        return new Double[]{x,y,z};
    }
    
    synchronized
    public void addListener(IStageMonitorListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
        IJ.log("StagePosMonitor.addListener: added "+listener.getClass().getName()+", "+listeners.size()+" total listeners");
    }

    synchronized
    public void removeListener(IStageMonitorListener listener) {
        if (listener==null) {
            IJ.log("StagePosMonitor.removeListener: listener==null");
        }
        if (listeners != null)
            listeners.remove(listener);
        IJ.log("StagePosMonitor.removeListener: removed "+listener.getClass().getName()+", "+listeners.size()+" listeners remaining");
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
                Double[] newPos = readStagePosition();
                for (int i=0; i<newPos.length; i++) {
                    if (newPos[i]!=null && !newPos[i].equals(currentPos[i])) {
                        publish(newPos);
                        break;
                    }
                }
                currentPos=newPos;
            } catch (InterruptedException ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    
    /**  
     * Sends latest stage position to listeners.
     * 
     */
    @Override
    protected void process(final List<Double[]> newPosList) {
        if (newPosList!=null) {
            final Double[] lastPos=newPosList.get(newPosList.size()-1);
            synchronized (listeners) {
                for (final IStageMonitorListener l : listeners) {
                    try {
                        l.stagePositionChanged(lastPos);
                    }
                    catch (RuntimeException e) {
                        IJ.log(getClass().getName()+": Unexpected exception in listener."+ e.getMessage());
                        listeners.remove(l);                               
                    }
                }
            }    
        }
    }


    @Override
    public void done() {
        IJ.log("StagePosMonitor.done.");
    }

}

    

