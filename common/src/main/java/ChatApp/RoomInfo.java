package ChatApp;

public class RoomInfo implements java.lang.Cloneable,
                                 java.io.Serializable
{
    public String name;

    public String owner;

    public int memberCount;

    public boolean joined;

    public RoomInfo()
    {
        this.name = "";
        this.owner = "";
    }

    public RoomInfo(String name, String owner, int memberCount, boolean joined)
    {
        this.name = name;
        this.owner = owner;
        this.memberCount = memberCount;
        this.joined = joined;
    }

    public boolean equals(java.lang.Object rhs)
    {
        if(this == rhs)
        {
            return true;
        }
        RoomInfo r = null;
        if(rhs instanceof RoomInfo)
        {
            r = (RoomInfo)rhs;
        }

        if(r != null)
        {
            if(this.name != r.name)
            {
                if(this.name == null || r.name == null || !this.name.equals(r.name))
                {
                    return false;
                }
            }
            if(this.owner != r.owner)
            {
                if(this.owner == null || r.owner == null || !this.owner.equals(r.owner))
                {
                    return false;
                }
            }
            if(this.memberCount != r.memberCount)
            {
                return false;
            }
            if(this.joined != r.joined)
            {
                return false;
            }

            return true;
        }

        return false;
    }

    public int hashCode()
    {
        int h_ = 5381;
        h_ = com.zeroc.IceInternal.HashUtil.hashAdd(h_, "::ChatApp::RoomInfo");
        h_ = com.zeroc.IceInternal.HashUtil.hashAdd(h_, name);
        h_ = com.zeroc.IceInternal.HashUtil.hashAdd(h_, owner);
        h_ = com.zeroc.IceInternal.HashUtil.hashAdd(h_, memberCount);
        h_ = com.zeroc.IceInternal.HashUtil.hashAdd(h_, joined);
        return h_;
    }

    public RoomInfo clone()
    {
        RoomInfo c = null;
        try
        {
            c = (RoomInfo)super.clone();
        }
        catch(CloneNotSupportedException ex)
        {
            assert false;
        }
        return c;
    }

    public void ice_writeMembers(com.zeroc.Ice.OutputStream ostr)
    {
        ostr.writeString(this.name);
        ostr.writeString(this.owner);
        ostr.writeInt(this.memberCount);
        ostr.writeBool(this.joined);
    }

    public void ice_readMembers(com.zeroc.Ice.InputStream istr)
    {
        this.name = istr.readString();
        this.owner = istr.readString();
        this.memberCount = istr.readInt();
        this.joined = istr.readBool();
    }

    static public void ice_write(com.zeroc.Ice.OutputStream ostr, RoomInfo v)
    {
        if(v == null)
        {
            _nullMarshalValue.ice_writeMembers(ostr);
        }
        else
        {
            v.ice_writeMembers(ostr);
        }
    }

    static public RoomInfo ice_read(com.zeroc.Ice.InputStream istr)
    {
        RoomInfo v = new RoomInfo();
        v.ice_readMembers(istr);
        return v;
    }

    static public void ice_write(com.zeroc.Ice.OutputStream ostr, int tag, java.util.Optional<RoomInfo> v)
    {
        if(v != null && v.isPresent())
        {
            ice_write(ostr, tag, v.get());
        }
    }

    static public void ice_write(com.zeroc.Ice.OutputStream ostr, int tag, RoomInfo v)
    {
        if(ostr.writeOptional(tag, com.zeroc.Ice.OptionalFormat.FSize))
        {
            int pos = ostr.startSize();
            ice_write(ostr, v);
            ostr.endSize(pos);
        }
    }

    static public java.util.Optional<RoomInfo> ice_read(com.zeroc.Ice.InputStream istr, int tag)
    {
        if(istr.readOptional(tag, com.zeroc.Ice.OptionalFormat.FSize))
        {
            istr.skip(4);
            return java.util.Optional.of(RoomInfo.ice_read(istr));
        }
        else
        {
            return java.util.Optional.empty();
        }
    }

    private static final RoomInfo _nullMarshalValue = new RoomInfo();

    public static final long serialVersionUID = -3699622691378173357L;
}
