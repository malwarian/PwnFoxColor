# PwnFoxColor

Un mismo repositorio contiene dos implementaciones:

| Producto | Carpeta | Paquete generado |
| --- | --- | --- |
| Burp Suite | Raíz (`src/`) | `build/libs/PwnFoxColor.jar` |
| Caido | [`caido/`](caido/README.md) | `caido/dist/plugin_package.zip` |

La versión de Caido identifica el contenedor con `X-PwnFox-Color`, colorea History con el HEX exacto y muestra la etiqueta en la vista **Session** de la petición. Consulta [sus instrucciones](caido/README.md) para compilarla e instalarla.

## Burp Suite

Extensión Montoya que colorea las entradas de **Proxy > HTTP history** según el puerto del listener de Burp. Permite asociar a cada puerto una etiqueta como `Blue Container` o `Session 1` y genera el Bambda necesario para verla en una columna personalizada.

## Requisitos

- Burp Suite con soporte para Montoya API 2026.7.
- Java 17 o superior para compilar.
- Burp Suite Professional para la columna personalizada de HTTP history. El coloreado y la pestaña de configuración funcionan sin esa columna.

## Compilar e instalar

Con Gradle instalado:

```powershell
gradle jar
```

El archivo resultante es `build/libs/PwnFoxColor.jar`. En Burp, abre **Extensions > Installed > Add**, elige **Java** y selecciona ese JAR.

## Configuración

La interfaz se abre en **English** por defecto. Usa **Language** en la parte superior de la pestaña **PwnFoxColor** para elegir **Español**; Burp recuerda la selección al recargar la extensión. Cambiar el idioma no modifica reglas ni etiquetas.

1. Abre la pestaña **PwnFoxColor** de Burp y pulsa **Create listeners 8081–8088** para añadir automáticamente los listeners PwnFox que falten. Se vinculan solo a la interfaz local y los listeners ya existentes conservan su configuración. Comprueba que aparezcan como **Running** en **Settings > Tools > Proxy > Proxy listeners**. También puedes crearlos manualmente.
2. Añade una fila por listener: puerto, etiqueta y color HEX. Puedes escribir `#RRGGBB` (o `#RGB`) o pulsar **Choose color...** para abrir la paleta. La tabla muestra una muestra del HEX elegido y el resaltado nativo que Burp aplicará (por ejemplo, `#6666FF` corresponde a `BLUE`). Pulsa **Save rules**.
   También puedes pulsar **Add PwnFox 8081–8088** para guardar de una vez los ocho colores de PwnFox en ese orden. El botón conserva las reglas que ya existan en esos puertos. La creación de listeners y el mapeo de colores son acciones separadas; cada contenedor debe seguir configurado para usar su puerto correspondiente.
3. Las peticiones nuevas de esos puertos reciben automáticamente el resaltado nativo de Burp. Si quieres aplicar las reglas a entradas ya capturadas, pulsa **Color existing history**. Esta acción reemplaza el resaltado manual de las entradas que coincidan.
4. En Burp Professional, pulsa **Prepare Session column**. Esto guarda las reglas e importa el Bambda a la biblioteca de Burp. Después ve a **Proxy > HTTP history > menú ⋮ sobre la tabla > Add custom column > Load**, selecciona `PwnFoxColor Session`, escribe `Session` en **Column header** y pulsa **Apply & close**. El menú de casillas que aparece al pulsar el encabezado de una columna solo muestra columnas existentes; no permite añadir una nueva. Si cambias etiquetas o puertos, vuelve a preparar la columna y carga la versión actualizada. Como alternativa, **Copy column Bambda** permite pegar el código directamente en el editor de la columna.

Las reglas se guardan en las preferencias de Burp, de modo que permanecen al recargar la extensión. La columna evalúa el `listenerPort()` de cada entrada y muestra la etiqueta configurada; la extensión no modifica las notas ni el contenido HTTP.

## Privacidad

La extensión no realiza llamadas de red propias ni envía datos a servicios externos. Las etiquetas, colores e idioma se guardan en las preferencias locales de Burp. Al crear listeners, la configuración del proyecto se procesa en memoria y se importa de vuelta a Burp; la extensión no la escribe en archivos. El Bambda se copia al portapapeles solo cuando pulsas el botón correspondiente, o se importa a la biblioteca local de Burp cuando pulsas **Prepare Session column**.

## Límite de la API

Montoya permite leer el puerto del listener y establecer los colores de resaltado de Burp. El selector HEX es exacto en la configuración, pero **HTTP history solo puede usar uno de los nueve colores nativos**; la extensión elige el más cercano. Montoya permite importar el Bambda a la biblioteca, pero no activar directamente una columna en la tabla de HTTP history; ese último paso se hace desde la interfaz de Burp Professional.

Para crear listeners, la extensión exporta la configuración del proyecto y vuelve a importar solo la lista de listeners con los puertos que faltan. Los puertos existentes se preservan. Si otro programa ocupa alguno, Burp puede dejarlo sin iniciar; revisa el estado **Running**. Los listeners nuevos aceptan conexiones locales; el botón no cambia la configuración de PwnFox ni de los contenedores.

## Fuentes de la API

- [Puerto de listener en el historial](https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/proxy/ProxyHttpRequestResponse.html)
- [Exportar e importar configuración del proyecto](https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/burpsuite/BurpSuite.html)
- [Resaltado mediante anotaciones](https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/core/Annotations.html)
- [Columnas personalizadas de HTTP history](https://portswigger.net/burp/documentation/desktop/tools/proxy/http-history/custom-columns)
- [Orden de los contenedores PwnFox](https://github.com/yeswehack/PwnFox/blob/master/firefox/src/popup/popup.js)
- [Colores de PwnFox](https://github.com/yeswehack/PwnFox/blob/master/firefox/src/popup/colors.css)
