package com.moon.sample.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CuratorTemplateConfig {

    @Autowired
    WrapperZk wrapperZk;

    @Bean
    public CuratorFramework curatorFramework() {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient(
                wrapperZk.getConnectString(),
                wrapperZk.getSessionTimeoutMs(),
                wrapperZk.getConnectionTimeoutMs(),
                new RetryNTimes(wrapperZk.getRetryCount(), wrapperZk.getElapsedTimeMs()));
        curatorFramework.start();
        return curatorFramework;
    }
}
