# Copy Maven Plugin

[![Java 8](https://img.shields.io/badge/java-8-blue.svg)](https://adoptopenjdk.net/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.evolvedbinary.maven.mfrey/opy-maven-plugin/badge.svg)](https://search.maven.org/search?q=g:com.evolvedbinary.maven.mfrey)

A Maven Plugin to copy files from one path to another based on several `path` instructions.
Also replaces contents inside the copied files on the fly.

## Provenance
This is a fork of the Copy Maven Plugin from [Antibrumm/copy-maven-plugin](https://github.com/Antibrumm/copy-maven-plugin), which itself was inspired by the `goldin` `copy-maven-plugin`. 

[![Build Status](https://travis-ci.org/evolvedbinary/copy-maven-plugin.png)](https://travis-ci.org/Antibrumm/copy-maven-plugin)

## Requirements
* Java 8
* Maven 3.2.0+

## Usage
```xml
<plugin>
    <groupId>com.evolvedbinary.maven.mfrey</groupId>
    <artifactId>copy-maven-plugin</artifactId>
    <version>2.0.0</version>
    <executions>
        <execution>
            <id>copy</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>copy</goal>
            </goals>
            <configuration>
                <showfiles>true</showfiles>
                <resources>
                    <resource>
                        <directory>${project.build.directory}</directory>
                        <move>true</move>
                        <excludes>
                            <exclude>**/*.java</exclude>
                            <exclude>${some-artifactId}-${some-version}/src/**</exclude>
                        </excludes>
                        <paths>
                            <path>
                                <from>tmp</from>
                                <to>output</to>
                            </path>
                        </paths>
                    </resource>
                    <resource>
                        <directory>${project.build.directory}</directory>
                        <move>true</move>
                        <includes>
                            <include>**/*.java</include>
                        </includes>
                        <excludes>
                            <exclude>${some-artifactId}-${some-version}/src/**</exclude>
                        </excludes>
                        <paths>
                            <path>
                                <from>tmp(\w+)</from>
                                <to>output$1</to>
                                <regex>true</regex>
                            </path>
                            <path>
                                <from>${escaped-groupId-folders}</from>
                                <to>${new-groupId-folders}</to>
                            </path>
                            <path>
                                <from>${escaped-artifactId}</from>
                                <to>${new-artifactId}</to>
                            </path>
                        </paths>
                        <replaces>
                            <replace>
                                <from>${escaped-groupId}</from>
                                <to>${new-groupId}</to>
                            </replace>
                            <replace>
                                <from>${escaped-artifactId}</from>
                                <to>${new-artifactId}</to>
                            </replace>
                        </replaces>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Options
```xml
<showfiles>true</showfiles>
<resource>
    <skip>false</skip>
    <directory>${project.build.directory}</directory>
    <charset>UTF-8</charset>
    <move>false</move>
    <replaceExisting>false</replaceExisting>
    <workOnFullPath>false</workOnFullPath>
    <normalizePath>true</normalizePath>
    <includes>
        <include>**/*.*</include>
    </includes>
    <excludes>
        <exclude>none</exclude>
    </excludes>
    <paths>
        <path>
            <from>folder</from>
            <to>some/more/folders</to>
            <regex>false</regex>
        </path>
    </paths>
    <replaces>
        <replace>
            <from>property</from>
            <to>newname</to>
            <regex>false</regex>
        </replace>
    </replaces>
</resource>
```
