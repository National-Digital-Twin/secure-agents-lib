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

import java.util.Set;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.parameters.ModeParametersProvider;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.AbstractHealthResource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.DataResource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.HealthResource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.ParamsResource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.ProblemsResource;

public class MockApplication extends AbstractApplication {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = super.getClasses();
        classes.add(ModeParametersProvider.class);
        classes.add(HealthResource.class);
        classes.add(DataResource.class);
        classes.add(ParamsResource.class);
        classes.add(ProblemsResource.class);
        classes.add(ProblemCustomReaderWriter.class);
        return classes;
    }

    @Override
    protected Class<? extends AbstractHealthResource> getHealthResourceClass() {
        return null;
    }
}
