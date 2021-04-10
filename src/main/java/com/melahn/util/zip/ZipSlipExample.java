package com.melahn.util.zip;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class ZipSlipExample {

    private static final Logger logger = LogManager.getLogger("ZipSlipExample");
    private static final char PATH_SEPARATOR = java.nio.file.FileSystems.getDefault().getSeparator().charAt(0);

    /**
     * Using test.tgz or the name supplied in args[0], unzip the tgz without
     * checking for the zip slip issue.
     * 
     * @param args optonally args[0] contains the name of the zip file
     */
    public static void main(String[] args) {
        try {
            String zipFileName = args.length > 0 ? args[0] : "test.tgz";
            logger.info("Zip File: {}", zipFileName);
            ZipSlipExample zse = new ZipSlipExample();
            Path tempDir = zse.createTempDir();
            zse.unzip(zipFileName, tempDir);
        } catch (IOException e) {
            logger.error("IOException: {}", e.getMessage());
        }
    }

    /**
     * Induce the Zip Slip security issue by not checking an entry in the file crossing the boundary of the 
     * zip file directory
     * 
     * @param z The name of a tgz file
     * @param t The directory in which to unzip the file
     * @throws IOException
     */
    private void unzip(String z, Path t) throws IOException {
        try ( InputStream is = Files.newInputStream(Paths.get(z));
                BufferedInputStream bis = new BufferedInputStream(is);
                GzipCompressorInputStream gis = new GzipCompressorInputStream(bis);
                TarArchiveInputStream tis = new TarArchiveInputStream(gis);) {
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tis.getNextEntry()) != null) {
                String name = entry.getName();
                String dirNameFromEntry = entry.isDirectory() ? entry.getName()
                        : entry.getName().substring(0, name.lastIndexOf(PATH_SEPARATOR));
                Path dirNameToCreate = t.resolve(dirNameFromEntry);
                if (!Files.exists(dirNameToCreate)) {
                    Files.createDirectories(dirNameToCreate);
                    logger.info("Directory {} created",dirNameToCreate);
                }
            }
        } catch (IOException e) {
            logger.error("IO Exception unzipping {}", z);
            throw e;
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
            logger.info("{} will be used as the directory.", p);
            return p;
        } catch (IOException e) {
            logger.error("IO Exception creating directory {}", p);
            throw e;
        }
    }
}