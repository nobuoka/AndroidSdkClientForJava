/*
 * Copyright 2014 NOBUOKA Yu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.vividcode.android.sdk.client;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class ConsoleProxyProcessIoHandler implements ProcessIoHandler {

    public static class Factory implements ProcessIoHandler.Factory {
        @Override
        public ProcessIoHandler createHandler(Process process) {
            InputStream procStdOutput = process.getInputStream();
            InputStream procErrOutput = process.getErrorStream();
            OutputStream procInput = process.getOutputStream();
            return new ConsoleProxyProcessIoHandler(procStdOutput, procErrOutput, procInput);
        }
    }

    private class RedirectionTask implements Runnable {
        final InputStream mIn;
        final PrintStream mOut;
        RedirectionTask(InputStream i, PrintStream o) {
            mIn = i;
            mOut = o;
        }
        @Override
        public void run() {
            byte[] buf = new byte[2048];
            while (true) {
                int length;
                try {
                    if (mIn.available() == 0) {
                        mOut.flush();
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        } else {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            continue;
                        }
                    }
                    length = mIn.read(buf);
                    mOut.write(buf, 0, length);
                    if (length < 0) break;
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }
    }

    Thread[] mThreads;
    Thread mInputRedirectionThread;
    final InputStream mProcStdOutput;
    final InputStream mProcErrOutput;
    final OutputStream mProcInput;

    ConsoleProxyProcessIoHandler(InputStream procStdOutput, InputStream procErrOutput, OutputStream input) {
        mProcStdOutput = procStdOutput;
        mProcErrOutput = procErrOutput;
        mProcInput = input;
    }

    @Override
    public void start() {
        final Console console = System.console();
        if (console == null) {
            throw new RuntimeException("no console");
        }
        mInputRedirectionThread = new Thread() {
            @Override
            public void run() {
                try (BufferedReader reader = new BufferedReader(console.reader())) {
                    while (!Thread.currentThread().isInterrupted()) {
                        if (!reader.ready()) {
                            try {
                                Thread.sleep(100);
                            } catch(InterruptedException err) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                            continue;
                        }
                        String input = reader.readLine();
                        mProcInput.write(input.getBytes("UTF-8"));
                        mProcInput.flush();
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        mThreads = new Thread[] {
            mInputRedirectionThread,
            new Thread(new RedirectionTask(mProcStdOutput, System.out)),
            new Thread(new RedirectionTask(mProcErrOutput, System.err)),
        };
        for (Thread t : mThreads) t.start();
    }

    void requestToStopAndWait() {
        for (Thread t: mThreads) t.interrupt();
        for (Thread t: mThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void close() throws IOException {
        requestToStopAndWait();
        mProcStdOutput.close();
        mProcErrOutput.close();
        mProcInput.close();
    }

}
