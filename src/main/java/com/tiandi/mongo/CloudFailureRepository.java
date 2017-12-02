package com.tiandi.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/**
 * @author 谢天帝
 * @version v0.1 2017/2/22.
 */
public interface CloudFailureRepository extends MongoRepository<CloudFailure,String> {
//    public CloudFailure findByName(String name);
    public CloudFailure findById(String id);
    public List<CloudFailure> findByIndex(String index);
    public List<CloudFailure> findByIsCategory(Boolean isCategory);

    @Query("{'index':{ '$size':?0}}")
    public List<CloudFailure> findByIndexSize(int size);
}
