package net.explorviz.code.watch;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Watches folder.
 */
@ApplicationScoped
public class WatchFolderRoute extends RouteBuilder {

  @ConfigProperty(name = "explorviz.watchservice.folder")
  /* default */ String folderPath; // NOCS

  @ConfigProperty(name = "explorviz.watchservice.events")
  /* default */ String events; // NOCS

  @Inject
  WatchFolderProcessor processor;

  @Override
  public void configure() throws Exception {
    this.fromF("file-watch://%s?events=%s&antInclude=**/*.java", this.folderPath, this.events)
        .process(this.processor);
  }

}
