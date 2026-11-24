-- MALL / FREEPDB1でF5実行。通貨はJPY固定のため未使用の通貨識別カラムを削除します。
-- 商品価格・注文金額・会員・ポイント残高・履歴は削除/換算しません。
DECLARE
  invalid_rows NUMBER;
  FUNCTION has_column(p_table VARCHAR2,p_column VARCHAR2) RETURN BOOLEAN IS
    found NUMBER;
  BEGIN
    SELECT COUNT(*) INTO found FROM user_tab_columns WHERE table_name=p_table AND column_name=p_column;
    RETURN found>0;
  END;
BEGIN
  IF USER <> 'MALL' OR SYS_CONTEXT('USERENV','CON_NAME') <> 'FREEPDB1' THEN
    RAISE_APPLICATION_ERROR(-20001,'Connect as MALL to FREEPDB1.');
  END IF;
  -- 変更前に、金額の意味が変わる既存データがないことを確認します。
  IF has_column('MALL_ORDERS','CURRENCY') THEN
    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM mall_orders WHERE currency<>''JPY''' INTO invalid_rows;
    IF invalid_rows>0 THEN
      RAISE_APPLICATION_ERROR(-20002,'Non-JPY orders exist. No columns were removed. Review existing data first.');
    END IF;
  END IF;
  IF has_column('MALL_MEMBERS','POINT_CURRENCY') THEN
    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM mall_members WHERE point_currency<>''JPY'' AND points<>0' INTO invalid_rows;
    IF invalid_rows>0 THEN
      RAISE_APPLICATION_ERROR(-20003,'Non-JPY point balances exist. No columns were removed. Review existing data first.');
    END IF;
  END IF;
  IF has_column('MALL_PRODUCTS','CURRENCY') THEN
    EXECUTE IMMEDIATE 'ALTER TABLE mall_products DROP COLUMN currency';
  END IF;
  IF has_column('MALL_ORDERS','CURRENCY') THEN
    EXECUTE IMMEDIATE 'ALTER TABLE mall_orders DROP COLUMN currency';
  END IF;
  IF has_column('MALL_MEMBERS','POINT_CURRENCY') THEN
    EXECUTE IMMEDIATE 'ALTER TABLE mall_members DROP COLUMN point_currency';
  END IF;
END;
/
SELECT id,name,price,stock FROM mall_products ORDER BY id;
