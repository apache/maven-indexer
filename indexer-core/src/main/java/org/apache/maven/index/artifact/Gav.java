package org.apache.maven.index.artifact;

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

import java.util.Objects;

/**
 * An immutable value class representing unique artifact coordinates.
 * 
 * @author cstamas
 * @author jvanzyl
 */
public class Gav
{
    /**
     * Enumeration representing Maven artifact hash types
     */
    public enum HashType
    {
        sha1, md5
    }

    /**
     * Enumeration representing Maven artifact signature types
     */
    public enum SignatureType
    {
        gpg;

        @Override
        public String toString()
        {
            if ( this == SignatureType.gpg )
            {
                return "asc";
            }
            return "unknown-signature-type";
        }
    }

    private final String groupId;

    private final String artifactId;

    private final String version;

    private final String baseVersion;

    private final String classifier;

    private final String extension;

    private final Integer snapshotBuildNumber;

    private final Long snapshotTimeStamp;

    private final String name;

    private final boolean snapshot;

    private final boolean hash;

    private final HashType hashType;

    private final boolean signature;

    private final SignatureType signatureType;

    public Gav( String groupId, String artifactId, String version )
    {
        this( groupId, artifactId, version, null, null, null, null, null, false, null, false, null );
    }

    public Gav( String groupId, String artifactId, String version, String classifier, String extension,
                Integer snapshotBuildNumber, Long snapshotTimeStamp, String name, boolean hash, HashType hashType,
                boolean signature, SignatureType signatureType )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.snapshot = VersionUtils.isSnapshot( version );

        if ( !snapshot )
        {
            this.baseVersion = null;
        }
        else
        {
            if ( version.contains( "SNAPSHOT" ) )
            {
                // this is not a timestamped version
                this.baseVersion = null;
            }
            else
            {
                // this is a timestamped version (verified against pattern, see above)
                // we have XXXXXX-YYYYMMDD.HHMMSS-B
                // but XXXXXX may contain "-" too!

                // if ( new DefaultNexusEnforcer().isStrict() )
                // {
                // this.baseVersion = version.substring( 0, version.lastIndexOf( '-' ) );
                // this.baseVersion = baseVersion.substring( 0, baseVersion.lastIndexOf( '-' ) ) + "-SNAPSHOT";
                // }
                // also there may be no XXXXXX (i.e. when version is strictly named SNAPSHOT
                // BUT this is not the proper scheme, we will simply loosen up here if requested
                // else
                // {
                // trim the part of 'YYYYMMDD.HHMMSS-BN
                String tempBaseVersion = version.substring( 0, version.lastIndexOf( '-' ) );
                tempBaseVersion = tempBaseVersion.substring( 0, tempBaseVersion.length() - 15 );

                if ( tempBaseVersion.length() > 0 )
                {
                    this.baseVersion = tempBaseVersion + "SNAPSHOT";
                }
                else
                {
                    this.baseVersion = "SNAPSHOT";
                }
                // }
            }
        }

        this.classifier = classifier;
        this.extension = extension;
        this.snapshotBuildNumber = snapshotBuildNumber;
        this.snapshotTimeStamp = snapshotTimeStamp;
        this.name = name;
        this.hash = hash;
        this.hashType = hashType;
        this.signature = signature;
        this.signatureType = signatureType;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public String getBaseVersion()
    {
        if ( baseVersion == null )
        {
            return getVersion();
        }
        else
        {
            return baseVersion;
        }
    }

    public String getClassifier()
    {
        return classifier;
    }

    public String getExtension()
    {
        return extension;
    }

    public String getName()
    {
        return name;
    }

    public boolean isSnapshot()
    {
        return snapshot;
    }

    public Integer getSnapshotBuildNumber()
    {
        return snapshotBuildNumber;
    }

    public Long getSnapshotTimeStamp()
    {
        return snapshotTimeStamp;
    }

    public boolean isHash()
    {
        return hash;
    }

    public HashType getHashType()
    {
        return hashType;
    }

    public boolean isSignature()
    {
        return signature;
    }

    public SignatureType getSignatureType()
    {
        return signatureType;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        Gav gav = (Gav) o;
        return snapshot == gav.snapshot && hash == gav.hash && signature == gav.signature && Objects.equals( groupId,
                gav.groupId ) && Objects.equals( artifactId, gav.artifactId ) && Objects.equals( version,
                gav.version ) && Objects.equals( baseVersion, gav.baseVersion ) && Objects.equals( classifier,
                gav.classifier ) && Objects.equals( extension, gav.extension ) && Objects.equals( snapshotBuildNumber,
                gav.snapshotBuildNumber ) && Objects.equals( snapshotTimeStamp,
                gav.snapshotTimeStamp ) && Objects.equals( name,
                gav.name ) && hashType == gav.hashType && signatureType == gav.signatureType;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( groupId, artifactId, version, baseVersion, classifier, extension, snapshotBuildNumber,
                snapshotTimeStamp, name, snapshot, hash, hashType, signature, signatureType );
    }
}
