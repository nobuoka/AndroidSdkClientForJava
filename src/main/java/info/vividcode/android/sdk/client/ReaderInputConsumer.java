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

import java.io.IOException;
import java.io.Reader;

class ReaderInputConsumer {

    private final Reader mReader;
    private final Callback mCallback;

    interface Callback {
        void onOutputReceived(String str);
        void onLineEnded();
    }

    ReaderInputConsumer(Reader reader, Callback callback) {
        mReader = reader;
        mCallback = callback;
    }

    private void callOnOutputReceived(String outputStr) {
        mCallback.onOutputReceived(outputStr);
    }

    private void callOnLineEnded() {
        mCallback.onLineEnded();
    }

    private void outputStringBuffer(StringBuilder sb) {
        if (sb.length() == 0) return;
        String out = sb.toString();
        sb.setLength(0);
        callOnOutputReceived(out);
        if (out.length() != 0 && out.charAt(out.length() - 1) == '\r') {
            callOnLineEnded();
        }
    }

    void consumeSynchronously() throws IOException {
        char[] chars = new char[2048];
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (!mReader.ready()) {
                outputStringBuffer(sb);
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }

            int length = mReader.read(chars);
            if (length >= 0) {
                sb.append(chars, 0, length);
                outputEndedLines(sb);
            }
        }
        outputStringBuffer(sb);
    }

    private void outputEndedLines(StringBuilder sb) {
        while (true) {
            int idxLf = sb.indexOf("\n");
            int idxCr = sb.indexOf("\r");
            if (idxLf < 0 && idxCr < 0) break;
            if (idxCr + 1 == sb.length() && idxLf < 0) break;

            int sepIdx, nextIdx;
            if (idxLf >= 0 && idxCr >= 0) {
                if (idxLf + 1 == idxCr) {
                    sepIdx = idxCr;
                    nextIdx = idxCr + 1;
                } else {
                    sepIdx = nextIdx = Math.min(idxLf, idxCr) + 1;
                }
            } else {
                sepIdx = nextIdx = Math.max(idxLf, idxCr) + 1;
            }
            String line = sb.substring(0, sepIdx - 1) + "\n";
            sb.delete(0, nextIdx);
            callOnOutputReceived(line);
            callOnLineEnded();
        }
    }

}
