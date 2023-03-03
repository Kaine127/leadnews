package com.heima.model.wemedia.dtos;

import lombok.Data;

@Data
public class WmDownOrUpDto {
    private Integer id;
    /**
     * 上下架 0下架 1上架
     */
    private Short enable;
}
