package io.gallery.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;

import java.io.File;

class ShellTest {

    private static final Log logger = LogFactory.getLog(ShellTest.class);

    @Test
    public void excute() {
        String result = Shell.excute("tasklist -fi " + '"' + "imagename eq " + "nginx.exe" + '"');
        logger.debug(result);
    }

    @Test
    public void startNginx() {
        String result = Shell.excute("cmd /c start nginx", new String[]{}, new File("D:\\Program Files\\nginx-1.16.1"));
        //"/usr/local/nginx/sbin/nginx"
        logger.debug(result);
    }

    @Test
    public void killNginx() {
        String result = Shell.excute("taskkill /f /t /im nginx.exe");
        //String result = ShellUtil.excute("pkill -9 nginx");
        logger.debug(result);
    }
}