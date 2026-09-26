/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.index.examples;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.ModelWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author mtodorov
 */
public class SimpleArtifactGenerator {

    public SimpleArtifactGenerator() {
        // no op
    }

    public Path generateArtifact(
            Path repositoryBasedir,
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String extension)
            throws IOException, NoSuchAlgorithmException, XmlPullParserException {
        Path artifactFile = Path.of(
                repositoryBasedir.toString(),
                groupId.replaceAll("\\.", File.separator) + File.separatorChar + artifactId + File.separatorChar
                        + version + File.separatorChar + artifactId + "-" + version
                        + (classifier != null ? "-" + classifier + File.separatorChar : "") + "." + extension);

        Files.createDirectories(artifactFile.getParent());

        createArchive(artifactFile, groupId, artifactId, version, extension);

        return artifactFile;
    }

    private void createArchive(Path artifactFile, String groupId, String artifactId, String version, String extension)
            throws NoSuchAlgorithmException, IOException, XmlPullParserException {
        ZipOutputStream zos = null;

        try {
            // Make sure the artifact's parent directory exists before writing the model.
            Files.createDirectories(artifactFile.getParent());

            String fileName = artifactFile.getFileName().toString();
            Path pomFile = artifactFile.resolveSibling(fileName.substring(0, fileName.lastIndexOf(".")) + ".pom");

            zos = new ZipOutputStream(Files.newOutputStream(artifactFile));

            generatePom(pomFile, groupId, artifactId, version, extension);

            addMavenPomFile(zos, pomFile, groupId, artifactId);
        } finally {
            if (zos != null) {
                zos.close();
            }
        }
    }

    protected void generatePom(Path pomFile, String groupId, String artifactId, String version, String type)
            throws IOException, XmlPullParserException, NoSuchAlgorithmException {

        // Make sure the artifact's parent directory exists before writing the model.
        Files.createDirectories(pomFile.getParent());

        Model model = new Model();
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setVersion(version);
        model.setPackaging(type); // This is not exactly correct.

        ModelWriter writer = new DefaultModelWriter();
        writer.write(pomFile.toFile(), null, model);
    }

    private void addMavenPomFile(ZipOutputStream zos, Path pomFile, String groupId, String artifactId)
            throws IOException {
        ZipEntry ze = new ZipEntry("META-INF/maven/" + groupId + "/" + artifactId + "/" + "pom.xml");
        zos.putNextEntry(ze);

        InputStream fis = Files.newInputStream(pomFile);

        byte[] buffer = new byte[1024];
        int len;
        while ((len = fis.read(buffer)) > 0) {
            zos.write(buffer, 0, len);
        }

        fis.close();
        zos.closeEntry();
    }

    public static String convertGAVToPath(
            String groupId, String artifactId, String version, String classifier, String extension) {
        String path = "";

        path += groupId.replaceAll("\\.", "/") + "/";
        path += artifactId + "/";
        path += version + "/";
        path += artifactId + "-";
        path += version;
        path += classifier != null ? "-" + classifier : "";
        path += "." + extension;

        return path;
    }
}
