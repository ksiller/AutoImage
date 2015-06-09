/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import autoimage.area.Area;

/**
 *
 * @author Karsten
 */
public interface RetilingListener {
    
    public void areaRetiled(Area a);
    public void retilingAborted();
    public void retilingFinished();
}
