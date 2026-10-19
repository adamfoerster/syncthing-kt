package com.adamfoerster.syncthing.android

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Regression test for 1.0.1: on Android 13 the health check to the core's plain-HTTP API on
 * 127.0.0.1 was rejected by the default cleartext policy, so the app reported a start timeout.
 */
class NetworkSecurityConfigTest {

    private fun parse(path: String) = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(File(path)).documentElement

    @Test
    fun manifestUsesTheNetworkSecurityConfig() {
        val application = parse("src/main/AndroidManifest.xml").getElementsByTagName("application").item(0) as Element
        val config = application.getAttributeNS("http://schemas.android.com/apk/res/android", "networkSecurityConfig")
        assertThat(config).isEqualTo("@xml/network_security_config")
    }

    @Test
    fun cleartextIsAllowedForLoopbackOnly() {
        val root = parse("src/main/res/xml/network_security_config.xml")
        val base = root.getElementsByTagName("base-config").item(0) as Element
        assertThat(base.getAttribute("cleartextTrafficPermitted")).isEqualTo("false")

        val domainConfig = root.getElementsByTagName("domain-config").item(0) as Element
        assertThat(domainConfig.getAttribute("cleartextTrafficPermitted")).isEqualTo("true")
        val domains = domainConfig.getElementsByTagName("domain")
        val names = (0 until domains.length).map { domains.item(it).textContent.trim() }
        assertThat(names).containsExactlyInAnyOrder("127.0.0.1", "localhost")
    }
}
