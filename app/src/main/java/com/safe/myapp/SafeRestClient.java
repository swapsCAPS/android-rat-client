package com.safe.myapp;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

public class SafeRestClient {
    private static final String BASE_URL = SafeService.HTTP_SERVER;
    private static final int TIMEOUT = 30 * 1000;


    private static SyncHttpClient syncHttpClientlient = new SyncHttpClient(SafeService.HTTP_PORT);

    public static void syncGet(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        syncHttpClientlient.setTimeout(TIMEOUT);
        syncHttpClientlient.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void syncPost(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        syncHttpClientlient.setTimeout(TIMEOUT);
        syncHttpClientlient.post(getAbsoluteUrl(url), params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}