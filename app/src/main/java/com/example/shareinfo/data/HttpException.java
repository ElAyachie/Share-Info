package com.example.shareinfo.data;

import java.io.IOException;

/** A request that reached the server but came back with a non-2xx status. */
public class HttpException extends IOException {
    public final int code;

    public HttpException(int code, String url) {
        super("HTTP " + code + " from " + url);
        this.code = code;
    }
}
