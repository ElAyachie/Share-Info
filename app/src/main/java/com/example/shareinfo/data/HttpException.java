package com.example.shareinfo.data;

import java.io.IOException;

/** A request that reached the server but came back with a non-2xx status. */
public class HttpException extends IOException {
    public final int code;
    /** The start of the error response, which some APIs use to say why; may be empty. */
    public final String body;

    public HttpException(int code, String url) {
        this(code, url, "");
    }

    public HttpException(int code, String url, String body) {
        super("HTTP " + code + " from " + url);
        this.code = code;
        this.body = body == null ? "" : body;
    }
}
