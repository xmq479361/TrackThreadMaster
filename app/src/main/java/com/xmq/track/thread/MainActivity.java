package com.xmq.track.thread;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void runWithNewThread(View view) {
        Log.i(TAG, "runWithNewThread: ");
        new Thread() {
            @Override
            public void run() {
                super.run();
                Log.i(TAG, "runWithNewThread: " + Thread.currentThread().getName());
            }
        }.start();
    }

    public void runWithNewThreadRunable(View view) {
        Log.i(TAG, "runWithNewThreadRunable: ");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "runWithNewThreadRunable: " + Thread.currentThread().getName());
            }
        }).start();
    }

    public void runWithExcutorsThreadPools(View view) {
        Log.i(TAG, "runWithExcutorsThreadPools: ");
        Executors.newCachedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "runWithExcutorsThreadPools: " + Thread.currentThread().getName());
            }
        });
    }

    public void runWithNewThreadPools(View view) {
        Log.i(TAG, "runWithNewThreadPools: ");
        new ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, new LinkedBlockingQueue()).execute(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "runWithNewThreadPools: " + Thread.currentThread().getName());
            }
        });
    }

    public void runWithAsyncTask(View view) {
        Log.i(TAG, "runWithAsyncTask: ");
    }
}