package jupar;


import jupar.objects.Modes;
import jupar.objects.Release;
import jupar.parsers.ReleaseXMLParser;
import jupar.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.*;

public class JuparMain {

    private static final Logger logger = LoggerFactory.getLogger(JuparMain.class);

    private String link, update_dir, home_dir, update_app_name, stage;
    private Release current_release, new_release;
    private int wait_start, skip_first_instructions;

    private static AtomicInteger progress = new AtomicInteger(0);

    public static double getProgress() {
        return (double) progress.get() / (double) 100.0;
    }

    public Release getNewReleaseInfo() {
        if (new_release == null)
            checkNew();
        return new_release;
    }

    public Release getCurrentReleaseInfo() { return current_release; }

    private static class JuparSingletonHolder {
        private final static JuparMain instance = new JuparMain();
    }

    public static JuparMain getInstance() {
        return JuparSingletonHolder.instance;
    }

    public void setUpdateURL(String update_url) {
        link = update_url;
    }

    public int checkNew() {
        /**
         * Check for new version
         */
        int answer = -1;

        ReleaseXMLParser parser = new ReleaseXMLParser();
        try {
            new_release = parser.parse(link + "latest.xml", Modes.URL);
            if (new_release.compareTo(current_release) > 0) {
                logger.info("A new version of this program is available");
                answer = 0;
            }
        } catch (SAXException ex) {
            logger.error("The xml wasn't loaded succesfully!", ex);
            answer = -1;
        } catch (FileNotFoundException ex) {
            logger.error("Files were unable to be read or created successfully!\n" +
                    "Please be sure that you have the right permissions and internet connectivity!" +
                    "Something went wrong!", ex);
            answer = -1;
        } catch (IOException ex) {
            logger.error("Something went wrong!", ex);
            answer = -1;
        } catch (InterruptedException ex) {
            logger.error("The connection has been lost!\n" +
                    "Please check your internet connectivity!" +
                    "Something went wrong!", ex);
            answer = -1;
        }
        progress.set(10);
        return answer;
    }

    public boolean download() {
        logger.info("Downloading...");

        Downloader dl = new Downloader(update_dir, "./", home_dir);
        dl.setProgressVar(progress);
        try {
            dl.download(link + "files.xml", update_dir, Modes.URL);
            return true;
        } catch (SAXException ex) {
            logger.error("The xml wasn't loaded succesfully!", ex);
            return false;
        } catch (FileNotFoundException ex) {
            logger.error("Files were unable to be read or created successfully!\n" +
                    "Please be sure that you have the right permissions and internet connectivity!" +
                    "Something went wrong!", ex);
            return false;
        } catch (IOException ex) {
            logger.error("Something went wrong!", ex);
            return false;
        } catch (InterruptedException ex) {
            logger.error("The connection has been lost!\n" +
                    "Please check your internet connectivity!" +
                    "Something went wrong!", ex);
            return false;
        }
    }

    public boolean update() {
        logger.info("Updating...");
        /**
         * Start the updating procedure
         */
        try {
            Updater update = new Updater();
            update.setProgressVar(progress);
            update.setSkip_first_instructions(skip_first_instructions);
            update.setJuparMainUpdater(this);
            update.update("update.xml", update_dir, home_dir, Modes.FILE);
            logger.info("The update was completed successfuly.\n" +
                    "Please restart the application in order the changes take effect.");
            return true;
        } catch (SAXException ex) {
            logger.error("Wrong update.xml format", ex);
        } catch (InterruptedException ex) {
            logger.error("Something went wrong! (InterruptedException)", ex);
        } catch (IOException ex) {
            logger.error("Something went wrong! (IOException)", ex);
        }
        return false;
    }

    public void clean() {
        /**
         * Delete tmp directory
         */
        File tmp = new File(update_dir);
        if (tmp.exists()) {
            try {
                FileUtils.deleteRecursive(tmp);
            } catch (FileNotFoundException e) {
                logger.error("cleanup failed!", e);
            }
        }
    }

    protected void configureFromArgs(String[] args) throws ParseException {
        logger.info("Updater started with args: {}", Arrays.asList(args).toString());
        Options options = new Options();
        options.addOption("s", "stage", true, "update stage");
        options.addOption("v", "pkgver", true, "pkgver");
        options.addOption("r", "pkgrel", true, "pkgrel");
        options.addOption("u", "update_dir", true, "tmp dir");
        options.addOption("h", "home_dir", true, "home dir");
        options.addOption("l", "link", true, "update link");
        options.addOption("w", "wait", true, "wait millis");
        options.addOption("i", "skip_first_instructions", true, "skip first instructions");

        CommandLineParser parser = new PosixParser();
        org.apache.commons.cli.CommandLine cmd = parser.parse(options, args, true);

        String pkgver = cmd.getOptionValue("pkgver");
        String pkgrel = cmd.getOptionValue("pkgrel");

        if (pkgver != null && pkgrel != null) {
            current_release = new Release();
            current_release.setpkgver(pkgver);
            current_release.setPkgrel(pkgrel);
        }

        if (!cmd.getOptionValue("link").isEmpty())
            link = cmd.getOptionValue("link");

        update_dir = cmd.getOptionValue("update_dir");
        home_dir = cmd.getOptionValue("home_dir");
        stage = cmd.getOptionValue("stage");

        try {
            wait_start = Integer.parseInt(cmd.getOptionValue("wait"));
        } catch (Exception e) {
            wait_start = 3000;
        }

        try {
            skip_first_instructions = Integer.parseInt(cmd.getOptionValue("skip_first_instructions"));
        } catch (Exception e) {
            e.printStackTrace();
            skip_first_instructions = 0;
        }

        logger.info("executed_dir={}", FileSystems.getDefault().getPath(".").toAbsolutePath());
        logger.info("link={}", link);
        logger.info("update_dir={}", update_dir);
        logger.info("home_dir={}", home_dir);
        logger.info("wait_start={}", wait_start);
    }

    public void configureFromManifest() {
        current_release = new Release();
        try {
            Manifest mf = new Manifest();
            mf.read(Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/MANIFEST.MF"));
            Attributes atts = mf.getMainAttributes();
            current_release.setpkgver(atts.getValue("pkgver"));
            current_release.setPkgrel(atts.getValue("pkgrel"));
            link = atts.getValue("update_url");
            update_app_name = atts.getValue("update_app_name");
        } catch (IOException e) {
            logger.error("Failed to load version info", e);
        }
    }

    private void waitIfNeed() {
        if (wait_start > 0)
            try {
                Thread.currentThread().sleep(wait_start);
            } catch (InterruptedException e) {
                logger.error("sleep error", e);
            }
    }

    public JuparMain() {
        home_dir = safeRemDots(FileSystems.getDefault().getPath(".").toAbsolutePath().toString());
        if (".".equals(home_dir) || "".equals(home_dir))
            update_dir = "tmp";
        else update_dir = home_dir + File.separator + "tmp";

        skip_first_instructions = 0;
        current_release = null;
        new_release = null;
        configureFromManifest();
    }

    private String safeRemDots(String s) {
        s = s.replace("\\.\\", "\\").replace("/./", "/");
        if (s.endsWith("/.") || s.endsWith("\\."))
            s = s.substring(0, s.length() - 2);
        if (s.startsWith("./") || s.startsWith(".\\"))
            s = s.substring(2);
        return s;
    }

    public void runExtUpdater(int skip_first_instructions_num, String update_app_name) {
        if (update_app_name == null)
            update_app_name = this.update_app_name;
        if ("updateme".equals(stage))
            return;
        String home_dir_p = safeRemDots(FileSystems.getDefault().getPath(home_dir).toAbsolutePath().toString());
        String update_dir_p = safeRemDots(FileSystems.getDefault().getPath(update_dir).toAbsolutePath().toString());
        String[] exec_arr = {update_dir_p + File.separator + update_app_name,
                "--stage=updateme",
                "--link=" + link,
                "--update_dir=" + update_dir_p,
                "--home_dir=" + home_dir_p,
                "--skip_first_instructions=" + Integer.toString(skip_first_instructions_num),
        };
        String exec_str = Arrays.asList(exec_arr).toString().replaceAll("^\\[|\\]$", "").replaceAll(",", "");
        try {
            logger.info("Executing: " + exec_str);
            Runtime.getRuntime().exec(exec_arr);
        } catch (IOException e) {
            logger.error("Execute error: {}", exec_str, e);
        }
    }

    public static void main(String[] args) throws ParseException {
        JuparMain updater = getInstance();
        updater.configureFromArgs(args);
        updater.waitIfNeed();
        updater.download();
        updater.update();
        progress.set(100);
    }

    public static void fullUpdate() {
        JuparMain updater = getInstance();
        updater.download();
        updater.update();
        progress.set(100);
    }

    public static boolean checkNewStatic() {
        JuparMain updater = getInstance();
        return updater.checkNew() == 0;
    }

    public static void cleanup() {
        JuparMain updater = getInstance();
        updater.clean();
        progress.set(100);
    }
}
