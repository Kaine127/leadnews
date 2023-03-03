package com.heima.wemedia.gateway.filter;


import com.heima.wemedia.gateway.util.AppJwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthorizeFilter implements Ordered, GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        //1.获取request和response对象
//        ServerHttpRequest request = exchange.getRequest();
//        ServerHttpResponse response = exchange.getResponse();
//
//        //2.判断是否是登录
//        if(request.getURI().getPath().contains("/login")){
//            //放行
//            return chain.filter(exchange);
//        }
//
//        //3.获取token
//        String token = request.getHeaders().getFirst("token");
//
//        //4.判断token是否存在
//        if(StringUtils.isBlank(token)){
//            response.setStatusCode(HttpStatus.UNAUTHORIZED);
//            return response.setComplete();
//        }
//
//        //5.判断token是否有效
//        try {
//            Claims claimsBody = AppJwtUtil.getClaimsBody(token);
//            //是否是过期
//            int result = AppJwtUtil.verifyToken(claimsBody);
//            if(result == 1 || result  == 2){
//                response.setStatusCode(HttpStatus.UNAUTHORIZED);
//                return response.setComplete();
//            }
//            //获取用户信息
//            Object userId = claimsBody.get("id");
//
//            //存储Header中
//            ServerHttpRequest serverHttpRequest = request.mutate().headers(httpHeaders ->
//                    httpHeaders.add("userId", userId + "")).build();
//
//            //存储完成后,重置请求
//            exchange.mutate().request(serverHttpRequest);
//
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        //6.放行
//        return chain.filter(exchange);

        //获取request和response
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        //查看是否是登录请求
        if (request.getURI().getPath().contains("/login")){
            //如果是登录请求, 放行
            return chain.filter(exchange);
        }
        //不是登录请求, 查看是否有token
        String token = request.getHeaders().getFirst("token");
        if (StringUtils.isEmpty(token)){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        //有token 查看是都失效
        try {
            Claims claimsBody = AppJwtUtil.getClaimsBody(token);

            if (claimsBody ==null){
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            int result = AppJwtUtil.verifyToken(claimsBody);
            if (result == 1 || result == 2){
                //token已经失效了
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            //获得用户信息
            Object userId = claimsBody.get("id");
            //将用户信息存储到Header中, 进行request的重新封装
            ServerHttpRequest serverHttpRequest = request.mutate().headers(httpHeaders -> httpHeaders.add("userId", userId + "")).build();
            //重置Header, 进行exchange的重新封装,exchange全局只有一个, 所以不用重新复制
            exchange.mutate().request(serverHttpRequest).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //不是登录, token验证成功, 放行
        return chain.filter(exchange);
    }

    /**
     * 优先级设置  值越小  优先级越高
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
