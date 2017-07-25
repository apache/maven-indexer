package org.apache.maven.index.util.zip;

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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * A very simplistic approach to hide the underlying mech to deal with ZipFiles, suited for use cases happening in Maven
 * Indexer.
 * 
 * @author cstamas
 */
public interface ZipHandle
    extends Closeable
{
    /**
     * Returns true if Zip file this handle is pointing to contains an entry at given path.
     * 
     * @param path
     * @return
     */
    boolean hasEntry( String path )
        throws IOException;

    /**
     * Returns a list of string, with each string representing a valid path for existing entry in this Zip handle.
     * 
     * @return
     */
    List<String> getEntries();

    /**
     * Returns a list of string, with each string representing a valid path for existing entry in this Zip handle.
     * 
     * @return
     */
    List<String> getEntries( EntryNameFilter filter );

    /**
     * Returns the "payload" (uncompressed) of the entry at given path, or null if no such path exists in the Zip file
     * this handle points to.
     * 
     * @param path
     * @return
     */
    InputStream getEntryContent( String path )
        throws IOException;

    /**
     * Closes the zip handle (performs resource cleanup). This method should be called when this zip handle is not
     * needed anymore, and calling it should be obligatory to prevent resource leaks.
     */
    void close()
        throws IOException;
}
