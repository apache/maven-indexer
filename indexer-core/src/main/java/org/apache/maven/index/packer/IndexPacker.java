package org.apache.maven.index.packer;

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

import java.io.IOException;

/**
 * An index packer used to create transfer index format that can be used by the
 * {@link org.apache.maven.index.updater.IndexUpdater}.
 * 
 * @author Tamas Cservenak
 */
public interface IndexPacker
{
    String ROLE = IndexPacker.class.getName();

    /**
     * Pack a context into a target directory. If the directory does not exists, it will be created. If the directory
     * exists, it should be writable.
     * 
     * @param request the request to process.
     * @throws IllegalArgumentException when the targetDir already exists and is not a writable directory.
     * @throws IOException on lethal IO problem
     */
    void packIndex( IndexPackingRequest request )
        throws IOException, IllegalArgumentException;

}
