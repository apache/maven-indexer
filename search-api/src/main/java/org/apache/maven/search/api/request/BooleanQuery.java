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

import static java.util.Objects.requireNonNull;

/**
 * Boolean query.
 */
public abstract class BooleanQuery extends Query {
    /**
     * The maximum nesting depth of a boolean query. Backends and {@link #toString()} walk the tree recursively; the
     * value matches the default Lucene boolean clause limit.
     */
    public static final int MAX_DEPTH = 1024;

    protected final Query left;

    protected final Query right;

    private final int depth;

    protected BooleanQuery(Query left, String op, Query right) {
        super(op);
        this.left = requireNonNull(left);
        this.right = requireNonNull(right);
        this.depth = 1 + Math.max(depthOf(left), depthOf(right));
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException(
                    "boolean query nesting depth " + depth + " exceeds the maximum of " + MAX_DEPTH);
        }
    }

    private static int depthOf(Query query) {
        return query instanceof BooleanQuery ? ((BooleanQuery) query).depth : 0;
    }

    /**
     * Returns left term of this boolean query, never {@code null}.
     */
    public Query getLeft() {
        return left;
    }

    /**
     * Returns right term of this boolean query, never {@code null}.
     */
    public Query getRight() {
        return right;
    }

    @Override
    public String toString() {
        return getLeft() + " " + getValue() + " " + getRight();
    }

    public static class And extends BooleanQuery {
        protected And(Query left, Query right) {
            super(left, "AND", right);
        }
    }

    /**
     * Creates Logical AND query (requires presence of all queries) out of passed in queries (at least 2 or more
     * should be given).
     */
    public static BooleanQuery and(Query left, Query... rights) {
        if (rights.length == 0) {
            throw new IllegalArgumentException("one or more on right needed");
        }
        BooleanQuery result = null;
        for (Query right : rights) {
            result = new And(result == null ? left : result, right);
        }
        return result;
    }
}
