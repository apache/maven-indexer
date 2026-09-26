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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UT for {@link CleaningEncoder}.
 */
public class CleaningEncoderTest {
    private final CleaningEncoder encoder = new CleaningEncoder();

    @Test
    public void plainTextIsUnchanged() {
        assertEquals("commons logging", encoder.encodeText("commons logging"));
    }

    @Test
    public void textIsHtmlEscaped() {
        assertEquals("a &lt;b&gt; &amp; &quot;c&quot;", encoder.encodeText("a <b> & \"c\""));
    }

    @Test
    public void lineBreaksAreRemoved() {
        assertEquals("line oneline two", encoder.encodeText("line one\nline two"));
    }
}
