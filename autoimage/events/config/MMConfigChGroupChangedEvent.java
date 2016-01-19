package autoimage.events.config;

import autoimage.data.acquisition.AcqSetting;

/**
 *
 * @author Karsten Siller
 */
public class MMConfigChGroupChangedEvent {
    
    private final AcqSetting acqSetting;
    private final String previousChannelGroup;
    private final String currentChannelGroup;
    
    public MMConfigChGroupChangedEvent (AcqSetting setting, String previous, String current) {
        this.acqSetting=setting;
        this.previousChannelGroup=previous;
        this.currentChannelGroup=current;
    }
    
    public AcqSetting getAcqSetting() {
        return this.acqSetting;
    }
    
    public String getPreviousChannelGroup() {
        return this.previousChannelGroup;
    }

    public String getCurrentChannelGroup() {
        return this.currentChannelGroup;
    }
}
