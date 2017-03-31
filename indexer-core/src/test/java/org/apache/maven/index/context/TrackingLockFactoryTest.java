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
package org.apache.maven.index.context;

import java.io.IOException;
import java.util.Set;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tomas Zezula
 */
public class TrackingLockFactoryTest {

    public TrackingLockFactoryTest() {
    }

    @Test
    public void testLockUnlock() throws IOException {
        final TrackingLockFactory lf = new TrackingLockFactory(new SingleInstanceLockFactory());
        final RAMDirectory ram = new RAMDirectory(lf);
        final Lock foo = ram.obtainLock("foo");
        final Lock bar = ram.obtainLock("bar");
        bar.close();
        foo.close();
        ram.close();
    }

    @Test
    public void testLockLocked() throws IOException {
        final TrackingLockFactory lf = new TrackingLockFactory(new SingleInstanceLockFactory());
        final RAMDirectory ram = new RAMDirectory(lf);
        final Lock foo = ram.obtainLock("foo");
        boolean thrownLOFE = false;
        try {
            ram.obtainLock("foo");
        } catch (LockObtainFailedException e) {
            thrownLOFE = true;
        }
        assertTrue(thrownLOFE);
        foo.close();
        final Lock foo2 = ram.obtainLock("foo");
        foo2.close();
        ram.close();
    }

    @Test
    public void testEmmittedLocks() throws IOException {
        final TrackingLockFactory lf = new TrackingLockFactory(new SingleInstanceLockFactory());
        final RAMDirectory ram = new RAMDirectory(lf);
        final Lock l1 = ram.obtainLock("l1");
        final Lock l2 = ram.obtainLock("l2");
        final Lock l3 = ram.obtainLock("l3");
        l2.close();
        Set<? extends Lock> emittedLocks = lf.getEmittedLocks(null);
        assertEquals(2, emittedLocks.size());
        assertTrue(emittedLocks.contains(l1));
        assertTrue(emittedLocks.contains(l3));
        emittedLocks = lf.getEmittedLocks("l3");
        assertEquals(1, emittedLocks.size());
        assertTrue(emittedLocks.contains(l3));
    }
}
