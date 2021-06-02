package com.melahn.util.extract;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class ArchiveExtract {

    public static final String DEFAULT_ARCHIVE_FILENAME = "test.tgz";
    public static final String EXTRACT_DIR_OUTPUT_LABEL = "Extract Target Directory: ";
    public static final String EXTRACT_FILE_OUTPUT_LABEL = "Archive File: ";
    private static final Logger logger = LogManager.getLogger("ArchiveExtract");
    private static final String SEPARATOR = FileSystems.getDefault().getSeparator();
    private int depth = 0; // for keeping track of nested archive file depth
    private boolean halted = false; // of keeping track of when an extract is deliberately halted prematurely

    /**
     * Using test.tgz or the name of some other archive supplied in args[0], extract
     * the archive, checking for the zip slip vulnerability and deeply nested
     * archives.
     * 
     * @param args optionally, args[0] contains the file name of the archive
     * @throws IOException if an IO error occurs during extraction
     * @throws ArchiveExtractException if a zip slip exception occcurs
     * @throws IllegalArgumentException if an empty archive name is used
     */
    public static void main(String[] args) throws ArchiveExtractException, IOException {
        try {
            String archiveFileName = args.length > 0 ? args[0] : DEFAULT_ARCHIVE_FILENAME;
            ArchiveExtract ae = new ArchiveExtract();
            logger.info("{}{}", EXTRACT_FILE_OUTPUT_LABEL, archiveFileName);
            Path tempDir = ae.createExtractDir();
            logger.info("{}{}", EXTRACT_DIR_OUTPUT_LABEL, tempDir);
            ae.extract(archiveFileName, tempDir);
        } catch (IOException e) {
            logger.error("Exception {}: {}", e.getClass(), e.getMessage());
            throw e;
        } catch (ArchiveExtractException e) {
            logger.error("{}: {}", e.getClass().getName(), e.getMessage());
            throw e;
        }
    }

    /**
     * Extract the files in a tgz archive file
     * 
     * @param a The name of an archive file
     * @param t The path in which to extract the file
     * @throws IOException during IO on an archive entry
     * @throws IllegalArgumentException if a is empty or null or t is null
     */
    public void extract(String a, Path t) throws IOException, IllegalArgumentException {
        if (a == null || a.isEmpty() || t == null ) {
            throw new IllegalArgumentException();
        }
        if (depth > 5) {
            logger.info("Too many layers of embedded archives were found. Extraction halted");
            halted = true;
            return;
        }
        logger.info("Depth = {}", depth++);
        try (InputStream is = Files.newInputStream(Paths.get(a));
                BufferedInputStream bis = new BufferedInputStream(is);
                GzipCompressorInputStream gis = new GzipCompressorInputStream(bis);
                TarArchiveInputStream tis = new TarArchiveInputStream(gis);) {
            TarArchiveEntry entry = null;
            while ((entry = (TarArchiveEntry) tis.getNextEntry()) != null) {
                String name = entry.getName();
                if (isHidden(name)) {
                    logger.debug("Entry {} skipped", name);
                    continue;
                }
                // Note the order in which the entries appear is not predictable so it is
                // possible to encounter a file before encountering the directory to which
                // the file will be extracted
                Path fileToCreate = t.resolve(name).normalize();
                Path parent = fileToCreate.getParent().normalize().toAbsolutePath();
                // Check for the Zip Slip Vulnerability
                checkForZipSlip(parent, t, name);
                processEntry(parent, fileToCreate, entry, tis);
            }
            logger.info("Archive File {} {}", a, halted ? "extraction was halted" : "successfully extracted");
            depth--;
        }
    }

    /**
     * Process an entry in the archive
     * 
     * @param p path of the parent directory
     * @param f path of the file to create
     * @param e the archive entry
     * @param t input stream to read the entry
     * @throws IOException when an exception occurs creating or reading a file
     */
    protected void processEntry(Path p, Path f, TarArchiveEntry e, TarArchiveInputStream t) throws IOException {
        if (Files.notExists(p)) { // first create the parent directory if it does not exist
            Files.createDirectories(p);
            logger.debug("Directory {} created", p);
        }
        if (e.isDirectory() && Files.notExists(f)) { // create a directory
            Files.createDirectory(f);
            logger.debug("Directory {} created", f);
        } else if (Files.notExists(f)) { // create a file
            Path newFile = Files.createFile(f);
            Files.copy(t,newFile,StandardCopyOption.REPLACE_EXISTING);
            logger.debug("File {} created", f);
            if (isArchive(e.getName())) {
                logger.debug("An embedded archive {} was found", e.getName());
                extract(newFile.toString(), newFile.getParent()); // recursion
            }
        }
    }

    /**
     * create a directory in which to extract the archive
     * 
     * @return a Path for the created directory
     * @throws IOException if an IO error creating the dir
     */
    protected Path createExtractDir() throws IOException {
        String t = this.getClass().getCanonicalName() + "." + "Temporary.";
        try {
            FileAttribute<Set<PosixFilePermission>> a = PosixFilePermissions
                    .asFileAttribute(PosixFilePermissions.fromString("rwxr-----"));
            Path p = Files.createTempDirectory(t, a);
            if (p == null) {
                throw new IOException();
            }
            return p;
        } catch (IOException e) {
            logger.error("IO Exception creating directory {}", t);
            throw e;
        }
    }

    /**
     * Answers whether a file name is hidden or not
     * 
     * @param s a file name
     * @return true if hidden, false otherwise
     */
    protected boolean isHidden(String s) {
        // strip any trailing slash to test for both hidden files and directories
        String t = s != null && s.endsWith(SEPARATOR) ? s.substring(0, s.length() - 1) : s;
        return t != null && !t.contentEquals(".") && !t.contentEquals("..") // not null and not just relative
                                                                            // directories
                && (t.contains(SEPARATOR) && (t.substring(t.lastIndexOf(SEPARATOR) + 1, t.length())).startsWith(".")
                        // last segment starts with a dot
                        || (!t.contains(SEPARATOR) && t.startsWith("."))); // just a filename with a leading dot
    }

    /**
     * Answers whether a file name is an archive file
     * 
     * @param s a file name
     * @return true if an archive file, false otherwise
     */
    protected boolean isArchive(String s) {
        return s != null && GzipUtils.isCompressedFilename(s);
    }

    /**
     * Checks for the zip slip vulnerability by testing whether a file, if
     * extracted, would lie outside of the target directory for extracting the
     * archive. If the vulnerability is detected the method throws the
     * ArchiveExtractException.
     * 
     * For example, if the target extract directory is /foo/target and the file
     * evil.txt would have the path /bar/evil.txt, then the method would throw the
     * ArchiveExtractException.
     * 
     * @param p the parent directory of the file, if it were extracted
     * @param t the target directory where files from the archive are extracted
     * @param n the name of the file (just used for the exception message, if there
     *          is an exception)
     * @throws ArchiveExtractException if a zip slip issue was found
     */
    private void checkForZipSlip(Path p, Path t, String n) throws ArchiveExtractException {
        if (!p.startsWith(t)) {
            throw new ArchiveExtractException(
                    String.format("File %s lies outside of target directory which is a security exposure", n));
        }
    }
}