/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.apache.asterix.external.feed.ml.tools.textanalysis;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Util {
    public static final String OS_ARCH = System.getProperty("os.arch");

    public static final boolean JRE_IS_64BIT;
    static {
        String x = System.getProperty("sun.arch.data.model");
        if (x != null) {
            JRE_IS_64BIT = x.indexOf("64") != -1;
        } else {
            if (OS_ARCH != null && OS_ARCH.indexOf("64") != -1) {
                JRE_IS_64BIT = true;
            } else {
                JRE_IS_64BIT = false;
            }
        }
    }

    public static int[] findMaxAndMin(int array[]) {
        int max = 0;
        int min = Integer.MAX_VALUE;

        for (int value : array) {
            if (value > max) {
                max = value;
            }
            if (value < min) {
                min = value;
            }
        }
        return new int[] { max, min };
    }

    public static <T> void addAll(Collection<T> set, T... array) {
        for (T e : array) {
            set.add(e);
        }
    }

    public static <T> Set<T> asSet(T... array) {
        Set<T> newSet = new HashSet<T>();
        for (T e : array)
            newSet.add(e);

        return newSet;
    }

    public static <T> T genericGrowArray(int minSize, T... array) {
        if (minSize <= array.length)
            minSize = array.length + 1;

        T newArray = (T) Array.newInstance(array.getClass().getComponentType(), minSize);

        System.arraycopy(array, 0, newArray, 0, array.length);

        return newArray;
    }

    public static char[] growArray(int minSize, char[] array) {
        return genericGrowArray(minSize, array);
    }

    /**
     * Returns an array size >= minTargetSize, generally
     * over-allocating exponentially to achieve amortized
     * linear-time cost as the array grows.
     * NOTE: this was originally borrowed from Python 2.4.2
     * listobject.c sources...
     *
     * @param minTargetSize
     *            Minimum required value to be returned.
     * @param bytesPerElement
     *            Bytes used by each element of
     *            the array.
     */

    public static int oversize(int minTargetSize, int bytesPerElement) {

        if (minTargetSize < 0) {
            // catch usage that accidentally overflows int
            throw new IllegalArgumentException("invalid array size " + minTargetSize);
        }

        if (minTargetSize == 0) {
            // wait until at least one element is requested
            return 0;
        }

        // asymptotic exponential growth by 1/8th, favors
        // spending a bit more CPU to not tie up too much wasted
        // RAM:
        int extra = minTargetSize >> 3;

        if (extra < 3) {
            // for very small arrays, where constant overhead of
            // realloc is presumably relatively high, we grow
            // faster
            extra = 3;
        }

        int newSize = minTargetSize + extra;

        // add 7 to allow for worst case byte alignment addition below:
        if (newSize + 7 < 0) {
            // int overflowed -- return max allowed array size
            return Integer.MAX_VALUE;
        }

        if (JRE_IS_64BIT) {
            // round up to 8 byte alignment in 64bit env
            switch (bytesPerElement) {
                case 4:
                    // round up to multiple of 2
                    return (newSize + 1) & 0x7ffffffe;
                case 2:
                    // round up to multiple of 4
                    return (newSize + 3) & 0x7ffffffc;
                case 1:
                    // round up to multiple of 8
                    return (newSize + 7) & 0x7ffffff8;
                case 8:
                    // no rounding
                default:
                    // odd (invalid?) size
                    return newSize;
            }
        } else {
            // round up to 4 byte alignment in 64bit env
            switch (bytesPerElement) {
                case 2:
                    // round up to multiple of 2
                    return (newSize + 1) & 0x7ffffffe;
                case 1:
                    // round up to multiple of 4
                    return (newSize + 3) & 0x7ffffffc;
                case 4:
                case 8:
                    // no rounding
                default:
                    // odd (invalid?) size
                    return newSize;
            }
        }
    }

}
