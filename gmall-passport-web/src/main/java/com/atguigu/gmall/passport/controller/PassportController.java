package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.passport.config.JwtUtil;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    private UserService userService;

    @Value("${token.key}")
    private String key;

    @RequestMapping("index")
    public String index(HttpServletRequest request) {

        //获取originUrl
        String originUrl = request.getParameter("originUrl");
        System.out.println("originUrl:" + originUrl);

        request.setAttribute("originUrl", originUrl);
        return "index";
    }

    /**
     * http://localhost:8087/login
     * 控制器获取页面的数据
     *
     * @return
     */
    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo, HttpServletRequest request) {
        //调用登陆方法
        UserInfo info = userService.login(userInfo);
        if (info != null) {
            //制造tocken
            HashMap<String, Object> privateKeyMap = new HashMap<>();//私钥部分
            privateKeyMap.put("userId", info.getId());
            privateKeyMap.put("nickName", info.getNickName());
            String remoteAddr = request.getHeader("X-forwarded-for");
            String tocken = JwtUtil.encode(key, privateKeyMap, remoteAddr);
            System.out.println("tocken:" + tocken);
            return tocken;
        } else {
            return "fail";
        }
    }

    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request) {
        //1. 获取用户的 ip  和tocken
        //2. key + ip 解密 Tocken,得到用户的信息： userId,nickName
        //3. 判断用户是否登陆： key = user:userId:info  value=userInfo
        //4. userInfo != null ? true(success): false(fail)
        String token = request.getParameter("token");

        //调用jwt工具类
//        String remoteAddr = request.getHeader("X-forwarded-for");
//        Map<String, Object> privateKeyMap = JwtUtil.decode(token, key, remoteAddr);
        String salt = request.getParameter("salt");
        Map<String, Object> privateKeyMap = JwtUtil.decode(token, key, salt);
        if (privateKeyMap != null && privateKeyMap.size() > 0){
            String userId = (String)privateKeyMap.get("userId");
            //调用服务层验证用户是否已经登陆了
            UserInfo info = userService.verify(userId);
            if (info != null){
                return "success";
            }
        }
        return "fail";
    }

}
