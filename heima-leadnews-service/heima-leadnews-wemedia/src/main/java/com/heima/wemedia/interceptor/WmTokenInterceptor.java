package com.heima.wemedia.interceptor;

import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.thread.WmThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;
@Slf4j
public class WmTokenInterceptor implements HandlerInterceptor {

    /**
     * 得到header中的用户信息,并且存入到当前线程中
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

//        //得到header中的信息
//        String userId = request.getHeader("userId");
//        Optional<String> optional = Optional.ofNullable(userId);
//        if (optional.isPresent()){
//            //存入当前线程中
//            WmUser wmUser = new WmUser();
//            wmUser.setId(Integer.valueOf(userId));
//            WmThreadLocalUtil.setUser(wmUser);
//            log.info("wmTokenFilter设置用户信息到threadlocal中");
//        }
//
//        return true;

        //先从Header中获取用户id ,该id是从网关中加入的
        String userId = request.getHeader("userId");
        Optional<String> optional = Optional.ofNullable(userId);
        if (optional.isPresent()){
            //userId不是空, 创建WmUser对象 存入ThreadLocal中
            WmUser wmUser = new WmUser();
            wmUser.setId(Integer.valueOf(userId));
            WmThreadLocalUtil.setUser(wmUser);
            log.info("wmTokenFilter设置用户信息到threadlocal中");
        }

        return true;
    }


    /**
     * 清理数据
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        log.info("清理threadlocal...");
        WmThreadLocalUtil.clear();
    }
}
