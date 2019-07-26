/*
 * Copyright 2014-2019 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agrona.collections;

import org.agrona.BitUtil;
import org.agrona.generation.DoNotSub;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Queue of ints which stores the elements without boxing. Null is represented by a special {@link #nullValue()}.
 *
 * <b>Note:</b> This class is not threadsafe.
 */
public class IntArrayQueue extends AbstractQueue<Integer> implements Serializable
{
    /**
     * Default representation of null for an element.
     */
    public static final int DEFAULT_NULL_VALUE = Integer.MIN_VALUE;

    /**
     * Minimum capacity for the queue which must also be a power of 2.
     */
    @DoNotSub public static final int MIN_CAPACITY = 8;

    @DoNotSub private int head;
    @DoNotSub private int tail;
    @DoNotSub private int capacity;
    private final int nullValue;
    private int[] elements;

    public IntArrayQueue()
    {
        this(MIN_CAPACITY, DEFAULT_NULL_VALUE);
    }

    public IntArrayQueue(final int nullValue)
    {
        this(MIN_CAPACITY, nullValue);
    }

    public IntArrayQueue(
        @DoNotSub final int initialCapacity,
        final int nullValue)
    {
        this.nullValue = nullValue;

        if (initialCapacity < MIN_CAPACITY)
        {
            throw new IllegalArgumentException("initial capacity < MIN_INITIAL_CAPACITY : " + initialCapacity);
        }

        capacity = BitUtil.findNextPositivePowerOfTwo(initialCapacity);
        if (capacity < MIN_CAPACITY)
        {
            throw new IllegalArgumentException("invalid initial capacity: " + initialCapacity);
        }

        elements = new int[capacity];
        Arrays.fill(elements, nullValue);
    }

    /**
     * The value representing a null element.
     *
     * @return value representing a null element.
     */
    public int nullValue()
    {
        return nullValue;
    }

    /**
     * The current capacity for the collection
     *
     * @return the current capacity for the collection
     */
    public int capacity()
    {
        return capacity;
    }

    /**
     * {@inheritDoc}
     */
    @DoNotSub public int size()
    {
        return (tail - head) & (elements.length - 1);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
    {
        return head == tail;
    }

    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        if (head != tail)
        {
            Arrays.fill(elements, nullValue);
            head = 0;
            tail = 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean offer(final Integer element)
    {
        return offerInt(element);
    }

    /**
     * Offer an element to the tail of the queue without boxing.
     *
     * @param element to be offer to the queue.
     * @return will always be true as long as the underlying array can be expanded.
     */
    public boolean offerInt(final int element)
    {
        if (nullValue == element)
        {
            throw new NullPointerException(); // @DoNotSub
        }

        elements[tail++] = element;

        if ((tail & (elements.length - 1)) == head)
        {
            increaseCapacity();
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean add(final Integer element)
    {
        return offerInt(element);
    }

    /**
     * Offer an element to the tail of the queue without boxing.
     *
     * @param element to be offer to the queue.
     * @return will always be true as long as the underlying array can be expanded.
     */
    public boolean addInt(final int element)
    {
        return offerInt(element);
    }

    /**
     * {@inheritDoc}
     */
    public Integer peek()
    {
        final int element = elements[head];

        return element == nullValue ? null : element;
    }

    /**
     * Peek at the element on the head of the queue without boxing.
     *
     * @return the element at the head of the queue without removing it.
     */
    public int peekInt()
    {
        return elements[head];
    }

    /**
     * {@inheritDoc}
     */
    public Integer poll()
    {
        final int element = pollInt();

        return element == nullValue ? null : element;
    }

    /**
     * Poll the element from the head of the queue without boxing.
     *
     * @return the element at the head of the queue removing it. If empty then {@link #nullValue}.
     */
    public int pollInt()
    {
        final int element = elements[head];
        if (nullValue == element)
        {
            return nullValue;
        }

        elements[head] = nullValue;
        head = (head + 1) & (elements.length - 1);

        return element;
    }

    /**
     * {@inheritDoc}
     */
    public Integer remove()
    {
        final int element = pollInt();
        if (nullValue == element)
        {
            throw new NoSuchElementException();
        }

        return element;
    }

    /**
     * {@inheritDoc}
     */
    public Integer element()
    {
        final int element = elements[head];
        if (nullValue == element)
        {
            throw new NoSuchElementException();
        }

        return element;
    }

    /**
     * Peek at the element on the head of the queue without boxing.
     *
     * @return the element at the head of the queue without removing it.
     * @throws NoSuchElementException if the queue is empty.
     */
    public int elementInt()
    {
        final int element = elements[head];
        if (nullValue == element)
        {
            throw new NoSuchElementException();
        }

        return element;
    }

    /**
     * Remove the element at the head of the queue without boxing.
     *
     * @return the element at the head of the queue.
     * @throws NoSuchElementException if the queue is empty.
     */
    public int removeInt()
    {
        final int element = pollInt();
        if (nullValue == element)
        {
            throw new NoSuchElementException();
        }

        return element;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(final Object other)
    {
        if (other == this)
        {
            return true;
        }

        boolean isEqual = false;

        if (other instanceof IntArrayQueue)
        {
            return equals((IntArrayQueue)other);
        }
        else if (other instanceof Queue)
        {
            final Queue<?> that = (Queue<?>)other;

            if (this.size() == that.size())
            {
                isEqual = true;
                @DoNotSub int i = this.head;

                for (final Object o : that)
                {
                    if (o instanceof Integer)
                    {
                        final Integer thisValue = elements[i];
                        final Integer thatValue = (Integer)o;

                        if (Objects.equals(thisValue, thatValue))
                        {
                            i = (i + 1) & (elements.length - 1);
                            continue;
                        }
                    }

                    isEqual = false;
                    break;
                }
            }
        }

        return isEqual;
    }

    public boolean equals(final IntArrayQueue that)
    {
        if (that == this)
        {
            return true;
        }

        boolean isEqual = false;

        if (this.size() == that.size())
        {
            isEqual = true;

            for (@DoNotSub int i = this.head, j = that.head; i != this.tail; )
            {
                final int thisValue = this.elements[i];
                final int thatValue = that.elements[j];

                if (thisValue != thatValue)
                {
                    isEqual = false;
                    break;
                }

                i = (i + 1) & (this.elements.length - 1);
                j = (j + 1) & (that.elements.length - 1);
            }
        }

        return isEqual;
    }

    /**
     * {@inheritDoc}
     */
    @DoNotSub public int hashCode()
    {
        @DoNotSub int hashCode = 0;
        for (@DoNotSub int i = head; i != tail; )
        {
            final int value = elements[i];
            hashCode = 31 * hashCode + Hashing.hash(value);
            i = (i + 1) & (elements.length - 1);
        }

        return hashCode;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append('[');

        for (@DoNotSub int i = head; i != tail; )
        {
            sb.append(elements[i]).append(", ");
            i = (i + 1) & (elements.length - 1);
        }

        if (sb.length() > 1)
        {
            sb.setLength(sb.length() - 2);
        }

        sb.append(']');

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public void forEach(final Consumer<? super Integer> action)
    {
        for (@DoNotSub int i = head; i != tail; )
        {
            action.accept(elements[i]);
            i = (i + 1) & (elements.length - 1);
        }
    }

    /**
     * Iterate over the collection without boxing.
     *
     * @param action to be taken for each element.
     */
    public void forEachInt(final IntConsumer action)
    {
        for (@DoNotSub int i = head; i != tail; )
        {
            action.accept(elements[i]);
            i = (i + 1) & (elements.length - 1);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Integer> iterator()
    {
        return null;
    }

    private void increaseCapacity()
    {
        @DoNotSub final int oldHead = head;
        @DoNotSub final int oldCapacity = elements.length;
        @DoNotSub final int toEndOfArray = oldCapacity - oldHead;
        @DoNotSub final int newCapacity = oldCapacity << 1;

        if (newCapacity < MIN_CAPACITY)
        {
            throw new IllegalStateException("max capacity reached");
        }

        final int[] array = new int[newCapacity];
        Arrays.fill(array, oldCapacity, newCapacity, nullValue);
        System.arraycopy(elements, oldHead, array, 0, toEndOfArray);
        System.arraycopy(elements, 0, array, toEndOfArray, oldHead);

        elements = array;
        head = 0;
        tail = oldCapacity;
    }
}
