package top.kwseeker.async.threadpool;

import org.junit.Test;

public class ThreadPoolTest {

    @Test
    public void test() {
        System.out.println(Integer.toBinaryString(-1 << 29));
        System.out.println(Integer.toBinaryString(1 << 29));
        System.out.println(Integer.toBinaryString(2 << 29));
        System.out.println(Integer.toBinaryString(3 << 29));

        System.out.println(Integer.toBinaryString(-536870911)); //11100000000000000000000000000001
    }
}
