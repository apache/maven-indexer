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

/**
 * Designated search types that NexusIndexer supports.
 * 
 * @author cstamas
 */
public enum SearchType
{
    /**
     * Scored search types are usually meant for human query input, where loose-matching and result ranking is
     * happening. The order of index hits is important, since it will reflect the hits ordered by score (1st hit, best
     * match, last hit worst).
     */
    SCORED,

    /**
     * Exact search types are usually meant for applications filtering index content for some exact filtering condition
     * even in a "future proof" way (example with packaging "maven-archetype" vs "foo-archetype-maven").
     */
    EXACT;

    public boolean matchesIndexerField( IndexerField field )
    {
        switch ( this )
        {
            case SCORED:
                return !field.isKeyword();

            case EXACT:
                return field.isKeyword();

            default:
                return false;
        }
    }
}
