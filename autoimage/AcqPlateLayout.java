/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage;

import java.io.File;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten
 */
public class AcqPlateLayout extends AcqLayout {
    
    private int columns;
    private int rows;
    private double leftEdgeToA1;
    private double topEdgeToA1;
    private String wellShape;
    private double wellDepth;
    private double wellDiameter;
    private double wellDistance;
    
    public static final String TAG_LAYOUT_COLUMNS = "COLUMNS";
    public static final String TAG_LAYOUT_ROWS = "ROWS";
    public static final String TAG_LAYOUT_LEFT_EDGE_TO_A1 = "LEFT_EDGE_TO_A1";
    public static final String TAG_LAYOUT_TOP_EDGE_TO_A1 = "TOP_EDGE_TO_A1";
    public static final String TAG_LAYOUT_WELL_DEPTH = "WELL_DEPTH";
    public static final String TAG_LAYOUT_WELL_SHAPE = "WELL_SHAPE";
    public static final String TAG_LAYOUT_WELL_DIAMETER = "WELL_DIAMETER";
    public static final String TAG_LAYOUT_WELL_DISTANCE = "WELL_DISTANCE";
    
    public AcqPlateLayout() {
        super();
    }
    
    public AcqPlateLayout (File f, PlateConfiguration configuration) {
        this();
        file=f;
        width=configuration.width; //physical dim in um
        length=configuration.length; //physical dim in um
        height=configuration.height; //physical dim in um
        columns=configuration.columns;
        rows=configuration.rows;
        leftEdgeToA1=configuration.distLeftEdgeToA1;
        topEdgeToA1=configuration.distTopEdgeToA1;
        bottomMaterial=configuration.bottomMaterial;
        bottomThickness=configuration.bottomThickness;
        wellShape=configuration.wellShape;
        wellDepth=configuration.wellDepth;
        wellDiameter=configuration.wellDiameter;
        wellDistance=configuration.wellDistance;
        double oX=(double)configuration.distLeftEdgeToA1-configuration.wellDiameter/2;
        double oY=(double)configuration.distTopEdgeToA1-configuration.wellDiameter/2;
        int areaNum=columns*rows;
        areas = new ArrayList<Area>(areaNum);
        int id=1;
        String wellName;
        for (int row=0; row<rows; row++) {
            for (int column=0; column<columns; column++) {
                if (row>=Area.PLATE_ALPHABET.length)
                    wellName=Integer.toString(id);
                else
                    wellName=Area.PLATE_ALPHABET[row]+Integer.toString(column+1);
                Area a=null;
                if (wellShape.equals("Square"))
                    a = new RectArea(wellName, id, oX+wellDistance*column, oY+wellDistance*row,0,wellDiameter,wellDiameter,false,"");
                else if (wellShape.equals("Round"))
                    a = new EllipseArea(wellName, id, oX+wellDistance*column, oY+wellDistance*row,0,configuration.wellDiameter,configuration.wellDiameter,false,"");
                areas.add(a);
                id++;
            }    
        }
                 
        addLandmark(new RefArea("Landmark 1",0,0,0,oX+wellDiameter/2,oY+wellDiameter/2,0,512,512,"landmark_1.tif")); //expects stage then layout coords
        addLandmark(new RefArea("Landmark 2",(columns-1)*wellDistance,0,0,oX+wellDiameter/2+(columns-1)*wellDistance,oY+wellDiameter/2,0,512,512,"landmark_2.tif")); //expects stage then layout coords
        addLandmark(new RefArea("Landmark 3",0,(rows-1)*wellDistance,0,oX+wellDiameter/2,oY+wellDiameter/2+(rows-1)*wellDistance,0,512,512,"landmark_3.tif")); //expects stage then layout coords
    }
    /*
    public AcqPlateLayout (File f, int cols, int rws, double w, double l, double h, double a1ColumnOffset, double a1RowOffset, double wellDiam, double wellSpacingX, double wellSpacingY, String shape, double wellDpth, double thickness, String material){
        this();
        width=w; //physical dim in um
        length=l; //physical dim in um
        height=h; //physical dim in um
        bottomMaterial=material;
        bottomThickness=thickness;
        wellShape=shape;
        wellDepth=wellDpth;
        columns=cols;
        rows=rws;
        double oX=(double)a1ColumnOffset-wellDiam/2;
        double oY=(double)a1RowOffset-wellDiam/2;
        int areaNum=columns*rows;
        areas = new ArrayList<Area>(areaNum);
        int id=1;
        String wellName;
        for (int row=0; row<rows; row++) {
            for (int column=0; column<columns; column++) {
                if (row>=Area.PLATE_ALPHABET.length)
                    wellName=Integer.toString(id);
                else
                    wellName=Area.PLATE_ALPHABET[row]+Integer.toString(column+1);
                Area a=null;
                if (wellShape.equals("Square"))
                    a = new RectArea(wellName, id, oX+wellSpacingX*column, oY+wellSpacingY*row,0,wellDiam,wellDiam,false,"");
                else if (wellShape.equals("Round"))
                    a = new EllipseArea(wellName, id, oX+wellSpacingX*column, oY+wellSpacingY*row,0,wellDiam,wellDiam,false,"");
                areas.add(a);
                id++;
            }    
        }
                 
        addLandmark(new RefArea("Landmark 1",0,0,0,oX+wellDiam/2,oY+wellDiam/2,0,512,512,"landmark_1.tif")); //expects stage then layout coords
        addLandmark(new RefArea("Landmark 2",(columns-1)*wellSpacingX,0,0,oX+wellDiam/2+(columns-1)*wellSpacingX,oY+wellDiam/2,0,512,512,"landmark_2.tif")); //expects stage then layout coords
        addLandmark(new RefArea("Landmark 3",0,(rows-1)*wellSpacingY,0,oX+wellDiam/2,oY+wellDiam/2+(rows-1)*wellSpacingY,0,512,512,"landmark_3.tif")); //expects stage then layout coords
    }
*/
    @Override
    protected void initializeFromJSONObject(JSONObject obj) throws JSONException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        super.initializeFromJSONObject(obj);
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
    
}
