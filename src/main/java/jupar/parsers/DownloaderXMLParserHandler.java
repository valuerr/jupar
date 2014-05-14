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
package jupar.parsers;

import java.util.ArrayList;

import jupar.objects.DownloadURL;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Periklis Ntanasis
 */
public class DownloaderXMLParserHandler extends DefaultHandler {

    private String currentelement = "";
    private String current_md5 = "";
    private String current_path = "";

    private ArrayList<DownloadURL> downloadURLs = new ArrayList<DownloadURL>();

    public DownloaderXMLParserHandler() {
        super();
    }

    @Override
    public void startElement(String uri, String name,
            String qName, Attributes atts) {
        currentelement = qName;
        if (currentelement.equals("file"))
            if(atts != null && atts.getLength() > 0) {
                current_md5 = atts.getValue("md5");
                current_path = atts.getValue("path");
                if (current_md5 == null) current_md5 = "";
                if (current_path == null) current_path = "";
            }
    }

    @Override
    public void characters(char ch[], int start, int length) {
        String value = null;
        if (!currentelement.equals("")) {
            value = String.copyValueOf(ch, start, length).trim();
        }

        if (currentelement.equals("file")) {
            downloadURLs.add(new DownloadURL(value, current_md5, current_path));
        }
        currentelement = "";

    }

    public ArrayList<DownloadURL> getDownloadURLs() {
        return downloadURLs;
    }
}
