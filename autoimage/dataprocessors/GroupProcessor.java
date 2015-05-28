/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
 *
 * @author Karsten
 * @param <E> File or TaggedImage
 */
public abstract class GroupProcessor<E> extends BranchedProcessor<E> implements IMetaDataModifier {

    protected List<Group> groupList;
    protected List<String> criteriaKeys; 
    protected boolean processIncompleteGrps;//if true, groups will be processed at cleanup even if not complete
    protected boolean processOnTheFly;

    public final static String CRITERIA_TAG = "Criteria";

    protected class Group<E> {
        protected List<E> images;
//        protected boolean listComplete;
        protected long status;
        protected JSONObject groupCriteria;
        protected long maxGroupSize=-1; //-1 unknown --> process all groups during clean up
        
        protected final static long IS_NOT_PROCESSED = 0;
        protected final static long IS_PROCESSING = 1;
        protected final static long IS_PROCESSED = 2;
        protected final static long IS_INTERRUPTED =3;
        
        
        protected Group(JSONObject grpCriteria) {
            status=IS_NOT_PROCESSED;
//            listComplete=false;
            images=new ArrayList<E>();
            if (grpCriteria == null) {
                groupCriteria = new JSONObject();
            } else {
                groupCriteria=grpCriteria;
            }
        }
        
        protected void addCriterium(String key, Object value) throws JSONException {
            groupCriteria.put(key,value);
        }
        
        protected boolean belongsToGroup(JSONObject imgMeta) throws JSONException {
//            IJ.log("   belongsToGroup....");
            Iterator it=groupCriteria.keys();
            while (it.hasNext()) {
                String key=(String)it.next();
                if (!groupCriteria.get(key).equals(imgMeta.get(key))) {
                   return false; 
                }
            }
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
                images.add(img);
                return true;
            } else {
                return false;
            }
        }
        
        private void setMaxGroupSize(long max) {
            maxGroupSize=max;
        }
        
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
    public boolean isSupportedDataType(Class<?> clazz) {
        if (clazz==java.io.File.class)
            return true;
        else
            return false;
    }
    
    protected abstract List<E> processGroup(final Group<E> group) throws InterruptedException;
    
    private boolean groupIsComplete(Group<E> group) {
        Long max=group.getMaxGroupSize();
        return (max!=null && max != -1 && group.images.size()>=max);
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
                    IJ.log("    Setting maxGroupSize="+max);
                } catch (JSONException jex) {
                    //leave as -1 (default), which by default means that group will be processed during cleanup 
                    currentGroup.setMaxGroupSize(-1);
                    IJ.log("    Setting maxGroupSize=-1");
                }    
//                IJ.log("    Creating new group");
                for (String key:criteriaKeys) {
                    currentGroup.addCriterium(key, meta.get(key));
//                    IJ.log("    Added Criterium: "+key+", "+meta.get(key).toString());
                }
                currentGroup.addToGroup(element);
                groupList.add(currentGroup);
                IJ.log("    Added to group #"+(groupList.size()-1));
                IJ.log("    "+groupList.size()+" groups");
            }
            if (!stopRequested && groupIsComplete(currentGroup) && currentGroup.status==Group.IS_NOT_PROCESSED) {
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
            grp.images.clear();
            i++;
        }
        clearGroups();
    }    
}
