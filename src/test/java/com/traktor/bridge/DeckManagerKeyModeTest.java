package com.traktor.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiOut;

class DeckManagerKeyModeTest {
    @Test
    void deckA12mSendsMinorValueOnDoubleAndTripleClick() {
        final List<List<Integer>> messages = new ArrayList<>();
        final List<Runnable> scheduled = new ArrayList<>();
        final ControllerHost host = (ControllerHost) Proxy.newProxyInstance(
                ControllerHost.class.getClassLoader(), new Class<?>[] {ControllerHost.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("scheduleTask") && args[0] instanceof Runnable task) {
                        assertEquals(1000L, args[1]);
                        scheduled.add(task);
                    }
                    return null;
                });
        final MidiOut midiOut = (MidiOut) Proxy.newProxyInstance(
                MidiOut.class.getClassLoader(), new Class<?>[] {MidiOut.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sendMidi")) {
                        messages.add(List.of((Integer) args[0], (Integer) args[1], (Integer) args[2]));
                    }
                    return null;
                });
        final DeckManager manager = new DeckManager(host, midiOut);

        manager.setDeckKey(0, "12m");
        manager.activateDeck(0);
        assertEquals(List.of(0xB9, 17, 10), messages.get(1));

        manager.activateDeckTripleClick(0);
        scheduled.get(0).run();
        scheduled.get(1).run();
        assertEquals(3, messages.size());
        assertEquals(List.of(0xB9, 17, 10), messages.get(2));
    }

    @Test
    void minorAndMajorDecksKeepTheirOwnKeyAndMode() {
        final List<List<Integer>> messages = new ArrayList<>();
        final ControllerHost host = (ControllerHost) Proxy.newProxyInstance(
                ControllerHost.class.getClassLoader(), new Class<?>[] {ControllerHost.class},
                (proxy, method, args) -> null);
        final MidiOut midiOut = (MidiOut) Proxy.newProxyInstance(
                MidiOut.class.getClassLoader(), new Class<?>[] {MidiOut.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sendMidi")) {
                        messages.add(List.of((Integer) args[0], (Integer) args[1], (Integer) args[2]));
                    }
                    return null;
                });
        final DeckManager manager = new DeckManager(host, midiOut);
        manager.setDeckKey(0, "2m");
        manager.setDeckKey(1, "11d");

        manager.activateDeck(0);
        manager.activateDeck(1);

        assertEquals(List.of(
                List.of(0xB9, 16, 46), List.of(0xB9, 17, 10),
                List.of(0xB9, 16, 115), List.of(0xB9, 17, 0)), messages);
    }

    @Test
    void logsExpectedKeyOnlyOnDoubleClickWithoutClaimingBitwigReceivedIt() {
        final List<String> logs = new ArrayList<>();
        final ControllerHost host = (ControllerHost) Proxy.newProxyInstance(
                ControllerHost.class.getClassLoader(), new Class<?>[] {ControllerHost.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("println")) {
                        logs.add((String) args[0]);
                    }
                    return null;
                });
        final MidiOut midiOut = (MidiOut) Proxy.newProxyInstance(
                MidiOut.class.getClassLoader(), new Class<?>[] {MidiOut.class},
                (proxy, method, args) -> null);
        final DeckManager manager = new DeckManager(host, midiOut);
        manager.activateDeck(2);
        assertTrue(logs.contains("DECK C TRAKTOR KEY: unknown; EXPECTED BITWIG KEY: unknown (no track key received)"));

        manager.setDeckKey(0, "2m");
        manager.setDeckKey(1, "11d");
        logs.clear();
        manager.activateDeck(0);
        manager.activateDeckTripleClick(0);
        manager.activateDeck(1);
        assertEquals(1, logs.stream().filter(
                "DECK A TRAKTOR KEY: 2m -> EXPECTED BITWIG KEY: E Minor (CC16=46, CC17=10)"::equals).count());
        assertTrue(logs.contains("DECK B TRAKTOR KEY: 11d -> EXPECTED BITWIG KEY: A# Major (CC16=115, CC17=0)"));
    }
}
