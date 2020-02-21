package com.atguigu.gmall.manage.controller;

import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@CrossOrigin
public class FileUploadController {

    //@Value 使用的前提条件是当前类必须再spring容器中
    @Value("${fileServer.url}")
    private String fileUrl;

    /**
     * http://localhost:8082/fileUpload
     *
     * 获取上传文件，需要使用springmvc技术
     * @return
     */
    @RequestMapping("fileUpload")
    public String fileUpload(MultipartFile file) throws IOException, MyException {

        String imgUrl = fileUrl;

        //当文件不为空的时候才能进行上传
        if (file != null){
            String configFile = this.getClass().getResource("/tracker.conf").getFile();
            ClientGlobal.init(configFile);
            TrackerClient trackerClient=new TrackerClient();
            TrackerServer trackerServer=trackerClient.getTrackerServer();
            StorageClient storageClient=new StorageClient(trackerServer,null);
//            String orginalFilename="d://imgs/金背.jpg";
            //String[] upload_file = storageClient.upload_file(orginalFilename, suffixName, null);//本地文件
            //获取上传文件名称
            String orginalFilename = file.getOriginalFilename();
            //获取文件的后缀名
            String suffixName = StringUtils.substringAfterLast(orginalFilename, ".");
            String[] upload_file = storageClient.upload_file(file.getBytes(),suffixName,null);
            for (int i = 0; i < upload_file.length; i++) {
                String s = upload_file[i];
                System.out.println("s = " + s);
                imgUrl += "/" + upload_file[i];
            }
        }
        // return "http://dsjrz8//group1/M00/00/00/wKgjil5LRNmAAL2PAADfnMDJkAM309.jpg";
        return imgUrl;
    }


}
