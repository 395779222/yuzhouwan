package com.yuzhouwan.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Copyright @ 2016 yuzhouwan.com
 * All right reserved.
 * Function: File Utils
 *
 * @author Benedict Jin
 * @since 2016/8/20
 */
public class FileUtils {

    public static byte[] readFile(String filename) throws IOException {
        File file = new File(filename);
        long len = file.length();
        byte data[] = new byte[(int) len];
        FileInputStream fin = new FileInputStream(file);
        int r = fin.read(data);
        if (r != len)
            throw new IOException("Only read " + r + " of " + len + " for " + file);
        fin.close();
        return data;
    }

    public static void writeFile(String filename, byte data[])
            throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(filename);
        fileOutputStream.write(data);
        fileOutputStream.close();
    }
}