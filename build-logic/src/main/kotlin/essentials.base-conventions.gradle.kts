import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("java")
    id("net.kyori.indra")
    id("net.kyori.indra.checkstyle")
    id("net.kyori.indra.publishing")
}

val baseExtension = extensions.create<EssentialsBaseExtension>("essentials", project)

val checkstyleVersion = "8.36.2"
val paperVersion = "1.21.6-R0.1-SNAPSHOT"
val paperTestVersion = "1.21.5-R0.1-SNAPSHOT"
val junit5Version = "5.12.2"
val junitPlatformVersion = "1.12.2"
val mockitoVersion = "5.18.0"

dependencies {
    testImplementation("org.junit.jupiter", "junit-jupiter", junit5Version)
    testImplementation("org.junit.platform", "junit-platform-launcher", junitPlatformVersion)
    testImplementation("org.mockito", "mockito-core", mockitoVersion)
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.50.0") {
        exclude(module = "paper-api")
        exclude(module = "spigot-api")
    }

    constraints {
        implementation("org.yaml:snakeyaml:1.28") {
            because("Bukkit API ships old versions, Configurate requires modern versions")
        }
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("PASSED", "SKIPPED", "FAILED")
    }
}

afterEvaluate {
    if (baseExtension.injectBukkitApi.get()) {
        dependencies {
            api("io.papermc.paper", "paper-api", paperVersion)
            testImplementation("io.papermc.paper", "paper-api", paperTestVersion)
        }

        configurations {
            testCompileClasspath {
                resolutionStrategy {
                    dependencySubstitution {
                        substitute( module("io.papermc.paper:paper-api"))
                            .using(module("io.papermc.paper:paper-api:$paperTestVersion"))
                    }
                }
            }
            testRuntimeClasspath {
                resolutionStrategy {
                    dependencySubstitution {
                        substitute( module("io.papermc.paper:paper-api"))
                            .using(module("io.papermc.paper:paper-api:$paperTestVersion"))
                    }
                }
            }
        }

        java {
            disableAutoTargetJvm()
        }
    }
    if (baseExtension.injectBstats.get()) {
        dependencies {
            implementation("org.bstats", "bstats-bukkit", "2.2.1")
        }
    }
}

tasks {
    // Version Injection
    processResources {
        // Always process resources if version string or git branch changes
        val fullVersion = rootProject.ext["FULL_VERSION"] as String
        val gitBranch = rootProject.ext["GIT_BRANCH"] as String
        inputs.property("fullVersion", fullVersion)
        inputs.property("gitBranch", gitBranch)
        filter<ReplaceTokens>(
            "beginToken" to "\${",
            "endToken" to "}",
            "tokens" to mapOf(
                "full.version" to fullVersion,
                "git.branch" to gitBranch
            )
        )
    }
    compileJava {
        options.compilerArgs.add("-Xlint:-deprecation")
    }
    javadoc {
        title = "${project.name} API (v${rootProject.ext["FULL_VERSION"]})"
        val options = options as? StandardJavadocDocletOptions ?: return@javadoc
        options.links(
            "https://hub.spigotmc.org/javadocs/spigot/"
        )
        options.addBooleanOption("Xdoclint:none", true)
    }
    withType<Jar> {
        archiveVersion.set(rootProject.ext["FULL_VERSION"] as String)
        manifest {
            attributes("paperweight-mappings-namespace" to "mojang")
        }
    }
    withType<Sign> {
        onlyIf { project.hasProperty("forceSign") }
    }
}

// Dependency caching
configurations.all {
    resolutionStrategy.cacheChangingModulesFor(5, "minutes")
}

indra {
    checkstyle(checkstyleVersion)

    github("EssentialsX", "Essentials")
    gpl3OnlyLicense()

    publishReleasesTo("essx", "https://repo.essentialsx.net/releases/")
    publishSnapshotsTo("essx", "https://repo.essentialsx.net/snapshots/")

    configurePublications {
        pom {
            description.set("The essential plugin suite for Minecraft servers.")
            url.set("https://essentialsx.net")
            developers {
                developer {
                    id.set("mdcfe")
                    name.set("MD")
                    email.set("md@n3fs.co.uk")
                }
                developer {
                    id.set("pop4959")
                }
                developer {
                    id.set("JRoy")
                    name.set("Josh Roy")
                }
            }
            ciManagement {
                system.set("Jenkins")
                url.set("https://ci.ender.zone/job/EssentialsX")
            }
        }
    }

    javaVersions {
        target(8)
        minimumToolchain(21)
        // Don't enforce running tests on Java 8; we only care about the release for compiling, not running tests
        strictVersions(false)
    }
}

// undo https://github.com/KyoriPowered/indra/blob/master/indra-common/src/main/kotlin/net/kyori/indra/IndraPlugin.kt#L57
extensions.getByType<BasePluginExtension>().archivesName.set(project.name)
