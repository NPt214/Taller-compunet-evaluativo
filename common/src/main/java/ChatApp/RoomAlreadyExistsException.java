package ChatApp;

public class RoomAlreadyExistsException extends ChatException
{
    public RoomAlreadyExistsException()
    {
        super();
        this.room = "";
    }

    public RoomAlreadyExistsException(Throwable cause)
    {
        super(cause);
        this.room = "";
    }

    public RoomAlreadyExistsException(String reason, String room)
    {
        super(reason);
        this.room = room;
    }

    public RoomAlreadyExistsException(String reason, String room, Throwable cause)
    {
        super(reason, cause);
        this.room = room;
    }

    public String ice_id()
    {
        return "::ChatApp::RoomAlreadyExistsException";
    }

    public String room;

    @Override
    protected void _writeImpl(com.zeroc.Ice.OutputStream ostr_)
    {
        ostr_.startSlice("::ChatApp::RoomAlreadyExistsException", -1, false);
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

    public static final long serialVersionUID = -7419238641034192798L;
}
