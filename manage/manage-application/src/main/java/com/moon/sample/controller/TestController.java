package com.moon.sample.controller;

import com.moon.sample.service.TestService;
import com.moon.sample.template.CuratorTemplate;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @Reference
    TestService testService;

    @Autowired
    CuratorTemplate curatorTemplate;

    @GetMapping("/get1")
    public Object get1() {
        return testService.get();

    }

    @GetMapping("/lock")
    public void lock() throws InterruptedException {
        curatorTemplate.lock("order");
        Thread.currentThread().sleep(3000);
        curatorTemplate.release("order");
    }

    @GetMapping("/nextId")
    public Object nextId() {
        return curatorTemplate.nextId("order");
    }
}
