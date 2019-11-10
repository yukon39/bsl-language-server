/*
 * This file is a part of BSL Language Server.
 *
 * Copyright © 2018-2019
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
package com.github._1c_syntax.bsl.languageserver.diagnostics;

import com.github._1c_syntax.bsl.languageserver.configuration.LanguageServerConfiguration;
import com.github._1c_syntax.bsl.languageserver.context.FileType;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticCompatibilityMode;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticInfo;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticMetadata;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticScope;
import com.github._1c_syntax.mdclasses.metadata.additional.CompatibilityMode;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DiagnosticSupplier {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticSupplier.class.getSimpleName());

  private final LanguageServerConfiguration configuration;
  private List<Class<? extends BSLDiagnostic>> diagnosticClasses;

  public DiagnosticSupplier(LanguageServerConfiguration configuration) {
    this.configuration = configuration;
    diagnosticClasses = createDiagnosticClasses();
  }

  public List<Class<? extends BSLDiagnostic>> getDiagnosticClasses() {
    return new ArrayList<>(diagnosticClasses);
  }

  public Optional<Class<? extends BSLDiagnostic>> getDiagnosticClass(String diagnosticCode) {
    return diagnosticClasses.stream()
      .filter(diagnosticClass -> getDiagnosticInfo(diagnosticClass).getDiagnosticCode().equals(diagnosticCode))
      .findAny();
  }

  public List<BSLDiagnostic> getDiagnosticInstances(FileType fileType, CompatibilityMode compatibilityMode) {
    return diagnosticClasses.stream()
      .map(this::getDiagnosticInfo)
      .filter(this::isEnabled)
      .filter(info -> inScope(info, fileType))
      .filter(info -> passedCompatibilityMode(info, compatibilityMode))
      .map(this::createDiagnosticInstance)
      .peek(this::configureDiagnostic)
      .collect(Collectors.toList());
  }

  public BSLDiagnostic getDiagnosticInstance(Class<? extends BSLDiagnostic> diagnosticClass) {
    DiagnosticInfo info = new DiagnosticInfo(diagnosticClass, configuration);
    BSLDiagnostic diagnosticInstance = createDiagnosticInstance(info);
    configureDiagnostic(diagnosticInstance);

    return diagnosticInstance;
  }

  private BSLDiagnostic createDiagnosticInstance(DiagnosticInfo diagnosticInfo) {
    BSLDiagnostic diagnostic = null;
    Class<? extends BSLDiagnostic> diagnosticClass = diagnosticInfo.getDiagnosticClass();
    DiagnosticInfo info = getDiagnosticInfo(diagnosticClass);
    try {
      diagnostic = diagnosticClass.getDeclaredConstructor(DiagnosticInfo.class).newInstance(info);
    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      LOGGER.error("Can't instantiate diagnostic", e);
    }
    return diagnostic;
  }

  private DiagnosticInfo getDiagnosticInfo(Class<? extends BSLDiagnostic> diagnosticClass) {
    return new DiagnosticInfo(diagnosticClass, configuration);
  }

  private boolean inScope(DiagnosticInfo diagnosticInfo, FileType fileType) {
    DiagnosticScope scope = diagnosticInfo.getScope();
    DiagnosticScope fileScope;
    if (fileType == FileType.OS) {
      fileScope = DiagnosticScope.OS;
    } else {
      fileScope = DiagnosticScope.BSL;
    }
    return scope == DiagnosticScope.ALL || scope == fileScope;
  }

  private boolean passedCompatibilityMode(
    DiagnosticInfo diagnosticInfo,
    CompatibilityMode contextCompatibilityMode
  ) {
    DiagnosticCompatibilityMode compatibilityMode = diagnosticInfo.getCompatibilityMode();

    if (compatibilityMode == DiagnosticCompatibilityMode.UNDEFINED) {
      return true;
    }
    if (contextCompatibilityMode == null) {
      return false;
    }

    return CompatibilityMode.compareTo(contextCompatibilityMode, compatibilityMode.getCompatibilityMode()) >= 0;
  }

  private void configureDiagnostic(BSLDiagnostic diagnostic) {
    Either<Boolean, Map<String, Object>> diagnosticConfiguration =
      configuration.getDiagnostics().get(diagnostic.getInfo().getDiagnosticCode());
    if (diagnosticConfiguration != null && diagnosticConfiguration.isRight()) {
      diagnostic.configure(diagnosticConfiguration.getRight());
    }
  }

  private boolean isEnabled(DiagnosticInfo diagnosticInfo) {
    if (diagnosticInfo == null) {
      return false;
    }

    Either<Boolean, Map<String, Object>> diagnosticConfiguration =
      configuration.getDiagnostics().get(diagnosticInfo.getDiagnosticCode());

    boolean activatedByDefault = diagnosticConfiguration == null && diagnosticInfo.isActivatedByDefault();
    boolean hasCustomConfiguration = diagnosticConfiguration != null && diagnosticConfiguration.isRight();
    boolean enabledDirectly = diagnosticConfiguration != null
      && diagnosticConfiguration.isLeft()
      && diagnosticConfiguration.getLeft();

    return activatedByDefault
      || hasCustomConfiguration
      || enabledDirectly;
  }

  @SuppressWarnings("unchecked")
  private static List<Class<? extends BSLDiagnostic>> createDiagnosticClasses() {

    Reflections diagnosticReflections = new Reflections(
      new ConfigurationBuilder()
        .setUrls(
          ClasspathHelper.forPackage(
            BSLDiagnostic.class.getPackage().getName(),
            ClasspathHelper.contextClassLoader(),
            ClasspathHelper.staticClassLoader()
          )
        )
    );

    return diagnosticReflections.getTypesAnnotatedWith(DiagnosticMetadata.class)
      .stream()
      .map(aClass -> (Class<? extends BSLDiagnostic>) aClass)
      .collect(Collectors.toList());
  }

}
