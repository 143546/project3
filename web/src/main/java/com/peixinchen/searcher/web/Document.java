package com.peixinchen.searcher.web;

import lombok.Data;

@Data
public class Document {
    private Integer docId;
    private String title;
    private String url;
    private String content;
    private String desc;

    @Override
    public String toString() {
        return String.format("Document{docId=%d, title=%s, url=%s}", docId, title, url);
    }
}
