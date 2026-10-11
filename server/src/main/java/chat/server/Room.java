package chat.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    public boolean addMember(String nickname) {
        return members.add(nickname);
    }

    public boolean removeMember(String nickname) {
        return members.remove(nickname);
    }

    public boolean isMember(String nickname) {
        return members.contains(nickname);
    }

    public String[] membersSnapshot() {
        return members.stream().sorted().toArray(String[]::new);
    }

    public int size() {
        return members.size();
    }
}
