package com.example.demo;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * IP过滤器
 * OncePerRequestFilter：可以对同一个请求，只经过一次过滤
 * @author kelly
 * @create 2020/9/14 3:02 下午
 */
@Component
public class IPFilter extends OncePerRequestFilter {

    private final RedisTemplate<Object, Object> redisTemplate;

    public IPFilter(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * IP黑名单
     * 模拟数据库
     */
    private static final Set<String> BLACKLISTS = new HashSet<>();

    static {
        BLACKLISTS.add("192.168.0.54");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        // 用户真是IP
        String ip = getIpAddress(httpServletRequest);
        // 用户请求的接口
        String uri = httpServletRequest.getRequestURI();
        // Redis key
        String key = ip + uri;

        // 黑名单用户，拒绝访问
        if (ipBlacklist(ip)){
            System.out.println("黑名单IP，拒绝访问。");
            return;
        }

        // 第一次访问的用户
        if (redisTemplate.opsForValue().get(key) == null) {
            redisTemplate.opsForValue().set(key,1L,1L, TimeUnit.MINUTES);
            filterChain.doFilter(httpServletRequest,httpServletResponse);
            return;
        }
        // 访问次数递增
        Long count = redisTemplate.opsForValue().increment(key);


        int limit = 100;
        // 当前IP在一分钟内访问相同接口次数多于1000次，因此将此IP列入黑名单
        // 并拒绝访问
        if (limit < count) {
            System.out.println("IP：" + ip + "，访问同一个接口频率过高，已列入黑名单。");
            blacklisted(ip);
            return;
        }
        filterChain.doFilter(httpServletRequest,httpServletResponse);
    }


    /**
     * 将IP列入黑名单
     * @param ip
     */
    private void blacklisted(String ip){
        // TODO 写入数据库
        BLACKLISTS.add(ip);
    }


    /**
     * 判断IP是否在黑名单列表中
     * @param ip
     * @return
     */
    public boolean ipBlacklist(String ip){
        // TODO 读取数据库
        return BLACKLISTS.contains(ip);
    }


    /**
     * 获取用户真实IP地址
     * @param request
     * @return
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            if("127.0.0.1".equals(ip)||"0:0:0:0:0:0:0:1".equals(ip)){
                //根据网卡取本机配置的IP
                InetAddress inet=null;
                try {
                    inet = InetAddress.getLocalHost();
                    ip= inet.getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
        return ip;
    }
}
