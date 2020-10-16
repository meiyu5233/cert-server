package com.tmxbase.certserver.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

/**
 * @Auther meiyu
 * @Date 2020/10/9
 */
@Configuration
public class MongoConfig {
    //获取配置文件中数据库信息
    @Value("${spring.data.mongodb.pr.database}")
    String db;

    ////GridFSBucket用于打开下载流
    @Bean
    public GridFSBucket getGridFSBucket(MongoClient mongoClient){
        MongoDatabase mongoDatabase = mongoClient.getDatabase("License");
        GridFSBucket bucket = GridFSBuckets.create(mongoDatabase,"fs1");
        return bucket;
    }

    @Bean
    public GridFsTemplate gridFsTemplate(MongoDatabaseFactory factory, MongoConverter converter) {
        return new GridFsTemplate(factory, converter, "fs1");
    }
}