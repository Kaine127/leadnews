package com.heima.model.wemedia.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WmDownOrUpMsgDto {
    private long id;
    /**
     * 上下架 0下架 1上架
     */
    private Short enable;

    public void WmDownOrUpMsgDto(){

    }
}
