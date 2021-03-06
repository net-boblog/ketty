package com.dempe.ketty.srv.core;

import com.dempe.ketty.common.utils.PackageUtils;
import com.dempe.ketty.mvc.anno.*;
import com.dempe.ketty.mvc.ioc.Injector;
import com.dempe.ketty.srv.KettyServer;
import com.dempe.ketty.srv.interceptor.KettyInterceptor;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: zhengdaxia
 * Date: 15/10/17
 * Time: 上午10:41
 * To change this template use File | Settings | File Templates.
 */
public class RequestMapping {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestMapping.class);

    public Map<String, ActionMethod> mapping = new ConcurrentHashMap<String, ActionMethod>();

    public Map<String, KettyInterceptor> interceptorMap = new ConcurrentHashMap<String, KettyInterceptor>();

    private KettyServer.Builder builder;

    public RequestMapping(KettyServer.Builder builder) {
        this.builder = builder;
        initInterceptorMap();
        initMapping();
    }


    public void initInterceptorMap() {
        List<String> packages = new ArrayList<String>();
        packages.add(builder.getPackageName());
        for (String scanPackage : packages) {
            String[] classNames = PackageUtils.findClassesInPackage(scanPackage + ".*"); // 目录下通配
            for (String className : classNames) {
                try {
                    Class<?> actionClass = Class.forName(className);
                    Around around = actionClass.getAnnotation(Around.class);
                    if (around == null) {
                        continue;
                    }
                    String id = around.id();
                    if (StringUtils.isBlank(id)) {
                        id = StringUtils.uncapitalize(actionClass.getSimpleName());
                    }
                    KettyInterceptor target = (KettyInterceptor) actionClass.newInstance();
                    Injector.doInject(target);
                    interceptorMap.put(id, target);
                    LOGGER.info("registering interceptor id: {} , ", id);

                } catch (ClassNotFoundException e) {
                    LOGGER.error("FAIL to initiate handle instance", e);
                } catch (Exception e) {
                    LOGGER.error("FAIL to initiate handle instance", e);
                }

            }
        }

    }

    /**
     * 扫描packet下面所有的映射，初始化mapping
     */

    public void initMapping() {
        LOGGER.info("handles begin initiating");
        List<String> packages = new ArrayList<String>();

        packages.add(builder.getPackageName());
        LOGGER.info("scanned packages : " + packages);
        for (String scanPackage : packages) {
            LOGGER.info("begin get classes from package : " + scanPackage);
            String[] classNames = PackageUtils.findClassesInPackage(scanPackage + ".*"); // 目录下通配
            for (String className : classNames) {
                try {
                    Class<?> actionClass = Class.forName(className);
                    Action action = actionClass.getAnnotation(Action.class);
                    if (action == null) {
                        continue;
                    }
                    String actionVal = action.value();
                    if (StringUtils.isBlank(actionVal)) {
                        actionVal = StringUtils.uncapitalize(actionClass.getSimpleName());
                    }
                    LOGGER.info("registering action  : " + actionVal);
                    for (Method method : actionClass.getDeclaredMethods()) {
                        if (method.getModifiers() == Modifier.PUBLIC) {
                            Path refs = method.getAnnotation(Path.class);
                            if (refs != null) {
                                String pathVal = String.valueOf(refs.value());
                                if (StringUtils.isBlank(pathVal)) {
                                    pathVal = method.getName();
                                }
                                String uri = "/" + actionVal + "/" + pathVal;
                                if (mapping.containsKey(uri)) {
                                    LOGGER.warn("Method:{} declares duplicated uri:{}, previous one will be overwritten", method, uri);
                                }

                                makeAccessible(method);

                                /**
                                 * 这里注入action里面的依赖
                                 */
                                Object target = actionClass.newInstance();
                                Injector.doInject(target);

                                ActionMethod actionMethod = new ActionMethod(target, method);
                                Interceptor interceptor = method.getAnnotation(Interceptor.class);
                                if (interceptor != null) {
                                    String ids = interceptor.id();
                                    String idArray[] = ids.split(",");
                                    for (String id : idArray) {
                                        KettyInterceptor kettyInterceptor = interceptorMap.get(id);
                                        if (kettyInterceptor == null) {
                                            LOGGER.error("interceptor id :{} is not found !", id);
                                        } else {
                                            actionMethod.addInterceptor(kettyInterceptor);
                                        }
                                    }
                                }


                                // RateLimiter
                                Rate rate = method.getAnnotation(Rate.class);
                                if (rate != null) {
                                    int value = rate.value();
                                    if (value < 0) {
                                        LOGGER.warn("rate value:{} < 0 ,set 1000 as default. ", value);
                                        value = 1000;
                                    }
                                    actionMethod.setRateLimiter(RateLimiter.create(value));
                                }
                                LOGGER.info("[REQUEST MAPPING] = {}, uri = {}", actionVal, uri);
                                mapping.put(uri, actionMethod);
                            }
                        }
                    }

                } catch (ClassNotFoundException e) {
                    LOGGER.error("FAIL to initiate handle instance", e);
                } catch (Exception e) {
                    LOGGER.error("FAIL to initiate handle instance", e);
                }
            }
        }
        LOGGER.info("Handles  Initialization successfully");
    }

    public ActionMethod tack(String uri) {
        return mapping.get(uri);
    }


    protected void makeAccessible(Method method) {
        if ((!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers()))
                && !method.isAccessible()) {
            method.setAccessible(true);
        }
    }

}
