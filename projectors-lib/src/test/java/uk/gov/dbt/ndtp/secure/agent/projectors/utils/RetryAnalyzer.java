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
package uk.gov.dbt.ndtp.secure.agent.projectors.utils;

import org.testng.ITestResult;
import org.testng.util.RetryAnalyzerCount;

/**
 * A test retry analyzer for tests that are "flaky", this is typically used for tests that are looking at timing or
 * concurrency issues that run reliably on a developers' machine but can randomly fail when running on CI/CD
 * infrastructure.
 */
public class RetryAnalyzer extends RetryAnalyzerCount {

    /**
     * Creates a new analyzer with a default maximum retries of 3
     */
    public RetryAnalyzer() {
        super();
        this.setCount(3);
    }

    @Override
    public boolean retryMethod(ITestResult result) {
        // Always retry the test, this method is only called by the base class when we're within our maximum permitted
        // retries (3) so always safe to return true
        return true;
    }
}
