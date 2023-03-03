package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.apis.article.IArticleClient;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.common.exception.CustomException;
import com.heima.common.tess4j.Tess4jClient;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
@Transactional
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {


    //对象创建之后调用
    @Override
    @PostConstruct
    @Scheduled(fixedRate = 1000L * 60L * 60L * 24L)
    public void init(){
        //获取所有的敏感词
        List<WmSensitive> wmSensitives = wmSensitiveMapper.selectList(Wrappers.<WmSensitive>lambdaQuery().select(WmSensitive::getSensitives));
        List<String> sensitiveList = wmSensitives.stream().map(WmSensitive::getSensitives).collect(Collectors.toList());

        //初始化敏感词
        SensitiveWordUtil.initMap(sensitiveList);

    }


    @Autowired
    private WmNewsMapper wmNewsMapper;

    /**
     * 自媒体文章审核
     *
     * @param id 自媒体文章id
     */
    @Override
    @Async  //标明当前方法是一个异步方法
    public void autoScanWmNews(Integer id) {

//        int a = 1/0;

        //1.查询自媒体文章
        WmNews wmNews = wmNewsMapper.selectById(id);
        if (wmNews == null) {
            throw new RuntimeException("WmNewsAutoScanServiceImpl-文章不存在");
        }

        if (wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())) {
            //从内容中提取纯文本内容和图片
            Map<String, Object> textAndImages = handleTextAndImages(wmNews);

            //自管理的敏感词过滤
            boolean isSensitive = handleSensitiveScan((String) textAndImages.get("content"), wmNews);
            if (!isSensitive) return;

            //2.审核文本内容  阿里云接口
            boolean isTextScan = handleTextScan((String) textAndImages.get("content"), wmNews);
            if (!isTextScan) return;

            //3.审核图片  阿里云接口
            boolean isImageScan = handleImageScan((List<String>) textAndImages.get("images"), wmNews);
            if (!isImageScan) return;

            //4.审核成功，保存app端的相关的文章数据
            ResponseResult responseResult = saveAppArticle(wmNews);
            if (responseResult ==null){
                throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
            }
            if (responseResult.getCode() == null || !responseResult.getCode().equals(200)) {
                //整合seata分布式事务使article微服务进行回滚AT模式进行补偿
                throw new RuntimeException("WmNewsAutoScanServiceImpl-文章审核，保存app端相关文章数据失败");
            }
            //回填article_id
            wmNews.setArticleId((Long) responseResult.getData());
            updateWmNews(wmNews, (short) 9, "审核成功");

        }
    }

    @Autowired
    private WmSensitiveMapper wmSensitiveMapper;

    /**
     * 自管理的敏感词审核
     *
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleSensitiveScan(String content, WmNews wmNews) {

        boolean flag = true;

//        //获取所有的敏感词
//        List<WmSensitive> wmSensitives = wmSensitiveMapper.selectList(Wrappers.<WmSensitive>lambdaQuery().select(WmSensitive::getSensitives));
//        List<String> sensitiveList = wmSensitives.stream().map(WmSensitive::getSensitives).collect(Collectors.toList());
//
//        //初始化敏感词库
//        SensitiveWordUtil.initMap(sensitiveList);
        init();

        //查看文章中是否包含敏感词
        Map<String, Integer> map = SensitiveWordUtil.matchWords(content);
        if (map.size() > 0) {
            updateWmNews(wmNews, (short) 2, "当前文章中存在违规内容" + map);
            flag = false;
        }

        return flag;
    }

    @Autowired
    private IArticleClient articleClient;

    @Autowired
    private WmChannelMapper wmChannelMapper;

    @Autowired
    private WmUserMapper wmUserMapper;

    /**
     * 保存app端相关的文章数据
     *
     * @param wmNews
     */
    private ResponseResult saveAppArticle(WmNews wmNews) {

//        ArticleDto dto = new ArticleDto();
//        //属性的拷贝
//        BeanUtils.copyProperties(wmNews, dto);
//        //文章的布局
//        dto.setLayout(wmNews.getType());
//        //频道
//        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
//        if (wmChannel != null) {
//            dto.setChannelName(wmChannel.getName());
//        }
//
//        //作者
//        dto.setAuthorId(wmNews.getUserId().longValue());
//        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
//        if (wmUser != null) {
//            dto.setAuthorName(wmUser.getName());
//        }
//
//        //设置文章id
//        if (wmNews.getArticleId() != null) {
//            dto.setId(wmNews.getArticleId());
//        }
//        dto.setCreatedTime(new Date());
//
//        ResponseResult responseResult = articleClient.saveArticle(dto);
//        return responseResult;

        //获得dto
        ArticleDto articleDto = new ArticleDto();
        //复制内容
        BeanUtils.copyProperties(wmNews,articleDto);
        //设置文章的布局
        articleDto.setLayout(wmNews.getType());
        //设置频道
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        if (wmChannel != null){
            articleDto.setChannelName(wmChannel.getName());
        }

        //设置作者id和姓名
        articleDto.setAuthorId(wmNews.getUserId().longValue());
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        if (wmUser != null){
            articleDto.setAuthorName(wmUser.getName());
        }

        //设置文章id
        if (wmNews.getArticleId()!=null){
            articleDto.setId(wmNews.getArticleId());
        }

        //调用feign进行分传递
        ResponseResult responseResult = articleClient.saveArticle(articleDto);
        return responseResult;

    }


    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private GreenImageScan greenImageScan;

    @Autowired
    private Tess4jClient tess4jClient;

    /**
     * 审核图片
     *
     * @param images
     * @param wmNews
     * @return
     */
    private boolean handleImageScan(List<String> images, WmNews wmNews) {

//        boolean flag = true;
//
//        if (images == null || images.size() == 0) {
//            return flag;
//        }
//
//        //下载图片 minIO
//        //图片去重
//        images = images.stream().distinct().collect(Collectors.toList());
//
//        List<byte[]> imageList = new ArrayList<>();
//
//        try {
//            for (String image : images) {
//                byte[] bytes = fileStorageService.downLoadFile(image);
//
//                //byte[] 转换为bufferedImage
//                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
//                BufferedImage bufferedImage = ImageIO.read(in);
//
//                //图片识别
//                String result = tess4jClient.doOCR(bufferedImage);
//                //过滤文字
//                boolean isSensitive = handleSensitiveScan(result, wmNews);
//                if(!isSensitive){
//                    return isSensitive;
//                }
//                imageList.add(bytes);
//
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//
//
//
//        //审核图片
//        try {
//            Map map = greenImageScan.imageScan(imageList);
//            if (map != null) {
//                //审核失败
//                if (map.get("suggestion").equals("block")) {
//                    flag = false;
//                    updateWmNews(wmNews, (short) 2, "当前文章中存在违规内容");
//                }
//
//                //不确定信息  需要人工审核
//                if (map.get("suggestion").equals("review")) {
//                    flag = false;
//                    updateWmNews(wmNews, (short) 3, "当前文章中存在不确定内容");
//                }
//            }
//
//        } catch (Exception e) {
//            flag = false;
//            e.printStackTrace();
//        }
//        return flag;

        //设置flag
        Boolean flag = true;
        //判断是否有图片, 如果没有, 就说明不用审核
        if (images == null || images.size() == 0){
            return flag;
        }

        //去重图片路径, 准备从minio中获取
        images = images.stream().distinct().collect(Collectors.toList());
        //准备用于阿里云图片审核的List<byte[]>
        List<byte[]> imageList = new ArrayList<>();

        try {
            for (String image : images) {
                byte[] bytes = fileStorageService.downLoadFile(image);

                //转换成图片输入流
                ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                BufferedImage bufferedImage = ImageIO.read(inputStream);

                //用第三方工具进行文字的提取
                String result = tess4jClient.doOCR(bufferedImage);
                //用自管理敏感词库进行检测
                boolean isSensitive = handleSensitiveScan(result, wmNews);
                if (!isSensitive){
                    //如果审核没通过, 返回flase
                    return isSensitive;
                }
                //审核通过, 加入到集合中准备用于阿里云图片审核
                imageList.add(bytes);

            }
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }

        //进行阿里云图片审核

        try {
            Map map = greenImageScan.imageScan(imageList);
            String suggestion = (String)map.get("suggestion");
            if (StringUtils.isNotBlank(suggestion)){
                if ("block".equals(suggestion)){
                    updateWmNews(wmNews,(short) 2,"图片含有敏感内容");
                    flag= false;
                }

                if ("review".equals(suggestion)){
                    updateWmNews(wmNews,(short) 3,"图片含有敏感内容");
                    flag = false;
                }
            }
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }

        return flag;
    }

    @Autowired
    private GreenTextScan greenTextScan;

    /**
     * 审核纯文本内容
     *
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleTextScan(String content, WmNews wmNews) {

//        //内容为空,没有必要审核
//       if(StringUtils.isBlank(content)){
//           return true;
//       }
//       //调用阿里云接口
//        try {
//            Map map = greenTextScan.greeTextScan(content);//前面已经将标题加入到content中
//
//            if (map != null){
//                String suggestion = (String)map.get("suggestion");
//                if ("block".equals(suggestion)){
//                    updateWmNews(wmNews, (short) 2, "文章中包含敏感内容");//审核不通过
//                    return false;
//                }else if ("review".equals(suggestion)){
//                    updateWmNews(wmNews, (short) 3, "文章中可能包含敏感内容");//人工审核
//                    return false;
//                }else if("pass".equals(suggestion)){
//                    return true;//审核通过
//                }
//            }
//        } catch (Exception e) {
//            //e.printStackTrace();//不用这个抛错方法,这个是输出到控制台上,需要把错误输出到日志中
//            log.error("调用阿里云的审核接口失败",e);
//        }
//
//        updateWmNews(wmNews, (short) 3, "文章中可能包含敏感内容");//上述三种情况以外的情况
//        return false;
        //查看是否有内容
        if (StringUtils.isBlank(content)){
            //没有内容 不用审核
            return true;
        }

        //调用阿里云接口的方法
        try {
            Map map = greenTextScan.greeTextScan(content);
            String suggestion = (String)map.get("suggestion");
            if (StringUtils.isNotBlank(suggestion)){
                if ("block".equals(suggestion)){
                    updateWmNews(wmNews,(short) 2,"内容中含有敏感信息.");//审核不通过
                    return false;
                }else if ("review".equals(suggestion)){
                    updateWmNews(wmNews,(short) 3,"文章中含有敏感信息");//需要人工审核
                }else if ("pass".equals(suggestion)){
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("调用阿里云的审核接口失败",e);
        }
        //第三种情况, 无法审核, 需要人工审核
        updateWmNews(wmNews,(short) 3,"文章中含有敏感信息");//需要人工审核
        return false;
    }

    /**
     * 修改文章内容
     *
     * @param wmNews
     * @param status
     * @param reason
     */
    private void updateWmNews(WmNews wmNews, short status, String reason) {
        wmNews.setStatus(status);
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }

    /**
     * 1。从自媒体文章的内容中提取文本和图片
     * 2.提取文章的封面图片
     *
     * @param wmNews
     * @return
     */
    private Map<String, Object> handleTextAndImages(WmNews wmNews) {
//
//        //存储纯文本内容
//        StringBuilder stringBuilder =new StringBuilder();
//
//        Set<String> images = new HashSet<>();//TreeSet 放入顺序或者自定义排序规则
//
//        //1.从自媒体文章的内容中提取文本和图片[{type: text/image,value: }.{}]
//        if (StringUtils.isNotBlank(wmNews.getContent())){
//            List<Map> maps = JSONArray.parseArray(wmNews.getContent(),Map.class);
//            for (Map map : maps) {
//                //提取内容中的文本
//                if ("text".equals(map.get("type"))){
//                    stringBuilder.append(","+map.get("value"));
//                }
//                //提取内容中的图片
//                else if ("image".equals(map.get("type"))){
//                    images.add((String) map.get("value"));
//                }
//            }
//        }
//        //2.提取文章的封面图片
//        if (StringUtils.isNotBlank(wmNews.getImages())){
//            String[] split = wmNews.getImages().split(",");
//            images.addAll(Arrays.asList(split));
//        }
//        //处理标题
//        String title = wmNews.getTitle();
//        if (StringUtils.isNotBlank(wmNews.getTitle())){
//            stringBuilder.append(",").append(title);
//        }
//
//        Map<String,Object> resultMap = new HashMap<>();
//        resultMap.put("content",stringBuilder.toString());
//        resultMap.put("iamges",new ArrayList<>(images));
//        return resultMap;

        //1. 存放纯文本内容
        StringBuilder stringBuilder = new StringBuilder();
        //2. 存储图片路径数据
        Set<String> images = new HashSet<>();

        //判断是否有内容
        if (StringUtils.isNotBlank(wmNews.getContent())){
            List<Map> maps = JSONArray.parseArray(wmNews.getContent(), Map.class);
            for (Map map : maps) {
                if ("text".equals(map.get("type"))){
                    //纯文本数据
                    stringBuilder.append(map.get("value"));
                }

                if ("iamge".equals(map.get("type"))){
                    //图片数据
                    images.add((String) map.get("value"));
                }
            }
        }

        //添加封面图片内容
        if (StringUtils.isNotBlank(wmNews.getImages())){
            //获取图片路径集合
            String[] split = wmNews.getImages().split(",");
            //转换为Set数组
            images.addAll(Arrays.asList(split));
        }

        //将标题添加进去
        String title = wmNews.getTitle();
        if (StringUtils.isNotBlank(title)){
            stringBuilder.append(",").append(title);
        }

        //存入到Map集合中
        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("content",stringBuilder.toString());
        resultMap.put("images",images);
        //返回Map集合
        return resultMap;
    }
}
