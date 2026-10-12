## 1. ¿Por qué se utiliza una arquitectura híbrida TCP/UDP?

Se utiliza una arquitectura híbrida porque los diferentes tipos de información tienen necesidades distintas.

El control, el chat y la transferencia de archivos necesitan que los datos lleguen completos y en el orden correcto. Si se pierde un mensaje o un fragmento de un archivo, el resultado puede ser incorrecto. Por eso, se utiliza Ice sobre TCP, que permite una comunicación confiable y ofrece mecanismos para manejar errores y organizar las solicitudes.

En cambio, el audio necesita llegar rápidamente. Si una parte del audio tarda demasiado en llegar, ya no es útil reproducirla, porque la conversación debe continuar en tiempo real. Por esta razón, se utiliza UDP, que permite enviar audio sin esperar a que se recuperen los paquetes perdidos.

### ¿Qué ocurriría si se transmitiera la voz mediante Ice/TCP?

- **Bloqueos por pérdida de paquetes:** cuando se pierde un segmento, TCP debe recuperarlo antes de entregar los datos posteriores. Esto puede provocar silencios y hacer que el audio llegue con retraso.
- **Aumento de la latencia:** TCP intenta recuperar los datos perdidos en lugar de descartarlos. Como resultado, el audio puede acumular retraso y dejar de coincidir con la conversación.
- **Reducción de la velocidad de envío:** cuando TCP detecta pérdidas, puede disminuir la cantidad de datos que envía para evitar sobrecargar la red. Esto puede empeorar la fluidez del audio.
- **Mayor sobrecarga de comunicación:** utilizar una invocación de Ice por cada fragmento de audio genera trabajo adicional para procesar las solicitudes, preparar los datos y atender las respuestas.

Con UDP, los paquetes perdidos se descartan y la reproducción continúa con los siguientes. Esto puede generar pequeñas interrupciones, pero evita esperar por datos que ya no son útiles.

Para reducir las variaciones en el tiempo de llegada de los paquetes, se utiliza un búfer de audio (*jitter buffer*). En este proyecto, el `AudioMixer` cuenta con un margen inicial de 40 ms y un máximo de 200 ms.

## 3. ¿Qué diferencias existen entre Relay y Mesh cuando hay N participantes hablando al mismo tiempo?

Para comparar las dos arquitecturas, se supone que cada participante envía un paquete de audio cada 20 ms, es decir, 50 paquetes por segundo. También se supone que los N participantes están enviando audio al mismo tiempo.

Las siguientes cantidades corresponden a cada intervalo de 20 ms. Para calcular los paquetes por segundo, se multiplican por 50.

| Característica | Relay (utilizado) | Mesh P2P |
|---|---|---|
| Paquetes enviados por cada cliente | 1, al servidor | N − 1, uno a cada participante |
| Paquetes recibidos por cada cliente | N − 1 | N − 1 |
| Paquetes recibidos por el servidor | N | 0 |
| Paquetes enviados por el servidor | N × (N − 1) | 0 |
| Total de paquetes enviados en la red | N² | N × (N − 1) |
| Carga de subida de cada cliente | Constante | Aumenta con N |

Se estima que cada datagrama ocupa unos 690 bytes, incluyendo 640 bytes de audio PCM y las cabeceras del protocolo. Con este tamaño, un flujo consume aproximadamente 275 kb/s.

Por ejemplo, con cinco participantes, el servidor Relay recibe aproximadamente 1,4 Mb/s y envía alrededor de 5,5 Mb/s.

### Ventajas y desventajas

**Relay:** cada cliente solo necesita enviar su audio al servidor. Esto mantiene constante su consumo de subida y evita que tenga que conectarse directamente con todos los demás participantes. Sin embargo, el servidor debe reenviar los paquetes a los demás clientes, por lo que su carga aumenta a medida que crece el número de participantes. Además, si el servidor falla, la comunicación se interrumpe.

**Mesh P2P:** los participantes se envían el audio directamente entre sí, por lo que el servidor no necesita recibir ni reenviar estos paquetes. Sin embargo, cada cliente debe enviar su audio a todos los demás, lo que aumenta su consumo de ancho de banda y la complejidad de las conexiones.

Se eligió Relay porque simplifica la comunicación entre los participantes, reduce la carga de los clientes y facilita el funcionamiento cuando existen restricciones de red, como las relacionadas con NAT. Por estas razones, resulta adecuado para salas de tamaño medio en un entorno de laboratorio.

Si se necesitara admitir un número mucho mayor de participantes, se podría mejorar el servidor mezclando el audio de varias personas antes de enviarlo, mediante una arquitectura MCU, o reenviando únicamente el audio de quienes estén hablando.

## 4. ¿Cómo funcionan los callbacks y la transparencia en Ice?

### Ciclo de vida de los callbacks en el cliente

1. **Crear el `Communicator`:** permite inicializar la comunicación mediante Ice.
2. **Crear un `ObjectAdapter`:** el cliente necesita su propio adaptador para recibir solicitudes del servidor. De esta manera, también puede actuar como servidor.
3. **Registrar los servants:** se registran las implementaciones de los callbacks, como `PrivateCallbackI` y `CallCallbackI`, utilizando una identidad única, por ejemplo, un UUID. El método `adapter.add(...)` permite obtener un proxy para cada objeto.
4. **Activar el adaptador:** mediante `adapter.activate()`, el cliente queda preparado para recibir solicitudes.
5. **Enviar el proxy al servidor:** durante el inicio de sesión, el cliente ejecuta `login(nickname, callbackProxy)`. El servidor guarda el proxy y puede utilizarlo para enviar notificaciones al cliente.
6. **Cerrar la conexión:** al salir, el cliente ejecuta `logout` y cierra el `Communicator`, lo que destruye el adaptador. Si el cliente se desconecta inesperadamente, el servidor puede detectar el fallo cuando intenta ejecutar un callback y la comunicación genera una excepción, como `ConnectFailed` o `ConnectionLost`.

### Diferencias entre proxies directos e indirectos

Un **proxy directo** contiene la dirección del objeto remoto, incluyendo el host y el puerto. Esto permite que el servidor se conecte directamente al cliente para ejecutar los callbacks.

En este proyecto se utiliza un proxy directo. Por eso, es importante configurar una dirección IP que el servidor pueda alcanzar. En equipos con varias interfaces de red, se puede establecer mediante `Ice.Default.Host` o mediante la opción `-h` en los endpoints. También es necesario permitir las conexiones entrantes al puerto del adaptador a través del firewall.

Por otro lado, un **proxy indirecto** identifica el objeto y su adaptador, pero no incluye directamente la dirección de red. Por ejemplo, puede utilizar el formato `identidad@AdapterId`. Un servicio de localización, como IceGrid, permite encontrar la dirección del objeto cuando se necesita utilizarlo. Esto facilita cambiar el objeto de máquina o de puerto sin modificar todos los clientes, aunque requiere configurar IceGrid.

Otra alternativa es utilizar **conexiones bidireccionales**, mediante `connection.setAdapter(adapter)` en el cliente y `ice_fixed(connection)` en el servidor. Esta opción permite reutilizar una conexión existente para las llamadas de retorno y puede facilitar la comunicación cuando el cliente está detrás de NAT o de un firewall que bloquea conexiones entrantes.