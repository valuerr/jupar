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
import java.nio.file.FileSystems;
import java.nio.file.Files;

import static jupar.utils.FileUtils.smartCopy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jupar.objects.Instruction;
import jupar.objects.Modes;
import org.xml.sax.SAXException;
import jupar.parsers.UpdateXMLParser;

/**
 * @author Periklis Ntanasis
 */
public class Updater {

    private static final Logger logger = LoggerFactory.getLogger(Updater.class);
    private int skip_first_instructions = 0;
    private JuparMain juparMainUpdater;
    private AtomicInteger progress;

    public void setSkip_first_instructions(int skip_first_instructions) {
        this.skip_first_instructions = skip_first_instructions;
    }

    public void setJuparMainUpdater(JuparMain juparMainUpdater) {
        this.juparMainUpdater = juparMainUpdater;
    }

    public void update(String instructionsxml, String update_dir, Modes mode) throws SAXException,
            IOException, InterruptedException {
        update(instructionsxml, update_dir, "", mode);
    }

    public void update(String instructionsxml, String update_dir, String home_dir, Modes mode) throws SAXException,
            IOException, InterruptedException {

        if (!update_dir.isEmpty() && !update_dir.endsWith(File.separator))
            update_dir += File.separator;
        if (!home_dir.isEmpty() && !home_dir.endsWith(File.separator))
            home_dir += File.separator;

        UpdateXMLParser parser = new UpdateXMLParser();
        ArrayList<Instruction> parsed = parser.parse(update_dir + instructionsxml, mode);
        Iterator iterator = parsed.iterator();
        Instruction instruction;

        int instruction_now = 0;
        int total = parsed.size();
        setProgress(0, total);
        while (iterator.hasNext()) {
            instruction = (Instruction) iterator.next();

            ++instruction_now;
            if (instruction_now <= skip_first_instructions) {
                setProgress(instruction_now, total);
                continue;
            }
            skip_first_instructions = instruction_now;

            switch (instruction.getAction()) {
                case MOVE:
                    Path src_file = FileSystems.getDefault().getPath(update_dir + instruction.getFilename());
                    Path dst_file = FileSystems.getDefault().getPath(home_dir + instruction.getDestination());
                    if (smartCopy(src_file, dst_file))
                        logger.info("Copy OK: {} --> {}", src_file, dst_file);
                    break;

                case DELETE:
                    Files.deleteIfExists(FileSystems.getDefault().getPath(home_dir + instruction.getDestination()));
                    logger.info("Delete OK: {}", FileSystems.getDefault().getPath(home_dir + instruction.getDestination()));
                    break;

                case EXECUTE:
                    String[] exec_arr = instruction.getFilename().split(" ");
                    exec_arr[0] = home_dir + exec_arr[0];
                    Runtime.getRuntime().exec(exec_arr);
                    logger.info("Execute OK: {}", home_dir + instruction.getFilename());
                    break;

                case EXECUTE_EXT_UPDIR_UPDATER:
                    juparMainUpdater.runExtUpdater(skip_first_instructions, instruction.getFilename());
                    logger.info("Execute ext updater OK: {}", instruction.getFilename());
                    return;
            }
            setProgress(instruction_now, total);
        }
    }

    public void setProgressVar(AtomicInteger progress) {
        this.progress = progress;
    }

    private void setProgress(int progress, int total) {
        this.progress.set(70 + (int) (((double) progress / (double) total) * 30));
    }
}
