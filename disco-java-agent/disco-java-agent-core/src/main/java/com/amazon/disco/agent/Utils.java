/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package com.amazon.disco.agent;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;

/**
 * Various utilities methods.
 */
public class Utils {
    private final static Utils INSTANCE = new Utils();

    /**
     * Private constructor for singleton semantics
     */
    private Utils() {
    }

    /**
     * Singleton access
     * @return the Utils singleton
     */
    public static Utils getInstance() {
        return INSTANCE;
    }

    /**
     * Finds a folder for temporary files.
     *
     * @return the folder where temporary files should be stored.
     */
    public File getTempFolder() {
        File tmpFolder = new File(System.getProperty("java.io.tmpdir"), "com.amazon.disco.agent");
        if (!tmpFolder.exists())
            tmpFolder.mkdirs();
        return tmpFolder;
    }
}
