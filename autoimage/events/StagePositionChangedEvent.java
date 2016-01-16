package autoimage.events;

/**
 *
 * @author Karsten Siller
 */
public class StagePositionChangedEvent {
    
    private final Double[] stagePos;
    
    public StagePositionChangedEvent(Double[] stagePos) {
        this.stagePos=stagePos;
    }
    
    public Double getStageX() {
        return (stagePos!=null && stagePos.length>0 ? stagePos[0] : null);
    }
    
    public Double getStageY() {
        return (stagePos!=null && stagePos.length>1 ? stagePos[1] : null);
    }
    
    public Double getStageZ() {
        return (stagePos!=null && stagePos.length>2 ? stagePos[2] : null);
    }
    
}
