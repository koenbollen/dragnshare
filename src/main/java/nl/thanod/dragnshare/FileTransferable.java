/**
 * 
 */
package nl.thanod.dragnshare;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FileTransferable implements Transferable 
{
    final private List<File> files;
    final private DataFlavor[] flavors;

    /**
     * A drag-and-drop object for transfering a file.
     * @param file file to transfer -- this file should already exist,
     * otherwise it may not be accepted by drag targets.
     */
    public FileTransferable(Collection<File> files) {
        this.files = Collections.unmodifiableList(
                new ArrayList<File>(files));
        this.flavors = new DataFlavor[] 
                { DataFlavor.javaFileListFlavor };
    }

    static FileTransferable createFileInTempDirectory(String filename) 
        throws IOException
    {
        File dir = new File(System.getProperty("java.io.tmpdir"));
        File f = new File(dir, filename);
        f.createNewFile();
        return new FileTransferable(Collections.singletonList(f));
    }

    public List<File> getFiles() { return this.files; }

    @Override public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException 
    {
        if (isDataFlavorSupported(flavor))
            return this.files;
        else
            return null;
    }

    @Override public DataFlavor[] getTransferDataFlavors() {
        return this.flavors;
    }

    @Override public boolean isDataFlavorSupported(DataFlavor flavor) {
        return DataFlavor.javaFileListFlavor.equals(flavor);
    }
}