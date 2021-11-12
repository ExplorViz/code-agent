package net.explorviz.code.watch;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;
import javax.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class WatchFolderProcessor implements Processor {

  private static final Logger LOGGER = LoggerFactory.getLogger(WatchFolderProcessor.class);

  @Override
  public void process(final Exchange exchange) throws Exception {

    final long lastModified =
        new BigDecimal(String.valueOf(exchange.getMessage().getHeader("CamelFileLastModified")))
            .setScale(0).longValue();


    final String eventDate = LocalDateTime
        .ofInstant(Instant.ofEpochMilli(lastModified), TimeZone.getDefault().toZoneId()).toString();

    final String filename = exchange.getMessage().getHeader("CamelFileAbsolutePath").toString();
    final String eventType = exchange.getMessage().getHeader("CamelFileEventType").toString();

    LOGGER.trace("File {} was {} at {}.", filename, eventType, eventDate);


  }

}
