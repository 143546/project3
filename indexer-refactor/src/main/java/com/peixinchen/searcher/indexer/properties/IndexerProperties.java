package com.peixinchen.searcher.indexer.properties;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component  // 是注册到 Spring 的一个 bean
@ConfigurationProperties("searcher.indexer")
@Data // = @Getter + @Setter + @ToString + @EqualsAndHashCode
public class IndexerProperties {
    // 对应 application.yml 配置下的 searcher.indexer.doc-root-path
    private String docRootPath;
    // 对应 application.yml 配置下的 searcher.indexer.url-prefix
    private String urlPrefix;
    // 对应 application.yml 配置下的 searcher.indexer.index-root-path
    private String indexRootPath;
}
