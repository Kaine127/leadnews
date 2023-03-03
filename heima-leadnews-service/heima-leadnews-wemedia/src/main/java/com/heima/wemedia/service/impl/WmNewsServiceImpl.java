package com.heima.wemedia.service.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.constants.WmNewsMessageConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmDownOrUpDto;
import com.heima.model.wemedia.dtos.WmDownOrUpMsgDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import com.heima.wemedia.service.WmNewsTaskService;
import com.heima.wemedia.vo.ContentVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {

    @Autowired
    private KafkaTemplate kafkaTemplate;
    /**
     * 条件查询文章列表
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findList(WmNewsPageReqDto dto) {
//        //1.检查参数
//        //分页检查
//        dto.checkParam();
//
//        //2.分页条件查询
//        IPage page = new Page(dto.getPage(), dto.getSize());
//        LambdaQueryWrapper<WmNews> lambdaQueryWrapper = new LambdaQueryWrapper();
//        //状态精确查询
//        if (dto.getStatus() != null) {
//            lambdaQueryWrapper.eq(WmNews::getStatus, dto.getStatus());
//        }
//
//        //频道精确查询
//        if (dto.getChannelId() != null) {
//            lambdaQueryWrapper.eq(WmNews::getChannelId, dto.getChannelId());
//        }
//
//        //时间范围查询
//        if (dto.getBeginPubDate() != null && dto.getEndPubDate() != null) {
//            lambdaQueryWrapper.between(WmNews::getPublishTime, dto.getBeginPubDate(), dto.getEndPubDate());
//        }
//
//        //关键字的模糊查询
//        if (StringUtils.isNotBlank(dto.getKeyword())) {
//            lambdaQueryWrapper.like(WmNews::getTitle, dto.getKeyword());
//        }
//
//        //查询当前登录人的文章
//        lambdaQueryWrapper.eq(WmNews::getUserId, WmThreadLocalUtil.getUser().getId());
//
//        //按照发布时间倒序查询
//        lambdaQueryWrapper.orderByDesc(WmNews::getPublishTime);
//
//
//        page = page(page, lambdaQueryWrapper);
//
//        //3.结果返回
//        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
//        responseResult.setData(page.getRecords());
//
//
//        return responseResult;

        //查询参数
        dto.checkParam();
        //查询参数是否存在
        if (dto == null ){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //获取当前用户, 并查询是否存在
        WmUser user = WmThreadLocalUtil.getUser();
        if (user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        //构建分页构造器
        IPage page = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmNews> queryWrapper = new LambdaQueryWrapper<>();
        //查询状态
        if (dto.getStatus() != null){
            queryWrapper.eq(WmNews::getStatus,dto.getStatus());
        }
        //是否查询频道id
        if (dto.getChannelId() != null){
            queryWrapper.eq(WmNews::getChannelId,dto.getChannelId());
        }

        //是否根据开始结束时间查询
        if (dto.getBeginPubDate()!= null && dto.getEndPubDate() != null){
            queryWrapper.between(WmNews::getPublishTime,dto.getBeginPubDate(),dto.getEndPubDate());
        }
        //根据关键字模糊查询
        if (StringUtils.isNotBlank(dto.getKeyword())){
            queryWrapper.like(WmNews::getTitle,dto.getKeyword());
        }
        //按照用户id查询
        queryWrapper.eq(WmNews::getUserId,user.getId());

        //按照发布时间倒叙查询
        queryWrapper.orderByDesc(WmNews::getPublishTime);

        //查询
        page = page(page,queryWrapper);

        //创建分页结果集
        PageResponseResult responseResult = new PageResponseResult((int) page.getCurrent(), (int) page.getSize(), (int) page.getTotal());
        //设置文章内容
        responseResult.setData(page.getRecords());

        //返回结果
        return responseResult;
    }

    /**
     * 发布修改文章或保存为草稿
     * @param dto
     * @return
     */
//    @Override
//    public ResponseResult submitNews(WmNewsDto dto) {
//
//        //0.条件判断
//        if(dto == null || dto.getContent() == null){
//            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
//        }
//
//        //1.保存或修改文章
//
//        WmNews wmNews = new WmNews();
//        //属性拷贝 属性名词和类型相同才能拷贝
//        BeanUtils.copyProperties(dto,wmNews);
//        //封面图片  list---> string
//        if(dto.getImages() != null && dto.getImages().size() > 0){
//            //[1dddfsd.jpg,sdlfjldk.jpg]-->   1dddfsd.jpg,sdlfjldk.jpg
//            String imageStr = StringUtils.join(dto.getImages(), ",");
//            wmNews.setImages(imageStr);
//        }
//        //如果当前封面类型为自动 -1
//        if(dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)){
//            wmNews.setType(null);
//        }
//
//        saveOrUpdateWmNews(wmNews);
//
//        //2.判断是否为草稿  如果为草稿结束当前方法
//        if(dto.getStatus().equals(WmNews.Status.NORMAL.getCode())){
//            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
//        }
//
//        //3.不是草稿，保存文章内容图片与素材的关系
//        //获取到文章内容中的图片信息
//        List<String> materials =  ectractUrlInfo(dto.getContent());
//        saveRelativeInfoForContent(materials,wmNews.getId());
//
//        //4.不是草稿，保存文章封面图片与素材的关系，如果当前布局是自动，需要匹配封面图片
//        saveRelativeInfoForCover(dto,wmNews,materials);
//
//        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
//
//    }


    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;
    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;
    @Autowired
    private WmMaterialMapper wmMaterialMapper;

    @Autowired
    private WmNewsTaskService wmNewsTaskService;

    /**
     * 发布修改文章或保存为草稿
     * @param dto
     * @return
     */
    @Override
    public ResponseResult submitNews(WmNewsDto dto){
//        //1. 对参数进行校验
//        if(dto == null || StringUtils.isBlank(dto.getContent()) || dto.getTitle() == null
//        || dto.getTitle().length() <= 2|| dto.getTitle().length() >= 6){//判空一定要在最前面
//
//            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
//        }
//
//        //2.构建数据库实体对象
//        WmNews wmNews = new WmNews();
//        BeanUtils.copyProperties(dto,wmNews);
//
//        //3.提取内容中的图片
//        String content = dto.getContent();
//        List<Map> maps = JSON.parseArray(content, Map.class);
//        List<String> contentUrls = new ArrayList<>();
//        //判断当前集合有没有内容
//        if(!CollectionUtils.isEmpty(maps)){
//            for (Map map : maps) {
//                if("image".equals(map.get("type"))){
//                    contentUrls.add(map.get("value").toString());
//                }
//            }
//        }
//
//        //流去重(内容中的图片集合)
//        contentUrls = contentUrls.stream().distinct().collect(Collectors.toList());
//
//        //接收封面图片
//        List<String> images = dto.getImages();
//        //4.自动选择封面
//        if (WemediaConstants.WM_NEWS_TYPE_AUTO.equals(dto.getType())) {
//            int size = contentUrls.size();
//
//            if (size >= 3) {
//                //4.1 超过3张 选前三张
//                images = contentUrls.subList(0, 3);
//                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
//            } else if (size >= 1) {
//                //4.2 1-2张 第一张
//                images = contentUrls.subList(0, 1);
//                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
//            } else {
//                //4.3 无图
//                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
//            }
//        }
//
//            //5.将封面list转换成字符串保存到数据库中
//            if (!CollectionUtils.isEmpty(images)){
//                wmNews.setImages(StringUtils.join(images,","));
//            }
//
//            wmNews.setEnable((short)1);
//            if (dto.getStatus() != null && WmNews.Status.SUBMIT.getCode()==dto.getStatus()){
//                //只有提交的时候才会设置提交时间,草稿时不用设置
//                wmNews.setSubmitedTime(new Date());
//            }
//
//            wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
//
//            if (dto.getId() == null){
//                //新增
//                wmNews.setCreatedTime(new Date());
//                save(wmNews);
//            }else {
//                //修改
//                //先删除关联表,把所有
//                wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery()
//                        .eq(WmNewsMaterial::getNewsId,dto.getId()));
//                updateById(wmNews);
//            }
//
//            //7.判断是否为草稿,如果是草稿直接结束
//        if (dto.getStatus() != null && WmNews.Status.NORMAL.getCode()==dto.getStatus()){
//            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
//        }
//        //8.建立内容中图片和文章关联关系
//        saveRelation(contentUrls,dto.getId(),WemediaConstants.WM_CONTENT_REFERENCE);
//        //9.建立封面中图片和文章关联关系
//        saveRelation(images,dto.getId(),WemediaConstants.WM_COVER_REFERENCE);
//
//
//        //审核文章
//        wmNewsAutoScanService.autoScanWmNews(wmNews.getId());
//        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);

        //1.判断参数是否没问题
        if (dto ==null || StringUtils.isBlank(dto.getContent()) || dto.getTitle() == null ||
        dto.getTitle().length()<=2 || dto.getTitle().length()>=6){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.抽取出文章对象用于后续保存
        WmNews wmNews = new WmNews();
        BeanUtils.copyProperties(dto,wmNews);
        //3.获取内容中的图片
        String content = dto.getContent();
        //此map集合中包含图片以及文字信息
        List<Map> maps = JSON.parseArray(content, Map.class);
        //获取内容图片集合
        List<String> contentUrls = new ArrayList<>();

        if (!CollectionUtils.isEmpty(maps)){
            //提取出来图片内容
            for (Map map : maps) {
                if (map.get("type").equals("image")){
                    contentUrls.add(map.get("value").toString());
                }
            }
        }

        //进行流去重
        contentUrls = contentUrls.stream().distinct().collect(Collectors.toList());

        //获取封面图片
        List<String> images = dto.getImages();
        //4.根据用户选择的类型选择封面的图片的处理,自动的情况
        if (WemediaConstants.WM_NEWS_TYPE_AUTO.equals(dto.getType())){
            int size = images.size();
            if (size >=3 ){
                //超过三张 ,选前三张
                images = contentUrls.subList(0,3);
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
            }else if (size >= 1){
                //超过一张 小于三张 选第一张
                images = contentUrls.subList(0,1);
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
            }else {
                //没有内容图片的情况
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            }
        }

        //5.将封面图片设置WmNew中
        if (!CollectionUtils.isEmpty(images)){
            wmNews.setImages(StringUtils.join(images,","));
        }

        wmNews.setEnable((short)1);
        //判断是不是草稿, 需不需要设置提交时间
        if (dto.getStatus() !=0 && WmNews.Status.SUBMIT.getCode() == dto.getStatus()){

            //只有不是草稿的时候才会设置提交时间,草稿时不用设置
            wmNews.setSubmitedTime(new Date());
        }

        //设置用户id
        wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
        //接下来判断是id是否为空, 如果是空的话就是新增, 如果不是,就是修改
        if (dto.getId() == null){
            //新增的情况
            save(wmNews);
        }else {
            //修改, 修改前要删除图片文章关联表中的数据
            wmNewsMaterialMapper.delete(
                    Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId,dto.getId()));
            //根据id更新
            updateById(wmNews);
        }

        //判断是否是草稿
        if (dto.getStatus() != null && WmNews.Status.NORMAL.getCode() == dto.getStatus()){
            //是草稿 直接结束
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }

        //如果不是草稿的话, 保存相关的图片文章关联
        //保存内容图片与文章的关联
        saveRelation(contentUrls,dto.getId(),WemediaConstants.WM_CONTENT_REFERENCE);
        //保存封面图片与文章的关联
        saveRelation(images,dto.getId(),WemediaConstants.WM_COVER_REFERENCE);

        //进行内容审查
        //wmNewsAutoScanService.autoScanWmNews(wmNews.getId());
        wmNewsTaskService.addNewsToTask(wmNews.getId(),wmNews.getPublishTime());

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);

    }



    // 图片url集合 文章id 图片类型
    private void saveRelation(List<String> urls,int newsId,short type){
//
//        //判断图片是否为空
//        if (CollectionUtils.isEmpty(urls)){
//            return;
//        }
//        //根据图片urls拿到图片id
//        List<WmMaterial> wmMaterials = wmMaterialMapper.selectList(Wrappers.<WmMaterial>lambdaQuery()
//                .in(WmMaterial::getUrl, urls).select(WmMaterial::getId));
//
//        if (urls.size() != wmMaterials.size()){
//            //如果数量不同,说明素材库图片被删除,需要回滚事务
//            throw new CustomException(AppHttpCodeEnum.MATERIASL_REFERENCE_FAIL);
//        }
//
//        //获取所有图片资源的id;
//        List<Integer> ids = wmMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());
//
//        wmNewsMaterialMapper.saveRelations(ids,newsId,type);

        //判断图片是否为空
        if (CollectionUtils.isEmpty(urls)){
            //没有图片的情况, 无需保存图片和文章的关联
            return;
        }
        //有图片的情况, 根据urls获取图片资源的集合
        List<WmMaterial> wmMaterials = wmMaterialMapper.selectList(Wrappers.<WmMaterial>lambdaQuery().in(WmMaterial::getUrl, urls).select(WmMaterial::getId));

        //判断urls中的图片数量和查询出来的图片素材数量是够相等
        if (wmMaterials.size() != urls.size()){
            throw new CustomException(AppHttpCodeEnum.MATERIASL_REFERENCE_FAIL);
        }
        //没有问题的情况下, 获取所有的查询出来的图片的id, 用于最后关联表的输入
        List<Integer> idCollection = wmMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());
        wmNewsMaterialMapper.saveRelations(idCollection,newsId,type);
    }


    /**
     * 第一个功能：如果当前封面类型为自动，则设置封面类型的数据
     * 匹配规则：
     * 1，如果内容图片大于等于1，小于3  单图  type 1
     * 2，如果内容图片大于等于3  多图  type 3
     * 3，如果内容没有图片，无图  type 0
     *
     * 第二个功能：保存封面图片与素材的关系
     * @param dto
     * @param wmNews
     * @param materials
     */
//    private void saveRelativeInfoForCover(WmNewsDto dto, WmNews wmNews, List<String> materials) {
//
//        List<String> images = dto.getImages();
//
//        //如果当前封面类型为自动，则设置封面类型的数据
//        if(dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)){
//            //多图
//            if(materials.size() >= 3){
//                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
////                images = materials.stream().limit(3).collect(Collectors.toList());
//                images = materials.subList(0,3);
//            }else if(materials.size() >= 1){
//                //单图
//                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
////                images = materials.stream().limit(1).collect(Collectors.toList());
//                images = materials.subList(0,1);
//            }else {
//                //无图
//                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
//            }
//
//            //修改文章
//            if(!CollectionUtils.isEmpty(images)){
//                wmNews.setImages(StringUtils.join(images,","));
//            }
//            updateById(wmNews);
//        }
//        //第二个功能：保存封面图片与素材的关系
//        if(images != null && images.size() > 0){
//            saveRelativeInfo(images,wmNews.getId(),WemediaConstants.WM_COVER_REFERENCE);
//        }
//
//    }
//
//
//    /**
//     * 处理文章内容图片与素材的关系
//     * @param materials
//     * @param newsId
//     */
//    private void saveRelativeInfoForContent(List<String> materials, Integer newsId) {
//        saveRelativeInfo(materials,newsId,WemediaConstants.WM_CONTENT_REFERENCE);
//    }
//

//
//    /**
//     * 保存文章图片与素材的关系到数据库中
//     * @param materials
//     * @param newsId
//     * @param type
//     */
//    private void saveRelativeInfo(List<String> materials, Integer newsId, Short type) {
//        if(materials != null && !materials.isEmpty()){
//            //通过图片的url查询素材的id
//            List<WmMaterial> dbMaterials = wmMaterialMapper.selectList(Wrappers.<WmMaterial>lambdaQuery()
//                    .in(WmMaterial::getUrl, materials)
//                    .select(WmMaterial::getId));//设置select时,只会查询出id字段
//
//            //判断素材是否有效
//            if(dbMaterials==null || dbMaterials.size() == 0){
//                //手动抛出异常   第一个功能：能够提示调用者素材失效了，第二个功能，进行数据的回滚
//                throw new CustomException(AppHttpCodeEnum.MATERIASL_REFERENCE_FAIL);
//            }
//
//            if(materials.size() != dbMaterials.size()){
//                throw new CustomException(AppHttpCodeEnum.MATERIASL_REFERENCE_FAIL);
//            }
//
//            List<Integer> idList = dbMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());
//
//            //批量保存
//            wmNewsMaterialMapper.saveRelations(idList,newsId,type);
//        }
//    }
//
//
//    /**
//     * 提取文章内容中的图片信息
//     * @param content
//     * @return
//     */
//    private List<String> ectractUrlInfo(String content) {
//
//        List<ContentVO> contentVOS = JSON.parseArray(content, ContentVO.class);
//        if (CollectionUtils.isEmpty(contentVOS)){
//
//            //如果没有数据,直接返回
//            return null;
//        }
//
//        Set<String> setData = new HashSet<>();
//        for (ContentVO contentVO : contentVOS) {
//            if ("image".equals(contentVO.getType())){
//                //添加图片
//                setData.add(contentVO.getValue());
//            }
//        }
//
//        //set集合本身没有转换为List集合的方法,用父类的构造方法
//        return new ArrayList<>(setData);
//    }
//

//
//    /**
//     * 保存或修改文章
//     * @param wmNews
//     */
//    private void saveOrUpdateWmNews(WmNews wmNews) {
//        //补全属性
//        wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
//        wmNews.setCreatedTime(new Date());
//        wmNews.setSubmitedTime(new Date());
//        wmNews.setEnable((short)1);//默认上架
//
//        if(wmNews.getId() == null){
//            //保存
//            save(wmNews);
//        }else {
//            //修改
//            //删除文章图片与素材的关系
//            wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId,wmNews.getId()));
//            updateById(wmNews);
//        }
//    }
//
//



    /**
     * 文章上下架
     * @param dto
     * @return
     */
    @Override
    public ResponseResult downOrUp(WmDownOrUpDto dto) {
        //1.检查参数
        if (dto.getId() == null || dto.getEnable() == null ||dto.getEnable()<0 || dto.getEnable()>1){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //从ThreadLocal中获取用户
        WmUser user = WmThreadLocalUtil.getUser();
        if (user == null || user.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        //查询文章
        WmNews wmNews = getOne(Wrappers.<WmNews>lambdaQuery().eq(WmNews::getUserId, user.getId())
                .eq(WmNews::getId, dto.getId()));
        if (wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"文章不存在");
        }
        //判断文章是否已发布
        if (wmNews.getEnable() == null || !wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"当前文章不是发布状态,不能上下架");
        }

        //修改文章enbale
        if (!wmNews.getEnable().equals(dto.getEnable())){
            update(Wrappers.<WmNews>lambdaUpdate().set(WmNews::getEnable,dto.getEnable())
                    .eq(WmNews::getId,dto.getId()));
            if (wmNews.getArticleId() != null){
                //发送消息, 通知article修改文章的配置

                WmDownOrUpMsgDto wmDownOrUpMsgDto = WmDownOrUpMsgDto.builder().id(wmNews.getArticleId())
                        .enable(dto.getEnable()).build();
                kafkaTemplate.send(WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC,JSON.toJSONString(wmDownOrUpMsgDto));
            }else{
                log.error("已发表文章articleId为空,文章ID:{},文章状态:{}",wmNews.getId(),wmNews.getStatus());
            }
        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

}
