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
            logger.info("Unzip Target Directory: {}", tempDir);
            zse.unzip(zipFileName, tempDir);
            zse.unzipEmbeddedZips(tempDir);
        } catch (IOException e) {
            logger.error("IOException: {}", e.getMessage());
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
                // protect against hidden files and relative names
                if (name.contains(SEPARATOR) && name.lastIndexOf(SEPARATOR) != name.length() - 1
                        && (name.substring(name.lastIndexOf(SEPARATOR) + 1, name.length())).startsWith(".")) {
                    logger.info("Entry {} skipped", name);
                    continue;
                }
                Path fileToCreate = t.resolve(name);
                final int bufferSize = 1024;
                byte[] data = new byte[bufferSize];
                // Note the order in which the entries appear is not predictable
                Path parent = fileToCreate.getParent();
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
                    while ((tis.read(data, 0, bufferSize)) != -1) {
                        Files.write(newFile, data, StandardOpenOption.APPEND);
                    }
                    logger.info("File {} created", fileToCreate);
                }
            }
            logger.info("File {} unzipped", z);
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
        try (Stream<Path> w = Files.walk(d, 10)) {
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
}