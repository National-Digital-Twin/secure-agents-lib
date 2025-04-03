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
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.applications;

import uk.gov.dbt.ndtp.jena.abac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.abac.Hierarchy;
import uk.gov.dbt.ndtp.jena.abac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeValue;
import uk.gov.dbt.ndtp.jena.abac.attributes.ValueTerm;
import uk.gov.dbt.ndtp.jena.abac.lib.AttributesStore;
import uk.gov.dbt.ndtp.jena.abac.lib.AttributesStoreLocal;
import uk.gov.dbt.ndtp.jena.abac.lib.AttributesStoreRemote;
import java.io.IOException;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.utils.RandomPortProvider;

public class TestAttributesServer {

    private static final RandomPortProvider PORT = new RandomPortProvider(17333);

    @BeforeMethod
    public void setup() {
        System.getProperties().remove(AttributesStore.class);
    }


    @Test
    public void givenMockAttributesServerWithNoStore_whenStartingAndLookingUpAttributes_thenNullIsReturned() throws
            IOException {
        // Given
        MockAttributesServer server = new MockAttributesServer(PORT.newPort(), null);
        try {
            // When
            server.start();
            AttributesStoreRemote remote = createRemoteStore(server);
            AttributeValueSet values = remote.attributes("test");

            // Then
            Assert.assertNull(values);
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void givenMockAttributesServerWithValidStore_whenStartingAndLookingUpAttributes_thenAttributesAreReturned() throws
            IOException {
        // Given
        AttributeValueSet expected = AttributeValueSet.of(
                List.of(AttributeValue.of("name", ValueTerm.value("Thomas T. Test")),
                        AttributeValue.of("admin", ValueTerm.value(true))));
        AttributesStoreLocal local = new AttributesStoreLocal();
        local.put("test", expected);
        MockAttributesServer server = new MockAttributesServer(PORT.newPort(), local);
        try {
            // When
            server.start();
            AttributesStoreRemote remote = createRemoteStore(server);
            AttributeValueSet actual = remote.attributes("test");

            // Then
            Assert.assertNotNull(actual);
            Assert.assertEquals(actual, expected);
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void givenMockAttributesServerWithNoStore_whenStartingAndLookingUpHierarchies_thenNullIsReturned() throws
            IOException {
        // Given
        MockAttributesServer server = new MockAttributesServer(PORT.newPort(), null);
        try {
            // When
            server.start();
            AttributesStoreRemote remote = createRemoteStore(server);
            Hierarchy hierarchy = remote.getHierarchy(Attribute.create("clearance"));

            // Then
            Assert.assertNull(hierarchy);
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void givenMockAttributesServerWithHierarchyAwareStore_whenStartingAndLookingUpHierarchies_thenHierarchyIsReturned() throws
            IOException {
        // Given
        AttributesStoreLocal local = new AttributesStoreLocal();
        Attribute key = Attribute.create("clearance");
        local.addHierarchy(Hierarchy.create(key, "P", "O", "S", "TS"));
        MockAttributesServer server = new MockAttributesServer(PORT.newPort(), local);
        try {
            // When
            server.start();
            AttributesStoreRemote remote = createRemoteStore(server);
            Hierarchy hierarchy = remote.getHierarchy(key);

            // Then
            Assert.assertNotNull(hierarchy);
            Assert.assertEquals(hierarchy.values().size(), 4);
        } finally {
            server.shutdown();
        }
    }

    private static AttributesStoreRemote createRemoteStore(MockAttributesServer server) {
        return new AttributesStoreRemote(server.getUserLookupUrl(), server.getHierarchyLookupUrl());
    }
}
