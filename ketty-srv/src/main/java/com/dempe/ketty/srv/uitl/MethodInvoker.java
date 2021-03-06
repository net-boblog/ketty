package com.dempe.ketty.srv.uitl;

import com.dempe.ketty.srv.core.ActionMethod;
import com.dempe.ketty.srv.interceptor.KettyInterceptor;
import com.google.common.util.concurrent.RateLimiter;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Dempe
 * Date: 2015/11/4
 * Time: 10:06
 * To change this template use File | Settings | File Templates.
 */
public class MethodInvoker {


    public static Object interceptorInvoker(ActionMethod actionMethod, Object[] parameterValues)
            throws InvocationTargetException, IllegalAccessException {

        // 速率限定
        RateLimiter rateLimiter = actionMethod.getRateLimiter();
        if (rateLimiter != null) {
            rateLimiter.acquire();
        }

        List<KettyInterceptor> interceptorList = actionMethod.getInterceptorList();
        Iterator<KettyInterceptor> interceptorIterator = interceptorList.iterator();
        boolean flag = true;
        while (interceptorIterator.hasNext() && flag) {
            KettyInterceptor interceptor = interceptorIterator.next();
            flag = interceptor.before();
        }
        if (!flag) {
            return null;
        }
        // 拦截器前
        Object result = actionMethod.call(parameterValues);
        interceptorIterator = interceptorList.iterator();
        // 拦截器后
        while (interceptorIterator.hasNext() && flag) {
            KettyInterceptor interceptor = interceptorIterator.next();
            flag = interceptor.after();
        }
        return result;
    }
}
