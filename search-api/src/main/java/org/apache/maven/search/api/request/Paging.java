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
package org.apache.maven.search.api.request;

/**
 * Paging.
 */
public final class Paging {
    private final int pageSize;

    private final int pageOffset;

    /**
     * Creates paging instance with given page size (must be greater than 0) and page offset (must be non-negative).
     */
    public Paging(int pageSize, int pageOffset) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize");
        }
        if (pageOffset < 0) {
            throw new IllegalArgumentException("pageOffset");
        }
        this.pageSize = pageSize;
        this.pageOffset = pageOffset;
    }

    /**
     * Creates paging instance with given page size (must be grater than 0) and 0 page offset.
     */
    public Paging(int pageSize) {
        this(pageSize, 0);
    }

    /**
     * Returns the page size: positive integer, never zero or less.
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Returns the page offset: a zero or a positive integer.
     */
    public int getPageOffset() {
        return pageOffset;
    }

    /**
     * Creates "next page" instance relative to this instance.
     */
    public Paging nextPage() {
        return new Paging(pageSize, pageOffset + 1);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{pageSize=" + pageSize + ", pageOffset=" + pageOffset + "}";
    }
}
