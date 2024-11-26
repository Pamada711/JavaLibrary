/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package java.lang.constant;

import java.lang.Enum.EnumDesc;
import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle.VarHandleDesc;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.constant.DirectMethodHandleDesc.Kind;
import static java.lang.constant.DirectMethodHandleDesc.Kind.STATIC;

/**
 * プリミティブクラス型やその他の一般的なプラットフォーム型の記述子を含む、
 * 標準的なブートストラップメソッド用のメソッドハンドルの記述子を含む、一般的な定数のための名目的記述子の事前定義された値。
 *
 * @see ConstantDesc
 *
 * @since 12
 */
public final class ConstantDescs {
    // No instances
    private ConstantDescs() { }

    /** 名前が必要ない場合に使用する呼び出し名、例えばコンストラクタの名前や、
     * ブートストラップが呼び出し名を無視することが知られている場合の動的定数または動的コールサイトの呼び出し名。
     */
    public static final String DEFAULT_NAME = "_";

    // Don't change the order of these declarations!

    /*
     * 定数情報の定義: ConstantDescs クラスは、さまざまな Java のクラス型（例えば Object や String、Integer など）の情報を保持するために
     * ClassDesc オブジェクトを静的定数として定義しています。これにより、これらのクラス型に関連する情報を簡単に参照できるようになります。
     */
    /** {@link ClassDesc} representing {@link Object} */
    public static final ClassDesc CD_Object = ClassDesc.of("java.lang.Object");

    /** {@link ClassDesc} representing {@link String} */
    public static final ClassDesc CD_String = ClassDesc.of("java.lang.String");

    /** {@link ClassDesc} representing {@link Class} */
    public static final ClassDesc CD_Class = ClassDesc.of("java.lang.Class");

    /** {@link ClassDesc} representing {@link Number} */
    public static final ClassDesc CD_Number = ClassDesc.of("java.lang.Number");

    /** {@link ClassDesc} representing {@link Integer} */
    public static final ClassDesc CD_Integer = ClassDesc.of("java.lang.Integer");

    /** {@link ClassDesc} representing {@link Long} */
    public static final ClassDesc CD_Long = ClassDesc.of("java.lang.Long");

    /** {@link ClassDesc} representing {@link Float} */
    public static final ClassDesc CD_Float = ClassDesc.of("java.lang.Float");

    /** {@link ClassDesc} representing {@link Double} */
    public static final ClassDesc CD_Double = ClassDesc.of("java.lang.Double");

    /** {@link ClassDesc} representing {@link Short} */
    public static final ClassDesc CD_Short = ClassDesc.of("java.lang.Short");

    /** {@link ClassDesc} representing {@link Byte} */
    public static final ClassDesc CD_Byte = ClassDesc.of("java.lang.Byte");

    /** {@link ClassDesc} representing {@link Character} */
    public static final ClassDesc CD_Character = ClassDesc.of("java.lang.Character");

    /** {@link ClassDesc} representing {@link Boolean} */
    public static final ClassDesc CD_Boolean = ClassDesc.of("java.lang.Boolean");

    /** {@link ClassDesc} representing {@link Void} */
    public static final ClassDesc CD_Void = ClassDesc.of("java.lang.Void");

    /** {@link ClassDesc} representing {@link Throwable} */
    public static final ClassDesc CD_Throwable = ClassDesc.of("java.lang.Throwable");

    /** {@link ClassDesc} representing {@link Exception} */
    public static final ClassDesc CD_Exception = ClassDesc.of("java.lang.Exception");

    /** {@link ClassDesc} representing {@link Enum} */
    public static final ClassDesc CD_Enum = ClassDesc.of("java.lang.Enum");

    /** {@link ClassDesc} representing {@link VarHandle} */
    public static final ClassDesc CD_VarHandle = ClassDesc.of("java.lang.invoke.VarHandle");

    /** {@link ClassDesc} representing {@link MethodHandles} */
    public static final ClassDesc CD_MethodHandles = ClassDesc.of("java.lang.invoke.MethodHandles");

    /** {@link ClassDesc} representing {@link Lookup} */
    public static final ClassDesc CD_MethodHandles_Lookup = CD_MethodHandles.nested("Lookup");

    /** {@link ClassDesc} representing {@link MethodHandle} */
    public static final ClassDesc CD_MethodHandle = ClassDesc.of("java.lang.invoke.MethodHandle");

    /** {@link ClassDesc} representing {@link MethodType} */
    public static final ClassDesc CD_MethodType = ClassDesc.of("java.lang.invoke.MethodType");

    /** {@link ClassDesc} representing {@link CallSite} */
    public static final ClassDesc CD_CallSite = ClassDesc.of("java.lang.invoke.CallSite");

    /** {@link ClassDesc} representing {@link Collection} */
    public static final ClassDesc CD_Collection = ClassDesc.of("java.util.Collection");

    /** {@link ClassDesc} representing {@link List} */
    public static final ClassDesc CD_List = ClassDesc.of("java.util.List");

    /** {@link ClassDesc} representing {@link Set} */
    public static final ClassDesc CD_Set = ClassDesc.of("java.util.Set");

    /** {@link ClassDesc} representing {@link Map} */
    public static final ClassDesc CD_Map = ClassDesc.of("java.util.Map");

    /** {@link ClassDesc} representing {@link ConstantDesc} */
    public static final ClassDesc CD_ConstantDesc = ClassDesc.of("java.lang.constant.ConstantDesc");

    /** {@link ClassDesc} representing {@link ClassDesc} */
    public static final ClassDesc CD_ClassDesc = ClassDesc.of("java.lang.constant.ClassDesc");

    /** {@link ClassDesc} representing {@link EnumDesc} */
    public static final ClassDesc CD_EnumDesc = CD_Enum.nested("EnumDesc");

    /** {@link ClassDesc} representing {@link MethodTypeDesc} */
    public static final ClassDesc CD_MethodTypeDesc = ClassDesc.of("java.lang.constant.MethodTypeDesc");

    /** {@link ClassDesc} representing {@link MethodHandleDesc} */
    public static final ClassDesc CD_MethodHandleDesc = ClassDesc.of("java.lang.constant.MethodHandleDesc");

    /** {@link ClassDesc} representing {@link DirectMethodHandleDesc} */
    public static final ClassDesc CD_DirectMethodHandleDesc = ClassDesc.of("java.lang.constant.DirectMethodHandleDesc");

    /** {@link ClassDesc} representing {@link VarHandleDesc} */
    public static final ClassDesc CD_VarHandleDesc = CD_VarHandle.nested("VarHandleDesc");

    /** {@link ClassDesc} representing {@link Kind} */
    public static final ClassDesc CD_MethodHandleDesc_Kind = CD_DirectMethodHandleDesc.nested("Kind");

    /** {@link ClassDesc} representing {@link DynamicConstantDesc} */
    public static final ClassDesc CD_DynamicConstantDesc = ClassDesc.of("java.lang.constant.DynamicConstantDesc");

    /** {@link ClassDesc} representing {@link DynamicCallSiteDesc} */
    public static final ClassDesc CD_DynamicCallSiteDesc = ClassDesc.of("java.lang.constant.DynamicCallSiteDesc");

    /** {@link ClassDesc} representing {@link ConstantBootstraps} */
    public static final ClassDesc CD_ConstantBootstraps = ClassDesc.of("java.lang.invoke.ConstantBootstraps");

    private static final ClassDesc[] INDY_BOOTSTRAP_ARGS = {
            ConstantDescs.CD_MethodHandles_Lookup,
            ConstantDescs.CD_String,
            ConstantDescs.CD_MethodType};

    private static final ClassDesc[] CONDY_BOOTSTRAP_ARGS = {
            ConstantDescs.CD_MethodHandles_Lookup,
            ConstantDescs.CD_String,
            ConstantDescs.CD_Class};

    /** {@link MethodHandleDesc} representing {@link ConstantBootstraps#primitiveClass(Lookup, String, Class) ConstantBootstraps.primitiveClass} */
    public static final DirectMethodHandleDesc BSM_PRIMITIVE_CLASS
            = ofConstantBootstrap(CD_ConstantBootstraps, "primitiveClass",
            CD_Class);

    /** {@link MethodHandleDesc} representing {@link ConstantBootstraps#enumConstant(Lookup, String, Class) ConstantBootstraps.enumConstant} */
    public static final DirectMethodHandleDesc BSM_ENUM_CONSTANT
            = ofConstantBootstrap(CD_ConstantBootstraps, "enumConstant",
            CD_Enum);

    /**
     * {@link MethodHandleDesc} representing {@link ConstantBootstraps#getStaticFinal(Lookup, String, Class, Class) ConstantBootstraps.getStaticFinal}
     * @since 15
     */
    public static final DirectMethodHandleDesc BSM_GET_STATIC_FINAL
            = ofConstantBootstrap(CD_ConstantBootstraps, "getStaticFinal",
            CD_Object, CD_Class);

    /** {@link MethodHandleDesc} representing {@link ConstantBootstraps#nullConstant(Lookup, String, Class) ConstantBootstraps.nullConstant} */
    public static final DirectMethodHandleDesc BSM_NULL_CONSTANT
            = ofConstantBootstrap(CD_ConstantBootstraps, "nullConstant",
            CD_Object);

    /** {@link MethodHandleDesc} representing {@link ConstantBootstraps#fieldVarHandle(Lookup, String, Class, Class, Class) ConstantBootstraps.fieldVarHandle} */
    public static final DirectMethodHandleDesc BSM_VARHANDLE_FIELD
            = ofConstantBootstrap(CD_ConstantBootstraps, "fieldVarHandle",
            CD_VarHandle, CD_Class, CD_Class);

    /** {@link MethodHandleDesc} representing {@link ConstantBootstraps#staticFieldVarHandle(Lookup, String, Class, Class, Class) ConstantBootstraps.staticVarHandle} */
    public static final DirectMethodHandleDesc BSM_VARHANDLE_STATIC_FIELD
            = ofConstantBootstrap(CD_ConstantBootstraps, "staticFieldVarHandle",
            CD_VarHandle, CD_Class, CD_Class);

    /** {@link MethodHandleDesc} representing {@link ConstantBootstraps#arrayVarHandle(Lookup, String, Class, Class) ConstantBootstraps.arrayVarHandle} */
    public static final DirectMethodHandleDesc BSM_VARHANDLE_ARRAY
            = ofConstantBootstrap(CD_ConstantBootstraps, "arrayVarHandle",
            CD_VarHandle, CD_Class);

    /** {@link MethodHandleDesc} representing {@link ConstantBootstraps#invoke(Lookup, String, Class, MethodHandle, Object...) ConstantBootstraps.invoke} */
    public static final DirectMethodHandleDesc BSM_INVOKE
            = ofConstantBootstrap(CD_ConstantBootstraps, "invoke",
            CD_Object, CD_MethodHandle, CD_Object.arrayType());

    /**
     * {@link MethodHandleDesc} representing {@link ConstantBootstraps#explicitCast(Lookup, String, Class, Object)} ConstantBootstraps.explicitCast}
     * @since 15
     */
    public static final DirectMethodHandleDesc BSM_EXPLICIT_CAST
            = ofConstantBootstrap(CD_ConstantBootstraps, "explicitCast",
            CD_Object, CD_Object);

    /** {@link ClassDesc} representing the primitive type {@code int} */
    public static final ClassDesc CD_int = ClassDesc.ofDescriptor("I");

    /** {@link ClassDesc} representing the primitive type {@code long} */
    public static final ClassDesc CD_long = ClassDesc.ofDescriptor("J");

    /** {@link ClassDesc} representing the primitive type {@code float} */
    public static final ClassDesc CD_float = ClassDesc.ofDescriptor("F");

    /** {@link ClassDesc} representing the primitive type {@code double} */
    public static final ClassDesc CD_double = ClassDesc.ofDescriptor("D");

    /** {@link ClassDesc} representing the primitive type {@code short} */
    public static final ClassDesc CD_short = ClassDesc.ofDescriptor("S");

    /** {@link ClassDesc} representing the primitive type {@code byte} */
    public static final ClassDesc CD_byte = ClassDesc.ofDescriptor("B");

    /** {@link ClassDesc} representing the primitive type {@code char} */
    public static final ClassDesc CD_char = ClassDesc.ofDescriptor("C");

    /** {@link ClassDesc} representing the primitive type {@code boolean} */
    public static final ClassDesc CD_boolean = ClassDesc.ofDescriptor("Z");

    /** {@link ClassDesc} representing the primitive type {@code void} */
    public static final ClassDesc CD_void = ClassDesc.ofDescriptor("V");

    /*
     * 動的定数の管理: ConstantDescs クラスは、動的定数（例えば null や Boolean.TRUE など）を表現するために、DynamicConstantDesc を使用しています。
     * これらの定数は、実行時に決定される値を持つ定数であり、invokedynamic のような動的なメカニズムを使用して値が取得されます。
     */
    /** Nominal descriptor representing the constant {@code null} */
    public static final ConstantDesc NULL
            = DynamicConstantDesc.ofNamed(ConstantDescs.BSM_NULL_CONSTANT,
                                          DEFAULT_NAME, ConstantDescs.CD_Object);

    /**
     * Nominal descriptor representing the constant {@linkplain Boolean#TRUE}
     * @since 15
     */
    public static final DynamicConstantDesc<Boolean> TRUE
            = DynamicConstantDesc.ofNamed(BSM_GET_STATIC_FINAL,
                                          "TRUE", CD_Boolean, CD_Boolean);

    /**
     * Nominal descriptor representing the constant {@linkplain Boolean#FALSE}
     * @since 15
     */
    public static final DynamicConstantDesc<Boolean> FALSE
            = DynamicConstantDesc.ofNamed(BSM_GET_STATIC_FINAL,
                                          "FALSE", CD_Boolean, CD_Boolean);

    static final DirectMethodHandleDesc MHD_METHODHANDLE_ASTYPE
            = MethodHandleDesc.ofMethod(Kind.VIRTUAL, CD_MethodHandle, "asType",
                                        MethodTypeDesc.of(CD_MethodHandle, CD_MethodType));
    /**
     * invokedynamic コールサイトのためのブートストラップメソッドに対応する MethodHandleDesc を返します。
     * これは、最初のパラメータータイプが Lookup、String、MethodType である静的メソッドです。
     *
     * @param owner メソッドを宣言しているクラス
     * @param name メソッドの名前（修飾なし）
     * @param returnType メソッドの戻り値の型
     * @param paramTypes 静的ブートストラップ引数の型（存在する場合）
     * @throws NullPointerException もし引数のいずれかが null の場合
     * @jvms 4.2.2 Unqualified Names
     */
    public static DirectMethodHandleDesc ofCallsiteBootstrap(ClassDesc owner,
                                                             String name,
                                                             ClassDesc returnType,
                                                             ClassDesc... paramTypes) {
        return MethodHandleDesc.ofMethod(STATIC, owner, name, MethodTypeDesc.of(returnType, paramTypes)
                                                                            .insertParameterTypes(0, INDY_BOOTSTRAP_ARGS));
    }

    /**
     * 動的定数のためのブートストラップメソッドに対応する MethodHandleDesc を返します。これは、最初の引数が Lookup、String、Class である静的メソッドです。
     *
     * @param owner メソッドを宣言しているクラス
     * @param name メソッドの名前（修飾なし）
     * @param returnType メソッドの戻り値の型
     * @param paramTypes 静的ブートストラップ引数の型（存在する場合）
     * @throws NullPointerException もし引数のいずれかが null の場合
     * @jvms 4.2.2 Unqualified Names
     */
    public static DirectMethodHandleDesc ofConstantBootstrap(ClassDesc owner,
                                                             String name,
                                                             ClassDesc returnType,
                                                             ClassDesc... paramTypes) {
        return MethodHandleDesc.ofMethod(STATIC, owner, name, MethodTypeDesc.of(returnType, paramTypes)
                                                                            .insertParameterTypes(0, CONDY_BOOTSTRAP_ARGS));
    }
}
