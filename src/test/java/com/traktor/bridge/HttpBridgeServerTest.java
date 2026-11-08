package com.traktor.bridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class HttpBridgeServerTest {
    @Test
    void filtersOnlyElapsedTimeUpdates() {
        assertTrue(HttpBridgeServer.isElapsedTimeOnly(new JSONObject("{\"elapsedTime\":368.13}")));
        assertFalse(HttpBridgeServer.isElapsedTimeOnly(new JSONObject("{\"elapsedTime\":368.13,\"key\":\"1m\"}")));
        assertFalse(HttpBridgeServer.isElapsedTimeOnly(new JSONObject("{\"key\":\"1m\"}")));
        assertFalse(HttpBridgeServer.isElapsedTimeOnly(new JSONObject("{}")));
    }
}
