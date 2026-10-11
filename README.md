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
