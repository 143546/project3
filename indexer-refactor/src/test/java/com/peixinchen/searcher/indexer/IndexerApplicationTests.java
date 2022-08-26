package com.peixinchen.searcher.indexer;

import com.peixinchen.searcher.indexer.command.Indexer;
import com.peixinchen.searcher.indexer.mapper.IndexDatabaseMapper;
import com.peixinchen.searcher.indexer.model.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class IndexerApplicationTests {
    @MockBean   // 通过这个 @MockBean 修饰，使得测试的时候，不需要真正的去执行 Indexer bean 下的操作
    private Indexer indexer;

    @Autowired
    private IndexDatabaseMapper mapper;

    @Test
    void batchInsert() {
        List<Document> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String s = String.valueOf(i);
            Document document = new Document(s, s, s);
            list.add(document);
        }

        mapper.batchInsertForwardIndexes(list);

        for (Document document : list) {
            System.out.println(document);
        }
    }
}
