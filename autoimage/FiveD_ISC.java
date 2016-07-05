package autoimage;

import autoimage.gui.views.AcqFrame;
import com.google.common.eventbus.EventBus;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

/**
 *
 * @author Karsten Siller, University of Virginia
 */
public class FiveD_ISC implements MMPlugin {
   public static String menuName = "5dISC";
   public static String tooltipDescription = "5dISC: 5d Image Sequence Controller";
   private AcqFrame acqFrame;
   private EventBus eventbus = new EventBus();
    
    @Override
    public void dispose() {
        if (acqFrame.cleanUp()); {        
            acqFrame.dispose();
        }
    }

    @Override
    public void setApp(ScriptInterface app) {
        if (acqFrame == null) {
            acqFrame = new AcqFrame(app, eventbus);
        }
        if (!acqFrame.isInitialized()) {
            acqFrame=null;
        }
    }

    @Override
    public void show() {
        if (acqFrame!=null && acqFrame.isInitialized())
            acqFrame.setVisible(true);
    }

    @Override
    public String getDescription() {
        return tooltipDescription;
    }

    @Override
    public String getInfo() {
        return tooltipDescription;
    }

    @Override
    public String getVersion() {
        return "Version 1.0";
    }

    @Override
    public String getCopyright() {
        return "Karsten Siller - University of Virginia, 2015"; 
    }
        
}
