package autoimage.dataprocessors;

import autoimage.ImgUtils;
import autoimage.api.ExtImageTags;
import autoimage.MMCoreUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.process.ImageProcessor;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import mmcorej.TaggedImage;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.MMTags;
import org.micromanager.utils.MMScriptException;

/**
 * This class is used to simulate image acquisition in demo mode.
 * Images are loaded on the fly from an existing AutoImage acquisition data set
 * and the loaded pixel arrays replace the acquired image pixel array.
 * 
 * This is useful to demonstrate acquisition in offline mode, or to test 
 * image processing modules offline on existing data sets.
 * 
 * This processor processes TaggedImages and should be added immediately 
 * downstream of "Acquisition Engine" and somewhere upstream of the "Image Storage" 
 * node in the image processing tree
 * 
 * Note:
 * - Pixel array dimension (image size) and pixel type have to be matching.
 * - offline data set needs to contain images for the corresponding 
 *   areas, clusters, sites, channels, timepoints (frames) and focal planes (slices)  
 * 
 * 
 * @author Karsten Siller
 * 
 */
public class ImageLoader extends ExtDataProcessor<TaggedImage> {
    
    String imagePath = "";//path to saved acquisition data set
    
    
    @Override
    protected void process() {
        try {
            TaggedImage element = poll();
//            produce(element);
            if (TaggedImageQueue.isPoison(element)) {
                produce(element);
                IJ.log(getClass().getSimpleName()+" "+procName+" : Poison");
                done=true;
            } else {   
                JSONObject meta = element.tags;
                try {
                    JSONObject summary=meta.getJSONObject(MMTags.Root.SUMMARY);
                    String ch=meta.getString(MMTags.Image.CHANNEL_NAME);
                    int z=meta.getInt(MMTags.Image.SLICE_INDEX);
                    int t=meta.getInt(MMTags.Image.FRAME_INDEX);

                    String area = meta.getString(ExtImageTags.AREA_NAME);
                    String cluster=Integer.toString(meta.getInt(ExtImageTags.CLUSTER_INDEX));
                    String site= String.format("%06d", meta.getInt(ExtImageTags.SITE_INDEX));
                    File path;
                    if (!cluster.equals("-1"))
                        path=new File(new File(imagePath,area+"-Cluster"+cluster+"-Site"+site),"img_"+String.format("%09d",t)+"_"+ch+"_"+String.format("%03d",z)+".tif");
                    else
                        path=new File(new File(imagePath,area+"-Site"+site),"img_"+String.format("%09d",t)+"_"+ch+"_"+String.format("%03d",z)+".tif");
                    IJ.log(this.getClass().getName()+": "+(path.exists() ? "found" : "not found ")+ path.getAbsolutePath());
                    ImagePlus imp=IJ.openImage(path.getAbsolutePath());
//                    element=org.micromanager.utils.ImageUtils.makeTaggedImage(imp.getProcessor);
//                    element.tags=new JSONObject(element.tags.toString());
                    element=ImgUtils.createTaggedImage(imp, new JSONObject(element.tags.toString()));
                } catch (JSONException ex) {
                    IJ.log(this.getClass().getName()+": "+ex.getMessage());
                    Logger.getLogger(ImageLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
                produce(element);
            }
        } catch (NullPointerException e) {
            IJ.log(getClass().getSimpleName()+" "+procName+" : "+e);
        }    
    }
    
    @Override
    public JSONObject getParameters() throws JSONException {
        JSONObject obj=super.getParameters();
        obj.put("ImagePath", imagePath);
        return obj;
    }
    
    @Override
    public void setParameters(JSONObject obj) throws JSONException {
        super.setParameters(obj);
        imagePath=obj.getString("ImagePath");
    }
    
    @Override
    public void makeConfigurationGUI() {

        JPanel optionPanel = new JPanel();
        BoxLayout layout = new BoxLayout(optionPanel,BoxLayout.Y_AXIS);

        optionPanel.setLayout(layout);
        JLabel l=new JLabel("Image Directory:");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.add(l);
        JPanel filePanel = new JPanel();
        BoxLayout flayout = new BoxLayout(filePanel,BoxLayout.X_AXIS);
        final JFormattedTextField pathField = new JFormattedTextField();
        pathField.setColumns(30);
        pathField.setValue(imagePath);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        filePanel.add(pathField);
        JButton fileButton=new JButton("Browse");
        filePanel.add(fileButton);
        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                JFileChooser fc=new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (new File(imagePath).exists())
                    fc.setCurrentDirectory(new File(imagePath));
                else {
                    fc.setCurrentDirectory(new File(Prefs.getImageJDir()));
                }    
                int result=fc.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    if (fc.getSelectedFile().exists()) {
                        imagePath = fc.getSelectedFile().getAbsolutePath();
                    } else {
                        imagePath = fc.getCurrentDirectory().getAbsolutePath();
                    }                    
                    pathField.setValue(imagePath);
                }    
            }
        });
        filePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.add(filePanel);
        
        int result=-100;
        File scriptfile=null;
        do {    
            if (result!=-100) {
                JOptionPane.showMessageDialog(null,"Directory "+imagePath+"not found.");
            }
            result = JOptionPane.showConfirmDialog(null, optionPanel, 
                this.getClass().getName(), JOptionPane.OK_CANCEL_OPTION);
            scriptfile=new File(pathField.getText());
        } while (result == JOptionPane.OK_OPTION && !scriptfile.exists());
        if (result == JOptionPane.OK_OPTION) {
            imagePath=pathField.getText();
        }
    }

    @Override
    public boolean isSupportedDataType(Class<?> clazz) {
        return clazz==TaggedImage.class;
    }
    
}
