/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
package java.lang;

import jdk.internal.access.JavaLangInvokeAccess;
import jdk.internal.access.SharedSecrets;

import java.lang.StackWalker.StackFrame;
import java.lang.invoke.MethodType;

/**
 * このコードは、Javaの内部的なスタックフレーム（呼び出しスタックの各フレーム）情報を表現し、管理するためのクラス `StackFrameInfo` を定義しています。
 * このクラスは、`StackWalker.StackFrame` インターフェースを実装し、スタックトレースやデバッグ情報の詳細を取得する際に役立ちます。
 * 以下に、このコードの主な機能や用途を解説します。<br>
 *
 * ### 1. **スタックフレーム情報の管理**<br>
 * - **`StackFrameInfo` クラス**:  <br>
 *   各スタックフレーム（メソッド呼び出しの単位）の情報を保持します。これには、クラス名、メソッド名、ファイル名、行番号、バイトコードインデックス（BCI）などが含まれます。<br>
 *
 * - **`memberName` フィールド**:  <br>
 *   スタックフレームに関連付けられたメソッド情報を表す `MemberName` オブジェクト。このオブジェクトは内部APIを通じて管理され、VMによって初期化されます。<br>
 *
 * ### 2. **VMとの連携**<br>
 * - **`SharedSecrets.getJavaLangInvokeAccess()`**:  <br>
 *   `SharedSecrets` を使用して、`JavaLangInvokeAccess`（内部API）にアクセスします。このAPIを介して、`MemberName` オブジェクトの情報を取得します。<br>
 *
 * - **VMで初期化されるフィールド**:<br>
 *   - `bci`（バイトコードインデックス）: 実行中のメソッド内での位置を表す整数。<br>
 *   - `memberName`: メソッドの情報を保持する内部オブジェクト。<br>
 *
 * - **VMから情報を取得するメソッド例**:<br>
 *   - `JLIA.getDeclaringClass(memberName)`: メソッドを定義しているクラスを取得。<br>
 *   - `JLIA.getName(memberName)`: メソッド名を取得。<br>
 *   - `JLIA.isNative(memberName)`: メソッドがネイティブかどうかを判定。<br>
 *
 * ### 3. **`StackWalker` との統合**<br>
 * - **`StackWalker` との連携**:<br>
 *   - コンストラクタ `StackFrameInfo(StackWalker walker)` は、`StackWalker` から渡されたオプション（例: クラス参照を保持するかどうか）を設定します。<br>
 *   - スタックウォーク中に VM がこのオブジェクトを初期化し、必要なデータを埋め込みます。<br>
 *
 * - **スタックウォークの用途**:<br>
 *   - スタックフレームごとの情報を収集し、デバッグやトレースの用途で使用します。<br>
 *   - JVMの内部状態やメソッド呼び出しの詳細を分析するために使用されます。<br>
 *
 *
 * ### 5. **典型的な利用シーン**<br>
 * - **スタックトレースの詳細分析**:  <br>
 *   例えば、例外発生時のスタックトレースをより詳細に取得し、クラスやメソッドの情報を分析します。<br>
 *
 * - **デバッグツールの構築**:  <br>
 *   開発者がスタック情報をリアルタイムで解析するデバッグツールの基盤として利用されます。<br>
 *
 * - **パフォーマンスモニタリング**:  <br>
 *   メソッド呼び出しの頻度や実行場所を記録して、パフォーマンスのボトルネックを特定します。<br>
 *
 * ---
 *
 * ### 注意点<br>
 * このクラスは内部API (`jdk.internal.access`) を使用しており、通常のアプリケーション開発者が直接利用することは推奨されません。<br>
 * JDK内部の実装やデバッグ用途のために設計されており、将来的に変更や削除される可能性があります。<br>
 *
 *
 */
class StackFrameInfo implements StackFrame {
    private static final JavaLangInvokeAccess JLIA =
        SharedSecrets.getJavaLangInvokeAccess();

    private final boolean retainClassRef;
    private final Object memberName;    // MemberName initialized by VM
    private int bci;                    // initialized by VM to >= 0
    private volatile StackTraceElement ste;

    /*
     * Construct an empty StackFrameInfo object that will be filled by the VM
     * during stack walking.
     *
     * @see StackStreamFactory.AbstractStackWalker#callStackWalk
     * @see StackStreamFactory.AbstractStackWalker#fetchStackFrames
     */
    StackFrameInfo(StackWalker walker) {
        this.retainClassRef = walker.retainClassRef;
        this.memberName = JLIA.newMemberName();
    }

    // このメソッドは StackStreamFactory によって呼び出され、機能チェック（capability check）をスキップするために使用されます。
    Class<?> declaringClass() {
        return JLIA.getDeclaringClass(memberName);
    }

    // ----- implementation of StackFrame methods

    @Override
    public String getClassName() {
        return declaringClass().getName();
    }

    @Override
    public Class<?> getDeclaringClass() {
        ensureRetainClassRefEnabled();
        return declaringClass();
    }

    @Override
    public String getMethodName() {
        return JLIA.getName(memberName);
    }

    @Override
    public MethodType getMethodType() {
        ensureRetainClassRefEnabled();
        return JLIA.getMethodType(memberName);
    }

    @Override
    public String getDescriptor() {
        return JLIA.getMethodDescriptor(memberName);
    }

    @Override
    public int getByteCodeIndex() {
        // bci not available for native methods
        if (isNativeMethod())
            return -1;

        return bci;
    }

    @Override
    public String getFileName() {
        return toStackTraceElement().getFileName();
    }

    @Override
    public int getLineNumber() {
        // line number not available for native methods
        if (isNativeMethod())
            return -2;

        return toStackTraceElement().getLineNumber();
    }


    @Override
    public boolean isNativeMethod() {
        return JLIA.isNative(memberName);
    }

    @Override
    public String toString() {
        return toStackTraceElement().toString();
    }

    @Override
    public StackTraceElement toStackTraceElement() {
        StackTraceElement s = ste;
        if (s == null) {
            synchronized (this) {
                s = ste;
                if (s == null) {
                    ste = s = StackTraceElement.of(this);
                }
            }
        }
        return s;
    }

    private void ensureRetainClassRefEnabled() {
        if (!retainClassRef) {
            throw new UnsupportedOperationException("No access to RETAIN_CLASS_REFERENCE");
        }
    }
}
