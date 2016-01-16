package autoimage.events;

import autoimage.data.acquisition.Job;

/**
 *
 * @author Karsten Siller
 */
public class JobImageStoredEvent<T extends Object> extends JobEvent {
    
    private final T image;
    private final long imageNumber;
    private final long totalImages;
    
    public JobImageStoredEvent (Job j, T ti, long imagesRec, long totImages) {
        super (j);
        image=ti;
        imageNumber=imagesRec;
        totalImages=totImages;
    }
    
    public T getImage() {
        return image;
    } 
    
    public long getImageNumber() {
        return imageNumber;
    }
    
    public long getTotalImages() {
        return totalImages;
    }
    
}
