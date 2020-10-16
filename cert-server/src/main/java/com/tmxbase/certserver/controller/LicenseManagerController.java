package com.tmxbase.certserver.controller;

import com.mongodb.client.FindIterable;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.tmxbase.certserver.creator.LicenseCreator;
import com.tmxbase.certserver.infos.AbstractServerInfos;
import com.tmxbase.certserver.infos.LinuxServerInfos;
import com.tmxbase.certserver.infos.WindowsInfos;
import com.tmxbase.certserver.model.LicenseCheckModel;
import com.tmxbase.certserver.model.LicenseParam;
import com.tmxbase.certserver.utils.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/license")
public class LicenseManagerController {

    /**
     * 证书生成路径
     */
    @Value("${license.licensePath}")
    private String licensePath;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private GridFSBucket gridFSBucket;



    /**
     * 获取服务器硬件信息
     * @param osName 操作系统类型，如果为空则自动判断
     */
    @RequestMapping(value = "/getServerInfos",produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public LicenseCheckModel getServerInfos(@RequestParam(value = "osName",required = false) String osName) {
        //操作系统类型
        if(StringUtils.isBlank(osName)){
            osName = System.getProperty("os.name");
        }
        osName = osName.toLowerCase();

        AbstractServerInfos abstractServerInfos = null;

        //根据不同操作系统类型选择不同的数据获取方法
        if (osName.startsWith("windows")) {
            abstractServerInfos = new WindowsInfos();
        } else if (osName.startsWith("linux")) {
            abstractServerInfos = new LinuxServerInfos();
        }else{//其他服务器类型
            abstractServerInfos = new LinuxServerInfos();
        }

        return abstractServerInfos.getServerInfos();
    }

    /**
     * 生成证书
     * @param param 生成证书需要的参数，如：{"subject":"ccx-models","privateAlias":"privateKey","keyPass":"5T7Zz5Y0dJFcqTxvzkH5LDGJJSGMzQ","storePass":"3538cef8e7","licensePath":"C:/Users/zifangsky/Desktop/license.lic","privateKeysStorePath":"C:/Users/zifangsky/Desktop/privateKeys.keystore","issuedTime":"2018-04-26 14:48:12","expiryTime":"2018-12-31 00:00:00","consumerType":"User","consumerAmount":1,"description":"这是证书描述信息","licenseCheckModel":{"ipAddress":["192.168.245.1","10.0.5.22"],"macAddress":["00-50-56-C0-00-01","50-7B-9D-F9-18-41"],"cpuSerial":"BFEBFBFF000406E3","mainBoardSerial":"L1HF65E00X9"}}
     */
    @RequestMapping(value = "/generateLicense",produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public Map<String,Object> generateLicense(@RequestBody(required = true) LicenseParam param) {
        Map<String,Object> resultMap = new HashMap<>(2);
        AbstractServerInfos infos = new WindowsInfos();
        System.out.println(infos.getServerInfos().toString());
        String name = MD5Util.encryption(infos.getServerInfos().toString());
        if(StringUtils.isBlank(param.getLicensePath())){
            param.setLicensePath(licensePath+param.getSubject()+".lic");
        }
        LicenseCreator licenseCreator = new LicenseCreator(param);
        boolean result = licenseCreator.generateLicense();

        if(result){
            resultMap.put("result","ok");
            resultMap.put("msg",param);
        }else{
            resultMap.put("result","error");
            resultMap.put("msg","证书文件生成失败！");
        }

        return resultMap;
    }

    /**
     * 将证书存放到mongodb中
     * @param
     * @throws FileNotFoundException
     */
    @RequestMapping("/filePush")
    public void filePush(@RequestParam(value = "LicenseName",required = true) String LicenseName) throws FileNotFoundException {
        licensePath = licensePath+LicenseName;
        File file = new File(licensePath);
        FileInputStream fio = new FileInputStream(file);
        ObjectId objectId = gridFsTemplate.store(fio,file.getName(), StandardCharsets.UTF_8);
        System.out.println("文件保存ID：" + objectId);
    }

    /**
     * 获取保存的所有证书名
     * @return
     */
    @RequestMapping("/getAllFileName")
    public Map<Object,String> getFile() {
        Map<Object,String> resultMap = new HashMap<>();
        GridFsResource[] licenseFiles = gridFsTemplate.getResources("*");
        for (GridFsResource liceseFile : licenseFiles) {
            resultMap.put(liceseFile.getFileId(),liceseFile.getFilename());
        }
        return resultMap;
    }

    @RequestMapping("/getFile")
    public void getFile(HttpServletResponse response) throws Exception {
        String id = "5f8161adfef2cb7fc6e8df95";
        Query query = Query.query(Criteria.where("_id").is(id));
        GridFSFile file = gridFsTemplate.findOne(query);
        if(null != file){
            GridFSDownloadStream in = gridFSBucket.openDownloadStream(file.getObjectId());
            GridFsResource resource = new GridFsResource(file,in);
            InputStream inputStream = resource.getInputStream();
            byte[] f = getBytes(inputStream);
            response.setHeader("Content-Disposition", "inline;fileName=\"" + new String((file.getFilename()).getBytes("utf-8"),"ISO8859-1") + "\"");
            OutputStream out = response.getOutputStream();
            out.write(f);
        }
    }

    private byte[] getBytes(InputStream inputStream) throws  Exception{
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] b = new byte[1024];
        int  i = 0;
        while (-1!=(i=inputStream.read(b))){
            bos.write(b,0,i);
        }
        return bos.toByteArray();
    }


}
