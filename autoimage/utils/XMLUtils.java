/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 *
 * @author Karsten
 */
public class XMLUtils {
    public static final String LINE_FEED = "\n";
    public static int indent;
    public static final int FILE_IO_OK = 0;
    public static final int FILE_NOT_FOUND = 1;
    public static final int FILE_READ_ERROR = 2;
    public static final int FILE_WRITE_ERROR = 3;


    public static void initialize() {
        indent=0;
    }
    
    public static void wStartElement(XMLStreamWriter xtw, String tag) throws XMLStreamException {
        for (int i = 0; i < indent; i++) {
            xtw.writeCharacters("    ");
        }
        xtw.writeStartElement(tag);
        xtw.writeCharacters(LINE_FEED);
        indent++;
    }

    public static void writeLine(XMLStreamWriter xtw, String tag, String content) throws XMLStreamException {
        for (int i = 0; i < indent; i++) {
            xtw.writeCharacters("    ");
        }
        xtw.writeStartElement(tag);
        xtw.writeCharacters(content);
        xtw.writeEndElement();
        xtw.writeCharacters(LINE_FEED);
    }

    public static void wEndElement(XMLStreamWriter xtw) throws XMLStreamException {
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
