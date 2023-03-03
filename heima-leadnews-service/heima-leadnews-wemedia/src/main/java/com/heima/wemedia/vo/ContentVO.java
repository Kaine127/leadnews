package com.heima.wemedia.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.codehaus.jackson.annotate.JsonProperty;

@NoArgsConstructor
@Data
public class ContentVO {
    @JsonProperty("type")
    private String type;
    @JsonProperty("value")
    private String value;
}
