/*
 * Copyright 2013 Basho Technologies Inc. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.basho.riak.client.core;

import com.basho.riak.client.core.fixture.NetworkTestFixture;
import io.netty.channel.Channel;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;


/**
 *
 * @author Brian Roach <roach at basho dot com>
 */
public class ConnectionPoolFixtureTest extends FixtureTest
{
    
    @Test
    public void closedConnectionsTriggerHealthCheck() throws UnknownHostException, InterruptedException, Exception
    {
        ConnectionPool pool = PowerMockito.spy(new ConnectionPool.Builder(Protocol.HTTP)
                               .withPort(NetworkTestFixture.ACCEPT_THEN_CLOSE)
                               .withMinConnections(10)
                               .build());
        pool.start();
        Thread.sleep(3000);
        pool.shutdown();
        
        PowerMockito.verifyPrivate(pool).invoke("checkHealth", new Object[0]);
        
    }
    
    @Test
    public void idleConnectionsAreRemoved() throws UnknownHostException, InterruptedException
    {
        ConnectionPool pool = PowerMockito.spy(new ConnectionPool.Builder(Protocol.HTTP)
                               .withPort(NetworkTestFixture.PB_FULL_WRITE_STAY_OPEN)
                               .withMinConnections(10)
                               .withIdleTimeout(1000)
                               .build());
        
        pool.start();
        List<Channel> channelList = new LinkedList<Channel>();
        for (int i = 0; i < 12; i++)
        {
            channelList.add(pool.getConnection());
        }
        
        for (Channel c : channelList)
        {
            pool.returnConnection(c);
        }
        
        LinkedBlockingDeque<?> available = Whitebox.getInternalState(pool, "available");
        assertEquals(available.size(), 12);
        
        Thread.sleep(10000);
        
        assertEquals(available.size(), 10);
        
        pool.shutdown();
        
    }
    
}
