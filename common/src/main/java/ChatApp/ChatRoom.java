package ChatApp;

public interface ChatRoom extends com.zeroc.Ice.Object
{
    void login(String nickname, ClientCallbackPrx callback, com.zeroc.Ice.Current current)
        throws ChatException;

    void postMessage(String nickname, String message, com.zeroc.Ice.Current current)
        throws ChatException;

    ChatMessage[] getPendingMessages(String nickname, long lastMessageId, com.zeroc.Ice.Current current);

    String[] getOnlineUsers(com.zeroc.Ice.Current current);

    void logout(String nickname, com.zeroc.Ice.Current current);

    DeliveryReceipt sendPrivateMessage(String sender, String recipient, String text, com.zeroc.Ice.Current current)
        throws ChatException;

    void createRoom(String nickname, String room, com.zeroc.Ice.Current current)
        throws ChatException;

    RoomInfo[] listRooms(String nickname, com.zeroc.Ice.Current current)
        throws ChatException;

    String[] joinRoom(String nickname, String room, com.zeroc.Ice.Current current)
        throws ChatException;

    void leaveRoom(String nickname, String room, com.zeroc.Ice.Current current)
        throws ChatException;

    String[] getRoomMembers(String nickname, String room, com.zeroc.Ice.Current current)
        throws ChatException;

    long sendRoomMessage(String sender, String room, String text, com.zeroc.Ice.Current current)
        throws ChatException;

    static final String[] _iceIds =
    {
        "::ChatApp::ChatRoom",
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
        return "::ChatApp::ChatRoom";
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_login(ChatRoom obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
        throws com.zeroc.Ice.UserException
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_nickname;
        ClientCallbackPrx iceP_callback;
        iceP_nickname = istr.readString();
        iceP_callback = ClientCallbackPrx.uncheckedCast(istr.readProxy());
        inS.endReadParams();
        obj.login(iceP_nickname, iceP_callback, current);
        return inS.setResult(inS.writeEmptyParams());
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_postMessage(ChatRoom obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
        throws com.zeroc.Ice.UserException
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_nickname;
        String iceP_message;
        iceP_nickname = istr.readString();
        iceP_message = istr.readString();
        inS.endReadParams();
        obj.postMessage(iceP_nickname, iceP_message, current);
        return inS.setResult(inS.writeEmptyParams());
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_getPendingMessages(ChatRoom obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(com.zeroc.Ice.OperationMode.Idempotent, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_nickname;
        long iceP_lastMessageId;
        iceP_nickname = istr.readString();
        iceP_lastMessageId = istr.readLong();
        inS.endReadParams();
        ChatMessage[] ret = obj.getPendingMessages(iceP_nickname, iceP_lastMessageId, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        MessageSeqHelper.write(ostr, ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_getOnlineUsers(ChatRoom obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(com.zeroc.Ice.OperationMode.Idempotent, current.mode);
        inS.readEmptyParams();
        String[] ret = obj.getOnlineUsers(current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeStringSeq(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_logout(ChatRoom obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_nickname;
        iceP_nickname = istr.readString();
        inS.endReadParams();
        obj.logout(iceP_nickname, current);
        return inS.setResult(inS.writeEmptyParams());
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_sendPrivateMessage(ChatRoom obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
        throws com.zeroc.Ice.UserException
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_sender;
        String iceP_recipient;
        String iceP_text;
        iceP_sender = istr.readString();
        iceP_recipient = istr.readString();
        iceP_text = istr.readString();
        inS.endReadParams();
        DeliveryReceipt ret = obj.sendPrivateMessage(iceP_sender, iceP_recipient, iceP_text, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        DeliveryReceipt.ice_write(ostr, ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_createRoom(ChatRoom obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
        throws com.zeroc.Ice.UserException
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_nickname;
        String iceP_room;
        iceP_nickname = istr.readString();
        iceP_room = istr.readString();
        inS.endReadParams();
        obj.createRoom(iceP_nickname, iceP_room, current);
        return inS.setResult(inS.writeEmptyParams());
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_listRooms(ChatRoom obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
        throws com.zeroc.Ice.UserException
    {
        com.zeroc.Ice.Object._iceCheckMode(com.zeroc.Ice.OperationMode.Idempotent, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_nickname;
        iceP_nickname = istr.readString();
        inS.endReadParams();
        RoomInfo[] ret = obj.listRooms(iceP_nickname, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        RoomSeqHelper.write(ostr, ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_joinRoom(ChatRoom obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
        throws com.zeroc.Ice.UserException
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_nickname;
        String iceP_room;
        iceP_nickname = istr.readString();
        iceP_room = istr.readString();
        inS.endReadParams();
        String[] ret = obj.joinRoom(iceP_nickname, iceP_room, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeStringSeq(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_leaveRoom(ChatRoom obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
        throws com.zeroc.Ice.UserException
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_nickname;
        String iceP_room;
        iceP_nickname = istr.readString();
        iceP_room = istr.readString();
        inS.endReadParams();
        obj.leaveRoom(iceP_nickname, iceP_room, current);
        return inS.setResult(inS.writeEmptyParams());
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_getRoomMembers(ChatRoom obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
        throws com.zeroc.Ice.UserException
    {
        com.zeroc.Ice.Object._iceCheckMode(com.zeroc.Ice.OperationMode.Idempotent, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_nickname;
        String iceP_room;
        iceP_nickname = istr.readString();
        iceP_room = istr.readString();
        inS.endReadParams();
        String[] ret = obj.getRoomMembers(iceP_nickname, iceP_room, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeStringSeq(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_sendRoomMessage(ChatRoom obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
        throws com.zeroc.Ice.UserException
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_sender;
        String iceP_room;
        String iceP_text;
        iceP_sender = istr.readString();
        iceP_room = istr.readString();
        iceP_text = istr.readString();
        inS.endReadParams();
        long ret = obj.sendRoomMessage(iceP_sender, iceP_room, iceP_text, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeLong(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    final static String[] _iceOps =
    {
        "createRoom",
        "getOnlineUsers",
        "getPendingMessages",
        "getRoomMembers",
        "ice_id",
        "ice_ids",
        "ice_isA",
        "ice_ping",
        "joinRoom",
        "leaveRoom",
        "listRooms",
        "login",
        "logout",
        "postMessage",
        "sendPrivateMessage",
        "sendRoomMessage"
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
                return _iceD_createRoom(this, in, current);
            }
            case 1:
            {
                return _iceD_getOnlineUsers(this, in, current);
            }
            case 2:
            {
                return _iceD_getPendingMessages(this, in, current);
            }
            case 3:
            {
                return _iceD_getRoomMembers(this, in, current);
            }
            case 4:
            {
                return com.zeroc.Ice.Object._iceD_ice_id(this, in, current);
            }
            case 5:
            {
                return com.zeroc.Ice.Object._iceD_ice_ids(this, in, current);
            }
            case 6:
            {
                return com.zeroc.Ice.Object._iceD_ice_isA(this, in, current);
            }
            case 7:
            {
                return com.zeroc.Ice.Object._iceD_ice_ping(this, in, current);
            }
            case 8:
            {
                return _iceD_joinRoom(this, in, current);
            }
            case 9:
            {
                return _iceD_leaveRoom(this, in, current);
            }
            case 10:
            {
                return _iceD_listRooms(this, in, current);
            }
            case 11:
            {
                return _iceD_login(this, in, current);
            }
            case 12:
            {
                return _iceD_logout(this, in, current);
            }
            case 13:
            {
                return _iceD_postMessage(this, in, current);
            }
            case 14:
            {
                return _iceD_sendPrivateMessage(this, in, current);
            }
            case 15:
            {
                return _iceD_sendRoomMessage(this, in, current);
            }
        }

        assert(false);
        throw new com.zeroc.Ice.OperationNotExistException(current.id, current.facet, current.operation);
    }
}
