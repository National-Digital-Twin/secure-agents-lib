// SPDX-License-Identifier: Apache-2.0
// Originally developed by Telicent Ltd.; subsequently adapted, enhanced, and maintained by the National Digital Twin Programme.

/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 *  Modifications made by the National Digital Twin Programme (NDTP)
 *  © Crown Copyright 2025. This work has been developed by the National Digital Twin Programme
 *  and is legally attributed to the Department for Business and Trade (UK) as the governing entity.
 */
package uk.gov.dbt.ndtp.secure.agent.observability.events;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dbt.ndtp.secure.agent.observability.events.CounterEvent.counterEvent;

import org.mockito.Mockito;
import org.testng.annotations.Test;

public class EventSourceSupportTest {
    @Test
    public void whenAListenerIsRegisteredWithTheEventSource_thenTheListenerIsNotifiedOfEventsEmitted() {
        // Given an event source support delegate
        final EventSourceSupport<ComponentEvent> eventSourceSupport = new EventSourceSupport<>();
        final EventListener<ComponentEvent> listener = mock(EventListener.class);
        final ComponentEvent event = counterEvent("someEvent");
        final ComponentEvent otherEvent = counterEvent("someOtherEvent");

        // When the listener is registered with the event source
        eventSourceSupport.addListener(listener);

        // And an event is emitted by the event source
        eventSourceSupport.dispatch(event);

        // Then the listener is notified of the event
        verify(listener).on(event);
        verifyNoMoreInteractions(listener);

        // When the listener is unregistered from the event source
        eventSourceSupport.removeListener(listener);

        // And an event is emitted by the event source
        eventSourceSupport.dispatch(otherEvent);

        // Then the listener is not notified of any mre events
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void whenAListenerThrowsAnExceptionDuringEventDispatch_thenTheEventIsCaughtByTheEventDispatcherAppropriately() {
        // Given an event source support delegate
        final EventSourceSupport<ComponentEvent> eventSourceSupport = new EventSourceSupport<>();
        final ComponentEvent event = counterEvent("someEvent");
        final EventListener<ComponentEvent> listener = mock(EventListener.class);
        Mockito.doThrow(new RuntimeException("The listener error")).when(listener).on(any());

        // When the listener is registered with the event source
        eventSourceSupport.addListener(listener);

        // And an event is emitted by the event source successfully without rethrowing exception
        eventSourceSupport.dispatch(event);
    }
}
