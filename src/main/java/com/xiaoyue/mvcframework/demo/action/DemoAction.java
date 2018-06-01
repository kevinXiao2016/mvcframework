package com.xiaoyue.mvcframework.demo.action;

import com.xiaoyue.mvcframework.framework.annocation.MyAutowire;
import com.xiaoyue.mvcframework.framework.annocation.MyController;
import com.xiaoyue.mvcframework.framework.annocation.MyRequestMapping;
import com.xiaoyue.mvcframework.framework.annocation.MyRequestParameter;
import com.xiaoyue.mvcframework.demo.service.IDemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @program: mvcframework
 * @description: Demo
 * @author: xiaoyue
 * @create: 2018-05-25 16:53
 **/
@MyController
@MyRequestMapping("/demo")
public class DemoAction {

    @MyAutowire
    private IDemoService iDemoService;

    @MyRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response, @MyRequestParameter("name") String name) {

        String result = iDemoService.get(name);

        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @MyRequestMapping("/add")
    public void add(HttpServletRequest request, HttpServletResponse response, @MyRequestParameter("a") Integer a,@MyRequestParameter("b") Integer b) {

        Integer c = a + b;
        try {
            response.getWriter().write(a + "+" + b + "=" + c);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @MyRequestMapping("/remove")
    public void remove(HttpServletRequest request, HttpServletResponse response, @MyRequestParameter("name") String name) {

        String result = iDemoService.get(name);

        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
