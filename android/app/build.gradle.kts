// La app de Android: un WebView con la web de la raíz del repo dentro (se
// copia al compilar, en assets/web/). Sin librerías: sólo Java y Android.
import javax.inject.Inject

plugins {
    id("com.android.application")
}

// Lo mismo que lleva el ejecutable (herramientas/construir-ejecutables.py),
// sin sw.js: aquí no hay service worker.
val laWeb = listOf(
    "index.html", "reproductor.html", "jugadores.html", "manifest.webmanifest",
    "estilos/**", "scripts/**", "datos/**", "tipografias/**", "imagenes/**", "sonidos/**",
)
val fuera = listOf("**/*.txt", "**/*.md", "**/*.py")
val raizDelRepo: File = rootDir.parentFile

abstract class CopiarWeb : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val entrada: ConfigurableFileCollection

    @get:Internal
    abstract val raiz: DirectoryProperty

    @get:Input
    abstract val incluir: ListProperty<String>

    @get:Input
    abstract val excluir: ListProperty<String>

    @get:OutputDirectory
    abstract val salida: DirectoryProperty

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun copiar() {
        val inc = incluir.get()
        val exc = excluir.get()
        fs.sync {
            from(raiz) {
                include(inc)
                exclude(exc)
                into("web")
            }
            into(salida)
        }
    }
}

val copiarWeb = tasks.register<CopiarWeb>("copiarWeb") {
    raiz.set(raizDelRepo)
    incluir.set(laWeb)
    excluir.set(fuera)
    entrada.from(fileTree(raizDelRepo) {
        include(laWeb)
        exclude(fuera)
    })
}

// Las versiones: el número de la compilación de GitHub (cada una, mayor
// que la anterior) y la fecha.
val numero = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
val nombreVersion = System.getenv("VERSION_APP") ?: "dev"
// La firma de las versiones publicadas: de los secretos de GitHub (ver LEEME).
val almacen: String? = System.getenv("ANDROID_ALMACEN")

android {
    namespace = "richi.pantallamaster"
    compileSdk = 36

    defaultConfig {
        applicationId = "richi.pantallamaster"
        minSdk = 24
        targetSdk = 36
        versionCode = numero
        versionName = nombreVersion
    }

    signingConfigs {
        // Las de prueba, con una clave que va en el repo (prueba.keystore):
        // así una de prueba se instala encima de otra sin perder lo guardado.
        getByName("debug") {
            storeFile = file("../prueba.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (almacen != null) {
            create("publicar") {
                storeFile = file(almacen)
                storePassword = System.getenv("ANDROID_ALMACEN_CLAVE")
                keyAlias = System.getenv("ANDROID_ALIAS")
                keyPassword = System.getenv("ANDROID_ALIAS_CLAVE")
            }
        }
    }

    buildTypes {
        debug {
            // Otra app («Máster (prueba)», ver src/debug/): convive con la de verdad.
            applicationIdSuffix = ".prueba"
            versionNameSuffix = "-prueba"
        }
        release {
            isMinifyEnabled = false
            if (almacen != null) {
                signingConfig = signingConfigs.getByName("publicar")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
    }
}

androidComponents {
    onVariants { variante ->
        variante.sources.assets?.addGeneratedSourceDirectory(copiarWeb, CopiarWeb::salida)
    }
}
