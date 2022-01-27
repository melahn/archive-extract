package com.melahn.util.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.melahn.util.test.ArchiveExtractTestUtil;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArchiveExtractIntegrationTest {

    private List<String> args = new ArrayList<String>();
    private final String targetTestDirName = "target/integration-test";
    private final Path targetTestPath = Paths.get(targetTestDirName);
    private final String className = "com.melahn.util.extract.ArchiveExtract";
    private final Path JaCocoAgentPath = Paths.get("", "lib/org.jacoco.agent-0.8.7-runtime").toAbsolutePath();
    private final String JaCocoAgentString = JaCocoAgentPath.toString()
            .concat(".jar=destfile=../jacoco.exec,append=true");
    private final Path logFilePath = Paths.get(TARGET_TEST_DIR_NAME, "sub-process-out.txt");
    private final ArchiveExtractTestUtil utility = new ArchiveExtractTestUtil();

    private final static String DIVIDER = "---------------------------";
    private static final String TARGET_TEST_DIR_NAME = "target/integration-test";
    private static final Path TARGET_TEST_DIR_PATH = Paths.get(TARGET_TEST_DIR_NAME);
    private static final Path TARGET_TEST_FILE_PATH = Paths.get(TARGET_TEST_DIR_NAME, "test.tgz");
    private static final Path TARGET_TEST_SOURCE_FILE_PATH = Paths.get("src/test/resources/test.tgz");

    /**
     * Performs Integration Test setup by cleaning, then recreating, the test directory.
     */
    @BeforeAll
    static void setUp() {
        System.out.println(DIVIDER.concat(" INTEGRATION TESTS START ").concat(DIVIDER));
        try {
            ArchiveExtractTestUtil.cleanDirectory(TARGET_TEST_DIR_PATH);
            Files.createDirectories(TARGET_TEST_DIR_PATH);
            Files.copy(TARGET_TEST_SOURCE_FILE_PATH, TARGET_TEST_FILE_PATH);
            assertTrue(Files.exists(TARGET_TEST_FILE_PATH));
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @AfterAll
    static void cleanup() {
        /**
         * if the test.tgz file exists in the test directory, then remove it
         */
        try {
            Files.deleteIfExists(TARGET_TEST_FILE_PATH);
        } catch (IOException e) {
            System.out.println("Could not cleanup after test cases: ".concat(e.getMessage()));
        }
        System.out.println(DIVIDER.concat(" TESTS END ").concat(DIVIDER));
    }

    /*
     * Tests the no error, normal case in the shaded jar.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void normalTest() throws InterruptedException, IOException {
        args.add(TARGET_TEST_FILE_PATH.toAbsolutePath().toString());
        int exitValue = utility.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null, JaCocoAgentString,
                className, targetTestPath, logFilePath);
        assertEquals(0, exitValue);
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }
}
