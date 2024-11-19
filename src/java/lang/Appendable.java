/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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
 * 文字列や値を追加できるオブジェクト。Appendable インターフェースは、java.util.Formatter からフォーマット済みの出力を受け取ることを目的とするクラスで実装される必要があります。
 * 追加される文字は、Unicode文字表現で説明されている有効なUnicode文字であるべきです。
 * ただし、補助文字（サロゲートペアで表される文字）は、複数の16ビットのchar値で構成される可能性があることに注意してください。
 * Appendable インターフェースを実装したオブジェクトは、必ずしもスレッドセーフではありません。スレッドセーフ性の確保は、このインターフェースを拡張・実装するクラスの責任です。
 * また、このインターフェースは異なるエラーハンドリングのスタイルを持つ既存のクラスで実装される可能性があるため、エラーが呼び出し元に伝播することが保証されていません。
 *
 * @since 1.5
 */
public interface Appendable {

    /**
     * 指定された文字列シーケンスを、この Appendable に追加します。
     *
     * 文字列シーケンス csq をどのクラスが実装しているかによって、シーケンス全体が追加されない場合があります。
     * たとえば、csq が java.nio.CharBuffer の場合、追加される部分シーケンスはバッファの position（現在位置）と limit（上限）によって定義されます。
     *
     * @param  csq
     *         The character sequence to append.  If {@code csq} is
     *         {@code null}, then the four characters {@code "null"} are
     *         appended to this Appendable.
     *
     * @return  A reference to this {@code Appendable}
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    Appendable append(CharSequence csq) throws IOException;

    /**
     * 指定された文字列シーケンスの部分シーケンスを、この Appendable に追加します。
     *
     * csq が null でない場合、このメソッドを次の形式で呼び出した場合: out.append(csq, start, end) は、まったく同じ方法で動作します。
     *
     * <pre>
     *     out.append(csq.subSequence(start, end)) </pre>
     *
     * @param  csq
     *         The character sequence from which a subsequence will be
     *         appended.  If {@code csq} is {@code null}, then characters
     *         will be appended as if {@code csq} contained the four
     *         characters {@code "null"}.
     *
     * @param  start
     *         The index of the first character in the subsequence
     *
     * @param  end
     *         The index of the character following the last character in the
     *         subsequence
     *
     * @return  A reference to this {@code Appendable}
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code start} or {@code end} are negative, {@code start}
     *          is greater than {@code end}, or {@code end} is greater than
     *          {@code csq.length()}
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    Appendable append(CharSequence csq, int start, int end) throws IOException;

    /**
     * Appends the specified character to this {@code Appendable}.
     *
     * @param  c
     *         The character to append
     *
     * @return  A reference to this {@code Appendable}
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    Appendable append(char c) throws IOException;
}
