package autoimage.events.config;

import autoimage.data.acquisition.AcqSetting;
import autoimage.data.acquisition.MMConfig;

/**
 *
 * @author Karsten Siller
 */
public class SingleMMConfigEvent {
    
    private final AcqSetting acqSetting;
    private final MMConfig mmConfig;
    
    public SingleMMConfigEvent(AcqSetting setting, MMConfig config) {
        this.acqSetting=setting;
        this.mmConfig=config;
    }
 
    public AcqSetting getAcqSetting() {
        return acqSetting;
    }
    
    public MMConfig getMMConfig() {
        return mmConfig;
    }
        
}
