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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

public class NexusIndexerCliIT extends AbstractNexusIndexerCliTest {

    private Commandline createCommandLine() {
        try {
            Commandline cmd = new Commandline();
            cmd.setExecutable("java");
            cmd.setWorkingDirectory(Path.of(".").toFile().getCanonicalFile());
            cmd.createArg().setValue("-jar");
            cmd.createArg()
                    .setValue(Path.of(System.getProperty("indexerJar")).toFile().getCanonicalPath());
            return cmd;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected int execute(String... args) {
        Commandline cmd = createCommandLine();
        for (String arg : args) {
            cmd.createArg().setValue(arg);
        }
        // stdout and stderr are pumped by separate threads, so each gets its own buffer
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        try {
            int code = CommandLineUtils.executeCommandLine(
                    cmd,
                    line -> stdout.append(line).append('\n'),
                    line -> stderr.append(line).append('\n'));
            out.write(stderr.append(stdout).toString().getBytes());
            return code;
        } catch (CommandLineException e) {
            return -1;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
