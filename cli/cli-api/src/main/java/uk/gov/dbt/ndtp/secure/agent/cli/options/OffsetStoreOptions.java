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
package uk.gov.dbt.ndtp.secure.agent.cli.options;

import com.github.rvesse.airline.annotations.Option;
import java.io.File;
import uk.gov.dbt.ndtp.secure.agent.sources.offsets.OffsetStore;
import uk.gov.dbt.ndtp.secure.agent.sources.offsets.file.YamlOffsetStore;

/**
 * Options relating to external offset stores
 */
public class OffsetStoreOptions {

    @Option(name = "--offsets-file", title = "OffsetsFile", description = "Specifies an application controlled file that will be used to store Kafka offsets in addition to Kafka Consumer Groups.")
    private File offsetsFile;

    /**
     * Gets the configured offset store (if any)
     *
     * @return Offset store if configured, otherwise {@code null}
     */
    public OffsetStore getOffsetStore() {
        if (this.offsetsFile != null) {
            return new YamlOffsetStore(this.offsetsFile);
        }
        return null;
    }
}
