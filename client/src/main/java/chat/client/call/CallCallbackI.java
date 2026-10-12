package chat.client.call;

import CallApp.CallCallback;
import CallApp.Endpoint;
import com.zeroc.Ice.Current;


public class CallCallbackI implements CallCallback {

    private final CallManager manager;

    public CallCallbackI(CallManager manager) {
        this.manager = manager;
    }

    @Override
    public void onIncomingCall(String callId, String caller, Endpoint callerUdp, Current current) {
        manager.onIncomingCall(callId, caller, callerUdp);
    }

    @Override
    public void onCallAccepted(String callId, String callee, Endpoint calleeUdp, Current current) {
        manager.onCallAccepted(callId, callee, calleeUdp);
    }

    @Override
    public void onCallRejected(String callId, String callee, String reason, Current current) {
        manager.onCallRejected(callId, callee, reason);
    }

    @Override
    public void onCallEnded(String callId, String by, String reason, Current current) {
        manager.onCallEnded(callId, by, reason);
    }

    @Override
    public void onGroupCallEvent(String room, String nickname, boolean joined, Current current) {
        manager.onGroupCallEvent(room, nickname, joined);
    }
}