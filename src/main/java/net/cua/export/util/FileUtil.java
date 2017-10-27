package net.cua.export.util;


import com.sun.istack.internal.NotNull;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 文件操作工具类
 *
 * @author Sun Xiaochen
 * @since 2008-2-1
 */

public class FileUtil {
    static Logger logger = Logger.getLogger(FileUtil.class);

    // 限制实例化
    private FileUtil() {
    }


    /**
     * 关闭输入流
     *
     * @param inputStream 文件输入流
     */
    public static void close(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.error("close FileInputStream fail", e);
            }
        }
    }

    /**
     * 关闭输出流
     *
     * @param outputStream 文件输出流
     */
    public static void close(OutputStream outputStream) {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                logger.error("close FileInputStream fail", e);
            }
        }
    }

    /**
     * 关闭BufferedReader
     *
     * @param br BufferedReader
     */
    public static void close(Reader br) {
        if (br != null) {
            try {
                br.close();
            } catch (IOException e) {
                logger.error("close BufferedReader fail", e);
            }
        }
    }

    /**
     * 关闭BufferedWriter
     *
     * @param bw BufferedWriter
     */
    public static void close(Writer bw) {
        if (bw != null) {
            try {
                bw.close();
            } catch (IOException e) {
                logger.error("close BufferedWriter fail", e);
            }
        }
    }

    /**
     * 如果文件存在删除文件
     *
     * @param path 文件路径
     */
    public static void rm(String path) {
        rm(new File(path));
    }

    /**
     * Remove if is file
     * @param file
     */
    public static void rm(File file) {
        if (file.exists()) {
            boolean boo = file.delete();
            if (!boo) {
                logger.error("删除文件[" + file.getPath() + "]失败.");
            }
        }
    }

    /**
     * Remove file and sub files
     * @param root
     * @param rmSelf Remove self if true
     */
    public static void rmRf(File root, boolean rmSelf) {
        File temp = root;
        if (root.isDirectory()) {
            List<File> files = new ArrayList<>();
            int index = 0;
            do {
                files.addAll(Arrays.asList(root.listFiles()));
                for (; index < files.size(); index++) {
                    if (files.get(index).isDirectory()) {
                        root = files.get(index++);
                        break;
                    }
                }
            } while (index < files.size());

            for ( ; --index >= 0; ) {
                rm(files.get(index));
            }
        }
        if (rmSelf) {
            rm(temp);
        }
    }


    /**
     * 复制单个文件
     *
     * @param srcFile  源文件
     * @param descFile 目标文件
     */
    public static void copyFile(@NotNull File srcFile, @NotNull File descFile) {
        if (srcFile.length() == 0l) {
            try {
                boolean boo = descFile.createNewFile();
                if (!boo)
                    logger.error("Copy file from [" + srcFile.getPath() + "] to [" + descFile.getPath() + "] failed...");
                return;
            } catch (IOException e) {
            }
        }
        FileChannel inChannel = null, outChannel = null;
        try {
            inChannel = new FileInputStream(srcFile).getChannel();
            outChannel = new FileOutputStream(descFile).getChannel();

            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            logger.error("Copy file from [" + srcFile.getPath() + "] to [" + descFile.getPath() + "] failed...");
        } finally {
            if (inChannel != null) {
                try {
                    inChannel.close();
                } catch (IOException e) {
                }
            }
            if (outChannel != null) {
                try {
                    outChannel.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void copyFile(@NotNull InputStream is, @NotNull File descFile) {
        try (BufferedInputStream bis = new BufferedInputStream(is);
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(descFile))) {
            byte[] bytes = new byte[2048];
            int len;
            while ((len = bis.read(bytes)) > 0) {
                bos.write(bytes, 0, len);
            }
        } catch (IOException e) {
            logger.error("Copy file to [" + descFile.getPath() + "] failed...");
        }
    }

    /**
     * 复制整个文件夹的内容
     *
     * @param srcPath       源目录
     * @param descPath      目标目录
     * @param moveSubFolder 是否需要移动子文件夹
     */
    public static void copyFolder(String srcPath, String descPath, boolean moveSubFolder) {
        File src = new File(srcPath), desc = new File(descPath);
        // 源文件夹不存在
        if (!src.exists() || !src.isDirectory()) {
            throw new RuntimeException("源目录[" + srcPath + "]不是存在或者不是一个文件夹.");
        }
        // 目标文件夹不存在
        if (!desc.exists()) {
            boolean boo = desc.mkdirs();
            if (!boo) {
                throw new RuntimeException("目标文件夹[" + descPath + "]无法创建.");
            }
        }

        logger.info("开始扫描文件...");
        String ss[] = src.list();
        if (ss == null) return;
        List<File> files = new ArrayList<>();
        LinkedList<File> folders = new LinkedList<>();
        for (String s : ss) {
            File f = new File(src, s);
            if (f.isFile()) files.add(f);
            else folders.push(f);
        }
        ss = null;
        int src_path_len = srcPath.length();
        // 如果需要复制子文件夹并且源文件夹有子文件夹
        if (moveSubFolder && !folders.isEmpty()) {
            // 1. 扫描所有子文件夹, 这里不采取递归
            while (!folders.isEmpty()) {
                File f = folders.pollLast(), df = new File(desc, f.getPath().substring(src_path_len));
                // 1.1 扫描的同时为目标文件夹创建目录
                if (!df.exists() && !df.mkdir()) {
                    logger.warn("创建子文件夹[" + df.getPath() + "]失败跳过.");
                    continue;
                }
                File[] fs = f.listFiles();
                if (fs == null) continue;
                // 1.2 将文件及目标目录保存
                for (File _f : fs) {
                    if (_f.isFile()) files.add(_f);
                    else folders.push(_f);
                }
            }
        }
        logger.info("扫描完成. 共计[" + files.size() + "]个文件. 开始复制文件...");
        // 2. 复制文件
        files.parallelStream().forEach(f -> copyFile(f, new File(descPath + f.getPath().substring(src_path_len))));
        logger.info("复制结束.");
    }

    public static void writeToDisk(Document doc, String path) {
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            boolean mp = file.getParentFile().mkdirs();
            if (!mp) {
                System.out.println("Create " + file.getParent() + " error.");
                return;
            }
        }
        try (FileOutputStream fos = new FileOutputStream(path)) {
            //write the created document to an arbitrary file

            OutputFormat outformat = OutputFormat.createPrettyPrint();
            XMLWriter writer = new ExtXMLWriter(fos, outformat);
            writer.write(doc);
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            // catch exception
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // catch exception
            e.printStackTrace();
        } catch (IOException e) {
            // catch exception
            e.printStackTrace();
        }
    }
}