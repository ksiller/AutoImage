/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.data.acquisition;

import autoimage.data.acquisition.AcqSetting;
import autoimage.api.IAcqLayout;
import autoimage.data.ProcSetting;

/**
 *
 * @author Karsten
 */
public class SequenceConfig {
    
    private final AcqSetting acqSetting;
    private final IAcqLayout acqLayout;
    private final ProcSetting procSetting;
    
    public SequenceConfig (AcqSetting acqS, IAcqLayout layout, ProcSetting procS) {
        acqSetting=acqS;
        acqLayout=layout;
        procSetting=procS;
    }
    
    public AcqSetting getAcqSetting() {
        return acqSetting;
    }
    
    public IAcqLayout getAcqLayout() {
        return acqLayout;
    }
    
    public ProcSetting getProcSetting() {
        return procSetting;
    }
}
