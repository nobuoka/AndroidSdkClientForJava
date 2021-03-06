/*
 * Copyright 2014, 2016 NOBUOKA Yu
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

/**
 * Class which is read output of process and respond to it if necessary.
 */
public interface ProcessIoHandler extends AutoCloseable {

    /**
     * Start to handle input and output of Process.
     */
    void start();

    /**
     * Close input stream and output streams of the process, and other
     * resources if necessary.
     */
    @Override
    void close() throws IOException;

    interface Factory<T extends ProcessIoHandler> {
        T createHandler(Process process);
    }

}
