package org.apache.maven.index.updater;

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.index.updater.DefaultIndexUpdater;

/**
 * ResourceFetcher that keeps track of all requested remote resources.
 * 
 * @author igor
 */
public class TrackingFetcher
    extends DefaultIndexUpdater.FileFetcher
{

    private final ArrayList<String> resources = new ArrayList<String>();

    public TrackingFetcher( File basedir )
    {
        super( basedir );
    }

    @Override
    public InputStream retrieve( String name )
        throws IOException, FileNotFoundException
    {
        resources.add( name );
        return super.retrieve( name );
    }

    @Override
    public void retrieve( String name, File targetFile )
        throws IOException, FileNotFoundException
    {
        resources.add( name );
        super.retrieve( name, targetFile );
    }

    public List<String> getRetrievedResources()
    {
        return resources;
    }
}
