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

package com.google.template.soy.jbcsrc;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.auto.value.AutoValue;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

/**
 * A local variable representation.
 * 
 * <p>This does nothing to enforce required constraints, e.g.:
 * <ul>
 *    <li>This does not ensure that {@link #start()} and {@link #end()} are valid and exist in the
 *        method.
 *    <li>This does not ensure that the {@link #index} is otherwise unused and that only one 
 *        variable is active at a time with the index.
 * </ul>
 * 
 * <p>Note: This class does not attempt to make use of the convenience methods on generator adapter
 * such as {@link GeneratorAdapter#newLocal(Type)} or {@link GeneratorAdapter#loadArg(int)} that
 * make it easier to work with local variables (and calculating local variable indexes).  Instead
 * we push this responsibility onto our caller.  This is because GeneratorAdapter doesn't make it
 * possible to generate local variable debugging tables in this case (e.g. there is no way to map
 * a method parameter index to a local variable index).
 */
@AutoValue abstract class LocalVariable extends Expression {
  static LocalVariable createThisVar(TypeInfo owner, Label start, Label end) {
    return new AutoValue_LocalVariable("this", owner.type(), 0, start, end);
  }

  static LocalVariable createLocal(String name, int index, Type type, Label start, Label end) {
    checkArgument(!name.equals("this"));
    return new AutoValue_LocalVariable(name, type, index, start, end);
  }

  /** The name of the variable, ends up in debugging tables. */
  abstract String variableName();
  @Override abstract Type resultType();

  abstract int index();

  /** A label defining the earliest point at which this variable is defined. */
  abstract Label start();

  /** A label defining the latest point at which this variable is defined. */
  abstract Label end();

  /**
   * Write a local variable table entry for this variable.  This informs debuggers about variable
   * names, types and lifetime.
   */
  void tableEntry(GeneratorAdapter mv) {
    mv.visitLocalVariable(
        variableName(),
        resultType().getDescriptor(),
        null /** no generic signature */,
        start(),
        end(),
        index());
  }

  @Override public void gen(GeneratorAdapter mv) {
    mv.visitVarInsn(resultType().getOpcode(Opcodes.ILOAD), index());
  }

  /** Writes the value at the top of the stack to the local variable. */
  void store(GeneratorAdapter mv) {
    mv.visitVarInsn(resultType().getOpcode(Opcodes.ISTORE), index());
  }
}
