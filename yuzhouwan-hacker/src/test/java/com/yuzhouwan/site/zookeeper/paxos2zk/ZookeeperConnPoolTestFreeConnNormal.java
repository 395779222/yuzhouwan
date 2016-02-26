package com.yuzhouwan.site.zookeeper.paxos2zk;

import com.yuzhouwan.site.hacker.zookeeper.paxos2zk.ZooKeeperConnPool;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Copyright @ 2015 yuzhouwan.com
 * All right reserved.
 * Function: ZooKeeperConnPool Tester
 *
 * @author Benedict Jin
 * @since 2015/12/8 0008
 */
public class ZookeeperConnPoolTestFreeConnNormal {

    @Test
    public void testFreeConnNormal() throws InterruptedException {
        ZooKeeperConnPool zookeeperConnPool = ZooKeeperConnPool.getInstance();

        ZooKeeper zooKeeper = zookeeperConnPool.getConn();
        isAlive(zooKeeper);
        zookeeperConnPool.freeConn(zooKeeper);
        isAlive(zooKeeper);
    }

    private void isAlive(ZooKeeper zooKeeper) {
        assertEquals(true, zooKeeper.getState().isAlive());
    }
}