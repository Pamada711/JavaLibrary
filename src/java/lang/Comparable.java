/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.*;

/**
 * このインターフェースは、実装するクラスのオブジェクトに対して全順序を課します。
 * この順序はクラスの「自然順序」と呼ばれ、クラスの {@code compareTo} メソッドはその「自然比較メソッド」と呼ばれます。
 * このインターフェースを実装したオブジェクトのリスト（および配列）は、{@code Collections.sort}（および {@code Arrays.sort}）によって自動的にソートできます。
 * このインターフェースを実装したオブジェクトは、ソートされたマップのキーやソートされたセットの要素として使用でき、コンパレータを指定する必要はありません。
 * クラス C の自然順序が {@code equals} と一貫していると言えるのは、次の条件が満たされる場合です：
 * {@code e1.compareTo(e2) == 0} が {@code e1.equals(e2)} と同じブール値を返す場合に限ります。
 * ここで、{@code null} はどのクラスのインスタンスでもないことに注意してください。
 * {@code e.compareTo(null)} は {@link NullPointerException} をスローするべきですが、{@code e.equals(null)} は <i>false</i> を返します。
 *
 * 自然順序が {@code equals} と一貫していることは強く推奨されます（必須ではありません）。
 * 理由は、明示的なコンパレータを使用しないソートされたセット（およびソートされたマップ）では、要素（またはキー）の自然順序が {@code equals} と一致しない場合、
 * 動作が「奇妙」になるからです。特に、このようなソートされたセット（またはソートされたマップ）は、
 * {@code equals} メソッドに基づいて定義される集合（またはマップ）の一般的な契約に違反することになります。
 *
 * 例えば、キー <i>a</i> と <i>b</i> があり、({@code !a.equals(b) && a.compareTo(b) == 0}) の場合、
 * この2つのキーを明示的なコンパレータなしでソートされたセットに追加すると、2回目の追加操作が <i>false</i> を返し（セットのサイズが増えない）、
 * セットの視点では <i>a</i> と <i>b</i> が等価であるためです。
 *
 * Java のコアクラスのほとんどは、{@link Comparable} を実装しており、その自然順序は {@code equals} と一致しています。
 * 例外としては、{@link java.math.BigDecimal} があり、異なる表現（例えば、4.0 と 4.00）の同じ数値を持つ {@code BigDecimal} オブジェクトを等しいと見なす自然順序を持っています。
 * しかし、{@code BigDecimal.equals()} は、2つの {@code BigDecimal} オブジェクトの表現と数値が同一でなければ <i>true</i> を返しません。
 *
 * 数学的に言えば、与えられたクラス C の自然順序を定義する関係は次の通りです：
 * <pre>{@code
 *       {(x, y) such that x.compareTo(y) <= 0}.
 * }</pre> The <i>quotient</i> for this total order is: <pre>{@code
 *       {(x, y) such that x.compareTo(y) == 0}.
 * }</pre>
 *
 * これは、{@code compareTo} の契約から直接導き出されるものであり、この商がクラス C 上の等価関係であること、
 * そして自然順序がクラス C 上の全順序であることを意味します。
 * クラスの自然順序が {@code equals} メソッドと一貫していると言う場合、その自然順序に対応する商が、
 * クラスの {@code equals(Object)} メソッドで定義される等価関係であることを指します：
 * <pre>{@code
 *     {(x, y) such that x.equals(y)}.}
 * </pre><p>
 *
 * 言い換えると、クラスの自然順序が {@code equals} メソッドと一貫している場合、{@code equals} メソッドによる等価関係で定義される同値類と、
 * {@code compareTo} メソッドの商によって定義される同値類が一致します。
 *
 * <p>This interface is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @param <T> the type of objects that this object may be compared to
 *
 * @author  Josh Bloch
 * @see Comparator
 * @since 1.2
 */
public interface Comparable<T> {
    /**
     *
     * このオブジェクトを指定されたオブジェクトと順序付けのために比較します。
     * このオブジェクトが指定されたオブジェクトより小さい場合は負の整数、等しい場合はゼロ、大きい場合は正の整数を返します。
     * 実装者は以下を保証する必要があります：
     * <ul>
     *  <li>対称性:<br>
     *  {@code signum(x.compareTo(y)) == -signum(y.compareTo(x))} がすべての x および y について成り立つこと。
     *  （これは、{@code x.compareTo(y) }が例外をスローする場合、{@code y.compareTo(x)} も例外をスローすることを意味します。）
     *  <li>推移性:<br>
     *  {@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} の場合、{@code x.compareTo(z) > 0} が成り立つこと。
     *  <li>一貫性:<br>
     *  {@code x.compareTo(y) == 0} の場合、すべての z について {@code signum(x.compareTo(z)) == signum(y.compareTo(z))} が成り立つこと。
     * </ul>
     * これらの条件を満たすことで、{@code compareTo} メソッドによる順序付けが信頼できるものとなります。
     *
     * @apiNote
     * 強く推奨されますが、厳密には必須ではないのが、{@code (x.compareTo(y) == 0) == (x.equals(y))} という条件です。
     * 一般的に、{@code Comparable} インターフェースを実装し、この条件に違反するクラスは、その事実を明確に示すべきです。
     * 推奨される説明文は以下の通りです：
     * 「注意：このクラスは、{@code equals} と一致しない自然な順序付けを持ちます。」
     * このような注意書きを加えることで、開発者がこのクラスの動作を正しく理解し、意図しない誤解を避ける助けとなります。
     *
     * @param   o the object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *          is less than, equal to, or greater than the specified object.
     *
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException if the specified object's type prevents it
     *         from being compared to this object.
     */
    public int compareTo(T o);
}
