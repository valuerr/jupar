/**
 * Written by Periklis Master_ex Ntanasis <pntanasis@gmail.com>
 * http://masterex.github.com/
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package jupar;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static jupar.utils.FileUtils.smartCopyOrWget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import jupar.objects.DownloadURL;
import jupar.objects.Modes;
import jupar.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import jupar.parsers.DownloaderXMLParser;

/**
 * @author Periklis Ntanasis
 */
public class Downloader {

    private static final Logger logger = LoggerFactory.getLogger(Downloader.class);
    Map<String, Path> md5files = new TreeMap<String, Path>();
    private AtomicInteger progress;

    public Downloader() {
        initPartialCopy("./");
    }

    public Downloader(String home_dir) {
        initPartialCopy(home_dir);
    }

    public void initPartialCopy(String copyPath) {
        try {
            Path start = FileSystems.getDefault().getPath(copyPath);
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        String md5 = FileUtils.md5File(file.toString());
                        md5files.put(md5, file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void download(String filesxml, String destinationdir, Modes mode) throws SAXException,
            FileNotFoundException, IOException, InterruptedException {

        DownloaderXMLParser parser = new DownloaderXMLParser();
        ArrayList<DownloadURL> paresed = parser.parse(filesxml, mode);
        Iterator iterator = paresed.iterator();
        java.net.URL url;
        String md5;

        File dir = new File(destinationdir);
        if (!dir.exists()) {
            dir.mkdir();
        }

        int total = paresed.size();
        int current = 0;
        setProgress(0, total);
        while (iterator.hasNext()) {
            DownloadURL downloadURL = (DownloadURL) iterator.next();
            md5 = downloadURL.getHash();
            url = new java.net.URL(downloadURL.getFile());

            File netfile = new File(url.getFile());
            Path src_file = null;
            Path dst_file = FileSystems.getDefault().getPath(dir.getAbsolutePath() + File.separator + netfile.getName());

            if (!md5.equals("") && md5files.containsKey(md5))
                src_file = md5files.get(md5);

            smartCopyOrWget(src_file, dst_file, url);
            setProgress(++current, total);
        }
    }

    public void setProgressVar(AtomicInteger progress) {
        this.progress = progress;
    }

    private void setProgress(int progress, int total) {
        this.progress.set(10 + (int) (((double) progress / (double) total) * 60));
    }
}
