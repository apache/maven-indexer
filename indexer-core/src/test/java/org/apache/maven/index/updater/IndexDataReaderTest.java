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
package org.apache.maven.index.updater;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UT for {@link IndexDataReader} reading documents from a gzipped stream.
 */
public class IndexDataReaderTest {

    @Test
    public void streamEndingBetweenDocumentsIsTheEnd() throws IOException {
        IndexDataReader reader = reader(gzip(document("value".length(), "value")));

        Document document = reader.readDocument();
        assertNotNull(document);
        assertEquals("value", document.get("name"));
        assertNull(reader.readDocument());
    }

    @Test
    public void streamEndingEarlyIsAnError() throws IOException {
        byte[] data = gzip(document("value".length(), "value"));
        IndexDataReader reader = reader(Arrays.copyOf(data, data.length - 4));

        assertThrows(IOException.class, () -> {
            while (reader.readDocument() != null) {
                // read on
            }
        });
    }

    @Test
    public void negativeValueLengthIsAnError() throws IOException {
        IndexDataReader reader = reader(gzip(document(-1, "")));

        assertThrows(IOException.class, reader::readDocument);
    }

    private static IndexDataReader reader(byte[] data) throws IOException {
        IndexDataReader reader = new IndexDataReader(new BufferedInputStream(new ByteArrayInputStream(data)));
        reader.readHeader();
        return reader;
    }

    /**
     * A single document with one stored field {@code name}, whose value is written with the given length.
     */
    private static byte[] document(int valueLength, String value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(1); // field count
        out.writeByte(IndexDataWriter.F_STORED);
        out.writeUTF("name");
        out.writeInt(valueLength);
        out.writeBytes(value);
        return bytes.toByteArray();
    }

    private static byte[] gzip(byte[] documents) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
            out.writeByte(IndexDataWriter.VERSION);
            out.writeLong(0L);
            out.write(documents);
        }
        return bytes.toByteArray();
    }
}
