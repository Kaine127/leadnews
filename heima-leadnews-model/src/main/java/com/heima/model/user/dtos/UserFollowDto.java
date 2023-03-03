package com.heima.model.user.dtos;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class UserFollowDto {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long articleId;
    private Long authorId;
    private Short operation;
}
