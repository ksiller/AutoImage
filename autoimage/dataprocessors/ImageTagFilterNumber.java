/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.dataprocessors;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Karsten
 */
public class ImageTagFilterNumber<E> extends ImageTagFilter<E,Number> {

    public ImageTagFilterNumber() {
        this("",null);
    }
    
    public ImageTagFilterNumber(String key, List<Number> values) {
        super(key,values);
    }
    
    @Override
    public Number valueOf(String v) {
        return null;
    }

    @Override
    public boolean equalValue(Number t1, Number t2) {
        return ((Number)t1).doubleValue()==((Number)t2).doubleValue();
    }
    
}