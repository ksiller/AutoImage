package autoimage.events.config;

import autoimage.data.acquisition.AcqSetting;
import autoimage.data.acquisition.MMConfig;

/**
 *
 * @author Karsten Siller
 */
public class MMConfigDeletedEvent extends SingleMMConfigEvent {

    public MMConfigDeletedEvent(AcqSetting setting, MMConfig config) {
        super(setting, config);
    }
    
}