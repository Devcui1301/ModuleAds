package com.ads.control.applovin;

import com.ads.control.config.AdModel;

import java.util.ArrayList;

public class MaxPlacement {

    public static ArrayList<AdModel> listAdModel = new ArrayList<AdModel>();


    public static void setListAdModel(ArrayList<AdModel> listAdModel) {
        MaxPlacement.listAdModel = listAdModel;
    }

    public static String getNamePlacement(String idAds) {
        for (AdModel adModel : listAdModel) {
            if (adModel.getId().equals(idAds)) {
                return adModel.getAdName();
            }
        }
        return "";
    }

}
