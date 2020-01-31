/*
 * This file is a part of BSL Language Server.
 *
 * Copyright © 2018-2020
 * Alexey Sosnoviy <labotamy@gmail.com>, Nikita Gryzlov <nixel2007@gmail.com> and contributors
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * BSL Language Server is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * BSL Language Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BSL Language Server.
 */
package com.github._1c_syntax.bsl.languageserver;

import com.github._1c_syntax.bsl.languageserver.configuration.LanguageServerConfiguration;
import com.github._1c_syntax.bsl.languageserver.context.ServerContext;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp4j.CodeLensOptions;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class BSLLanguageServer implements LanguageServer, LanguageClientAware {

  private final LanguageServerConfiguration configuration;

  private BSLTextDocumentService textDocumentService;
  private BSLWorkspaceService workspaceService;
  private boolean shutdownWasCalled;
  private ServerContext context;

  public BSLLanguageServer(LanguageServerConfiguration configuration, File configurationFile) {
    this(configuration);

    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.submit(() -> {
      String configurationFileName = configurationFile.getName();
      try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
        Path configurationDir = configurationFile.toPath().getParent();
        configurationDir.register(
          watchService,
          StandardWatchEventKinds.ENTRY_CREATE,
          StandardWatchEventKinds.ENTRY_DELETE,
          StandardWatchEventKinds.ENTRY_MODIFY
        );

        WatchKey key;
        while ((key = watchService.take()) != null) {
          for (WatchEvent<?> event : key.pollEvents()) {
            Path watchedFile = (Path) event.context();
            if (watchedFile == null || !watchedFile.toFile().getName().equals(configurationFileName)) {
              continue;
            }

            LOGGER.info(watchedFile.toFile().getPath());
            LOGGER.info(watchedFile.toAbsolutePath().toFile().getPath());

            var newConfiguration = LanguageServerConfiguration.create(watchedFile.toAbsolutePath().toFile());
            var params = new DidChangeConfigurationParams(newConfiguration);
            workspaceService.didChangeConfiguration(params);
            
            LOGGER.info("BSL Language Server configuration was reloaded");
          }
          key.reset();
        }

      } catch (IOException e) {
        LOGGER.error(e.getMessage(), e);
      } catch (InterruptedException e) {
        LOGGER.error(e.getMessage(), e);
        Thread.currentThread().interrupt();
      }
    });

  }

  public BSLLanguageServer(LanguageServerConfiguration configuration) {
    this.configuration = configuration;

    context = new ServerContext();
    workspaceService = new BSLWorkspaceService(configuration);
    textDocumentService = new BSLTextDocumentService(configuration, context);
  }

  public BSLLanguageServer() {
    this(LanguageServerConfiguration.create());
  }

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {

    setConfigurationRoot(params);

    ServerCapabilities capabilities = new ServerCapabilities();
    capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
    capabilities.setDocumentRangeFormattingProvider(Boolean.TRUE);
    capabilities.setDocumentFormattingProvider(Boolean.TRUE);
    capabilities.setFoldingRangeProvider(Boolean.TRUE);
    capabilities.setDocumentSymbolProvider(Boolean.TRUE);
    capabilities.setCodeActionProvider(Boolean.TRUE);
    capabilities.setCodeLensProvider(new CodeLensOptions());

    InitializeResult result = new InitializeResult(capabilities);

    return CompletableFuture.completedFuture(result);
  }

  private void setConfigurationRoot(InitializeParams params) {
    if (params.getRootUri() == null) {
      return;
    }

    Path rootPath;
    try {
      rootPath = new File(new URI(params.getRootUri()).getPath()).getCanonicalFile().toPath();
    } catch (URISyntaxException | IOException e) {
      LOGGER.error("Can't read root URI from initialization params.", e);
      return;
    }

    Path configurationRoot = LanguageServerConfiguration.getCustomConfigurationRoot(
      configuration,
      rootPath);
    context.setConfigurationRoot(configurationRoot);
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    shutdownWasCalled = true;
    textDocumentService.reset();
    context.clear();
    return CompletableFuture.completedFuture(Boolean.TRUE);
  }

  @Override
  public void exit() {
    int status = shutdownWasCalled ? 0 : 1;
    System.exit(status);
  }

  @Override
  public TextDocumentService getTextDocumentService() {
    return textDocumentService;
  }

  @Override
  public WorkspaceService getWorkspaceService() {
    return workspaceService;
  }

  @Override
  public void connect(LanguageClient client) {
    textDocumentService.connect(client);
  }
}
