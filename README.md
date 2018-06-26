ReentrantReadWriteLock详解及应用：缓存和大量数据并发访问

1. 创建，两种fair和unfair
     unfair：read和write锁获取依赖于Reentrancy规则，不存在先后顺序
     fair：各个线程按照时间顺序进行锁的获取，当一个线程试图获取read锁时，如果当前没有线程获取write锁且试图获取write的线程请求时间没有请求read锁的时间久，则获取read锁。当一个线程试图获取write锁时，当且仅当当前所有的read锁和write锁全部释放才能成功（ReadLoac.tryLock()和WriteLock.tryLock()不受该约束）
2. ReentrantReadWriteLock提供了两个锁，readLock和writeLock，readLock可多线程并发执行，writeLock只能单线程执行（类似于synchronized）,线程获取writeLock锁之后可以继续获取readLock，反过来不行，后面相关说明
3. 降级：writeLock变成readLock成为锁降级，具体操作为writeLock.lock->readLock.lock->writeLock.unlock
这样就把writeLock变成了readLock
4. 两种锁再获取锁期间都支持interruption
5. 只有writeLock支持condition，通过writeLock.newCondition获得condition对象，对weiteLock进行await和signal、signalAll控制，因为readLock本身支持多线程迸发访问，所以condition控制对readLock没什么用
6. 下面是官方的两个例子，说明ReentrantReadWriteLock的用法
1：缓存
     
[java] view plain copy
class CachedData {  
   Object data;  
   volatile boolean cacheValid;  
   ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();  
  
   void processCachedData() {  
     rwl.readLock().lock();  
     if (!cacheValid) {  
        // Must release read lock before acquiring write lock  
        rwl.readLock().unlock();  
        rwl.writeLock().lock();  
        // Recheck state because another thread might have acquired  
        //   write lock and changed state before we did.  
        if (!cacheValid) {  
          data = ...  
          cacheValid = true;  
        }  
        // Downgrade by acquiring read lock before releasing write lock  
        rwl.readLock().lock();  
        rwl.writeLock().unlock(); // Unlock write, still hold read  
     }  
     use(data);  
     rwl.readLock().unlock();  
   }  
 }  
1. 第三行cacheValid变量使用volatile修饰保证了多线程变量值修改的一致性
2.上面读取缓存的过程：
 程序锁的步骤
1. 获取readlock->若缓存失效->readlock.unlock （此期间有可能其他线程获取writelock修改了缓存数据，所以加了二次判断）->writelock.lock(持有readlock不能直接获取writelock)->二次判断如果缓存数据仍然失效，重新写入数据->获取readlock（用到了前面第三条提到的锁降级使用，即write锁获取期间再次获取readlock，防止期间其他线程获取到writelock修改数据，导致没有返回最新缓存）->释放writelock（因为此时已经成功获取readlock其他线程无法获得writelock修改数据，此时可以是否writelock）->读取缓存->释放readlock
看起来很麻烦，之所以这么操作主要是避免多线程读写数据造成数据不一致，同时readlock线程不阻塞的特性使得缓存读取速度快，同时利用获取read锁后其他线程无法获取write锁的特性避免了读取缓存期间其他线程修改缓存的问题。

2. treeMap当数据操作较大或者read操作明显多于write操作时，由于readLock的不阻塞性质使得ReentrantReadWriteLock效率明显高于synchronized
 
[java] view plain copy
class RWDictionary {  
    private final Map m = new TreeMap();  
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();  
    private final Lock r = rwl.readLock();  
    private final Lock w = rwl.writeLock();  
  
    public Data get(String key) {  
        r.lock();  
        try { return m.get(key); }  
        finally { r.unlock(); }  
    }  
    public String[] allKeys() {  
        r.lock();  
        try { return m.keySet().toArray(); }  
        finally { r.unlock(); }  
    }  
    public Data put(String key, Data value) {  
        w.lock();  
        try { return m.put(key, value); }  
        finally { w.unlock(); }  
    }  
    public void clear() {  
        w.lock();  
        try { m.clear(); }  
        finally { w.unlock(); }  
    }  
 }}  