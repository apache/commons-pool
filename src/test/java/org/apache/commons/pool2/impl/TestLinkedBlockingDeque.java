/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.pool2.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link LinkedBlockingDeque}.
 */
public class TestLinkedBlockingDeque {

    private static final Integer ONE = Integer.valueOf(1);
    private static final Integer TWO = Integer.valueOf(2);
    private static final Integer THREE = Integer.valueOf(3);

    LinkedBlockingDeque<Integer> deque;

    @Before
    public void setUp() {
        deque = new LinkedBlockingDeque<Integer>(2);
    }

    @Test
    public void testConstructors() {
        LinkedBlockingDeque<Integer> deque = new LinkedBlockingDeque<Integer>();
        assertEquals(Integer.MAX_VALUE, deque.remainingCapacity());

        deque = new LinkedBlockingDeque<Integer>(2);
        assertEquals(2, deque.remainingCapacity());

        deque = new LinkedBlockingDeque<Integer>(Arrays.asList(ONE, TWO));
        assertEquals(2, deque.size());

        try {
            deque = new LinkedBlockingDeque<Integer>(Arrays.asList(ONE, null));
            fail("Not supposed to get here");
        } catch (NullPointerException npe) {
            // OK
        }
    }

    @Test
    public void testAddFirst() {
        deque.addFirst(ONE);
        deque.addFirst(TWO);
        assertEquals(2, deque.size());
        try {
            deque.addFirst(THREE);
            fail("Not supposed to get here");
        } catch (IllegalStateException e) {}
        assertEquals(Integer.valueOf(2), deque.pop());
    }

    @Test
    public void testAddLast() {
        deque.addLast(ONE);
        deque.addLast(TWO);
        assertEquals(2, deque.size());
        try {
            deque.addLast(THREE);
            fail("Not supposed to get here");
        } catch (IllegalStateException e) {}
        assertEquals(Integer.valueOf(1), deque.pop());
    }

    @Test
    public void testOfferFirst() {
        deque.offerFirst(ONE);
        deque.offerFirst(TWO);
        assertEquals(2, deque.size());
        try {
            deque.offerFirst(null);
            fail("Not supposed to get here");
        } catch (NullPointerException e) {}
        assertEquals(Integer.valueOf(2), deque.pop());
    }

    @Test
    public void testOfferLast() {
        deque.offerLast(ONE);
        deque.offerLast(TWO);
        assertEquals(2, deque.size());
        try {
            deque.offerLast(null);
            fail("Not supposed to get here");
        } catch (NullPointerException e) {}
        assertEquals(Integer.valueOf(1), deque.pop());
    }

    @Test
    public void testPutFirst() throws InterruptedException {
        try {
            deque.putFirst(null);
            fail("Not supposed to get here");
        } catch (NullPointerException e) {}
        deque.putFirst(ONE);
        deque.putFirst(TWO);
        assertEquals(2, deque.size());
        assertEquals(Integer.valueOf(2), deque.pop());
    }

    @Test
    public void testPutLast() throws InterruptedException {
        try {
            deque.putLast(null);
            fail("Not supposed to get here");
        } catch (NullPointerException e) {}
        deque.putLast(ONE);
        deque.putLast(TWO);
        assertEquals(2, deque.size());
        assertEquals(Integer.valueOf(1), deque.pop());
    }

    @Test
    public void testOfferFirstWithTimeout() throws InterruptedException {
        try {
            deque.offerFirst(null);
            fail("Not supposed to get here");
        } catch (NullPointerException e) {}
        assertTrue(deque.offerFirst(ONE, 50, TimeUnit.MILLISECONDS));
        assertTrue(deque.offerFirst(TWO, 50, TimeUnit.MILLISECONDS));
        assertFalse(deque.offerFirst(THREE, 50, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOfferLastWithTimeout() throws InterruptedException {
        try {
            deque.offerLast(null);
            fail("Not supposed to get here");
        } catch (NullPointerException e) {}
        assertTrue(deque.offerLast(ONE, 50, TimeUnit.MILLISECONDS));
        assertTrue(deque.offerLast(TWO, 50, TimeUnit.MILLISECONDS));
        assertFalse(deque.offerLast(THREE, 50, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testRemoveFirst() {
        try {
            deque.removeFirst();
            fail("Not supposed to get here");
        } catch (NoSuchElementException e) {}
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(1), deque.removeFirst());
        try {
            deque.removeFirst();
            deque.removeFirst();
            fail("Not supposed to get here");
        } catch (NoSuchElementException e) {}
    }

    @Test
    public void testRemoveLast() {
        try {
            deque.removeLast();
            fail("Not supposed to get here");
        } catch (NoSuchElementException e) {}
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(2), deque.removeLast());
        try {
            deque.removeLast();
            deque.removeLast();
            fail("Not supposed to get here");
        } catch (NoSuchElementException e) {}
    }

    @Test
    public void testPollFirst() {
        assertNull(deque.pollFirst());
        assertTrue(deque.offerFirst(ONE));
        assertTrue(deque.offerFirst(TWO));
        assertEquals(Integer.valueOf(2), deque.pollFirst());
    }

    @Test
    public void testPollLast() {
        assertNull(deque.pollLast());
        assertTrue(deque.offerFirst(ONE));
        assertTrue(deque.offerFirst(TWO));
        assertEquals(Integer.valueOf(1), deque.pollLast());
    }

    @Test
    public void testTakeFirst() throws InterruptedException {
        assertTrue(deque.offerFirst(ONE));
        assertTrue(deque.offerFirst(TWO));
        assertEquals(Integer.valueOf(2), deque.takeFirst());
    }

    @Test
    public void testTakeLast() throws InterruptedException {
        assertTrue(deque.offerFirst(ONE));
        assertTrue(deque.offerFirst(TWO));
        assertEquals(Integer.valueOf(1), deque.takeLast());
    }

    @Test
    public void testPollFirstWithTimeout() throws InterruptedException {
        assertNull(deque.pollFirst());
        assertNull(deque.pollFirst(50, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testPollLastWithTimeout() throws InterruptedException {
        assertNull(deque.pollLast());
        assertNull(deque.pollLast(50, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetFirst() {
        try {
            deque.getFirst();
            fail("Not supposed to get here");
        } catch (NoSuchElementException e){}
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(1), deque.getFirst());
    }

    @Test
    public void testGetLast() {
        try {
            deque.getLast();
            fail("Not supposed to get here");
        } catch (NoSuchElementException e){}
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(2), deque.getLast());
    }

    @Test
    public void testPeekFirst() {
        assertNull(deque.peekFirst());
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(1), deque.peekFirst());
    }

    @Test
    public void testPeekLast() {
        assertNull(deque.peekLast());
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(2), deque.peekLast());
    }

    @Test
    public void testRemoveLastOccurence() {
        assertFalse(deque.removeLastOccurrence(null));
        assertFalse(deque.removeLastOccurrence(ONE));
        deque.add(ONE);
        deque.add(ONE);
        assertTrue(deque.removeLastOccurrence(ONE));
        assertTrue(deque.size() == 1);
    }

    @Test
    public void testAdd() {
        assertTrue(deque.add(ONE));
        assertTrue(deque.add(TWO));
        try {
            assertTrue(deque.add(THREE));
            fail("Not supposed to get here");
        } catch (IllegalStateException e) {}
        try {
            assertTrue(deque.add(null));
            fail("Not supposed to get here");
        } catch (NullPointerException e) {}
    }

    @Test
    public void testOffer() {
        assertTrue(deque.offer(ONE));
        assertTrue(deque.offer(TWO));
        assertFalse(deque.offer(THREE));
        try {
            deque.offer(null);
            fail("Not supposed to get here");
        } catch (NullPointerException e) {}
    }

    @Test
    public void testPut() throws InterruptedException {
        try {
            deque.put(null);
            fail("Not supposed to get here");
        } catch (NullPointerException e) {}
        deque.put(ONE);
        deque.put(TWO);
    }

    @Test
    public void testOfferWithTimeout() throws InterruptedException {
        assertTrue(deque.offer(ONE, 50, TimeUnit.MILLISECONDS));
        assertTrue(deque.offer(TWO, 50, TimeUnit.MILLISECONDS));
        assertFalse(deque.offer(THREE, 50, TimeUnit.MILLISECONDS));
        try {
            deque.offer(null, 50, TimeUnit.MILLISECONDS);
            fail("Not supposed to get here");
        } catch (NullPointerException e) {}
    }

    @Test
    public void testRemove() {
        try {
            deque.remove();
            fail("Not supposed to get here");
        } catch (NoSuchElementException e) {}
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(1), deque.remove());
    }

    @Test
    public void testTake() throws InterruptedException {
        assertTrue(deque.offerFirst(ONE));
        assertTrue(deque.offerFirst(TWO));
        assertEquals(Integer.valueOf(2), deque.take());
    }

    @Test
    public void testPollWithTimeout() throws InterruptedException {
        assertNull(deque.poll(50, TimeUnit.MILLISECONDS));
        assertNull(deque.poll(50, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testElement() {
        try {
            deque.element();
            fail("Not supposed to get here");
        } catch (NoSuchElementException e){}
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(1), deque.element());
    }

    @Test
    public void testPeek() {
        assertNull(deque.peek());
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(1), deque.peek());
    }

    @Test
    public void testDrainTo() {
        Collection<Integer> c = new ArrayList<Integer>();
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(2, deque.drainTo(c));
        assertEquals(2, c.size());

        c = new ArrayList<Integer>();
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(1, deque.drainTo(c, 1));
        assertEquals(1, deque.size());
        assertEquals(1, c.size());
        assertEquals(Integer.valueOf(1), c.iterator().next());
    }

    @Test
    public void testPush() {
        deque.push(ONE);
        deque.push(TWO);
        assertEquals(2, deque.size());
        try {
            deque.push(THREE);
            fail("Not supposed to get here");
        } catch (IllegalStateException e) {}
        assertEquals(Integer.valueOf(2), deque.pop());
    }

    @Test
    public void testPop() {
        try {
            deque.pop();
            fail("Not supposed to get here");
        } catch (NoSuchElementException e) {}
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(1), deque.pop());
        try {
            deque.pop();
            deque.pop();
            fail("Not supposed to get here");
        } catch (NoSuchElementException e) {}
    }

    @Test
    public void testContains() {
        deque.add(ONE);
        assertTrue(deque.contains(ONE));
        assertFalse(deque.contains(TWO));
        assertFalse(deque.contains(null));
        deque.add(TWO);
        assertTrue(deque.contains(TWO));
        assertFalse(deque.contains(THREE));
    }

    @Test
    public void testToArray() {
        deque.add(ONE);
        deque.add(TWO);
        Object[] arr = deque.toArray();
        assertEquals(Integer.valueOf(1), arr[0]);
        assertEquals(Integer.valueOf(2), arr[1]);

        arr = deque.toArray(new Integer[0]);
        assertEquals(Integer.valueOf(1), arr[0]);
        assertEquals(Integer.valueOf(2), arr[1]);

        arr = deque.toArray(new Integer[deque.size()]);
        assertEquals(Integer.valueOf(1), arr[0]);
        assertEquals(Integer.valueOf(2), arr[1]);
    }

    @Test
    public void testClear() {
        deque.add(ONE);
        deque.add(TWO);
        deque.clear();
        deque.add(ONE);
        assertEquals(1, deque.size());
    }

    @Test
    public void testIterator() {
        try {
            deque.iterator().next();
            fail("Not supposed to get here");
        } catch (NoSuchElementException e) {}
        deque.add(ONE);
        deque.add(TWO);
        Iterator<Integer> iter = deque.iterator();
        assertEquals(Integer.valueOf(1), iter.next());
        iter.remove();
        assertEquals(Integer.valueOf(2), iter.next());
    }

    @Test
    public void testDescendingIterator() {
        try {
            deque.descendingIterator().next();
            fail("Not supposed to get here");
        } catch (NoSuchElementException e) {}
        deque.add(ONE);
        deque.add(TWO);
        Iterator<Integer> iter = deque.descendingIterator();
        assertEquals(Integer.valueOf(2), iter.next());
        iter.remove();
        assertEquals(Integer.valueOf(1), iter.next());
    }

}
