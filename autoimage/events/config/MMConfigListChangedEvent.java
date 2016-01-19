package autoimage.events.config;

import autoimage.data.acquisition.AcqSetting;
import autoimage.data.acquisition.MMConfig;
import java.util.List;

/**
 *
 * @author Karsten Siller
 */
public class MMConfigListChangedEvent extends MMConfigListEvent {
    
    public MMConfigListChangedEvent(AcqSetting setting, List<MMConfig> list) {
        super(setting,list);
    }
    
}
