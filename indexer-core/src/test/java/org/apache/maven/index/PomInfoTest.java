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
package org.apache.maven.index;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.EntityReplacementMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that {@link PomInfo} agrees with the Maven model reader on the POMs in the test resources.
 */
class PomInfoTest {

    private static PomInfo read(String xml) throws IOException {
        return PomInfo.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    @SuppressWarnings("deprecation")
    private static void assertParity(Path pom) throws Exception {
        Model model;
        try (InputStream in = Files.newInputStream(pom)) {
            model = new MavenXpp3Reader().read(in, false);
        }
        PomInfo info;
        try (InputStream in = Files.newInputStream(pom)) {
            info = PomInfo.read(in);
        }
        assertEquals(model.getName(), info.getName(), pom + " name");
        assertEquals(model.getDescription(), info.getDescription(), pom + " description");
        assertEquals(model.getPackaging(), info.getPackaging(), pom + " packaging");
    }

    @Test
    void sameAsModelReaderOnAllTestPoms() throws Exception {
        List<Path> poms;
        try (Stream<Path> files = Files.walk(Path.of("src/test"))) {
            poms = files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".pom")
                            || p.getFileName().toString().equals("pom.xml"))
                    .collect(Collectors.toList());
        }
        assertFalse(poms.isEmpty());
        for (Path pom : poms) {
            assertParity(pom);
        }
    }

    @Test
    void pomWithParent() throws Exception {
        Path pom = Path.of("src/test/nexus-3233/cisco/infra/dft/dma.maven.plugins/1.0-SNAPSHOT/"
                + "dma.maven.plugins-1.0-20080409.021413-1.pom");
        assertParity(pom);
    }

    @Test
    void packagingOmitted() throws Exception {
        PomInfo info = read("<project><parent><artifactId>p</artifactId><name>not me</name></parent>"
                + "<artifactId>a</artifactId></project>");
        assertEquals("jar", info.getPackaging());
        assertNull(info.getName());
        assertNull(info.getDescription());
    }

    @Test
    void onlyTopLevelElementsAndTrimmed() throws Exception {
        PomInfo info = read("<project><name>\n  My Name </name><description/>"
                + "<dependencies><dependency><name>x</name><packaging>y</packaging></dependency></dependencies>"
                + "<packaging> war </packaging></project>");
        assertEquals("My Name", info.getName());
        assertEquals("", info.getDescription());
        assertEquals("war", info.getPackaging());
    }

    @Test
    void latin1EntitiesAndCdata() throws Exception {
        PomInfo info = read("<project><name>Bj&oslash;rn &amp; <![CDATA[<x>]]> &#65;</name></project>");
        assertEquals("Bj\u00f8rn & <x> A", info.getName());
    }

    @Test
    void resolvesEveryEntityTheModelReaderResolves() throws Exception {
        java.lang.reflect.Field names = EntityReplacementMap.class.getDeclaredField("entityName");
        java.lang.reflect.Field values = EntityReplacementMap.class.getDeclaredField("entityReplacement");
        java.lang.reflect.Field end = EntityReplacementMap.class.getDeclaredField("entityEnd");
        names.setAccessible(true);
        values.setAccessible(true);
        end.setAccessible(true);
        EntityReplacementMap map = EntityReplacementMap.defaultEntityReplacementMap;
        String[] entityNames = (String[]) names.get(map);
        String[] entityValues = (String[]) values.get(map);
        int count = end.getInt(map);
        assertTrue(count > 240, "unexpectedly few entities: " + count);
        for (int i = 0; i < count; i++) {
            String xml = "<project><name>&" + entityNames[i] + ";</name><description>a&" + entityNames[i]
                    + ";b</description></project>";
            Model model =
                    new MavenXpp3Reader().read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), false);
            PomInfo info = read(xml);
            assertEquals(entityValues[i], info.getName(), entityNames[i]);
            assertEquals(model.getName(), info.getName(), entityNames[i]);
            assertEquals(model.getDescription(), info.getDescription(), entityNames[i]);
        }
    }
}
