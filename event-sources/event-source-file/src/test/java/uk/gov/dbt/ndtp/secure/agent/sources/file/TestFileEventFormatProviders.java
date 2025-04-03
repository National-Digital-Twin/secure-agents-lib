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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestFileEventFormatProviders {

    @Test
    public void yaml_registered() {
        Assert.assertNotNull(FileEventFormats.get("yaml"));
    }

    @Test
    public void plaintext_registered() {
        Assert.assertNotNull(FileEventFormats.get("text"));
    }

    @Test
    public void rdf_registered() {
        Assert.assertNotNull(FileEventFormats.get("rdf"));
    }

    @DataProvider(name = "formats")
    public Object[][] getRegisteredFormats() {
        List<String> available = new ArrayList<>(FileEventFormats.available());
        Object[][] formats = new Object[available.size()][];
        for (int i = 0; i < available.size(); i++) {
            formats[i] = new Object[] { available.get(i) };
        }
        return formats;
    }

    @Test(dataProvider = "formats")
    public void name_01(String format) {
        FileEventFormatProvider provider = FileEventFormats.get(format);
        Assert.assertNotNull(provider);
        Assert.assertNotNull(provider.name());
        Assert.assertFalse(StringUtils.isBlank(provider.name()));
    }

    @Test(dataProvider = "formats")
    public void default_file_extension_01(String format) {
        FileEventFormatProvider provider = FileEventFormats.get(format);
        Assert.assertNotNull(provider);
        Assert.assertNotNull(provider.defaultFileExtension());
        Assert.assertFalse(StringUtils.isBlank(provider.defaultFileExtension()));
    }

    @Test(dataProvider = "formats")
    public void create_reader_01(String format) {
        FileEventFormatProvider provider = FileEventFormats.get(format);
        FileEventReader<Integer, String> reader =
                provider.createReader(Serdes.INTEGER_DESERIALIZER, Serdes.STRING_DESERIALIZER);
        Assert.assertNotNull(reader);

        if (reader instanceof FileEventReaderWriter<?, ?> fileEventReaderWriter) {
            Assert.assertEquals(fileEventReaderWriter.getMode(), FileEventAccessMode.READ_ONLY);
        }
    }

    @Test(dataProvider = "formats")
    public void create_writer_01(String format) {
        FileEventFormatProvider provider = FileEventFormats.get(format);
        FileEventWriter<Integer, String> writer =
                provider.createWriter(Serdes.INTEGER_SERIALIZER, Serdes.STRING_SERIALIZER);
        Assert.assertNotNull(writer);

        if (writer instanceof FileEventReaderWriter<?, ?> fileEventReaderWriter) {
            Assert.assertEquals(fileEventReaderWriter.getMode(), FileEventAccessMode.WRITE_ONLY);
        }
    }

    @Test(dataProvider = "formats")
    public void create_reader_writer_01(String format) {
        FileEventFormatProvider provider = FileEventFormats.get(format);
        FileEventReaderWriter<Integer, String> readerWriter =
                provider.createReaderWriter(Serdes.INTEGER_DESERIALIZER, Serdes.STRING_DESERIALIZER,
                                            Serdes.INTEGER_SERIALIZER, Serdes.STRING_SERIALIZER);
        Assert.assertNotNull(readerWriter);
        Assert.assertNotNull(readerWriter.getMode(), "Null mode for format " + format);
        Assert.assertEquals(readerWriter.getMode(), FileEventAccessMode.READ_WRITE);
    }

    @Test(dataProvider = "formats")
    public void create_source_01(String format) {
        FileEventFormatProvider provider = FileEventFormats.get(format);
        File workingDir = new File(Paths.get(".").toAbsolutePath().toString());
        FileEventSource<Integer, String> source =
                provider.createSource(Serdes.INTEGER_DESERIALIZER, Serdes.STRING_DESERIALIZER, workingDir);
        Assert.assertFalse(source.availableImmediately());
        Assert.assertTrue(source.isExhausted());
        Assert.assertEquals(source.remaining(), 0L);

        source.close();
    }

    @Test(dataProvider = "formats")
    public void create_source_02(String format) {
        FileEventFormatProvider provider = FileEventFormats.get(format);
        File workingDir = new File(Paths.get("pom.xml").toAbsolutePath().toString());
        FileEventSource<Integer, String> source =
                provider.createSingleFileSource(Serdes.INTEGER_DESERIALIZER, Serdes.STRING_DESERIALIZER, workingDir);
        Assert.assertTrue(source.availableImmediately());
        Assert.assertFalse(source.isExhausted());
        Assert.assertEquals(source.remaining(), 1L);

        source.close();
    }
}
