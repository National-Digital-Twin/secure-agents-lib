// SPDX-License-Identifier: Apache-2.0
// Originally developed by Telicent Ltd.; subsequently adapted, enhanced, and maintained by the National Digital Twin Programme.

/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 *  Modifications made by the National Digital Twin Programme (NDTP)
 *  © Crown Copyright 2025. This work has been developed by the National Digital Twin Programme
 *  and is legally attributed to the Department for Business and Trade (UK) as the governing entity.
 */
package uk.gov.dbt.ndtp.secure.agent.sources.file;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * A comparator for filenames which extracts the numeric portion of the filename, parses it to a long integer and
 * compares based on that resulting in sorting files into numeric order
 */
public class NumericFilenameComparator implements Comparator<File> {

    private final Map<File, Long> fileToNumberCache = new HashMap<>();

    @Override
    public int compare(File x, File y) {
        if (x == y) {
            return 0;
        } else if (x == null) {
            return -1;
        } else if (y == null) {
            return 1;
        }

        Long xNum = fileToNumberCache.computeIfAbsent(x, NumericFilenameComparator::fileToNumber);
        Long yNum = fileToNumberCache.computeIfAbsent(y, NumericFilenameComparator::fileToNumber);

        int c = xNum.compareTo(yNum);
        if (c == 0) {
            c = x.getAbsolutePath().compareTo(y.getAbsolutePath());
        }

        return c;
    }

    /**
     * Converts a file into a number, or if not possible returns {@link Long#MIN_VALUE}
     *
     * @param f File, <strong>MUST</strong> not be {@code null}
     * @return Number
     */
    private static Long fileToNumber(File f) {
        String name = f.getName();
        if (StringUtils.contains(name, ".")) {
            name = name.substring(0, name.indexOf("."));
        }
        name = StringUtils.getDigits(name);
        if (StringUtils.isBlank(name)) {
            return Long.MIN_VALUE;
        }
        try {
            return Long.parseLong(name);
        } catch (NumberFormatException e) {
            return Long.MIN_VALUE;
        }
    }
}
