# test-zip

A project to test zip vulnerabilities with Sonar

![version](https://img.shields.io/badge/version-1.0.0-SNAPSHOT-orange)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

[![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-white.svg)](https://sonarcloud.io/dashboard?id=melahn_test-zip)

## Overview

The Zip Slip vulnerability is well described [here](https://github.com/snyk/zip-slip-vulnerability). It is possible for a naive extracter to
unintentionally extract a potentially malicious file without explictly checking for this vulnerability.

Fortunately, Sonar catches this case and will alert you to the exposure. See this [example](https://sonarcloud.io/project/issues?id=helm-chartmap&open=AXi35lCDkE4Xha4vbz5h&resolved=false&types=VULNERABILITY).

## Illustration

Consider the case where an archive contains a file which, when extracted, would be extracted in a directory outside the target directory. This would be
a way for malicious files to be delivered. An example of such an archive can be found [here](./src/test/resources/test-chart-file-with-zip-slip-vulnerability.tgz).  This file contains a file named *evil.txt* that would be extracted to the parent directory of the unzip directory.

This project checks for this case and throws an exception when found.

To see an illustration, run this command.

    java -jar target/test-zip-1.0.0-SNAPSHOT.jar src/test/resources/test-chart-file-with-zip-slip-vulnerability.tgz
