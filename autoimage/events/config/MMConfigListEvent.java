package autoimage.events.config;

import autoimage.data.acquisition.AcqSetting;
import autoimage.data.acquisition.MMConfig;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Karsten Siller
 */
public class MMConfigListEvent {
    
    private final AcqSetting acqSetting;
    private final List<MMConfig> mmConfigList;
    
    public MMConfigListEvent(AcqSetting setting, List<MMConfig> list) {
        this.acqSetting=setting;
        this.mmConfigList=list;
    }
    
    public AcqSetting getAcqSetting() {
        return this.acqSetting;
    }
    
    public List<MMConfig> getMMConfigList() {
        return this.mmConfigList;
    }
}
