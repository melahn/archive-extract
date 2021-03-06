# archive-extract

A project to extract a compressed archive.

![version](https://img.shields.io/badge/version-1.0.0-black)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Build](https://github.com/melahn/test-zip-slip/actions/workflows/build.yml/badge.svg)](https://github.com/melahn/archive-extract/actions/workflows/build.yml)
[![Deploy](https://github.com/melahn/test-zip-slip/actions/workflows/deploy.yml/badge.svg)](https://github.com/melahn/archive-extract/actions/workflows/deploy.yml)
[![Sonar Cloud Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=melahn_java-trace-visualizer&metric=alert_status)](https://sonarcloud.io/dashboard?id=melahn_test-zip)

## Overview

Extracting a compressed archive file is well understood but I wanted a project that would test specifically for
the *Zip Slip Vulnerability* described [here](https://github.com/snyk/zip-slip-vulnerability). It is possible for a naive archive extractor to
unintentionally extract a potentially malicious file without explicitly checking for this vulnerability. I also wanted the extractor to protect the user from extracting archives with unusual nesting patterns. And I wanted it to have 100% Sonar code coverage with no code smells, maintainability issues, security hotspots, etc.

## Dependency Info

Available from the [Maven Central Repository](https://search.maven.org/search?q=melahn) and from [GitHub Packages](https://github.com/melahn/archive-extract/packages)

```xml
<dependency>
  <groupId>com.melahn</groupId>
  <artifactId>archive-extract</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Usage

Download the Jar Package from a [GitHub workflow](https://github.com/melahn/archive-extract/actions) and extract the jar from it, or build the jar using mvn install.  Then, run the command line as described below.

### Command Line

To extract a compressed archive, run this command.

```bash
     java -jar <archive-extract jar file name> <compressed archive file name>
```

### Command Line Examples

#### Safe archives

Benign archives that don't contain any known vulnerabilities are included in the project.  To try one of them out, run this command.

```bash
     java -jar target/archive-extract-1.0.0.jar src/test/resources/test.tgz
```

#### Unsafe Archives

##### Zip Slip Vulnerability

Consider the case where an archive contains a file which, when extracted, would be placed in a directory outside the target directory. This would be
a way for malicious files to be delivered. An example of such an archive can be found [here](./src/test/resources/test-chart-file-with-zip-slip-vulnerability.tgz). This archive contains a file named *evil.txt* waiting to be extracted to the *parent* directory of the extract directory.

This project contains code that extracts a tgz archive, checks for this vulnerabilty and throws an exception when found.

To try it out, run this command.

```bash
    java -jar target/archive-extract-1.0.0.jar src/test/resources/test-chart-file-with-zip-slip-vulnerability.tgz
```

##### Nested Archive

Another example of a potentially unsafe archive is one that contains many levels of nested zip files. An extreme example of that is a [quine](https://research.swtch.com/zip) which would expand infinitely. The extractor
in this project stops extracting when it finds itself more than five levels deep in a nested archive. An example of such an archive can be found [here](./src/test/resources/test-with-depth-six.tgz).

To try it out, run this command.

```bash
    java -jar target/archive-extract-1.0.0.jar src/test/resources/test-with-depth-six.tgz
```

### Java API

```java
     public void extract(String archiveName, Path targetDirectory) throws IOException, IllegalArgumentException
```

### Java API Example

```java
     public class ArchiveExtractExample {
          public static void main (String args[]) throws java.io.IOException {
               String archive = "src/test/resources/test.tgz";
               java.nio.file.Path targetDirectory = java.nio.file.Files.createTempDirectory("ArchiveExtractExample");
               new com.melahn.util.extract.ArchiveExtract().extract(archive, targetDirectory);
          }
     }
```

## Build and CI/CD Notes

The artifact is published to both the Maven Central Repository and as a GitHub Package using the [Deploy GitHub Workflow](https://github.com/melahn/archive-extract/blob/main/.github/workflows/deploy.yml).

The artifact is also build automatically whenever code is pushed or pulled, using the [Build GitHub Workflow](https://github.com/melahn/archive-extract/blob/main/.github/workflows/build.yml).

The artifact can also be built locally using the default maven build profile. This has been tested with Maven 3.8.3 though other versions >= 3 should also work.  

You will see a warning

```text
     target/classes (Is a directory)
```

when the shaded jar is built. This warning is due to a long-standing issue where the shade plugin checks if a classpath element is a jar, and if it is not, swallows useful error information, instead printing out a meaningless warning '(Is a directory)'.  See <https://issues.apache.org/jira/browse/MSHADE-376>

See <https://github.com/melahn/maven-shade-plugin> if you want to install your own version of the plugin, with a fix.
