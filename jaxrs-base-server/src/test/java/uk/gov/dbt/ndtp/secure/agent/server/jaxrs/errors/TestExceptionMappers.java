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
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.errors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.applications.MockApplication;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.ParamsResource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.utils.ParamInfo;

public class TestExceptionMappers {

    @Test
    public void fallback_01() {
        FallbackExceptionMapper mapper = new FallbackExceptionMapper();

        Exception e = new RuntimeException("Failed",
                                           new RuntimeException("Caused by", new RuntimeException("The real error")));
        try(Response response = mapper.toResponse(e)) {
            Assert.assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    @Test
    public void fallback_02() {
        FallbackExceptionMapper mapper = new FallbackExceptionMapper();

        Exception e = new RuntimeException("Failed", new RuntimeException(new RuntimeException("The real error")));
        try(Response response = mapper.toResponse(e)) {
            Assert.assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    @Test
    public void fallback_03() {
        FallbackExceptionMapper mapper = new FallbackExceptionMapper();

        Exception e =
                new RuntimeException("Failed", new RuntimeException(null, new RuntimeException("The real error")));
        try(Response response = mapper.toResponse(e)) {
            Assert.assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }


    @Test
    public void constraint_validation_01() {
        ConstraintViolation<?> a = mock(ConstraintViolation.class);
        when(a.getRootBeanClass()).thenAnswer((Answer<Class<?>>) invocation -> ParamsResource.class);
        Path path = mock(Path.class);
        when(path.iterator()).thenReturn(Collections.emptyIterator());
        when(path.toString()).thenReturn("test");
        when(a.getPropertyPath()).thenReturn(path);

        ParamInfo info = ParamInfo.fromViolation(a);
        Assert.assertEquals(info.getName(), "test");
        Assert.assertNull(info.getType());
    }

    @Test
    public void constraint_validation_02() {
        ConstraintViolation<?> a = mock(ConstraintViolation.class);
        when(a.getRootBeanClass()).thenAnswer((Answer<Class<?>>) invocation -> MockApplication.class);

        Path.BeanNode node = mock(Path.BeanNode.class);
        when(node.getKind()).thenReturn(ElementKind.BEAN);
        when(node.getContainerClass()).thenAnswer((Answer<Class<?>>) invocation -> ParamsResource.class);

        Path path = mock(Path.class);
        when(path.iterator()).thenReturn(List.<Path.Node>of(node).iterator());
        when(path.toString()).thenReturn("test");
        when(a.getPropertyPath()).thenReturn(path);

        ParamInfo info = ParamInfo.fromViolation(a);
        Assert.assertEquals(info.getName(), "test");
        Assert.assertNull(info.getType());
    }

    @Test
    public void constraint_validation_03() {
        ConstraintViolation<?> a = mock(ConstraintViolation.class);

        Path.MethodNode node = mock(Path.MethodNode.class);
        when(node.getKind()).thenReturn(ElementKind.METHOD);

        Path path = mock(Path.class);
        when(path.iterator()).thenReturn(List.<Path.Node>of(node).iterator());
        when(path.toString()).thenReturn("test");
        when(a.getPropertyPath()).thenReturn(path);

        ParamInfo info = ParamInfo.fromViolation(a);
        Assert.assertEquals(info.getName(), "test");
        Assert.assertNull(info.getType());
    }

    @Test
    public void constraint_validation_04() {
        ConstraintViolation<?> a = mock(ConstraintViolation.class);
        when(a.getRootBeanClass()).thenAnswer((Answer<Class<?>>) invocation -> MockApplication.class);

        Path.MethodNode node = mock(Path.MethodNode.class);
        when(node.getKind()).thenReturn(ElementKind.METHOD);
        when(node.getName()).thenReturn("noSuchMethod");
        when(node.getParameterTypes()).thenReturn(Collections.emptyList());

        Path path = mock(Path.class);
        when(path.iterator()).thenReturn(List.<Path.Node>of(node).iterator());
        when(path.toString()).thenReturn("test");
        when(a.getPropertyPath()).thenReturn(path);

        ParamInfo info = ParamInfo.fromViolation(a);
        Assert.assertEquals(info.getName(), "test");
        Assert.assertNull(info.getType());
    }

    @DataProvider(name = "testParameters")
    private Object[][] getTestParameters() {
        return new Object[][] {
                { "path", 0, "Path" },
                { "query", 1, "Query" },
                { "X-Custom-Header", 2, "Header" },
                { "cookie", 3, "Cookie" },
                { "form", 4, "Form" }
        };
    }

    @Test(dataProvider = "testParameters")
    public void constraint_validation_05(String name, int index, String type) {
        ConstraintViolation<?> a = mock(ConstraintViolation.class);
        when(a.getRootBeanClass()).thenAnswer((Answer<Class<?>>) invocation -> ParamsResource.class);

        Path.MethodNode node = mock(Path.MethodNode.class);
        when(node.getKind()).thenReturn(ElementKind.METHOD);
        when(node.getName()).thenReturn("everything");
        when(node.getParameterTypes()).thenReturn(
                List.of(String.class, String.class, String.class, String.class, String.class));

        Path.ParameterNode param = mock(Path.ParameterNode.class);
        when(param.getKind()).thenReturn(ElementKind.PARAMETER);
        when(param.getName()).thenReturn(name);
        when(param.getParameterIndex()).thenReturn(index);

        Path path = mock(Path.class);
        when(path.iterator()).thenReturn(List.of(node, param).iterator());
        when(a.getPropertyPath()).thenReturn(path);

        ParamInfo info = ParamInfo.fromViolation(a);
        Assert.assertEquals(info.getName(), name);
        Assert.assertEquals(info.getType(), type);
    }

    @Test(dataProvider = "testParameters")
    public void constraint_validation_06(String name, int index, String type) {
        ConstraintViolation<?> a = mock(ConstraintViolation.class);
        when(a.getRootBeanClass()).thenAnswer((Answer<Class<?>>) invocation -> ParamsResource.class);

        Path.ParameterNode param = mock(Path.ParameterNode.class);
        when(param.getKind()).thenReturn(ElementKind.PARAMETER);
        when(param.getName()).thenReturn(name);
        when(param.getParameterIndex()).thenReturn(index);

        Path path = mock(Path.class);
        when(path.iterator()).thenReturn(List.<Path.Node>of(param).iterator());
        when(a.getPropertyPath()).thenReturn(path);

        ParamInfo info = ParamInfo.fromViolation(a);
        Assert.assertEquals(info.getName(), name);
        Assert.assertNull(info.getType());
    }

    @Test
    public void constraint_validation_07() {
        // given
        ConstraintViolationMapper mapper = new ConstraintViolationMapper();

        UriInfo mockInfo = mock(UriInfo.class);
        when(mockInfo.getPath()).thenReturn("test");
        mapper.uri = mockInfo;

        ConstraintViolation<?> a = mock(ConstraintViolation.class);
        when(a.getRootBeanClass()).thenAnswer((Answer<Class<?>>) invocation -> ParamsResource.class);
        Path path = mock(Path.class);
        when(path.iterator()).thenReturn(Collections.emptyIterator());
        when(path.toString()).thenReturn("test");
        when(a.getPropertyPath()).thenReturn(path);

        ConstraintViolationException exception = new ConstraintViolationException(Set.of(a));
        // when
        try(Response response = mapper.toResponse(exception)) {
            // then
            Assert.assertEquals(response.getStatus(), 400);
            Assert.assertEquals(response.getStatusInfo().toString(), "Bad Request");
        }
    }
}
