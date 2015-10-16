
package autoimage.api;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author Karsten
 */
public class ImageFileQueue extends LinkedBlockingQueue<File> {

    public final static String POISON_NAME = "FILE_POISON";
    public final static File POISON = new File(POISON_NAME);

    public static boolean isPoison(File f) {
        return (f!=null && f.getName().equals(POISON_NAME));
    }
    
}
