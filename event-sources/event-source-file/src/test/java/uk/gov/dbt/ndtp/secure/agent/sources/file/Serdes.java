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

import java.util.Map;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Bytes;
import uk.gov.dbt.ndtp.secure.agent.sources.Header;
import uk.gov.dbt.ndtp.secure.agent.sources.file.rdf.RdfEventReaderWriter;
import uk.gov.dbt.ndtp.secure.agent.sources.file.yaml.YamlEventReaderWriter;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.serializers.*;

public class Serdes {
    public static final StringSerializer STRING_SERIALIZER = new StringSerializer();
    public static final StringDeserializer STRING_DESERIALIZER = new StringDeserializer();
    public static final YamlEventReaderWriter<String, String> YAML_STRING_STRING =
            YamlEventReaderWriter.<String, String>create()
                                 .keySerializer(STRING_SERIALIZER)
                                 .valueSerializer(STRING_SERIALIZER)
                                 .keyDeserializer(STRING_DESERIALIZER)
                                 .valueDeserializer(STRING_DESERIALIZER)
                                 .build();
    public static final IntegerSerializer INTEGER_SERIALIZER = new IntegerSerializer();
    public static final IntegerDeserializer INTEGER_DESERIALIZER = new IntegerDeserializer();
    public static final YamlEventReaderWriter<Integer, String> YAML_INTEGER_STRING =
            YamlEventReaderWriter.<Integer, String>create()
                                 .keySerializer(INTEGER_SERIALIZER)
                                 .valueSerializer(STRING_SERIALIZER)
                                 .keyDeserializer(INTEGER_DESERIALIZER)
                                 .valueDeserializer(STRING_DESERIALIZER)
                                 .build();
    public static final GraphSerializer GRAPH_SERIALIZER = new GraphSerializer();
    public static final GraphDeserializer GRAPH_DESERIALIZER = new GraphDeserializer();
    public static final DatasetGraphSerializer DATASET_GRAPH_SERIALIZER = new DatasetGraphSerializer();
    public static final DatasetGraphDeserializer DATASET_GRAPH_DESERIALIZER = new DatasetGraphDeserializer();
    public static final RdfEventReaderWriter<Integer, DatasetGraph>
            RDF_INTEGER_STRING =
            new RdfEventReaderWriter<>(INTEGER_DESERIALIZER, DATASET_GRAPH_DESERIALIZER,
                                       INTEGER_SERIALIZER, DATASET_GRAPH_SERIALIZER);
    public static final BytesSerializer BYTES_SERIALIZER = new BytesSerializer();
    public static final BytesDeserializer BYTES_DESERIALIZER = new BytesDeserializer();
    public static final YamlEventReaderWriter<Bytes, String> YAML_BYTES_STRING =
            YamlEventReaderWriter.<Bytes, String>create()
                                 .keySerializer(BYTES_SERIALIZER)
                                 .valueSerializer(STRING_SERIALIZER)
                                 .keyDeserializer(BYTES_DESERIALIZER)
                                 .valueDeserializer(STRING_DESERIALIZER)
                                 .build();

    public static final Serializer<Header> HEADER_SERIALIZER = new AbstractJacksonSerializer<>();

    public static final Deserializer<Header> HEADER_DESERIALIZER = new HeaderDeserializer();

    public static final Serializer<Map> MAP_SERIALIZER = new AbstractJacksonSerializer<>();

    public static final Deserializer<Map> MAP_DESERIALIZER = new MapDeserializer();
}
