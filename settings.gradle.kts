/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

rootProject.name = "software.amazon.disco"

include("disco-java-agent:disco-java-agent")
include("disco-java-agent:disco-java-agent-plugin-api")
include("disco-java-agent:disco-java-agent-api")
include("disco-java-agent:disco-java-agent-core")
include("disco-java-agent:disco-java-agent-inject-api")
include("disco-java-agent:disco-java-agent-inject-test")
include("disco-java-agent:disco-java-agent-deps:byte-buddy")

include("disco-java-agent-aws")
include("disco-java-agent-aws:disco-java-agent-aws-api")
include("disco-java-agent-aws:disco-java-agent-aws-plugin")

include("disco-java-agent-web")
include("disco-java-agent-web:disco-java-agent-web-plugin")

include("disco-java-agent-sql")
include("disco-java-agent-sql:disco-java-agent-sql-plugin")

include("disco-java-agent-kotlin")
include("disco-java-agent-kotlin:disco-java-agent-kotlin-plugin")

include("disco-java-agent-instrumentation-preprocess")
include("disco-java-agent-instrumentation-preprocess-test")
include("disco-java-agent-instrumentation-preprocess-test:disco-java-agent-instrumentation-preprocess-test-plugin")
include("disco-java-agent-instrumentation-preprocess-test:disco-java-agent-instrumentation-preprocess-test-target")

include("disco-toolkit-bom")
