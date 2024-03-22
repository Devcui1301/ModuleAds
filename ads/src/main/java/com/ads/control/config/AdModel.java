package com.ads.control.config;

public class AdModel {

    private String id;
    private String adName;

    public AdModel(String id, String adName) {
        this.id = id;
        this.adName = adName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAdName() {
        return adName;
    }

    public void setAdName(String adName) {
        this.adName = adName;
    }
}
