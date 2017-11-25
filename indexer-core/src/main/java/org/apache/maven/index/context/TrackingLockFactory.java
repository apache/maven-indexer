package org.apache.maven.index.context;

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
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.HashSet;

/**
 *
 * @author Tomas Zezula
 */
final class TrackingLockFactory
    extends LockFactory
{

    private final LockFactory delegate;

    private final Set<TrackingLock> emittedLocks;

    TrackingLockFactory( final LockFactory delegate )
    {
        this.delegate = checkNotNull( delegate );
        this.emittedLocks = Collections.newSetFromMap( new ConcurrentHashMap<TrackingLock, Boolean>() );
    }

    Set<? extends Lock> getEmittedLocks( String name )
    {
        final Set<Lock> result = new HashSet<>();
        for ( TrackingLock lock : emittedLocks )
        {
            if ( name == null || name.equals( lock.getName() ) )
            {
                result.add( lock );
            }
        }
        return result;
    }

    @Override
    public Lock obtainLock( Directory dir, String lockName )
        throws IOException
    {
        final TrackingLock lck = new TrackingLock( delegate.obtainLock( dir, lockName ), lockName );
        emittedLocks.add( lck );
        return lck;
    }

    private final class TrackingLock
        extends Lock
    {
        private final Lock delegate;

        private final String name;

        TrackingLock( final Lock delegate, final String name )
        {
            this.delegate = checkNotNull( delegate );
            this.name = checkNotNull( name );
        }

        String getName()
        {
            return name;
        }

        @Override
        public void close()
            throws IOException
        {
            try
            {
                delegate.close();
            }
            finally
            {
                emittedLocks.remove( this );
            }
        }

        @Override
        public void ensureValid()
            throws IOException
        {
            delegate.ensureValid();
        }
    }
}
