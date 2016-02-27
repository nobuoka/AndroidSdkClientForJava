/*
 * Copyright 2016 NOBUOKA Yu
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
import java.io.InputStream;

/**
 * This class converts `0D 0A` to `0A`.
 *
 * @see <a href="http://stackoverflow.com/questions/13578416/read-binary-stdout-data-from-adb-shell"
 *     >Read binary stdout data from adb shell?</a>
 */
public class AdbInputStream extends InputStream {

    private int mNextValueOrMinusOne = -1;

    private final InputStream mIs;

    public AdbInputStream(InputStream is) {
        mIs = is;
    }

    @Override
    public int read() throws IOException {
        if (mNextValueOrMinusOne == -1) {
            int next = mIs.read();
            if (next == -1) {
                return -1;
            }
            mNextValueOrMinusOne = next;
        }

        if (mNextValueOrMinusOne == 0x0D) {
            int valueFollowingNext = mIs.read();
            if (valueFollowingNext == 0x0A) {
                mNextValueOrMinusOne = -1;
                return 0x0A;
            } else {
                int next = mNextValueOrMinusOne;
                mNextValueOrMinusOne = valueFollowingNext;
                return next;
            }
        } else {
            int next = mNextValueOrMinusOne;
            mNextValueOrMinusOne = -1;
            return next;
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        mIs.close();
    }

}
