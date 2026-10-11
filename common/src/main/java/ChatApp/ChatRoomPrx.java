package ChatApp;

public interface ChatRoomPrx extends com.zeroc.Ice.ObjectPrx
{
    default void login(String nickname, ClientCallbackPrx callback)
        throws ChatException
    {
        login(nickname, callback, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default void login(String nickname, ClientCallbackPrx callback, java.util.Map<String, String> context)
        throws ChatException
    {
        try
        {
            _iceI_loginAsync(nickname, callback, context, true).waitForResponseOrUserEx();
        }
        catch(ChatException ex)
        {
            throw ex;
        }
        catch(com.zeroc.Ice.UserException ex)
        {
            throw new com.zeroc.Ice.UnknownUserException(ex.ice_id(), ex);
        }
    }

    default java.util.concurrent.CompletableFuture<Void> loginAsync(String nickname, ClientCallbackPrx callback)
    {
        return _iceI_loginAsync(nickname, callback, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<Void> loginAsync(String nickname, ClientCallbackPrx callback, java.util.Map<String, String> context)
    {
        return _iceI_loginAsync(nickname, callback, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<Void> _iceI_loginAsync(String iceP_nickname, ClientCallbackPrx iceP_callback, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<Void> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "login", null, sync, _iceE_login);
        f.invoke(true, context, null, ostr -> {
                     ostr.writeString(iceP_nickname);
                     ostr.writeProxy(iceP_callback);
                 }, null);
        return f;
    }

    static final Class<?>[] _iceE_login =
    {
        ChatException.class
    };

    default void postMessage(String nickname, String message)
        throws ChatException
    {
        postMessage(nickname, message, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default void postMessage(String nickname, String message, java.util.Map<String, String> context)
        throws ChatException
    {
        try
        {
            _iceI_postMessageAsync(nickname, message, context, true).waitForResponseOrUserEx();
        }
        catch(ChatException ex)
        {
            throw ex;
        }
        catch(com.zeroc.Ice.UserException ex)
        {
            throw new com.zeroc.Ice.UnknownUserException(ex.ice_id(), ex);
        }
    }

    default java.util.concurrent.CompletableFuture<Void> postMessageAsync(String nickname, String message)
    {
        return _iceI_postMessageAsync(nickname, message, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<Void> postMessageAsync(String nickname, String message, java.util.Map<String, String> context)
    {
        return _iceI_postMessageAsync(nickname, message, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<Void> _iceI_postMessageAsync(String iceP_nickname, String iceP_message, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<Void> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "postMessage", null, sync, _iceE_postMessage);
        f.invoke(true, context, null, ostr -> {
                     ostr.writeString(iceP_nickname);
                     ostr.writeString(iceP_message);
                 }, null);
        return f;
    }

    static final Class<?>[] _iceE_postMessage =
    {
        ChatException.class
    };

    default ChatMessage[] getPendingMessages(String nickname, long lastMessageId)
    {
        return getPendingMessages(nickname, lastMessageId, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default ChatMessage[] getPendingMessages(String nickname, long lastMessageId, java.util.Map<String, String> context)
    {
        return _iceI_getPendingMessagesAsync(nickname, lastMessageId, context, true).waitForResponse();
    }

    default java.util.concurrent.CompletableFuture<ChatMessage[]> getPendingMessagesAsync(String nickname, long lastMessageId)
    {
        return _iceI_getPendingMessagesAsync(nickname, lastMessageId, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<ChatMessage[]> getPendingMessagesAsync(String nickname, long lastMessageId, java.util.Map<String, String> context)
    {
        return _iceI_getPendingMessagesAsync(nickname, lastMessageId, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<ChatMessage[]> _iceI_getPendingMessagesAsync(String iceP_nickname, long iceP_lastMessageId, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<ChatMessage[]> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "getPendingMessages", com.zeroc.Ice.OperationMode.Idempotent, sync, null);
        f.invoke(true, context, null, ostr -> {
                     ostr.writeString(iceP_nickname);
                     ostr.writeLong(iceP_lastMessageId);
                 }, istr -> {
                     ChatMessage[] ret;
                     ret = MessageSeqHelper.read(istr);
                     return ret;
                 });
        return f;
    }

    default String[] getOnlineUsers()
    {
        return getOnlineUsers(com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default String[] getOnlineUsers(java.util.Map<String, String> context)
    {
        return _iceI_getOnlineUsersAsync(context, true).waitForResponse();
    }

    default java.util.concurrent.CompletableFuture<String[]> getOnlineUsersAsync()
    {
        return _iceI_getOnlineUsersAsync(com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<String[]> getOnlineUsersAsync(java.util.Map<String, String> context)
    {
        return _iceI_getOnlineUsersAsync(context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<String[]> _iceI_getOnlineUsersAsync(java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<String[]> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "getOnlineUsers", com.zeroc.Ice.OperationMode.Idempotent, sync, null);
        f.invoke(true, context, null, null, istr -> {
                     String[] ret;
                     ret = istr.readStringSeq();
                     return ret;
                 });
        return f;
    }

    default void logout(String nickname)
    {
        logout(nickname, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default void logout(String nickname, java.util.Map<String, String> context)
    {
        _iceI_logoutAsync(nickname, context, true).waitForResponse();
    }

    default java.util.concurrent.CompletableFuture<Void> logoutAsync(String nickname)
    {
        return _iceI_logoutAsync(nickname, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<Void> logoutAsync(String nickname, java.util.Map<String, String> context)
    {
        return _iceI_logoutAsync(nickname, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<Void> _iceI_logoutAsync(String iceP_nickname, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<Void> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "logout", null, sync, null);
        f.invoke(false, context, null, ostr -> {
                     ostr.writeString(iceP_nickname);
                 }, null);
        return f;
    }

    default DeliveryReceipt sendPrivateMessage(String sender, String recipient, String text)
        throws ChatException
    {
        return sendPrivateMessage(sender, recipient, text, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default DeliveryReceipt sendPrivateMessage(String sender, String recipient, String text, java.util.Map<String, String> context)
        throws ChatException
    {
        try
        {
            return _iceI_sendPrivateMessageAsync(sender, recipient, text, context, true).waitForResponseOrUserEx();
        }
        catch(ChatException ex)
        {
            throw ex;
        }
        catch(com.zeroc.Ice.UserException ex)
        {
            throw new com.zeroc.Ice.UnknownUserException(ex.ice_id(), ex);
        }
    }

    default java.util.concurrent.CompletableFuture<DeliveryReceipt> sendPrivateMessageAsync(String sender, String recipient, String text)
    {
        return _iceI_sendPrivateMessageAsync(sender, recipient, text, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<DeliveryReceipt> sendPrivateMessageAsync(String sender, String recipient, String text, java.util.Map<String, String> context)
    {
        return _iceI_sendPrivateMessageAsync(sender, recipient, text, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<DeliveryReceipt> _iceI_sendPrivateMessageAsync(String iceP_sender, String iceP_recipient, String iceP_text, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<DeliveryReceipt> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "sendPrivateMessage", null, sync, _iceE_sendPrivateMessage);
        f.invoke(true, context, null, ostr -> {
                     ostr.writeString(iceP_sender);
                     ostr.writeString(iceP_recipient);
                     ostr.writeString(iceP_text);
                 }, istr -> {
                     DeliveryReceipt ret;
                     ret = DeliveryReceipt.ice_read(istr);
                     return ret;
                 });
        return f;
    }

    static final Class<?>[] _iceE_sendPrivateMessage =
    {
        ChatException.class
    };

    default void createRoom(String nickname, String room)
        throws ChatException
    {
        createRoom(nickname, room, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default void createRoom(String nickname, String room, java.util.Map<String, String> context)
        throws ChatException
    {
        try
        {
            _iceI_createRoomAsync(nickname, room, context, true).waitForResponseOrUserEx();
        }
        catch(ChatException ex)
        {
            throw ex;
        }
        catch(com.zeroc.Ice.UserException ex)
        {
            throw new com.zeroc.Ice.UnknownUserException(ex.ice_id(), ex);
        }
    }

    default java.util.concurrent.CompletableFuture<Void> createRoomAsync(String nickname, String room)
    {
        return _iceI_createRoomAsync(nickname, room, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<Void> createRoomAsync(String nickname, String room, java.util.Map<String, String> context)
    {
        return _iceI_createRoomAsync(nickname, room, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<Void> _iceI_createRoomAsync(String iceP_nickname, String iceP_room, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<Void> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "createRoom", null, sync, _iceE_createRoom);
        f.invoke(true, context, null, ostr -> {
                     ostr.writeString(iceP_nickname);
                     ostr.writeString(iceP_room);
                 }, null);
        return f;
    }

    static final Class<?>[] _iceE_createRoom =
    {
        ChatException.class
    };

    default RoomInfo[] listRooms(String nickname)
        throws ChatException
    {
        return listRooms(nickname, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default RoomInfo[] listRooms(String nickname, java.util.Map<String, String> context)
        throws ChatException
    {
        try
        {
            return _iceI_listRoomsAsync(nickname, context, true).waitForResponseOrUserEx();
        }
        catch(ChatException ex)
        {
            throw ex;
        }
        catch(com.zeroc.Ice.UserException ex)
        {
            throw new com.zeroc.Ice.UnknownUserException(ex.ice_id(), ex);
        }
    }

    default java.util.concurrent.CompletableFuture<RoomInfo[]> listRoomsAsync(String nickname)
    {
        return _iceI_listRoomsAsync(nickname, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<RoomInfo[]> listRoomsAsync(String nickname, java.util.Map<String, String> context)
    {
        return _iceI_listRoomsAsync(nickname, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<RoomInfo[]> _iceI_listRoomsAsync(String iceP_nickname, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<RoomInfo[]> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "listRooms", com.zeroc.Ice.OperationMode.Idempotent, sync, _iceE_listRooms);
        f.invoke(true, context, null, ostr -> {
                     ostr.writeString(iceP_nickname);
                 }, istr -> {
                     RoomInfo[] ret;
                     ret = RoomSeqHelper.read(istr);
                     return ret;
                 });
        return f;
    }

    static final Class<?>[] _iceE_listRooms =
    {
        ChatException.class
    };

    default String[] joinRoom(String nickname, String room)
        throws ChatException
    {
        return joinRoom(nickname, room, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default String[] joinRoom(String nickname, String room, java.util.Map<String, String> context)
        throws ChatException
    {
        try
        {
            return _iceI_joinRoomAsync(nickname, room, context, true).waitForResponseOrUserEx();
        }
        catch(ChatException ex)
        {
            throw ex;
        }
        catch(com.zeroc.Ice.UserException ex)
        {
            throw new com.zeroc.Ice.UnknownUserException(ex.ice_id(), ex);
        }
    }

    default java.util.concurrent.CompletableFuture<String[]> joinRoomAsync(String nickname, String room)
    {
        return _iceI_joinRoomAsync(nickname, room, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<String[]> joinRoomAsync(String nickname, String room, java.util.Map<String, String> context)
    {
        return _iceI_joinRoomAsync(nickname, room, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<String[]> _iceI_joinRoomAsync(String iceP_nickname, String iceP_room, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<String[]> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "joinRoom", null, sync, _iceE_joinRoom);
        f.invoke(true, context, null, ostr -> {
                     ostr.writeString(iceP_nickname);
                     ostr.writeString(iceP_room);
                 }, istr -> {
                     String[] ret;
                     ret = istr.readStringSeq();
                     return ret;
                 });
        return f;
    }

    static final Class<?>[] _iceE_joinRoom =
    {
        ChatException.class
    };

    default void leaveRoom(String nickname, String room)
        throws ChatException
    {
        leaveRoom(nickname, room, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default void leaveRoom(String nickname, String room, java.util.Map<String, String> context)
        throws ChatException
    {
        try
        {
            _iceI_leaveRoomAsync(nickname, room, context, true).waitForResponseOrUserEx();
        }
        catch(ChatException ex)
        {
            throw ex;
        }
        catch(com.zeroc.Ice.UserException ex)
        {
            throw new com.zeroc.Ice.UnknownUserException(ex.ice_id(), ex);
        }
    }

    default java.util.concurrent.CompletableFuture<Void> leaveRoomAsync(String nickname, String room)
    {
        return _iceI_leaveRoomAsync(nickname, room, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<Void> leaveRoomAsync(String nickname, String room, java.util.Map<String, String> context)
    {
        return _iceI_leaveRoomAsync(nickname, room, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<Void> _iceI_leaveRoomAsync(String iceP_nickname, String iceP_room, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<Void> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "leaveRoom", null, sync, _iceE_leaveRoom);
        f.invoke(true, context, null, ostr -> {
                     ostr.writeString(iceP_nickname);
                     ostr.writeString(iceP_room);
                 }, null);
        return f;
    }

    static final Class<?>[] _iceE_leaveRoom =
    {
        ChatException.class
    };

    default String[] getRoomMembers(String nickname, String room)
        throws ChatException
    {
        return getRoomMembers(nickname, room, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default String[] getRoomMembers(String nickname, String room, java.util.Map<String, String> context)
        throws ChatException
    {
        try
        {
            return _iceI_getRoomMembersAsync(nickname, room, context, true).waitForResponseOrUserEx();
        }
        catch(ChatException ex)
        {
            throw ex;
        }
        catch(com.zeroc.Ice.UserException ex)
        {
            throw new com.zeroc.Ice.UnknownUserException(ex.ice_id(), ex);
        }
    }

    default java.util.concurrent.CompletableFuture<String[]> getRoomMembersAsync(String nickname, String room)
    {
        return _iceI_getRoomMembersAsync(nickname, room, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<String[]> getRoomMembersAsync(String nickname, String room, java.util.Map<String, String> context)
    {
        return _iceI_getRoomMembersAsync(nickname, room, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<String[]> _iceI_getRoomMembersAsync(String iceP_nickname, String iceP_room, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<String[]> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "getRoomMembers", com.zeroc.Ice.OperationMode.Idempotent, sync, _iceE_getRoomMembers);
        f.invoke(true, context, null, ostr -> {
                     ostr.writeString(iceP_nickname);
                     ostr.writeString(iceP_room);
                 }, istr -> {
                     String[] ret;
                     ret = istr.readStringSeq();
                     return ret;
                 });
        return f;
    }

    static final Class<?>[] _iceE_getRoomMembers =
    {
        ChatException.class
    };

    default long sendRoomMessage(String sender, String room, String text)
        throws ChatException
    {
        return sendRoomMessage(sender, room, text, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default long sendRoomMessage(String sender, String room, String text, java.util.Map<String, String> context)
        throws ChatException
    {
        try
        {
            return _iceI_sendRoomMessageAsync(sender, room, text, context, true).waitForResponseOrUserEx();
        }
        catch(ChatException ex)
        {
            throw ex;
        }
        catch(com.zeroc.Ice.UserException ex)
        {
            throw new com.zeroc.Ice.UnknownUserException(ex.ice_id(), ex);
        }
    }

    default java.util.concurrent.CompletableFuture<java.lang.Long> sendRoomMessageAsync(String sender, String room, String text)
    {
        return _iceI_sendRoomMessageAsync(sender, room, text, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<java.lang.Long> sendRoomMessageAsync(String sender, String room, String text, java.util.Map<String, String> context)
    {
        return _iceI_sendRoomMessageAsync(sender, room, text, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<java.lang.Long> _iceI_sendRoomMessageAsync(String iceP_sender, String iceP_room, String iceP_text, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<java.lang.Long> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "sendRoomMessage", null, sync, _iceE_sendRoomMessage);
        f.invoke(true, context, null, ostr -> {
                     ostr.writeString(iceP_sender);
                     ostr.writeString(iceP_room);
                     ostr.writeString(iceP_text);
                 }, istr -> {
                     long ret;
                     ret = istr.readLong();
                     return ret;
                 });
        return f;
    }

    static final Class<?>[] _iceE_sendRoomMessage =
    {
        ChatException.class
    };

    static ChatRoomPrx checkedCast(com.zeroc.Ice.ObjectPrx obj)
    {
        return com.zeroc.Ice.ObjectPrx._checkedCast(obj, ice_staticId(), ChatRoomPrx.class, _ChatRoomPrxI.class);
    }

    static ChatRoomPrx checkedCast(com.zeroc.Ice.ObjectPrx obj, java.util.Map<String, String> context)
    {
        return com.zeroc.Ice.ObjectPrx._checkedCast(obj, context, ice_staticId(), ChatRoomPrx.class, _ChatRoomPrxI.class);
    }

    static ChatRoomPrx checkedCast(com.zeroc.Ice.ObjectPrx obj, String facet)
    {
        return com.zeroc.Ice.ObjectPrx._checkedCast(obj, facet, ice_staticId(), ChatRoomPrx.class, _ChatRoomPrxI.class);
    }

    static ChatRoomPrx checkedCast(com.zeroc.Ice.ObjectPrx obj, String facet, java.util.Map<String, String> context)
    {
        return com.zeroc.Ice.ObjectPrx._checkedCast(obj, facet, context, ice_staticId(), ChatRoomPrx.class, _ChatRoomPrxI.class);
    }

    static ChatRoomPrx uncheckedCast(com.zeroc.Ice.ObjectPrx obj)
    {
        return com.zeroc.Ice.ObjectPrx._uncheckedCast(obj, ChatRoomPrx.class, _ChatRoomPrxI.class);
    }

    static ChatRoomPrx uncheckedCast(com.zeroc.Ice.ObjectPrx obj, String facet)
    {
        return com.zeroc.Ice.ObjectPrx._uncheckedCast(obj, facet, ChatRoomPrx.class, _ChatRoomPrxI.class);
    }

    @Override
    default ChatRoomPrx ice_context(java.util.Map<String, String> newContext)
    {
        return (ChatRoomPrx)_ice_context(newContext);
    }

    @Override
    default ChatRoomPrx ice_adapterId(String newAdapterId)
    {
        return (ChatRoomPrx)_ice_adapterId(newAdapterId);
    }

    @Override
    default ChatRoomPrx ice_endpoints(com.zeroc.Ice.Endpoint[] newEndpoints)
    {
        return (ChatRoomPrx)_ice_endpoints(newEndpoints);
    }

    @Override
    default ChatRoomPrx ice_locatorCacheTimeout(int newTimeout)
    {
        return (ChatRoomPrx)_ice_locatorCacheTimeout(newTimeout);
    }

    @Override
    default ChatRoomPrx ice_invocationTimeout(int newTimeout)
    {
        return (ChatRoomPrx)_ice_invocationTimeout(newTimeout);
    }

    @Override
    default ChatRoomPrx ice_connectionCached(boolean newCache)
    {
        return (ChatRoomPrx)_ice_connectionCached(newCache);
    }

    @Override
    default ChatRoomPrx ice_endpointSelection(com.zeroc.Ice.EndpointSelectionType newType)
    {
        return (ChatRoomPrx)_ice_endpointSelection(newType);
    }

    @Override
    default ChatRoomPrx ice_secure(boolean b)
    {
        return (ChatRoomPrx)_ice_secure(b);
    }

    @Override
    default ChatRoomPrx ice_encodingVersion(com.zeroc.Ice.EncodingVersion e)
    {
        return (ChatRoomPrx)_ice_encodingVersion(e);
    }

    @Override
    default ChatRoomPrx ice_preferSecure(boolean b)
    {
        return (ChatRoomPrx)_ice_preferSecure(b);
    }

    @Override
    default ChatRoomPrx ice_router(com.zeroc.Ice.RouterPrx router)
    {
        return (ChatRoomPrx)_ice_router(router);
    }

    @Override
    default ChatRoomPrx ice_locator(com.zeroc.Ice.LocatorPrx locator)
    {
        return (ChatRoomPrx)_ice_locator(locator);
    }

    @Override
    default ChatRoomPrx ice_collocationOptimized(boolean b)
    {
        return (ChatRoomPrx)_ice_collocationOptimized(b);
    }

    @Override
    default ChatRoomPrx ice_twoway()
    {
        return (ChatRoomPrx)_ice_twoway();
    }

    @Override
    default ChatRoomPrx ice_oneway()
    {
        return (ChatRoomPrx)_ice_oneway();
    }

    @Override
    default ChatRoomPrx ice_batchOneway()
    {
        return (ChatRoomPrx)_ice_batchOneway();
    }

    @Override
    default ChatRoomPrx ice_datagram()
    {
        return (ChatRoomPrx)_ice_datagram();
    }

    @Override
    default ChatRoomPrx ice_batchDatagram()
    {
        return (ChatRoomPrx)_ice_batchDatagram();
    }

    @Override
    default ChatRoomPrx ice_compress(boolean co)
    {
        return (ChatRoomPrx)_ice_compress(co);
    }

    @Override
    default ChatRoomPrx ice_timeout(int t)
    {
        return (ChatRoomPrx)_ice_timeout(t);
    }

    @Override
    default ChatRoomPrx ice_connectionId(String connectionId)
    {
        return (ChatRoomPrx)_ice_connectionId(connectionId);
    }

    @Override
    default ChatRoomPrx ice_fixed(com.zeroc.Ice.Connection connection)
    {
        return (ChatRoomPrx)_ice_fixed(connection);
    }

    static String ice_staticId()
    {
        return "::ChatApp::ChatRoom";
    }
}
