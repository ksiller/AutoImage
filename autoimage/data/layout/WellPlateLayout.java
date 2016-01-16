package autoimage.data.layout;

import autoimage.data.layout.area.RectWellArea;
import autoimage.data.layout.area.EllipseWellArea;
import autoimage.api.BasicArea;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten
 */
public class WellPlateLayout extends CustomLayout {
    
    private int columns;
    private int rows;
    private double leftEdgeToA1; // in um
    private double topEdgeToA1; // in um
    private String wellShape;
    private double wellDepth; // in um
    private double wellDiameter; // in um
    private double wellToWellDistance; // in um
    
    public static final String TAG_LAYOUT_COLUMNS = "COLUMNS";
    public static final String TAG_LAYOUT_ROWS = "ROWS";
    public static final String TAG_LAYOUT_LEFT_EDGE_TO_A1 = "LEFT_EDGE_TO_A1";
    public static final String TAG_LAYOUT_TOP_EDGE_TO_A1 = "TOP_EDGE_TO_A1";
    public static final String TAG_LAYOUT_WELL_DEPTH = "WELL_DEPTH";
    public static final String TAG_LAYOUT_WELL_SHAPE = "WELL_SHAPE";
    public static final String TAG_LAYOUT_WELL_DIAMETER = "WELL_DIAMETER";
    public static final String TAG_LAYOUT_WELL_DISTANCE = "WELL_DISTANCE";
    
    public static final String[] PLATE_ROW_ALPHABET={"A","B","C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "AA", "AB", "AC", "AD", "AE", "AF"};
    public static final String[] PLATE_COL_ALPHABET=ColAlphabetFactory();
    
    public WellPlateLayout() {
        super();
    }
    
    //all dimensions are stored in um
    public WellPlateLayout (File f, WellPlateConfig configuration) {
        this();
        setName(configuration.getName());
        setFile(f);
        setWidth(configuration.getWidth());
        setLength(configuration.getLength());
        setHeight(configuration.getHeight());
        columns=configuration.getColumns();
        rows=configuration.getRows();
        leftEdgeToA1=configuration.getDistLeftEdgeToA1Center();
        topEdgeToA1=configuration.getDistTopEdgeToA1Center();
        setBottomMaterial(configuration.getBottomMaterial());
        setBottomThickness(configuration.getBottomThickness());
        wellShape=configuration.getWellShape();
        wellDepth=configuration.getWellDepth();
        wellDiameter=configuration.getWellDiameter();
        wellToWellDistance=configuration.getWellToWellDistance();
        double oX=(double)configuration.getDistLeftEdgeToA1Center();//-configuration.wellDiameter/2;
        double oY=(double)configuration.getDistTopEdgeToA1Center();//-configuration.wellDiameter/2;
        initializeAreaList();
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
                if (row>=PLATE_ROW_ALPHABET.length)
                    wellName=Integer.toString(id);
                else
                    wellName=PLATE_ROW_ALPHABET[row]+Integer.toString(column+1);
                BasicArea a=null;
                if (wellShape.equals("Square"))
                    a = new RectWellArea(wellName, id, oX+wellToWellDistance*column, oY+wellToWellDistance*row,0,wellDiameter,wellDiameter,false,"");
                else if (wellShape.equals("Round"))
                    a = new EllipseWellArea(
                            wellName, 
                            id, 
                            oX+wellToWellDistance*column, 
                            oY+wellToWellDistance*row,
                            0,
                            configuration.getWellDiameter(),
                            configuration.getWellDiameter(),
                            false,
                            "");
//                areas.add(a);
                addArea(a);
                id++;
            }    
        }
        Landmark.Builder builder=new Landmark.Builder();
        addLandmark(builder
            .setName("Landmark "+PLATE_ROW_ALPHABET[0]+PLATE_COL_ALPHABET[0])
            .setStageCoord(0, 0, 0)
            .setLayoutCoord(oX, oY, 0)
            .setPhysDimension(512, 512)
            .build());    
        addLandmark(builder
            .setName("Landmark "+PLATE_ROW_ALPHABET[rows-1]+PLATE_COL_ALPHABET[0])
            .setStageCoord((columns-1)*wellToWellDistance, 0, 0)
            .setLayoutCoord(oX+(columns-1)*wellToWellDistance, oY, 0)
            .build());    
        addLandmark(builder
            .setName("Landmark "+PLATE_ROW_ALPHABET[rows-1]+PLATE_COL_ALPHABET[columns-1])
            .setStageCoord(0, (rows-1)*wellToWellDistance, 0)
            .setLayoutCoord(oX, oY+(rows-1)*wellToWellDistance, 0)
            .build());    
//        addLandmark(new Landmark("Landmark 1",0,0,0,oX,oY,0,512,512,"landmark_1.tif")); //expects stage then layout coords
//        addLandmark(new Landmark("Landmark 2",(columns-1)*wellToWellDistance,0,0,oX+(columns-1)*wellToWellDistance,oY,0,512,512,"landmark_2.tif")); //expects stage then layout coords
//        addLandmark(new Landmark("Landmark 3",0,(rows-1)*wellToWellDistance,0,oX,oY+(rows-1)*wellToWellDistance,0,512,512,"landmark_3.tif")); //expects stage then layout coords
    }
    
    private static String[] ColAlphabetFactory() {
        String[] alphabet=new String[48];
        for (int i=0; i<48; i++) {
            alphabet[i]=Integer.toString(i+1);
        }
        return alphabet;
    }       
    
    @Override
    protected void initializeAreaList() {
        setAreaArray(new ArrayList<BasicArea>(columns*rows));
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
        wellToWellDistance=obj.getDouble(TAG_LAYOUT_WELL_DISTANCE);
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
        obj.put(TAG_LAYOUT_WELL_DISTANCE,wellToWellDistance);
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
    
    public double getWellToWellDistance() {
        return wellToWellDistance;
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
