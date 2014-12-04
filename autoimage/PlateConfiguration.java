/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package autoimage;

/**
 *
 * @author Karsten
 */
public class PlateConfiguration {
    String name;
    String fileLocation;
    double width;
    double length;
    double height;
    int columns;
    int rows;
    double distLeftEdgeToA1;
    double distTopEdgeToA1;
    double wellDiameter;
    double wellDistance;
    double wellDepth;
    double bottomThickness;
    String bottomMaterial;
    String wellShape;
    
    public PlateConfiguration() {
        name="NewPlate";
        fileLocation="";
        width=127760; //ANSI/SLAS-1 2004; former ANSI/SBS-1 20014
        length=85480; //ANSI/SLAS-1 2004; former ANSI/SBS-1 20014
        height=14350; //ANSI/SLAS-2 2004; former ANSI/SBS-2 20014
        columns=1;
        rows=1;
        distLeftEdgeToA1=0;
        distTopEdgeToA1=0;
        wellDiameter=0;
        wellDistance=0;
        wellDepth=0;
        bottomThickness=0.17;
        bottomMaterial="Glass";
        wellShape="Circle";
    }
}
