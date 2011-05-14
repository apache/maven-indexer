package org.apache.maven.index.fs;

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
import java.io.IOException;

/**
 * Filesystem locker. Can be used to synchronize access to filesystem directories from different operating system
 * processes.
 * 
 * @author igor
 */
public interface Locker
{
    String LOCK_FILE = ".lock";

    /**
     * Acquires exclusive lock on specified directory. Most implementation will use marker file and will only work if
     * all processes that require access to the directory use the same filename.
     */
    Lock lock( File directory )
        throws IOException;
}
