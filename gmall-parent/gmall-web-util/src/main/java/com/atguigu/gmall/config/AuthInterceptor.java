package com.atguigu.gmall.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

@Component
public class AuthInterceptor  extends HandlerInterceptorAdapter {
    //加入拦截器之前
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String sb = request.getRequestURL().toString();
        System.out.println("getRequestURL(): " +sb );

        System.out.println("*************拦截器拦截了************");
        String token = request.getParameter("newToken");
        //将tocken放入到cookie里面
//        Cookie token = new Cookie("token", token);
//        response.addCookie(token);
        if (token != null){
            CookieUtil.setCookie(request,response,"token",token,WebConst.COOKIE_MAXAGE,false);
        }
        //当用户访问非登陆之后的页面时，登陆之后，继续访问其他业务模块时，url并没有 newToken ，
        // 但是后太可能已经将 token 放入到cookie中了
        if (token == null){
            token=CookieUtil.getCookieValue(request,"token",false);
        }

        //从cookie中获取tocken,解密
        if (token != null){
            //读取token
            Map map = getUserMapByToken(token);
            String nickName = (String) map.get("nickName");
            request.setAttribute("nickName", nickName);
            System.out.println("*********拦截器中的nickName " + nickName + "**********");
        }

        //在拦截器中获取方法上的注解
        HandlerMethod handlerMethod = (HandlerMethod)handler;
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
        if (methodAnnotation != null){
            //此时有注解@Retention，表示需要访问这个业务需要先登陆
            //则需要调用 passport.atguigu.com/verify？token=xxx&salt=xxx
            String salt = request.getHeader("X-forwarded-for");
            String verifyResult = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&salt=" + salt);
            if (verifyResult.equals("success")){
                //认证通过，表明已经登陆了，可以访问当前业务
                //保存一下userId
                Map map = getUserMapByToken(token);
                String userId = (String) map.get("userId");
                request.setAttribute("userId", userId);
                return true;
            }else{
                //认证失败。并且 @LoginRequire(autoRedirect = true)，则必须登陆
                if (methodAnnotation.autoRedirect()){
                    //必须登陆，跳转到登陆页面
                    String requestUrl = request.getRequestURL().toString(); //  requestUrl 类似 http://item.gmall.com/42.html
                    System.out.println(" requestUrl : " + requestUrl);
                    //将 requestUrl转码
                    String encodeRequestUrl = URLEncoder.encode(requestUrl, "UTF-8"); //encodeRequestUrl 类似 http%3A%2F%2Fitem.gmall.com%2F42.html
                    System.out.println("encodeRequestUrl : " + encodeRequestUrl);
                    response.sendRedirect(WebConst.LOGIN_ADDRESS + "?originUrl=" + encodeRequestUrl);
                    return false;
                }
            }
        }
        return true;
    }

    private Map getUserMapByToken(String token) {
        //token: eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkF0Z3VpZ3UiLCJ1c2VySWQiOiIxIn0.UDfFHpBWjyZRqCHVdWmzZtD4alF02yOrY5njSwd2UG0
        //获取token的中间部分
        String tokenUserInfo = StringUtils.substringBetween(token,".");
        //将 tokenUserInfo 进行base64解码v
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] decodeUserInfo = base64UrlCodec.decode(tokenUserInfo);
        //需要 先将decodeUserInfo  转换为字符串，然后才能转换为map
        String userInfoMapJson = null;
        try {
            userInfoMapJson = new String(decodeUserInfo,"utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return JSON.parseObject(userInfoMapJson,Map.class);
    }

    //具体怎么处理
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        super.postHandle(request, response, handler, modelAndView);
    }

    //最后
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        super.afterCompletion(request, response, handler, ex);
    }
}
