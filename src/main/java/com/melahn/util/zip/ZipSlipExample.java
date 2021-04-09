package com.melahn.util.zip;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ZipSlipExample {

    private static final Logger logger = LogManager.getLogger("ZipSlipExample");
    public static void main(String[] args ) {
        String zipFileName = args.length > 0? args[0] : "test.tgz";
        logger.info("Zip File: {}", zipFileName);
    }
}
