/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.apache.commons.pool.impl;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestCursorableLinkedList {

//    @Test
//    public void testHashCode() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testAddT() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testAddIntT() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testAddAllCollectionOfQextendsT() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testAddAllIntCollectionOfQextendsT() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testAddFirst() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testAddLast() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testClear() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testContains() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testContainsAll() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testCursor() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testCursorInt() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testEqualsObject() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testGet() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testGetFirst() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testGetLast() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testIndexOf() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testIsEmpty() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testIterator() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testLastIndexOf() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testListIterator() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testListIteratorInt() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testRemoveObject() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testRemoveInt() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testRemoveAll() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testRemoveFirst() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testRemoveLast() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testRetainAll() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testSet() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testSize() {
//        // fail("Not yet implemented");
//    }

    @Test
    public void testToArray() {
        CursorableLinkedList<Integer> cll = new CursorableLinkedList<Integer>();
        cll.add(Integer.valueOf(1));
        cll.add(Integer.valueOf(2));
        Object[] oa;
        oa = cll.toArray();
        assertEquals(cll.size(),oa.length);
        assertNotNull(oa[0]);
        assertEquals("java.lang.Integer",oa[0].getClass().getCanonicalName());
    }

    @Test
    public void testToArrayEArray() {
        CursorableLinkedList<Integer> cll = new CursorableLinkedList<Integer>();
        cll.add(Integer.valueOf(1));
        cll.add(Integer.valueOf(2));
        Integer[] ia;
        ia = cll.toArray(new Integer[0]);
        assertEquals(cll.size(),ia.length);
        ia = cll.toArray(new Integer[10]);
        assertEquals(10,ia.length);
        assertNotNull(ia[0]);
        assertNull(ia[cll.size()]);
        try {
            cll.toArray(new String[0]);
            fail("Should have generated ArrayStoreException");
        } catch (ArrayStoreException expected){
            // expected
        }
        cll.toArray(new Number[0]);
        try {
            cll.toArray(null);
            fail("Should have generated NullPointerException");
        } catch (NullPointerException expected){
            // expected
        }
    }

//    @Test
//    public void testToString() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testSubList() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testInsertListable() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testRemoveListable() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testGetListableAt() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testRegisterCursor() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testUnregisterCursor() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testInvalidateCursors() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testBroadcastListableChanged() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testBroadcastListableRemoved() {
//        // fail("Not yet implemented");
//    }
//
//    @Test
//    public void testBroadcastListableInserted() {
//        // fail("Not yet implemented");
//    }

}
