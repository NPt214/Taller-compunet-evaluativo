package ChatApp;

public interface ClientCallback extends com.zeroc.Ice.Object
{
    void onUserConnected(String nickname, com.zeroc.Ice.Current current);

    void onUserDisconnected(String nickname, com.zeroc.Ice.Current current);

    void onPrivateMessage(PrivateMessage message, com.zeroc.Ice.Current current);

    void onPrivateMessageFailed(long messageId, String recipient, String reason, com.zeroc.Ice.Current current);

    void onRoomCreated(String room, String owner, com.zeroc.Ice.Current current);

    void onRoomMessage(RoomMessage message, com.zeroc.Ice.Current current);

    void onRoomMemberJoined(String room, String nickname, com.zeroc.Ice.Current current);

    void onRoomMemberLeft(String room, String nickname, com.zeroc.Ice.Current current);

    static final String[] _iceIds =
    {
        "::ChatApp::ClientCallback",
        "::Ice::Object"
    };

    @Override
    default String[] ice_ids(com.zeroc.Ice.Current current)
    {
        return _iceIds;
    }

    @Override
    default String ice_id(com.zeroc.Ice.Current current)
    {
        return ice_staticId();
    }

    static String ice_staticId()
    {
        return "::ChatApp::ClientCallback";
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_onUserConnected(ClientCallback obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_nickname;
        iceP_nickname = istr.readString();
        inS.endReadParams();
        obj.onUserConnected(iceP_nickname, current);
        return inS.setResult(inS.writeEmptyParams());
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_onUserDisconnected(ClientCallback obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_nickname;
        iceP_nickname = istr.readString();
        inS.endReadParams();
        obj.onUserDisconnected(iceP_nickname, current);
        return inS.setResult(inS.writeEmptyParams());
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_onPrivateMessage(ClientCallback obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        PrivateMessage iceP_message;
        iceP_message = PrivateMessage.ice_read(istr);
        inS.endReadParams();
        obj.onPrivateMessage(iceP_message, current);
        return inS.setResult(inS.writeEmptyParams());
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_onPrivateMessageFailed(ClientCallback obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        long iceP_messageId;
        String iceP_recipient;
        String iceP_reason;
        iceP_messageId = istr.readLong();
        iceP_recipient = istr.readString();
        iceP_reason = istr.readString();
        inS.endReadParams();
        obj.onPrivateMessageFailed(iceP_messageId, iceP_recipient, iceP_reason, current);
        return inS.setResult(inS.writeEmptyParams());
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_onRoomCreated(ClientCallback obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_room;
        String iceP_owner;
        iceP_room = istr.readString();
        iceP_owner = istr.readString();
        inS.endReadParams();
        obj.onRoomCreated(iceP_room, iceP_owner, current);
        return inS.setResult(inS.writeEmptyParams());
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_onRoomMessage(ClientCallback obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        RoomMessage iceP_message;
        iceP_message = RoomMessage.ice_read(istr);
        inS.endReadParams();
        obj.onRoomMessage(iceP_message, current);
        return inS.setResult(inS.writeEmptyParams());
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_onRoomMemberJoined(ClientCallback obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_room;
        String iceP_nickname;
        iceP_room = istr.readString();
        iceP_nickname = istr.readString();
        inS.endReadParams();
        obj.onRoomMemberJoined(iceP_room, iceP_nickname, current);
        return inS.setResult(inS.writeEmptyParams());
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_onRoomMemberLeft(ClientCallback obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_room;
        String iceP_nickname;
        iceP_room = istr.readString();
        iceP_nickname = istr.readString();
        inS.endReadParams();
        obj.onRoomMemberLeft(iceP_room, iceP_nickname, current);
        return inS.setResult(inS.writeEmptyParams());
    }

    final static String[] _iceOps =
    {
        "ice_id",
        "ice_ids",
        "ice_isA",
        "ice_ping",
        "onPrivateMessage",
        "onPrivateMessageFailed",
        "onRoomCreated",
        "onRoomMemberJoined",
        "onRoomMemberLeft",
        "onRoomMessage",
        "onUserConnected",
        "onUserDisconnected"
    };

    @Override
    default java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceDispatch(com.zeroc.IceInternal.Incoming in, com.zeroc.Ice.Current current)
        throws com.zeroc.Ice.UserException
    {
        int pos = java.util.Arrays.binarySearch(_iceOps, current.operation);
        if(pos < 0)
        {
            throw new com.zeroc.Ice.OperationNotExistException(current.id, current.facet, current.operation);
        }

        switch(pos)
        {
            case 0:
            {
                return com.zeroc.Ice.Object._iceD_ice_id(this, in, current);
            }
            case 1:
            {
                return com.zeroc.Ice.Object._iceD_ice_ids(this, in, current);
            }
            case 2:
            {
                return com.zeroc.Ice.Object._iceD_ice_isA(this, in, current);
            }
            case 3:
            {
                return com.zeroc.Ice.Object._iceD_ice_ping(this, in, current);
            }
            case 4:
            {
                return _iceD_onPrivateMessage(this, in, current);
            }
            case 5:
            {
                return _iceD_onPrivateMessageFailed(this, in, current);
            }
            case 6:
            {
                return _iceD_onRoomCreated(this, in, current);
            }
            case 7:
            {
                return _iceD_onRoomMemberJoined(this, in, current);
            }
            case 8:
            {
                return _iceD_onRoomMemberLeft(this, in, current);
            }
            case 9:
            {
                return _iceD_onRoomMessage(this, in, current);
            }
            case 10:
            {
                return _iceD_onUserConnected(this, in, current);
            }
            case 11:
            {
                return _iceD_onUserDisconnected(this, in, current);
            }
        }

        assert(false);
        throw new com.zeroc.Ice.OperationNotExistException(current.id, current.facet, current.operation);
    }
}
