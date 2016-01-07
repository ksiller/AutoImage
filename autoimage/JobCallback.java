package autoimage;

/**
 *
 * @author Karsten Siller
 */
public abstract class JobCallback<T> implements Runnable {
    
    private T parameter;
    
    public JobCallback(T param) {
        parameter=param;
    }
    
    public void setParameter(T param) {
        parameter=param;
    }
    
    public abstract void execute(T param) throws RuntimeException;
    
    @Override
    public final void run() {
        execute(parameter);
    }
    
}
