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

package com.google.template.soy.jssrc.internal;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.template.soy.jssrc.SoyJsSrcOptions;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.shared.SharedTestUtils;
import com.google.template.soy.shared.internal.GuiceSimpleScope;
import com.google.template.soy.shared.restricted.ApiCallScopeBindingAnnotations.IsUsingIjData;
import com.google.template.soy.soyparse.ParseResult;
import com.google.template.soy.soytree.SoyFileSetNode;
import com.google.template.soy.soytree.SoyNode;

import javax.annotation.Nullable;

/**
 * Utilities for unit tests in the Js Src backend.
 *
 */
class JsSrcTestUtils {

  private JsSrcTestUtils() {}


  /**
   * Simulates the start of a new Soy API call by entering/re-entering the ApiCallScope and seeding
   * scoped values.
   *
   * @param injector The Guice injector responsible for injections during the API call.
   */
  public static void simulateNewApiCall(Injector injector) {
    simulateNewApiCall(injector, new SoyJsSrcOptions(), null, 0);
  }


  /**
   * Simulates the start of a new Soy API call by entering/re-entering the ApiCallScope and seeding
   * scoped values.
   *
   * @param injector The Guice injector responsible for injections during the API call.
   * @param jsSrcOptions The options for generating JS source code.
   */
  public static void simulateNewApiCall(Injector injector, SoyJsSrcOptions jsSrcOptions) {
    simulateNewApiCall(injector, jsSrcOptions, null, 0);
  }


  /**
   * Simulates the start of a new Soy API call by entering/re-entering the ApiCallScope and seeding
   * scoped values.
   *
   * @param injector The Guice injector responsible for injections during the API call.
   * @param jsSrcOptions The options for generating JS source code.
   * @param msgBundle The bundle of translated messages, or null to use the messages from the
   *     Soy source.
   * @param bidiGlobalDir The bidi global directionality (ltr=1, rtl=-1), or 0 to use a value
   *     derived from the message bundle.
   */
  public static void simulateNewApiCall(
      Injector injector, SoyJsSrcOptions jsSrcOptions, @Nullable SoyMsgBundle msgBundle,
      int bidiGlobalDir) {

    GuiceSimpleScope apiCallScope =
        SharedTestUtils.simulateNewApiCall(injector, msgBundle, bidiGlobalDir);
    apiCallScope.seed(SoyJsSrcOptions.class, jsSrcOptions);
    apiCallScope.seed(Key.get(Boolean.class, IsUsingIjData.class), jsSrcOptions.isUsingIjData());
  }


  /**
   * Parses the given piece of Soy code as the full body of a template.
   *
   * <p> Important: ReplaceMsgsWithGoogMsgsVisitor will be run on the parse tree.
   *
   * @param soyCode The code to parse as the full body of a template.
   * @return The resulting parse tree.
   */
  static ParseResult<SoyFileSetNode> parseSoyCode(String soyCode) {
    ParseResult<SoyFileSetNode> result = SharedTestUtils.parseSoyCode(soyCode);
    (new ReplaceMsgsWithGoogMsgsVisitor()).exec(result.getParseTree());
    return result;
  }


  /**
   * Parses the given piece of Soy code as the full body of a template, and then returns the node
   * within the resulting template parse tree indicated by the given indices to reach the desired
   * node.
   *
   * <p> Important: ReplaceMsgsWithGoogMsgsVisitor will be run on the parse tree.
   *
   * @param soyCode The code to parse as the full body of a template.
   * @param indicesToNode The indices to reach the desired node to retrieve. E.g. To retrieve the
   *     first child of the template, simply pass a single 0.
   * @return The desired node in the resulting template parse tree.
   */
  static ParseResult<SoyNode> parseSoyCodeAndGetNode(String soyCode, int... indicesToNode) {
    ParseResult<SoyFileSetNode> result = parseSoyCode(soyCode);
    if (!result.isSuccess()) {
      return new ParseResult<>(null, result.getParseErrors());
    }
    SoyNode node = SharedTestUtils.getNode(result.getParseTree(), indicesToNode);
    return new ParseResult<>(node, result.getParseErrors());
  }

}
