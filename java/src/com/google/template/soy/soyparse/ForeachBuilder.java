/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.template.soy.soyparse;

import static com.google.common.base.Preconditions.checkState;

import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.base.internal.IdGenerator;
import com.google.template.soy.exprparse.ExpressionParser;
import com.google.template.soy.exprtree.ExprRootNode;
import com.google.template.soy.soytree.ForeachIfemptyNode;
import com.google.template.soy.soytree.ForeachNode;
import com.google.template.soy.soytree.ForeachNonemptyNode;
import com.google.template.soy.soytree.SoyNode.StandaloneNode;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A helper for building {@link ForeachNode}s and its two immediate children: 
 * {@link ForeachNonemptyNode} and {@link ForeachIfemptyNode}.
 */
final class ForeachBuilder {

  private static final SoyError INVALID_COMMAND_TEXT
      = SoyError.of("Invalid ''foreach'' command text \"{0}\".");

  /** Regex pattern for the command text. */
  // 2 capturing groups: local var name, expression
  private static final Pattern FOR_EACH_COMMAND_TEXT_PATTERN =
      Pattern.compile("( [$] \\w+ ) \\s+ in \\s+ (\\S .*)", Pattern.COMMENTS | Pattern.DOTALL);

  static ForeachBuilder create(IdGenerator nodeIdGen, ErrorReporter errorReporter) {
    return new ForeachBuilder(nodeIdGen, errorReporter);
  }

  private final IdGenerator nodeIdGen;
  private final ErrorReporter errorReporter;
  private String cmdText;
  private List<StandaloneNode> templateBlock;

  private SourceLocation ifEmptyLocation;
  private List<StandaloneNode> ifEmptyBlock;
  private SourceLocation commandLocation;

  private ForeachBuilder(IdGenerator nodeIdGen, ErrorReporter errorReporter) {
    this.nodeIdGen = nodeIdGen;
    this.errorReporter = errorReporter;
  }

  ForeachBuilder setCommandLocation(SourceLocation location) {
    this.commandLocation = location;
    return this;
  }

  ForeachBuilder setCommandText(String cmdText) {
    this.cmdText = cmdText;
    
    return this;
  }
  
  ForeachBuilder setLoopBody(List<StandaloneNode> templateBlock) {
    this.templateBlock = templateBlock;
    return this;
  }

  ForeachBuilder setIfEmptyBody(SourceLocation ifEmptyLocation,
      List<StandaloneNode> ifEmptyBlock) {
    this.ifEmptyLocation = ifEmptyLocation;
    this.ifEmptyBlock = ifEmptyBlock;
    return this;
  }

  ForeachNode build() {
    checkState(cmdText != null, "You must call .setCommandText()");
    checkState(commandLocation != null, "You must call .setCommandLocation()");
    checkState(templateBlock != null, "You must call .setLoopBody()");

    String varName = "__error__";
    ExprRootNode<?> expr = null;
    Matcher matcher = FOR_EACH_COMMAND_TEXT_PATTERN.matcher(cmdText);
    if (!matcher.matches()) {
      errorReporter.report(commandLocation, INVALID_COMMAND_TEXT, cmdText);
    } else {
      varName = new ExpressionParser(matcher.group(1), commandLocation, errorReporter)
          .parseVariable()
          .getChild(0)
          .getName();
      expr = new ExpressionParser(matcher.group(2), commandLocation, errorReporter)
          .parseExpression();
    }
    
    ForeachNode foreach = new ForeachNode(nodeIdGen.genId(), expr, cmdText);
    foreach.setSourceLocation(commandLocation);
    ForeachNonemptyNode nonEmpty = new ForeachNonemptyNode(nodeIdGen.genId(), varName);
    nonEmpty.setSourceLocation(commandLocation);
    nonEmpty.addChildren(templateBlock);
    foreach.addChild(nonEmpty);
    if (ifEmptyBlock != null) {
      ForeachIfemptyNode ifEmpty = new ForeachIfemptyNode(nodeIdGen.genId());
      ifEmpty.setSourceLocation(ifEmptyLocation);
      ifEmpty.addChildren(ifEmptyBlock);
      foreach.addChild(ifEmpty);
    }
    return foreach;
  }
}
