package com.peixinchen.searcher.web;

import lombok.Data;

@Data
public class DocumentWightWeight {
    private int docId;
    private String title;
    private String url;
    private String content;
    public int weight;

    public DocumentWightWeight() {}
    public DocumentWightWeight(DocumentWightWeight documentWightWeight) {
        this.docId = documentWightWeight.docId;
        this.title = documentWightWeight.title;
        this.url = documentWightWeight.url;
        this.content = documentWightWeight.content;
        this.weight = documentWightWeight.weight;
    }

    public Document toDocument() {
        Document document = new Document();
        document.setDocId(this.docId);
        document.setTitle(this.title);
        document.setUrl(this.url);
        document.setContent(this.content);

        return document;
    }
}
