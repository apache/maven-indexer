package org.apache.maven.index.locator;

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
 * An sha1 locator to locate the sha1 file relative to POM.
 * 
 * @author Jason van Zyl
 */
public class Sha1Locator
    implements Locator
{
    public File locate( File source )
    {
        // return new File( source.getParentFile(), source.getName() + ".sha1" );
        return new File( source.getAbsolutePath() + ".sha1" );
    }
}
