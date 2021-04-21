package com.melahn.util.zip;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ZipSlipExample {

    private static final Logger logger = LogManager.getLogger("ZipSlipExample");
    private static final String SEPARATOR = FileSystems.getDefault().getSeparator();
    private static final int BUFFER_SIZE=1024;
    private static final int MAX_DEPTH=20;
    public static final String DEFAULT_TGZ_FILENAME = "test.tgz";

    /**
     * Using test.tgz or the name supplied in args[0], unzip the tgz without
     * checking for the zip slip issue.
     * 
     * @param args optonally args[0] contains the name of the zip file
     */
    public static void main(String[] args) throws ZipSlipException {
        try {
            String zipFileName = args.length > 0 ? args[0] : DEFAULT_TGZ_FILENAME;
            ZipSlipExample zse = new ZipSlipExample();
            logger.info("Zip File: {}", zipFileName);
            Path tempDir = zse.createTempDir();
            logger.info("Unzip Target Directory: {}", tempDir);
            zse.unzip(zipFileName, tempDir);
            zse.unzipEmbeddedZips(tempDir);
        } catch (IOException e) {
            logger.error("Exception {}: {}", e.getClass(), e.getMessage());
        } catch (ZipSlipException e) {
            logger.error("ZipSlipException: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Induce the Zip Slip security issue by not checking an entry in the file
     * crossing the boundary of the zip file directory
     * 
     * @param z The name of a tgz file
     * @param t The directory in which to unzip the file
     * @throws IOException
     */
    private void unzip(String z, Path t) throws IOException {
        try (InputStream is = Files.newInputStream(Paths.get(z));
                BufferedInputStream bis = new BufferedInputStream(is);
                GzipCompressorInputStream gis = new GzipCompressorInputStream(bis);
                TarArchiveInputStream tis = new TarArchiveInputStream(gis);) {
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tis.getNextEntry()) != null) {
                String name = entry.getName();
                if (isHiddenFilename(name)) {
                    logger.info("Entry {} skipped", name);
                    continue;
                }
                Path fileToCreate = t.resolve(name);
                byte[] data = new byte[BUFFER_SIZE];
                // Note the order in which the entries appear is not predictable so it is
                // possible to encounter a file before encountering the directory to which
                // the file will be extracted
                Path parent = fileToCreate.getParent().normalize().toAbsolutePath();
                // Check for the Zip Slip Exception
                checkForZipSlip(parent, t, name);
                if (Files.notExists(parent)) { // first create the parent directory if it does not exist
                    Files.createDirectories(parent);
                    logger.info("Directory {} created", parent);
                }
                if (entry.isDirectory() && Files.notExists(fileToCreate)) { // create the directory
                    Files.createDirectory(fileToCreate);
                    logger.info("Directory {} created", fileToCreate);
                } else if (Files.notExists(fileToCreate)) { // create the file
                    Path newFile = Files.createFile(fileToCreate);
                    logger.info("File {} created", fileToCreate);
                    while ((tis.read(data, 0, BUFFER_SIZE)) != -1) {
                        Files.write(newFile, data, StandardOpenOption.APPEND);
                    }
                    logger.info("File {} created", fileToCreate);
                }
            }
            logger.info("File {} unzipped", z);
        } 
    }

    /**
     * create a directory in which to unzip the file
     * 
     * @return a Path for the created directory
     * @throws IOException
     */
    private Path createTempDir() throws IOException {
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
     * Unpacks any tgz files found in a directory
     *
     * @param d the name of the directory in which to look
     */
    private void unzipEmbeddedZips(Path d) throws IOException {
        try (Stream<Path> w = Files.walk(d, MAX_DEPTH)) {
            List<Path> tgzFiles = w.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".tgz")).collect(Collectors.toList());
            for (Path z : tgzFiles) {
                Path r = d.resolve(z);
                logger.info("tgz file: {} found", r);
                unzip(r.toString(), r.getParent());
            }
        } catch (IOException e) {
            logger.error("IOException looking for embedded zip files");
        }
    }

    private boolean isHiddenFilename(String s) {
        return s.contains(SEPARATOR) && s.lastIndexOf(SEPARATOR) != s.length() - 1
                && (s.substring(s.lastIndexOf(SEPARATOR) + 1, s.length())).startsWith(".");
    }

    private void checkForZipSlip(Path p, Path t, String n) throws ZipSlipException {
        if (!p.startsWith(t)) {
            throw new ZipSlipException(String.format("File %s lies outside of target directory which is a security exposure", n));
        }
    }
}