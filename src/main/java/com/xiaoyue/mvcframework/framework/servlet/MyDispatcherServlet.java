package com.xiaoyue.mvcframework.framework.servlet;

import com.xiaoyue.mvcframework.framework.annocation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: mvcframework
 * @description: 调度servlet
 * @author: xiaoyue
 * @create: 2018-05-25 16:34
 **/
public class MyDispatcherServlet extends HttpServlet {

    private Properties contextConfig = new Properties();
    private List<String> classNames = new ArrayList<>();
    private Map<String, Object> ioc = new HashMap<>();
    //    private Map<String, Method> handlerMapping = new HashMap<>();
    private List<Handler> handlerMapping = new ArrayList<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1、加載配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2、初始化所有相关的类，扫描设定的包下所有的类
        doScanner(contextConfig.getProperty("scanPackage"));

        //3、拿到扫描的类，通过反射实例化，放到IOC容器中（beanname-bean）
        doInstance();

        //4、依赖注入
        doAutowire();

        //5、初始化HandlerMapping（将url和method对应起来）
        initHandlerMapping();


    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500,Exception Detail :" + Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        //6、等 待请求
        Handler handler = getHandler(req);

        if (handler == null) {
            resp.getWriter().write("404 Not Found");
        }

        //获取方法的参数列表
        Class<?>[] parameterTypes = handler.method.getParameterTypes();

        //保存所有需要自动赋值的参数值

        Object[] paramValues = new Object[parameterTypes.length];

        Map<String, String[]> parameterMap = req.getParameterMap();
        for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
            String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "");

            //如果找到匹配的对象，则开始填充数据
            if (!handler.paramIndexMapping.containsKey(param.getKey())) {
                continue;
            }

            Integer index = handler.paramIndexMapping.get(param.getKey());
            paramValues[index] = convert(parameterTypes[index], value);

        }

        //设置方法中的request和response对象
        Integer requestIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
        paramValues[requestIndex] = req;
        Integer responseIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
        paramValues[responseIndex] = resp;

        handler.method.invoke(handler.controller, paramValues);


    }

    private Object convert(Class<?> parameterType, String value) {
        if (Integer.class == parameterType) {
            return Integer.valueOf(value);
        }
        return value;


    }

    private Handler getHandler(HttpServletRequest req) {
        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        String url = requestURI.replace(contextPath, "").replaceAll("/+", "/");

        for (Handler handler : handlerMapping) {
            Matcher matcher = handler.pattern.matcher(url);
            if (!matcher.matches()) {
                continue;
            }
            return handler;
        }

        return null;
    }


    /**
     * @Description: asdasdasd
     * @Param: [contextConfigLocation]
     * @return: void
     * @Author: Mr.Wang
     * @Date: 2018/5/25
     */
    private void doLoadConfig(String contextConfigLocation) {

        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));

        File classdir = new File(url.getFile());
        File[] files = classdir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                String replace = scanPackage + "." + file.getName().replace(".class", "");
                classNames.add(replace);
            }


        }

    }

    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }

        try {
            for (String className : classNames) {

                Class<?> clazz = Class.forName(className);
                //判断是否实例化该类，即是否被MyController及MyService注解标识
                if (clazz.isAnnotationPresent(MyController.class)) {
                    //1、默认beanname
                    String defaultBeanName = lowerFirstCase(clazz.getSimpleName());
                    ioc.put(defaultBeanName, clazz.newInstance());

                    //2、自定义beanname


                    //3、接口类型作为key

                } else if (clazz.isAnnotationPresent(MyService.class)) {


                    MyService service = clazz.getAnnotation(MyService.class);
                    String beanName = service.value();
                    if ("".equals(beanName)) {
                        //1、默认beanname
                        beanName = lowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    //2、自定义beanname
                    ioc.put(beanName, instance);


                    //3、接口类型作为key

                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> anInterface : interfaces) {
                        ioc.put(anInterface.getName(), instance);
                    }


                }

            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    private void doAutowire() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();

            for (Field field : fields) {
                if (!field.isAnnotationPresent(MyAutowire.class)) {
                    return;
                }
                MyAutowire autowire = field.getAnnotation(MyAutowire.class);
                String beanName = autowire.value().trim();
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }

                //强制访问
                field.setAccessible(true);

                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }


            }


        }


    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {

            Class<?> clazz = entry.getValue().getClass();

            if (!clazz.isAnnotationPresent(MyController.class)) {
                return;
            }

            //访问url 由类上和方法上的url组合完成
            String baseUrl = "";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = requestMapping.value();
            }


            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }
                MyRequestMapping mapping = method.getAnnotation(MyRequestMapping.class);
                String regex = baseUrl + mapping.value().replaceAll("/+", "/");

                Pattern pattern = Pattern.compile(regex);

                handlerMapping.add(new Handler(entry.getValue(), method, pattern));
                System.out.println("Mapping " + regex + "," + method);

            }
        }
    }

    private String lowerFirstCase(String str) {
        char[] charArray = str.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }


    private class Handler {
        protected Object controller; // 方法对应的Controller类实例
        protected Method method; //方法
        protected Pattern pattern;
        protected Map<String, Integer> paramIndexMapping; //参数顺序


        public Handler(Object controller, Method method, Pattern pattern) {
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;

            paramIndexMapping = new HashMap<>();
            putParamIndexMapping(method);

        }

        private void putParamIndexMapping(Method method) {
            //提取方法中加了注解的参数

            Annotation[][] pas = method.getParameterAnnotations();

            for (int i = 0; i < pas.length; i++) {
                for (Annotation annotation : pas[i]) {
                    if (annotation instanceof MyRequestParameter) {
                        String paramName = ((MyRequestParameter) annotation).value();
                        if (!"".equals(paramName.trim())) {
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }

            //提起方法中request和response的参数
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];

                if (parameterType == HttpServletRequest.class || parameterType == HttpServletResponse.class) {
                    paramIndexMapping.put(parameterType.getName(), i);
                }

            }

        }
    }

}
