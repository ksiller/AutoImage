/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.olddp;

import autoimage.dataprocessors.BranchedProcessor;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Karsten
 */
public class NoFilterSeqAnalyzer<E> extends BranchedProcessor<E>{

    public NoFilterSeqAnalyzer() {
        this("");
    }
    
    public NoFilterSeqAnalyzer (String dir) {
        super ("No-Filter Branch",dir);
    }
    
    @Override
    protected boolean acceptElement(E element) {
        return true;
    }

    //need to create copy?
    @Override
    protected List<E> processElement(E element) {
/*        E modElement=null;
        if (element instanceof TaggedImage) {
            try {
                modElement=(E)Utils.duplicateTaggedImage((TaggedImage)element);
            } catch (JSONException ex) {
                Logger.getLogger(NoFilterSeqAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (element instanceof File) {
            try {
                Utils.copyFile((File)element, new File(new File(workDir,((File)modElement).getParent()),((File)modElement).getName()));
            } catch (IOException ex) {
                Logger.getLogger(NoFilterSeqAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return modElement;*/
        List<E> list = new ArrayList<E>(1);
        list.add(createModifiedOutput(element));
        return list;
    }

    
}
