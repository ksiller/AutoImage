package autoimage.dataprocessors;

import java.util.List;

/**
 * Class that specifically handles filtering of String values from a defined List of String
 * 
 * @author Karsten Siller
 * @param <E> element type in BlockingQueue (e.g. TaggedImage or File)
 */
public class ImageTagFilterOptString<E> extends ImageTagFilterOpt<E,String> {

    public ImageTagFilterOptString() {
        this("",null);
    }
    
    public ImageTagFilterOptString(String key, List<String> values) {
        super(key,values);
    }
    
    //convert String representation of value to Long object
    @Override
    public String valueOf(String v) {
        return v;
    }
/*
    @Override
    public boolean equalValue(String t1, String t2) {
        return t1.equals(t2);
    }
*/    
}
