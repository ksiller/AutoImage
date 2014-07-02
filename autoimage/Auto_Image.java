package autoimage;

import org.micromanager.api.ScriptInterface;

/**
 *
 * @author Karsten Siller, University of Virginia
 */
public class Auto_Image implements org.micromanager.api.MMPlugin{
   public static String MENU_NAME = "AutoImage";
   public static String TOOL_DESCR = "AutoImage: Automatic Image Acquisition and Processing";
 //  private ScriptInterface gui;
   private AcqFrame acqFrame;
    
    @Override
    public void dispose() {
        acqFrame.cleanUp();        
        acqFrame.dispose(); //create new onClosingWindow method as event handler to allow saving of Prefs before window is closed
    }

    @Override
    public void setApp(ScriptInterface app) {
      //  gui = app;                                        
        if (acqFrame == null) {
            acqFrame = new AcqFrame(app);//gui);
        }    
        acqFrame.setVisible(true);
    }

    @Override
    public void show() {
        if (acqFrame!=null)
            acqFrame.setVisible(true);
    }

    @Override
    public String getDescription() {
        return TOOL_DESCR;
    }

    @Override
    public String getInfo() {
        return TOOL_DESCR;
    }

    @Override
    public String getVersion() {
        return "Version 1.0";
    }

    @Override
    public String getCopyright() {
        return "University of Virginia, 2014"; 
    }
    
    
}
