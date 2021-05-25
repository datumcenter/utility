package io.gallery.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class Shell {
    public static String excute(String cmd) {
        String result = "";
        try {
            result = exec(Runtime.getRuntime().exec(cmd));
        } catch (Exception e) {
            result = "执行异常：" + e.getMessage();
            e.printStackTrace();
        }
        return result;
    }


    public static String excute(String[] cmd) {
        String result = "";
        try {
            result = exec(Runtime.getRuntime().exec(cmd));
        } catch (Exception e) {
            result = "执行异常：" + e.getMessage();
            e.printStackTrace();
        }
        return result;
    }

    public static String excute(String cmd, String[] env, File dir) {
        String result = "";
        try {
            result = exec(Runtime.getRuntime().exec(cmd, env, dir));
        } catch (Exception e) {
            result = "执行异常：" + e.getMessage();
            e.printStackTrace();
        }
        return result;
    }

    private static String exec(Process process) throws IOException {
        String result;
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("GBK")));
        if (process != null) {
            process.getOutputStream().close();
        }
        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        result = sb.toString();
        return result;
    }
}
