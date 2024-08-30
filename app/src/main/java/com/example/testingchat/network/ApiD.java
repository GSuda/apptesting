package com.example.testingchat.network;

import java.util.List;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiD {
    @Multipart
    @POST("/direction") // Replace with the actual endpoint for ApiD
    Call<List<Integer>> uploadFile(@Part MultipartBody.Part file);
}





