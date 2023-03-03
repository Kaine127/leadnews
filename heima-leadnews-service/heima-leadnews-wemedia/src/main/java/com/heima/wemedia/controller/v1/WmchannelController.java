package com.heima.wemedia.controller.v1;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.wemedia.service.WmChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/channel")
public class WmchannelController {
    @Autowired
    private WmChannelService wmMaterialService;

    @GetMapping("/channels")
    public ResponseResult findAll(){
        List<WmChannel> list = wmMaterialService.list(Wrappers.<WmChannel>lambdaQuery()
                .orderByAsc(WmChannel::getOrd, WmChannel::getCreatedTime));

        return ResponseResult.okResult(list);
    }
}
