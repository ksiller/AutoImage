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
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten
 * @param <E> File or TaggedImage
 */
public abstract class GroupProcessor<E> extends BranchedProcessor<E>{

    protected List<Group> groupList;
    protected List<String> criteriaKeys; 
    protected boolean processIncompleteGrps;//if true, groups will be processed at cleanup even if not complete
    

    protected class Group<E> {
        protected List<E> images;
        protected boolean listComplete;
        protected boolean processed;
        protected JSONObject groupCriteria;
        
        
        protected Group(JSONObject grpCriteria) {
            processed=false;
            listComplete=false;
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
    }    
    
    public GroupProcessor(final String pName) {
        this (pName,"",null, true);
    }
    
    public GroupProcessor(final String pName, final String path,final List<String> criteria, boolean procIncomplete) {
        super(pName, path);
        groupList=new ArrayList<Group>();
        criteriaKeys=criteria;
    }

    //returns list of processed images
    protected List<E> processGroup(final Group<E> group) {
        IJ.log(this.getClass().getName()+".processGroup: ");
        IJ.log("   Criteria: "+group.groupCriteria.toString());
        IJ.log("   Images: "+group.images.size()+" images");
        return group.images;
    }
    
    @Override
    protected List<E> processElement(E element) { 
        IJ.log(this.getClass().getName()+".processElement:");
        try {
            Group<E> currentGroup=null;
            int i=0;
            for (Group grp:groupList) {
                /* checks if elements meta tag matches group's criteria
                   if yes, it adds the element to the group and returns true
                */
                if (grp.addToGroup(element)) {
                    currentGroup=grp;
                IJ.log("    Added to group #"+i);
                    break;
                }
                i++;
            }    
            if (currentGroup==null) {//new to create new group
                currentGroup=new Group<E>(null);
                JSONObject meta=readMetadata(element);
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
            if (currentGroup.listComplete) {
                currentGroup.processed=true;
                return processGroup(currentGroup);
            }
        } catch (JSONException ex) {
            IJ.log("    JSONException");
            Logger.getLogger(GroupProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    protected void setCriteria (List<String> criteria) {
        criteriaKeys=criteria;
    }
    
    protected void clearGroups() {
        groupList.clear();
    }
    
    @Override
    protected void cleanUp() {
        IJ.log(this.getClass().getName()+".cleanUp");
        for (Group<E> grp:groupList) {
            if (processIncompleteGrps && !grp.processed) {
                List<E> modifiedElements=processGroup(grp);
                if (modifiedElements!=null) { //image processing was successful
                        //analyzers that form nodes need to place processed image in 
                        //modifiedOutput_ queue
                        //for last analyzer in tree branch (='leaf') modifiedOutput_ = null,
                        //which is handled in produceModified method

                    for (E element:modifiedElements)
                        produceModified(element);
                }
            }
            grp.images.clear();
        }
        groupList.clear();
    }
}
