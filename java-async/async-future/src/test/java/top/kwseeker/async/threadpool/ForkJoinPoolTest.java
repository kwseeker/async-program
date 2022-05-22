package top.kwseeker.async.threadpool;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class ForkJoinPoolTest {

    @Test
    public void testFirstSignalWorkCtl() {
        int parallelism = 8;
        long np = (long)(-parallelism); // offset ctl counts
        long ctl = ((np << 48) & 0xffffL << 48) | ((np << 32) & 0xffffL << 32);
        //long ctl = -1970359196712960L;
        System.out.println(Integer.toBinaryString(-8));
        System.out.println(ctl);
        System.out.println(Long.toBinaryString(ctl));
        // 1111111111111000 1111111111111000    高32位
        // 0000000000000000 0000000000000000    低32位
        int sp = (int) ctl;
        System.out.println(sp);

        long ADD_WORKER = 0x0001L << (32 + 15); // sign
        System.out.println(Long.toBinaryString(ADD_WORKER));
        boolean addWork = (ctl & ADD_WORKER) != 0L;
        System.out.println("add work ? " + addWork);
    }

    @Test
    public void testFinally() {
        for (int i = 0; i < 10; i++) {
            if (i == 3) {
                try {
                    System.out.println("3");
                    //return;
                } finally {
                    System.out.println("do finally");
                }
            } else {
                System.out.println(i);
            }
        }
    }

    //8 -> 16
    //wq array size: 4
    //000000 000001 000002
    //wq array size: 8
    //000003 000004
    //wq array size: 16
    //000005 000006 000007 000008
    //wq array size: 32
    //000009 000010 000011 000012 000013 000014 000015 000016
    //wq array size: 64
    //000017 000018 000019 000020 000021 000022 000023 000024 000025 000026 000027 000028 000029 000030 000031 000032
    //wq array size: 128
    //000033 000034 000035 000036 000037 000038 000039 000040 000041 000042 000043 000044 000045 000046 000047 000048 000049 000050 000051 000052 000053 000054 000055 000056 000057 000058 000059 000060 000061 000062 000063 000064
    //wq array size: 256
    //000065 000066 000067 000068 000069 000070 000071 000072 000073 000074 000075 000076 000077 000078 000079 000080 000081 000082 000083 000084 000085 000086 000087 000088 000089 000090 000091 000092 000093 000094 000095 000096 000097 000098 000099 000100 000101 000102 000103 000104 000105 000106 000107 000108 000109 000110 000111 000112 000113 000114 000115 000116 000117 000118 000119 000120 000121 000122 000123 000124 000125 000126 000127 000128
    //......
    @Test
    public void testGenerateWorkQueueArraySize() {
        Set<Integer> arrSizeSet = new HashSet<>();
        int n;
        for (int p = 0; p <= 65535; p++) {
            n = (p > 1) ? p - 1 : 1;
            n |= n >>> 1;
            n |= n >>> 2;
            n |= n >>> 4;
            n |= n >>> 8;
            n |= n >>> 16;
            n = (n + 1) << 1;

            if (!arrSizeSet.contains(n)) {
                System.out.println();
                System.out.println("wq array size: " + n);
                arrSizeSet.add(n);
            }
            System.out.printf("%06d ", p);
        }
    }
}
