package com.synogiestechnologies.flex_trader_reactive_websocket_server.TradeEventPublisher;

import com.synogiestechnologies.flex_trader_reactive_websocket_server.DTOResponse.BotSignalMessage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;


import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;


// class at the bottom, the two works anyway

@Component
//@Scope("singleton")
public class TradeEventPublisher {

    private final Sinks.Many<BotSignalMessage> sink;

    public TradeEventPublisher() {

//        this.sink = Sinks.many().multicast().onBackpressureBuffer();
        this.sink = Sinks.many().multicast().directBestEffort();
    }

    public void emit(BotSignalMessage signal) {

        sink.tryEmitNext(signal);
    }

    public Flux<BotSignalMessage> getStream() {

        return sink.asFlux().share();
    }
}



//@Component
//public class TradeEventPublisher {
//
//    // Keep sink alive even when there are no subscribers
//    private final Sinks.Many<BotSignalMessage> sink =
//            Sinks.many().multicast().onBackpressureBuffer(1, /*autoCancel*/ false);
//
//    public void emit(BotSignalMessage msg) {
//        Sinks.EmitResult r = sink.tryEmitNext(msg);
//        if (r.isFailure()) {
//            // Useful diagnostics while you verify the fix:
//            System.err.println("Emit failed: " + r);
//        }
//    }
//
//    public Flux<BotSignalMessage> getStream() {
//        // No .share() needed for a hot Sinks.Many
//        return sink.asFlux();
//    }
//}
