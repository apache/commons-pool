/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.pool2.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for {@link LinkedBlockingDeque}.
 */
class TestLinkedBlockingDeque {

    private static final Duration TIMEOUT_50_MILLIS = Duration.ofMillis(50);
    private static final Integer ONE = Integer.valueOf(1);
    private static final Integer TWO = Integer.valueOf(2);
    private static final Integer THREE = Integer.valueOf(3);

    LinkedBlockingDeque<Integer> deque;

    @BeforeEach
    public void setUp() {
        deque = new LinkedBlockingDeque<>(2);
    }

    @Test
    void testAdd() {
        assertTrue(deque.add(ONE));
        assertTrue(deque.add(TWO));
        assertThrows(IllegalStateException.class, () -> deque.add(THREE));
        assertThrows(NullPointerException.class, () -> deque.add(null));
    }

    @Test
    void testAddFirst() {
        deque.addFirst(ONE);
        deque.addFirst(TWO);
        assertEquals(2, deque.size());
        assertThrows(IllegalStateException.class, () -> deque.add(THREE));
        assertEquals(Integer.valueOf(2), deque.pop());
    }

    @Test
    void testAddLast() {
        deque.addLast(ONE);
        deque.addLast(TWO);
        assertEquals(2, deque.size());
        assertThrows(IllegalStateException.class, () -> deque.add(THREE));
        assertEquals(Integer.valueOf(1), deque.pop());
    }

    @Test
    void testClear() {
        deque.add(ONE);
        deque.add(TWO);
        deque.clear();
        deque.add(ONE);
        assertEquals(1, deque.size());
    }

    @Test
    void testConstructors() {
        LinkedBlockingDeque<Integer> deque = new LinkedBlockingDeque<>();
        assertEquals(Integer.MAX_VALUE, deque.remainingCapacity());

        deque = new LinkedBlockingDeque<>(2);
        assertEquals(2, deque.remainingCapacity());

        deque = new LinkedBlockingDeque<>(Arrays.asList(ONE, TWO));
        assertEquals(2, deque.size());

        assertThrows(NullPointerException.class, () -> new LinkedBlockingDeque<>(Arrays.asList(ONE, null)));
    }

    @Test
    void testContains() {
        deque.add(ONE);
        assertTrue(deque.contains(ONE));
        assertFalse(deque.contains(TWO));
        assertFalse(deque.contains(null));
        deque.add(TWO);
        assertTrue(deque.contains(TWO));
        assertFalse(deque.contains(THREE));
    }

    @Test
    void testDescendingIterator() {
        assertThrows(NoSuchElementException.class, () -> deque.descendingIterator().next());
        deque.add(ONE);
        deque.add(TWO);
        final Iterator<Integer> iter = deque.descendingIterator();
        assertEquals(Integer.valueOf(2), iter.next());
        iter.remove();
        assertEquals(Integer.valueOf(1), iter.next());
    }

    @Test
    void testDrainTo() {
        Collection<Integer> c = new ArrayList<>();
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(2, deque.drainTo(c));
        assertEquals(2, c.size());

        c = new ArrayList<>();
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(1, deque.drainTo(c, 1));
        assertEquals(1, deque.size());
        assertEquals(1, c.size());
        assertEquals(Integer.valueOf(1), c.iterator().next());
    }

    @Test
    void testElement() {
        assertThrows(NoSuchElementException.class, () -> deque.element());
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(1), deque.element());
    }

    @Test
    void testGetFirst() {
        assertThrows(NoSuchElementException.class, () -> deque.getFirst());
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(1), deque.getFirst());
    }

    @Test
    void testGetLast() {
        assertThrows(NoSuchElementException.class, () -> deque.getLast());
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(2), deque.getLast());
    }

    @Test
    void testIterator() {
        assertThrows(NoSuchElementException.class, () -> deque.iterator().next());
        deque.add(ONE);
        deque.add(TWO);
        final Iterator<Integer> iter = deque.iterator();
        assertEquals(Integer.valueOf(1), iter.next());
        iter.remove();
        assertEquals(Integer.valueOf(2), iter.next());
    }

    @Test
    void testOffer() {
        assertTrue(deque.offer(ONE));
        assertTrue(deque.offer(TWO));
        assertFalse(deque.offer(THREE));
        assertThrows(NullPointerException.class, () -> deque.offer(null));
    }

    @Test
    void testOfferFirst() {
        deque.offerFirst(ONE);
        deque.offerFirst(TWO);
        assertEquals(2, deque.size());
        assertThrows(NullPointerException.class, () -> deque.offerFirst(null));
        assertEquals(Integer.valueOf(2), deque.pop());
    }

    @Test
    void testOfferFirstWithTimeout() throws InterruptedException {
        assertThrows(NullPointerException.class, () -> deque.offerFirst(null, TIMEOUT_50_MILLIS));
        assertTrue(deque.offerFirst(ONE, TIMEOUT_50_MILLIS));
        assertTrue(deque.offerFirst(TWO, TIMEOUT_50_MILLIS));
        assertFalse(deque.offerFirst(THREE, TIMEOUT_50_MILLIS));
    }

    @Test
    void testOfferLast() {
        deque.offerLast(ONE);
        deque.offerLast(TWO);
        assertEquals(2, deque.size());
        assertThrows(NullPointerException.class, () -> deque.offerLast(null));
        assertEquals(Integer.valueOf(1), deque.pop());
    }

    @Test
    void testOfferLastWithTimeout() throws InterruptedException {
        assertThrows(NullPointerException.class, () -> deque.offerLast(null, TIMEOUT_50_MILLIS));
        assertTrue(deque.offerLast(ONE, TIMEOUT_50_MILLIS));
        assertTrue(deque.offerLast(TWO, TIMEOUT_50_MILLIS));
        assertFalse(deque.offerLast(THREE, TIMEOUT_50_MILLIS));
    }

    @Test
    void testOfferWithTimeout() throws InterruptedException {
        assertTrue(deque.offer(ONE, TIMEOUT_50_MILLIS));
        assertTrue(deque.offer(TWO, TIMEOUT_50_MILLIS));
        assertFalse(deque.offer(THREE, TIMEOUT_50_MILLIS));
        assertThrows(NullPointerException.class, () -> deque.offer(null, TIMEOUT_50_MILLIS));
    }

    @Test
    void testPeek() {
        assertNull(deque.peek());
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(1), deque.peek());
    }

    @Test
    void testPeekFirst() {
        assertNull(deque.peekFirst());
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(1), deque.peekFirst());
    }

    @Test
    void testPeekLast() {
        assertNull(deque.peekLast());
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(2), deque.peekLast());
    }

    @Test
    void testPollFirst() {
        assertNull(deque.pollFirst());
        assertTrue(deque.offerFirst(ONE));
        assertTrue(deque.offerFirst(TWO));
        assertEquals(Integer.valueOf(2), deque.pollFirst());
    }

    @Test
    void testPollFirstWithTimeout() throws InterruptedException {
        assertNull(deque.pollFirst());
        assertNull(deque.pollFirst(TIMEOUT_50_MILLIS));
    }

    @Test
    void testPollLast() {
        assertNull(deque.pollLast());
        assertTrue(deque.offerFirst(ONE));
        assertTrue(deque.offerFirst(TWO));
        assertEquals(Integer.valueOf(1), deque.pollLast());
    }

    @Test
    void testPollLastWithTimeout() throws InterruptedException {
        assertNull(deque.pollLast());
        assertNull(deque.pollLast(TIMEOUT_50_MILLIS));
    }

    @Test
    void testPollWithTimeout() throws InterruptedException {
        assertNull(deque.poll(TIMEOUT_50_MILLIS));
        assertNull(deque.poll(TIMEOUT_50_MILLIS));
    }

    @Test
    void testPop() {
        assertThrows(NoSuchElementException.class, () -> deque.pop());
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(1), deque.pop());
        assertThrows(NoSuchElementException.class, () -> {
            deque.pop();
            deque.pop();
        });
    }

    /*
     * https://issues.apache.org/jira/browse/POOL-281
     *
     * Should complete almost instantly when the issue is fixed.
     */
    @Test
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    void testPossibleBug() {

        deque = new LinkedBlockingDeque<>();
        for (int i = 0; i < 3; i++) {
            deque.add(Integer.valueOf(i));
        }

        // This particular sequence of method calls() (there may be others)
        // creates an internal state that triggers an infinite loop in the
        // iterator.
        final Iterator<Integer> iter = deque.iterator();
        iter.next();

        deque.remove(Integer.valueOf(1));
        deque.remove(Integer.valueOf(0));
        deque.remove(Integer.valueOf(2));

        iter.next();
    }

    @Test
    void testPush() {
        deque.push(ONE);
        deque.push(TWO);
        assertEquals(2, deque.size());
        assertThrows(IllegalStateException.class, () -> deque.push(THREE));
        assertEquals(Integer.valueOf(2), deque.pop());
    }

    @Test
    void testPut() throws InterruptedException {
        assertThrows(NullPointerException.class, () -> deque.put(null));
        deque.put(ONE);
        deque.put(TWO);
    }

    @Test
    void testPutFirst() throws InterruptedException {
        assertThrows(NullPointerException.class, () -> deque.putFirst(null));
        deque.putFirst(ONE);
        deque.putFirst(TWO);
        assertEquals(2, deque.size());
        assertEquals(Integer.valueOf(2), deque.pop());
    }

    @Test
    void testPutLast() throws InterruptedException {
        assertThrows(NullPointerException.class, () -> deque.putLast(null));
        deque.putLast(ONE);
        deque.putLast(TWO);
        assertEquals(2, deque.size());
        assertEquals(Integer.valueOf(1), deque.pop());
    }

    @Test
    void testRemove() {
        assertThrows(NoSuchElementException.class, deque::remove);
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(1), deque.remove());
    }

    @Test
    void testRemoveFirst() {
        assertThrows(NoSuchElementException.class, deque::removeFirst);
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(1), deque.removeFirst());
        assertThrows(NoSuchElementException.class, () -> {
            deque.removeFirst();
            deque.removeFirst();
        });
    }

    @Test
    void testRemoveLast() {
        assertThrows(NoSuchElementException.class, deque::removeLast);
        deque.add(ONE);
        deque.add(TWO);
        assertEquals(Integer.valueOf(2), deque.removeLast());
        assertThrows(NoSuchElementException.class, () -> {
            deque.removeLast();
            deque.removeLast();
        });
    }

    @Test
    void testRemoveLastOccurrence() {
        assertFalse(deque.removeLastOccurrence(null));
        assertFalse(deque.removeLastOccurrence(ONE));
        deque.add(ONE);
        deque.add(ONE);
        assertTrue(deque.removeLastOccurrence(ONE));
        assertEquals(1, deque.size());
    }

    @Test
    void testTake() throws InterruptedException {
        assertTrue(deque.offerFirst(ONE));
        assertTrue(deque.offerFirst(TWO));
        assertEquals(Integer.valueOf(2), deque.take());
    }

    @Test
    void testTakeFirst() throws InterruptedException {
        assertTrue(deque.offerFirst(ONE));
        assertTrue(deque.offerFirst(TWO));
        assertEquals(Integer.valueOf(2), deque.takeFirst());
    }

    @Test
    void testTakeLast() throws InterruptedException {
        assertTrue(deque.offerFirst(ONE));
        assertTrue(deque.offerFirst(TWO));
        assertEquals(Integer.valueOf(1), deque.takeLast());
    }

    @Test
    void testToArray() {
        deque.add(ONE);
        deque.add(TWO);
        Object[] arr = deque.toArray();
        assertEquals(Integer.valueOf(1), arr[0]);
        assertEquals(Integer.valueOf(2), arr[1]);

        arr = deque.toArray(new Integer[0]);
        assertEquals(Integer.valueOf(1), arr[0]);
        assertEquals(Integer.valueOf(2), arr[1]);

        arr = deque.toArray(new Integer[0]);
        assertEquals(Integer.valueOf(1), arr[0]);
        assertEquals(Integer.valueOf(2), arr[1]);
    }
}
