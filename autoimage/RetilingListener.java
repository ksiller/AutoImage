package autoimage;

import autoimage.api.SampleArea;

/**
 *
 * @author Karsten Siller
 */
public interface RetilingListener {
    
    public void areaRetiled(SampleArea a);
    public void retilingAborted();
    public void retilingFinished();
}
