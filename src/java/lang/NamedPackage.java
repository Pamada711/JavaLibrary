/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.module.Configuration;
import java.lang.module.ModuleReference;
import java.net.URI;

/**
 * NamedPackage は、特定のモジュール内で名前によってパッケージを表現します。
 * パッケージの名前とそのパッケージが属するモジュールを保持します。
 * クラスローダーは、クラスが定義されると自動的に各パッケージに対して NamedPackage を作成します。
 * パッケージオブジェクトは、Class::getPackage、Package::getPackage(s)、
 * または ClassLoader::getDefinedPackage(s) メソッドが呼び出されるまで遅延的に定義されます。
 * NamedPackage は、クラスローダーがランタイムのパッケージを最小限のフットプリントで追跡できるようにし、Package オブジェクトを構築するのを避けます。
 * @apiNote
 * Java 9 以降のモジュールシステム（JPMS）で、モジュール内のパッケージを効率的に管理するために使用されます。
 * 特に、クラスローダーがパッケージ情報を遅延的に取得する場合に役立ちます。
 * 名前付きモジュールのパッケージに関連する情報を提供し、モジュールの場所を確認するために利用されることがあります。
 * NamedPackage は、内部的にモジュールとパッケージの関係を効率的に処理するためのクラスであり、
 * 通常アプリケーションコードで直接使用されることは少なく、Javaランタイムやモジュールシステムの内部で利用されます。
 */
class NamedPackage {
    private final String name;
    private final Module module;

    NamedPackage(String pn, Module module) {
        if (pn.isEmpty() && module.isNamed()) {
            throw new InternalError("unnamed package in  " + module);
        }
        this.name = pn.intern();
        this.module = module;
    }

    /**
     * Returns the name of this package.
     */
    String packageName() {
        return name;
    }

    /**
     * Returns the module of this named package.
     */
    Module module() {
        return module;
    }

    /**
     * この名前付きパッケージが名前付きモジュール内にある場合、そのモジュールの場所を返します。
     * モジュールが名前付きでない場合やモジュールの場所が不明な場合には null を返します。
     */
    URI location() {
        if (module.isNamed() && module.getLayer() != null) {
            Configuration cf = module.getLayer().configuration();
            ModuleReference mref
                = cf.findModule(module.getName()).get().reference();
            return mref.location().orElse(null);
        }
        return null;
    }

    /**
     * Creates a Package object of the given name and module.
     */
    static Package toPackage(String name, Module module) {
        return new Package(name, module);
    }
}
