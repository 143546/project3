package com.peixinchen.searcher.indexer.mapper;

import com.peixinchen.searcher.indexer.model.Document;
import com.peixinchen.searcher.indexer.model.InvertedRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // 注册 Spring bean
@Mapper     // 是一个 Mybatis 管理的 Mapper
public interface IndexDatabaseMapper {
    void batchInsertForwardIndexes(@Param("list") List<Document> documentList);

    void batchInsertInvertedIndexes(@Param("list") List<InvertedRecord> recordList);
}
