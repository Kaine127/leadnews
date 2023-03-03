package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.LoginDto;
import com.heima.model.user.dtos.UserFollowDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.service.ApUserService;
import com.heima.user.vo.LoginUserVo;
import com.heima.utils.common.AppJwtUtil;
import com.heima.utils.common.MD5Utils;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.dynamic.TypeResolutionStrategy;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;


@Service
@Transactional
@Slf4j
public class ApUserServiceImpl extends ServiceImpl<ApUserMapper, ApUser> implements ApUserService {
    /**
     * app端登录功能
     * @param dto
     * @return
     */
    @Override
    public ResponseResult login(LoginDto dto) {
//        //1.正常登录 用户名和密码
//        if(StringUtils.isNotBlank(dto.getPhone()) && StringUtils.isNotBlank(dto.getPassword())){
//            //1.1 根据手机号查询用户信息
//            ApUser apUser = getOne(Wrappers.<ApUser>lambdaQuery().eq(ApUser::getPhone, dto.getPhone()));
//            if(apUser == null){
//                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"用户或密码错误");
//            }
//
//            //1.2 比对密码
//            String salt = apUser.getSalt();
//            String password = dto.getPassword();
//            String pswd = DigestUtils.md5DigestAsHex((password + salt).getBytes());
//            if(!pswd.equals(apUser.getPassword())){
//                return ResponseResult.errorResult(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
//            }
//
//            //1.3 返回数据  jwt  {code msg data: {user : 用户数据,token : 令牌jwt} }
//            LoginUserVo loginUserVo = new LoginUserVo();
//            apUser.setPassword("");
//            apUser.setSalt("");
//            loginUserVo.setUser(apUser);
//            loginUserVo.setToken(AppJwtUtil.getToken(apUser.getId().longValue()));
//
//            return ResponseResult.okResult(loginUserVo);
//        }else {
//            //2.游客登录
//            LoginUserVo loginUserVo = new LoginUserVo();
//            loginUserVo.setToken(AppJwtUtil.getToken(0L));
//            return ResponseResult.okResult(loginUserVo);
//        }
        //判断是否为正常用户登录
        if (StringUtils.isNotBlank(dto.getPhone())&&StringUtils.isNotBlank(dto.getPassword())){
            //有手机号和密码, 为正常登录 , 看看是否在user表中有对应的用户
            LambdaQueryWrapper<ApUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ApUser::getPhone,dto.getPhone());
            ApUser user = this.getOne(queryWrapper);
            if (user == null){
                //没有对应的user对象 , 返回错误结果集
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"用户不存在");
            }
            //user对象存在 , 对比密码
            String password = dto.getPassword();
            String salt = user.getSalt();
            String passwordMd5 = DigestUtils.md5DigestAsHex((password + salt).getBytes());
            if (!passwordMd5.equals(user.getPassword())){
                //密码不相等
                return ResponseResult.errorResult(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR,"密码错误");
            }

            //密码正确的情况 , 进行jwt生成和保存 ,保存user对象
            String token = AppJwtUtil.getToken(user.getId().longValue());
            //将user的密码和盐值设置为空
            user.setSalt("");
            user.setPassword("");
            //将user和token封装成一个对象
            LoginUserVo loginUserVo = new LoginUserVo();
            loginUserVo.setToken(token);
            loginUserVo.setUser(user);
            return ResponseResult.okResult(loginUserVo);
        }else {
            //不是正常登录 , 采用游客措施
            LoginUserVo loginUserVo = new LoginUserVo();
            String token = AppJwtUtil.getToken(0L);
            return ResponseResult.okResult(loginUserVo);
        }

    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean userFollow(UserFollowDto dto) {
        //2.关注数据存储到redis
        //我到作者 1-> 多
        //作者到粉丝 1->多 set
        Integer userId = AppThreadLocalUtil.getUser().getId();
        String followKey = "leadnews:user:follow" + userId;
        String fansKey = "leadnews:user:fans" + dto.getAuthorId();

        if (dto.getOperation() == 0){
            //添加校验是否已经关注
            Boolean member = stringRedisTemplate.opsForSet().isMember(followKey, dto.getAuthorId().toString());
            if (member != null && member){
                //已经关注了
                return false;
            }
            //关注
            stringRedisTemplate.opsForSet().add(followKey,dto.getAuthorId().toString());
            stringRedisTemplate.opsForSet().add(fansKey,userId.toString());
        }else {
            //取消
            stringRedisTemplate.opsForSet().remove(followKey,dto.getAuthorId().toString());
            stringRedisTemplate.opsForSet().remove(fansKey,userId.toString());
        }

        return true;

    }
}
