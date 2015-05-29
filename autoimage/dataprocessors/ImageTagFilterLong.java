package autoimage.dataprocessors;

import java.util.List;
import mmcorej.TaggedImage;

/**
 * Class that specifically handles filtering using free text values representing Long objects 
 * 
 * @author Karsten Siller
 * @param <E> element type in BlockingQueue (e.g. TaggedImage or File)
 */
public class ImageTagFilterLong<E> extends FilterProcessor<E,Long> {

    public ImageTagFilterLong() {
        this("",null);
    }
    
    public ImageTagFilterLong(String key, List<Long> values) {
        super(key,values);
    }
    
    @Override
    public Long valueOf(String v) {
//        return Long.parseLong(v);
        return Long.valueOf(v);
    }

/*    
    @Override
    public boolean equalValue(Long t1, Long t2) {
//        return (t1 == t2);
        return t1.equals(t2);
    }
*/    

    
}
