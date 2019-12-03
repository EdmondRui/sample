package com.moon.sample.template;

import com.moon.sample.config.WrapperZk;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Slf4j
@Component
public class CuratorTemplate implements InitializingBean {

    private static final String ROOT_PATH_LOCK = "/lock";

    private static final String ROOT_PATH_ID = "/id";

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    @Autowired
    private CuratorFramework curatorFramework;

    @Autowired
    private WrapperZk wrapperZk;

    /**
     * 获取分布式锁
     */
    public void lock(String path) {
        String keyPath = ROOT_PATH_LOCK + "/" + path;
        while (true) {
            try {
                curatorFramework
                        .create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath(keyPath);
                log.info("success to acquire lock for path:{}", keyPath);
                break;
            } catch (Exception e) {
                log.info("failed to acquire lock for path:{} , while try again .......", keyPath);
                try {
                    if (countDownLatch.getCount() <= 0) {
                        countDownLatch = new CountDownLatch(1);
                    }
                    countDownLatch.await();
                }catch (InterruptedException e_) {
                    log.error("countDownLatch.await() failed ", e_);
                }
            }
        }
    }

    /**
     * 释放分布式锁
     */
    public boolean release(String path) {
        try {
            String keyPath = ROOT_PATH_LOCK + "/" + path;
            if (curatorFramework.checkExists().forPath(keyPath) != null) {
                curatorFramework.delete().forPath(keyPath);
            }
        } catch (Exception e) {
            log.error("failed to release lock");
            return false;
        }
        return true;
    }

    /**
     * 创建watcher事件
     */
    private void addWatcher(String path) throws Exception {
        final PathChildrenCache cache = new PathChildrenCache(curatorFramework, path, false);
        cache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        cache.getListenable().addListener((client, event) -> {
            if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
                String oldPath = event.getData().getPath();
                log.info("success to release lock for path: {}", oldPath);
                if (oldPath.contains(path)) {
                    // 释放计数器，让当前请求获取锁
                    countDownLatch.countDown();
                }
            }
        });
    }

    /**
     * 获取下一个id
     */
    public Long nextId(String path) {
        String keyPath = ROOT_PATH_ID + "/" + path;
        Long id = null;
        try {
            DistributedAtomicLong atomicLong = new DistributedAtomicLong(curatorFramework, keyPath,
                    new RetryNTimes(wrapperZk.getRetryCount(), wrapperZk.getElapsedTimeMs()));
            AtomicValue<Long> sequence = atomicLong.increment();
            if (sequence.succeeded()) {
                id = sequence.postValue();
                log.info("threadId={}, sequence={}", Thread.currentThread().getId(), id);
            } else {
                log.warn("threadId={}, no sequence", Thread.currentThread().getId());
            }
        } catch (Exception e) {
            log.error("acquire section exception.", e);
        }
        return id;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            if (curatorFramework.checkExists().forPath(ROOT_PATH_LOCK) == null) {
                curatorFramework.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath(ROOT_PATH_LOCK);
                log.info("root lock path 节点创建成功");
            }
            addWatcher(ROOT_PATH_LOCK);
            log.info("root lock path 的 watcher 事件创建成功");

            if (curatorFramework.checkExists().forPath(ROOT_PATH_ID) == null) {
                curatorFramework.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath(ROOT_PATH_ID);
                log.info("root id path 创建成功");
            }
        } catch (Exception e){
            log.error("connect zookeeper fail，please check the log >> {}", e.getMessage(), e);
            throw e;
        }
    }
}
