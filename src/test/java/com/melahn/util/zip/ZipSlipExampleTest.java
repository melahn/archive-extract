package com.melahn.util.zip;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ZipSlipExampleTest { 

    private final PrintStream initialOut = System.out;
    private final ByteArrayOutputStream testOut = new ByteArrayOutputStream();
    private String stringOut;
   
    @Before
    public void setup() {
        System.setOut(new PrintStream(testOut));
    }
    
    @After
    public void teardown() {
        System.setOut(new PrintStream(initialOut));
    }

    @Test
    public void unzipTgzWithNoEmbeddedTgz() {
        Path tgzFile = Paths.get("src/test/resources/test-chart-file-with-embedded-tgz.tgz").toAbsolutePath();
        String[] args = new String[]{tgzFile.toString()};
        ZipSlipExample.main(args);
        System.setOut(initialOut);
        stringOut = new String(testOut.toByteArray(), 0, 2048);
        Path unzipDir = getUnzipDirectory(stringOut);
        System.out.println(String.format("Directory is %s", unzipDir));
        assertTrue(Files.exists(getUnzipDirectory(stringOut)));
        System.out.println(String.format("Directory %s exists as expected", unzipDir));
    }

    private Path getUnzipDirectory(String s) {
        String unzipDir = s.substring(s.indexOf("\n") + 1 + "Unzip Target Directory: ".length(), s.indexOf("\n", s.indexOf("\n") + 1));
        System.out.println(String.format("Unzip Directory = %s", unzipDir)); 
        return Paths.get(unzipDir);          
    }
}
