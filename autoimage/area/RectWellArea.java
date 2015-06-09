package autoimage.area;

import autoimage.area.RectArea;
import autoimage.area.Area;
import java.util.Comparator;

/**
 *
 * @author Karsten
 */
public class RectWellArea extends RectArea {
    
    public RectWellArea() { //expects name identifier
        super();
    }
    
    
    public RectWellArea(String n) { //expects name identifier
        super(n);
    }
    
    
    public RectWellArea(String n, int id, double ox, double oy, double oz, double w, double h, boolean s, String anot) {
        super(n,id,ox,oy,oz,w,h,s,anot);
    }

    @Override
    public String getShape() {
        return "Rectangular Well";
    }
    
    public static Comparator<String> NameComparator = new Comparator<String>() {

        @Override
	public int compare(String a1, String a2) {
            String a1Name = a1.toUpperCase();
            String a2Name = a2.toUpperCase();
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
        return Area.SUPPORT_WELLPLATE_LAYOUT;
    }

    
    
}
