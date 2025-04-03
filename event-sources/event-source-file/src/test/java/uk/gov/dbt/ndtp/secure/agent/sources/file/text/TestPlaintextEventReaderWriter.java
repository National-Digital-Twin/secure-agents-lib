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
package uk.gov.dbt.ndtp.secure.agent.sources.file.text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import org.apache.jena.sparql.core.DatasetGraph;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventReaderWriter;
import uk.gov.dbt.ndtp.secure.agent.sources.file.yaml.TestYamlEventReaderWriter;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.serializers.DatasetGraphDeserializer;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.serializers.DatasetGraphSerializer;

public class TestPlaintextEventReaderWriter {

    @Test
    public void plaintext_file_source_01() throws IOException {
        File sourceDir = new File("test-data", "rdf");
        PlainTextFileEventSource<Integer, DatasetGraph> source =
                new PlainTextFileEventSource<>(sourceDir, new DatasetGraphDeserializer());
        PlainTextEventReaderWriter<Integer, DatasetGraph> readerWriter =
                new PlainTextEventReaderWriter<>(new DatasetGraphDeserializer(), new DatasetGraphSerializer());
        while (!source.isExhausted()) {
            Event<Integer, DatasetGraph> event = source.poll(Duration.ZERO);
            Assert.assertNull(event.key());
            Assert.assertNotNull(event.value());

            Assert.assertEquals(event.value().size(), 0);
            Assert.assertEquals(event.value().stream().count(), 2);

            verifyRoundTrip(readerWriter, event);
        }
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "Invalid header line.*")
    public void plaintext_file_source_02() throws IOException {
        File f = new File("test-data/malformed1.txt");
        PlainTextEventReaderWriter<Integer, DatasetGraph> readerWriter =
                new PlainTextEventReaderWriter<>(new DatasetGraphDeserializer(), new DatasetGraphSerializer());
        readerWriter.read(f);
    }

    private static <TKey, TValue> void verifyRoundTrip(FileEventReaderWriter<TKey, TValue> writer,
                                                       Event<TKey, TValue> event) throws IOException {
        File f = Files.createTempFile("plaintext-event", ".txt").toFile();
        try {
            writer.write(event, f);
            Assert.assertNotEquals(f.length(), 0L);

            Event<TKey, TValue> retrieved = writer.read(f);
            TestYamlEventReaderWriter.verifySameEvent(event, retrieved);
        } finally {
            f.delete();
        }
    }
}
