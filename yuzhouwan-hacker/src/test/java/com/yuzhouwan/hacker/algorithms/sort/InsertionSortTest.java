package com.yuzhouwan.hacker.algorithms.sort;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * Copyright @ 2016 yuzhouwan.com
 * All right reserved.
 * Function：InsertionSort Tester
 *
 * @author Benedict Jin
 * @since 2015/9/21
 */
public class InsertionSortTest {

    private static final Logger _log = LoggerFactory.getLogger(InsertionSortTest.class);

    private InsertionSort insertionSort;

    @Before
    public void before() throws Exception {
        insertionSort = new InsertionSort();
    }

    @After
    public void after() throws Exception {
        insertionSort = null;
    }

    /**
     * Method: insertionSort(int[] unSort)
     */
    @Test
    public void testInsertionSort() throws Exception {
        StringBuilder strBuilder;
        {
            int[] unSort = {3, 2, 1};
            int[] sort = insertionSort.insertionSort(unSort);
            strBuilder = new StringBuilder();
            for (int i : sort)
                strBuilder.append(i);
            assertEquals("123", strBuilder.toString());
        }
        {
            int[] unSort = {1, 1, 1};
            int[] sort = insertionSort.insertionSort(unSort);
            strBuilder = new StringBuilder();
            for (int i : sort)
                strBuilder.append(i);
            assertEquals("111", strBuilder.toString());
        }
        {
            int[] unSort = {1, 2, 3};
            int[] sort = insertionSort.insertionSort(unSort);
            strBuilder = new StringBuilder();
            for (int i : sort)
                strBuilder.append(i);
            assertEquals("123", strBuilder.toString());
        }
    }

    @Test
    public void pressureTest() {

        final int ARRAY_SIZE = 1_0000;

        int[] sorted = new int[ARRAY_SIZE];
        int[] reversed = new int[ARRAY_SIZE];
        for (int i = 0; i < ARRAY_SIZE; i++)
            reversed[ARRAY_SIZE - i - 1] = sorted[i] = i;
        long begin;
        long end;
        {
            begin = System.currentTimeMillis();
            insertionSort.insertionSort(sorted);
            end = System.currentTimeMillis();
            _log.debug("Max: " + sorted[ARRAY_SIZE - 1] + ", and finished in " + (end - begin) + " millisecond\r\n");
        }
        {
            begin = System.currentTimeMillis();
            insertionSort.insertionSort(reversed);
            end = System.currentTimeMillis();
            _log.debug("Max: " + sorted[ARRAY_SIZE - 1] + ", and finished in " + (end - begin) + " millisecond\r\n");
        }
    }
}
