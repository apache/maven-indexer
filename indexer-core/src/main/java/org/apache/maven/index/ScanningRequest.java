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

import java.io.File;

import org.apache.maven.index.context.IndexingContext;
import org.codehaus.plexus.util.StringUtils;

/**
 * A scanning request provides various input parameters for repository scan
 *
 * @author Jason van Zyl
 */
public class ScanningRequest {
    private final IndexingContext context;

    private final ArtifactScanningListener artifactScanningListener;

    private final String startingPath;

    public ScanningRequest(final IndexingContext context, final ArtifactScanningListener artifactScanningListener) {
        this(context, artifactScanningListener, null);
    }

    public ScanningRequest(
            final IndexingContext context,
            final ArtifactScanningListener artifactScanningListener,
            final String startingPath) {
        this.context = context;
        this.artifactScanningListener = artifactScanningListener;
        this.startingPath = startingPath;
    }

    public IndexingContext getIndexingContext() {
        return context;
    }

    public ArtifactScanningListener getArtifactScanningListener() {
        return artifactScanningListener;
    }

    public String getStartingPath() {
        return startingPath;
    }

    public File getStartingDirectory() {
        if (StringUtils.isBlank(startingPath)) {
            return getIndexingContext().getRepository();
        } else {
            return new File(getIndexingContext().getRepository(), startingPath);
        }
    }
}
