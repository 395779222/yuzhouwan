package com.yuzhouwan.site.service.rpc.core;

import com.yuzhouwan.site.api.rpc.model.Call;
import com.yuzhouwan.site.api.rpc.service.Server;
import com.yuzhouwan.site.service.rpc.connection.Client;
import com.yuzhouwan.site.service.rpc.connection.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Copyright @ 2016 yuzhouwan.com
 * All right reserved.
 * Function：RPC
 *
 * @author Benedict Jin
 * @since 2016/9/1
 */
public class RPC {

    private static final Logger _log = LoggerFactory.getLogger(RPC.class);

    /**
     * @param clazz 提供服务的接口，便于服务端检验是否提供了此服务
     * @param host  服务端host
     * @param port  服务端port
     * @return 代理对象
     **/
    @SuppressWarnings("unchecked")
    public static <T> T getProxy(final Class<T> clazz, String host, int port) {
        final Client client = new Client(host, port);
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            Call call = new Call();
            call.setInterfaces(clazz);
            call.setMethodName(method.getName());
            call.setParams(args);
            call.setParameterTypes(method.getParameterTypes());
            client.invokeCall(call);
            return call.getResult(); //返回计算结果
        };
        return (T) Proxy.newProxyInstance(RPC.class.getClassLoader(), new Class[]{clazz}, handler);
    }

    public static class RPCServer implements Server {

        private int port = 20222;
        Map<String, Object> serviceEntry = new HashMap<>(); //键是服务，值是具体实现
        boolean isRunning = false;

        @Override
        public void start() {
            this.isRunning = true;
            new Thread(new Listener(this)).start();
        }

        @Override
        public void stop() {
            this.isRunning = false;
        }

        //注册服务
        @Override
        public void register(Class serviceInterface, Class ServiceImp) {
            if (!serviceEntry.containsKey(serviceInterface.getName())) {
                try {
                    serviceEntry.put(serviceInterface.getName(), ServiceImp.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        //调用服务端的具体实现
        @Override
        public void call(Call call) {
            String interfaceName = call.getInterfaces().getName();
            Object object = serviceEntry.get(interfaceName);
            if (object != null) {
                try {
                    Class<?> clazz = object.getClass();
                    Method method = clazz.getMethod(call.getMethodName(), call.getParameterTypes());
                    Object result = method.invoke(object, call.getParams());
                    call.setResult(result);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                _log.warn("Not registered this interface: {}!", interfaceName);
            }
        }

        @Override
        public boolean isRunning() {
            return this.isRunning;
        }

        @Override
        public int getPort() {
            return this.port;
        }
    }
}
