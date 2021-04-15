package com.melahn.util.zip;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class ZipSlipExampleTest { 

    private final PrintStream initialOut = System.out;
    private final ByteArrayOutputStream testOut = new ByteArrayOutputStream();
    private String stringOut;
   
    @Test
    public void unzipTgzWithoutEmbeddedTgz() {
        Path unzipDir = unzip(Paths.get("src/test/resources/test-chart-file-without-embedded-tgz.tgz").toAbsolutePath());
        assertTrue(Files.exists(getUnzipDirectory(stringOut)));
        System.out.println(String.format("tgz without embedded tgz files unzipped to %s", unzipDir));
    }

    @Test
    public void unzipTgzWithEmbeddedTgz() {
        Path unzipDir = unzip(Paths.get("src/test/resources/test-chart-file-with-embedded-tgz.tgz").toAbsolutePath());
        assertTrue(Files.exists(getUnzipDirectory(stringOut)));
        System.out.println(String.format("tgz with embedded tgz files unzipped to %s", unzipDir));
    }

    private Path unzip(Path tgzFile) {
        try {
            System.setOut(new PrintStream(testOut));
            String[] args = new String[] { tgzFile.toString() };
            ZipSlipExample.main(args);
            System.setOut(initialOut);
            stringOut = new String(testOut.toByteArray(), 0, 1024);
        } catch (Exception e) {

        }
        return getUnzipDirectory(stringOut);
    }

    private Path getUnzipDirectory(String s) {
        String unzipDir = s.substring(s.indexOf("\n") + 1 + "Unzip Target Directory: ".length(), s.indexOf("\n", s.indexOf("\n") + 1));
        return Paths.get(unzipDir);          
    }
}
