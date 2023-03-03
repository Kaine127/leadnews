package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustomException;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;


@Slf4j
@Service
@Transactional(rollbackFor = Exception.class) //默认只能对RuntimeException回滚，实现其他的要制定rollbackfor
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public ResponseResult uploadPicture(MultipartFile multipartFile) {
//        //1. 检查参数
//        if (multipartFile == null || multipartFile.getSize() == 0){
//            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
//        }
//
//        WmUser user = WmThreadLocalUtil.getUser();
//        if ((user == null)){
//            //token解析出错，防止通过网关校验后，token再之后过期失效
//            throw new CustomException(AppHttpCodeEnum.NEED_LOGIN);
//        }
//
//        //2.上传图片到minIO中
//        String fileName = UUID.randomUUID().toString().replace("-","");
//        String originalFilename = multipartFile.getOriginalFilename();
//
//        int index = originalFilename.lastIndexOf(".");
//        if (index != -1){
//            //当后缀名存在,将后缀拼接到fileName
//            String postfix = originalFilename.substring(originalFilename.lastIndexOf("."));
//            fileName = fileName + postfix;
//        }
//
//
//        String fileId = null;
//        try {
//            fileId = fileStorageService.uploadImgFile("", fileName, multipartFile.getInputStream());
//            log.info("上传图片到minIO中,fileId:{}",fileId);
//        } catch (IOException e) {
//            e.printStackTrace();
//            log.error("WmMaterialServiceImpl-上传文件失败");
//            throw new CustomException(AppHttpCodeEnum.SERVER_ERROR);//抛出异常作用：1.事务回滚 2.通过全局异常处理器向前端返回异常提示
//        }
//        //3. 保存到数据库中
//        try{
//            WmMaterial wmMaterial = new WmMaterial();
//            wmMaterial.setUserId(user.getId());
//            wmMaterial.setUrl(fileId);
//            wmMaterial.setIsCollection((short)0);
//            wmMaterial.setType((short)0);
//            wmMaterial.setCreatedTime(new Date());
//            save(wmMaterial);//调用的时serviceImpl自己的方法
//            return ResponseResult.okResult(wmMaterial);
//        }catch (Exception e){
//            //保存失败，处理垃圾图片
//            fileStorageService.delete(fileId);
//            throw e;
//        }

        if (multipartFile == null || multipartFile.getSize() == 0){
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }

        //token在网关检测后仍有可能过期, 检查是否过期了
        WmUser user = WmThreadLocalUtil.getUser();
        if (user == null){
            //token失效
            throw new CustomException(AppHttpCodeEnum.NEED_LOGIN);
        }

        //没有问题的情况下,进行存入前的准备措施
        //通过UUID获取图片名称
        String fileName = UUID.randomUUID().toString().replace("-", "");
        //获取后缀 先判断是否有后缀
        String originalFilename = multipartFile.getOriginalFilename();
        int index = originalFilename.lastIndexOf(".");
        if (index != -1){
            //存在后缀
            String postfix = originalFilename.substring(originalFilename.lastIndexOf("."));
            fileName = fileName + postfix;

        }
        String uploadImgFile = null;
        //存入minio
        try {
            uploadImgFile = fileStorageService.uploadImgFile("", fileName, multipartFile.getInputStream());
            log.info("图片存入minio,路径为:{}",uploadImgFile);
        } catch (IOException e) {
            e.printStackTrace();
            log.info("图片上传失败");
            throw new CustomException(AppHttpCodeEnum.SERVER_ERROR);//抛出异常作用：1.事务回滚 2.通过全局异常处理器向前端返回异常提示
        }

        //设置自媒体文章参数
        try {
            WmMaterial wmMaterial = new WmMaterial();
            wmMaterial.setUserId(user.getId());
            wmMaterial.setUrl(uploadImgFile);
            wmMaterial.setType((short)0);
            wmMaterial.setIsCollection((short)0);
            wmMaterial.setCreatedTime(new Date());
            save(wmMaterial);
            return ResponseResult.okResult(wmMaterial);

        } catch (Exception e) {

            //如果出错 , 删除minio中刚上传的文件
            fileStorageService.delete(uploadImgFile);
            throw e;
        }


    }

    /**
     * 素材列表查询
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findList(WmMaterialDto dto) {
//        //1.查询参数
//        dto.checkParam();
//        //2.分页查询
//        IPage page = new Page(dto.getPage(),dto.getSize());
//        LambdaQueryWrapper<WmMaterial> lambdaQueryWrapper = new LambdaQueryWrapper<>();
//        //是否收藏
//        if (dto.getIsColllection() != null && dto.getIsColllection() == 1){
//            //目前只能查到收藏的或全部的
//            lambdaQueryWrapper.eq(WmMaterial::getIsCollection,1);
//        }
//
//        //按照用户查询
//        lambdaQueryWrapper.eq(WmMaterial::getUserId,WmThreadLocalUtil.getUser().getId());
//
//        //按照时间倒序
//        lambdaQueryWrapper.orderByDesc(WmMaterial::getCreatedTime);
//
//        page = page(page,lambdaQueryWrapper);
//        //3.结果返回
//        PageResponseResult responseResult = new PageResponseResult((int)page.getCurrent(), (int)page.getSize(), (int) page.getTotal());
//        responseResult.setData(page.getRecords());//注意page类中的构造方法没有data
//        return responseResult;

        //1.查询参数
        dto.checkParam();
        //2.分页查询
        //建立分页构造器
        Page page = new Page(dto.getPage(),dto.getSize());
        //建立条件构造器
        LambdaQueryWrapper<WmMaterial> queryWrapper = new LambdaQueryWrapper<>();
        //是否是收藏的
        if (dto.getIsColllection() != null && dto.getIsColllection() ==2){
            //其他情况都是查询全部, 只有为2的时候是查询收藏的
            queryWrapper.eq(WmMaterial::getIsCollection,dto.getIsColllection());
        }
        //设置用户id
        queryWrapper.eq(WmMaterial::getUserId,WmThreadLocalUtil.getUser().getId());
        //设置时间排序
        queryWrapper.orderByDesc(WmMaterial::getCreatedTime);

        //查询
        page = page(page, queryWrapper);

        //封装成PageResult对象
        PageResponseResult responseResult = new PageResponseResult((int) page.getCurrent(), (int) page.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());

        return responseResult;

    }
}
