package com.peixinchen.searcher.indexer.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

@Slf4j      // 添加日志
@Service    // 注册成 Spring bean
public class FileScanner {
    /**
     * 以 rootPath 作为根目录，开始进行文件的扫描，把所有符合条件的 File 对象，作为结果，以 List 形式返回
     * @param rootPath 根目录的路径，调用者需要确保这个目录存在 && 一定是一个目录
     * @param filter 通过针对每个文件调用 filter.accept(file) 就知道，文件是否满足条件
     * @return 满足条件的所有文件
     */
    public List<File> scanFile(String rootPath, FileFilter filter) {
        List<File> resultList = new ArrayList<>();
        File rootFile = new File(rootPath);

        // 针对目录树进行遍历，深度优先 or 广度优先即可，确保每个文件都没遍历到即可
        // 我们这里采用深度优先遍历，使用递归完成
        traversal(rootFile, filter, resultList);

        return resultList;
    }

    private void traversal(File directoryFile, FileFilter filter, List<File> resultList) {
        // 1. 先通过目录，得到该目录下的孩子文件有哪些
        File[] files = directoryFile.listFiles();
        if (files == null) {
            // 说明有问题，我们不管（一般是权限等的问题），通常咱们遇不到这个错误
            return;
        }

        // 2. 遍历每个文件，检查是否符合条件
        for (File file : files) {
            // 通过 filter.accept(file) 的返回值，判断是否符合条件
            if (filter.accept(file)) {
                // 说明符合条件，需要把该文件加入到结果 List 中
                resultList.add(file);
            }
        }

        // 3. 遍历每个文件，针对是目录的情况，继续深度优先遍历（递归）
        for (File file : files) {
            if (file.isDirectory()) {
                traversal(file, filter, resultList);
            }
        }
    }
}
