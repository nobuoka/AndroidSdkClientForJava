package info.vividcode.android.sdk.client;

import java.io.IOException;

class Command<T extends ProcessIoHandler> implements AutoCloseable {

    private final Process mProcess;
    private final T mHandler;

    public Command(Process p, T h) {
        mProcess = p;
        mHandler = h;
    }

    public T getProcessIoHandler() {
        return mHandler;
    }

    public void waitFor() throws InterruptedException {
        mProcess.waitFor();
        int exitValue = mProcess.exitValue();
        if (exitValue != 0) {
            throw new RuntimeException("Process exits with non-zero exit value. exit value: " + exitValue);
        }
    }

    public void close() {
        /* Process#isAlive() は Java SE 8 から。
        if (mProcess.isAlive()) {
            mProcess.destroyForcibly();
        }
        */
        if (mHandler != null) {
            try {
                mHandler.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
