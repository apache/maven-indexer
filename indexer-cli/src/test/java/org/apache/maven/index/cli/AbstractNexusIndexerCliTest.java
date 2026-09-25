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
package org.apache.maven.index.cli;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractNexusIndexerCliTest extends InjectedTest {

    private static final long rand = new Random().nextLong();

    /*
     * private static final String DEST_DIR = new File( getBasedir(), "target/tests/clitest/output-"+rand
     * ).getAbsolutePath(); private static final String INDEX_DIR = new File( getBasedir(),
     * "target/tests/clitest/index-"+rand ).getAbsolutePath(); private static final String UNPACK_DIR = new File(
     * getBasedir(), "target/tests/clitest/unpack-"+rand ).getAbsolutePath(); private static final String TEST_REPO =
     * new File( getBasedir(), "src/test/repo" ).getAbsolutePath();
     */
    private final String DEST_DIR =
            new File(getBasedir(), "target/tests/clitest-" + rand + "/output").getAbsolutePath();

    private final String INDEX_DIR =
            new File(getBasedir(), "target/tests/clitest-" + rand + "/index").getAbsolutePath();

    private final String UNPACK_DIR =
            new File(getBasedir(), "target/tests/clitest-" + rand + "/unpack").getAbsolutePath();

    private final String TEST_REPO = new File(getBasedir(), "src/test/repo").getAbsolutePath();

    protected OutputStream out;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        out = new OutputStream() {

            private StringBuilder buf = new StringBuilder();

            @Override
            public void write(int b) {
                byte[] bytes = new byte[1];
                bytes[0] = (byte) b;
                buf.append(new String(bytes));
            }

            @Override
            public String toString() {
                String string = buf.toString();
                buf = new StringBuilder();
                return string;
            }
        };

        FileUtils.deleteDirectory(INDEX_DIR);
        FileUtils.deleteDirectory(DEST_DIR);
        FileUtils.deleteDirectory(UNPACK_DIR);
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    protected File getTestFile(String path) {
        return new File(new File(getBasedir()), path);
    }

    @Test
    public void testNoArgs() {
        int code = execute();
        String output = out.toString();
        assertEquals(1, code, output);
        assertTrue(output.contains("usage: nexus-indexer [options]"), "Should print usage");
    }

    @Test
    public void testRequiredArgs() throws Exception {
        int code = execute("--repository", TEST_REPO, "--index", INDEX_DIR, "-d", DEST_DIR);
        String output = out.toString();
        assertEquals(0, code, output);
    }

    @Test
    public void testUnpack() throws Exception {
        // first create an index, in the destination dir
        execute("--repository", TEST_REPO, "--index", INDEX_DIR, "-d", DEST_DIR);
        // then unpack it
        int code = execute("--unpack", "--index", DEST_DIR, "-d", UNPACK_DIR);
        String output = out.toString();
        assertEquals(0, code, output);

        // FIXME: Looks strange that a newly generated index can not be reopened.
        // assertIndexFiles( UNPACK_DIR );
    }

    @Test
    public void testMissingArgs() throws IOException {
        String usage = "usage: nexus-indexer";

        int code = execute("--repository", "--index", INDEX_DIR, "-d", DEST_DIR);
        String output = out.toString();
        assertEquals(1, code, output);
        assertTrue(output.contains(usage), "Should print bad usage");

        code = execute("--repository", TEST_REPO, "--index", "-d", DEST_DIR);
        output = out.toString();
        assertEquals(1, code, output);
        assertTrue(output.contains(usage), "Should print bad usage");

        code = execute("--repository", TEST_REPO, "--index", INDEX_DIR, "-d");
        output = out.toString();
        assertEquals(1, code, output);
        assertTrue(output.contains(usage), "Should print bad usage");

        code = execute("--repository", "--index", "-d");
        output = out.toString();
        assertEquals(1, code, output);
        assertTrue(output.contains(usage), "Should print bad usage but '" + output + "'");

        assertFalse(new File(INDEX_DIR).exists(), "Index file was generated");
    }

    @Test
    public void testAbrvsRequiredArgs() throws Exception {
        int code = execute("-r", TEST_REPO, "-i", INDEX_DIR, "-d", DEST_DIR);
        String output = out.toString();
        assertEquals(0, code, output);
    }

    @Test
    public void testInvalidRepo() throws Exception {
        int code = execute(
                "-r",
                new File("target/undexinting/repo/to/try/what/will/happen/here").getCanonicalPath(),
                "-i",
                INDEX_DIR,
                "-d",
                DEST_DIR);
        String output = out.toString();
        assertEquals(1, code, output);
    }

    protected abstract int execute(String... args);
}
