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

import java.io.File;

/**
 * A utility component that resolves POM packaging to artifact extension. Different implementations may provide
 * different means to do it.
 * 
 * @author cstamas
 */
public interface ArtifactPackagingMapper
{
    /**
     * Returns the extension belonging to given packaging, like "jar" for "jar", "jar" for "ear", etc.
     * 
     * @param packaging
     * @return
     */
    String getExtensionForPackaging( String packaging );

    /**
     * Sets the file to source the user provided mappings from, and resets the mappings, forcing it to reload the file.
     * 
     * @param file
     */
    void setPropertiesFile( File file );
}
