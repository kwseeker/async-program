# 带依赖关系的任务编排

工作中的项目也曾碰到过这种场景。

看到了一个京东开源的工具 [aysncTool](https://gitee.com/jd-platform-opensource/asyncTool)， 称实现了下面功能：

+ **支持任意编排**

  比如多个任务串行执行（顺序依赖）、并行执行（无依赖）、阻塞等待（一个任务依赖多个）、串并行相互依赖等。

  比如一个比较复杂的包含依赖关系的任务编排案例：

  ![](img/asyncTool-complex-scence.png)

+ **支持每个任务的执行结果回调**

+ **支持强依赖和弱依赖**

  强依赖：目标任务执行前必须先等待完成所有强依赖的任务。

  弱依赖：如果目标任务依赖的都不是强依赖，那么就可以任意一个依赖项执行完毕，就可以执行目标任务。

  其实作者这里想表达**全部**和**任一**的概念，说强依赖和弱依赖有点迷惑，不存在一个任务的依赖中既有强依赖又有弱依赖的情况。

+ **支持上游执行结果作为下游传参**

+ **支持设置全组任务超时时间**

+ **高性能、低线程数**

  

## 任务编排的实现方案

+ **CompletableFuture**

  功能比较简单；

  无法支持多依赖任务的结果传参给下游任务。

  CompletableFuture 实现上述包含复杂依赖关系的任务编排：async-future/top.kwseeker.async.future.jdAsyncTool.ComplexScene.java。

+ **自己设计框架**

  基于监听回调

+ **asyncTool**

+ **gobrs-async**

+ **Quasar / Loom**

+ **任务调度框架**

  一些任务调度框架也实现了带依赖关系的任务编排功能。

  + Celery

+ **工作流框架**

  任务编排其实和工作流任务编排类似。

  + Workflow
  + Activity

图表对比：



## 自行设计实现asyncTool描述的功能

感觉这个asyncTool描述的功能实现起来也比较简单，为了不被其他框架带节奏，先花几个小时自行设计并实现一个简单实现，然后对比其他实现。

先分析每个需求怎么实现：

+ 支持任意编排

  任务的依赖关系有两种：前置依赖（依赖其他任务）、后置依赖（被其他任务依赖），

  前置依赖和后置依赖都可能有0、1到多个。

  数据结构设计：

  ```java
  //任务名，通过这个字段唯一代表一个任务
  private final String name;
  //前置依赖任务，当前任务依赖的任务
  private Map<String, Task> prevTasks = new HashMap<>();
  //后置依赖任务，当前任务被依赖
  private Map<String, Task> postTasks = new HashMap<>();
  ```

+ 支持每个任务的执行结果回调

  给任务注册监听器就行，任务执行完毕回调其所有监听器；

  另外为了防止轮询等待任务执行完毕造成性能和资源损耗，使用回调触发后置依赖任务的执行。

+ 代码实现也比较简单支持强依赖和弱依赖

  强依赖（默认）：（全部依赖都执行了才执行）每个任务都包含前置依赖任务的引用，检查所有强依赖前置任务完成状态，都完成才能执行，后置依赖任务依靠它的前置依赖任务触发，前置依赖。

  弱依赖：（任一依赖任务执行就执行）由于后置依赖任务依靠它的前置依赖任务触发，所以不需要检查全部依赖都完成，但是弱依赖可能需要取消其他的未完成的依赖任务，如果需要取消的依赖任务还有依赖，也要取消，不过还有可能依赖的任务还被其他任务依赖，怎么处理？

  比如：F弱依赖于B、C，下图中其他依赖都是强依赖，B先执行完毕，C尚未执行或未执行完毕，这里由于存在 C->G->H的强依赖，所以不能取消任务C；如果G也弱依赖于C、E呢？如果H也弱依赖于F、G呢？（这时C是否应该取消的判断就变得很复杂）

  ![](img/async-tasks-weak-dependencies.png)

  暂时只想到一种方法可以减少部分任务的执行：每个任务都保存其最终任务的引用（可能多个），每个任务触发前都判断下其所有最终任务是否都已经开始执行或结束，如果是，则此任务无需继续执行。

  > 这里弱依赖的任务取消是难点。

+ 支持上游执行结果作为下游传参

  上游任务完成才提交下游任务，上游的结果直接放到下游任务中提交给线程池。

+ 支持设置全组任务超时时间

  可以给编排的任务封装一个组，每个组设置一个定时任务，到期检查全部尾部任务是否都完成（任务中加一个state记录状态），未完成的话取消正在执行的任务，抛超时异常；

  全组任务提前完成则设置completed状态，并清除定时任务。

+ 高性能、低线程数

  这个属于优化范畴的，可以先忽略。

代码实现：java-async/async-orchestration。



## asyncTool

主要分析主要的类实现和关键逻辑实现。

### 数据结构与接口设计

+ `IWorker<T, V>` 

  定义异步任务业务主体，里面通过 WorkerWrapper 装饰器模式增强任务主体逻辑。

  ```java
  public interface IWorker<T, V> {
      //定义任务业务实现，比如业务处理、RPC请求等
      V action(T object, Map<String, WorkerWrapper> allWrappers);
      //超时、异常时，返回的默认值
      default V defaultValue() {
          return null;
      }
  }
  ```

+ `ICallback<T, V>`

  回调接口，监听任务启动、任务执行结果。

+ `WorkerWrapper<T, V>`

  从这个类数据结构可以看到，asyncTool将前置依赖封装成了`DependWrapper`（封装依赖的任务和是否是强依赖的标识），

  其任务编排也是通过前后引用的方式（nextWrappers、dependWrappers），像链表一样。

  ```java
  /**
  * 该wrapper的唯一标识
  */
  private String id;
  /**
  * worker将来要处理的param
  */
  private T param;
  private IWorker<T, V> worker;
  private ICallback<T, V> callback;forParamUseWrappers
  /**
  * 在自己后面的wrapper，如果没有，自己就是末尾；如果有一个，就是串行；如果有多个，有几个就需要开几个线程</p>
  */
  private List<WorkerWrapper<?, ?>> nextWrappers;
  /**
  * 依赖的wrappers，有2种情况，1:必须依赖的全部完成后，才能执行自己 2:依赖的任何一个、多个完成了，就可以执行自己
  * 通过must字段来控制是否依赖项必须完成
  */
  private List<DependWrapper> dependWrappers;
  /**
  * 标记该事件是否已经被处理过了，譬如已经超时返回false了，后续rpc又收到返回值了，则不再二次回调
  * 经试验,volatile并不能保证"同一毫秒"内,多线程对该值的修改和拉取
  * <p>
  * 1-finish, 2-error, 3-working
  */
  private AtomicInteger state = new AtomicInteger(0);
  /**
  * 该map存放所有wrapper的id和wrapper映射
  */
  private Map<String, WorkerWrapper> forParamUseWrappers;
  /**
  * 也是个钩子变量，用来存临时的结果
  */
  private volatile WorkResult<V> workResult = WorkResult.defaultResult();
  /**
  * 是否在执行自己前，去校验nextWrapper的执行结果<p>
  * 1   4
  * -------3
  * 2
  * 如这种在4执行前，可能3已经执行完毕了（被2执行完后触发的），那么4就没必要执行了。
  * 注意，该属性仅在nextWrapper数量<=1时有效，>1时的情况是不存在的
  */
  private volatile boolean needCheckNextWrapperResult = true;
  
  private static final int FINISH = 1;
  private static final int ERROR = 2;
  private static final int WORKING = 3;
  private static final int INIT = 0;
  ```

  + `DependWrapper`

    ```java
    private WorkerWrapper<?, ?> dependWrapper;
    private boolean must = true;
    ```

  + `WorkerWrapper<T, V>$Builder<W,C>`

    使用构造器模式通过此Builder创建任务实例、做任务编排。

+ `Async`

  线程池封装，用于提交任务执行、管理线程池。

  这里重点看：有依赖的多个任务怎么执行的？上一个任务的结果怎么传递给下一个任务的？

  workerWrappers 其实是前面自己的实现中说的没有相互依赖关系的多个头部任务。

  同步接口：**借助CompletableFuture提交所有头部任务并等待执行结束，其中每个头部任务还会遍历自己所有后置任务同样借助CompletableFuture提交所有后置任务并等待执行结束(如果只有一个后置任务就直接同步调用)，循环往复直到所有任务后置依赖执行结束**。

  异步接口：**和同步接口的区别是头部任务是提交到了线程池并通过回调（IGroupCallback）处理结果，头部任务的后置任务的处理依旧是通过借助CompletableFuture提交所有后置依赖任务并等待执行结束(如果只有一个后置任务就直接同步调用)**。

  > 注意这么写其实有个**问题**：由于上面包含同步等待操作（`CompletableFuture.allOf(...).get(...)`）容易导致任务线程堆积，因为一个任务依赖链条中所有任务全部执行完毕，所有任务线程才会结束，使用有线程数量限制的线程池很容易耗尽线程，估计是这个原因默认使用了`Executors.newCachedThreadPool()`创建线程池。
  >
  > **所以使用自定义的线程池替换COMMON_POOL也要小心这个线程耗尽的坑**。
  >
  > ```java
  > private static final ThreadPoolExecutor COMMON_POOL = (ThreadPoolExecutor) Executors.newCachedThreadPool();
  > //还不如使用默认的ForkJoinPool，后面测试了使用默认的ForkJoinPool，即使线程数设置2也没有发生线程耗尽的情况。
  > futures[i] = CompletableFuture.runAsync(() -> wrapper.work(executorService, timeout, forParamUseWrappers), executorService);
  > ```
  >
  > 对比非同步等待的方式执行依赖任务，线程数量可能随链条长度成比例增长。
  >
  > 可以将`Async.COMMON_POOL`改成固定线程的线程池，然后执行`TestPar#testMutli7()`，验证上面问题的猜想；最终会发现有限的线程被占用完了，然后后面的任务无法处理，最后就超时了。
  >
  > ```
  > //private static final ThreadPoolExecutor COMMON_POOL = (ThreadPoolExecutor) Executors.newCachedThreadPool();
  > private static final ThreadPoolExecutor COMMON_POOL = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
  > ```
  >
  > 测试了CompletableFuture默认使用的ForkJoinPool没有看到线程被占用完，如下代码，其实是因为2是并行度，并不是可用的工作者线程的数量，实际可以创建很多工作者线程。
  >
  > ```java
  >//private static final ThreadPoolExecutor COMMON_POOL = (ThreadPoolExecutor) Executors.newCachedThreadPool();
  > //这样修改，重新执行 TestPar#testMutli7() 也可以正常执行，没有发生线程耗尽的问题
  > private static final ExecutorService COMMON_POOL =  new ForkJoinPool(2);
  > ```
  
  通过 WorkerWrapper param 字段将上一个任务结果传递给下一个任务。

  ```java
  public static boolean beginWork(long timeout, ExecutorService executorService, List<WorkerWrapper> workerWrappers) throws ExecutionException, InterruptedException {
      if(workerWrappers == null || workerWrappers.size() == 0) {
          return false;
      }
      //保存线程池变量
      Async.executorService = executorService;
      //定义一个map，存放所有的wrapper，key为wrapper的唯一id，value是该wrapper，可以从value中获取wrapper的result
      Map<String, WorkerWrapper> forParamUseWrappers = new ConcurrentHashMap<>();
      CompletableFuture[] futures = new CompletableFuture[workerWrappers.size()];
      for (int i = 0; i < workerWrappers.size(); i++) {
          WorkerWrapper wrapper = workerWrappers.get(i);
          //借助CompletableFuture等待所有头部任务执行结束
          //头部任务执行完毕，通过 WorkWrapper$beginNext() 执行后置任务
          futures[i] = CompletableFuture.runAsync(() -> wrapper.work(executorService, timeout, forParamUseWrappers), executorService);
      }
      try {
          CompletableFuture.allOf(futures).get(timeout, TimeUnit.MILLISECONDS);
          return true;
      } catch (TimeoutException e) {
          Set<WorkerWrapper> set = new HashSet<>();
          totalWorkers(workerWrappers, set);
          for (WorkerWrapper wrapper : set) {
              wrapper.stopNow();
          }
          return false;
      }
  }
  
  public static void beginWorkAsync(long timeout, ExecutorService executorService, IGroupCallback groupCallback, 	WorkerWrapper... workerWrapper) {
      if (groupCallback == null) {
          groupCallback = new DefaultGroupCallback();
      }
      IGroupCallback finalGroupCallback = groupCallback;
      if (executorService != null) {
          executorService.submit(() -> {
              try {
                  boolean success = beginWork(timeout, executorService, workerWrapper);
                  if (success) {
                      finalGroupCallback.success(Arrays.asList(workerWrapper));
                  } else {
                      finalGroupCallback.failure(Arrays.asList(workerWrapper), new TimeoutException());
                  }
              } catch (ExecutionException | InterruptedException e) {
                  e.printStackTrace();
                  finalGroupCallback.failure(Arrays.asList(workerWrapper), e);
              }
          });
      } else {
          COMMON_POOL.submit(() -> {
              try {
                  boolean success = beginWork(timeout, COMMON_POOL, workerWrapper);
                  if (success) {
                      finalGroupCallback.success(Arrays.asList(workerWrapper));
                  } else {
                      finalGroupCallback.failure(Arrays.asList(workerWrapper), new TimeoutException());
                  }
              } catch (ExecutionException | InterruptedException e) {
                  e.printStackTrace();
                  finalGroupCallback.failure(Arrays.asList(workerWrapper), e);
              }
          });
      }
  }
  
  //WorkerWrapper$beginNext
  //通过遍历nextWrappers使用CompletableFuture.runAsync()提交任务，并等待所有后置依赖任务执行完毕
  //CompletableFuture.allOf(futures).get(remainTime - costTime, TimeUnit.MILLISECONDS);
  private void beginNext(ExecutorService executorService, long now, long remainTime) {
      //花费的时间
      long costTime = SystemClock.now() - now;
      if (nextWrappers == null) {
          return;
      }
      if (nextWrappers.size() == 1) {
          nextWrappers.get(0).work(executorService, WorkerWrapper.this, remainTime - costTime, forParamUseWrappers);
          return;
      }
      CompletableFuture[] futures = new CompletableFuture[nextWrappers.size()];
      for (int i = 0; i < nextWrappers.size(); i++) {
          int finalI = i;
          //遍历nextWrappers使用CompletableFuture.runAsync()提交后置任务
          futures[i] = CompletableFuture.runAsync(() -> nextWrappers.get(finalI)
                .work(executorService, WorkerWrapper.this, remainTime - costTime, forParamUseWrappers), executorService);
      }
      try {
          //等待所有后置依赖任务执行完毕
          CompletableFuture.allOf(futures).get(remainTime - costTime, TimeUnit.MILLISECONDS);
      } catch (Exception e) {
          e.printStackTrace();
      }
  }
  ```



## gobrs-async

