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
import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.template.soy.data.restricted.BooleanData;
import com.google.template.soy.data.restricted.IntegerData;
import com.google.template.soy.data.restricted.NullData;
import com.google.template.soy.data.restricted.StringData;
import com.google.template.soy.data.restricted.UndefinedData;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Modifier;

/**
 * Representation of a field in a java class.
 */
@AutoValue abstract class FieldRef {
  static final FieldRef BOOLEAN_DATA_FALSE = staticFieldReference(BooleanData.class, "FALSE");
  static final FieldRef BOOLEAN_DATA_TRUE = staticFieldReference(BooleanData.class, "TRUE");
  static final FieldRef INTEGER_DATA_ZERO = staticFieldReference(IntegerData.class, "ZERO");
  static final FieldRef INTEGER_DATA_ONE = staticFieldReference(IntegerData.class, "ONE");
  static final FieldRef INTEGER_DATA_MINUS_ONE = 
      staticFieldReference(IntegerData.class, "MINUS_ONE");
  static final FieldRef NULL_DATA_INSTANCE = staticFieldReference(NullData.class, "INSTANCE");
  static final FieldRef UNDEFINED_DATA_INSTANCE = 
      staticFieldReference(UndefinedData.class, "INSTANCE");
  static final FieldRef STRING_DATA_EMPTY = staticFieldReference(StringData.class, "EMPTY_STRING");
  static final FieldRef SYSTEM_OUT = staticFieldReference(System.class, "out");

  static FieldRef createFinalField(TypeInfo owner, String name, Class<?> type) {
    return new AutoValue_FieldRef(
        owner, name, Type.getType(type), 
        Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL);
  }

  static FieldRef staticFieldReference(Class<?> owner, String name) {
    Class<?> fieldType;
    try {
      java.lang.reflect.Field declaredField = owner.getDeclaredField(name);
      if (!Modifier.isStatic(declaredField.getModifiers())) {
        throw new IllegalStateException("Field: " + declaredField + " is not static");
      }
      fieldType = declaredField.getType();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return new AutoValue_FieldRef(
        TypeInfo.create(owner), name, Type.getType(fieldType), Opcodes.ACC_STATIC);
  }

  static FieldRef createField(TypeInfo owner, String name, Class<?> type) {
    return createField(owner, name, Type.getType(type));
  }

  static FieldRef createField(TypeInfo owner, String name, Type type) {
    return new AutoValue_FieldRef(owner, name, type, Opcodes.ACC_PRIVATE);
  }

  static FieldRef createPackagePrivateField(TypeInfo owner, String name, Type type) {
    return new AutoValue_FieldRef(owner, name, type, 0);
  }
  
  /** The type that owns this field. */
  abstract TypeInfo owner();
  abstract String name();
  abstract Type type();

  /** 
   * The field access flags.  This is a bit set of things like {@link Opcodes#ACC_STATIC}
   * and {@link Opcodes#ACC_PRIVATE}.
   */
  abstract int accessFlags();

  final boolean isStatic() {
    return (accessFlags() & Opcodes.ACC_STATIC) != 0;
  }

  /** Defines the given field as member of the class. */
  void defineField(ClassVisitor cv) {
    cv.visitField(
        accessFlags(),
        name(),
        type().getDescriptor(), 
        null /* no generic signature */, 
        null /* no initializer */);
  }

  /**
   * Returns an accessor that accesses this field on the given owner.
   */
  Expression accessor(final Expression owner) {
    checkState(!isStatic());
    checkArgument(owner.resultType().equals(this.owner().type()));
    return new Expression() {
      @Override public void gen(GeneratorAdapter mv) {
        owner.gen(mv);
        mv.getField(owner().type(), FieldRef.this.name(), resultType());
      }
      @Override public Type resultType() {
        return FieldRef.this.type();
      }
    };
  }

  /**
   * Returns an expression that accesses this static field.
   */
  Expression accessor() {
    checkState(isStatic());
    return new Expression() {
      @Override public void gen(GeneratorAdapter mv) {
        mv.getStatic(owner().type(), FieldRef.this.name(), resultType());
      }
      @Override public Type resultType() {
        return FieldRef.this.type();
      }
    };
  }
  
  /**
   * Returns a {@link Statement} that stores the {@code value} in this field on the given 
   * {@code instance}.
   * 
   * @throws IllegalStateException if this is a static field
   */
  Statement putInstanceField(final Expression instance, final Expression value) {
    checkState(!isStatic(), "This field is static!");
    instance.checkType(owner().type());
    value.checkType(type());
    return new Statement() {
      @Override void doGen(GeneratorAdapter adapter) {
        instance.gen(adapter);
        value.gen(adapter);
        adapter.putField(owner().type(), name(), type());
      }
    };
  }
}
