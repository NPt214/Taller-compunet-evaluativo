package chat.server.call;


public final class ActiveCall {

    public enum State { RINGING, ACTIVE }

    public final String id;
    public final String caller;
    public final String callee;
    private volatile State state = State.RINGING;

    public ActiveCall(String id, String caller, String callee) {
        this.id = id;
        this.caller = caller;
        this.callee = callee;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean involves(String nick) {
        return caller.equals(nick) || callee.equals(nick);
    }

    public String other(String nick) {
        return caller.equals(nick) ? callee : caller;
    }
}