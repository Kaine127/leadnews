package com.heima.search.service.impl;

import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.HistorySearchDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.search.pojos.ApUserSearch;
import com.heima.search.service.ApUserSearchService;
import com.heima.utils.thread.AppThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ApUserSeachServiceImpl implements ApUserSearchService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 保存用户搜索记录
     * @param keyword
     * @param userId
     */
    @Override
    @Async
    public void insert(String keyword, Integer userId) {

        //1.查询当前用户的搜索关键词
        Query query = Query.query(Criteria.where("userId").is(userId).and("keyword").is(keyword));
        ApUserSearch apUserSearch = mongoTemplate.findOne(query, ApUserSearch.class);

        //2.存在 更新创建时间
        if (apUserSearch != null){
            apUserSearch.setCreatedTime(new Date());
            mongoTemplate.save(apUserSearch);
            return;
        }

        //3.不存在, 判断当前历史记录总条数是否超过10
        //创建要存放的apUserSearch对象
        apUserSearch = new ApUserSearch();
        apUserSearch.setUserId(userId);
        apUserSearch.setKeyword(keyword);
        apUserSearch.setCreatedTime(new Date());

        //查询条数
        Query query1 = Query.query(Criteria.where("userId").is(userId));
        query1.with(Sort.by(Sort.Direction.DESC,"createdTime"));
        List<ApUserSearch> apUserSearchList = mongoTemplate.find(query1, ApUserSearch.class);

        //判断是否有空位
        if (apUserSearchList == null || apUserSearchList.size() < 10){
            //有空位 直接保存
            mongoTemplate.save(apUserSearch);
        }else {
            //没有空位 获取10个记录中最后一个进行替换
            ApUserSearch lastUserSearch = apUserSearchList.get(apUserSearchList.size() - 1);
            lastUserSearch.setCreatedTime(new Date());
            lastUserSearch.setKeyword(keyword);
            mongoTemplate.save(lastUserSearch);
        }


    }

    @Override
    public List<ApUserSearch> findUserSearch() {
        //获取当前用户
        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null){
            throw new CustomException(AppHttpCodeEnum.NEED_LOGIN);
        }

        //根据用户查询数据
        List<ApUserSearch> apUserSearchList = mongoTemplate.find(Query.query(Criteria.where("userId").is(user.getId())).with(Sort.by(Sort.Direction.DESC, "createdTime")), ApUserSearch.class);

        return apUserSearchList;
    }

    @Override
    public void delUserSearch(HistorySearchDto dto) {
        //删除
        mongoTemplate.remove(Query.query(Criteria.where("userId").is(AppThreadLocalUtil.getUser().getId()).and("id").is(dto.getId())),ApUserSearch.class);
    }
}
