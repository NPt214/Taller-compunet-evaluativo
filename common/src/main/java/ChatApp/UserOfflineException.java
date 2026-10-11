package ChatApp;

public class UserOfflineException extends ChatException
{
    public UserOfflineException()
    {
        super();
        this.nickname = "";
    }

    public UserOfflineException(Throwable cause)
    {
        super(cause);
        this.nickname = "";
    }

    public UserOfflineException(String reason, String nickname)
    {
        super(reason);
        this.nickname = nickname;
    }

    public UserOfflineException(String reason, String nickname, Throwable cause)
    {
        super(reason, cause);
        this.nickname = nickname;
    }

    public String ice_id()
    {
        return "::ChatApp::UserOfflineException";
    }

    public String nickname;

    @Override
    protected void _writeImpl(com.zeroc.Ice.OutputStream ostr_)
    {
        ostr_.startSlice("::ChatApp::UserOfflineException", -1, false);
        ostr_.writeString(nickname);
        ostr_.endSlice();
        super._writeImpl(ostr_);
    }

    @Override
    protected void _readImpl(com.zeroc.Ice.InputStream istr_)
    {
        istr_.startSlice();
        nickname = istr_.readString();
        istr_.endSlice();
        super._readImpl(istr_);
    }

    public static final long serialVersionUID = -6374899441791948804L;
}
