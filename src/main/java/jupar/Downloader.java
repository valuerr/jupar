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
import java.net.MalformedURLException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.StandardCopyOption.*;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import jupar.objects.DownloadURL;
import jupar.objects.Modes;
import jupar.utils.Hasher;
import org.xml.sax.SAXException;
import jupar.parsers.DownloaderXMLParser;

/**
 * @author Periklis Ntanasis
 */
public class Downloader {

    Map<String, Path> md5files = new TreeMap<String, Path>();

    public Downloader() {
        String copyPath = "./";
        try {
            Path start = FileSystems.getDefault().getPath(copyPath);
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        String md5 = Hasher.md5File(file.toString());
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
        Iterator iterator = parser.parse(filesxml, mode).iterator();
        java.net.URL url;
        String md5;

        File dir = new File(destinationdir);
        if (!dir.exists()) {
            dir.mkdir();
        }

        while (iterator.hasNext()) {
            DownloadURL downloadURL = (DownloadURL) iterator.next();
            md5 = downloadURL.getHash();
            url = new java.net.URL(downloadURL.getFile());
            File netfile = new File(url.getFile());
            String destpath = destinationdir + File.separator + netfile.getName();

            if (!md5.equals("") && md5files.containsKey(md5)) {
                try {
                    Files.copy(md5files.get(md5), FileSystems.getDefault().getPath(destpath), REPLACE_EXISTING);
                } catch (Exception e) {
                    e.printStackTrace();
                    wget(url, destpath);
                }
            } else {
                wget(url, destpath);
            }
        }
    }

    private void wget(java.net.URL url, String destination) throws MalformedURLException, IOException {
        java.net.URLConnection conn = url.openConnection();
        java.io.InputStream in = conn.getInputStream();

        File dstfile = new File(destination);
        OutputStream out = new FileOutputStream(dstfile);

        byte[] buffer = new byte[512];
        int length;

        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }

        in.close();
        out.close();
    }
}
