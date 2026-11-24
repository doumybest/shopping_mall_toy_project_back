# 日本語・日本円ストア

サイト、APIエラー、認証メール、管理画面の既定言語は日本語です。
金額はすべて円（JPY）の整数、1ポイント＝1円、決済金額の1%を獲得します。
日付は日本時間（Asia/Tokyo）で表示します。

## 既存DBの整理

以前のV3で通貨識別カラムを追加した場合は、バックエンドを停止し、SQL Developerの
**MALL / FREEPDB1** 接続で `src/main/resources/db/migrations/V4__yen_only_oracle.sql` をF5実行してください。
カラムがない場合はスキップするので、V3を実行していないDBでも同じ手順で構いません。
商品・会員・注文・ポイント残高を削除する処理はありません。価格の数値も保持します。
実際の販売価格は商品管理画面で円単位の金額を設定してください。

新規DBは最新の `schema-oracle.sql` を使います。通貨識別カラムの追加やV4の実行は不要です。
H2 demoプロファイルでは起動時に未使用カラムを整理します。
初期サンプルの日本語表記更新だけが必要な場合はV3を実行できます。

## 起動

1. IntelliJでbackのMaven設定を再読み込みし、ShoppingMallApplicationを起動
2. frontで `pnpm.cmd dev` を実行
3. 管理者ログイン → 商品管理で価格・在庫を設定

DB/SMTP環境変数とログイン情報は従来どおりです。
メール認証・モック決済・注文キャンセル・ポイント返還を引き続き利用できます。
JPY以外の価格設定、通貨切替、移行待ち表示、通貨を理由にした購入制限はありません。
決済はテスト専用で、実際の請求やPayPayへの接続は行いません。

## 価格・日時

- 空のDBには日本語のサンプル6件を登録します。1800円などのデモ価格です。
- 税額の自動計算はなく、登録価格をそのまま決済金額として扱います。
- ユーザー名、レビュー、配送先などの入力済みデータは自動翻訳しません。
- 過去のオフセットなし日時は日本/韓国時間（UTC+09:00）として表示します。

```sql
SELECT id,name,price,stock FROM mall_products ORDER BY id;
SELECT id,status,amount_paid FROM mall_orders ORDER BY id DESC;
SELECT id,email,points FROM mall_members ORDER BY id;
```
