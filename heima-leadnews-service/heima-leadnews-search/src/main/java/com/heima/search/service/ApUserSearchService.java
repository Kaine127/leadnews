package com.heima.search.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.HistorySearchDto;
import com.heima.search.pojos.ApUserSearch;
import org.springframework.web.bind.annotation.RequestBody;

import javax.xml.ws.Response;
import java.util.List;

public interface ApUserSearchService {

    /**
     * 保存用户的搜索历史记录
     * @param keyword
     * @param userId
     */
    public void insert(String keyword,Integer userId);

    /**
     * 查询搜索记录
     * @return
     */
    public List<ApUserSearch> findUserSearch();

    /**
     * 删除历史记录
     * @param dto
     */
    public void delUserSearch(@RequestBody HistorySearchDto dto);
}
