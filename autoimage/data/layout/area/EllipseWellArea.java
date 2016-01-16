package autoimage.data.layout.area;

import autoimage.api.BasicArea;
import java.util.Comparator;

/**
 *
 * @author Karsten Siller
 */
public class EllipseWellArea extends EllipseArea {
        
    public EllipseWellArea() { //expects name identifier
        super();
    }

    
    public EllipseWellArea(String n) { //expects name identifier
        super(n);
    }
    
    
    public EllipseWellArea(String n, int id, double ox, double oy, double oz, double w, double h, boolean s, String anot) {
        super(n,id,ox,oy,oz,w,h,s,anot);
    }

    @Override
    public String getShapeType() {
        return "Circular Well";
    }
    
    public static Comparator<BasicArea> NameComparator = new Comparator<BasicArea>() {

        @Override
	public int compare(BasicArea a1, BasicArea a2) {
            String a1Name = a1.getName().toUpperCase();
            String a2Name = a2.getName().toUpperCase();
            int i=0;
            while (i<a1Name.length() && !Character.isDigit(a1Name.charAt(i))) { 
                i++;
            }
            int j = i;
            while (j<a1Name.length() && Character.isDigit(a1Name.charAt(j))) {
                j++;
            }
            Integer a1Col = Integer.parseInt(a1Name.substring(i, j));         
            String a1Row = a1Name.substring(0,i);
            
            i=0;
            while (i<a2Name.length() && !Character.isDigit(a2Name.charAt(i))) { 
                i++;
            }
            j = i;
            while (j<a2Name.length() && Character.isDigit(a2Name.charAt(j))) {
                j++;
            }
            Integer a2Col = Integer.parseInt(a2Name.substring(i, j));         
            String a2Row = a2Name.substring(0,i);

            if (a1Row.equals(a2Row)) {
                return a1Col.compareTo(a2Col);
            } else {
                return a1Row.compareTo(a2Row);
            }    
        }
    };

    @Override
    public int supportedLayouts() {
        return BasicArea.SUPPORT_WELLPLATE_LAYOUT;
    }

    
    
}
