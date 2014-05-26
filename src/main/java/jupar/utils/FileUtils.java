package jupar.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static String md5File(String filename) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        FileInputStream fis = new FileInputStream(filename);

        byte[] dataBytes = new byte[1024];

        int nread = 0;
        while ((nread = fis.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        }
        byte[] mdbytes = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte mdbyte : mdbytes) {
            sb.append(Integer.toString((mdbyte & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public static void copy(String source, String destination) throws IOException {
        File src_file = new File(source);
        File dst_file = new File(destination);
        InputStream in = new FileInputStream(src_file);
        OutputStream out = new FileOutputStream(dst_file);

        try {
            byte[] buffer = new byte[512];
            int length;

            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        } finally {
            in.close();
            out.close();
        }

        if (destination.toLowerCase().endsWith(".exe"))
            try {
                dst_file.setExecutable(true);
            } catch (SecurityException e) {
                logger.warn("Set executable flag failed: {}", destination);
            }
    }


    public static boolean smartCopy(Path src_file, Path dst_file) {
        return smartCopyOrWget(src_file, dst_file, null);
    }

    public static boolean smartCopyOrWget(Path src_file, Path dst_file, URL url) {
        try {
            if (dst_file.equals(src_file)
                    || dst_file.toString().equals(src_file.toString())
                    || (dst_file.compareTo(src_file) == 0))
                return true;
        } catch (NullPointerException e) {

        }


        try {
            Files.createDirectories(dst_file.getParent());
        } catch (IOException e) {
            logger.error("create parrent directories failed; path: {}", dst_file);
        }

        if (src_file != null) {
            try {
                Files.copy(src_file, dst_file, REPLACE_EXISTING);
                logger.info("Copy1 OK: {} --> {}", src_file, dst_file);
                return true;
            } catch (Exception e) {
                logger.warn("Copy1 error: {} --> {} ({})", src_file, dst_file, e.getMessage());
            }

            try {
                FileUtils.copy(src_file.toString(), dst_file.toString());
                logger.info("Copy2 OK: {} --> {}", src_file, dst_file);
                return true;
            } catch (Exception e) {
                logger.warn("Copy2 error: {} --> {} ({})", src_file, dst_file, e.getMessage());
            }
        }

        if (url != null)
            try {
                FileUtils.wget(url, dst_file.toString());
                logger.info("wget OK: {} --> {}", url, dst_file);
                return true;
            } catch (Exception e) {
                logger.error("wget error: {} --> {} ({})", url, dst_file, e.getMessage());
            }
        return false;
    }

    /**
     * By default File#delete fails for non-empty directories, it works like "rm".
     * We need something a little more brutual - this does the equivalent of "rm -r"
     *
     * @param path Root File Path
     * @return true iff the file and all sub files/directories have been removed
     * @throws FileNotFoundException
     */
    public static boolean deleteRecursive(File path) throws FileNotFoundException {
        if (!path.exists()) throw new FileNotFoundException(path.getAbsolutePath());
        boolean ret = true;
        if (path.isDirectory()) {
            for (File f : path.listFiles()) {
                ret = ret && FileUtils.deleteRecursive(f);
            }
        }
        return ret && path.delete();
    }


    public static void delete(String filename) {
        File file = new File(filename);
        file.delete();
    }

    public static void wget(java.net.URL url, String destination) throws MalformedURLException, IOException {
        java.net.URLConnection conn = url.openConnection();
        java.io.InputStream in = conn.getInputStream();

        File dst_file = new File(destination);
        OutputStream out = new FileOutputStream(dst_file);

        try {
            byte[] buffer = new byte[512];
            int length;

            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        } finally {
            in.close();
            out.close();
        }

        if (destination.toLowerCase().endsWith(".exe"))
            try {
                dst_file.setExecutable(true);
            } catch (SecurityException e) {
                logger.warn("Set executable flag failed: {}", destination);
            }
    }
}
