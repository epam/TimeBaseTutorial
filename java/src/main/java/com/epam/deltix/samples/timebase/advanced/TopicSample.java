package com.epam.deltix.samples.timebase.advanced;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.DuplicateTopicException;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.io.idlestrat.BusySpinIdleStrategy;
import com.epam.deltix.util.io.idlestrat.IdleStrategy;
import com.epam.deltix.util.lang.Disposable;
import com.epam.deltix.util.time.TimeKeeper;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Alexei Osipov
 */
public class TopicSample {

    private static final String TOPIC_KEY = "example_topic";

    public static void main(String[] args) {
        // Note: you need a running Timebase server for this test
        RemoteTickDB db = TickDBFactory.connect("localhost", 8011, false);
        db.open(false);

        createTopic(db);

        // We use that flag to stop our threads
        AtomicBoolean stopFlag = new AtomicBoolean(false);

        // Publisher example
        new Thread(() -> startPublisher(db, stopFlag)).start();

        // Different types of consumers (usually you use only one of them):
        // 1) Polling consumer
        new Thread(() -> startPollingConsumer(db, stopFlag)).start();
        // 2) Message Source
        new Thread(() -> startConsumerAsMessageSource(db, stopFlag)).start();
        // 3) Worker thread
        Disposable worker = startConsumerAsWorker(db);

        // Timer thread: stops this demo after specified time
        new Thread(() -> {
            // Wait for specified time
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(20));
            } catch (InterruptedException ignored) {
            }

            // Tell other threads to stop
            stopFlag.set(true);
            worker.close(); /// Stops worker

            db.close();
        }).start();
    }

    private static void createTopic(RemoteTickDB client) {
        RecordClassDescriptor rcd = getDescriptorForInstrumentMessage();

        try {
            client.getTopicDB().createTopic(TOPIC_KEY, new RecordClassDescriptor[]{rcd}, null);
        } catch (DuplicateTopicException ignore) {
            System.out.println("Topic already exists");
        }
    }

    private static void startPublisher(RemoteTickDB client, AtomicBoolean stopFlag) {
        MessageChannel<InstrumentMessage> channel = client.getTopicDB().createPublisher(TOPIC_KEY, null, new BusySpinIdleStrategy());
        InstrumentMessage msg = new InstrumentMessage();
        while (!stopFlag.get()) {
            msg.setSymbol("GOOG");
            msg.setTimeStampMs(TimeKeeper.currentTime);

            channel.send(msg);
        }
        channel.close();
    }

    private static void startPollingConsumer(RemoteTickDB client, AtomicBoolean stopFlag) {
        MessageProcessor messageProcessor = new MessageProcessor() {
            int messageCount = 0;

            @Override
            public void process(InstrumentMessage message) {
                if (message.getSymbol().equals("GOOG")) {
                    messageCount += 1;
                    if (messageCount == 1) {
                        System.out.println("Got message in startConsumer");
                    }
                }
            }
        };
        MessagePoller poller = client.getTopicDB().createPollingConsumer(TOPIC_KEY, null);
        IdleStrategy idleStrategy = new BusySpinIdleStrategy();
        while (!stopFlag.get()) {
            idleStrategy.idle(poller.processMessages(100, messageProcessor));
        }
        poller.close();
    }

    private static void startConsumerAsMessageSource(RemoteTickDB client, AtomicBoolean stopFlag) {
        MessageSource<InstrumentMessage> messageSource = client.getTopicDB().createConsumer(TOPIC_KEY, null, new BusySpinIdleStrategy());
        int messageCount = 0;
        while (!stopFlag.get() && messageSource.next()) {
            InstrumentMessage message = messageSource.getMessage();
            if (message.getSymbol().equals("GOOG")) {
                messageCount += 1;
                if (messageCount == 1) {
                    System.out.println("Got message in startConsumerAsMessageSource");
                }
            }
        }
        messageSource.close();
    }

    private static Disposable startConsumerAsWorker(RemoteTickDB client) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().build();
        return client.getTopicDB().createConsumerWorker(TOPIC_KEY, null, new BusySpinIdleStrategy(), threadFactory, new MessageProcessor() {
            int messageCount = 0;
            @Override
            public void process(InstrumentMessage message) {
                if (message.getSymbol().equals("GOOG")) {
                    messageCount += 1;
                    if (messageCount == 1) {
                        System.out.println("Got message in startConsumerAsWorker");
                    }
                }
            }
        });
    }

    private static RecordClassDescriptor getDescriptorForInstrumentMessage() {
        Introspector ix = Introspector.createEmptyMessageIntrospector();
        try {
            return ix.introspectRecordClass("Get RD for InstrumentMessage", InstrumentMessage.class);
        } catch (Introspector.IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }
}
