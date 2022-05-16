package top.kwseeker.async.threadpool.forkjoin.sum;

import java.util.Random;

public class RandomIntArrayUtil {

    public static int[] buildRandomIntArray(int size) {
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = new Random().nextInt(100);
        }
        return array;
    }
}
