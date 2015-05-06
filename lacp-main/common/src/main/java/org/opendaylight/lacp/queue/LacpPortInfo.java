package org.opendaylight.lacp.queue;

public class LacpPortInfo
{
    private long swId;
    private int portId;

    public LacpPortInfo (long switchId, int port)
    {
        this.swId = switchId;
        this.portId = port;
    }

    public long getSwitchId()
    {
        return swId;
    }
    public int getPortId()
    {
        return portId;
    }
}

