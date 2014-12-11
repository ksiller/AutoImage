/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author Karsten
 */
public class Sequence {
    
    private AcqLayout layout;
    private AcqSetting setting;
    private DefaultMutableTreeNode imageProcRoot;
    private TileManager tileManager;
    
    public Sequence() {
//        layout=new AcqLayout(null,null);
        layout=new AcqLayout();
    }
    
}
