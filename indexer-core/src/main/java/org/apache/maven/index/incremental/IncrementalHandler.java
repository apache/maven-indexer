package org.apache.maven.index.incremental;

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

import org.apache.maven.index.packer.IndexPackingRequest;
import org.apache.maven.index.updater.IndexUpdateRequest;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

public interface IncrementalHandler
{
    List<Integer> getIncrementalUpdates( IndexPackingRequest request, Properties properties )
        throws IOException;

    List<String> loadRemoteIncrementalUpdates( IndexUpdateRequest request, Properties localProperties,
                                               Properties remoteProperties )
        throws IOException;

    void initializeProperties( Properties properties );
}
