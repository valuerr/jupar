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
package jupar.objects;

/**
 *
 * @author Valentin Gorbunov valuerr
 */
public class DownloadURL {

    private String hash;
    private String file;
    private String path;

    public DownloadURL() {
    }

    public DownloadURL(String file, String hash, String path) {
        this.file = file;
        this.hash = hash;
        this.path = path;
    }

    public String getHash() {
        return hash;
    }

    public String getFile() {
        return file;
    }
    public String getPath() {
        return path;
    }

    public void setFile(String file) {
        this.file = file;
    }
    public void setPath(String path) {
        this.path = path;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
