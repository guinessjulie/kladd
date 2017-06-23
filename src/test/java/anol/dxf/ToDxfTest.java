package anol.dxf;


import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


class ToDxfTest {

    @Test
    public void writeDXFPolygonTest() {
        // Using nio.file
        Path path = Paths.get("output.dxf");
        Charset charset = Charset.forName("US-ASCII");
        try (BufferedWriter writer = Files.newBufferedWriter(path, charset)) {
            ToDxf toDxf = new ToDxf(writer);
            toDxf.prolog();
            //
            Integer iSides = 4;
            Double dblX = 0.0;
            Double dblY = 0.0;
            Double dblLen = 100.0;
            toDxf.polygon(iSides, dblX, dblY, dblLen);
            //
            iSides = 5;
            dblX = -40.0;
            dblY = 40.0;
            dblLen = 20.0;
            toDxf.polygon(iSides, dblX, dblY, dblLen);
            //
            toDxf.epilog();
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }

}