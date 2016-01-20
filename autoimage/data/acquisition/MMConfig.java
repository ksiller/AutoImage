package autoimage.data.acquisition;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Karsten Siller
 */
public class MMConfig {
    
    private final String groupName;
    private final String configName;
    private final List<String> availablePresets;
    private final boolean isChannel;
    private final boolean isValid;
//    private final boolean controlsMagnification;
    
    private MMConfig(String name, String selpreset, List<String> presets, boolean isChannel, boolean isValid) {//, boolean ctrMagnification) {
        this.groupName=name;
        this.configName=selpreset;
        //consider creating defensive copy of each preset in list?
        this.availablePresets=new ArrayList<String>(presets);
        this.isChannel=isChannel;
        this.isValid=isValid;
//        this.controlsMagnification=ctrMagnification;
    }
    
    public String getName() {
        return this.groupName;
    }
    
    public String getSelectedPreset() {
        return this.configName;
    }
    
    public List<String> getAvailablePresets() {
        return this.availablePresets;
    }
    
    public boolean isChannel() {
        return this.isChannel;
    }
    
    public boolean isValid() {
        return this.isValid;
    }
    
    public Builder copy() {
        Builder copyBuilder = new Builder()
                .name(this.groupName)
                .availablePresets(this.availablePresets)
                .selectPreset(this.configName)
                .controlChannel(this.isChannel)
                .validate(this.isValid);
        return copyBuilder;
    }
    
/*    public boolean controlsMagnification() {
        return this.controlsMagnification;
    } 
*/    
    public static class Builder {
    
        private String configName;
        private String selectedPreset = "<empty>";
        private List<String> availablePresets;
        private boolean isChannel = false;
        private boolean isValid = true;
//        private boolean controlsMagnification = false;
        
        public Builder () {    
        }
        
/*        public Builder (String groupName, List<ConfigPreset> presets) {
            this.configName=groupName;
            this.availablePresets=presets;
            if (presets!=null && !presets.isEmpty()) {
                this.configName=presets.get(0);
            }
        }*/
        
        public Builder name(String name) {
            this.configName=name;
            return this;
        }
        
        public Builder availablePresets(List<String> presets) {
            this.availablePresets=presets;
            return this;
        }
        
        public Builder selectPreset(String preset) {
            this.selectedPreset=preset;
            return this;
        }
        
        public Builder controlChannel(boolean isCh) {
            this.isChannel=isCh;
            return this;
        }
        
        public Builder validate(boolean valid) {
            this.isValid=valid;
            return this;
        }
/*        public Builder controlMagnification(boolean ctrlMag) {
            this.controlsMagnification=ctrlMag;
            return this;
        }
*/        
        public MMConfig build() {
            MMConfig config =new MMConfig(
                    this.configName, 
                    this.selectedPreset,
                    this.availablePresets,
                    this.isChannel,
                    this.isValid);
//                    this.controlsMagnification);
            return config;
        }
    }
}
