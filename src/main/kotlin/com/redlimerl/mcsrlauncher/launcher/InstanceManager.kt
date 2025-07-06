package com.redlimerl.mcsrlauncher.launcher

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.MCSRLauncher.JSON
import com.redlimerl.mcsrlauncher.MCSRLauncher.MAIN_FRAME
import com.redlimerl.mcsrlauncher.data.instance.BasicInstance
import com.redlimerl.mcsrlauncher.data.meta.MetaUniqueID
import com.redlimerl.mcsrlauncher.data.meta.MetaVersion
import org.apache.commons.io.FileUtils
import java.nio.file.Path

object InstanceManager {
    val path: Path = MCSRLauncher.BASE_PATH.resolve("instances.json")
    val instances = linkedMapOf<String, ArrayList<BasicInstance>>()

    fun load() {
        if (!path.toFile().exists()) return save()

        instances.putAll(JSON.decodeFromString<Map<String, ArrayList<BasicInstance>>>(FileUtils.readFileToString(path.toFile(), Charsets.UTF_8)))
    }

    private fun save() {
        FileUtils.writeStringToFile(path.toFile(), JSON.encodeToString(instances), Charsets.UTF_8)
    }

    private fun getNewInstanceName(string: String): String {
        val allInstances = instances.values.flatten()
        if (allInstances.find { it.name == string } == null) return string

        var appendNumber = 2
        while (true) {
            val newName = "${string}${appendNumber}"
            if (allInstances.find { it.name == newName } == null) return newName
            appendNumber++
        }
    }

    fun createInstance(text: String, vanillaVersion: MetaVersion, fabricVersion: MetaVersion?): BasicInstance {
        return BasicInstance(
            getNewInstanceName(text.replace(" ", "_").replace(Regex("[^\\p{L}\\p{N}_.]"), "")),
            text,
            hashMapOf<MetaUniqueID, String>().apply {
                put(MetaUniqueID.MINECRAFT, vanillaVersion.version)
                for (require in vanillaVersion.requires) {
                    put(require.uid, require.equals ?: require.suggests ?: throw IllegalStateException("Not found version requirements for MC ${vanillaVersion.version}"))
                }

                if (fabricVersion != null) {
                    put(MetaUniqueID.FABRIC_LOADER, fabricVersion.version)
                    for (require in fabricVersion.requires) {
                        put(require.uid, vanillaVersion.version)
                    }
                }
            }
        )
    }

    fun addInstance(instance: BasicInstance, group: String?) {
        instances.getOrPut(group ?: "Default") { arrayListOf() }.add(instance)
        instance.onCreate()
        save()
        refreshInstanceList()
    }

    fun deleteInstance(instance: BasicInstance) {
        for (key in instances.keys) {
            instanceLoop@ for (basicInstance in instances[key]!!) {
                if (instance == basicInstance) {
                    instances[key]?.remove(instance)
                    instance.getDirPath().toFile().also {
                        if (it.exists()) it.deleteRecursively()
                    }
                    break@instanceLoop
                }
            }
            if (instances[key].isNullOrEmpty()) instances.remove(key)
        }
        save()
        refreshInstanceList()
    }

    fun refreshInstanceList() {
        MAIN_FRAME.loadInstanceList()
    }
}