package info.vividcode.android.sdk.client;

import java.io.BufferedReader;
import java.io.InputStream;

public abstract class OutputLineDispatchProcessIoHandler implements ProcessIoHandler {

    private final Process mProcess;
    private final BufferedReaderCreator mBufferedReaderCreator;
    private final OnOutputLineReceived mOnStdOutLineReceived;
    private final OnOutputLineReceived mOnErrOutLineReceived;

    private final OutputDispatchThread mStdOutDispatchThread = new StdOutReadingThread();
    private final OutputDispatchThread mErrOutDispatchThread = new ErrOutReadingThread();

    private OutputLineDispatchProcessIoHandler(
            Process p,
            BufferedReaderCreator bufferedReaderCreatorCreator,
            OnOutputLineReceived onStdOutLineReceived,
            OnOutputLineReceived onErrOutLineReceived) {
        mProcess = p;
        mBufferedReaderCreator = bufferedReaderCreatorCreator;
        mOnStdOutLineReceived = onStdOutLineReceived;
        mOnErrOutLineReceived = onErrOutLineReceived;
    }

    @Override
    public void start() {
        mStdOutDispatchThread.start();
        mErrOutDispatchThread.start();
    }

    @Override
    public void close() {
        if (mStdOutDispatchThread.isAlive()) {
            mStdOutDispatchThread.interrupt();
        }
        if (mErrOutDispatchThread.isAlive()) {
            mErrOutDispatchThread.interrupt();
        }
    }

    public void waitFor() throws InterruptedException {
        mStdOutDispatchThread.join();
        mErrOutDispatchThread.join();
    }

    private abstract class OutputDispatchThread extends Thread {
        protected abstract InputStream getInputStream();
        protected abstract void dispatchOutputLine(String line);
        protected abstract void onFinish(Throwable errorOrNull);
        @Override
        public void run() {
            Throwable error = null;
            try (BufferedReader reader = mBufferedReaderCreator.create(getInputStream())) {
                String line;
                while ((line = reader.readLine()) != null) {
                    dispatchOutputLine(line);
                }
            } catch (Throwable e) {
                error = e;
            } finally {
                onFinish(error);
            }
        }
    }

    private class StdOutReadingThread extends OutputDispatchThread {
        @Override
        protected InputStream getInputStream() {
            return mProcess.getInputStream();
        }
        @Override
        protected void dispatchOutputLine(String line) {
            mOnStdOutLineReceived.onOutputLineReceived(line);
        }
        @Override
        protected void onFinish(Throwable errorOrNull) {
        }
    }

    private class ErrOutReadingThread extends OutputDispatchThread {
        @Override
        protected InputStream getInputStream() {
            return mProcess.getErrorStream();
        }
        @Override
        protected void dispatchOutputLine(String line) {
            mOnErrOutLineReceived.onOutputLineReceived(line);
        }
        @Override
        protected void onFinish(Throwable errorOrNull) {
        }
    }

    public interface BufferedReaderCreator {
        BufferedReader create(InputStream is);
    }

    public interface OnOutputLineReceived {
        void onOutputLineReceived(String line);
    }

}
