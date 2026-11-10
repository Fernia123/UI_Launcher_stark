// Fix for AGP AndroidLocationsException when both ANDROID_PREFS_ROOT and ANDROID_USER_HOME are set in OS environment
@Suppress("UNCHECKED_CAST")
fun removeEnv(key: String) {
    try {
        val unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null)
        val pe = Class.forName("java.lang.ProcessEnvironment")
        val fieldOffset = unsafe.javaClass.getMethod("staticFieldOffset", java.lang.reflect.Field::class.java)
        val getObject = unsafe.javaClass.getMethod("getObject", Any::class.java, Long::class.javaPrimitiveType)

        for (fieldName in listOf("theEnvironment", "theCaseInsensitiveEnvironment")) {
            try {
                val f = pe.getDeclaredField(fieldName)
                val offset = fieldOffset.invoke(unsafe, f) as Long
                var map = getObject.invoke(unsafe, pe, offset)
                if (map != null) {
                    if (map.javaClass.name.contains("UnmodifiableMap")) {
                        val mField = map.javaClass.declaredFields.first { it.name == "m" || Map::class.java.isAssignableFrom(it.type) }
                        val mOffset = fieldOffset.invoke(unsafe, mField) as Long
                        map = getObject.invoke(unsafe, map, mOffset)
                    }
                    val mutableMap = map as? MutableMap<String, String>
                    if (mutableMap != null) {
                        val keysToRemove = mutableMap.keys.filter { it.equals(key, ignoreCase = true) }
                        for (k in keysToRemove) {
                            mutableMap.remove(k)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    } catch (_: Exception) {}
}
removeEnv("ANDROID_PREFS_ROOT")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Prubea_.01_bloat"
include(":app")
