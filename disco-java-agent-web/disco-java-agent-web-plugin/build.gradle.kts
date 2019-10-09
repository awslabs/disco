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

plugins {
    id("com.github.johnrengelman.shadow")
}

dependencies {
    compile(project(":disco-java-agent-web")) {
        //by setting this flag false, we take only what is described by the above project, and not its entire
        //closure of transitive dependencies (i.e. all of Core, all of Bytebuddy, etc)
        //this makes our generated Jar minimal, containing only our source files, and our manifest. All those other
        //dependencies are expected to be in the base agent, which loads this plugin.
        //Ideally we would have a test for this which inspects the final Jar's content, but it can be reviewed manually
        //on the command line with "jar -tf disco-java-agent-web-plugin.jar"
        isTransitive = false
    }
}

tasks.shadowJar  {
    manifest {
        attributes(mapOf(
            //this has to align with the contents of WebSupport.java. Would be good to find a way to avoid this duplication
            "Disco-Installable-Classes" to "com.amazon.disco.agent.web.servlet.HttpServletServiceInterceptor"
        ))
    }
}
