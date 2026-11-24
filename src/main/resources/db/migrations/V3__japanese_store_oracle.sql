-- 日本語の初期サンプル表記を更新します。MALL / FREEPDB1でF5実行。
UPDATE mall_members SET name='管理者' WHERE role='ADMIN' AND name='관리자';
UPDATE mall_products SET name='デイリーセラミックマグ',category='暮らし・雑貨',description='一日のはじまりを彩る、お気に入りのカップ。' WHERE name='데일리 세라믹 머그' AND category='리빙' AND DBMS_LOB.COMPARE(description, TO_CLOB('따뜻한 하루를 시작하는 나만의 컵'))=0;
UPDATE mall_products SET name='ナチュラルキャンバスバッグ',category='ファッション',description='気軽なお出かけに、毎日持ちたくなるバッグ。' WHERE name='내추럴 캔버스 백' AND category='패션' AND DBMS_LOB.COMPARE(description, TO_CLOB('가볍게 들고 나서는 매일의 동행'))=0;
UPDATE mall_products SET name='ワイヤレスヘッドホン',category='デジタル',description='好きな音楽に、じっくり耳を傾ける時間。' WHERE name='무선 헤드폰' AND category='디지털' AND DBMS_LOB.COMPARE(description, TO_CLOB('좋아하는 음악에 집중하는 시간'))=0;
UPDATE mall_products SET name='やわらかコットンブランケット',category='暮らし・雑貨',description='くつろぎの時間に、やさしい肌ざわりを。' WHERE name='포근한 코튼 블랭킷' AND category='리빙' AND DBMS_LOB.COMPARE(description, TO_CLOB('휴식에 부드러움을 더해 주세요'))=0;
UPDATE mall_products SET name='クラシックデイリースニーカー',category='ファッション',description='どこへでも心地よく、自分らしい一歩を。' WHERE name='클래식 데일리 스니커즈' AND category='패션' AND DBMS_LOB.COMPARE(description, TO_CLOB('어디든 편안하게, 나다운 걸음'))=0;
UPDATE mall_products SET name='ミニテーブルライト',category='暮らし・雑貨',description='小さな空間を、やわらかい光で満たします。' WHERE name='미니 테이블 조명' AND category='리빙' AND DBMS_LOB.COMPARE(description, TO_CLOB('작은 공간을 채우는 은은한 빛'))=0;
COMMIT;
