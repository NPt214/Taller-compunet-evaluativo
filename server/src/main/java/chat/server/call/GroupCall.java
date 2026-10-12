package chat.server.call;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;


public final class GroupCall {

    private final String room;
    private final Set<String> members = ConcurrentHashMap.newKeySet();

    public GroupCall(String room) {
        this.room = room;
    }

    public String getRoom() {
        return room;
    }

    public boolean add(String nick) {
        return members.add(nick);
    }

    public boolean remove(String nick) {
        return members.remove(nick);
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public Set<String> snapshot() {
        return new TreeSet<>(members);
    }
}