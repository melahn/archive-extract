package com.melahn.util.zip;

import java.util.UUID;

/*
 *  ZipSlipException is an exception class designed for use in testing zip slip exceptions
 *  as described by https://github.com/snyk/zip-slip-vulnerability
*/
public class ZipSlipException extends SecurityException {
    static final long serialVersionUID = UUID.fromString("5a8dba66-71e1-492c-bf3b-53cceb67b785")
            .getLeastSignificantBits();

    public ZipSlipException(String message) {
        super(message);
    }
}
