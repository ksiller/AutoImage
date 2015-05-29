package autoimage.dataprocessors;

import autoimage.Utils;
import ij.IJ;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import mmcorej.TaggedImage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * GroupProcessor processes element groups in batches
 * - groups are formed by unique value combinations of all criteriaKeys
 * 
 * 
 * @author Karsten Siller
 * @param <E> element type, e.g. java.io.File or TaggedImage
 */
public abstract class GroupProcessor<E> extends BranchedProcessor<E> implements IMetaDataModifier {

    protected List<Group> groupList;//provides bins for elements
    protected List<String> criteriaKeys;//metadata tags that are used to create groups
    protected boolean processIncompleteGrps;//if true, groups will be processed at cleanup even if not complete
    protected boolean processOnTheFly;//if true, processing is launched for a group as soon as all its elements are collected 

    public final static String CRITERIA_TAG = "Criteria";

    /**
     * a Group is a container for elements that should be processed together
     * 
     * @param <E> element type, e.g. java.io.File or TaggedImage
     */
    protected class Group<E> {
        protected List<E> elements; //"images" 
        protected long status;//not processed, processing, processed, interrupted
        protected JSONObject groupCriteria;//String key refers to metadata tag, Object value refers to metadata value for this key
        protected long maxGroupSize=-1; //-1 unknown --> process all groups during clean up
        
        protected final static long IS_NOT_PROCESSED = 0;
        protected final static long IS_PROCESSING = 1;
        protected final static long IS_PROCESSED = 2;
        protected final static long IS_INTERRUPTED =3;
        
        
        protected Group(JSONObject grpCriteria) {
            status=IS_NOT_PROCESSED;
            elements=new ArrayList<E>();
            if (grpCriteria == null) {
                groupCriteria = new JSONObject();
            } else {
                groupCriteria=grpCriteria;
            }
        }
        
        //metadata key-value paris are added to group criteria
        protected void addCriterium(String key, Object value) throws JSONException {
            groupCriteria.put(key,value);
        }
        
        //compares element's metadata with group's criteria to determine if element is part of this group
        protected boolean belongsToGroup(JSONObject imgMeta) throws JSONException {
            if (imgMeta==null) { 
                //cannot parse metadata --> reject by default
                return false;
            }    
            //iterate over all criteria for this group
            Iterator it=groupCriteria.keys();
            while (it.hasNext()) {
                String key=(String)it.next();
                //compare imgMeta data with group's criteria
                if (!groupCriteria.get(key).equals(imgMeta.get(key))) {
                   //no match --> does not belong to this group
                   return false; 
                }
            }
            //all criteria are matching --> belongs to this group
            return true;
        }
        
        protected boolean addToGroup(E img) throws JSONException {
            JSONObject meta=null;
            if (img instanceof File) {
                meta=Utils.parseMetadata((File)img);
            }
            if (img instanceof TaggedImage) {
                meta=((TaggedImage)img).tags;
            }
            if (belongsToGroup(meta)) {
                elements.add(img);
                return true;
            } else {
                return false;
            }
        }
        
        //needs to be called with max!=-1 for "on-the-fly" processing; 
        private void setMaxGroupSize(long max) {
            maxGroupSize=max;
        }
        
        //used to determine if groups is complete
        private long getMaxGroupSize() {
            return maxGroupSize;
        }
        
    }    
    
    public GroupProcessor(final String pName) {
        this (pName,"",null, true);
    }
    
    public GroupProcessor(final String pName, final String path,final List<String> criteria, boolean procIncomplete) {
        super(pName, path);
        groupList=new ArrayList<Group>();
        if (criteria!=null)
            criteriaKeys=criteria;
        else 
            criteriaKeys=new ArrayList<String>();
        processOnTheFly=true;
    }

    @Override
    public void setParameters(JSONObject obj) throws JSONException {
        super.setParameters(obj);
        JSONArray criteriaArray=obj.getJSONArray(CRITERIA_TAG);
        criteriaKeys=new ArrayList<String>(criteriaArray.length());
        for (int i=0; i<criteriaArray.length(); i++) {
            criteriaKeys.add(criteriaArray.getString(i));
        } 
        processOnTheFly=obj.getBoolean("ProcessOnTheFly");
    }
    
    @Override
    public JSONObject getParameters() throws JSONException {
        JSONObject obj=super.getParameters();
        JSONArray criteriaArray=new JSONArray(criteriaKeys);
        obj.put(CRITERIA_TAG, criteriaArray);
        obj.put("ProcessOnTheFly", processOnTheFly);
        return obj;
    } 
    
    public boolean isProcessOnTheFly() {
        return processOnTheFly;
    }
    
    public void setProcessOnTheFly(boolean b) {
        processOnTheFly=b;
    }
    
    //processes only File image data by default 
    @Override
    public abstract boolean isSupportedDataType(Class<?> clazz);
    
    protected abstract List<E> processGroup(final Group<E> group) throws InterruptedException;
    
    //should return true if group is complete
    private boolean isGroupReady(Group<E> group) {
        Long max=group.getMaxGroupSize();
        return (max!=null && max != -1 && group.elements.size()>=max);
    }
    
    protected abstract long determineMaxGroupSize(JSONObject meta) throws JSONException;
    
    @Override
    protected List<E> processElement(E element) throws InterruptedException { 
        IJ.log(this.getClass().getName()+".processElement:");
        try {
            Group<E> currentGroup=null;
            int i=0;
            for (Group grp:groupList) {
                /* grp.addToGroup checks if element's metadata tags match group's criteria.
                   If yes, it adds the element to the group and returns true
                */
                if (grp.addToGroup(element)) {
                    currentGroup=grp;
                    IJ.log("    Added to group #"+i);
                    break;
                }
                i++;
            }    
            if (currentGroup==null) {//need to create new group
                currentGroup=new Group<E>(null);
                JSONObject meta=Utils.readMetadata(element,false);
                try {
                    long max=determineMaxGroupSize(meta);
                    currentGroup.setMaxGroupSize(max);
                } catch (JSONException jex) {
                    //leave as -1 (default), which by default means that group will be processed during cleanup 
                    currentGroup.setMaxGroupSize(-1);
                }    
                for (String key:criteriaKeys) {
                    currentGroup.addCriterium(key, meta.get(key));
//                    IJ.log("    Added Criterium: "+key+", "+meta.get(key).toString());
                }
                currentGroup.addToGroup(element);
                groupList.add(currentGroup);
                IJ.log("    Added to group #"+(groupList.size()-1));
                IJ.log("    "+groupList.size()+" groups");
            }
            if (!stopRequested && isGroupReady(currentGroup) && currentGroup.status==Group.IS_NOT_PROCESSED) {
                currentGroup.status=Group.IS_PROCESSING;
                List<E> results=processGroup(currentGroup);
                currentGroup.status=Group.IS_PROCESSED;
                return results;
            }
        } catch (JSONException ex) {
            IJ.log("    JSONException");
            Logger.getLogger(GroupProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new ArrayList<E>();
    }

    protected void setCriteria (List<String> criteria) {
        criteriaKeys=criteria;
    }
    
    protected void clearGroups() {
        groupList.clear();
    }
    
    @Override
    protected void cleanUp() {
        IJ.log(this.getClass().getName()+".cleanUp: "+groupList.size()+" groups");
        int i=0;
        for (Group<E> grp:groupList) {
            if (!stopRequested && processIncompleteGrps && grp.status==Group.IS_NOT_PROCESSED) {
                try {
                    IJ.log(this.getClass().getName()+".cleanUp: processing group "+i);
                    grp.status=Group.IS_PROCESSING;
                    List<E> modifiedElements=processGroup(grp);
                    grp.status=Group.IS_PROCESSED;
                    if (modifiedElements!=null) { //image processing was successful
                        //analyzers that form nodes need to place processed image in
                        //modifiedOutput_ queue
                        //for last analyzer in tree branch (='leaf') modifiedOutput_ = null,
                        //which is handled in produceModified method
                        
                        for (E element:modifiedElements)
                            produceModified(element);
                    }
                } catch (InterruptedException ex) {
                    grp.status=Group.IS_INTERRUPTED;
                    Logger.getLogger(GroupProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    IJ.log(this.getClass().getName()+": cleanUp - InterruptedException");
                }
            } 
            IJ.log(this.getClass().getName()+".cleanUp: group "+i+" - Status("+grp.status+")");
            grp.elements.clear();
            i++;
        }
        clearGroups();
    }    
}
