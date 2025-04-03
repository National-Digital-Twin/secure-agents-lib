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
package uk.gov.dbt.ndtp.secure.agent.live.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.event.Level;

/**
 * An error structure used to report errors to IANode Live
 */
public class LiveError {

    //@formatter:off
    /*
    Example from documentation
    TODO Establish new URL to point to document detailing the security labels

    {
      "securityLabels": {
        "defaults": "clearance=S",
        "type": "clearance=1",
        "message": "clearance=2",
        "traceback": "clearance=3",
        "level": "clearance=4",
        "counter": "clearance=S"
      },
      "id": "-from-knowledge-to-elasticsearch",
      "timestamp": "2000-01-01T09:45:00+00:00",
      "error_type": "TypeError",
      "error_message": "Invalid integer value",
      "stack_trace": "Traceback of the exception",
      "level": "WARN",
      "counter": 1
    }
     */
    //@formatter:on

    private final Map<String, String> securityLabels = new HashMap<>();
    private String id, type, message, traceback, level;
    private Date timestamp;
    private Long counter;

    /**
     * Creates an empty live error, primarily intended for deserializer usage
     */
    private LiveError() {
    }

    /**
     * Creates a populated live error
     *
     * @param id             Application ID
     * @param timestamp      Error timestamp
     * @param type           Error type
     * @param message        Error message
     * @param traceback      Error traceback
     * @param level          Error level
     * @param counter        Record counter (if applicable)
     * @param securityLabels Security labels that apply to the error (if any)
     */
    private LiveError(String id, Date timestamp, String type, String message, String traceback, String level,
                      Long counter, Map<String, String> securityLabels) {
        this.id = id;
        this.timestamp = timestamp;
        this.type = type;
        this.message = message;
        this.traceback = traceback;
        this.level = level;
        this.counter = counter;
        this.securityLabels.putAll(securityLabels);
    }

    /**
     * Converts a stack trace into a string traceback
     *
     * @param t Throwable
     * @return Traceback
     */
    private static String getTraceback(Throwable t) {
        StringWriter writer = new StringWriter();
        try (PrintWriter printer = new PrintWriter(writer)) {
            t.printStackTrace(printer);
        }
        return writer.toString();
    }

    /**
     * Gets the security labels
     *
     * @return Security labels
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, String> getSecurityLabels() {
        return securityLabels;
    }

    /**
     * Sets the security labels
     *
     * @param securityLabels Security labels
     */
    public void setSecurityLabels(Map<String, String> securityLabels) {
        this.securityLabels.clear();
        this.securityLabels.putAll(securityLabels);
    }

    /**
     * Gets the application ID
     *
     * @return Application ID
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getId() {
        return id;
    }

    /**
     * Sets the application ID
     *
     * @param id Application ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the error type
     *
     * @return Error type
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("error_type")
    public String getType() {
        return type;
    }

    /**
     * Sets the error type
     *
     * @param type Error type
     */
    @JsonProperty("error_type")
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the error message
     *
     * @return Error message
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("error_message")
    public String getMessage() {
        return message;
    }

    /**
     * Sets the error message
     *
     * @param message Error message
     */
    @JsonProperty("error_message")
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the error traceback
     *
     * @return Traceback
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("stack_trace")
    public String getTraceback() {
        return traceback;
    }

    /**
     * Sets the error traceback
     *
     * @param traceback Traceback
     */
    @JsonProperty("stack_trace")
    public void setTraceback(String traceback) {
        this.traceback = traceback;
    }

    /**
     * Sets the error message and traceback from the given {@link Throwable}
     *
     * @param t Throwable
     */
    @JsonIgnore
    public void setError(Throwable t) {
        if (t == null) {
            return;
        }
        this.message = t.getMessage();
        this.traceback = getTraceback(t);
    }

    /**
     * Gets the error level
     *
     * @return Error level
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getLevel() {
        return level;
    }

    /**
     * Sets the error level
     *
     * @param level Error level
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * Gets the error timestamp
     *
     * @return Timestamp
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the error timestamp
     *
     * @param timestamp Timestamp
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Sets the timestamp of the error to the current time
     */
    @JsonIgnore
    public void setTimestampToNow() {
        this.setTimestamp(Date.from(Instant.now()));
    }

    /**
     * Gets the record counter
     *
     * @return Record counter
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getCounter() {
        return counter;
    }

    /**
     * Sets the record counter
     *
     * @param counter Record counter
     */
    public void setCounter(Long counter) {
        this.counter = counter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LiveError liveError = (LiveError) o;
        //@formatter:off
        return Objects.equals(securityLabels, liveError.securityLabels) &&
               Objects.equals(id, liveError.id) &&
               Objects.equals(type, liveError.type) &&
               Objects.equals(message, liveError.message) &&
               Objects.equals(traceback, liveError.traceback) &&
               Objects.equals(level, liveError.level) &&
               Objects.equals(timestamp, liveError.timestamp) &&
               Objects.equals(counter, liveError.counter);
        //@formatter:on
    }

    @Override
    public int hashCode() {
        return Objects.hash(securityLabels, id, type, message, traceback, level, timestamp, counter);
    }

    /**
     * Takes a copy of the error
     *
     * @return Error copy
     */
    public LiveError copy() {
        return new LiveError(this.id, this.timestamp, this.type, this.message, this.traceback, this.level, this.counter,
                             this.securityLabels);
    }

    /**
     * Creates a new builder for live errors
     *
     * @return Builder
     */
    public static Builder create() {
        return new Builder();
    }

    /**
     * Builder for live errors
     */
    public static class Builder {
        private final Map<String, String> securityLabels = new HashMap<>();
        private String id, type, message, traceback, level;
        private Date timestamp;
        private Long counter;

        /**
         * Sets security labels
         *
         * @param securityLabels Security labels
         * @return Builder
         */
        public Builder securityLabels(Map<String, String> securityLabels) {
            this.securityLabels.clear();
            this.securityLabels.putAll(securityLabels);
            return this;
        }

        /**
         * Adds a security label
         *
         * @param field Field
         * @param label Security label
         * @return Builder
         */
        public Builder securityLabel(String field, String label) {
            this.securityLabels.put(field, label);
            return this;
        }

        /**
         * Sets the Application ID for the error
         *
         * @param id Application ID
         * @return Builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the error type
         *
         * @param type Error type
         * @return Builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the error message
         *
         * @param message Error message
         * @return Builder
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the error traceback
         *
         * @param traceback Traceback
         * @return Builder
         */
        public Builder traceback(String traceback) {
            this.traceback = traceback;
            return this;
        }

        /**
         * Sets the error message and traceback
         *
         * @param t Throwable
         * @return Builder
         */
        public Builder error(Throwable t) {
            if (t == null) {
                return this;
            }
            this.message = t.getMessage();
            this.traceback = LiveError.getTraceback(t);
            return this;
        }

        /**
         * Sets the error level
         *
         * @param level Error level
         * @return Builder
         */
        public Builder level(Level level) {
            return level(level.toString());
        }

        /**
         * Sets the error level
         *
         * @param level Error level
         * @return Builder
         */
        public Builder level(String level) {
            this.level = level;
            return this;
        }

        /**
         * Sets the error timestamp
         *
         * @param timestamp Timestamp
         * @return Builder
         */
        public Builder timestamp(Date timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Sets the error timestamp to the current time
         *
         * @return Builder
         */
        public Builder now() {
            return timestamp(Date.from(Instant.now()));
        }

        /**
         * Sets the record counter
         *
         * @param counter Record counter
         * @return Builder
         */
        public Builder recordCounter(Long counter) {
            this.counter = counter;
            return this;
        }

        /**
         * Builds the live error
         *
         * @return Live error
         */
        public LiveError build() {
            return new LiveError(this.id, this.timestamp, this.type, this.message, this.traceback, this.level,
                                 this.counter, this.securityLabels);
        }
    }
}
