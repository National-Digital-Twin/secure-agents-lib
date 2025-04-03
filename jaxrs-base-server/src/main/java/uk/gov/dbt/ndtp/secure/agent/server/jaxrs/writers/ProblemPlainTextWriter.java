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
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.writers;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import org.apache.commons.lang3.StringUtils;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.Problem;

/**
 * A message body writer than can serialize {@link Problem} instances into Plain Text responses
 */
@Provider
@Produces(MediaType.TEXT_PLAIN)
public class ProblemPlainTextWriter implements MessageBodyWriter<Problem> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Problem.class && mediaType.equals(MediaType.TEXT_PLAIN_TYPE);
    }

    @Override
    public void writeTo(Problem problem, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws
            WebApplicationException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(entityStream))) {
            writer.print(problem.getStatus());
            writer.print(' ');
            int titleLength = Integer.toString(problem.getStatus()).length() + 1;
            if (StringUtils.isNotBlank(problem.getTitle())) {
                writer.println(problem.getTitle());
                titleLength += problem.getTitle().length();
                addTitleUnderline(writer, titleLength);
            } else {
                writer.println();
                addTitleUnderline(writer, titleLength);
            }
            writer.println(problem.getDetail());
            writer.println();
        }
    }

    private static void addTitleUnderline(PrintWriter writer, int titleLength) {
        writer.println(StringUtils.repeat('-', titleLength));
        writer.println();
    }
}
