package com.yuzhouwan.site.aop;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * Copyright @ yuzhouwan.com
 * All right reserved.
 * Function：SpringAOPWithAspectJ Tester
 *
 * @author Benedict Jin
 * @since 2015/11/9
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/service/spring.service.xml")
public class SpringAOPWithAspectJTest extends AbstractJUnit4SpringContextTests {

    @Resource
    private TargetAOP targetAOP;

    @Test
    public void test() {

        targetAOP.targetBefore();
        targetAOP.targetAfter();
        targetAOP.targetAfterReturning(true);
        targetAOP.targetAfterReturning(false);
        System.out.println("----------------------------------");
        targetAOP.targetAround();
        System.out.println("----------------------------------");
        try {
            targetAOP.targetAfterThrowing(new RuntimeException("error"));
            targetAOP.targetAfterThrowing(null);
        } catch (Throwable throwable) {
        }
    }

}
