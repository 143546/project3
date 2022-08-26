package com.peixinchen.searcher.indexer.core;

import com.peixinchen.searcher.indexer.aop.Timing;
import com.peixinchen.searcher.indexer.mapper.IndexDatabaseMapper;
import com.peixinchen.searcher.indexer.model.Document;
import com.peixinchen.searcher.indexer.model.InvertedRecord;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class IndexManager {
    private final IndexDatabaseMapper mapper;
    private final ExecutorService executorService;

    @Autowired
    public IndexManager(IndexDatabaseMapper mapper, ExecutorService executorService) {
        this.mapper = mapper;
        this.executorService = executorService;
    }

    // 先批量生成、保存正排索引（单线程版本）
    public void saveForwardIndexes(List<Document> documentList) {
        // 1. 批量插入时，每次插入多少条记录（由于每条记录比较大，所以这里使用 10 条就够了）
        int batchSize = 10;
        // 2. 一共需要执行多少次 SQL？   向上取整(documentList.size() / batchSize)
        int listSize = documentList.size();
        int times = (int) Math.ceil(1.0 * listSize / batchSize);    // ceil(天花板): 向上取整
        log.debug("一共需要 {} 批任务。", times);

        // 3. 开始分批次插入
        for (int i = 0; i < listSize; i += batchSize) {
            // 从 documentList 中截取这批要插入的 文档列表（使用 List.subList(int from, int to)
            int from = i;
            int to = Integer.min(from + batchSize, listSize);

            List<Document> subList = documentList.subList(from, to);

            // 针对这个 subList 做批量插入
            mapper.batchInsertForwardIndexes(subList);
        }
    }

    @Timing("构建 + 保存正排索引 —— 多线程版本")
    @SneakyThrows
    public void saveForwardIndexesConcurrent(List<Document> documentList) {
        // 1. 批量插入时，每次插入多少条记录（由于每条记录比较大，所以这里使用 10 条就够了）
        int batchSize = 10;
        // 2. 一共需要执行多少次 SQL？   向上取整(documentList.size() / batchSize)
        int listSize = documentList.size();
        int times = (int) Math.ceil(1.0 * listSize / batchSize);    // ceil(天花板): 向上取整
        log.debug("一共需要 {} 批任务。", times);

        CountDownLatch latch = new CountDownLatch(times);   // 统计每个线程的完全情况，初始值是 times(一共多少批)

        // 3. 开始分批次插入
        for (int i = 0; i < listSize; i += batchSize) {
            // 从 documentList 中截取这批要插入的 文档列表（使用 List.subList(int from, int to)
            int from = i;
            int to = Integer.min(from + batchSize, listSize);

            Runnable task = () -> { // 内部类 / lambda 表达式里如果用到了外部变量，外部变量必须的 final（或者隐式 final 的变量）
                List<Document> subList = documentList.subList(from, to);

                // 针对这个 subList 做批量插入
                mapper.batchInsertForwardIndexes(subList);

                latch.countDown();      //  每次任务完成之后，countDown()，让 latch 的个数减一
            };

            executorService.submit(task);   // 主线程只负责把一批批的任务提交到线程池，具体的插入工作，由线程池中的线程完成
        }

        // 4. 循环结束，只意味着主线程把任务提交完成了，但任务有没有做完是不知道的
        // 主线程等在 latch 上，只到 latch 的个数变成 0，也就是所有任务都已经执行完了
        latch.await();
    }

    @SneakyThrows
    public void saveInvertedIndexes(List<Document> documentList) {
        int batchSize = 10000;  // 批量插入时，最多 10000 条

        List<InvertedRecord> recordList = new ArrayList<>();    // 放这批要插入的数据

        for (Document document : documentList) {
            Map<String, Integer> wordToWeight = document.segWordAndCalcWeight();
            for (Map.Entry<String, Integer> entry : wordToWeight.entrySet()) {
                String word = entry.getKey();
                int docId = document.getDocId();
                int weight = entry.getValue();

                InvertedRecord record = new InvertedRecord(word, docId, weight);

                recordList.add(record);

                // 如果 recordList.size() == batchSize，说明够一次插入了
                if (recordList.size() == batchSize) {
                    mapper.batchInsertInvertedIndexes(recordList);  // 批量插入
                    recordList.clear();                             // 清空 list，视为让 list.size() = 0
                }
            }
        }

        // recordList 还剩一些，之前放进来，但还不够 batchSize 个的，所以最后再批量插入一次
        mapper.batchInsertInvertedIndexes(recordList);  // 批量插入
        recordList.clear();
    }

    static class InvertedInsertTask implements Runnable {
        private final CountDownLatch latch;
        private final int batchSize;
        private final List<Document> documentList;
        private final IndexDatabaseMapper mapper;

        InvertedInsertTask(CountDownLatch latch, int batchSize, List<Document> documentList, IndexDatabaseMapper mapper) {
            this.latch = latch;
            this.batchSize = batchSize;
            this.documentList = documentList;
            this.mapper = mapper;
        }

        @Override
        public void run() {
            List<InvertedRecord> recordList = new ArrayList<>();    // 放这批要插入的数据

            for (Document document : documentList) {
                Map<String, Integer> wordToWeight = document.segWordAndCalcWeight();
                for (Map.Entry<String, Integer> entry : wordToWeight.entrySet()) {
                    String word = entry.getKey();
                    int docId = document.getDocId();
                    int weight = entry.getValue();

                    InvertedRecord record = new InvertedRecord(word, docId, weight);

                    recordList.add(record);

                    // 如果 recordList.size() == batchSize，说明够一次插入了
                    if (recordList.size() == batchSize) {
                        mapper.batchInsertInvertedIndexes(recordList);  // 批量插入
                        recordList.clear();                             // 清空 list，视为让 list.size() = 0
                    }
                }
            }

            // recordList 还剩一些，之前放进来，但还不够 batchSize 个的，所以最后再批量插入一次
            mapper.batchInsertInvertedIndexes(recordList);  // 批量插入
            recordList.clear();

            latch.countDown();
        }
    }

    @Timing("构建 + 保存倒排索引 —— 多线程版本")
    @SneakyThrows
    public void saveInvertedIndexesConcurrent(List<Document> documentList) {
        int batchSize = 10000;  // 批量插入时，最多 10000 条
        int groupSize = 50;
        int listSize = documentList.size();
        int times = (int) Math.ceil(listSize * 1.0 / groupSize);
        CountDownLatch latch = new CountDownLatch(times);

        for (int i = 0; i < listSize; i += groupSize) {
            int from = i;
            int to = Integer.min(from + groupSize, listSize);
            List<Document> subList = documentList.subList(from, to);
            Runnable task = new InvertedInsertTask(latch, batchSize, subList, mapper);
            executorService.submit(task);
        }

        latch.await();
    }
}
