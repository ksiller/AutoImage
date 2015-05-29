package autoimage.dataprocessors;

import java.util.List;

/**
 * Class that specifically handles filtering using free text values representing String objects
 * 
 * @author Karsten Siller
 * @param <E> element type in BlockingQueue (e.g. TaggedImage or File)
 */
public class ImageTagFilterString<E> extends FilterProcessor<E,String> {

    public ImageTagFilterString() {
        this("",null);
    }
    
    public ImageTagFilterString(String key, List<String> values) {
        super(key,values);
    }
    
    @Override
    public String valueOf(String v) {
        return v;
    }

/*
    @Override
    public boolean equalValue(String t1, String t2) {
        return (t1.equals(t2));
    }
*/
    
}
