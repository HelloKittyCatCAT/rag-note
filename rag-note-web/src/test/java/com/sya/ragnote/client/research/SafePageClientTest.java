package com.sya.ragnote.client.research;

import com.sya.ragnote.exception.ServiceSystemException;
import java.net.InetAddress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafePageClientTest {
    @Test
    void rejectsLocalAndReservedAddresses() throws Exception {
        for (String address : new String[]{"127.0.0.1", "10.1.2.3", "169.254.169.254",
                "100.64.1.1", "192.0.2.1", "198.18.0.1", "::1", "::ffff:127.0.0.1"}) {
            assertFalse(SafePageClient.publicAddress(InetAddress.getByName(address)), address);
        }
        assertThrows(ServiceSystemException.class, () ->
                new SafePageClient().read("http://127.0.0.1", Long.MAX_VALUE));
    }
}
