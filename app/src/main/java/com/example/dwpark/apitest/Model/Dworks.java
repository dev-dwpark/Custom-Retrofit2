package com.example.dwpark.apitest.Model;

import com.example.dwpark.apitest.Rf.DxApi;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dwpark on 2017. 9. 7..
 */

public class Dworks extends DxApi.Data {
    @SerializedName("greatTitle") private String greatTitle;
    @SerializedName("greatDescription") private String greatDescription;
    @SerializedName("integerVal") private int integerVal;
    @SerializedName("data") private List<Data> data = new ArrayList<>();

    public class Data{
        @SerializedName("title") private String title;
        @SerializedName("item") private List<Item> item;

        public String getTitle() {
            return title;
        }

        public List<Item> getItem() {
            return item;
        }
    }

    public class Item{
        @SerializedName("index") private int index;
        @SerializedName("description") private String description;

        public int getIndex() {
            return index;
        }

        public String getDescription() {
            return description;
        }
    }

    public String getGreatTitle() {
        return greatTitle;
    }

    public String getGreatDescription() {
        return greatDescription;
    }

    public int getIntegerVal() {
        return integerVal;
    }

    public List<Data> getData() {
        return data;
    }
}




