package com.peixinchen.searcher.indexer.model;

import lombok.Data;

// 这个对象映射 inverted_indexes 表中的一条记录（我们不关心表中的 id，就不写 id 了）
@Data
public class InvertedRecord {
    private String word;
    private int docId;
    private int weight;

    public InvertedRecord(String word, int docId, int weight) {
        this.word = word;
        this.docId = docId;
        this.weight = weight;
    }
}
