package io.gallery.util;

import io.gallery.util.bean.FileInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

class ExplorerTest {
    private static final Log logger = LogFactory.getLog(ExplorerTest.class);

    @Test
    void listRoots() {
        List<FileInfo> files = Explorer.listRoots();
        logger.debug(JSON.toJSONString(files));
        for (FileInfo file : files) {
            Explorer.listFiles(file.getOriginal());
        }
    }
}