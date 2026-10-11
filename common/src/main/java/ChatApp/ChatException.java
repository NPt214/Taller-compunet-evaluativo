package ChatApp;

public class ChatException extends com.zeroc.Ice.UserException
{
    public ChatException()
    {
        this.reason = "";
    }

    public ChatException(Throwable cause)
    {
        super(cause);
        this.reason = "";
    }

    public ChatException(String reason)
    {
        this.reason = reason;
    }

    public ChatException(String reason, Throwable cause)
    {
        super(cause);
        this.reason = reason;
    }

    public String ice_id()
    {
        return "::ChatApp::ChatException";
    }

    public String reason;

    @Override
    protected void _writeImpl(com.zeroc.Ice.OutputStream ostr_)
    {
        ostr_.startSlice("::ChatApp::ChatException", -1, true);
        ostr_.writeString(reason);
        ostr_.endSlice();
    }

    @Override
    protected void _readImpl(com.zeroc.Ice.InputStream istr_)
    {
        istr_.startSlice();
        reason = istr_.readString();
        istr_.endSlice();
    }

    public static final long serialVersionUID = -1254534298020021442L;
}
