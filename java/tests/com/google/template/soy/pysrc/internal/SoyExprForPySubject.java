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

package com.google.template.soy.pysrc.internal;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;
import com.google.common.truth.SubjectFactory;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.template.soy.SoyModule;
import com.google.template.soy.exprtree.ExprNode;
import com.google.template.soy.pysrc.internal.GenPyExprsVisitor.GenPyExprsVisitorFactory;
import com.google.template.soy.pysrc.internal.TranslateToPyExprVisitor.TranslateToPyExprVisitorFactory;
import com.google.template.soy.pysrc.restricted.PyExpr;
import com.google.template.soy.shared.SharedTestUtils;
import com.google.template.soy.soyparse.ParseResult;
import com.google.template.soy.soytree.PrintNode;
import com.google.template.soy.soytree.SoyFileSetNode;
import com.google.template.soy.soytree.SoyNode;

import java.util.List;
import java.util.Map;

/**
 * Truth assertion which compiles the provided soy code and asserts that the generated PyExprs match
 * the expected expressions. This subject is only valid for soy code which can be represented as one
 * or more Python expressions.
 *
 */
public final class SoyExprForPySubject extends Subject<SoyExprForPySubject, String> {

  private static final Injector INJECTOR = Guice.createInjector(new SoyModule());

  private final LocalVariableStack localVarExprs;


  SoyExprForPySubject(FailureStrategy failureStrategy, String expr) {
    super(failureStrategy, expr);
    localVarExprs = new LocalVariableStack();
  }

  public SoyExprForPySubject with(Map<String, PyExpr> localVarFrame) {
    localVarExprs.pushFrame();
    for(String name : localVarFrame.keySet()) {
      localVarExprs.addVariable(name, localVarFrame.get(name));
    }
    return this;
  }

  /**
   * Asserts the subject compiles to the correct PyExpr.
   *
   * @param expectedPyExpr The expected result of compilation.
   */
  public void compilesTo(PyExpr expectedPyExpr) {
    compilesTo(ImmutableList.of(expectedPyExpr));
  }

  /**
   * Asserts the subject compiles to the correct list of PyExprs.
   *
   * <p>The given Soy expr is wrapped in a full body of a template. The actual result is replaced
   * with ids for ### so that tests don't break when ids change.
   *
   * @param expectedPyExprs The expected result of compilation.
   */
  public void compilesTo(List<PyExpr> expectedPyExprs) {
    ParseResult<SoyFileSetNode> result = SharedTestUtils.parseSoyCode(getSubject());
    SoyNode node = SharedTestUtils.getNode(result.getParseTree(), 0);

    SharedTestUtils.simulateNewApiCall(INJECTOR, null, null);
    GenPyExprsVisitor genPyExprsVisitor = INJECTOR.getInstance(
        GenPyExprsVisitorFactory.class).create(localVarExprs);
    List<PyExpr> actualPyExprs = genPyExprsVisitor.exec(node);

    assertThat(actualPyExprs).hasSize(expectedPyExprs.size());
    for (int i = 0; i < expectedPyExprs.size(); i++) {
      PyExpr expectedPyExpr = expectedPyExprs.get(i);
      PyExpr actualPyExpr = actualPyExprs.get(i);
      assertThat(actualPyExpr.getText().replaceAll("\\([0-9]+", "(###"))
        .isEqualTo(expectedPyExpr.getText());
      assertThat(actualPyExpr.getPrecedence()).isEqualTo(expectedPyExpr.getPrecedence());
    }
  }

  /**
   * Asserts the subject translates to the expected PyExpr.
   *
   * @param expectedPyExpr The expected result of translation.
   */
  public void translatesTo(PyExpr expectedPyExpr) {
    translatesTo(expectedPyExpr, null);
  }

  /**
   * Asserts the subject translates to the expected PyExpr including verification of the exact
   * PyExpr class (e.g. {@code PyStringExpr.class}).
   *
   * @param expectedPyExpr The expected result of translation.
   * @param expectedClass The expected class of the resulting PyExpr.
   */
  public void translatesTo(PyExpr expectedPyExpr, Class<? extends PyExpr> expectedClass) {
    String soyExpr = String.format("{print %s}", getSubject());
    ParseResult<SoyFileSetNode> result = SharedTestUtils.parseSoyCode(soyExpr);
    PrintNode node = (PrintNode)SharedTestUtils.getNode(result.getParseTree(), 0);
    ExprNode exprNode = node.getExprUnion().getExpr();

    PyExpr actualPyExpr = INJECTOR.getInstance(TranslateToPyExprVisitorFactory.class)
        .create(localVarExprs)
        .exec(exprNode);
    assertThat(actualPyExpr.getText()).isEqualTo(expectedPyExpr.getText());
    assertThat(actualPyExpr.getPrecedence()).isEqualTo(expectedPyExpr.getPrecedence());

    if (expectedClass != null) {
      assertThat(actualPyExpr.getClass()).isEqualTo(expectedClass);
    }
  }


  //-----------------------------------------------------------------------------------------------
  // Public static functions for starting a SoyExprForPySubject test.


  private static final SubjectFactory<SoyExprForPySubject, String> SOYEXPR =
      new SubjectFactory<SoyExprForPySubject, String>() {
        @Override
        public SoyExprForPySubject getSubject(FailureStrategy failureStrategy, String expr) {
          return new SoyExprForPySubject(failureStrategy, expr);
        }
      };

  public static SoyExprForPySubject assertThatSoyExpr(String expr) {
    return assertAbout(SOYEXPR).that(expr);
  }
}
