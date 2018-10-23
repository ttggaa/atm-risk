package com.risk.controller.service.mongo.dao;

import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class MongoDao {
    Logger logger = Logger.getLogger(this.getClass());

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 保存数据
     **/
    public void save(Object obj, String collectionName) {
        try {
            // 增加创建时间、更新时间
            JSONObject jsonObject = null;
            if (obj instanceof JSONObject) {
                jsonObject = (JSONObject) obj;
            } else {
                jsonObject = JSONObject.parseObject(obj.toString());
            }
            jsonObject.put("createTime", System.currentTimeMillis());
            jsonObject.put("updateTime", System.currentTimeMillis());
            mongoTemplate.save(jsonObject, collectionName);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }


    public <T> T findOne(List<MongoQuery> yhbQueries, Class<T> cless, String collectionName) {
        Sort sort = new Sort(Sort.Direction.DESC, "createTime");
        return this.findOne(yhbQueries, cless, collectionName, sort);
    }

    /**
     * 查询mongo
     * @param yhbQueries
     * @param cless
     * @param collectionName
     * @param sort
     * @param <T>
     * @return
     */
    public <T> T findOne(List<MongoQuery> yhbQueries, Class<T> cless, String collectionName, Sort sort) {
        Query query = new Query();
        for (MongoQuery MongoQuery : yhbQueries) {
            if (com.risk.controller.service.mongo.dao.MongoQuery.MongoQueryBaseType.eq.equals(MongoQuery.getBaseType())) {
                query.addCriteria(new Criteria(MongoQuery.getKey()).is(MongoQuery.getValue()));
            } else if (com.risk.controller.service.mongo.dao.MongoQuery.MongoQueryBaseType.ge.equals(MongoQuery.getBaseType())) {
                query.addCriteria(new Criteria(MongoQuery.getKey()).gte(MongoQuery.getValue()));
            } else if (com.risk.controller.service.mongo.dao.MongoQuery.MongoQueryBaseType.gt.equals(MongoQuery.getBaseType())) {
                query.addCriteria(new Criteria(MongoQuery.getKey()).gt(MongoQuery.getValue()));
            } else if (com.risk.controller.service.mongo.dao.MongoQuery.MongoQueryBaseType.le.equals(MongoQuery.getBaseType())) {
                query.addCriteria(new Criteria(MongoQuery.getKey()).lte(MongoQuery.getValue()));
            } else if (com.risk.controller.service.mongo.dao.MongoQuery.MongoQueryBaseType.lt.equals(MongoQuery.getBaseType())) {
                query.addCriteria(new Criteria(MongoQuery.getKey()).lt(MongoQuery.getValue()));
            }
        }
        if (!mongoTemplate.collectionExists(collectionName)) {
            mongoTemplate.createCollection(collectionName);
        }
        if (null != sort) {
            query = query.with(sort);
        }

        return mongoTemplate.findOne(query, cless, collectionName);
    }

    public void update(List<MongoQuery> yhbQueries, Update update, String collectionName) {
        Query query = new Query();
        for (MongoQuery MongoQuery : yhbQueries) {
            if (com.risk.controller.service.mongo.dao.MongoQuery.MongoQueryBaseType.eq.equals(MongoQuery.getBaseType())) {
                query.addCriteria(new Criteria(MongoQuery.getKey()).is(MongoQuery.getValue()));
            } else if (com.risk.controller.service.mongo.dao.MongoQuery.MongoQueryBaseType.ge.equals(MongoQuery.getBaseType())) {
                query.addCriteria(new Criteria(MongoQuery.getKey()).gte(MongoQuery.getValue()));
            } else if (com.risk.controller.service.mongo.dao.MongoQuery.MongoQueryBaseType.gt.equals(MongoQuery.getBaseType())) {
                query.addCriteria(new Criteria(MongoQuery.getKey()).gt(MongoQuery.getValue()));
            } else if (com.risk.controller.service.mongo.dao.MongoQuery.MongoQueryBaseType.le.equals(MongoQuery.getBaseType())) {
                query.addCriteria(new Criteria(MongoQuery.getKey()).lte(MongoQuery.getValue()));
            } else if (com.risk.controller.service.mongo.dao.MongoQuery.MongoQueryBaseType.lt.equals(MongoQuery.getBaseType())) {
                query.addCriteria(new Criteria(MongoQuery.getKey()).lt(MongoQuery.getValue()));
            }
        }
        mongoTemplate.upsert(query, update, collectionName);
    }

    public <T> List<T> find(List<MongoQuery> yhbQueries, Class<T> cless, String collectionName, Sort sort) {
        Query query = new Query();
        for (MongoQuery MongoQuery : yhbQueries) {
            if (com.risk.controller.service.mongo.dao.MongoQuery.MongoQueryBaseType.eq.equals(MongoQuery.getBaseType())) {
                query.addCriteria(new Criteria(MongoQuery.getKey()).is(MongoQuery.getValue()));
            } else if (com.risk.controller.service.mongo.dao.MongoQuery.MongoQueryBaseType.ge.equals(MongoQuery.getBaseType())) {
                query.addCriteria(new Criteria(MongoQuery.getKey()).gte(MongoQuery.getValue()));
            } else if (com.risk.controller.service.mongo.dao.MongoQuery.MongoQueryBaseType.gt.equals(MongoQuery.getBaseType())) {
                query.addCriteria(new Criteria(MongoQuery.getKey()).gt(MongoQuery.getValue()));
            } else if (com.risk.controller.service.mongo.dao.MongoQuery.MongoQueryBaseType.le.equals(MongoQuery.getBaseType())) {
                query.addCriteria(new Criteria(MongoQuery.getKey()).lte(MongoQuery.getValue()));
            } else if (com.risk.controller.service.mongo.dao.MongoQuery.MongoQueryBaseType.lt.equals(MongoQuery.getBaseType())) {
                query.addCriteria(new Criteria(MongoQuery.getKey()).lt(MongoQuery.getValue()));
            }
        }
        if (!mongoTemplate.collectionExists(collectionName)) {
            mongoTemplate.createCollection(collectionName);
        }
        if (null != sort) {
            query = query.with(sort);
        }
        return mongoTemplate.find(query, cless, collectionName);
    }
}
