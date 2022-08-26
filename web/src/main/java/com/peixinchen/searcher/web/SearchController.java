package com.peixinchen.searcher.web;

import lombok.extern.slf4j.Slf4j;
import org.ansj.domain.Term;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class SearchController {
    private final SearchMapper mapper;
    private final DescBuilder descBuilder;

    @Autowired
    public SearchController(SearchMapper mapper, DescBuilder descBuilder) {
        this.descBuilder = descBuilder;
        ToAnalysis.parse("预热分词");
        this.mapper = mapper;
    }

    @GetMapping("/web")
    public String search(String query, @RequestParam(value = "page", required = false) String pageString, Model model) {
        log.debug("查询: query = {}", query);

        // 参数的合法性检查 + 处理
        if (query == null) {
            log.debug("query 为 null，重定向到首页");
            return "redirect:/";
        }

        query = query.trim().toLowerCase();
        if (query.isEmpty()) {
            log.debug("query 为空字符串，重定向到首页");
            return "redirect:/";
        }

        // 分词
        List<String> queryList = ToAnalysis.parse(query)
                .getTerms()
                .stream()
                .map(Term::getName)
                .collect(Collectors.toList());

        if (queryList.isEmpty()) {
            log.debug("query 分词后一个词都没有，重定向到首页");
            return "redirect:/";
        }

        log.debug("进行查询的词: {}", query);

        int limit = 20;
        int offset = 0;
        int page = 1;

        if (pageString != null) {
            pageString = pageString.trim();
            try {
                page = Integer.parseInt(pageString);
                if (page <= 0) {
                    page = 1;
                }

                limit = page * 20;
            } catch (NumberFormatException ignored) {}
        }

        log.debug("limit = {}, offset = {}, page = {}", limit, offset, page);

        // 分别搜索 -> 聚合 -> 排序 -> 区间

        List<DocumentWightWeight> totalList = new ArrayList<>();
        for (String s : queryList) {
            List<DocumentWightWeight> documentList = mapper.queryWithWeight(s, limit, offset);
            totalList.addAll(documentList);
        }

        // 针对所有文档列表，做权重聚合工作
        // 维护:
        // docId -> document 的 map
        Map<Integer, DocumentWightWeight> documentMap = new HashMap<>();
        for (DocumentWightWeight documentWightWeight : totalList) {
            int docId = documentWightWeight.getDocId();
            if (documentMap.containsKey(docId)) {
                DocumentWightWeight item = documentMap.get(docId);
                item.weight += documentWightWeight.weight;
                continue;
            }

            DocumentWightWeight item = new DocumentWightWeight(documentWightWeight);
            documentMap.put(docId, item);
        }

        Collection<DocumentWightWeight> values = documentMap.values();
        // Collection 没有排序这个概念（只有线性结构才有排序的概念），所以我们需要一个 List
        List<DocumentWightWeight> list = new ArrayList<>(values);

        // 按照 weight 的从大到小排序了
        Collections.sort(list, (item1, item2) -> {
            return item2.weight - item1.weight;
        });

        int from = (page - 1) * 20;
        int to = Integer.min(from + 20, list.size());
        // 从 list 中把分页区间取出来
        List<DocumentWightWeight> subList = list.subList(from, to);
        List<Document> documentList = subList.stream()
                .map(DocumentWightWeight::toDocument)
                .collect(Collectors.toList());

        // lambda 中无法使用非 final 变量
        List<String> wordList = queryList;
        documentList = documentList.stream()
                .map(doc -> descBuilder.build(wordList, doc))
                .collect(Collectors.toList());

        // 这里将数据添加到 model 中，是为了在 渲染模板的时候用到
//        model.addAttribute("name", "陈沛鑫");
        model.addAttribute("query", query);
        model.addAttribute("docList", documentList);
        model.addAttribute("page", page);
//        List<String> testList = new ArrayList<>();
//        testList.add("甲");
//        testList.add("乙");
//        testList.add("丙");
//        testList.add("丁");
//        testList.add("戊");
//        model.addAttribute("testList", testList);

        return "search";
    }
}
