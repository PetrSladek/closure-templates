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

package com.google.template.soy.sharedpasses.opti;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.template.soy.ErrorReporterModule;
import com.google.template.soy.SoyFileSetParserBuilder;
import com.google.template.soy.basicfunctions.BasicFunctionsModule;
import com.google.template.soy.data.SoyValue;
import com.google.template.soy.data.SoyValueHelper;
import com.google.template.soy.data.SoyValueProvider;
import com.google.template.soy.exprtree.ExprRootNode;
import com.google.template.soy.shared.internal.SharedModule;
import com.google.template.soy.sharedpasses.SharedPassesModule;
import com.google.template.soy.sharedpasses.render.RenderException;
import com.google.template.soy.sharedpasses.render.TestingEnvironment;
import com.google.template.soy.soytree.PrintNode;

import junit.framework.TestCase;

import java.util.HashMap;

/**
 * Unit tests for PreevalVisitor.
 *
 */
public class PreevalVisitorTest extends TestCase {


  public void testPreevalNoData() throws Exception {

    assertEquals(-210, preeval("-99+-111").integerValue());
    assertEquals("-99-111", preeval("-99 + '-111'").stringValue());
    assertEquals(false, preeval("false or 0 or 0.0 or ''").booleanValue());
    assertEquals(true, preeval("0 <= 0").booleanValue());
    assertEquals(true, preeval("'22' == 22").booleanValue());
    assertEquals(true, preeval("'22' == '' + 22").booleanValue());

    // With functions.
    // TODO SOON: Uncomment these tests when basic functions have been changed to SoyJavaFunction.
    //assertEquals(8, preeval("max(4, 8)").integerValue());
    //assertEquals(3, preeval("floor(7/2)").integerValue());

    // With impure function.
    try {
      preeval("randomInt(1000)");
      fail();
    } catch (RenderException re) {
      assertTrue(re.getMessage().equals("Cannot preevaluate impure function."));
    }
  }


  public void testPreevalWithData() throws Exception {

    assertEquals(8, preeval("$boo").integerValue());
    assertEquals(2, preeval("$boo % 3").integerValue());
    assertEquals(false, preeval("not $boo").booleanValue());
    assertEquals("8", preeval("$boo + ''").stringValue());

    // With functions.
    // TODO SOON: Uncomment these tests when basic functions have been changed to SoyJavaFunction.
    //assertEquals(8, preeval("max(4, $boo)").integerValue());
    //assertEquals(2, preeval("floor($boo / 3)").integerValue());
    //assertEquals(3, preeval("round($boo / 3)").integerValue());

    // With undefined data.
    try {
      preeval("4 + $foo");
      fail();
    } catch (RenderException re) {
      // Test passes.
    }
  }


  public void testPreevalWithIjData() throws Exception {

    try {
      preeval("6 + $ij.foo");
      fail();
    } catch (RenderException re) {
      // Test passes.
    }
  }


  // -----------------------------------------------------------------------------------------------
  // Helpers.


  private static final Injector INJECTOR = Guice.createInjector(
      new ErrorReporterModule(),
      new SharedModule(),
      new SharedPassesModule(),
      new BasicFunctionsModule());

  /**
   * Evaluates the given expression and returns the result.
   * @param expression The expression to preevaluate.
   * @return The expression result.
   * @throws Exception If there's an error.
   */
  private static SoyValue preeval(String expression) throws Exception {
    PrintNode code =
        (PrintNode) SoyFileSetParserBuilder.forTemplateContents("{" + expression + "}")
            .parse()
            .getChild(0)
            .getChild(0)
            .getChild(0);
    ExprRootNode expr = code.getExprUnion().getExpr();
    PreevalVisitor preevalVisitor =
        INJECTOR.getInstance(PreevalVisitorFactory.class).create(
            null,
            TestingEnvironment.createForTest(
                SoyValueHelper.UNCUSTOMIZED_INSTANCE.newEasyDict("boo", 8),
                new HashMap<String, SoyValueProvider>()));
    return preevalVisitor.exec(expr);
  }

}
