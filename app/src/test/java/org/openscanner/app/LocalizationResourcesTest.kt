package org.openscanner.app

import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class LocalizationResourcesTest {
    private val english by lazy { readResources("values") }
    private val simplifiedChinese by lazy { readResources("values-zh-rCN") }

    @Test
    fun englishAndSimplifiedChineseHaveMatchingKeysAndPlaceholders() {
        assertEquals(english.keys, simplifiedChinese.keys)

        val mismatches = english.keys.filter { key ->
            val englishValue = english.getValue(key)
            val chineseValue = simplifiedChinese.getValue(key)
            englishValue.kind != chineseValue.kind || when (englishValue.kind) {
                "string" -> placeholders(englishValue.values.getValue("value")) !=
                    placeholders(chineseValue.values.getValue("value"))
                "plurals" -> {
                    val sharedQuantities = englishValue.values.keys intersect chineseValue.values.keys
                    sharedQuantities.isEmpty() || sharedQuantities.any { quantity ->
                        placeholders(englishValue.values.getValue(quantity)) !=
                            placeholders(chineseValue.values.getValue(quantity))
                    }
                }
                else -> true
            }
        }

        assertTrue("Placeholder mismatches: $mismatches", mismatches.isEmpty())
    }

    @Test
    fun semanticWifiLabelsAreTranslatedIntoSimplifiedChinese() {
        val expected = mapOf(
            "common_hidden_network" to "隐藏网络",
            "common_private_network_alias" to "网络 %1\$d",
            "common_connected_network_alias" to "已连接网络",
            "common_yes" to "是",
            "common_no" to "否",
            "label_security_enterprise" to "企业级",
            "label_security_wpa3_enterprise" to "WPA3 企业级",
            "label_wifi_generation_legacy" to "传统制式",
            "label_wifi_generation_unknown" to "未知",
        )

        expected.forEach { (key, value) ->
            assertEquals(value, simplifiedChinese.getValue(key).values.getValue("value"))
        }
    }

    private fun readResources(qualifier: String): Map<String, ResourceValue> {
        val workingDirectory = Path.of(System.getProperty("user.dir"))
        val appDirectory = if (Files.isDirectory(workingDirectory.resolve("src/main/res"))) {
            workingDirectory
        } else {
            workingDirectory.resolve("app")
        }
        val stringsFile = appDirectory.resolve("src/main/res/$qualifier/strings.xml")
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val document = factory.newDocumentBuilder().parse(stringsFile.toFile())
        val values = linkedMapOf<String, ResourceValue>()
        val children = document.documentElement.childNodes
        for (index in 0 until children.length) {
            val element = children.item(index) as? Element ?: continue
            val name = element.getAttribute("name").takeIf { it.isNotBlank() } ?: continue
            when (element.tagName) {
                "string" -> values[name] = ResourceValue("string", mapOf("value" to element.textContent.trim()))
                "plurals" -> {
                    val quantities = linkedMapOf<String, String>()
                    val items = element.getElementsByTagName("item")
                    for (itemIndex in 0 until items.length) {
                        val item = items.item(itemIndex) as Element
                        quantities[item.getAttribute("quantity")] = item.textContent.trim()
                    }
                    values[name] = ResourceValue("plurals", quantities)
                }
            }
        }
        return values
    }

    private fun placeholders(value: String): List<String> =
        PLACEHOLDER.findAll(value).map { it.value }.sorted().toList()

    private data class ResourceValue(
        val kind: String,
        val values: Map<String, String>,
    )

    private companion object {
        val PLACEHOLDER = Regex("%(?:\\d+\\$)?[a-zA-Z]")
    }
}
