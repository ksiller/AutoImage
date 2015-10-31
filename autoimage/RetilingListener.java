package autoimage;

import autoimage.api.BasicArea;

/**
 *
 * @author Karsten Siller
 */
public interface RetilingListener {
    
    public void areaRetiled(BasicArea a);
    public void retilingAborted();
    public void retilingFinished();
}
