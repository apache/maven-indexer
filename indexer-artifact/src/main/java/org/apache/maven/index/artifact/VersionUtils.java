package org.apache.maven.index.artifact;

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

import java.util.regex.Pattern;

/**
 * Utility methods for working with artifact version strings.
 */
public class VersionUtils
{

    private static String SNAPSHOT_VERSION = "SNAPSHOT";

    private static final Pattern VERSION_FILE_PATTERN =
        Pattern.compile(
            "^(.*)-([0-9]{8}.[0-9]{6})-([0-9]+)$|^([0-9]{8}.[0-9]{6})-([0-9]+)$|^(.*)([0-9]{8}.[0-9]{6})-([0-9]+)$" );

    public static boolean isSnapshot( String version )
    {
        return VERSION_FILE_PATTERN.matcher( version ).matches() || version.endsWith( SNAPSHOT_VERSION );
    }
}
