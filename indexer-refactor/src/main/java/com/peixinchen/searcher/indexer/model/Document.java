package com.peixinchen.searcher.indexer.model;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Data
public class Document {
    private Integer docId;  // docId 会在正排索引插入后才会赋值

    private String title;   // 从文件名中解析出来
    private String url;     // 依赖两个额外的信息（1. https://docs.oracle.com/javase/8/docs/api/  2. 相对路径的相对位置）
    private String content; // 从文件中读取出来，并且做一定的处理

    // 专门给测试用例用的构造方法
    public Document(String title, String url, String content) {
        this.title = title;
        this.url = url;
        this.content = content;
    }

    public Document(File file, String urlPrefix, File rootFile) {
        this.title = parseTitle(file);
        this.url = parseUrl(file, urlPrefix, rootFile);
        this.content = parseContent(file);
//        log.debug("构建文档：{}", title);
    }

    // 针对文档进行分词，并且分别计算每个词的权重
    public Map<String, Integer> segWordAndCalcWeight() {
        // 统计标题中的每个词出现次数 | 分词：标题有哪些词
        List<String> wordInTitle = ToAnalysis.parse(title)
                .getTerms()
                .stream()
                .parallel()
                .map(Term::getName)
                .collect(Collectors.toList());

        // 统计标题中，每个词的出现次数 | 统计次数
        Map<String, Integer> titleWordCount = new HashMap<>();
        for (String word : wordInTitle) {
            int count = titleWordCount.getOrDefault(word, 0);
            titleWordCount.put(word, count + 1);
        }

        // 统计内容中的词，以及词的出现次数
        List<String> wordInContent = ToAnalysis.parse(content)
                .getTerms()
                .stream()
                .parallel()
                .map(Term::getName)
                .collect(Collectors.toList());
        Map<String, Integer> contentWordCount = new HashMap<>();
        for (String word : wordInContent) {
            int count = contentWordCount.getOrDefault(word, 0);
            contentWordCount.put(word, count + 1);
        }

        // 计算权重值
        Map<String, Integer> wordToWeight = new HashMap<>();
        // 先计算出有哪些词，不重复
        Set<String> wordSet = new HashSet<>(wordInTitle);
        wordSet.addAll(wordInContent);

        for (String word : wordSet) {
            int titleCount = titleWordCount.getOrDefault(word, 0);
            int contentCount = contentWordCount.getOrDefault(word, 0);
            int weight = titleCount * 10 + contentCount;

            wordToWeight.put(word, weight);
        }

        return wordToWeight;
    }

    private String parseTitle(File file) {
        // 从文件名中，将 .html 后缀去掉，剩余的看作标题
        String name = file.getName();
        String suffix = ".html";
        return name.substring(0, name.length() - suffix.length());
    }

    @SneakyThrows
    private String parseUrl(File file, String urlPrefix, File rootFile) {
        // 需要得到一个相对路径，file 相对于 rootFile 的相对路径
        // 比如：rootFile 是 D:\课程\2022-08-02-Java19-22班-项目-2\docs\api
        //      file 是     D:\课程\2022-08-02-Java19-22班-项目-2\docs\api\javax\sql\DataSource.html
        // 则相对路径就是：javax\sql\DataSource.html
        // 把所有反斜杠(\) 变成正斜杠(/)
        // 最终得到 java/sql/DataSource.html

        String rootPath = rootFile.getCanonicalPath();
        rootPath = rootPath.replace("/", "\\");
        if (!rootPath.endsWith("\\")) {
            rootPath = rootPath + "\\";
        }

        String filePath = file.getCanonicalPath();
        String relativePath = filePath.substring(rootPath.length());
        relativePath = relativePath.replace("\\", "/");

        return urlPrefix + relativePath;
    }

    @SneakyThrows
    private String parseContent(File file) {
        StringBuilder contentBuilder = new StringBuilder();

        try (InputStream is = new FileInputStream(file)) {
            try (Scanner scanner = new Scanner(is, "ISO-8859-1")) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    contentBuilder.append(line).append(" ");
                }

                return contentBuilder.toString()
                        .replaceAll("<script[^>]*>[^<]*</script>", " ")
                        .replaceAll("<[^>]*>", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
            }
        }
    }

//    @SneakyThrows
//    private String parseContent(File file) {
//        StringBuilder contentBuilder = new StringBuilder();
//        // 假定文件一定是存在的
//        try (InputStream is = new FileInputStream(file)) {
//            // 这些文档的字符是 ISO-8859-1，这个是明确告诉大家的，不是一个知识点
//            try (Scanner scanner = new Scanner(is, "ISO-8859-1")) {
//                while (scanner.hasNextLine()) {
//                    String line = scanner.nextLine();
//
//                    // 首先去掉 <script ...>...</script>
//                    line = line.replaceAll("<script[^>]*>[^<]*</script>", " ");     // 这两个操作会比较慢
//                    // 去掉标签
//                    line = line.replaceAll("<[^>]*>", " ");                         // 这两个操作会比较慢
//                    // 多带的空格的意思是，把 换行符 也视为空格了
//                    contentBuilder.append(line).append(" ");
//                }
//            }
//        }
//
//        // 把最后多出来的空格删除掉
//        return contentBuilder.toString().replaceAll("\\s+", " ").trim();
//    }
}
