package ChatApp;

public class NotRoomMemberException extends ChatException
{
    public NotRoomMemberException()
    {
        super();
        this.room = "";
    }

    public NotRoomMemberException(Throwable cause)
    {
        super(cause);
        this.room = "";
    }

    public NotRoomMemberException(String reason, String room)
    {
        super(reason);
        this.room = room;
    }

    public NotRoomMemberException(String reason, String room, Throwable cause)
    {
        super(reason, cause);
        this.room = room;
    }

    public String ice_id()
    {
        return "::ChatApp::NotRoomMemberException";
    }

    public String room;

    @Override
    protected void _writeImpl(com.zeroc.Ice.OutputStream ostr_)
    {
        ostr_.startSlice("::ChatApp::NotRoomMemberException", -1, false);
        ostr_.writeString(room);
        ostr_.endSlice();
        super._writeImpl(ostr_);
    }

    @Override
    protected void _readImpl(com.zeroc.Ice.InputStream istr_)
    {
        istr_.startSlice();
        room = istr_.readString();
        istr_.endSlice();
        super._readImpl(istr_);
    }

    public static final long serialVersionUID = -2780606323762310079L;
}
