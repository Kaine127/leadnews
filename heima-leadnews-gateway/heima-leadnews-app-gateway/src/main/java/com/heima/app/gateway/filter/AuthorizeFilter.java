package com.heima.app.gateway.filter;


import com.heima.app.gateway.util.AppJwtUtil;
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
//
//        //3.获取token
//        String token = request.getHeaders().getFirst("token");
//
//        //4.判断token是否存在
//        if(StringUtils.isBlank(token)){
//            response.setStatusCode(HttpStatus.UNAUTHORIZED);
//            //关闭请求返回数据给前端
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
//        }catch (Exception e){
//            //token不是合法jwt
//            e.printStackTrace();
//            response.setStatusCode(HttpStatus.UNAUTHORIZED);
//            return response.setComplete();
//        }
//
//        //6.放行
//        return chain.filter(exchange);

        //1.获得request和response
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        //2.判断是否是登录请求
        if (request.getURI().getPath().contains("/login")){
            //是登录请求 , 放行
            return chain.filter(exchange);
        }

        //不是登录请求 , 查看是否有token
        String token = request.getHeaders().getFirst("token");
        if (StringUtils.isBlank(token)){
            //没有token进行 设置response的状态码
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            //返回response
            return response.setComplete();
        }

        try {
            //如果有token , 验证token是否过期
            Claims claimsBody = AppJwtUtil.getClaimsBody(token);
            int result = AppJwtUtil.verifyToken(claimsBody);

            if (result == 1|| result == 2){
                //如果token失效了 , 那就设置response的状态码 然后返回
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            //获取用户信息
            Object userId = claimsBody.get("id");

            //存储到header中
            ServerHttpRequest serverHttpRequest = request.mutate().headers(httpHeaders ->
                    httpHeaders.add("userId", userId + "")).build();
            //重置请求
            exchange.mutate().request(serverHttpRequest);
        } catch (Exception e) {
            e.printStackTrace();
            //出错的情况, 也要进行授权失败的返回
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        //有token , 且没有失效的情况, 放行
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
