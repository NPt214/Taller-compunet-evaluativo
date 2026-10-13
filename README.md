# TallerV2

# Integrantes 
- Anna Sophia A00411470

- Juan Camilo Borrero A00430714
  
- Andres Martinez A00411070

## Estructura: dos funcionalidades dentro del mismo proyecto

Se mantiene la estructura `common / server / client`. `common` y `server` tienen una carpeta interna por funcionalidad; el cliente es una sola clase.

| | CHATROOM | 1 A 1 |
|---|---|---|
| Slice | `common/src/main/slice/chatroom/Chat.ice` (incluye archivos) | `common/src/main/slice/privatechat/PrivateChat.ice` (nuevo) |
| Modulo Slice / stubs | `ChatApp` | `PrivateChatApp` |
| Servidor | `server/.../chat/server/chatroom/` (`ChatRoomI`) | `server/.../chat/server/privatechat/` (`PrivateChatI`, `PrivateSession`) |
| Identidad Ice | `ChatService` | `PrivateChatService` |

`ServerMain` (en `chat/server/`) es compartido: un solo adaptador en el puerto 10000 que registra los dos servicios.

`ClientMain` (en `chat/client/`) es un unico cliente para las dos funcionalidades, porque se usan desde la misma terminal. Con un solo nickname hace login en los dos servicios y, dentro de la clase, las partes estan marcadas: hilo de polling y comandos del chatroom, y `PrivateCallbackI` + `/msg` para el 1 a 1.

`PrivateChat.ice` es independiente (no hace `#include` de `Chat.ice`) y define solo lo que necesita el 1 a 1:
`PrivateMessage`, `DeliveryReceipt`, `PrivateChatException` (+ `NicknameInUseException`, `UserNotFoundException`, `UserOfflineException`), `PrivateCallback` y la interfaz `PrivateChat` (`login`, `logout`, `getOnlineUsers`, `sendPrivateMessage`).

## Transferencia de archivos (RF-04)

- Contrato: dentro de `Chat.ice` (modulo `ChatApp`). Se agregaron `ByteSeq`, el struct `FileInfo`, el callback `FileCallback` y seis operaciones en `ChatRoom`. No hay excepciones nuevas: todo usa `ChatException`, y los demas errores (ruta invalida, archivo inexistente, red) se manejan en `ClientMain`.
- Servidor: `ChatRoomI` delega las operaciones a `chat/server/chatroom/FileTransfer.java`.
- Funciona por sesiones:
  1. Despues del login, el cliente registra su callback con `registerFileCallback`.
  2. El emisor llama `startUpload(FileInfo)` con los metadatos (nombre, tamano total, tamano de fragmento, total de fragmentos y SHA-256) y recibe un id de subida.
  3. Envia los fragmentos de 64 KB en orden con `writeChunk(id, indice, bytes)` y termina con `closeUpload(id)`.
  4. El servidor guarda el archivo en una carpeta temporal, verifica tamano y SHA-256 y avisa a los destinatarios con `onFileAvailable` (al usuario, o a los miembros de la sala menos el emisor).
  5. Cada receptor descarga con `readChunk(...)` en un hilo aparte, verifica el SHA-256, lo guarda en `descargas/<nickname>/` y llama `closeDownload`. Cuando todos terminan, el servidor borra el temporal.
- Como el archivo se sube una sola vez, en una sala cada miembro lo descarga a su ritmo y un cliente lento no frena al emisor.
- `ClientMain` valida la ruta antes de enviar (que exista, que no sea carpeta, permisos, maximo 200 MB) y el envio corre en un hilo aparte para no bloquear la consola.
- Rutas con espacios: escribirlas entre comillas, por ejemplo `/sendfile ana "C:/mis fotos/foto.png"`.

## Generacion de stubs

`compileSlice` (en `common/build.gradle`) genera con `slice2java` los archivos `chatroom/Chat.ice`, `privatechat/PrivateChat.ice` y `call/Call.ice`.

## Ejecucion

Requisitos: JDK 17+, `slice2java` (ZeroC Ice 3.7) en el PATH.

```bash
# Servidor (puerto TCP 10000, ambos servicios)
./gradlew :server:run

# Cliente (uno por terminal). Argumentos opcionales: host y puerto
./gradlew :client:run -q --console=plain
./gradlew :client:run -q --console=plain --args="192.168.1.10 10000"
```

## Comandos del cliente

| Comando | Descripcion |
|---|---|
| `/rooms` | Lista las salas (chatroom) |
| `/create <sala>` | Crea una sala (chatroom) |
| `/join <sala>` | Entra a una sala (chatroom) |
| `/leave <sala>` | Sale de una sala (chatroom) |
| `/say <sala> <mensaje>` | Envia un mensaje a una sala (chatroom) |
| `/msg <nickname> <mensaje>` | Mensaje privado (1 a 1) |
| `/sendfile <nickname> <ruta>` | Envia un archivo a un usuario |
| `/sendfile #<sala> <ruta>` | Envia un archivo a todos los miembros de una sala |
| `/users` | Lista los usuarios conectados |
| `/help` | Muestra la ayuda |
| `/exit` | Cierra sesion |
