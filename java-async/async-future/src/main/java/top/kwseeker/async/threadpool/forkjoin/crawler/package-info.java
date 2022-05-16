package top.kwseeker.async.threadpool.forkjoin.crawler;

/*
测试来源：
Java Tip: When to use ForkJoinPool vs ExecutorService
https://www.infoworld.com/article/2078440/java-tip-when-to-use-forkjoinpool-vs-executorservice.html

@Override
public boolean visited(String s) {
    return false;//visitedLinks.contains(s);
}

上面代码改为false（允许重新查询已查过的链接内的链接），查询link数改为3000后， ForkJoinPool的性能是 ThreadPoolExecutor 的 1.5 倍，
之前基本差不多。

作者也没说为何速度会变快。

 */