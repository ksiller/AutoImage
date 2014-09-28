/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage;

import java.util.List;

/**
 *
 * @author Karsten
 */
public interface IStagePosListener {
    
    public void stagePosAdded(String area, Vec3d stagePos, Object source);
    public void stagePosListAdded(String area, List<Vec3d> stagePosList, Object source);
}
