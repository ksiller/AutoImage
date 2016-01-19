package autoimage.events.config;

import autoimage.data.acquisition.AcqSetting;
import autoimage.data.acquisition.MMConfig;

/**
 *
 * @author Karsten Siller
 */
public class MMConfigAddedEvent extends SingleMMConfigEvent {

    public MMConfigAddedEvent(AcqSetting setting, MMConfig config) {
        super(setting, config);
    }
    
}