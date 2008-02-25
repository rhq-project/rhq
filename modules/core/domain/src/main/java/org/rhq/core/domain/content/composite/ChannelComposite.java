package org.rhq.core.domain.content.composite;

import org.rhq.core.domain.content.Channel;

public class ChannelComposite {

    private Channel channel;
    private long packageVersionCount;

    public ChannelComposite(Channel channel, long packageVersionCount) {
        super();
        this.channel = channel;
        this.packageVersionCount = packageVersionCount;
    }

    public Channel getChannel() {
        return channel;
    }

    public long getPackageVersionCount() {
        return packageVersionCount;
    }
}
