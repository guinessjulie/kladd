package anol.pdf;

import anol.converter.ConcretePart;
import org.w3c.dom.Element;

import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static java.awt.geom.PathIterator.*;

public class ToPs {

    // The PostScript coordinate system has origo in the bottom-left corner

    private double oldX = 0.111111111;
    private double oldY = 0.111111111;
    private boolean colors;
    private Annotations annotations;

    public ToPs(boolean colors) {
        this.colors = colors;
        this.annotations = new Annotations(colors);
    }

    static double mm2pt(double mm) {
        return mm * 72.0 / 25.4;
    }

    String getCreationDate() {
        return (new Date()).toString();
    }

    public String getDocumentHeader(String title, String pageSize, String boundingBox, int numberOfPages) {
        annotations.setPageSize(pageSize);
        annotations.setNumberOfPages(numberOfPages);
        return "%!PS-Adobe-2.0\n" +
                "%%Creator: kladd\n" +
                "%%CreationDate: " + getCreationDate() + "\n" +
                "%%Title: " + title + "\n" +
                "%%Pages: " + numberOfPages + "\n" +
                "%%PageOrder: Ascend\n" +
                "%%BoundingBox: " + boundingBox + "\n" +
                "%%DocumentPaperSizes: " + pageSize + "\n" +
                "%%Orientation: Landscape\n" +
                "%%EndComments\n";
    }

    public String getDocumentTrailer() {
        return "%%EOF\n";
    }

/*
    A0 = 2384 x 3370
    A1 = 1684 x 2384
    A2 = 1191 x 1684
    A3 = 842 x 1191
    A4 = 595 x 842
*/

    public String getPageHeader(Element design, Element sheet, int pageNumber) {
        pageNumber++;
        String header = "%%Page: " + pageNumber + " " + pageNumber + "\n" +
                "%%BeginPageSetup\n" +
                "/pagelevel save def\n" +
                "90 rotate\n";
        header += "%%EndPageSetup\n";
        return header;
    }

    public String getPageTrailer() {
        return "pagelevel restore\n" + "showpage\n";
    }

    public String convertArea(ConcretePart part) {
        double origoX = part.getOrigoX();
        double origoY = part.getOrigoY();
        String outputString = "0.5 setlinewidth 1 setlinecap 0 0 0 setrgbcolor\n";
        for (PathIterator pi = part.getPathIterator(); !pi.isDone(); pi.next()) {
            double[] coords = new double[6];
            int type = pi.currentSegment(coords);
            switch (type) {
                case SEG_MOVETO: // 1 point
                    outputString += "newpath\n";
                    outputString += mm2pt(-coords[0] + origoX) + " " + mm2pt(coords[1] + origoY) + " moveto\n";
                    break;
                case SEG_LINETO: // 1 point
                    outputString += mm2pt(-coords[0] + origoX) + " " + mm2pt(coords[1] + origoY) + " lineto\n";
                    break;
                case SEG_QUADTO: // 2 point
                    outputString += mm2pt(-coords[0] + origoX) + " " + mm2pt(coords[1] + origoY) + " lineto\n";
                    outputString += mm2pt(-coords[2] + origoX) + " " + mm2pt(coords[3] + origoY) + " lineto\n";
                    break;
                case SEG_CUBICTO: // 3 points
                    outputString += mm2pt(-coords[0] + origoX) + " " + mm2pt(coords[1] + origoY) + " ";
                    outputString += mm2pt(-coords[2] + origoX) + " " + mm2pt(coords[3] + origoY) + " ";
                    outputString += mm2pt(-coords[4] + origoX) + " " + mm2pt(coords[5] + origoY) + " curveto\n";
                    break;
                case SEG_CLOSE: // 0 points
                    outputString += "closepath stroke\n";
                    break;
                default:
                    System.out.print("?");
            }
        }
        return outputString;
    }

    private static String toPsString(double d) {
        //DecimalFormat formatter = new DecimalFormat("#.##");
        //String s = "(" + formatter.format(d) + ")";
        return "(" + d + ")";
    }

    private String drawMarker(Point2D.Double point, Point2D.Double localOrigo, Point2D.Double globalOrigo) {
        double x = mm2pt(-point.getX() + globalOrigo.getX());
        double y = mm2pt(point.getY() + globalOrigo.getY());
        double newX = localOrigo.getX() - point.getX();
        double newY = -localOrigo.getY() + point.getY();
        String outputString = "";
        outputString += "newpath\n";
        outputString += (x - 8) + " " + (y) + " moveto\n";
        outputString += (x + 8) + " " + (y) + " lineto\n";
        outputString += "closepath stroke\n";
        outputString += "newpath\n";
        outputString += (x) + " " + (y - 8) + " moveto\n";
        outputString += (x) + " " + (y + 7) + " lineto\n";
        outputString += "closepath stroke\n";
        if (oldY != newY) {
            outputString += "newpath\n";
            outputString += (x + 9) + " " + (y - 8) + " moveto\n";
            outputString += toPsString(newY) + " show\n";
            outputString += "stroke\n";
            oldY = newY;
        }
        if (oldX != newX) {
            outputString += "newpath\n";
            outputString += (x - 8) + " " + (y + 8) + " moveto\n";
            outputString += toPsString(newX) + " show\n";
            outputString += "stroke\n";
            oldX = newX;
        }
        return outputString;
    }

    private String drawName(String name, Point2D.Double localOrigo, Point2D.Double globalOrigo) {
        double x = mm2pt(-localOrigo.getX() + globalOrigo.getX());
        double y = mm2pt(localOrigo.getY() + globalOrigo.getY());
        String outputString = "";
        outputString += "/Times-Roman findfont 24 scalefont setfont\n";
        outputString += "newpath\n";
        outputString += (x - 20) + " " + (y + 20) + " moveto\n";
        outputString += "(" + name + ") show\n";
        outputString += "stroke\n";
        return outputString;
    }

    public String convertPoints(ConcretePart part) {
        Rectangle2D bounds = part.getBounds();
        Point2D.Double localOrigo = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
        Point2D.Double globalOrigo = part.getOrigo();
        oldX = 0.111111111;
        oldY = 0.111111111;
        String outputString = "0.25 setlinewidth 1 setlinecap [] 0 setdash\n";
        if (colors) {
            outputString += "1 0.2 0.2 setrgbcolor\n";
        } else {
            outputString += "0 0 0 setrgbcolor\n";
        }
        outputString += drawName(part.getName(), localOrigo, globalOrigo);
        outputString += "/Times-Roman findfont 7 scalefont setfont\n";
        Iterator<Point2D.Double> iterator = part.getMajorPointIterator();
        while (iterator.hasNext()) {
            Point2D.Double point = iterator.next();
            outputString += drawMarker(point, localOrigo, globalOrigo);
        }
        return outputString;
    }

    public String printHelpLines(ConcretePart part) {
        String outputString = "0.25 setlinewidth 1 setlinecap [3] 0 setdash\n";
        if (colors) {
            outputString += "0.3 1 0.3 setrgbcolor\n";
        } else {
            outputString += "0 0 0 setrgbcolor\n";
        }

        /*
        outputString += "newpath\n";
        outputString += (x - 8) + " " + (y) + " moveto\n";
        outputString += (x + 8) + " " + (y) + " lineto\n";
        outputString += "closepath stroke\n";
        */

        outputString += "0 0 0 setrgbcolor [] 0 setdash\n";
        return outputString;
    }

    public String printSheetAnnotations(String title, String sheet1st, String sheet2nd, int pageNumber) {
        SheetAnnotations sheetAnnotations = new SheetAnnotations(this.annotations);
        List<String> lines = new ArrayList<String>();
        lines.add(title);
        lines.add(sheet1st);
        lines.add(sheet2nd);
        lines.add(SheetAnnotations.getTimeAndPage(pageNumber, annotations.getNumberOfPages()));
        return sheetAnnotations.print(lines);
    }
}
