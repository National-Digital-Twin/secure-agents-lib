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
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestProblem {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    public void givenProblemDetails_whenConstructingProblem_thenValuesAsExpected_andRoundTripsViaJson() throws
            JsonProcessingException {
        // Given and When
        Problem problem = createTestProblem();

        // Then
        Assert.assertTrue(problem.equals(problem));
        Assert.assertFalse(problem.equals(null));
        Assert.assertFalse(problem.equals(new Object()));

        // And
        String json = this.json.writeValueAsString(problem);
        Problem retrieved = this.json.readValue(json, Problem.class);
        Assert.assertEquals(retrieved, problem);
        Assert.assertTrue(retrieved.equals(problem));
        Assert.assertEquals(retrieved.hashCode(), problem.hashCode());
        Assert.assertEquals(retrieved.toString(), problem.toString());
    }

    @Test
    public void givenTwoIdenticalProblems_whenComparingEquality_thenCorrect_andMutatingOneChangesEqualityResult() {
        // Given
        Problem a = createTestProblem();
        Problem b = createTestProblem();

        // When and Then
        Assert.assertEquals(a, b);
        Assert.assertTrue(a.equals(b));

        // And
        // Mutate each field checking equality no longer holds
        b.setDetail(null);
        verifyNotEquals(a, b);
        b.setInstance(null);
        verifyNotEquals(a, b);
        b.setTitle(null);
        verifyNotEquals(a, b);
        b.setType(null);
        verifyNotEquals(a, b);
        b.setStatus(-1);
        verifyNotEquals(a, b);
    }

    private static void verifyNotEquals(Problem a, Problem b) {
        Assert.assertNotEquals(a, b);
        Assert.assertFalse(a.equals(b));
    }

    @Test
    public void givenProblem_whenConvertingToResponse_thenDefaultContentTypeSet() {
        // Given
        Problem problem = createTestProblem();

        // When
        Response response = problem.toResponse();

        // Then
        Assert.assertEquals(response.getMediaType(), Problem.APPLICATION_PROBLEM_JSON);
    }

    @Test
    public void givenProblem_whenConvertingToResponseWithWildcardAccept_thenDefaultContentTypeSet() {
        // Given
        Problem problem = createTestProblem();
        HttpHeaders headers = mock(HttpHeaders.class);
        when(headers.getAcceptableMediaTypes()).thenReturn(List.of(MediaType.WILDCARD_TYPE));

        // When
        Response response = problem.toResponse(headers);

        // Then
        Assert.assertEquals(response.getMediaType(), Problem.APPLICATION_PROBLEM_JSON);
    }

    @DataProvider(name = "mediaTypes")
    private Object[][] mediaTypes() {
        return new Object[][] {
                {
                        List.of(MediaType.APPLICATION_JSON, Problem.MEDIA_TYPE), MediaType.APPLICATION_JSON
                }, {
                        List.of(MediaType.TEXT_PLAIN), MediaType.TEXT_PLAIN
                }, {
                        List.of(Problem.MEDIA_TYPE), Problem.MEDIA_TYPE
                }, {
                        List.of("application/custom"), null
                }
        };
    }

    @Test(dataProvider = "mediaTypes")
    public void givenProblem_whenConvertingToResponseWithAccept_thenExpectedContentTypeSet(List<String> accept,
                                                                                           String expected) {
        // Given
        Problem problem = createTestProblem();
        HttpHeaders headers = mock(HttpHeaders.class);
        when(headers.getAcceptableMediaTypes()).thenReturn(accept.stream().map(MediaType::valueOf).toList());

        // When
        Response response = problem.toResponse(headers);

        // Then
        if (expected != null) {
            Assert.assertEquals(response.getMediaType(), MediaType.valueOf(expected));
        } else {
            Assert.assertNull(response.getMediaType());
        }
    }

    private static Problem createTestProblem() {
        return new Problem("Test", "Title", 400, "You were being bad", "BadRequest#1234");
    }
}
