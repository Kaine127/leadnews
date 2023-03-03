package com.heima.article.listener;

import com.alibaba.fastjson.JSON;
import com.heima.article.service.ApArticleConfigService;
import com.heima.common.constants.WmNewsMessageConstants;
import com.heima.model.wemedia.dtos.WmDownOrUpMsgDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ArticleIsDownListener {

    @Autowired
    private ApArticleConfigService apArticleConfigService;
    //指定接收哪些主题
    @KafkaListener(topics = WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC)
    public void onMessage(String message){
        if (StringUtils.isNotBlank(message)){
            WmDownOrUpMsgDto wmDownOrUpMsgDto = JSON.parseObject(message, WmDownOrUpMsgDto.class);
            apArticleConfigService.updateUpOrDown(wmDownOrUpMsgDto.getId(),wmDownOrUpMsgDto.getEnable());
        }

    }
}
