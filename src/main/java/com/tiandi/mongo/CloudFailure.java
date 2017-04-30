package com.tiandi.mongo;

import org.springframework.data.annotation.Id;

import java.util.List;

/**
 * @author 谢天帝
 * @version v0.1 2017/2/22.
 */
public class CloudFailure {
    @Id
    public String id;

    public String name;
    public String description;
    public Boolean isCategory;
    public String faultLocation;
    public String cause;
    public List<String> index;
    public List<String> children;

    public CloudFailure() {}

    public CloudFailure(String id, String name, String description, Boolean isCategory, String faultLocation, String cause, List<String> index, List<String> children) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isCategory = isCategory;
        this.faultLocation = faultLocation;
        this.cause = cause;
        this.index = index;
        this.children = children;
    }

    @Override
    public String toString() {
        return String.format(
                "CloudFailure[id=%s]",
                id);
    }

}
