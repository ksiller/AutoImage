/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.dataprocessors;

import java.util.List;

/**
 *
 * @author Karsten
 */
public class ImageTagFilterOptString<E> extends ImageTagFilterOpt<E,String> {

    public ImageTagFilterOptString() {
        this("",null);
    }
    
    public ImageTagFilterOptString(String key, List<String> values) {
        super(key,values);
    }
    
    @Override
    public String valueOf(String v) {
        return v;
    }

    @Override
    public boolean equalValue(String t1, String t2) {
        return t1.equals(t2);
    }
    
}
