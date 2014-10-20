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
import org.micromanager.api.ScriptInterface;

/**
 *
 * @author Karsten
 */
class StagePosMonitor extends SwingWorker<Void, Double[]> {

    private ScriptInterface gui;
    private CMMCore core;
    private String xyStageName;
    private String focusDeviceName;
    private int interval_ms;//interval for polling live mode in millisecond
    private Double[] currentPos;
    private boolean readZPos;
    private final List<IStageMonitorListener> listeners;
    private final ExecutorService listenerExecutor;
    
        
    public StagePosMonitor(ScriptInterface gui, int interval) {
        this.gui=gui;
        listeners=new ArrayList<IStageMonitorListener>();
        setCore(gui.getMMCore());
        interval_ms=interval;
        readZPos=true;
        
        listenerExecutor = Executors.newFixedThreadPool(1);
        currentPos=readStagePosition();
        publish(currentPos);
    }

    public StagePosMonitor(ScriptInterface gui) {
        this(gui,100);
    }

    public void setCore(CMMCore core) {
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
        IJ.log("StagePosMonitor.addListener: "+listener.getClass().getName()+", "+listeners.size()+" listeners");
    }

    synchronized
    public void removeListener(IStageMonitorListener listener) {
        if (listener==null) {
            IJ.log("StagePosMonitor.removeListener: listener==null");
        }
        if (listeners != null)
            listeners.remove(listener);
        IJ.log("StagePosMonitor.removeListener: "+listener.getClass().getName()+", "+listeners.size()+" listeners remaining");
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
                if (!newPos[0].equals(currentPos[0]) || !newPos[1].equals(currentPos[1]) || !newPos[2].equals(currentPos[2]))
                    publish(newPos);
                currentPos=newPos;
            } catch (InterruptedException ex) {
                Logger.getLogger(AcqFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    
    /*  sends latest stage position to listeners    
    */
    @Override
    protected void process(final List<Double[]> newPosList) {
        if (newPosList!=null) {
            final Double[] lastPos=newPosList.get(newPosList.size()-1);
            synchronized (listeners) {
                for (final IStageMonitorListener l : listeners) {
/*                    listenerExecutor.submit(
                        new Runnable() {
                            @Override
                            public void run() {*/
                                try {
                                    l.stagePositionChanged(lastPos);
                                }
                                catch (RuntimeException e) {
                                    IJ.log(getClass().getName()+": Unexpected exception in listener."+ e.getMessage());
                                    listeners.remove(l);                               
                                }
/*                            }
                        });*/
                }
            }    
        }
    }


    @Override
    public void done() {
        IJ.log("StagePosMonitor.done.");
    }

}

    

