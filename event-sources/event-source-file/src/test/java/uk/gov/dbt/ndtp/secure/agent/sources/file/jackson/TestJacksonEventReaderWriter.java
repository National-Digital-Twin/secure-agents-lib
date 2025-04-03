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
package uk.gov.dbt.ndtp.secure.agent.sources.file.jackson;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.collections4.CollectionUtils;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventAccessMode;
import uk.gov.dbt.ndtp.secure.agent.sources.file.Serdes;

public class TestJacksonEventReaderWriter extends AbstractJacksonEventReaderWriter<Integer, String> {
    /**
     * Creates a new Jackson based event reader/writer
     */
    public TestJacksonEventReaderWriter() {
        super(new ObjectMapper(), FileEventAccessMode.READ_WRITE, Serdes.INTEGER_SERIALIZER, Serdes.STRING_SERIALIZER,
              Serdes.INTEGER_DESERIALIZER,
              Serdes.STRING_DESERIALIZER);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "Unexpected JSON Token START_ARRAY.*")
    public void test_01() throws IOException {
        JsonParser parser = mock(JsonParser.class);
        final Queue<JsonToken> mockTokens = new LinkedList<>();
        CollectionUtils.addAll(mockTokens, JsonToken.START_OBJECT, JsonToken.START_ARRAY);
        final AtomicReference<JsonToken> currentToken = new AtomicReference<>(null);
        when(parser.nextToken()).then((Answer<JsonToken>) invocation -> {
            if (!mockTokens.isEmpty()) {
                currentToken.set(mockTokens.poll());
                return currentToken.get();
            }
            throw new IOException("EOF");
        });
        when(parser.currentToken()).then((Answer<JsonToken>) invocation -> currentToken.get());

        this.readEvent(parser);
    }
}
