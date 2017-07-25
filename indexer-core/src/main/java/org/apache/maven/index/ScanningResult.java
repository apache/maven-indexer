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

import java.util.ArrayList;
import java.util.List;

/**
 * A scanning result holds result of repository scan
 * 
 * @author Jason van Zyl
 */
public class ScanningResult
{
    private int totalFiles = 0;

    private int deletedFiles = 0;

    private List<Exception> exceptions = new ArrayList<>();

    private final ScanningRequest request;

    public ScanningResult( ScanningRequest request )
    {
        this.request = request;
    }

    public void setTotalFiles( int totalFiles )
    {
        this.totalFiles = totalFiles;
    }

    public void setDeletedFiles( int deletedFiles )
    {
        this.deletedFiles = deletedFiles;
    }

    public int getTotalFiles()
    {
        return totalFiles;
    }

    public int getDeletedFiles()
    {
        return deletedFiles;
    }

    public void addException( Exception e )
    {
        exceptions.add( e );
    }

    public boolean hasExceptions()
    {
        return exceptions.size() != 0;
    }

    public List<Exception> getExceptions()
    {
        return exceptions;
    }

    public ScanningRequest getRequest()
    {
        return request;
    }

}
