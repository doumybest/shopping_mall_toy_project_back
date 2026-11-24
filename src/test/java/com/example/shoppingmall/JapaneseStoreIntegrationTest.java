package com.example.shoppingmall;

import com.example.shoppingmall.auth.MemberService;
import com.example.shoppingmall.cart.CartService;
import com.example.shoppingmall.order.OrderService;
import com.example.shoppingmall.product.ProductService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class JapaneseStoreIntegrationTest {
    @Autowired MemberService members;
    @Autowired ProductService products;
    @Autowired CartService carts;
    @Autowired OrderService orders;

    MemberService.Member member() {
        return members.create(new MemberService.Registration(UUID.randomUUID()+"@test.example","TestMember!123","山田 太郎"),"MEMBER");
    }
    @Test void yenProductCanBePurchasedWithoutCurrencySetup() {
        var member=member();
        var product=products.create(new ProductService.Edit("マグカップ","暮らし・雑貨",1800,"テスト商品です。","☕","",3));
        carts.put(member.id(),new CartService.Change(product.id(),1));
        var order=orders.checkout(member.id(),checkout());
        assertThat(order.amountPaid()).isEqualTo(1800);
        assertThat(order.pointsEarned()).isEqualTo(18);
        assertThat(order.phone()).isEqualTo("090-1234-5678");
    }
    @Test void cancellationRestoresYenPointsWithoutMigrationGate() {
        var member=member();
        var product=products.create(new ProductService.Edit("マグカップ","暮らし・雑貨",1000,"テスト商品です。","☕","",3));
        carts.put(member.id(),new CartService.Change(product.id(),1));
        var order=orders.checkout(member.id(),checkout());
        assertThat(members.byId(member.id()).points()).isEqualTo(10);
        assertThat(orders.cancel(member.id(),order.id()).status()).isEqualTo("CANCELLED");
        assertThat(members.byId(member.id()).points()).isZero();
        assertThat(products.findById(product.id()).stock()).isEqualTo(3);
    }
    @Test void h2CleanupRemovesUnusedColumnsAndPreservesData() throws Exception {
        var dataSource=new SingleConnectionDataSource("jdbc:h2:mem:cleanup_"+UUID.randomUUID()+";MODE=Oracle;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","",true);
        try {
            var source=new ClassPathResource("db/schema-h2.sql");
            new ResourceDatabasePopulator(source).execute(dataSource);
            var db=new JdbcTemplate(dataSource);
            db.execute("ALTER TABLE mall_members ADD point_currency VARCHAR2(3) DEFAULT 'JPY'");
            db.execute("ALTER TABLE mall_products ADD currency VARCHAR2(3) DEFAULT 'JPY'");
            db.execute("ALTER TABLE mall_orders ADD currency VARCHAR2(3) DEFAULT 'JPY'");
            db.update("INSERT INTO mall_members(email,password_hash,name,role,points) VALUES('sample@test.example','not-a-real-hash','会員','MEMBER',100)");
            db.update("INSERT INTO mall_products(name,category,price,description,emoji,stock,active) VALUES('商品','暮らし・雑貨',1800,'商品です。','☕',3,1)");
            new ResourceDatabasePopulator(source).execute(dataSource);
            assertThat(db.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='public' AND column_name IN ('currency','point_currency')",Integer.class)).isZero();
            assertThat(db.queryForObject("SELECT points FROM mall_members WHERE email='sample@test.example'",Long.class)).isEqualTo(100);
            assertThat(db.queryForObject("SELECT price FROM mall_products WHERE name='商品'",Long.class)).isEqualTo(1800);
            new ResourceDatabasePopulator(source).execute(dataSource);
            assertThat(db.queryForObject("SELECT COUNT(*) FROM mall_products",Integer.class)).isEqualTo(1);
        } finally { dataSource.destroy(); }
    }
    private OrderService.Checkout checkout() {
        return new OrderService.Checkout(UUID.randomUUID().toString(),0,"山田 太郎","０９０－１２３４－５６７８","東京都渋谷区神宮前1-2-3","MOCK_CARD");
    }
}
