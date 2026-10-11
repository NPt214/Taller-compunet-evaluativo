# TallerV2

# Integrantes 
- Anna Sophia A00411470

- Juan Camilo Borrero A00430714
  
- Andres Martinez A00411070

## Ejecucion

Requisitos: JDK 17+, `slice2java` (ZeroC Ice 3.7) en el PATH.

```bash
# Servidor (puerto TCP 10000)
./gradlew :server:run

# Cliente (uno por terminal). Argumentos opcionales: host y puerto del servidor
./gradlew :client:run -q --console=plain
./gradlew :client:run -q --console=plain --args="192.168.1.10 10000"
```

## Comandos del cliente (CLI)

| Comando | Descripcion |
|---|---|
| `/msg <nickname> <texto>` | Mensaje privado 1 a 1 (RF-02) |
| `/create <sala>` | Crea una sala; quedas unido y pasa a ser la sala activa (RF-03) |
| `/rooms` | Catalogo de salas (`*` = eres miembro) (RF-03) |
| `/join <sala>` | Te une a una sala; puedes estar en varias a la vez (RF-03) |
| `/leave [sala]` | Sales de la sala indicada o de la activa (RF-03) |
| `/use <sala>` | Cambia la sala activa (RF-03) |
| `/room <sala> <texto>` | Mensaje a una sala concreta (RF-03) |
| `<texto>` (sin `/`) | Mensaje a la sala activa (RF-03) |
| `/members [sala]` | Miembros de la sala indicada o de la activa (RF-03) |
| `/users` | Usuarios conectados (RF-01) |
| `/login <nickname>` | Inicia sesion si no hay una activa (RF-01) |
| `/logout` | Cierra la sesion actual (RF-01) |
| `/quit` | Cierra sesion y sale |
| `/help` | Ayuda |

## RF-01: Sesion, presencia y callbacks

- `login(nickname, ClientCallback*)`: el cliente crea su propio `ObjectAdapter` (`CallbackAdapter`), registra el servant `ClientCallbackI` y envia el proxy al servidor. Si el nickname ya esta en uso se lanza `NicknameInUseException` (extiende `ChatException`).
- Presencia: el servidor notifica `onUserConnected` / `onUserDisconnected` a todos los demas clientes con invocaciones asincronas (AMI).
- Logout limpio: `logout` elimina la sesion y el proxy de callback del mapa `ConcurrentHashMap`.
- Caidas abruptas: el cliente envia heartbeats ACM cada pocos segundos; si el servidor no recibe trafico en 10 s, o la conexion TCP se cierra, se ejecuta el `CloseCallback` de la conexion y la sesion se elimina. Si un callback no responde (timeout 5 s) el cliente tambien se retira.

## RF-02: Mensajeria privada (1 a 1)

- Toda la definicion Slice esta en `Chat.ice` (seccion RF-02), reutilizando el `ClientCallback` registrado en RF-01.
- `sendPrivateMessage(sender, recipient, text)` devuelve un `DeliveryReceipt` (confirmacion de que el servidor recibio y despacho el mensaje) o lanza `UserNotFoundException` (el usuario nunca existio), `UserOfflineException` (se desconecto) o `ChatException` (sin sesion, mensaje vacio, envio a si mismo).
- El servidor localiza el callback del destinatario y le entrega `onPrivateMessage` de forma asincrona; si la entrega falla despues, el remitente recibe `onPrivateMessageFailed`.
- El cliente invoca `sendPrivateMessageAsync`, por lo que la consola nunca se bloquea esperando la respuesta.

## RF-03: Grupos / salas de chat (canales)

- Toda la definicion Slice esta en `Chat.ice` (seccion RF-03): `RoomMessage`, `RoomInfo`, `RoomSeq` y las excepciones `RoomAlreadyExistsException`, `RoomNotFoundException` y `NotRoomMemberException` (todas extienden `ChatException`). Se reutiliza el `ClientCallback` de RF-01 con `onRoomCreated`, `onRoomMessage`, `onRoomMemberJoined` y `onRoomMemberLeft`.
- Creacion dinamica: `createRoom` usa `putIfAbsent` sobre un `ConcurrentHashMap` (clave en minusculas), asi el nombre es unico aunque dos usuarios creen la misma sala a la vez. El creador queda unido y todos los conectados reciben `onRoomCreated`.
- Afiliacion: `listRooms`, `joinRoom`, `leaveRoom` y `getRoomMembers`. Cada `Room` guarda sus miembros en un set concurrente y un usuario puede estar en varias salas.
- Difusion: `sendRoomMessage` envia `onRoomMessage` de forma asincrona (AMI) solo a los miembros de esa sala con sesion activa, nunca al emisor. El mensaje lleva remitente y sala. El emisor recibe el id como confirmacion y el cliente muestra el eco local.
- Aislamiento: solo los miembros pueden publicar o ver los miembros de una sala (`NotRoomMemberException`); los demas usuarios nunca reciben esos mensajes.
- Salida de usuarios: al hacer logout o perder la conexion, el servidor retira al usuario de todas sus salas y avisa a los miembros restantes con `onRoomMemberLeft`.

