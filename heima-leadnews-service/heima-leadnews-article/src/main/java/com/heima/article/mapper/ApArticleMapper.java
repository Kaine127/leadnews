package com.heima.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface ApArticleMapper extends BaseMapper<ApArticle> {

    //@Param给参数定义名字 1 加载更多  2 加载更新
    public IPage<ApArticle> loadArticleList(IPage<ApArticle> page,@Param("dto") ArticleHomeDto dto, @Param("type") Short type);

    IPage<ApArticle> selectPageVo(IPage<ApArticle> page,@Param("dto") ArticleHomeDto dto,@Param("type")Short type);

    public List<ApArticle> findArticleListByLast5days(@Param("dayParam") Date dayParam);

}