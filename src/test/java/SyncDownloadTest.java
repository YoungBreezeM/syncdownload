import com.fw.domain.DownloadTask;
import com.fw.domain.DownloadThread;
import com.fw.domain.FileDownload;
import com.fw.domain.ThreadPoolConfig;
import com.fw.factory.DownloadThreadFactory;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.*;

public class SyncDownloadTest {




    @Test
    public void testMain(){






    }



    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {

        FileDownload fileDownload = new FileDownload(
                "https://qd.myapp.com/myapp/qqteam/linuxQQ/linuxqq_2.0.0-b1-1024_amd64.deb",
                "/home/yqf/下载",
                ThreadPoolConfig.WORK_THREAD
        );

        new DownloadTask(fileDownload).start();

    }
}
