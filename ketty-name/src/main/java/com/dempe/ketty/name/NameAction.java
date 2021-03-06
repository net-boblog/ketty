package com.dempe.ketty.name;

import com.alibaba.fastjson.JSONArray;
import com.dempe.ketty.common.utils.JSONResult;
import com.dempe.ketty.mvc.anno.Action;
import com.dempe.ketty.mvc.anno.Path;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Dempe
 * Date: 2015/12/10
 * Time: 14:36
 * To change this template use File | Settings | File Templates.
 */
@Action
public class NameAction {

    private NameManager nameManager = new NameManager();

    /**
     * 注册服务接口
     *
     * @param name
     * @param host
     * @param port
     * @return
     */
    @Path
    public String registerServerByName(String name, String host, int port) {
        JSONResult result = new JSONResult();
        result.putResult(nameManager.registerServer(name, host, port).getCode());
        return result.toJSONString();
    }


    /**
     * 注销服务接口
     *
     * @param name
     * @param host
     * @param port
     * @return
     */
    @Path
    public String deRegisterServerByName(String name, String host, int port) {
        JSONResult result = new JSONResult();
        result.putResult(nameManager.deRegisterServer(name, host, port).getCode());
        return result.toJSONString();
    }


    /**
     * 通过名称获取服务接口
     *
     * @param name
     * @return
     */
    @Path
    public String listServerByName(String name) {
        List<NodeInfo> nodeInfoList = nameManager.listServerByName(name);
        return JSONArray.toJSONString(nodeInfoList);
    }


}
