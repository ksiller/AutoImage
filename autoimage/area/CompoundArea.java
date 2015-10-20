package autoimage.area;

import autoimage.Tile;
import autoimage.api.SampleArea;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten Siller
 */
public class CompoundArea extends SampleArea {
    
    public static final String TAG_AREAS_TO_ADD = "AREAS_TO_ADD";
    public static final String TAG_AREAS_TO_SUBTRACT = "AREAS_TO_SUBTRACT";

    private List<SampleArea> areasToAdd;
    private List<SampleArea> areasToSubtract;
    
    
    public CompoundArea() {
        super();
        areasToAdd = new ArrayList<SampleArea> ();
        areasToSubtract = new ArrayList<SampleArea> ();
        createShape();
        setRelDefaultPos();
        createGeneralPath();        
    }
    
    public CompoundArea(String n) { //expects name identifier
        super(n);
        areasToAdd = new ArrayList<SampleArea> ();
        areasToSubtract = new ArrayList<SampleArea> ();
        createShape();
        setRelDefaultPos();
        createGeneralPath();
    }
    
    public CompoundArea(String n, int id, double ox, double oy, double oz, List<SampleArea> a, List<SampleArea> s, boolean selForAcq, String anot) {
        super(n,id,ox,oy,oz,selForAcq,anot);
        areasToAdd = a;
        areasToSubtract = s;
        createShape();
        setRelDefaultPos();
        createGeneralPath();
    }

    @Override
    protected void initializeFromJSONObject(JSONObject obj) throws JSONException {
        areasToAdd = new ArrayList<SampleArea> ();
        JSONArray areaArray=obj.getJSONArray(TAG_AREAS_TO_ADD);
        for (int i=0; i<areaArray.length(); i++) {
            try {
                areasToAdd.add(SampleArea.createFromJSONObject(areaArray.getJSONObject(i)));
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(CompoundArea.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(CompoundArea.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(CompoundArea.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        areasToSubtract = new ArrayList<SampleArea> ();
        areaArray=obj.getJSONArray(TAG_AREAS_TO_SUBTRACT);
        for (int i=0; i<areaArray.length(); i++) {
            try {
                areasToSubtract.add(SampleArea.createFromJSONObject(areaArray.getJSONObject(i)));
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(CompoundArea.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(CompoundArea.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(CompoundArea.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    protected void addFieldsToJSONObject(JSONObject obj) throws JSONException {
        JSONArray areaArray=new JSONArray();
        for (SampleArea area:areasToAdd) {
            areaArray.put(area.toJSONObject());
        }
        obj.put(TAG_AREAS_TO_ADD, areaArray);
        
        areaArray=new JSONArray();
        for (SampleArea area:areasToSubtract) {
            areaArray.put(area.toJSONObject());
        }
        obj.put(TAG_AREAS_TO_SUBTRACT, areaArray);
    }    


    @Override
    public String getShapeType() {
        return "Compound";
    }

    @Override
    public SampleArea duplicate() {
        CompoundArea newArea = new CompoundArea(this.getName());
        newArea.setId(this.getId());
        newArea.centerXYPos=new Point2D.Double(this.getCenterXYPos().getX(), this.getCenterXYPos().getY());
        newArea.setRelativeZPos(this.relativeZPos);
        newArea.affineTrans=new AffineTransform(this.affineTrans);
        newArea.setSelectedForAcq(isSelectedForAcq());
        newArea.setSelectedForMerge(isSelectedForMerge());
        newArea.setComment(this.comment);
        newArea.setAcquiring(this.acquiring);
        newArea.tilePosList=new ArrayList<Tile>(this.getTilePositions());
        newArea.setUnknownTileNum(this.hasUnknownTileNum());
        newArea.areasToAdd = new ArrayList<SampleArea>();
        for (SampleArea area:this.areasToAdd) {
            newArea.areasToAdd.add(area.duplicate());
        }
        newArea.areasToSubtract = new ArrayList<SampleArea>();
        for (SampleArea area:this.areasToSubtract) {
            newArea.areasToSubtract.add(area.duplicate());
        }
        newArea.createShape();
        newArea.setRelDefaultPos();
        newArea.createGeneralPath();
        return newArea;    
    }

    public void addAreaToAdd(SampleArea area) {
        if (areasToAdd==null) {
            areasToAdd = new ArrayList<SampleArea>();
        }
        areasToAdd.add(area);
    }
    
    public boolean removeAreaToAdd(SampleArea area) {
        if (areasToAdd==null) {
            return false;
        }
        return areasToAdd.remove(area);
    }
    
    public void addAreaToSubtract(SampleArea area) {
        if (areasToSubtract==null) {
            areasToSubtract = new ArrayList<SampleArea>();
        }
        areasToSubtract.add(area);
    }
    
    public boolean removeAreaToSubtract(SampleArea area) {
        if (areasToSubtract==null) {
            return false;
        }
        return areasToSubtract.remove(area);
    }
    
    @Override
    public SampleArea showConfigDialog(Rectangle2D layoutBounds) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int supportedLayouts() {
        return SampleArea.SUPPORT_CUSTOM_LAYOUT;
    }

    @Override
    protected void createShape() {
        if (areasToAdd==null || areasToAdd.isEmpty()) {
            shape=new Path2D.Double();
            return;
        }
        Path2D combinedAdd=new Path2D.Double();
        for (SampleArea a:areasToAdd) {
            if (a!=null) {
                combinedAdd.append(a, false);
            }
        }
        java.awt.geom.Area add = new java.awt.geom.Area(combinedAdd);
        
        if (areasToSubtract!=null && !areasToSubtract.isEmpty()) {
            Path2D combinedSubtract = new Path2D.Double();
            for (SampleArea a:areasToSubtract) {
                if (a!=null) {
                    combinedSubtract.append(a, false);
                }
            }
            java.awt.geom.Area sub = new java.awt.geom.Area(combinedSubtract);
            add.subtract(sub);
        }
        shape=new Path2D.Double(add);
    }

    @Override
    protected void setRelDefaultPos() {
        relDefaultXYPos=new Point2D.Double(0,0);
    }
        
}
