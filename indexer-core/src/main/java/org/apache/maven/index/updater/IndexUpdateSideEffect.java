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

import org.apache.lucene.store.Directory;
import org.apache.maven.index.context.IndexingContext;

/**
 * Ability to spread index updates to (possible) plugin receivers. (NEXUS-2644) Implement this as Plexus component to
 * add new behaviour.
 * 
 * @author Toni Menzel
 */
public interface IndexUpdateSideEffect
{

    /**
     * Given a full or partial (see context partial parameter) lucene index (directory + context it has been integrated
     * into), this can let other participants (implementations of this type) know about the update. Any activity should
     * not influence the callers further process (not fail via unchecked exception) if possible. Implementations are
     * most likely optional plugins.
     * 
     * @param directory - the directory to merge
     * @param context - original context
     * @param partial - this update is partial (true) or a full update (false).
     */
    void updateIndex( Directory directory, IndexingContext context, boolean partial );

}
