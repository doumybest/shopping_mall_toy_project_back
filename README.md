# ソソマーケット バックエンド

ソソマーケットは、日本語・日本円に対応した学習用のオンラインショップです。このバックエンドは、商品閲覧からメール認証付きの会員登録、カート、模擬決済による注文、ポイント精算、購入者レビューまでを REST API で提供します。一般会員は自分の注文やポイント履歴を確認でき、管理者は商品情報と在庫を管理できます。

バックエンドは Java 21、Spring Boot 4.1.2、Spring Security、Spring Data JPA（Hibernate）、Maven で構成されています。標準のデータベースは Oracle で、ローカルデモと自動テストでは H2 を使用します。画面は兄弟ディレクトリ `../front` の Vue 3 アプリにあり、開発時は Vite が `/api` をバックエンドに転送します。

決済は模擬専用です。カード番号の収集や、クレジットカード・PayPay・Toss Payments などの決済サービスとの通信は行いません。

## 環境設定と起動

### 1. 必要なツールと実行モード

- バックエンドには JDK 21 と Maven が必要です。Maven Wrapper は同梱されていません。コマンドラインで Maven を使えない場合は IntelliJ IDEA の内蔵 Maven を利用できます。
- Oracle モードでは Oracle Database と、SQL スクリプトを実行できるクライアント（例として SQL Developer）が必要です。
- 実際にメールを送って新規会員登録を試す場合は SMTP の設定が必要です。

| プロファイル | データベース | 用途 |
| --- | --- | --- |
| 未指定 | Oracle | 標準モード。初回起動前にスキーマを手動で作成します。 |
| `demo` | H2 ファイル DB（Oracle 互換モード） | Oracle を使わずに API や画面を確認できます。作業ディレクトリを `back` にすると `back/data/mall.mv.db` に保存され、再起動後もデータが残ります。Oracle とのデータ移行は自動では行われません。 |
| `test` | H2 インメモリ DB（Oracle 互換モード） | 自動テスト専用です。テスト用の管理者設定は実環境で使わないでください。 |

### 2. Oracle の初期設定

1. SQL Developer で、例として `localhost:1521/FREEPDB1` に `SYSTEM` で接続します。`FREEPDB1` はこのプロジェクトの既定の PDB サービスです。環境に応じて接続先を変更してください。Oracle Free のルートサービス `FREE` とは接続先が異なります。
2. 接続先の PDB を確認し、アプリ専用の `mall` ユーザーが存在しない場合に限り、次の SQL で作成します。例のパスワードは自分で決めた値に置き換え、ソースや Git に保存しないでください。

   ```sql
   SELECT SYS_CONTEXT('USERENV', 'CON_NAME') AS container_name FROM dual;

   CREATE USER mall IDENTIFIED BY "ReplaceWithYourOwnPassword"
     DEFAULT TABLESPACE users QUOTA 100M ON users;
   GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO mall;
   ```

3. `mall` で同じ PDB に接続します。**新規環境**では [schema-oracle.sql](src/main/resources/db/schema-oracle.sql) を SQL Developer のスクリプト実行（F5）で一度実行します。初期スキーマには現行の定義が含まれます。
4. **既存環境**では、導入済みの変更を確認し、未適用で対象となる次の SQL をバージョン順に手動実行します。

   | SQL | 変更内容 |
   | --- | --- |
   | [V2__email_verification_oracle.sql](src/main/resources/db/migrations/V2__email_verification_oracle.sql) | メール認証と送信制限のテーブル、会員のメール認証日時列を追加します。 |
   | [V3__japanese_store_oracle.sql](src/main/resources/db/migrations/V3__japanese_store_oracle.sql) | 旧初期データに一致する管理者表示名とサンプル商品を日本語表記に更新します。 |
   | [V4__yen_only_oracle.sql](src/main/resources/db/migrations/V4__yen_only_oracle.sql) | 日本円固定に伴い通貨識別列を削除します。スクリプトは `MALL`／`FREEPDB1` への接続と既存データの条件を確認します。金額の換算は行いません。 |

Oracle モードでは SQL の自動初期化を無効にしています。起動時に Hibernate の `ddl-auto=validate` がエンティティとスキーマの整合性を検証します。JPA への移行自体に追加 SQL は不要です。H2 デモでは起動時に `schema-h2.sql` を実行します。

### 3. 環境変数

IntelliJ IDEA の実行構成に設定するか、Maven や Java を起動するシェルの環境変数として設定します。`.env` ファイルを置くだけでは Spring Boot に読み込まれません。

| 変数 | 既定値・設定内容 |
| --- | --- |
| `DB_URL` | `jdbc:oracle:thin:@localhost:1521/FREEPDB1`。Oracle の接続先に合わせます。 |
| `DB_USERNAME` | `mall`。Oracle のアプリ用ユーザーです。 |
| `DB_PASSWORD` | 既定は空欄。Oracle ユーザーのパスワードを設定します。 |
| `PORT` | `8080`。バックエンドの HTTP ポートです。 |
| `COOKIE_SECURE` | `false`。HTTPS で運用する場合は `true` に設定します。 |
| `MALL_ADMIN_EMAIL` | 初回作成する管理者のメールアドレスです。管理者が不要なら設定を省略できます。 |
| `MALL_ADMIN_PASSWORD` | 新規管理者のログイン用パスワードです。10〜64文字かつ UTF-8 で72バイト以内です。 |
| `MAIL_HOST` | SMTP サーバーのホスト名です。Gmail の例は `smtp.gmail.com` です。 |
| `MAIL_PORT` | `587`。SMTP サーバーに合わせて設定します。 |
| `MAIL_USERNAME` | SMTP 認証用のユーザー名です。Gmail では送信用アドレスを指定します。 |
| `MAIL_PASSWORD` | SMTP 認証用のパスワードです。Gmail では発行したアプリパスワードを使用します。 |
| `MAIL_FROM` | 送信元アドレスです。省略時は `MAIL_USERNAME` を使用します。 |
| `MAIL_SMTP_AUTH` / `MAIL_STARTTLS` | いずれも既定は `true`。SMTP 認証と STARTTLS の設定です。 |

指定したメールアドレスの管理者がすでに存在する場合、起動時にパスワードは上書きされません。同じアドレスが一般会員に使われている場合は、権限を変更せず起動を中断します。作成済みの管理者は設定を外しても DB に残ります。商品テーブルが空なら、起動時に日本語のサンプル商品が6件登録されます。

SMTP 設定は Oracle とデモのどちらでも新規会員登録に必要です。Gmail のアプリパスワードの準備、疎通確認、送信エラーの確認手順は [Gmail の設定手順](docs/GMAIL_SETUP.md) を参照してください。DB や SMTP のパスワードはバックエンド側で管理し、フロントエンドの `VITE_*` 変数には設定しないでください。

### 4. PowerShell から起動・確認

以下の Maven コマンドは `pom.xml` のある `back` で実行します。

```powershell
java -version
mvn -version
mvn test
```

Oracle を用意せずに起動する場合は、次のコマンドを実行します。

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Oracle を使う場合は、接続先とパスワードを設定し、プロファイルを指定せずに起動します。パスワードはプロンプトから入力します。

```powershell
$env:DB_URL = 'jdbc:oracle:thin:@localhost:1521/FREEPDB1'
$env:DB_USERNAME = 'mall'
$env:DB_PASSWORD = (Get-Credential -UserName 'mall' -Message 'Oracle のパスワードを入力').GetNetworkCredential().Password
mvn spring-boot:run
```

別のターミナルで稼働確認を行います。既定ポートでは `status` が `UP`、`paymentMode` が `MOCK` と返ります。

```powershell
Invoke-RestMethod 'http://localhost:8080/api/health'
```

JAR を作成して起動する場合は、次のコマンドを使います。Oracle で起動する場合は環境変数を設定し、`--spring.profiles.active=demo` を外します。

```powershell
mvn clean package
java -jar target/shopping-mall-0.1.0-SNAPSHOT.jar --spring.profiles.active=demo
```

画面の起動手順はフロントエンド側に記載しています。 [フロントエンド README](../front/README.md)

### 5. IntelliJ IDEA から起動

1. `back/pom.xml` を Maven プロジェクトとして開き、Maven ツールウィンドウからプロジェクトを再読み込みします。
2. Project SDK と実行環境の JRE に JDK 21 を指定します。
3. `Run → Edit Configurations` で `ShoppingMallApplication` の実行構成を作成し、作業ディレクトリを `back` に設定します。
4. `Environment variables` に必要な環境変数を入力します。`Active profiles` は Oracle なら空欄、H2 なら `demo` にします。
5. `ShoppingMallApplication.main` を実行し、`/api/health` の応答を確認します。テストは Maven ツールウィンドウの `Lifecycle → test` から実行できます。

## プロジェクト構成

```text
back/
├── pom.xml                                  Maven の依存関係とビルド設定
├── README.md
├── docs/
│   ├── ARCHITECTURE.md                      API・DB 構成と手動確認の手順
│   ├── GMAIL_SETUP.md                       Gmail／SMTP によるメール認証の設定
│   └── JPA_STRUCTURE.md                     JPA 実装とコードの読み方
└── src/
    ├── main/
    │   ├── java/com/example/shoppingmall/
    │   │   ├── ShoppingMallApplication.java   Spring Boot の起動クラス
    │   │   ├── auth/                        会員、認証、権限、メール認証
    │   │   ├── product/                     商品の公開参照と管理者の更新操作
    │   │   ├── cart/                        カートとお気に入り
    │   │   ├── order/                       注文、決済、在庫、ポイント、取消
    │   │   ├── review/                      購入者レビュー
    │   │   ├── health/                      稼働確認 API
    │   │   └── common/                      共通エンティティ、エラー応答、初期データ、日本語設定
    │   └── resources/
    │       ├── application.yml              共通設定と Oracle の接続設定
    │       ├── application-demo.yml         H2 ファイル DB の設定
    │       ├── ValidationMessages.properties 入力検証メッセージ
    │       └── db/
    │           ├── schema-oracle.sql        Oracle の新規スキーマ
    │           ├── schema-h2.sql            H2 の初期化
    │           ├── recover-initial-schema-oracle.sql  Oracle の初期スキーマ復旧用
    │           └── migrations/             既存 Oracle 環境向けの変更 SQL
    └── test/
        ├── java/com/example/shoppingmall/   統合テストと商品サービスのテスト
        └── resources/application-test.yml  H2 インメモリ DB の設定
```

HTTP リクエストは Controller が受け取り、Service が業務ルールとトランザクションを管理します。Repository は JPA エンティティを介して DB にアクセスします。11個のテーブルをエンティティと `JpaRepository` に対応させています。API の入出力には DTO を使います。たとえば `ProductEntity` は DB マッピング用、`Product` レコードは商品 API の応答用です。本体の DB アクセスは Repository が担当し、テストの `JdbcTemplate` は DB の結果確認や障害の再現に利用します。 [JPA 実装の詳細](docs/JPA_STRUCTURE.md)・[API とデータベースの構成](docs/ARCHITECTURE.md)

## データベース構成

| テーブル | 保存内容と主な制約 |
| --- | --- |
| `mall_members` | 会員のメールアドレス、パスワードハッシュ、権限、ポイント残高、メール認証日時。メールアドレスは一意です。 |
| `mall_products` | 商品名、カテゴリー、円価格、在庫、販売状態と画像情報。価格と在庫に範囲制約があります。 |
| `mall_cart` | 会員と商品を複合主キーとするカート明細。数量は1〜99です。 |
| `mall_wishlist` | 会員と商品を複合主キーとするお気に入り。 |
| `mall_orders` | 会員、配送先、注文状態、金額、ポイント、リクエストキー。キーは会員ごとに一意で、商品合計は利用ポイントと支払額の合計に一致します。 |
| `mall_order_items` | 注文ごとの商品、注文時の商品名、単価、数量。 |
| `mall_payments` | 模擬決済・返金の種別、金額、参照番号。注文ごとに決済と返金をそれぞれ1件まで記録できます。 |
| `mall_point_ledger` | 注文に伴うポイントの利用・付与・返還・回収と、処理後の残高。 |
| `mall_reviews` | 会員による商品の評価と本文。会員と商品の組み合わせは一意です。 |
| `mall_email_verifications` | メールごとの確認コードとセッションのハッシュ、有効期限、使用日時、送信・入力回数。 |
| `mall_email_send_limits` | 送信元 IP のハッシュと時間枠内の送信回数。 |

注文は会員にひもづき、注文 ID で商品明細、模擬決済、ポイント履歴をたどれます。Oracle の定義では、会員別の注文・ポイント履歴、注文別の商品明細、商品別レビューの参照にインデックスを用意しています。 [Oracle スキーマ定義](src/main/resources/db/schema-oracle.sql)

Oracle のデータを確認する場合は、SQL Developer で `mall` ユーザーに接続して次の参照クエリを実行します。

```sql
SELECT id, name, price, stock, active FROM mall_products ORDER BY id;
SELECT id, member_id, status, subtotal, points_used, amount_paid
FROM mall_orders ORDER BY id DESC;
SELECT order_id, kind, amount, balance_after
FROM mall_point_ledger ORDER BY id DESC;
```

## 実装済みの機能

| 機能 | 実装内容 |
| --- | --- |
| メール認証と会員登録 | SMTP で6桁の確認コードを送信します。コードは5分間有効で、再送間隔は60秒、送信上限は1時間にメールアドレスあたり5回、送信元 IP あたり20回です。コード入力は送信ごとに最大5回です。確認後10分以内に、同じメールアドレスとブラウザーセッションで1回登録できます。 |
| ログインと権限管理 | パスワードは BCrypt でハッシュ化します。セッションログイン、ログアウト、ログイン中の会員情報取得、CSRF 対策、一般会員と管理者のアクセス制御を実装しています。セッションはサーバーのメモリに保持されるため、再起動後は再ログインが必要です。 |
| 商品閲覧と管理 | 販売中の商品を一覧・詳細で公開します。管理者は商品を登録し、価格や在庫などを変更し、販売を停止できます。販売停止時は `active=0` にして商品レコードを保持します。 |
| カートとお気に入り | 会員ごとに DB に保存します。カートでは商品を追加し、商品ごとの数量を指定し、削除できます。お気に入りは一覧、追加、解除に対応します。 |
| 注文と模擬決済 | 受取人・電話番号・配送先を入力し、カート全体を注文します。送料は無料です。決済時点の価格と在庫をサーバーで確認し、`MOCK_CARD` または `MOCK_PAY` で模擬決済を記録します。注文時の商品名と単価を明細に保存します。 |
| 二重注文と在庫の保護 | 同じ会員が同じ `requestKey` と注文内容で再送すると、既存の注文を返します。同じキーで内容が異なる場合は HTTP 409 を返します。会員と商品に行ロックをかけ、在庫更新、決済記録、注文保存、ポイント更新、カートの消去を同一トランザクションで実行します。 |
| ポイント | 金額は整数の日本円（JPY）で、1ポイントは1円です。利用上限は利用できる正の残高と商品合計の小さい方です。ポイント利用後の模擬決済額の1%を、小数点以下切り捨てで付与します。利用・付与・取消時の精算は履歴に記録します。 |
| 注文履歴と全体取消 | 会員は自分の注文一覧と明細を参照できます。注文全体を取り消すと、在庫を戻し、模擬返金を記録し、利用ポイントを返還して付与ポイントを取り消します。付与ポイントをすでに使っている場合、残高がマイナスになることがあります。以後の付与で相殺され、正の残高を利用できます。 |
| 商品レビュー | レビュー一覧は公開されます。購入済みの商品に、会員1人につき1件のレビューを投稿できます。評価は1〜5、本文は1,000文字以内です。投稿者は自分のレビューを削除でき、投稿後に注文を取り消しても既存のレビューは残ります。 |
| 日本語と稼働確認 | 初期商品、入力検証メッセージ、API の代表的なエラー応答を日本語で提供します。`/api/health` でアプリの稼働状態と模擬決済モードを確認できます。 |

## 主な API

| メソッド | パス | 用途 |
| --- | --- | --- |
| GET | `/api/health`、`/api/auth/csrf` | 稼働確認と CSRF トークンの取得 |
| POST | `/api/auth/email/send`、`/api/auth/email/verify`、`/api/auth/register` | 確認コード送信・照合・会員登録 |
| POST | `/api/auth/login`、`/api/auth/logout` | セッションの開始と終了 |
| GET | `/api/auth/me` | ログイン中の会員情報 |
| GET | `/api/products`、`/api/products/{id}` | 商品一覧と詳細 |
| POST / PUT / DELETE | `/api/admin/products`（POST）、`/api/admin/products/{id}`（PUT・DELETE） | 管理者による商品登録・更新・販売停止 |
| GET / PUT / DELETE | `/api/cart`（GET・PUT）、`/api/cart/{productId}`（DELETE） | 自分のカートの参照・数量設定・削除 |
| GET / PUT / DELETE | `/api/wishlist`（GET）、`/api/wishlist/{productId}`（PUT・DELETE） | 自分のお気に入りの参照・追加・解除 |
| GET | `/api/orders`、`/api/orders/{orderId}`、`/api/points` | 自分の注文一覧・注文詳細・ポイント履歴 |
| POST | `/api/orders/checkout`、`/api/orders/{orderId}/cancel` | 注文確定と全体取消 |
| GET / POST / DELETE | `/api/products/{productId}/reviews`（GET・POST）、`/api/reviews/{reviewId}`（DELETE） | レビューの閲覧・投稿・削除 |

API を直接呼び出す場合は、`GET /api/auth/csrf` が返す `headerName` と `token` を変更リクエストのヘッダーに設定し、同じセッションの Cookie を送信します。ログインは `application/x-www-form-urlencoded` で `email` と `password` を送信します。会員登録や注文など、リクエスト本文を受け取る他の API では JSON を使用します。ログインやログアウト後は CSRF トークンを取得し直します。

## テストと現在の提供範囲

`mvn test` では、H2 の Oracle 互換モードを使い、会員権限と CSRF、メール認証、二重注文、同時注文時の在庫、トランザクションのロールバック、ポイント精算、レビュー権限などを確認します。メール送信のテストはローカル SMTP サーバーを使用します。実際の Oracle への接続や Gmail での受信は、それぞれの環境で別途確認してください。

現行版はローカル学習用の MVP です。大規模な一覧のページング、パスワード再設定、ログイン試行回数の制限、配送業務、注文の一部取消、画像アップロード、外部決済の承認・Webhook・返金照合は未実装です。公開環境に配置する場合は、HTTPS、`COOKIE_SECURE=true`、同一オリジンの `/api` プロキシ、データベースや SMTP の認証情報の管理が必要です。
