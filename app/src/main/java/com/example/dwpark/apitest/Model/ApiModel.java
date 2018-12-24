package com.example.dwpark.apitest.Model;

import com.example.dwpark.apitest.Rf.RfFactory;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by dwpark on 2017. 9. 19..
 */

public interface ApiModel {
    @GET("/test/test.json?")
    RfFactory.Call_<Dworks> getDworks();
    @GET("/test/test.json?")
    RfFactory.Call_<Dummy> getDummy();
}
