package io.dsub.dumpdbmgmt.util;

import java.util.Arrays;

public class ArraysUtil {

    public static Long[] remove(Long[] source, Long target) {
        if (source == null || source.length == 0) {
            return new Long[0];
        }

        Long[] arr = new Long[source.length];

        for (int i = 0; i < source.length; i++) {
            if (!source[i].equals(target)) {
                arr[i] = source[i];
            }
        }
        arr = ArraysUtil.removeZero(arr);
        Arrays.sort(arr);
        return arr;
    }


    public static Long[] merge(Long[] source, Long... target) {

        Long[] arr;

        // Check null or empty array
        if (source == null || source.length == 0) {
            if (target == null || target.length == 0) {
                return new Long[0];
            } else {
                return target;
            }
        } else {
            if (target != null && target.length > 0) {
                arr = new Long[source.length + target.length];
            } else {
                return source;
            }
        }

        int i = 0;

        for (Long var : source) {
            if (!ArraysUtil.contains(arr, var)) {
                arr[i++] = var;
            }
        }

        for (Long var : target) {
            if (!ArraysUtil.contains(arr, var)) {
                arr[i++] = var;
            }
        }
        arr = removeDuplicate(arr);
        Arrays.sort(arr);

        return arr;
    }

    public static Long[] removeZero(Long[] source) {
        if (source == null || source.length == 0) {
            return source;
        }

        int count = 0;

        for (Long val : source) {
            if (val == null || val == 0) {
                count++;
            }
        }

        if (count == source.length) {
            return new Long[0];
        }

        Long[] target = new Long[source.length - count];

        int index = 0;
        for (Long val : source) {
            if (val != null && val != 0) {
                target[index++] = val;
            }
        }

        return target;
    }

    public static Boolean contains(Long[] source, Long target) {
        if (source == null || source.length == 0) {
            return false;
        }

        for (Long var : source) {
            if (var != null) {
                if (var.equals(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Long[] removeDuplicate(Long[] source) {
        if (source == null || source.length == 0) {
            return new Long[0];
        }

        if (source.length == 1) {
            return source;
        }

        int targetIndex = 0;
        Long[] target = new Long[source.length];

        for (Long var : source) {
            if (!ArraysUtil.contains(target, var)) {
                target[targetIndex] = var;
            }
            targetIndex++;
        }

        return ArraysUtil.removeZero(target);
    }
}
