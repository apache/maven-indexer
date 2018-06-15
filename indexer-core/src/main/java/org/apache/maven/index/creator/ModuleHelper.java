package org.apache.maven.index.creator;

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

import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

class ModuleHelper
{
    Optional<ModuleDescriptor> describeModule( Path path, boolean reportFileNameBasedModuleAsEmpty )
    {
        try
        {
            Set<ModuleReference> allModules = ModuleFinder.of( path ).findAll();
            if ( allModules.size() != 1 )
            {
                throw new IllegalStateException( "expected to find single module, but got: " + allModules );
            }
            ModuleReference reference = allModules.iterator().next();
            ModuleDescriptor descriptor = reference.descriptor();
            // TODO info("describeModule({0} -> {1})", path, descriptor);
            if ( reportFileNameBasedModuleAsEmpty )
            {
                if ( descriptor.isAutomatic() )
                {
                    if ( !isAutomaticModuleNameAttributeAvailable( reference ) )
                    {
                        return Optional.empty();
                    }
                }
            }
            return Optional.of( descriptor );
        }
        catch ( FindException e )
        {
            // TODO debug("finding module(s) failed: {0}", e);
            return Optional.empty();
        }
    }

    private boolean isAutomaticModuleNameAttributeAvailable( ModuleReference moduleReference )
    {
        try ( ModuleReader moduleReader = moduleReference.open() )
        {
            String manifestString =
                            moduleReader
                                            .read( "META-INF/MANIFEST.MF" )
                                            .map( StandardCharsets.UTF_8::decode )
                                            .map( Object::toString )
                                            .orElse( "" );
            if ( manifestString.contains( "Automatic-Module-Name" ) )
            {
                return true;
            }
        }
        catch ( Exception e )
        {
            // TODO debug("reading manifest failed: {0}", e);
        }
        return false;
    }
}
