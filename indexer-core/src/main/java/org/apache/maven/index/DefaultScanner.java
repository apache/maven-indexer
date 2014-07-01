package org.apache.maven.index;

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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.index.context.IndexingContext;

/**
 * A default repository scanner for Maven 2 repository.
 * 
 * @author Jason Van Zyl
 * @author Tamas Cservenak
 */
@Singleton
@Named
public class DefaultScanner
    implements Scanner
{

    private final ArtifactContextProducer artifactContextProducer;


    @Inject
    public DefaultScanner( ArtifactContextProducer artifactContextProducer )
    {
        this.artifactContextProducer = artifactContextProducer;
    }

    public ScanningResult scan( ScanningRequest request )
    {
        request.getArtifactScanningListener().scanningStarted( request.getIndexingContext() );

        ScanningResult result = new ScanningResult( request );

        scanDirectory( request.getStartingDirectory(), request );

        request.getArtifactScanningListener().scanningFinished( request.getIndexingContext(), result );

        return result;
    }

    private void scanDirectory( File dir, ScanningRequest request )
    {
        if ( dir == null )
        {
            return;
        }

        File[] fileArray = dir.listFiles();

        if ( fileArray != null )
        {
            Set<File> files = new TreeSet<File>( new ScannerFileComparator() );

            files.addAll( Arrays.asList( fileArray ) );

            for ( File f : files )
            {
                if ( f.getName().startsWith( "." ) )
                {
                    continue; // skip all hidden files and directories
                }

                if ( f.isDirectory() )
                {
                    scanDirectory( f, request );
                }
                // else if ( !AbstractIndexCreator.isIndexable( f ) )
                // {
                // continue; // skip non-indexable files
                // }
                else
                {
                    processFile( f, request );
                }
            }
        }
    }

    private void processFile( File file, ScanningRequest request )
    {
        IndexingContext context = request.getIndexingContext();

        ArtifactContext ac = artifactContextProducer.getArtifactContext( context, file );

        if ( ac != null )
        {
            request.getArtifactScanningListener().artifactDiscovered( ac );
        }
    }

    // ==

    /**
     * A special comparator to overcome some very bad limitations of nexus-indexer during scanning: using this
     * comparator, we force to "discover" POMs last, before the actual artifact file. The reason for this, is to
     * guarantee that scanner will provide only "best" informations 1st about same artifact, since the POM->artifact
     * direction of discovery is not trivial at all (pom read -> packaging -> extension -> artifact file). The artifact
     * -> POM direction is trivial.
     */
    private static class ScannerFileComparator
        implements Comparator<File>
    {
        public int compare( File o1, File o2 )
        {
            if ( o1.getName().endsWith( ".pom" ) && !o2.getName().endsWith( ".pom" ) )
            {
                // 1st is pom, 2nd is not
                return 1;
            }
            else if ( !o1.getName().endsWith( ".pom" ) && o2.getName().endsWith( ".pom" ) )
            {
                // 2nd is pom, 1st is not
                return -1;
            }
            else
            {
                // both are "same" (pom or not pom)
                // Use reverse order so that timestamped snapshots
                // use latest - not first
                return o2.getName().compareTo( o1.getName() );

            }
        }
    }
}
