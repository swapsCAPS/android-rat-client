package com.safe.myapp;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class SafeRestClient {
    private static final String BASE_URL = SafeService.HTTP_SERVER;
    private static final int TIMEOUT = 30 * 1000;

    private static AsyncHttpClient asyncHttpClientlient = new AsyncHttpClient(SafeService.HTTP_PORT);

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        asyncHttpClientlient.setTimeout(TIMEOUT);
        asyncHttpClientlient.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        asyncHttpClientlient.setTimeout(TIMEOUT);
        asyncHttpClientlient.post(getAbsoluteUrl(url), params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}