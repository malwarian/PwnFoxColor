# PwnFoxColor para Caido

Plugin de Caido para identificar los contenedores de PwnFox por la cabecera `X-PwnFox-Color`. Colorea las entradas nuevas de HTTP History con el HEX configurado y muestra la etiqueta de cada petición en la vista **Session** del editor.

## Compilar e instalar

Requiere Node.js, pnpm y una versión de Caido compatible con el SDK 0.58.3.

```powershell
cd caido
pnpm install
pnpm typecheck
pnpm build
```

Instala `caido/dist/plugin_package.zip` desde **Plugins > Install** en Caido. La versión Burp permanece en la raíz del mismo repositorio. Los artefactos de compilación y `node_modules` están excluidos de Git.

## Configurar

La interfaz comienza en **English**. El selector **Language** de la parte superior permite elegir **Español**; el plugin guarda esa preferencia en su base local y también la usa en la vista **Session**. Cambiar el idioma no altera los colores ni las etiquetas existentes.

1. Activa **Add container header** en PwnFox. El valor de `X-PwnFox-Color` identifica el contenedor (`blue`, `cyan`, `green`, `yellow`, `orange`, `red`, `magenta` o `purple`).
2. Abre **PwnFoxColor** en la barra lateral de Caido. Ya aparecen los ocho colores PwnFox en la primera instalación. Puedes escribir un HEX, elegirlo con la paleta y cambiar la etiqueta. **Add PwnFox 8081–8088** recupera solo los colores que falten; pulsa **Save rules** para aplicar cambios a las peticiones nuevas.
3. En HTTP History abre una petición y elige la vista **Session** para ver su etiqueta. El color se aplica en la tabla de History.
4. Si no quieres transmitir `X-PwnFox-Color` al servidor de destino, pulsa **Install header removal rule**. Crea una regla de Match & Replace para fuentes **Intercept** y **Replay**. Envía una petición de prueba a un destino controlado y comprueba el color en History y la ausencia de la cabecera en el destino.

La columna «Puerto orientativo» del plugin sirve para reconocer la correspondencia con PwnFox; el plugin **no puede leer el puerto local** por el que entró cada petición. El puerto obtenido de la petición por el SDK es el del servidor de destino. Los puertos de escucha se configuran al arrancar Caido. Por ejemplo:

```powershell
caido-cli --ui-listen 127.0.0.1:8080 --proxy-listen 127.0.0.1:8081 --proxy-listen 127.0.0.1:8082 --proxy-listen 127.0.0.1:8083 --proxy-listen 127.0.0.1:8084 --proxy-listen 127.0.0.1:8085 --proxy-listen 127.0.0.1:8086 --proxy-listen 127.0.0.1:8087 --proxy-listen 127.0.0.1:8088
```

Comprueba que esos puertos estén disponibles. El plugin no crea listeners en ejecución.

Caido tampoco ofrece una API para añadir una columna arbitraria a la tabla de HTTP History. La etiqueta se muestra en la vista **Session** dentro de la petición abierta.

## Datos y privacidad

La configuración y la asociación entre ID de petición y etiqueta se guardan en la base SQLite local del plugin dentro de Caido Data. No se guardan cuerpos HTTP, cookies ni credenciales. El plugin no realiza peticiones de red propias; usa la API GraphQL interna de Caido para asignar el color. La regla de Match & Replace solo se crea cuando pulsas el botón. Si la activas, verifica el orden de procesamiento en tu versión de Caido: el plugin debe poder leer la cabecera antes de que la regla la retire.

Si ya tienes otro plugin o workflow que colorea PwnFox, desactívalo para evitar que ambos asignen colores distintos a las mismas peticiones.

## Referencias

- [SDK de eventos de Caido](https://developer.caido.io/plugins/reference/sdks/backend/events.html)
- [Metadatos de petición mediante GraphQL](https://developer.caido.io/plugins/reference/sdks/backend/graphql.html)
- [Vista personalizada de petición](https://developer.caido.io/plugins/guides/view_modes)
- [Match & Replace y fuentes de tráfico](https://docs.caido.io/app/guides/match_replace_sources)
- [Puertos de escucha de Caido](https://docs.caido.io/app/guides/listening_ports)
