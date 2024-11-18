/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * このインターフェースを実装することで、オブジェクトは拡張for文（「for-eachループ」と呼ばれることもある）の対象にすることができます。
 *
 * @param <T> the type of elements returned by the iterator
 *
 * @since 1.5
 * @jls 14.14.2 The enhanced {@code for} statement
 */
public interface Iterable<T> {
    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    Iterator<T> iterator();

    /**
     * 指定されたアクションを、Iterable の各要素に対して順番に実行します。この処理はすべての要素が処理されるか、
     * アクションが例外をスローするまで続きます。順序が指定されている場合、アクションはその順序に従って実行されます。
     * アクションによってスローされた例外は呼び出し元に伝播されます。
     * このメソッドの動作は、アクションが副作用を伴い、要素の元となるデータを変更する場合に未定義です。
     * ただし、オーバーライドしたクラスが並行変更ポリシーを明示している場合は、そのポリシーに従います。
     *
     * @implSpec
     * <p>The default implementation behaves as if:
     * <pre>{@code
     *     for (T t : this)
     *         action.accept(t);
     * }</pre>
     *
     * @param action The action to be performed for each element
     * @throws NullPointerException if the specified action is null
     * @since 1.8
     */
    default void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        for (T t : this) {
            action.accept(t);
        }
    }

    /**
     * この Iterable によって記述されている要素に対する {@link Spliterator} を作成します。
     *
     * @implSpec
     * デフォルトの実装は、この Iterable の Iterator から早期バインディングの Spliterator を作成します。
     * この Spliterator は、Iterable の Iterator が持つフェイルファスト（fail-fast）の特性を引き継ぎます。
     *
     * @implNote
     * デフォルト実装は通常、オーバーライドされるべきです。
     * デフォルト実装によって返される Spliterator は、分割能力が低く、サイズ情報を持たず、Spliterator の特性も報告しません。
     * 実装クラスはほぼ常に、より優れた実装を提供できます。
     *
     * @return a {@code Spliterator} over the elements described by this
     * {@code Iterable}.
     * @since 1.8
     */
    default Spliterator<T> spliterator() {
        return Spliterators.spliteratorUnknownSize(iterator(), 0);
    }
}
