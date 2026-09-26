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
package org.apache.maven.index.reader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UT for {@link Utils}.
 */
class UtilsTest {
    @Test
    void loadProperties() throws Exception {
        Properties properties =
                Utils.loadProperties(resource("nexus.index.id=central\n".getBytes(StandardCharsets.UTF_8)));

        assertEquals("central", properties.getProperty("nexus.index.id"));
    }

    @Test
    void loadPropertiesRejectsOversizedFile() {
        assertThrows(IOException.class, () -> Utils.loadProperties(resource(new byte[2 * 1024 * 1024])));
    }

    private static ResourceHandler.Resource resource(byte[] data) {
        return new ResourceHandler.Resource() {
            @Override
            public InputStream read() {
                return new ByteArrayInputStream(data);
            }

            @Override
            public void close() {}
        };
    }
}
