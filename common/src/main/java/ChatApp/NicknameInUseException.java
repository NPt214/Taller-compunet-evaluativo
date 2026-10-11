package ChatApp;

public class NicknameInUseException extends ChatException
{
    public NicknameInUseException()
    {
        super();
        this.nickname = "";
    }

    public NicknameInUseException(Throwable cause)
    {
        super(cause);
        this.nickname = "";
    }

    public NicknameInUseException(String reason, String nickname)
    {
        super(reason);
        this.nickname = nickname;
    }

    public NicknameInUseException(String reason, String nickname, Throwable cause)
    {
        super(reason, cause);
        this.nickname = nickname;
    }

    public String ice_id()
    {
        return "::ChatApp::NicknameInUseException";
    }

    public String nickname;

    @Override
    protected void _writeImpl(com.zeroc.Ice.OutputStream ostr_)
    {
        ostr_.startSlice("::ChatApp::NicknameInUseException", -1, false);
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

    public static final long serialVersionUID = -8506795793508457664L;
}
