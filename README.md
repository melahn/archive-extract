# test-zip

A project to test zip vulnerabilities with Sonar

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

[![Quality gate](https://sonarcloud.io/api/project_badges/quality_gate?project=melahn_test-zip)](https://sonarcloud.io/dashboard?id=melahn_test-zip)

## Overview

The Zip Slip vulnerability is well described [here](https://github.com/snyk/zip-slip-vulnerability). It is possible for a naive archive extracter to
unintentionally extract a potentially malicious file without explictly checking for this vulnerability.

## Example

Consider the case where an archive contains a file which, when extracted, would be placed in a directory outside the target directory. This would be
a way for malicious files to be delivered. An example of such an archive can be found [here](./src/test/resources/test-chart-file-with-zip-slip-vulnerability.tgz). This archive  contains a file named *evil.txt* waiting to be extracted to the *parent* directory of the extract directory.

This project contains code that extracts a tgz archive, checks for this vulnerabilty and throws an exception when found.

To try it out, run this command.

    java -jar target/test-zip-1.0.0-SNAPSHOT.jar src/test/resources/test-chart-file-with-zip-slip-vulnerability.tgz

Benign archives that don't contain the vulnerability are also included in the project.  To try one of them out, run this command.

     java -jar target/test-zip-1.0.0-SNAPSHOT.jar src/test/resources/test.tgz
