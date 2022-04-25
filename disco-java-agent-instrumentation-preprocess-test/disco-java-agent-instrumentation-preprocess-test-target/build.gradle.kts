plugins {
    `java-library`
}

dependencies {
    compileOnly(project(":disco-java-agent:disco-java-agent-api"))
    compileOnly(project(":disco-java-agent:disco-java-agent-core"))
    compileOnly(project(":disco-java-agent:disco-java-agent-plugin-api"))
}
