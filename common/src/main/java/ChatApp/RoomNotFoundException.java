package ChatApp;

public class RoomNotFoundException extends ChatException
{
    public RoomNotFoundException()
    {
        super();
        this.room = "";
    }

    public RoomNotFoundException(Throwable cause)
    {
        super(cause);
        this.room = "";
    }

    public RoomNotFoundException(String reason, String room)
    {
        super(reason);
        this.room = room;
    }

    public RoomNotFoundException(String reason, String room, Throwable cause)
    {
        super(reason, cause);
        this.room = room;
    }

    public String ice_id()
    {
        return "::ChatApp::RoomNotFoundException";
    }

    public String room;

    @Override
    protected void _writeImpl(com.zeroc.Ice.OutputStream ostr_)
    {
        ostr_.startSlice("::ChatApp::RoomNotFoundException", -1, false);
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

    public static final long serialVersionUID = 6399228617857908839L;
}
