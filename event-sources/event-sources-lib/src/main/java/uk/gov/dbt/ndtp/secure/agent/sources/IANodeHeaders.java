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
package uk.gov.dbt.ndtp.secure.agent.sources;

/**
 * Provides constants related to IANode's standard event headers
 */
public class IANodeHeaders {

    private IANodeHeaders() {
    }

    /**
     * Event header used to specify the default security labels for data that is not more specifically labelled
     */
    public static final String SECURITY_LABEL = "Security-Label";

    /**
     * Event header used to track the execution path for an event
     */
    public static final String EXEC_PATH = "Exec-Path";

    /**
     * Event header conveying a summary for the reason an event was sent to a dead letter queue (DLQ)
     */
    public static final String DEAD_LETTER_REASON = "Dead-Letter-Reason";

    /**
     * Event header used to identify the ID of the input event that was used to generate an output event
     */
    public static final String INPUT_REQUEST_ID = "Input-Request-ID";

    /**
     * Event header used to uniquely identify a request flowing through the system
     */
    public static final String REQUEST_ID = "Request-ID";

    /**
     * Event header used to identify the content type, similar to HTTP, of the message body.  This is mainly used on our
     * {@code knowledge} topic where we exchange RDF messages and allow for them to be in any RDF serialization that
     * our RDF toolchain - Apache Jena - supports.
     */
    public static final String CONTENT_TYPE = "Content-Type";

    /**
     * Event header used to identify the name of the data source/pipeline that an event originated from
     */
    public static final String DATA_SOURCE_NAME = "Data-Source-Name";

    /**
     * Event header used to identify the type of the data source/pipeline that an event originated from
     */
    public static final String DATA_SOURCE_TYPE = "Data-Source-Type";
}
