package chat.client.file;

import ChatApp.FileCallback;
import ChatApp.FileInfo;
import com.zeroc.Ice.Current;

import java.util.function.Consumer;

public class FileCallbackI implements FileCallback {

    private final Consumer<FileInfo> onFileAvailable;

    public FileCallbackI(Consumer<FileInfo> onFileAvailable) {
        this.onFileAvailable = onFileAvailable;
    }

    @Override
    public void onFileAvailable(FileInfo info, Current current) {
        onFileAvailable.accept(info);
    }
}
