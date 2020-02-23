package ru.justagod.plugin.test.gradle

import org.junit.Test
import ru.justagod.plugin.test.test9.Test9Runner

class InvocationGradleTests : GradleCommon() {

    init {
        setUpDirectory("""
            cutter {
                printSidesTree = true
                annotation = "anno.SideOnly"
                classesDirs
                def serverSide = side('SERVER')
                def clientSide = side('CLIENT')
                builds {
                    client {
                        targetSides = [clientSide]
                        primalSides = [clientSide, serverSide]
                    }
                    server {
                        targetSides = [serverSide]
                        primalSides = [clientSide, serverSide]
                    }
                }
                invocation {
                    name = 'test9.ServerInvoke'
                    sides = [serverSide]
                    method = 'run()V'
                }
                invocation {
                    name = 'test9.ClientInvoke'
                    sides = [clientSide]
                    method = 'run()V'
                }
            }
        """.trimIndent())
    }

    @Test
    fun test9() {
        insertSourcesAndClean("test9")
        val serverJar = buildJar("server")
        Test9Runner.runServerCheck(root.resolve("build").resolve("libs").resolve(serverJar))

        clean()
        val clientJar = buildJar("client")
        Test9Runner.runClientCheck(root.resolve("build").resolve("libs").resolve(clientJar))
    }
}