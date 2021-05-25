package io.gallery.util;

import io.gallery.util.bean.FileInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Explorer {
    private static final Log logger = LogFactory.getLog(Explorer.class);

    public static List<FileInfo> listRoots() {
        List<FileInfo> result = new ArrayList<>();
        for (File root : File.listRoots()) {
            result.add(getFileInfo(root));
        }
        return result;
    }

    public static List<FileInfo> listFiles(File path) {
        List<FileInfo> result = new ArrayList<>();
        for (File file : path.listFiles()) {
            result.add(getFileInfo(file));
        }
        return result;
    }

    public static FileInfo getFileInfo(File file) {
        return Optional.ofNullable(file).map(f -> new FileInfo() {{
            setOriginal(f);
            setName(f.getName());
            setPath(f.getPath());
            setAbsoluteFile(f.getAbsolutePath());
            setAbsolutePath(f.getAbsolutePath());
            setLastModified(f.lastModified());
            setTotalSpace(f.getTotalSpace());
            setFreeSpace(f.getFreeSpace());
            setUsableSpace(f.getUsableSpace());
            setLength(f.length());
            setAbsolute(f.isAbsolute());
            setDirectory(f.isDirectory());
            setHidden(f.isHidden());
            setFile(f.isFile());
        }}).orElse(null);

    }
}
