package com.peixinchen.searcher.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DescBuilder {
    public Document build(List<String> queryList, Document doc) {
        // 找到 content 中包含关键字的位置
        // query = "list"
        // content = "..... hello list go come do ...."
        // desc = "hello <i>list</i> go com..."

        String content = doc.getContent().toLowerCase();
        String word = "";
        int i = -1;
        for (String query : queryList) {
            i = content.indexOf(query);
            if (i != -1) {
                word = query;
                break;
            }
        }
        if (i == -1) {
            // 这里中情况如果出现了，说明咱的倒排索引建立的有问题
            log.error("docId = {} 中不包含 {}", doc.getDocId(), queryList);
            throw new RuntimeException();
        }

        // 前面截 120 个字，后边截 120 个字
        int from = i - 120;
        if (from < 0) {
            // 说明前面不够 120 个字了
            from = 0;
        }

        int to = i + 120;
        if (to > content.length()) {
            // 说明后面不够 120 个字了
            to = content.length();
        }

        String desc = content.substring(from, to);

        desc = desc.replace(word, "<i>" + word + "</i>");

        doc.setDesc(desc);

        return doc;
    }
}
