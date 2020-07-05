package io.kt.cloud.tanda.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import io.kt.cloud.tanda.entity.BookRepository;
import io.kt.cloud.tanda.enumeration.BookStatus;
import io.kt.cloud.tanda.event.CancelConfirmed;

@Service
public class KafkaListener {

	private static final Logger logger = LoggerFactory.getLogger(KafkaListener.class);

	@Autowired
	BookRepository bookRepository;

	@StreamListener(target = Processor.INPUT, condition = CancelConfirmed.ONLY_ME)
	public void handleOrderPlaced(@Payload CancelConfirmed event) {
		if (event == null) {
			return;
		}
;
		if (event.getBookId() == null) {
			return;
		}

		logger.info("[SUB] " + event);

		bookRepository.findById(event.getBookId()).ifPresent(book -> {
			if (event.isConfirmed()) {
				book.setBookStatus(BookStatus.CANCELED_BY_CUSTUMER.getHangul());
			} else {
				book.setBookStatus(BookStatus.ACCEPTED.getHangul());
			}
			book.setConfirmed(true);
			bookRepository.save(book);
		});
	}

	@StreamListener(target = Processor.INPUT)
	public void handle(@Payload String unknownEvent) {
		// System.out.println(unknownEvent);
	}

}
