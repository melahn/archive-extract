package com.melahn.util.extract;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockedStatic.Verification;

class ArchiveExtractTest {

    private final PrintStream initialOut = System.out;
    private final ByteArrayOutputStream testOut = new ByteArrayOutputStream();
    private final static int EXTRACT_OUT_LENGTH = 2048;
    private final static String DEFAULT_TEST_FILENAME = "test.tgz";
    private final static String DIVIDER = "---------------------------";
    private final static String ARCHIVE_FILE_CONTAINING_HIDDEN_FILES = "src/test/resources/test-with-hidden-files";
    private final static String ARCHIVE_FILE_DEPTH_SIX = "src/test/resources/test-with-depth-six";

    @BeforeAll
    static void init() {
        /**
         * copy the test.tgz file to the project root to prep for the no parms case
         */
        System.out.println(DIVIDER.concat(" TESTS START ").concat(DIVIDER));
        try {
            Files.copy(Paths.get("src/test/resources/test.tgz"), Paths.get(DEFAULT_TEST_FILENAME),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.println("Could not initialize test cases: ".concat(e.getMessage()));
        }
    }

    @AfterAll
    static void cleanup() {
        /**
         * if the test.tgz file exists in the project directory, then remove it
         */
        try {
            Path testFile = Paths.get(DEFAULT_TEST_FILENAME);
            if (Files.exists(testFile)) {
                Files.delete(testFile);
            }
        } catch (IOException e) {
            System.out.println("Could not cleanup after test cases: ".concat(e.getMessage()));
        }
        System.out.println(DIVIDER.concat(" TESTS END ").concat(DIVIDER));
    }

    /**
     * Test an archive with the Zip Slip Vulnerabiluty
     */
    @Test
    void extractArchiveWithZipSlipVuln() {
        String[] args = prepForExtract("src/test/resources/test-chart-file-with-zip-slip-vulnerability.tgz");
        assertThrows(ArchiveExtractException.class, () -> {
            ArchiveExtract.main(args);
        });
        System.setOut(initialOut);
        System.out.println(String
                .format("SUCCESS: The archive %s was found to have the Zip Slip vulnerability as expected", args[0]));
    }

    /**
     * Test an archive that does not exist
     */
    @Test
    void extractArchiveNoExist() {
        String[] args = prepForExtract("notexist.tgz");
        assertThrows(NoSuchFileException.class, () -> {
            ArchiveExtract.main(args);
        });
        System.setOut(initialOut);
        System.out.println(String.format(String.format(
                "SUCCESS: A NoSuchFileException was thrown as expected for the non-existent file %s", args[0])));
    }

    /**
     * Test archives that don't have the Zip Slip Vulnerability
     * 
     * @throws IOException
     */
    @ParameterizedTest
    @ValueSource(strings = { "src/test/resources/test-chart-file-without-embedded-tgz-files.tgz",
            "src/test/resources/test-chart-file-with-embedded-tgz-files.tgz", "" })
    void extractVariant(String archiveFilename) throws IOException {
        Path extractDir = extractToPath(archiveFilename);
        System.out.println(String.format("SUCCESS: Parameterized test with %s. Archive was extracted to %s",
                archiveFilename.isEmpty() ? "src/test/resources/".concat(ArchiveExtract.DEFAULT_ARCHIVE_FILENAME)
                        : archiveFilename,
                extractDir));
    }

    /**
     * Test archive that has hidden files
     * 
     * @throws IOException
     */
    @Test
    void extractWithHiddenFiles() throws IOException {
        Path extractDir = extractToPath(ARCHIVE_FILE_CONTAINING_HIDDEN_FILES.concat(".tgz"));
        assertFalse(Files.exists(extractDir.resolve(ARCHIVE_FILE_CONTAINING_HIDDEN_FILES.concat("/.DS_Store")))
                || Files.exists(extractDir.resolve(ARCHIVE_FILE_CONTAINING_HIDDEN_FILES.concat("/A/.DS_Store"))));
        System.setOut(initialOut);
        System.out.println(String.format(String.format(
                "SUCCESS: The archive %s contains hidden files. When it was extracted to %s, the hidden files were not extracted.",
                ARCHIVE_FILE_CONTAINING_HIDDEN_FILES.concat(".tgz"), extractDir)));
    }

    /**
     * Test archive that has a depth greater than five (the max depth to prevent
     * extracting a runaway tgz file especially a quine tgz)
     * 
     * @throws IOException
     */
    @Test
    void extractWithDepthSix() throws IOException {
        Path extractDir = extractToPath(ARCHIVE_FILE_DEPTH_SIX.concat(".tgz"));
        assertFalse(Files.exists(extractDir.resolve(ARCHIVE_FILE_DEPTH_SIX.concat("/A/B/C/D/E/F/foo"))));
        System.setOut(initialOut);
        System.out.println(String.format(
                String.format("SUCCESS: The archive %s had a depth greater than five, so extraction to %s was halted.",
                        ARCHIVE_FILE_DEPTH_SIX.concat(".tgz"), extractDir)));
    }

    /**
     * Test method to detect hidden file pattern with null input
     */
    @Test
    void testIsHiddenNull() {
        assertFalse(new ArchiveExtract().isHidden(null));
        System.out.println("SUCCESS: The null filename was not detected as hidden.");
    }

    /**
     * Test method to detect file patterns that are not hidden
     */
    @ParameterizedTest
    @ValueSource(strings = { "", ".", "..", "./a/b/c/d", "./a/./c/d", "./a/.b/c/d", "a/b/c/d", "a/b/c/d/" })
    void testIsHiddenFalse(String h) {
        assertFalse(new ArchiveExtract().isHidden(h));
        System.out.println(String.format("SUCCESS: Parameterized test with filename %s was not detected as hidden", h));
    }

    /**
     * Test method to detect file patterns that are hidden
     */
    @ParameterizedTest
    @ValueSource(strings = { ".a", "/a/b/c/.d", "/a/b/c/.d/" })
    void testIsHiddenTrue(String h) {
        assertTrue(new ArchiveExtract().isHidden(h));
        System.out.println(String.format("SUCCESS: Parameterized test with filename %s was detected as hidden", h));
    }

    /**
     * Test method to detect archive files
     */
    @ParameterizedTest
    @ValueSource(strings = { "a.tgz", "a.TGZ", "a.tar.gz", "a.tar.GZ" })
    void testIsArchiveTrue(String a) {
        assertTrue(new ArchiveExtract().isArchive(a));
        System.out.println(String.format("SUCCESS: The file named %s was correctly detected as an archive.", a));
    }

    /**
     * Test method to detect files that are not archives
     */
    @ParameterizedTest
    @ValueSource(strings = { "", "a.tg", "a.tar, A.ZIP" })
    void testIsArchiveFalse(String a) {
        assertFalse(new ArchiveExtract().isArchive(a));
        System.out.println(String.format("SUCCESS: The file named %s was correctly detected as not an archive.", a));
    }

    /**
     * Test method for handling a null filename in archive detection
     */
    @Test
    void testIsArchiveNull() {
        assertFalse(new ArchiveExtract().isArchive(null));
        System.out.println(String.format("SUCCESS: Test that a null filename was not detected as an archive"));
    }

    /**
     * Test IO Exception creating the target directory
     *
     * @throws IOException
     */
    @Test
    void testIOExceptionCreatingExtractDirectory() throws IOException {
        FileAttribute<Set<PosixFilePermission>> a = PosixFilePermissions
                .asFileAttribute(PosixFilePermissions.fromString("rwxr-----"));
        ArchiveExtract zse = new ArchiveExtract();
        String d = zse.getClass().getCanonicalName() + "." + "Temporary.";
        try (MockedStatic<Files> fMock = Mockito.mockStatic(Files.class)) {
            fMock.when((Verification) Files.createTempDirectory(d, a)).thenThrow(IOException.class);
            assertThrows(IOException.class, () -> zse.createExtractDir());
        } finally {
            Files.deleteIfExists(Paths.get(d));
        }
        System.out.println("SUCCESS: Handling of an IO Exception when creating the extract directory was tested.");
    }

    /**
     * Test the case where the archive entry contains a directory that does not
     * exist
     *
     * @throws IOException
     */
    @Test
    void testArchiveEntryDirectoryNotExist() throws IOException {
        TarArchiveEntry entry = null;
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions
                .asFileAttribute(PosixFilePermissions.fromString("rwxr-----"));
        Path targetDirectory = Files.createTempDirectory("testArchiveEntryDirectoryNotExist", attr);
        try (InputStream is = Files.newInputStream(Paths.get("src/test/resources/test.tgz"));
                BufferedInputStream bis = new BufferedInputStream(is);
                GzipCompressorInputStream gis = new GzipCompressorInputStream(bis);
                TarArchiveInputStream tis = new TarArchiveInputStream(gis);) {
            while ((entry = (TarArchiveEntry) tis.getNextEntry()) != null) {
                String name = entry.getName();
                Path fileToCreate = targetDirectory.resolve(name).normalize();
                Path parent = fileToCreate.getParent().normalize().toAbsolutePath();
                ByteArrayOutputStream archiveEntry = new ByteArrayOutputStream();
                System.setOut(new PrintStream(archiveEntry));
                new ArchiveExtract().processEntry(parent, fileToCreate, entry, tis);
                System.setOut(initialOut);
                assertTrue(logContains(archiveEntry, String.format("Directory %s created", fileToCreate)));
                Files.delete(fileToCreate);
                archiveEntry.close();
                System.out.println(
                        "SUCCESS: Handling of an entry containing a directory that does not exist was tested.");
                Files.delete(targetDirectory);
                break;
            }
            Files.deleteIfExists(targetDirectory);
        } catch (IOException e) {
            System.out.println(String.format(
                    "FAIL: IOException %s when an entry containing a directory that does not exist was tested.",
                    e.getMessage()));
        }
    }

    /**
     * Test the case where the archive entry contains a directory that does exist
     *
     * @throws IOException
     */
    @Test
    void testArchiveEntryDirectoryExist() throws IOException {
        TarArchiveEntry entry = null;
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions
                .asFileAttribute(PosixFilePermissions.fromString("rwxr-----"));
        Path targetDirectory = Files.createTempDirectory("testArchiveEntryDirectoryExist", attr);
        Files.createDirectory(targetDirectory.resolve("test"));
        try (InputStream is = Files.newInputStream(Paths.get("src/test/resources/test.tgz"));
                BufferedInputStream bis = new BufferedInputStream(is);
                GzipCompressorInputStream gis = new GzipCompressorInputStream(bis);
                TarArchiveInputStream tis = new TarArchiveInputStream(gis);) {
            while ((entry = (TarArchiveEntry) tis.getNextEntry()) != null && entry.isDirectory()) {
                // while ((entry = (TarArchiveEntry) tis.getNextEntry()) != null) {
                String name = entry.getName();
                Path fileToCreate = targetDirectory.resolve(name).normalize();
                Path parent = fileToCreate.getParent().normalize().toAbsolutePath();
                ByteArrayOutputStream archiveEntry = new ByteArrayOutputStream();
                System.setOut(new PrintStream(archiveEntry));
                new ArchiveExtract().processEntry(parent, fileToCreate, entry, tis);
                System.setOut(initialOut);
                assertFalse(logContains(archiveEntry, String.format("Directory %s created", fileToCreate)));
                Files.delete(fileToCreate);
                archiveEntry.close();
                System.out.println(
                        "SUCCESS: Handling of an entry containing a directory that already exists was tested.");
                Files.delete(targetDirectory);
                break;
            }
            if (Files.exists(targetDirectory)) {
                Files.delete(targetDirectory); /// it should be empty at this point, if not the IO Exception will happen
            }
        } catch (IOException e) {
            System.out.println(String.format(
                    "FAIL: IOException %s when an entry containing a directory that already exists was tested.",
                    e.getMessage()));
        }
    }

    /**
     * Answers true if the log contains a particular entry
     * 
     * @param bais the log
     * @param s    entry being looked for
     * @return the Path of the extract directory
     */
    private boolean logContains(ByteArrayOutputStream bais, String s) {
        return bais.toString().contains(s);
    }

    /**
     * Parses the name of the extract directory from the input string which contains
     * redirected output from the archive extraction
     * 
     * @param s the outout from the archive extraction
     * @return the Path of the extract directory
     */
    private Path getExtractDirectory(String s) {
        String extractDir = s.substring(s.indexOf("\n") + 1 + "Extract Target Directory: ".length(),
                s.indexOf("\n", s.indexOf("\n") + 1));
        return Paths.get(extractDir);
    }

    /**
     * Extracts an archive
     * 
     * @oaram a the name of the archive file to extract
     * @return a Path to which the archive was extracted
     * @throws IOException getting the extract directory
     */
    private Path extractToPath(String a) throws IOException {
        Path extractDir = getExtractDirectory(extract(prepForExtract(a)));
        assertTrue(Files.exists(extractDir));
        return extractDir;
    }

    /**
     * Extract the archive
     * 
     * @param a the archive
     * @return a string containing the first part of the redirected output (enough
     *         to parse the name of the directory to which the archive was extracted)
     * @throws IOException during extraction
     */
    private String extract(String[] a) throws IOException {
        ArchiveExtract.main(a);
        System.setOut(initialOut);
        return new String(testOut.toByteArray(), 0,
                testOut.size() < EXTRACT_OUT_LENGTH ? testOut.size() : EXTRACT_OUT_LENGTH);
    }

    /**
     * Redirect standard out to another stream so it cam be inspected to gather
     * information like the name of the directory to which the archive was extracted
     * and then constructs the arguments to be passed to main
     * 
     * @param a The name of the archive
     * @return a atring array of zero or one element, namely a Path to the archive
     */
    private String[] prepForExtract(String a) {
        System.setOut(new PrintStream(testOut));
        if (!a.isEmpty()) {
            Path tgzFile = Paths.get(a).toAbsolutePath();
            return new String[] { tgzFile.toString() };
        }
        return new String[] {};
    }
}
