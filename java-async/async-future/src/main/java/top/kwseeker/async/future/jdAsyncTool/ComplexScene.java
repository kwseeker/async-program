package top.kwseeker.async.future.jdAsyncTool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * 对比使用 CompletableFuture 实现 jd asyncTool 中的复杂场景（只考虑强依赖）
 * <a href="https://gitee.com/jd-platform-opensource/asyncTool">asyncTool</a>
 */
public class ComplexScene {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // A -------> B ----
        //      └---> C ---└---> F ----
        // D -------> E -------> G ---└---> H
        CompletableFuture<String> aFuture = CompletableFuture.supplyAsync(new Task("A"));
        CompletableFuture<Object> fFuture = aFuture.thenApplyAsync(ar -> {
            System.out.println(ar + " -> B/C");
            try {
                //问题是allOf没有返回值，如果F需要依赖B和C的结果，就不能这么做了
                CompletableFuture.allOf(CompletableFuture.supplyAsync(new Task("B")),
                                CompletableFuture.supplyAsync(new Task("C")))
                        .thenApplyAsync(v -> new Task("F").get()).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            return "UK";
        });

        CompletableFuture<String> dFuture = CompletableFuture.supplyAsync(new Task("D"));
        CompletableFuture<String> eFuture = dFuture.thenApplyAsync(dr -> {
            System.out.println(dr + " -> E");
            return new Task("E").get();
        });
        CompletableFuture<String> gFuture = eFuture.thenApplyAsync(er -> {
            System.out.println(er + " -> G");
            return new Task("G").get();
        });

        CompletableFuture<String> future = CompletableFuture
                .allOf(fFuture, gFuture)
                .thenApplyAsync(v -> new Task("H").get());
        String s = future.get();
    }

    static class Task implements Supplier<String> {

        private final String result;

        public Task(String result) {
            this.result = result;
        }

        @Override
        public String get() {
            System.out.println(result + ", executed by thread: " + Thread.currentThread().getName());
            return result;
        }
    }
}
