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

import java.io.Serializable;
import java.util.regex.Pattern;

import org.apache.lucene.document.StoredField;

/**
 * Pulling out ArtifactInfo, clearing up. TBD. This gonna be extensible "map-like" class with fields.
 * 
 * @author cstamas
 */
public class ArtifactInfoRecord
    implements Serializable
{
    private static final long serialVersionUID = -4577081994768263824L;

    /** Field separator */
    public static final String FS = "|";

    public static final Pattern FS_PATTERN = Pattern.compile( Pattern.quote( FS ) );

    /** Non available value */
    public static final String NA = "NA";

    // ----------
    // V3 changes
    // TODO: use getters instead of public fields
    // ----------
    // Listing all the fields that ArtifactInfo has on LuceneIndex

    /**
     * Unique groupId, artifactId, version, classifier, extension (or packaging). Stored, indexed untokenized
     */
    public static final IndexerField FLD_UINFO = new IndexerField( NEXUS.UINFO, IndexerFieldVersion.V1, "u",
        "Artifact UINFO (as keyword, stored)", IndexerField.KEYWORD_STORED );

    /**
     * Del: contains UINFO to mark record as deleted (needed for incremental updates!). The original document IS
     * removed, but this marker stays on index to note that fact.
     */
    public static final IndexerField FLD_DELETED = new IndexerField( NEXUS.DELETED, IndexerFieldVersion.V1, "del",
        "Deleted field, will contain UINFO if document is deleted from index (not indexed, stored)", StoredField.TYPE );

}
