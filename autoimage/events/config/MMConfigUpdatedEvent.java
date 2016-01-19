package autoimage.events.config;

import autoimage.data.acquisition.AcqSetting;
import autoimage.data.acquisition.MMConfig;

/**
 *
 * @author Karsten Siller
 */
public class MMConfigUpdatedEvent extends SingleMMConfigEvent {

    public MMConfigUpdatedEvent(AcqSetting setting, MMConfig config) {
        super(setting, config);
    }
    
}
