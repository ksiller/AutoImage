package autoimage.dataprocessors;

import java.util.List;

/**
 * Class that specifically handles filtering of Long values from a List of Long
 * 
 * @author Karsten Siller
 * @param <E> element type in BlockingQueue (e.g. TaggedImage or File)
 */
public class ImageTagFilterOptLong<E> extends ImageTagFilterOpt<E,Long> {

    public ImageTagFilterOptLong() {
        this("",null);
    }
    
    public ImageTagFilterOptLong(String key, List<Long> values) {
        super(key,values);
    }
    
    /**
     * convert String representation of value to Long object
     * @param v String representation of filter value
     * @return Long object representation of filter value
     */    
    @Override
    public Long valueOf(String v) {
//        return Long.parseLong(v);
        return Long.valueOf(v);
    }

/*    @Override
    public boolean equalValue(Long t1, Long t2) {
        return t1.equals(t2);
    }
*/   
}