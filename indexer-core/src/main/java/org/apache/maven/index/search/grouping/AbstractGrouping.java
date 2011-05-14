package org.apache.maven.index.search.grouping;

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

import java.util.Comparator;
import java.util.Map;

import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactInfoGroup;
import org.apache.maven.index.Grouping;

/**
 * An abstract {@link Grouping} implementation.
 * 
 * @author Tamas Cservenak
 */
public abstract class AbstractGrouping
    implements Grouping
{
    private Comparator<ArtifactInfo> comparator;

    public AbstractGrouping()
    {
        this( ArtifactInfo.VERSION_COMPARATOR );
    }

    public AbstractGrouping( Comparator<ArtifactInfo> comparator )
    {
        super();
        this.comparator = comparator;
    }

    public boolean addArtifactInfo( Map<String, ArtifactInfoGroup> result, ArtifactInfo artifactInfo )
    {
        String key = getGroupKey( artifactInfo );

        ArtifactInfoGroup group = result.get( key );

        if ( group == null )
        {
            group = new ArtifactInfoGroup( key, comparator );

            result.put( key, group );
        }

        return group.addArtifactInfo( artifactInfo );
    }

    protected abstract String getGroupKey( ArtifactInfo artifactInfo );

}
