package com.fw.domain;

import com.fw.factory.DownloadThreadFactory;
import com.fw.http.HttpHeader;
import com.fw.http.HttpMethod;
import com.fw.http.HttpStatus;
import com.fw.utils.HttpHeaderIterator;
import com.fw.utils.NetSpeed;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author yqf
 * 下载任务
 */
public class DownloadTask {
    /**下载文件配置**/
    private FileDownload fileDownload;

    private static LinkedBlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<Runnable>(1024);

    private static DownloadThreadFactory downloadThreadFactory = new DownloadThreadFactory("sync-download");

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(
            ThreadPoolConfig.CORE_POOL_SIZE,
            ThreadPoolConfig.MAXIMUM_POOL_SIZE,
            ThreadPoolConfig.KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            blockingQueue,
            downloadThreadFactory
    );

    public DownloadTask(FileDownload fileDownload){

        this.fileDownload = fileDownload;
    }

    public void start()  {
        try {
            URL url = new URL(fileDownload.getServerPath());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestMethod(HttpMethod.GET);

            if (conn.getResponseCode()== HttpStatus.OK.value()){
                HttpHeaderIterator httpHeaderIterator = new HttpHeaderIterator();

                Map<String, String> header = httpHeaderIterator.getHeader(conn);

                String contentLength = header.get(HttpHeader.CONTENT_LENGTH);

                fileDownload.setFileLength(Long.parseLong(contentLength));;

                blockDownload(this.fileDownload.getFileLength());
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**分段下载**/
    private void blockDownload(long fileLength) throws ExecutionException, InterruptedException {

        System.out.println("分段文件下载大小为"+fileLength+"字节");
        long startTime = System.currentTimeMillis();
        long blockSize = fileLength / ThreadPoolConfig.WORK_THREAD;

        for (int i = 0; i <=ThreadPoolConfig.WORK_THREAD; i++) {
            long startIndex = i * blockSize;
            long endIndex = startIndex + blockSize - 1;
            if (i == ThreadPoolConfig.WORK_THREAD) {
                endIndex = fileLength;
            };

            DownloadThread downloadThread = new DownloadThread(i,fileDownload, startIndex, endIndex);
            executor.execute(downloadThread);
        }
        progress();
        shutdownAndAwaitTermination();
        monitor();
        long endTime = System.currentTimeMillis();
        System.out.println("本次下载公花费"+(endTime-startTime)+"s"+"平均下载速度"+fileDownload.getFileLength()/(endTime-startTime));

    }

    private void progress(){
        long oldLength = 0;
        while (true){
            long l = 0;
            try {
                Thread.sleep(1000);
                long[] workFileSize = fileDownload.getWorkFileSize();
                for (long value : workFileSize) {
                    l = l + value;
                }
                System.out.println(new NetSpeed(l-oldLength).compute());
                oldLength = l;
                if (l==fileDownload.getFileLength()){
                    break;
                }
                DecimalFormat decimalFormat=new DecimalFormat("0.00");
                String format = decimalFormat.format(((double) l/(double)fileDownload.getFileLength())*100);
                System.out.println("已下载："+format+"%");

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private static void monitor() {
        ThreadPoolExecutor threadPoolExecutor = executor;
        System.out.println("=====================================================================================");
        System.out.println("=【线程池任务】线程池中曾经创建过的最大线程数：" + threadPoolExecutor.getLargestPoolSize());
        System.out.println("=【线程池任务】线程池中线程数：" + threadPoolExecutor.getPoolSize());
        System.out.println("=【线程池任务】线程池中活动的线程数：" + threadPoolExecutor.getActiveCount());
        System.out.println("=【线程池任务】队列中等待执行的任务数：" + threadPoolExecutor.getQueue().size());
        System.out.println("=【线程池任务】线程池已执行完任务数：" + threadPoolExecutor.getCompletedTaskCount());
        System.out.println("=====================================================================================");

    }

    /**
     * 关闭线程池
     * 1. shutdown、shutdownNow 的原理都是遍历线程池中的工作线程，然后逐个调用线程的 interrupt 方法来中断线程。
     * 2. shutdownNow：将线程池的状态设置成 STOP，然后尝试停止所有的正在执行或暂停任务的线程，并返回等待执行任务的列表。
     * 3. shutdown：将线程池的状态设置成 SHUTDOWN 状态，然后中断所有没有正在执行任务的线程。
     */
    private static void shutdownAndAwaitTermination() {
        // 禁止提交新任务
        executor.shutdown();
        try {
            // 等待现有任务终止
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                // 取消当前正在执行的任务
                executor.shutdownNow();
                // 等待一段时间让任务响应被取消
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // 如果当前线程也中断，则取消
            executor.shutdownNow();
            // 保留中断状态
            Thread.currentThread().interrupt();
        }
    }
}

