package chat.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sala / canal de chat (RF-03).
 * El conjunto de miembros es concurrente: varios hilos del servidor pueden
 * unir, retirar y difundir a la vez sin condiciones de carrera.
 */
public final class Room {

    private final String name;
    private final String owner;
    private final Set<String> members = ConcurrentHashMap.newKeySet();

    public Room(String name, String owner) {
        this.name = name;
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    /** @return true si el usuario no era miembro y quedo agregado. */
    public boolean addMember(String nickname) {
        return members.add(nickname);
    }

    /** @return true si el usuario era miembro y fue retirado. */
    public boolean removeMember(String nickname) {
        return members.remove(nickname);
    }

    public boolean isMember(String nickname) {
        return members.contains(nickname);
    }

    /** Copia ordenada de los miembros actuales. */
    public String[] membersSnapshot() {
        return members.stream().sorted().toArray(String[]::new);
    }

    public int size() {
        return members.size();
    }
}
