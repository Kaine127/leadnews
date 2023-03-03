package com.heima.user.vo;

import com.heima.model.user.pojos.ApUser;
import lombok.Data;

@Data
public class LoginUserVo {
    //返回用户信息,密码和盐都需要置为空
    private ApUser user; //前端需要的参数是user
    private String token;

}
