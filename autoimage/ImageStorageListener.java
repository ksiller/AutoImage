/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import java.io.File;

/**
 *
 * @author Karsten
 */
public interface ImageStorageListener {
    
    public void storedImageReceived(File fn);
    
    public void imageStorageFinished();
}
