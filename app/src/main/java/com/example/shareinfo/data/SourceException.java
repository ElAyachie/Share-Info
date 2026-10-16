package com.example.shareinfo.data;

import java.io.IOException;

/** A source failure whose message is short and written for the user ("daily quota reached"). */
public class SourceException extends IOException {
    public SourceException(String userMessage) {
        super(userMessage);
    }

    public SourceException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
