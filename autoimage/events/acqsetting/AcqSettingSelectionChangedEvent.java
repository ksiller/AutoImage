package autoimage.events.acqsetting;

import autoimage.data.acquisition.AcqSetting;

/**
 *
 * @author Karsten Siller
 */
public class AcqSettingSelectionChangedEvent {
    
    private final AcqSetting previousAcqSetting;
    private final AcqSetting currentAcqSetting;
    
    public AcqSettingSelectionChangedEvent(AcqSetting previous, AcqSetting current) {
        this.previousAcqSetting=previous;
        this.currentAcqSetting=current;
    }
    
    public AcqSetting getPreviousAcqSetting() {
        return this.previousAcqSetting;
    }
    
    public AcqSetting getCurrentAcqSetting() {
        return this.currentAcqSetting;
    }
    
}
