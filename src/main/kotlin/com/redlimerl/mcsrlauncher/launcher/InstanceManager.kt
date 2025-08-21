package com.redlimerl.mcsrlauncher.launcher

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.MCSRLauncher.JSON
import com.redlimerl.mcsrlauncher.MCSRLauncher.MAIN_FRAME
import com.redlimerl.mcsrlauncher.data.instance.BasicInstance
import com.redlimerl.mcsrlauncher.data.instance.FabricVersionData
import com.redlimerl.mcsrlauncher.data.instance.LWJGLVersionData
import com.redlimerl.mcsrlauncher.data.meta.MetaVersion
import org.apache.commons.io.FileUtils
import java.nio.file.Path

object InstanceManager {

    private const val DEFAULT_GROUP = "Default"

    val path: Path = MCSRLauncher.BASE_PATH.resolve("instances.json")
    val instances = linkedMapOf<String, ArrayList<BasicInstance>>()

    fun load() {
        if (!path.toFile().exists()) return save()

        instances.putAll(JSON.decodeFromString<Map<String, ArrayList<BasicInstance>>>(FileUtils.readFileToString(path.toFile(), Charsets.UTF_8)))
    }

    fun save() {
        FileUtils.writeStringToFile(path.toFile(), JSON.encodeToString(instances), Charsets.UTF_8)
    }

    fun getNewInstanceName(string: String): String {
        val replacedName = string.replace(" ", "_").replace(Regex("[^\\p{L}\\p{N}_.]"), "")
        val allInstances = instances.values.flatten()
        if (allInstances.find { it.name == replacedName } == null) return replacedName

        var appendNumber = 2
        while (true) {
            val newName = "${replacedName}${appendNumber}"
            if (allInstances.find { it.name == newName } == null) return newName
            appendNumber++
        }
    }

    fun createInstance(text: String, group: String?, vanillaVersion: MetaVersion, lwjglVersion: LWJGLVersionData, fabricVersion: FabricVersionData?): BasicInstance {
        return BasicInstance(
            getNewInstanceName(text),
            text,
            group ?: DEFAULT_GROUP,
            vanillaVersion.version,
            lwjglVersion,
            fabricVersion
        ).also { addInstance(it, it.group) }
    }

    fun renameInstance(instance: BasicInstance, name: String) {
        instance.setInstanceName(name)
        save()
        refreshInstanceList()
    }

    fun moveInstanceGroup(instance: BasicInstance, group: String?) {
        for (key in instances.keys) {
            instanceLoop@ for (basicInstance in instances[key]!!) {
                if (instance == basicInstance) {
                    instances[key]?.remove(instance)
                    break@instanceLoop
                }
            }
            if (instances[key].isNullOrEmpty()) instances.remove(key)
        }

        instances.getOrPut(group ?: DEFAULT_GROUP) { arrayListOf() }.add(instance)
        instance.group = group ?: DEFAULT_GROUP
        save()
        refreshInstanceList()
    }

    fun getInstanceGroup(instance: BasicInstance): String {
        for (key in instances.keys) {
            instanceLoop@ for (basicInstance in instances[key]!!) {
                if (instance == basicInstance) {
                    return key
                }
            }
        }
        throw IllegalArgumentException("instance is not exist in InstanceManager")
    }

    private fun addInstance(instance: BasicInstance, group: String?) {
        instances.getOrPut(group ?: DEFAULT_GROUP) { arrayListOf() }.add(instance)
        instance.onCreate()
        save()
        refreshInstanceList()
    }

    fun deleteInstance(instance: BasicInstance) {
        for (key in instances.keys) {
            instanceLoop@ for (basicInstance in instances[key]!!) {
                if (instance == basicInstance) {
                    instances[key]?.remove(instance)
                    instance.getInstancePath().toFile().also {
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