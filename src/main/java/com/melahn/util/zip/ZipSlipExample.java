package com.melahn.util.zip;

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
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class ZipSlipExample {

    private static final Logger logger = LogManager.getLogger("ZipSlipExample");
    private static final String SEPARATOR = FileSystems.getDefault().getSeparator();
    private static final int BUFFER_SIZE = 1024;
    public static final String DEFAULT_TGZ_FILENAME = "test.tgz";
    private int depth = 0;

    /**
     * Using test.tgz or the name of some other archive supplied in args[0], unzip
     * the archive, checking for the zip slip vulnerability.
     * 
     * @param args optonally args[0] contains the file name of the archive
     */
    public static void main(String[] args) throws ZipSlipException, IOException {
        try {
            String zipFileName = args.length > 0 ? args[0] : DEFAULT_TGZ_FILENAME;
            ZipSlipExample zse = new ZipSlipExample();
            logger.info("Zip File: {}", zipFileName);
            Path tempDir = zse.createExtractDir();
            logger.info("Unzip Target Directory: {}", tempDir);
            zse.unzip(zipFileName, tempDir);
        } catch (IOException e) {
            logger.error("Exception {}: {}", e.getClass(), e.getMessage());
            throw e;
        } catch (ZipSlipException e) {
            logger.error("ZipSlipException: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Extract the files in a tgz archive file
     * 
     * @param z The name of a tgz file
     * @param t The path in which to unzip the file
     * @throws IOException
     */
    private void unzip(String z, Path t) throws IOException {
        if (depth > 5) {
            logger.info("Too many layers of embedded archives were found. Extraction halted");
            return;
        }
        logger.info("Depth = {}", depth++);
        try (InputStream is = Files.newInputStream(Paths.get(z));
                BufferedInputStream bis = new BufferedInputStream(is);
                GzipCompressorInputStream gis = new GzipCompressorInputStream(bis);
                TarArchiveInputStream tis = new TarArchiveInputStream(gis);) {
            TarArchiveEntry entry = null;
            while ((entry = (TarArchiveEntry) tis.getNextEntry()) != null) {
                String name = entry.getName();
                if (isHidden(name)) {
                    logger.info("Entry {} skipped", name);
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
            logger.info("File {} unzipped", z);
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
     * @return
     * @throws IOException
     */
    private void processEntry(Path p, Path f, TarArchiveEntry e, TarArchiveInputStream t) throws IOException {
        if (Files.notExists(p)) { // first create the parent directory if it does not exist
            Files.createDirectories(p);
            logger.info("Directory {} created", p);
        }
        if (e.isDirectory() && Files.notExists(f)) { // create a directory
            Files.createDirectory(f);
            logger.info("Directory {} created", f);
        } else if (Files.notExists(f)) { // create a file
            byte[] data = new byte[BUFFER_SIZE];
            Path newFile = Files.createFile(f);
            logger.info("File {} created", f);
            while ((t.read(data, 0, BUFFER_SIZE)) != -1) {
                Files.write(newFile, data, StandardOpenOption.APPEND);
            }
            logger.info("File {} created", f);
            if (isArchive(e.getName())) {
                logger.info("An embedded archive {} was found", e.getName());
                unzip(newFile.toString(), newFile.getParent()); // recursion
            }
        }
    }

    /**
     * create a directory in which to extract the archive
     * 
     * @return a Path for the created directory
     * @throws IOException
     */
    protected Path createExtractDir() throws IOException {
        Path p = null;
        try {
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions
                    .asFileAttribute(PosixFilePermissions.fromString("rwxr-----"));
            p = Files.createTempDirectory(this.getClass().getCanonicalName() + "." + "Temporary.", attr);
            return p;
        } catch (IOException e) {
            logger.error("IO Exception creating directory {}", p);
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
        return t != null && !t.contentEquals(".") && !t.contentEquals("..") // not null and not just relative directories 
                && (t.contains(SEPARATOR) && (t.substring(t.lastIndexOf(SEPARATOR) + 1, t.length())).startsWith(".") // last segment starts with a dot
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
     * ZipSlipException.
     * 
     * For example, if the target extract directory is /foo/target and the file
     * evil.txt would have the path /bar/evil.txt, then the method would throw the
     * ZipSlipException.
     * 
     * @param p the parent directory of the file, if it were extracted
     * @param t the target directory where files from the archive are extracted
     * @param n the name of the file (just used for the exception message, if there
     *          is an exception)
     * @throws ZipSlipException
     */
    private void checkForZipSlip(Path p, Path t, String n) throws ZipSlipException {
        if (!p.startsWith(t)) {
            throw new ZipSlipException(
                    String.format("File %s lies outside of target directory which is a security exposure", n));
        }
    }
}