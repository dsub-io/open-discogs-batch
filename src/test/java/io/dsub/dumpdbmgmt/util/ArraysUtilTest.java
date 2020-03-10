package io.dsub.dumpdbmgmt.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArraysUtilTest {

    @Test
    void addArrays() {
        Long[] arrOne = new Long[]{1L, 2L,3L,4L,5L,6L};
        Long[] arrTwo = new Long[]{7L,8L,9L};

        Long[] arr = ArraysUtil.merge(arrOne, arrTwo);
        assertEquals(arrOne.length + arrTwo.length, arr.length);
    }
}
