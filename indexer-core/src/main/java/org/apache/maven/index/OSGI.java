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
 * OSGI ontology.
 *
 * @author Olivier Lamy
 * @since 4.1.1
 */
public interface OSGI
{

    /**
     * OSGI namespace
     */
    String OSGI_NAMESPACE = "urn:osgi#";

    Field SYMBOLIC_NAME = new Field( null, OSGI_NAMESPACE, "symbolicName", "Bundle Symbolic Name" );

    Field VERSION = new Field( null, OSGI_NAMESPACE, "version", "Bundle Version" );

    Field EXPORT_PACKAGE = new Field( null, OSGI_NAMESPACE, "exportPackage", "Bundle Export-Package" );

    /**
     * Export-Service has been deprecated since OSGI R4 (2005), and was never used by resolvers.  It is replaced by
     * PROVIDE_CAPABILITY
     *
     * @deprecated
     */
    @Deprecated
    Field EXPORT_SERVICE = new Field( null, OSGI_NAMESPACE, "exportService", "Bundle Export-Service" );

    Field DESCRIPTION = new Field( null, OSGI_NAMESPACE, "bundleDescription", "Bundle-Description" );

    Field NAME = new Field( null, OSGI_NAMESPACE, "bundleName", "Bundle-Name" );

    Field LICENSE = new Field( null, OSGI_NAMESPACE, "bundleLicense", "Bundle-License" );

    Field DOCURL = new Field( null, OSGI_NAMESPACE, "bundleDocUrl", "Bundle-DocURL" );

    Field IMPORT_PACKAGE = new Field( null, OSGI_NAMESPACE, "importPackage", "Import-Package" );

    Field REQUIRE_BUNDLE  = new Field( null, OSGI_NAMESPACE, "requireBundle", "Require-Bundle" );

    /**
     * used by OSGI resolvers to determine which bundles / artifacts / environments, etc. can satisfy a given
     * requirement. It replaces headers like Export-Service and Required Execution Environment, and uses the default
     * OSGI header format
     *
     * @since 5.1.2
     */
    Field PROVIDE_CAPABILITY =
        new Field( null, OSGI_NAMESPACE, "provideCapability", "Bundle Provide-Capability" );

    /**
     * used by OSGI resolvers to indicate which services, features, etc are required by a given   .
     * It replaces headers like Import-Service, and uses the default OSGI header format.
     *
     * @since 5.1.2
     */
    Field REQUIRE_CAPABILITY =
        new Field( null, OSGI_NAMESPACE, "requireCapability", "Bundle Require-Capability" );

    /**
     * used to hold the SHA256 checksum required as identifier for OSGI Content resources.
     *
     * @since 5.1.2
     */
    Field SHA256 = new Field( null, OSGI_NAMESPACE, "sha256", "SHA-256 checksum" );

    /**
     * used to hold the Fragment Host header  for an OSGI Fragment bundle.
     *
     * @since 5.1.2
     */
    Field FRAGMENT_HOST = new Field( null, OSGI_NAMESPACE, "fragmentHost", "Bundle Fragment-Host" );

    /**
     * used to hold the Fragment Host header  for an OSGI Fragment bundle.
     *
     * @since 5.1.2
     */
    Field BUNDLE_REQUIRED_EXECUTION_ENVIRONMENT =
        new Field( null, OSGI_NAMESPACE, "bree", "Bundle Required Execution Environment" );

}
