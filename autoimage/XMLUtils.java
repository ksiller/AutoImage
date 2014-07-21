/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage;

import static autoimage.AcqSetting.TAG_ACQ_SETTING;
import static autoimage.AcqSetting.TAG_VERSION;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Iterator;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Karsten
 */
public class XMLUtils {
    protected static final String LINE_FEED = "\n";
    protected static int indent;
    
    public static final int FILE_IO_OK = 0;
    public static final int FILE_NOT_FOUND = 1;
    public static final int FILE_READ_ERROR = 2;
    public static final int FILE_WRITE_ERROR = 3;


    protected static void initialize() {
        indent=0;
    }
    
    protected static void wStartElement(XMLStreamWriter xtw, String tag) throws XMLStreamException {
        for (int i = 0; i < indent; i++) {
            xtw.writeCharacters("    ");
        }
        xtw.writeStartElement(tag);
        xtw.writeCharacters(LINE_FEED);
        indent++;
    }

    protected static void writeLine(XMLStreamWriter xtw, String tag, String content) throws XMLStreamException {
        for (int i = 0; i < indent; i++) {
            xtw.writeCharacters("    ");
        }
        xtw.writeStartElement(tag);
        xtw.writeCharacters(content);
        xtw.writeEndElement();
        xtw.writeCharacters(LINE_FEED);
    }

    protected static void wEndElement(XMLStreamWriter xtw) throws XMLStreamException {
        indent--;
        for (int i = 0; i < indent; i++) {
            xtw.writeCharacters("    ");
        }
        xtw.writeEndElement();
        xtw.writeCharacters(LINE_FEED);
    }
     
    public static XMLStreamWriter writeXMLHeader(String fname) throws FileNotFoundException, XMLStreamException {
        XMLUtils.initialize();
        XMLOutputFactory xof =  XMLOutputFactory.newInstance();
        XMLStreamWriter xtw; 
        xtw = xof.createXMLStreamWriter(new FileOutputStream(fname), "UTF-8"); 
        xtw.writeStartDocument("utf-8","1.0"); 
        xtw.writeCharacters(XMLUtils.LINE_FEED);
                    
        /*XMLUtils.wStartElement(xtw, TAG_ACQ_SETTING);
        XMLUtils.writeLine(xtw, TAG_VERSION, "1.0");*/
        return xtw;
    }
    
    public static void closeXMLFile(XMLStreamWriter xtw) throws XMLStreamException {
        XMLUtils.wEndElement(xtw);
        xtw.writeEndDocument(); 
        xtw.flush();
        xtw.close(); 
    }    


}
