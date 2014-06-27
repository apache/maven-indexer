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

/**
 * An interface to calculate <code>Gav</code> based on provided artifact path and to calculate an artifact path from
 * provided <code>Gav</code>.
 * 
 * @author Tamas Cservenak
 */
public interface GavCalculator
{
    /**
     * Calculates GAV from provided <em>repository path</em>. The path has to be absolute starting from repository root.
     * If path represents a proper artifact path (conforming to given layout), GAV is "calculated" from it and is
     * returned. If path represents some file that is not an artifact, but is part of the repository layout (like
     * maven-metadata.xml), or in any other case it returns null.
     * 
     * @param path the repository path
     * @return Gav parsed from the path
     */
    Gav pathToGav( String path );

    /**
     * Reassembles the repository path from the supplied GAV. It will be an absolute path.
     * 
     * @param gav
     * @return the path calculated from GAV, obeying current layout.
     */
    String gavToPath( Gav gav );
}
