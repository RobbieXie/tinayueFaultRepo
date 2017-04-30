package com.tiandi.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * @author 谢天帝
 * @version v0.1 2017/2/22.
 */
public interface CloudFailureRepository extends MongoRepository<CloudFailure,String> {
//    public CloudFailure findByName(String name);
    public CloudFailure findById(String id);
    public List<CloudFailure> findByIndex(String index);
}
