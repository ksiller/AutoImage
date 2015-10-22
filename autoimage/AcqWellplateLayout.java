package autoimage;

import autoimage.api.RefArea;
import autoimage.area.RectWellArea;
import autoimage.area.EllipseWellArea;
import autoimage.api.SampleArea;
import java.io.File;
import java.util.Comparator;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten
 */
public class AcqWellplateLayout extends AcqCustomLayout {
    
    private int columns;
    private int rows;
    private double leftEdgeToA1; // in um
    private double topEdgeToA1; // in um
    private String wellShape;
    private double wellDepth; // in um
    private double wellDiameter; // in um
    private double wellDistance; // in um
    
    public static final String TAG_LAYOUT_COLUMNS = "COLUMNS";
    public static final String TAG_LAYOUT_ROWS = "ROWS";
    public static final String TAG_LAYOUT_LEFT_EDGE_TO_A1 = "LEFT_EDGE_TO_A1";
    public static final String TAG_LAYOUT_TOP_EDGE_TO_A1 = "TOP_EDGE_TO_A1";
    public static final String TAG_LAYOUT_WELL_DEPTH = "WELL_DEPTH";
    public static final String TAG_LAYOUT_WELL_SHAPE = "WELL_SHAPE";
    public static final String TAG_LAYOUT_WELL_DIAMETER = "WELL_DIAMETER";
    public static final String TAG_LAYOUT_WELL_DISTANCE = "WELL_DISTANCE";
    
    public AcqWellplateLayout() {
        super();
    }
    
    //all dimensions are stored in um
    public AcqWellplateLayout (File f, PlateConfiguration configuration) {
        this();
        setFile(f);
        setWidth(configuration.width);
        setLength(configuration.length);
        setHeight(configuration.height);
        columns=configuration.columns;
        rows=configuration.rows;
        leftEdgeToA1=configuration.distLeftEdgeToA1;
        topEdgeToA1=configuration.distTopEdgeToA1;
        setBottomMaterial(configuration.bottomMaterial);
        setBottomThickness(configuration.bottomThickness);
        wellShape=configuration.wellShape;
        wellDepth=configuration.wellDepth;
        wellDiameter=configuration.wellDiameter;
        wellDistance=configuration.wellDistance;
        double oX=(double)configuration.distLeftEdgeToA1-configuration.wellDiameter/2;
        double oY=(double)configuration.distTopEdgeToA1-configuration.wellDiameter/2;
        initializeAreaList(columns*rows);
        int id=1;
        String wellName;
        for (int column=0; column<columns; column++) {
            for (int i=0; i<rows; i++) {
                //snake down and up through columns (from left to right):
                //even columns: down
                //odd columns: up
                int row;
                if (column%2==0)
                    row=i;
                else 
                    row=rows-i-1;
                if (row>=SampleArea.PLATE_ROW_ALPHABET.length)
                    wellName=Integer.toString(id);
                else
                    wellName=SampleArea.PLATE_ROW_ALPHABET[row]+Integer.toString(column+1);
                SampleArea a=null;
                if (wellShape.equals("Square"))
                    a = new RectWellArea(wellName, id, oX+wellDistance*column, oY+wellDistance*row,0,wellDiameter,wellDiameter,false,"");
                else if (wellShape.equals("Round"))
                    a = new EllipseWellArea(wellName, id, oX+wellDistance*column, oY+wellDistance*row,0,configuration.wellDiameter,configuration.wellDiameter,false,"");
//                areas.add(a);
                addArea(a);
                id++;
            }    
        }
                 
        addLandmark(new RefArea("Landmark 1",0,0,0,oX+wellDiameter/2,oY+wellDiameter/2,0,512,512,"landmark_1.tif")); //expects stage then layout coords
        addLandmark(new RefArea("Landmark 2",(columns-1)*wellDistance,0,0,oX+wellDiameter/2+(columns-1)*wellDistance,oY+wellDiameter/2,0,512,512,"landmark_2.tif")); //expects stage then layout coords
        addLandmark(new RefArea("Landmark 3",0,(rows-1)*wellDistance,0,oX+wellDiameter/2,oY+wellDiameter/2+(rows-1)*wellDistance,0,512,512,"landmark_3.tif")); //expects stage then layout coords
    }
    
    @Override
    public void initializeFromJSONObject(JSONObject obj,File f) throws JSONException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        super.initializeFromJSONObject(obj,f);
        columns=obj.getInt(TAG_LAYOUT_COLUMNS);
        rows=obj.getInt(TAG_LAYOUT_ROWS);
        leftEdgeToA1=obj.getDouble(TAG_LAYOUT_LEFT_EDGE_TO_A1);
        topEdgeToA1=obj.getDouble(TAG_LAYOUT_TOP_EDGE_TO_A1);
        wellShape=obj.getString(TAG_LAYOUT_WELL_SHAPE);
        wellDepth=obj.getDouble(TAG_LAYOUT_WELL_DEPTH);
        wellDistance=obj.getDouble(TAG_LAYOUT_WELL_DISTANCE);
        wellDiameter=obj.getDouble(TAG_LAYOUT_WELL_DIAMETER);
    }
    
    @Override
    public JSONObject toJSONObject () throws JSONException {
        JSONObject obj=super.toJSONObject();
        obj.put(TAG_LAYOUT_COLUMNS,columns);
        obj.put(TAG_LAYOUT_ROWS,rows);
        obj.put(TAG_LAYOUT_LEFT_EDGE_TO_A1,leftEdgeToA1);
        obj.put(TAG_LAYOUT_TOP_EDGE_TO_A1,topEdgeToA1);
        obj.put(TAG_LAYOUT_WELL_SHAPE,wellShape);
        obj.put(TAG_LAYOUT_WELL_DEPTH,wellDepth);
        obj.put(TAG_LAYOUT_WELL_DISTANCE,wellDistance);
        obj.put(TAG_LAYOUT_WELL_DIAMETER,wellDiameter);
        return obj;
    }
    
    public int getColumns() {
        return columns;
    }
    
    public int getRows() {
        return rows;
    }

    public double getLeftEdgeToA1() {
        return leftEdgeToA1;
    }
    
    public double getTopEdgeToA1() {
        return topEdgeToA1;
    }
    
    public String getWellShape() {
        return wellShape;
    }
    
    public double getWellDepth() {
        return wellDepth;
    }
    
    public double getWellDiameter() {
        return wellDiameter;
    }
    
    public double getWellDistance() {
        return wellDistance;
    }
 
    @Override
    public boolean isAreaMergingAllowed() {
        return false;
    }
    
    @Override
    public boolean isAreaAdditionAllowed() {
        return false;
    }
    
    @Override
    public boolean isAreaRemovalAllowed() {
        return false;
    }
    
    @Override
    public boolean isAreaEditingAllowed() {
        return false;
    }
    
    @Override
    public Comparator getAreaNameComparator() {
        return RectWellArea.NameComparator;
    }

    
}