package autoimage.dataprocessors;

import autoimage.ExtImageTags;
import autoimage.MMCoreUtils;
import autoimage.Utils;
import ij.IJ;
import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import mmcorej.TaggedImage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.api.MMTags;

/**
 *
 * @author Karsten Siller
 * @param <E> element type in BlockingQueue (e.g. TaggedImage or File)
 * @param <T> tag value type
 */

public abstract class FilterProcessor<E,T> extends BranchedProcessor<E> implements IMetaDataModifier {
    
    protected String key_; //key to retrieve property of JSONObject in TaggedImages that will be filtered
    protected List<T> values_; //accepted values for property retrieved with key_
    protected long imagesAfterFiltering; //used to adjust dimension properties of TaggedImage (# of 'CHANNELS', 'FRAMES', 'SLICES')
    protected JSONObject newSummary;
    protected Map<String,Long> sitesPerArea;
    protected Map<String,List<String>> clustersPerArea;
            
    public FilterProcessor() {
        this("Filter","",null);
    }
    
    public FilterProcessor(String key, List<T> values) {
        this("Filter",key,values);
    }
    
    public FilterProcessor(String pName, String key, List<T> values) {
        super(pName);
        this.key_=key;
        if (values!=null)
            this.values_=values;
        else 
            this.values_=new ArrayList<T>();
    }

    public boolean equalValue(T t1, T t2) {
        return t1.equals(t2);
    }
    
    //converts String representation of value v into <T> object
    public abstract T valueOf(String v);
                
    @Override
    protected void initialize() {
        super.initialize();
        newSummary=null;
        imagesAfterFiltering=0;
        sitesPerArea=new HashMap<String, Long>();
        clustersPerArea=new HashMap<String, List<String>>();
    }
    
    
    @Override
    public void setParameters(JSONObject obj) throws JSONException {
        super.setParameters(obj);
        key_=obj.getString("FilterKey");
        JSONArray vals=obj.getJSONArray("FilterValues");
        values_=new ArrayList<T>(vals.length());
        for (int i=0; i<vals.length(); i++) {
            T v;
            if (vals.get(i) instanceof Integer){ 
                v=(T)new Long(vals.getInt(i)); 
            }else {
                v=(T)vals.get(i);
            }    
            values_.add(v);
        } 
    }
    
    @Override
    public JSONObject getParameters() throws JSONException {
        JSONObject obj=super.getParameters();
        obj.put("FilterKey", key_);
        JSONArray vals=new JSONArray(values_);
        obj.put("FilterValues", vals);
        return obj;
    } 
    
    @Override
    public String getProcName() {
        return procName+" "+key_+" "+values_.toString();
    }
    
    public String getKey() {
        return key_;
    }
    
    public void setKey(String key) {
        key_=key;
    }
    
    public void setValuesFromStr(List<String> values) {
        values_.clear();
        for (String value:values)
            values_.add(valueOf(value));
    }
    
    public List<T> getValues() {
        return values_;
    }
   
    protected boolean listContainsElement(T value) {
        if (value instanceof Integer) {
            return values_.contains(new Long((Integer)value));   
        } else {
            return values_.contains(value);
        }    
    }
    
    //if parsing fails, element is rejected
    @Override
    protected boolean acceptElement(E element) {
        JSONObject tags;
        if (element instanceof TaggedImage) {
            IJ.log(this.getClass().getName()+".acceptElement - IMAGE: ");
            tags=((TaggedImage)element).tags;
        } else if (element instanceof File) {
            IJ.log(this.getClass().getName()+".acceptElement - FILE: "+((File)element).getAbsolutePath());
            try {
                tags=MMCoreUtils.parseMetadataFromFile((File)element);
            } catch (JSONException ex) {
                IJ.log("    "+this.getClass().getName()+".acceptElement: problem parsing metadata");
                Logger.getLogger(FilterProcessor.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        } else
            return false;
        if (tags!=null) {
            try {
                T entry = (T)tags.get(key_);
                if (listContainsElement(entry)) {
                    IJ.log(getProcName()+"   accepted "+entry.getClass().getSimpleName()+": "+entry);
                    return true;
                } else {
                    IJ.log(getProcName()+"   dropped "+entry.getClass().getSimpleName()+": "+entry);
                    return false;
                }    
            } catch (JSONException ex) {
                IJ.log("    "+this.getClass().getName()+".acceptElement: key '"+key_+"' does not exist.");
                Logger.getLogger(FilterProcessor.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
         } else
            return false; 
    }
   
    
    @Override
    public JSONObject updateTagValue(JSONObject meta,String newDir, String newPrefix, boolean isTaggedImage) throws JSONException {
        JSONObject summary=meta.getJSONObject(MMTags.Root.SUMMARY);
        //TaggedImage specific summary update
        if (!isTaggedImage && newDir!=null)
            summary.put(MMTags.Summary.DIRECTORY,newDir);
        if (!isTaggedImage && newPrefix!=null)
            summary.put(MMTags.Summary.PREFIX,newPrefix);
        
        if (key_.equals(MMTags.Image.CHANNEL_INDEX) 
                        || key_.equals(MMTags.Image.CHANNEL_NAME)
                        || key_.equals(MMTags.Image.CHANNEL)) {
            String thisChannel=meta.getString(MMTags.Image.CHANNEL_NAME);
            JSONArray chNames=summary.getJSONArray(MMTags.Summary.NAMES);
            JSONArray chColor=summary.getJSONArray(MMTags.Summary.COLORS);
            JSONArray chMin=summary.getJSONArray("ChContrastMin");//MMTags.Summary.CHANNEL_MINS);
            JSONArray chMax=summary.getJSONArray("ChContrastMax");//MMTags.Summary.CHANNEL_MAXES);
            JSONArray foundCh=new JSONArray();
            JSONArray foundChColor=new JSONArray();
            JSONArray foundChMin=new JSONArray();
            JSONArray foundChMax=new JSONArray();
            int newNoOfChannels=0;
            for (int i=0; i<chNames.length(); i++) {
                if (((List<String>)values_).contains((String)chNames.get(i))) {
                    foundCh.put(chNames.get(i));
                    foundChColor.put(chColor.get(i));
                    foundChMin.put(chMin.get(i));
                    foundChMax.put(chMax.get(i));
                    newNoOfChannels++;
                }
            }    
            int newChIndex=0;
            for (newChIndex=0; newChIndex<foundCh.length();newChIndex++) {
                if (thisChannel.equals(foundCh.get(newChIndex))) {
                   meta.put(MMTags.Image.CHANNEL_INDEX, newChIndex);
                   break;
                }
            }
            //update total # of channels, channel name array, channel color array, channel min contrast array, channel max contrast array
            summary.put(MMTags.Summary.CHANNELS, newNoOfChannels);//can't use values_.size() since filter may contain values not used during acqusition
            summary.put(MMTags.Summary.NAMES, foundCh);
            summary.put(MMTags.Summary.COLORS, foundChColor);
            summary.put("ChContrastMin",foundChMin);//MMTags.Summary.CHANNEL_MINS, foundChMin);
            summary.put("ChContrastMax",foundChMax);//MMTags.Summary.CHANNEL_MAXES, foundChMax);
            
        } else if (key_.equals(MMTags.Image.FRAME_INDEX) 
                        || key_.equals(MMTags.Image.FRAME)) {
            if (imagesAfterFiltering==0) {//
                long currentNoOfFrames=summary.getLong(MMTags.Summary.FRAMES);                        
                for (T value : values_) {
                    if ((Long)value < currentNoOfFrames) {
                        imagesAfterFiltering++;
                    }
                }    
            }
            Long oldIdx = meta.getLong(MMTags.Image.FRAME_INDEX);
            for (int i=0; i<values_.size(); i++) {
                if (oldIdx.equals((Long)values_.get(i))) {
                    meta.put(MMTags.Image.FRAME_INDEX,i);
                    meta.put(MMTags.Image.FRAME,i);
                    break;
                } else {
                }
            }    
            summary.put(MMTags.Summary.FRAMES, imagesAfterFiltering);
        } else if (key_.equals(MMTags.Image.SLICE_INDEX) 
                        || key_.equals(MMTags.Image.SLICE)) {
            if (imagesAfterFiltering==0) {
                long currentNoOfSlices=summary.getLong(MMTags.Summary.SLICES);
                for (T value:values_) {
                    if ((Long)value < currentNoOfSlices) {
                        imagesAfterFiltering++;
                    }
                }    
            }
            Long oldIdx = meta.getLong(MMTags.Image.SLICE_INDEX);
            for (int i=0; i<values_.size(); i++) {
                if (oldIdx.equals((Long)values_.get(i))) {
                    meta.put(MMTags.Image.SLICE,i);
                    meta.put(MMTags.Image.SLICE_INDEX,i);
                    break;
                } 
            }    
            summary.put(MMTags.Summary.SLICES, imagesAfterFiltering);
        } 
        
        else if (key_.equals(ExtImageTags.AREA_NAME)) {
            String area = meta.getString(ExtImageTags.AREA_NAME);
            JSONArray newPosArray;
            if (newSummary==null) {
                JSONArray oldPosArray=summary.getJSONArray(ExtImageTags.POSITION_ARRAY);
                newPosArray = new JSONArray();//holds new position list for new summary data
                List<String> areaClusterList=new ArrayList<String>();//holds unique area-cluster name combinations => total cluster #
                for (int i=0; i<oldPosArray.length(); i++) {
                    JSONObject posEntry=oldPosArray.getJSONObject(i);
                    String posLabel=posEntry.getString(ExtImageTags.POSITION_LABEL);
                    String cArea=posLabel.substring(0, posLabel.indexOf("-"));                    
                    String areaClusterName=posLabel.substring(0, posLabel.indexOf("-Site"));
                    if (values_.contains((T)cArea)) {
                        imagesAfterFiltering++;
                        newPosArray.put(posEntry);
                        if (!areaClusterList.contains(areaClusterName)) {
                            areaClusterList.add(areaClusterName);
                        }
                        if (sitesPerArea.containsKey(cArea)) {
                            long c=sitesPerArea.get(cArea);
                            sitesPerArea.put(cArea, c+1);
                        } else {
                            sitesPerArea.put(cArea, 1l);
                        }           
                    }  
                }            
                //update "InitialPositionList" in Summary
                summary.put(ExtImageTags.POSITION_ARRAY,newPosArray);
                //update other position related summary metadata
                summary.put(ExtImageTags.CLUSTERS, (long)areaClusterList.size()); 
                summary.put(MMTags.Summary.POSITIONS, (long)newPosArray.length());
                summary.put(ExtImageTags.AREAS, (long)sitesPerArea.size());
                //cache updated summary data so it doesn't have to be determined for subsequent accepted images
                newSummary=new JSONObject(summary.toString());
                
            } else {
                //replace summary metadata with copy of newSummary metadata
                summary=new JSONObject(newSummary.toString());
                newPosArray=summary.getJSONArray(ExtImageTags.POSITION_ARRAY);
            }

            //update POS_INDEX
            long posIndex=0;
            for (int i=0; i<newPosArray.length(); i++) {
                JSONObject posEntry=newPosArray.getJSONObject(i);
                String posLabel=posEntry.getString(ExtImageTags.POSITION_LABEL);
                if (posLabel.equals(meta.get(MMTags.Image.POS_NAME))) {
                    posIndex=i;
                    break;
                }
            }
            meta.put(MMTags.Image.POS_INDEX,posIndex); 
            meta.put(MMTags.Root.SUMMARY, summary);
            
            IJ.log("SUMMARY: Areas: "+summary.getLong(ExtImageTags.AREAS)+"; Clusters: "+summary.getLong(ExtImageTags.CLUSTERS)+"; Positions: "+summary.getLong(MMTags.Summary.POSITIONS));
            IJ.log("IMAGE: Clusters_In_Area: "+meta.get(ExtImageTags.CLUSTERS_IN_AREA)+"; Sites_In_Area: "+meta.get(ExtImageTags.SITES_IN_AREA)+"; PositioIndex: "+meta.get(MMTags.Image.POS_INDEX));
        } 
/*        
        else if (key_.equals(ExtImageTags.AREA_INDEX)) {
            long areaIndex = meta.getLong(ExtImageTags.AREA_INDEX);
//            long clusterIndex = meta.getLong(ExtImageTags.CLUSTER_INDEX);
//            long siteIndex = meta.getLong(ExtImageTags.SITE_INDEX);
            JSONArray newPosArray;
            if (newSummary==null) {
                JSONArray oldPosArray=summary.getJSONArray(ExtImageTags.POSITION_ARRAY);
                newPosArray = new JSONArray();
                long i=0;
                String lastArea="";
                long index=-1;
                List<String> areaClusterList=new ArrayList<String>();//holds unique area-cluster name combinations==total clsuter #
                while (i<oldPosArray.length()) {
                    JSONObject posEntry=oldPosArray.getJSONObject((int)i);
                    String posLabel=posEntry.getString(ExtImageTags.POSITION_LABEL);
                    String areaClusterName=posLabel.substring(0, posLabel.indexOf("-Site"));
                    String a=posLabel.substring(0, posLabel.indexOf("-"));
                    IJ.log("a: "+a+"; lastArea: "+lastArea);
                    if (!a.equals(lastArea)) {
                        lastArea=a;
                        index++;
                    }
                    if (values_.contains(index)) {
                        imagesAfterFiltering++;
                        newPosArray.put(posEntry);
                        if (!areaClusterList.contains(areaClusterName)) {
                            areaClusterList.add(areaClusterName);
                        }                        
                        IJ.log("YES");
//                        break;
                    } else {
                        IJ.log("NO");
                    }    
                    i++;
                }            
                //update "InitialPositionList" in Summary
                summary.put(ExtImageTags.POSITION_ARRAY,newPosArray);
                //update other position related summary metadata
                summary.put(ExtImageTags.CLUSTERS, areaClusterList.size()); 
                summary.put(MMTags.Summary.POSITIONS, newPosArray.length());
                summary.put(ExtImageTags.AREAS, sitesPerArea.size());
                //cache updated summary data so it doesn't have to be determined for subsequent accepted images
                newSummary=new JSONObject(summary.toString());
            } else {
                //replace summary metadata with copy of newSummary metadata
                summary=new JSONObject(newSummary.toString());
                newPosArray=summary.getJSONArray(ExtImageTags.POSITION_ARRAY);
            }

            //update POS_INDEX
            long posIndex=0;
            for (int i=0; i<newPosArray.length(); i++) {
                JSONObject posEntry=newPosArray.getJSONObject(i);
                String posLabel=posEntry.getString(ExtImageTags.POSITION_LABEL);
                if (posLabel.equals(meta.get(MMTags.Image.POS_NAME))) {
                    posIndex=i;
                    break;
                }
            }
            meta.put(MMTags.Image.POS_INDEX,posIndex);    
        }
*/        
        else if (key_.equals(ExtImageTags.CLUSTER_INDEX)) {
            JSONArray newPosArray;
            if (newSummary==null) {
                JSONArray oldPosArray=summary.getJSONArray(ExtImageTags.POSITION_ARRAY);
                newPosArray = new JSONArray();
                List<String> areaClusterList=new ArrayList<String>();//holds unique area-cluster name combinations==total clsuter #
                for (int i=0; i<oldPosArray.length(); i++) {
                    JSONObject posEntry=oldPosArray.getJSONObject((int)i);
                    String posLabel=posEntry.getString(ExtImageTags.POSITION_LABEL);
                    String area=posLabel.substring(0, posLabel.indexOf("-Cluster"));
                    String cluster=posLabel.substring(posLabel.indexOf("Cluster")+7, posLabel.indexOf("-Site"));
                    String areaClusterName=posLabel.substring(0, posLabel.indexOf("-Site"));
                    Long cIndex=Long.parseLong(cluster);
                    if (values_.contains((T)cIndex)) {
                        imagesAfterFiltering++;
                        newPosArray.put(posEntry);
                        if (!areaClusterList.contains(areaClusterName)) {
                            areaClusterList.add(areaClusterName);
                        }
                        if (sitesPerArea.containsKey(area)) {
                            long c=sitesPerArea.get(area);
                            sitesPerArea.put(area, c+1);
                        } else {
                            sitesPerArea.put(area, 1l);
                        }           
                        if (clustersPerArea.containsKey(area)) {
                            List<String> cList=clustersPerArea.get(area);
                            //add unique cluster names
                            if (!cList.contains(cluster))
                                cList.add(cluster);
                        } else {
                            //create new entry for area and add first cluster
                            List<String> cList=new ArrayList<String>();
                            cList.add(cluster);
                            clustersPerArea.put(area, cList);
                        }           
                    }    
                }            
                //update "InitialPositionList" in Summary
                summary.put(ExtImageTags.POSITION_ARRAY,newPosArray);
                //update other position related summary metadata
                summary.put(ExtImageTags.CLUSTERS, (long)areaClusterList.size()); 
                summary.put(MMTags.Summary.POSITIONS, (long)newPosArray.length());
                summary.put(ExtImageTags.AREAS, (long)sitesPerArea.size());
                //cache updated summary data so it doesn't have to be determined for subsequent accepted images
                newSummary=new JSONObject(summary.toString());
            } else {
                //replace summary metadata with copy of newSummary metadata
                summary=new JSONObject(newSummary.toString());
                newPosArray=summary.getJSONArray(ExtImageTags.POSITION_ARRAY);
            }
            
            //update CLUSTERS_IN_AREA
            String area=meta.getString(ExtImageTags.AREA_NAME);
            meta.put(ExtImageTags.CLUSTERS_IN_AREA, (long)clustersPerArea.get(area).size());

            //update SITES_IN_AREA
            meta.put(ExtImageTags.SITES_IN_AREA, sitesPerArea.get(area));

            //update POS_INDEX
            long posIndex=0;
            for (int i=0; i<newPosArray.length(); i++) {
                JSONObject posEntry=newPosArray.getJSONObject(i);
                String posLabel=posEntry.getString(ExtImageTags.POSITION_LABEL);
                if (posLabel.equals(meta.get(MMTags.Image.POS_NAME))) {
                    posIndex=i;
                    break;
                }
            }
            meta.put(MMTags.Image.POS_INDEX,posIndex);    
            meta.put(MMTags.Root.SUMMARY, summary);

            IJ.log("SUMMARY: Areas: "+summary.getLong(ExtImageTags.AREAS)+"; Clusters: "+summary.getLong(ExtImageTags.CLUSTERS)+"; Positions: "+summary.getLong(MMTags.Summary.POSITIONS));
            IJ.log("IMAGE: Clusters_In_Area: "+meta.get(ExtImageTags.CLUSTERS_IN_AREA)+"; Sites_In_Area: "+meta.get(ExtImageTags.SITES_IN_AREA)+"; PositioIndex: "+meta.get(MMTags.Image.POS_INDEX));
        }
        
        else if (key_.equals(MMTags.Image.POS_INDEX)) {
            JSONArray newPosArray;
            if (newSummary==null) {
                JSONArray oldPosArray=summary.getJSONArray(ExtImageTags.POSITION_ARRAY);
                newPosArray = new JSONArray();
                List<String> areaClusterList=new ArrayList<String>();//holds unique area-cluster name combinations==total cluster #
                //create new position list and collect unique area-cluster name conbinations (== total clusters)
                for (int i=0; i<values_.size(); i++) {
                    long fPosIndex=(Long)values_.get(i);
                    if (fPosIndex < oldPosArray.length()) {
                        JSONObject posEntry=oldPosArray.getJSONObject((int)fPosIndex);
                        String posLabel=posEntry.getString(ExtImageTags.POSITION_LABEL);
                        String areaClusterName=posLabel.substring(0, posLabel.indexOf("-Site"));
                        String area=posLabel.substring(0, posLabel.indexOf("-Cluster"));
                        String cluster=posLabel.substring(posLabel.indexOf("Cluster")+7, posLabel.indexOf("-Site"));
                        newPosArray.put(posEntry);
                        if (!areaClusterList.contains(areaClusterName)) {
                            areaClusterList.add(areaClusterName);
                        }
                        if (sitesPerArea.containsKey(area)) {
                            long c=sitesPerArea.get(area);
                            sitesPerArea.put(area, c);
                        } else {
                            sitesPerArea.put(area, 1l);
                        }           
                        if (clustersPerArea.containsKey(area)) {
                            List<String> cList=clustersPerArea.get(area);
                            //add unique cluster names
                            if (!cList.contains(cluster))
                                cList.add(cluster);
                        } else {
                            //create new entry for area and add first cluster
                            List<String> cList=new ArrayList<String>();
                            cList.add(cluster);
                            clustersPerArea.put(area, cList);
                        }           
                    }    
                }            
                //update "InitialPositionList" in Summary
                summary.put(ExtImageTags.POSITION_ARRAY,newPosArray);
                //update other position related summary metadata
                summary.put(ExtImageTags.CLUSTERS, (long)areaClusterList.size()); 
                summary.put(MMTags.Summary.POSITIONS, (long)newPosArray.length());
                summary.put(ExtImageTags.AREAS, (long)sitesPerArea.size());
                //cache updated summary data so it doesn't have to be determined for subsequent accepted images
                newSummary=new JSONObject(summary.toString());
            } else {
                //replace summary metadata with copy of newSummary metadata
                summary=new JSONObject(newSummary.toString());
                newPosArray=summary.getJSONArray(ExtImageTags.POSITION_ARRAY);
            }
            
            //update CLUSTERS_IN_AREA
            String area=meta.getString(ExtImageTags.AREA_NAME);
            meta.put(ExtImageTags.CLUSTERS_IN_AREA, (long)clustersPerArea.get(area).size());

            //update SITES_IN_AREA
            meta.put(ExtImageTags.SITES_IN_AREA, sitesPerArea.get(area));

            //update POS_INDEX
            long posIndex=0;
            for (int i=0; i<newPosArray.length(); i++) {
                JSONObject posEntry=newPosArray.getJSONObject(i);
                String posLabel=posEntry.getString(ExtImageTags.POSITION_LABEL);
                if (posLabel.equals(meta.get(MMTags.Image.POS_NAME))) {
                    posIndex=i;
                    break;
                }
            }
            meta.put(MMTags.Image.POS_INDEX,posIndex);    
            meta.put(MMTags.Root.SUMMARY, summary);

            IJ.log("SUMMARY: Areas: "+summary.getLong(ExtImageTags.AREAS)+"; Clusters: "+summary.getLong(ExtImageTags.CLUSTERS)+"; Positions: "+summary.getLong(MMTags.Summary.POSITIONS));
            IJ.log("IMAGE: Clusters_In_Area: "+meta.get(ExtImageTags.CLUSTERS_IN_AREA)+"; Sites_In_Area: "+meta.get(ExtImageTags.SITES_IN_AREA)+"; PositioIndex: "+meta.get(MMTags.Image.POS_INDEX));
        }        
       
        else if (key_.equals(ExtImageTags.SITE_INDEX)) {
            JSONArray newPosArray;
            if (newSummary==null) {
                JSONArray oldPosArray=summary.getJSONArray(ExtImageTags.POSITION_ARRAY);
                newPosArray = new JSONArray();
                List<String> areaClusterList=new ArrayList<String>();//holds unique area-cluster name combinations==total clsuter #
                for (int i=0; i<oldPosArray.length(); i++) {
                    JSONObject posEntry=oldPosArray.getJSONObject((int)i);
                    String posLabel=posEntry.getString(ExtImageTags.POSITION_LABEL);
                    String area=posLabel.substring(0, posLabel.indexOf("-Cluster"));
                    String cluster=posLabel.substring(posLabel.indexOf("Cluster")+7, posLabel.indexOf("-Site"));
                    String site=posLabel.substring(posLabel.indexOf("Site")+4);
                    IJ.log("Site: "+site);
                    String areaClusterName=posLabel.substring(0, posLabel.indexOf("-Site"));
                    Long sIndex=Long.parseLong(site);
                    if (values_.contains((T)sIndex)) {
                        imagesAfterFiltering++;
                        newPosArray.put(posEntry);
                        if (!areaClusterList.contains(areaClusterName)) {
                            areaClusterList.add(areaClusterName);
                        }
                        if (sitesPerArea.containsKey(area)) {
                            long c=sitesPerArea.get(area);
                            sitesPerArea.put(area, c+1);
                        } else {
                            sitesPerArea.put(area, 1l);
                        }           
                        if (clustersPerArea.containsKey(area)) {
                            List<String> cList=clustersPerArea.get(area);
                            //add unique cluster names
                            if (!cList.contains(cluster))
                                cList.add(cluster);
                        } else {
                            //create new entry for area and add first cluster
                            List<String> cList=new ArrayList<String>();
                            cList.add(cluster);
                            clustersPerArea.put(area, cList);
                        }           
                    }    
                }            
                //update "InitialPositionList" in Summary
                summary.put(ExtImageTags.POSITION_ARRAY,newPosArray);
                //update other position related summary metadata
                summary.put(ExtImageTags.CLUSTERS, areaClusterList.size()); 
                summary.put(MMTags.Summary.POSITIONS, newPosArray.length());
                summary.put(ExtImageTags.AREAS, sitesPerArea.size());
                //cache updated summary data so it doesn't have to be determined for subsequent accepted images
                newSummary=new JSONObject(summary.toString());
            } else {
                //replace summary metadata with copy of newSummary metadata
                summary=new JSONObject(newSummary.toString());
                newPosArray=summary.getJSONArray(ExtImageTags.POSITION_ARRAY);
            }
            
            //update CLUSTERS_IN_AREA
            String area=meta.getString(ExtImageTags.AREA_NAME);
            meta.put(ExtImageTags.CLUSTERS_IN_AREA, (long)clustersPerArea.get(area).size());

            //update SITES_IN_AREA
            meta.put(ExtImageTags.SITES_IN_AREA, sitesPerArea.get(area));

            //update POS_INDEX
            long posIndex=0;
            for (int i=0; i<newPosArray.length(); i++) {
                JSONObject posEntry=newPosArray.getJSONObject(i);
                String posLabel=posEntry.getString(ExtImageTags.POSITION_LABEL);
                if (posLabel.equals(meta.get(MMTags.Image.POS_NAME))) {
                    posIndex=i;
                    break;
                }
            }
            meta.put(MMTags.Image.POS_INDEX,posIndex);    
            meta.put(MMTags.Root.SUMMARY, summary);

            IJ.log("SUMMARY: Areas: "+summary.getLong(ExtImageTags.AREAS)+"; Clusters: "+summary.getLong(ExtImageTags.CLUSTERS)+"; Positions: "+summary.getLong(MMTags.Summary.POSITIONS));
            IJ.log("IMAGE: Clusters_In_Area: "+meta.get(ExtImageTags.CLUSTERS_IN_AREA)+"; Sites_In_Area: "+meta.get(ExtImageTags.SITES_IN_AREA)+"; PositioIndex: "+meta.get(MMTags.Image.POS_INDEX));
        }        
        return meta;
    }
    
    @Override
    protected List<E> processElement(E element) {
        // create copy, update metadata and pass accepted elements through
        List<E> list=new ArrayList<E>(1);
        list.add(createModifiedOutput(element));
        return list;
    }

    @Override
    public void makeConfigurationGUI() {
        JPanel optionPanel = new JPanel();
        BoxLayout layout = new BoxLayout(optionPanel,BoxLayout.Y_AXIS);

        optionPanel.setLayout(layout);
        JLabel l=new JLabel("Tag:");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.add(l);   
        JComboBox tagComboBox= new JComboBox(MMCoreUtils.getAvailableImageTags());
        tagComboBox.setSelectedItem(key_);
        tagComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.add(tagComboBox);
                        
        l=new JLabel("Filter Values:");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.add(l);
        JTextArea argField = new JTextArea();
        argField.setColumns(30);
        argField.setRows(10);
        argField.setLineWrap( true );
        argField.setWrapStyleWord( true );
        argField.setSize(argField.getPreferredSize().width, 1);
        argField.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (values_!=null && values_.size()>0) {
            //argField.setText(itf.getValues().toString());
            for (Object o:values_) {
                argField.append(o.toString()+"\n");
            }
        }    
        optionPanel.add(argField);
            
        int result = JOptionPane.showConfirmDialog(null, optionPanel, 
               "Filter: Image Tags", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            key_=(String)tagComboBox.getSelectedItem();
            String[] argArray=argField.getText().split("\\n");
            setValuesFromStr(Arrays.asList(argArray));
        }   
    }
    
    @Override
    protected void cleanUp() {
        super.cleanUp();
        imagesAfterFiltering=0;
        newSummary=null;
    }
    
    @Override
    public boolean isSupportedDataType(Class<?> clazz) {
        return clazz==TaggedImage.class || clazz==java.io.File.class;
    }
}
