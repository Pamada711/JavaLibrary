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

import java.io.IOException;

/**
 * Readable は文字の供給元です。
 * Readable からの文字は、readメソッドの呼び出し元に対して CharBuffer を介して利用可能となります。
 * @since 1.5
 */
public interface Readable {

    /**
     * 指定された文字バッファに文字を読み込もうとします。
     * このバッファはそのまま文字の保管場所として使用されます。行われる変更は、put操作の結果のみです。
     * バッファの「反転 (flipping)」や「巻き戻し (rewinding)」は行われません。
     *
     * @param cb the buffer to read characters into
     * @return バッファに追加された文字（char）の値の数、または、この文字ソースの終端に達した場合は -1 を返します。
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if cb is null
     * @throws java.nio.ReadOnlyBufferException if cb is a read only buffer
     */
    public int read(java.nio.CharBuffer cb) throws IOException;
}
