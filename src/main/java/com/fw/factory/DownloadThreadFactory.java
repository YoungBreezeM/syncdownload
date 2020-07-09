package com.fw.factory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * @author yqf
 */
public class DownloadThreadFactory implements ThreadFactory {

    private int counter;
    private String name;

    public DownloadThreadFactory(String name) {
        counter = 1;
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable runnable) {

        Thread t = new Thread(runnable, name + "-Thread_" + counter);
        counter++;
        return t;
    }


}
