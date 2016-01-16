/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.api;

import autoimage.data.layout.Landmark;
import autoimage.data.layout.TilingSetting;
import autoimage.services.DoxelManager;
import autoimage.data.FieldOfView;
import autoimage.data.layout.Tile;
import autoimage.data.Vec3d;
import autoimage.gui.dialogs.RefPointListDlg;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten Siller
 */
public interface IAcqLayout {
    
    public final static String TAG_CLASS_NAME = "CLASS";
    public final static String TAG_LANDMARK = "LANDMARK";
    public final static String TAG_LAYOUT = "LAYOUT";
    public final static String TAG_LAYOUT_BOTTOM_MATERIAL = "BOTTOM_MATERIAL";
    public final static String TAG_LAYOUT_BOTTOM_THICKNESS = "BOTTOM_THICKNESS";
//    public final static String TAG_LAYOUT_COORD_X = "LAYOUT_COORD_X";
//    public final static String TAG_LAYOUT_COORD_Y = "LAYOUT_COORD_Y";
//    public final static String TAG_LAYOUT_COORD_Z = "LAYOUT_COORD_Z";
//    public static final String TAG_REF_IMAGE_FILE="REF_IMAGE_FILE";
    public final static String TAG_LAYOUT_HEIGHT = "LAYOUT_HEIGHT";
    public final static String TAG_LAYOUT_LENGTH = "LAYOUT_LENGTH";
    public final static String TAG_LAYOUT_WIDTH = "LAYOUT_WIDTH";
    public final static String TAG_NAME = "NAME";
    public final static String TAG_STAGE_X = "STAGE_X";
    public final static String TAG_STAGE_Y = "STAGE_Y";
    public final static String TAG_STAGE_Z = "STAGE_Z";
    public final static String TAG_TILE_SEED_FILE = "TILE_SEEDS";
    public final static String TAG_VERSION = "VERSION";
    
    public final static int TILING_NO_EXECUTOR = -1;
    public final static int TILING_IN_PROGRESS = 1;
    public final static int TILING_IS_TERMINATING = 2;
    public final static int TILING_COMPLETED = 3;
    public final static int TILING_ABORTED = 4;

    void initializeFromJSONObject(JSONObject obj, File file) throws JSONException, ClassNotFoundException, InstantiationException, IllegalAccessException;
    
    void addArea(BasicArea a);

    void addLandmark(Landmark lm);

    // calculates 2D affine transform and normal vector for layout
    void calcStageToLayoutTransform();

    //returns minimal relative tile overlap (0 <= tileOverlap <=1) to eliminate all gaps
    //considers camera field rotation and stage-to-layout rotation
    double closeTilingGaps(FieldOfView fov, double accuracy);

    Vec3d convertLayoutToStagePos(double layoutX, double layoutY, double layoutZ) throws Exception;

    Point2D convertLayoutToStagePos_XY(Point2D layoutXY);

    Vec3d convertStageToLayoutPos(double stageX, double stageY, double stageZ) throws Exception;

    Point2D convertStageToLayoutPos_XY(Point2D stageXY);

    int createUniqueAreaId();

    void deleteArea(int index);

    Landmark deleteLandmark(int index);

    boolean deleteLandmark(Landmark lm);

    //void deselectAllLandmarks();

    ArrayList<BasicArea> getAllAreasInsideRect(Rectangle2D r);

    ArrayList<BasicArea> getAllAreasTouching(double x, double y);

    int[] getAllContainingAreaIndices(double x, double y);

    List<BasicArea> getAreaArray();

    //areas are loaded with sequential IDs, starting with 1 --> BasicArea with id is most likey to be in areas.get(id-1). If not, the entire AreaArray is searched for the ID
    BasicArea getAreaById(int id);

    BasicArea getAreaByIndex(int index);

    BasicArea getAreaByLayoutPos(double lx, double ly);

    BasicArea getAreaByName(String name);

    Comparator getAreaNameComparator();

    String getBottomMaterial();

    double getBottomThickness();

    /*
    //faulty: does not take stageToLayoutTransform into account
    public double getStageZPosForLayoutPos(double layoutX, double layoutY) throws Exception {
    if (getNoOfMappedStagePos() == 0)
    throw new Exception("Converting z position: no Landmarks defined");
    Landmark rp=getMappedLandmarks().get(0);
    //        double z=((-normalVec.x*(layoutX-rp.getStageCoordX())-normalVec.y*(layoutY-rp.getStageCoordY()))/normalVec.z)+rp.getStageCoordZ()-rp.getLayoutCoordZ();
    double z=((-normalVec.x*(layoutX-rp.getLayoutCoordX())-normalVec.y*(layoutY-rp.getLayoutCoordY()))/normalVec.z)+rp.getStageCoordZ()-rp.getLayoutCoordZ();
    return z;
    }
     */
    double getEscapeZPos();

    File getFile();

    //returns null if layout coordinate is not inside any area
    BasicArea getFirstContainingArea(double lx, double ly);

    //parameters are absolute stage positions
    BasicArea getFirstContainingAreaAbs(double stageX, double stageY, double fovX, double fovY);

    //returns null if fov surrounding layout coordinate does not touch any area
    BasicArea getFirstTouchingArea(double lx, double ly, double fovX, double fovY);

    double getHeight();

    Landmark getLandmark(int index);

    List<Landmark> getLandmarks();

    //in radians
    double getLayoutToStageRot();

    AffineTransform getLayoutToStageTransform();

    double getLength();

    //returns list of Landmark for which stagePosMapped==true
    List<Landmark> getMappedLandmarks();

    String getName();

    int getNoOfMappedStagePos();

    int getNoOfSelectedAreas();

    int getNoOfSelectedClusters();

    Vec3d getNormalVector();

    ArrayList<BasicArea> getSelectedAreasInsideRect(Rectangle2D r);

    ArrayList<BasicArea> getSelectedAreasTouching(double x, double y);

/*    double getStagePosX(BasicArea a, int tileIndex);

    double getStagePosY(BasicArea a, int tileIndex);
*/
    //in radians
    double getStageToLayoutRot();

    AffineTransform getStageToLayoutTransform();

    //returns stageZ pos that corresponds to layout reference plane (layoutZ = 0) at stageX/stageY pos
    double getStageZPosForStageXYPos(double stageX, double stageY) throws Exception;

    Comparator getTileNumberComparator();

    //returns angle between vertical vector and normalVec of layout (in degree)
    double getTilt() throws Exception;

    long getTotalTileNumber();

    ArrayList<BasicArea> getUnselectedAreasInsideRect(Rectangle2D r);

    ArrayList<BasicArea> getUnselectedAreasTouching(double x, double y);

    double getWidth();

    /**
     * @return index of first area with duplicate name. Returns -1 if all names are unique or list is empty
     */
    int hasDuplicateAreaNames();

    //returns true if gaps exist between tiles
    //considers camera field rotation and stage-to-layout rotation
    boolean hasGaps(FieldOfView fov, double tileOverlap);

    boolean isAreaAdditionAllowed();

    boolean isAreaEditingAllowed();

    boolean isAreaMergingAllowed();

    boolean isAreaRemovalAllowed();

    boolean isEmpty();

    boolean isModified();

    List<List<Tile>> readTileCoordsFromXMLFile(String fname);

    void removeAreaById(int id);

    void saveTileCoordsToXMLFile(String fname, TilingSetting setting, double tileW, double tileH, double pixSize);

    void setAreaArray(List<BasicArea> al);

    void setBottomMaterial(String material);

    void setBottomThickness(double thickness);

    void setFile(File f);

    void setHeight(double h);

    Landmark setLandmark(int index, Landmark lm);

    void setLandmarks(List<Landmark> lm);

    void setLength(double l);

    void setModified(boolean b);

    void setName(String n);

    void setWidth(double w);

    JSONObject toJSONObject() throws JSONException;

    void writeSingleArea(XMLStreamWriter xtw, BasicArea a, TilingSetting setting) throws XMLStreamException;
    
    public List<Future<Integer>> calculateTiles(List<BasicArea> areasToTile, final DoxelManager dManager, final FieldOfView fov, final TilingSetting tSetting);
    
    public int getTilingStatus();
    
    public long getCompletedTilingTasks();
    
    public double getCompletedTilingTasksPercent();
    
    public void cancelTileCalculation();

    public void registerForEvents(Object listener);
    
}
