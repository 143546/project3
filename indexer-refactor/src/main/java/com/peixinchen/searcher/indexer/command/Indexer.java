package com.peixinchen.searcher.indexer.command;

import com.peixinchen.searcher.indexer.core.IndexManager;
import com.peixinchen.searcher.indexer.model.Document;
import com.peixinchen.searcher.indexer.util.FileScanner;
import com.peixinchen.searcher.indexer.properties.IndexerProperties;
import lombok.extern.slf4j.Slf4j;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * 构建索引的模块，是整个程序的逻辑入口
 */
@Slf4j      // 添加 Spring 日志的使用
@Component  // 注册成 Spring 的 bean
//@Profile("run") // 让跑测试的时候不加载这个 bean（run != test)
public class Indexer implements CommandLineRunner {
    // 需要依赖 FileScanner 对象
    private final FileScanner fileScanner;
    private final IndexerProperties properties;
    private final IndexManager indexManager;
    private final ExecutorService executorService;

    @Autowired  // 构造方法注入的方式，让 Spring 容器，注入 FileScanner 对象进来 —— DI
    public Indexer(FileScanner fileScanner, IndexerProperties properties, IndexManager indexManager, ExecutorService executorService) {
        this.fileScanner = fileScanner;
        this.properties = properties;
        this.indexManager = indexManager;
        this.executorService = executorService;
    }

    @Override
    public void run(String... args) throws Exception {
        ToAnalysis.parse("随便分个什么，进行预热，避免优化的时候计算第一次特别慢的时间");

        log.info("这里的整个程序的逻辑入口");

        // 1. 扫描出来所有的 html 文件
        log.debug("开始扫描目录，找出所有的 html 文件。{}", properties.getDocRootPath());
        List<File> htmlFileList = fileScanner.scanFile(properties.getDocRootPath(), file -> {
            return file.isFile() && file.getName().endsWith(".html");
        });
        log.debug("扫描目录结束，一共得到 {} 个文件。", htmlFileList.size());

        // 2. 针对每个 html 文件，得到其 标题、URL、正文信息，把这些信息封装成一个对象（文档 Document）
        File rootFile = new File(properties.getDocRootPath());
        List<Document> documentList = htmlFileList.stream()
                .parallel()         // 【注意】由于我们使用了 Stream 用法，所以，可以通过添加 .parallel()，使得整个操作变成并行，利用多核增加运行速度
                .map(file -> new Document(file, properties.getUrlPrefix(), rootFile))
                .collect(Collectors.toList());
        log.debug("构建文档完毕，一共 {} 篇文档", documentList.size());

        // 3. 进行正排索引的保存
        indexManager.saveForwardIndexesConcurrent(documentList);
        log.debug("正排索引保存成功。");

        // 4. 进行倒排索引的生成核保存
        indexManager.saveInvertedIndexesConcurrent(documentList);
        log.debug("倒排索引保存成功。");

        // 5. 关闭线程池
        executorService.shutdown();
    }
}
