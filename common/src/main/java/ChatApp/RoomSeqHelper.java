package ChatApp;

public final class RoomSeqHelper
{
    public static void write(com.zeroc.Ice.OutputStream ostr, RoomInfo[] v)
    {
        if(v == null)
        {
            ostr.writeSize(0);
        }
        else
        {
            ostr.writeSize(v.length);
            for(int i0 = 0; i0 < v.length; i0++)
            {
                RoomInfo.ice_write(ostr, v[i0]);
            }
        }
    }

    public static RoomInfo[] read(com.zeroc.Ice.InputStream istr)
    {
        final RoomInfo[] v;
        final int len0 = istr.readAndCheckSeqSize(7);
        v = new RoomInfo[len0];
        for(int i0 = 0; i0 < len0; i0++)
        {
            v[i0] = RoomInfo.ice_read(istr);
        }
        return v;
    }

    public static void write(com.zeroc.Ice.OutputStream ostr, int tag, java.util.Optional<RoomInfo[]> v)
    {
        if(v != null && v.isPresent())
        {
            write(ostr, tag, v.get());
        }
    }

    public static void write(com.zeroc.Ice.OutputStream ostr, int tag, RoomInfo[] v)
    {
        if(ostr.writeOptional(tag, com.zeroc.Ice.OptionalFormat.FSize))
        {
            int pos = ostr.startSize();
            RoomSeqHelper.write(ostr, v);
            ostr.endSize(pos);
        }
    }

    public static java.util.Optional<RoomInfo[]> read(com.zeroc.Ice.InputStream istr, int tag)
    {
        if(istr.readOptional(tag, com.zeroc.Ice.OptionalFormat.FSize))
        {
            istr.skip(4);
            RoomInfo[] v;
            v = RoomSeqHelper.read(istr);
            return java.util.Optional.of(v);
        }
        else
        {
            return java.util.Optional.empty();
        }
    }
}
