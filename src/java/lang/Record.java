/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * これはすべてのJava言語レコードクラスの共通の基底クラスです。
 * レコードに関するより詳細な情報（コンパイラによって生成される暗黙的に宣言されたメソッドの説明を含む）は、Java言語仕様の第8.10節に記載されています。
 *
 * レコードクラスは、「レコードコンポーネント」と呼ばれる固定された値の集合を扱う浅い不変で透明なキャリアです。
 * Java言語はレコードクラスを宣言するための簡潔な構文を提供しており、レコードコンポーネントはレコードヘッダーで宣言されます。
 * レコードヘッダーで宣言されたレコードコンポーネントのリストが、レコード記述子（record descriptor）を形成します。
 * レコードクラスには以下の必須メンバーがあります：
 * 標準コンストラクタ（canonical constructor）：少なくともレコードクラスと同じレベルのアクセスを提供し、その記述子（descriptor）がレコード記述子と同じである必要があります。
 * 各コンポーネントに対応するプライベートでファイナルなフィールド：名前と型はコンポーネントと同じである必要があります。
 * 各コンポーネントに対応するパブリックなアクセサーメソッド：名前と戻り値の型はコンポーネントと同じである必要があります。
 * これらのメンバーがレコードの本体で明示的に宣言されていない場合、暗黙的な実装が提供されます。
 * 標準コンストラクタの暗黙的な宣言は、レコードクラスと同じアクセスレベルを持ち、対応するコンストラクタ引数からコンポーネントフィールドを初期化します。
 * アクセサーメソッドの暗黙的な宣言は、対応するコンポーネントフィールドの値を返します。Object.equals(Object)、Object.hashCode()、および Object.toString() メソッドの暗黙的な宣言は、すべてのコンポーネントフィールドに基づいて派生されます。
 * 標準コンストラクタやアクセサーメソッドを明示的に宣言する主な理由は、コンストラクタ引数を検証したり、
 * 可変コンポーネントに対して防御的コピーを実行したり、（例えば、有理数を最簡分数にするような）コンポーネントのグループを正規化することです。
 * すべてのレコードクラスに対して、次の不変条件が保持される必要があります：もしレコード R のコンポーネントが c1, c2, ... cn である場合、以下のようにレコードインスタンスをコピーすると：
 * <pre>
 *     R copy = new R(r.c1(), r.c2(), ..., r.cn());
 * </pre>
 * then it must be the case that {@code r.equals(copy)}.
 *
 * @apiNote
 * java.io.Serializable を実装するレコードクラスは、シリアライズ可能なレコード（serializable record）と呼ばれます。
 * シリアライズ可能なレコードは、通常のシリアライズ可能なオブジェクトとは異なる方法でシリアライズおよびデシリアライズされます。
 * デシリアライズ時には、レコードの標準コンストラクタ（canonical constructor）が呼び出され、レコードオブジェクトが構築されます。
 * シリアライズ可能なレコードでは、readObject や writeObject などの特定のシリアライズ関連メソッドは無視されます。
 * シリアライズ可能なレコードに関する詳細情報は、Java Object Serialization Specification の第1.13節「レコードのシリアライズ」に記載されています。
 *
 * @apiNote
 * A record class structure can be obtained at runtime via reflection.
 * See {@link Class#isRecord()} and {@link Class#getRecordComponents()} for more details.
 *
 * @jls 8.10 Record Types
 * @since 16
 */
public abstract class Record {
    /**
     * Constructor for record classes to call.
     */
    protected Record() {}

    /**
     * ある他のオブジェクトがこのオブジェクトと「等しい」かどうかを示します。Object.equals の一般的な契約に加えて、
     * レコードクラスはさらに以下の不変条件を守る必要があります。
     * それは、レコードインスタンスが以下のように、レコードコンポーネントのアクセサーメソッドの結果を標準コンストラクタ（canonical constructor）に渡すことで「コピー」された場合です：
     * <pre>
     *     R copy = new R(r.c1(), r.c2(), ..., r.cn());
     * </pre>
     * then it must be the case that {@code r.equals(copy)}.
     *
     * @implSpec
     * 暗黙的に提供される実装は、引数がこのレコードと同じレコードクラスのインスタンスであり、
     * このレコードの各コンポーネントが引数の対応するコンポーネントと等しい場合に限り true を返します。
     * それ以外の場合は false を返します。コンポーネント c の等価性は以下のように判断されます：
     *
     * コンポーネントが参照型の場合、Objects.equals(this.c, r.c) が true を返す場合に限り等しいと見なされます。
     * コンポーネントがプリミティブ型の場合、対応するプリミティブラッパークラス PW（例えば、int の対応ラッパークラスは java.lang.Integer）を使用して、
     * PW.compare(this.c, r.c) が 0 を返す場合に限り等しいと見なされます。
     * 上記で説明されているセマンティクスを除いて、暗黙的に提供される実装で使用される正確なアルゴリズムは指定されておらず、
     * 変更される可能性があります。この実装は、記載されている特定のメソッドを使用する場合としない場合があり、またコンポーネント宣言の順序で比較を行う場合と行わない場合があります。
     *
     * @see java.util.Objects#equals(Object,Object)
     *
     * @param   obj   the reference object with which to compare.
     * @return  {@code true} if this record is equal to the
     *          argument; {@code false} otherwise.
     */
    @Override
    public abstract boolean equals(Object obj);

    /**
     * このレコードのハッシュコード値を返します。Object.hashCode の一般契約に従います。レコードにおいては、
     * Record.equals の洗練された契約によってハッシュ動作が制約されており、同じコンポーネントから作成された任意の2つのレコードは同じハッシュコードを持たなければなりません。
     *
     * @implSpec
     * 暗黙的に提供される実装は、各コンポーネントから適切なハッシュを組み合わせることによって導出されたハッシュコード値を返します。
     * 暗黙的に提供される実装で使用される正確なアルゴリズムは指定されておらず、上記の制限内で変更される可能性があります。
     * 結果として得られる整数は、同じアプリケーションの別の実行においても、コンポーネント値のハッシュがこのように一貫性を保った場合でも、
     * 一貫性を保つ必要はありません。また、プリミティブ型のコンポーネントは、そのプリミティブラッパークラスの hashCode とは異なる方法でハッシュコードにビットを寄与する可能性があります。
     *
     * @see     Object#hashCode()
     *
     * @return  a hash code value for this record.
     */
    @Override
    public abstract int hashCode();

    /**
     * レコードの文字列表現を返します。Object.toString() の一般的な契約に従い、
     * toString メソッドは、このレコードを「文字で表現する」文字列を返します。その結果は、簡潔でありながら情報を含み、人間が読みやすい表現であるべきです。
     * この一般的な契約に加えて、レコードクラスは、等しいレコードは等しい文字列を生成しなければならないという不変条件にさらに従う必要があります。
     * ただし、対応する等しいコンポーネント値が、それ自体で等しい文字列を生成できないという稀な場合、この不変条件は緩和されます。
     *
     * @implSpec
     * 暗黙的に提供される実装は、レコードクラスの名前、レコードのコンポーネント名、およびコンポーネント値の文字列表現を含む文字列を返します。
     * この方法の契約を満たすためです。この暗黙的に提供される実装によって生成される正確な形式は変更される可能性があるため、
     * 現在の構文をアプリケーションで解析してレコードコンポーネントの値を取得することは避けるべきです。
     *
     * @see     Object#toString()
     *
     * @return  a string representation of the object.
     */
    @Override
    public abstract String toString();
}
