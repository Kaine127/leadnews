package com.heima.es;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.es.mapper.ApArticleMapper;
import com.heima.es.pojo.SearchArticleVo;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ApArticleTest {
    @Autowired
    private ApArticleMapper apArticleMapper;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 注意：数据量的导入，如果数据量过大，需要分页导入
     * @throws Exception
     */
    @Test
    public void init() throws Exception {
//        //1.查询所有符合条件的文章数据
//        List<SearchArticleVo> searchArticleVos = apArticleMapper.loadArticleList();
//
//        //2.批量导入到es索引库中
//        BulkRequest bulkRequest = new BulkRequest("app_info_article");
//
//        for (SearchArticleVo searchArticleVo : searchArticleVos) {
//            new IndexRequest().id(searchArticleVo.getId().toString())
//                    .source(JSON.toJSONString(searchArticleVo), XContentType.JSON);
//        }
//        restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);

        //查询所有数据量
        Long aLong = apArticleMapper.loadArticleListCount();

        //分批添加
        int perCount = 10;

        int cycle = (int)(aLong % perCount == 0 ? aLong/perCount : aLong/perCount +1);

        //分页查询

        for (int i = 1;i <= cycle; i++){
            //1.查询所有符合条件的文章数据
            Page<SearchArticleVo> page = new Page<>();
            page.setCurrent(i);
            page.setSize(perCount);
            page.setSearchCount(false);//关闭count查询
            IPage<SearchArticleVo> searchArticleVos = apArticleMapper.loadArticleList(page);

            //2.批量导入到es索引库

            BulkRequest bulkRequest = new BulkRequest("app_info_article");

            for (SearchArticleVo searchArticleVo : searchArticleVos.getRecords()){

                IndexRequest indexRequest = new IndexRequest().id(searchArticleVo.getId().toString())
                        .source(JSON.toJSONString(searchArticleVo), XContentType.JSON);

                //批量添加数据
                bulkRequest.add(indexRequest);
            }
            restHighLevelClient.bulk(bulkRequest,RequestOptions.DEFAULT);
        }
    }

}