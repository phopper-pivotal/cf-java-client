/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.operations.applications;

import org.cloudfoundry.Nullable;
import org.immutables.value.Value;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * The request options for the push application operation
 */
@Value.Immutable
abstract class _PushApplicationManifestRequest {

    /**
     * The manifests to be pushed
     */
    abstract List<ApplicationManifest> getManifests();

    /**
     * Do not start the application after pushing
     */
    @Nullable
    abstract Boolean getNoStart();

    /**
     * How long to wait for staging
     */
    @Value.Default
    Duration getStagingTimeout() {
        return Duration.ofMinutes(5);
    }

    /**
     * How long to wait for startup
     */
    @Value.Default
    Duration getStartupTimeout() {
        return Duration.ofMinutes(5);
    }

}
