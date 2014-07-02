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
public class ImageTagFilterOptLong<E> extends ImageTagFilterOpt<E,Long> {

    public ImageTagFilterOptLong() {
        this("",null);
    }
    
    public ImageTagFilterOptLong(String key, List<Long> values) {
        super(key,values);
    }
    
    @Override
    public Long valueOf(String v) {
        return Long.parseLong(v);
    }

    @Override
    public boolean equalValue(Long t1, Long t2) {
        return t1 == t2;
    }
    
}