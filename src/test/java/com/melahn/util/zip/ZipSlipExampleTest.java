package com.melahn.util.zip;

import static org.junit.Assert.assertThrows;
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
    private final int UNZIP_OUT_LENGTH = 2048;

     /**
     * Test an archive without the Zip Slip Vulnerabiluty and which does not contain other archives
     */
    @Test
    public void unzipTgzWithoutEmbeddedTgz() {
        Path unzipDir = getUnzipDirectory(
                unzip(prepForUnzip("src/test/resources/test-chart-file-without-embedded-tgz-files.tgz")));
        assertTrue(Files.exists(unzipDir));
        System.out.println(String.format("tgz file without embedded tgz files unzipped to %s", unzipDir));
  }

    /**
     * Test an archive without the Zip Slip Vulnerabiluty but which contains other archives
     */
    @Test
    public void unzipTgzWithEmbeddedTgz() {
        Path unzipDir = getUnzipDirectory(
                unzip(prepForUnzip("src/test/resources/test-chart-file-with-embedded-tgz-files.tgz")));
        assertTrue(Files.exists(unzipDir));
        System.out.println(String.format("tgz file with embedded tgz files unzipped to %s", unzipDir));
    }

    /**
     * Test an archive with the Zip Slip Vulnerabiluty
     */
    @Test
    public void unzipTgzWithZipSlipVuln() {
        String[] args = prepForUnzip("src/test/resources/test-chart-file-with-zip-slip-vulnerability.tgz");
        assertThrows(ZipSlipException.class, () -> {
            ZipSlipExample.main(args);
        });
        System.setOut(initialOut);
    }

    /**
     * Unzip the archive
     * @param a the archie
     * @return a string containing the first part of the redirected output (enough
     * to parse the name of the directory to which the archive was unzipped)
     */
    private String unzip(String[] a) {
        ZipSlipExample.main(a);
        System.setOut(initialOut);
        return new String(testOut.toByteArray(), 0, UNZIP_OUT_LENGTH);
    }

    /**
     * Parses the name of the unzip directory from the input string which contains redirected 
     * output from the archive extraction
     * 
     * @param s the outout from the archive extraction
     * @return the Path of the unzip directory
     */
    private Path getUnzipDirectory(String s) {
        String unzipDir = s.substring(s.indexOf("\n") + 1 + "Unzip Target Directory: ".length(),
                s.indexOf("\n", s.indexOf("\n") + 1));
        return Paths.get(unzipDir);
    }

    /**
     * Redirect standard out to another stream so it cam be inspected to gather information
     * like the name of the directory to which the archive was unzipped and then constructs
     * the arguments to be passed to main
     * 
     * @param a The name of the archive
     * @return a atring array of one element, namely a Path to the archive
     */
    private String[] prepForUnzip(String a) {
        System.setOut(new PrintStream(testOut));
        Path tgzFile = Paths.get(a).toAbsolutePath();
        return new String[] { tgzFile.toString() };
    }
}
