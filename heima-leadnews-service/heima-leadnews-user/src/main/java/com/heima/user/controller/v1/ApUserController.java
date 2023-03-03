package com.heima.user.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.UserFollowDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.service.ApUserService;
import com.heima.utils.thread.AppThreadLocalUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/user")
@Api(value = "app端用户接口",tags = "app端用户登录")
public class ApUserController {

    @Autowired
    private ApUserService apUserService;

    @PostMapping("/user_follow")
    @ApiOperation("关注接口")
    public ResponseResult userFollow(@RequestBody UserFollowDto dto){

        //1.检验参数
        if (dto.getArticleId() == null || dto.getAuthorId() == null || dto.getOperation() == null
        || dto.getOperation() > 1 || dto.getOperation() < 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //1.1检验用户是否登录
        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        boolean b = apUserService.userFollow(dto);
        if (b){
            //已经关注
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST,"已经关注,请不要再次点击!");
        }
        return  ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
