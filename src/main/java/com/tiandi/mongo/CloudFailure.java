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
    public List<String> tags;
    public String faultLocation;
    public String cause;
    public List<String> index;
    public List<String> children;

    public CloudFailure() {}

    public CloudFailure(String id, String name, String description, Boolean isCategory, List<String> tags, String faultLocation, String cause, List<String> index, List<String> children) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isCategory = isCategory;
        this.tags = tags;
        this.faultLocation = faultLocation;
        this.cause = cause;
        this.index = index;
        this.children = children;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getCategory() {
        return isCategory;
    }

    public void setCategory(Boolean category) {
        isCategory = category;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getFaultLocation() {
        return faultLocation;
    }

    public void setFaultLocation(String faultLocation) {
        this.faultLocation = faultLocation;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public List<String> getIndex() {
        return index;
    }

    public void setIndex(List<String> index) {
        this.index = index;
    }

    public List<String> getChildren() {
        return children;
    }

    public void setChildren(List<String> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return String.format(
                "CloudFailure[id=%s]",
                id);
    }

}
