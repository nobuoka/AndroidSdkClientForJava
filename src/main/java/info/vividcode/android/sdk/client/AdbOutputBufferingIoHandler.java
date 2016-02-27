package info.vividcode.android.sdk.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AdbOutputBufferingIoHandler implements ProcessIoHandler {

    private final Process mProcess;
    private final OutputReadingThread mThread = new OutputReadingThread();

    private final AtomicReference<String> mResult = new AtomicReference<>();
    private final AtomicReference<Throwable> mResultError = new AtomicReference<>();
    private AtomicBoolean mResultSuccess = new AtomicBoolean();
    private final CountDownLatch mResultWaiter = new CountDownLatch(1);

    private AdbOutputBufferingIoHandler(Process p) {
        mProcess = p;
    }

    public String getResultSync() throws InterruptedException, Throwable {
        mResultWaiter.await();
        if (mResultSuccess.get()) {
            return mResult.get();
        } else {
            throw mResultError.get();
        }
    }

    @Override
    public void start() {
        mThread.start();
    }

    @Override
    public void close() {
        if (mThread.isAlive()) {
            mThread.interrupt();
        }
    }

    private class OutputReadingThread extends Thread {
        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new AdbInputStream(new BufferedInputStream(mProcess.getInputStream()))))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (sb.length() != 0) sb.append("\n");
                    sb.append(line);
                }
                mResultSuccess.set(true);
                mResult.set(sb.toString());
            } catch (Throwable e) {
                mResultSuccess.set(false);
                mResultError.set(e);
            } finally {
                mResultWaiter.countDown();
            }
        }
    }

    public static final Factory FACTORY = new Factory();
    private static class Factory implements ProcessIoHandler.Factory<AdbOutputBufferingIoHandler> {
        @Override
        public AdbOutputBufferingIoHandler createHandler(Process process) {
            return new AdbOutputBufferingIoHandler(process);
        }
    }

}
