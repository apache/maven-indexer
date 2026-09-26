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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BooleanQueryTest {
    private static Query[] terms(int count) {
        Query[] terms = new Query[count];
        for (int i = 0; i < count; i++) {
            terms[i] = Query.query("t" + i);
        }
        return terms;
    }

    @Test
    void simpleAnd() {
        BooleanQuery query = BooleanQuery.and(Query.query("left"), Query.query("right"));
        assertEquals("left AND right", query.toString());
    }

    @Test
    void chainAtMaxDepthIsAcceptedAndConsumable() {
        BooleanQuery query = BooleanQuery.and(Query.query("left"), terms(BooleanQuery.MAX_DEPTH));
        // recursive consumption of a maximal legal chain must not overflow the stack
        assertNotNull(query.toString());
    }

    @Test
    void chainBeyondMaxDepthIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> BooleanQuery.and(Query.query("left"), terms(BooleanQuery.MAX_DEPTH + 1)));
    }

    @Test
    void nestedConstructionIsRejectedAtTheLimitToo() {
        Query deep = BooleanQuery.and(Query.query("left"), terms(BooleanQuery.MAX_DEPTH));
        // one more level on top of a maximal chain must fail, wherever it is nested
        assertThrows(IllegalArgumentException.class, () -> BooleanQuery.and(deep, Query.query("right")));
    }
}
