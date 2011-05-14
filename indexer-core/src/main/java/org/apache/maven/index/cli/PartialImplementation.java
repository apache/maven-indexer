package org.apache.maven.index.cli;

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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * An {@link java.lang.reflect.InvocationHandler} that can be extended with methods from the proxied interface. While
 * invocation it will look for a method within itself that matches the signature of the invoked proxy method. If found
 * the method will be invoked and result returned, otherwise an {@link UnsupportedOperationException} will be thrown.
 * 
 * @author Alin Dreghiciu
 */
public class PartialImplementation
    implements InvocationHandler
{

    public Object invoke( final Object proxy, final Method method, final Object[] args )
        throws Throwable
    {
        try
        {
            final Method localMethod = getClass().getMethod( method.getName(), method.getParameterTypes() );
            return localMethod.invoke( this, args );
        }
        catch ( NoSuchMethodException e )
        {
            throw new UnsupportedOperationException( "Method " + method.getName() + "() is not supported" );
        }
        catch ( IllegalAccessException e )
        {
            throw new UnsupportedOperationException( "Method " + method.getName() + "() is not supported" );
        }
        catch ( InvocationTargetException e )
        {
            throw e.getCause();
        }
    }

}
