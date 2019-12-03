package com.moon.sample.service;

import com.moon.sample.domain.User;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.stereotype.Component;

@Service
@Component
public class TestServiceImpl implements TestService {

    @Override
    public User get() {
        User user = new User();
        user.setId(1);
        user.setName("张三");
        return user;
    }
}
