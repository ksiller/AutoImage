/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author Karsten
 */
public class ImageFileQueue extends LinkedBlockingQueue<File> {

    public static File POISON = new File("FILE_POISON");

    public static boolean isPoison(File f) {
        return (f!=null && f.getName().equals("FILE_POISON"));
    }
    
}
