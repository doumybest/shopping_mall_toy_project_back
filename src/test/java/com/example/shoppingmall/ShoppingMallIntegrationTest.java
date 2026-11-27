package com.example.shoppingmall;

import com.example.shoppingmall.auth.MemberService;
import com.example.shoppingmall.cart.CartService;
import com.example.shoppingmall.order.OrderService;
import com.example.shoppingmall.product.*;
import com.example.shoppingmall.review.ReviewService;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ShoppingMallIntegrationTest {
    @DynamicPropertySource static void mail(DynamicPropertyRegistry registry) { TestSmtpServer.configure(registry); }
    @Autowired MemberService members;
    @Autowired ProductService products;
    @Autowired CartService carts;
    @Autowired OrderService orders;
    @Autowired ReviewService reviews;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @LocalServerPort int port;
    private static final String PASSWORD="MemberTest!123";
    MemberService.Member member() {
        return members.create(new MemberService.Registration(UUID.randomUUID()+"@test.example",PASSWORD,"테스터"),"MEMBER");
    }
    Product product(long price,int stock) { return products.create(new ProductService.Edit("테스트 商品","暮らし・雑貨",price,"商品説明","☕","",stock)); }
    OrderService.Checkout checkout(long points) { return new OrderService.Checkout(UUID.randomUUID().toString(),points,"테스터","010-1234-5678","東京都渋谷区神宮前1-2-3","MOCK_CARD"); }

    @Test void memberCouponAndShippingFlowUsesOrderTotalsAndEnforcesRoles() throws Exception {
        var member = member();
        var memberBrowser = new Browser();
        var admin = new Browser();
        var guest = new Browser();
        guest.csrf();
        assertThat(guest.send("GET", "/api/admin/events", null, false).statusCode()).isEqualTo(401);
        memberBrowser.csrf();
        assertThat(memberBrowser.login(member.email(), PASSWORD).statusCode()).isEqualTo(200);
        memberBrowser.csrf();
        admin.csrf();
        assertThat(admin.login("admin@test.example", "TestAdmin!12345").statusCode()).isEqualTo(200);
        admin.csrf();

        var now = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Tokyo"));
        var title = "イベント " + UUID.randomUUID();
        var event = new com.example.shoppingmall.event.EventService.EventInput(
            title, "会員クーポン", now.minusHours(1), now.plusHours(1), 300, true);
        assertThat(memberBrowser.send("POST", "/api/admin/events", event, true).statusCode()).isEqualTo(403);
        assertThat(admin.send("POST", "/api/admin/events", event, false).statusCode()).isEqualTo(403);
        assertThat(admin.send("POST", "/api/admin/events", event, true).statusCode()).isEqualTo(204);
        var hiddenTitle = "非公開 " + UUID.randomUUID();
        var hidden = new com.example.shoppingmall.event.EventService.EventInput(
            hiddenTitle, "非公開", now.minusHours(1), now.plusHours(1), 300, false);
        assertThat(admin.send("POST", "/api/admin/events", hidden, true).statusCode()).isEqualTo(204);
        var response = guest.send("GET", "/api/events", null, false);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(title).doesNotContain(hiddenTitle, "\"startsAt\"", "\"enabled\"");
        long eventId = jdbc.queryForObject("SELECT id FROM mall_events WHERE title = ?", Long.class, title);
        var claim = "/api/events/" + eventId + "/coupon";
        assertThat(guest.send("POST", claim, null, true).statusCode()).isEqualTo(401);
        assertThat(memberBrowser.send("POST", claim, null, true).statusCode()).isEqualTo(204);
        assertThat(memberBrowser.send("POST", claim, null, true).statusCode()).isEqualTo(409);
        assertThat(memberBrowser.send("GET", "/api/coupons/claimed-events", null, false).body()).isEqualTo("[" + eventId + "]");
        assertThat(guest.send("GET", "/api/coupons/claimed-events", null, false).statusCode()).isEqualTo(401);
        assertThat(guest.send("GET", "/api/events/" + eventId, null, false).statusCode()).isEqualTo(200);
        assertThat(guest.send("GET", "/api/events/" + eventId + "/image", null, false).statusCode()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM mall_event_coupons WHERE event_id = ?", Integer.class, eventId)).isEqualTo(1);

        var product = product(1000, 3);
        carts.put(member.id(), new CartService.Change(product.id(), 1));
        var body = new OrderService.Checkout(UUID.randomUUID().toString(), 0,
            "テスター", "010-1234-5678", "東京都渋谷区神宮前1-2-3", "MOCK_CARD", eventId);
        var order = orders.checkout(member.id(), body);
        assertThat(order.couponDiscount()).isEqualTo(300);
        assertThat(order.amountPaid()).isEqualTo(700);
        assertThat(order.pointsEarned()).isEqualTo(7);
        assertThat(orders.checkout(member.id(), body).id()).isEqualTo(order.id());
        assertThat(memberBrowser.send("GET", "/api/coupons", null, false).body()).isEqualTo("[]");
        orders.cancel(member.id(), order.id());
        assertThat(memberBrowser.send("GET", "/api/coupons", null, false).body()).contains(title);
        String eventPath = "/api/admin/events/" + eventId;
        assertThat(memberBrowser.send("DELETE", eventPath, null, true).statusCode()).isEqualTo(403);
        assertThat(admin.send("DELETE", eventPath, null, false).statusCode()).isEqualTo(403);
        assertThat(admin.send("DELETE", eventPath, null, true).statusCode()).isEqualTo(204);
        assertThat(guest.send("GET", "/api/events/" + eventId, null, false).statusCode()).isEqualTo(404);
        assertThat(guest.send("GET", "/api/events", null, false).body()).doesNotContain(title);
        assertThat(admin.send("GET", "/api/admin/events", null, false).body()).doesNotContain(title);
        assertThat(admin.send("PUT", eventPath, event, true).statusCode()).isEqualTo(404);
        assertThat(memberBrowser.send("GET", "/api/coupons", null, false).body()).contains(title);

        carts.put(member.id(), new CartService.Change(product.id(), 1));
        var second = orders.checkout(member.id(), new OrderService.Checkout(UUID.randomUUID().toString(), 0,
            body.recipient(), body.phone(), body.address(), body.paymentMethod(), eventId));
        var shipping = "/api/admin/orders/" + second.id() + "/shipping";
        assertThat(memberBrowser.send("PUT", shipping, Map.of("shippingStatus", "SHIPPED"), true).statusCode()).isEqualTo(403);
        assertThat(admin.send("GET", "/api/admin/orders", null, false).statusCode()).isEqualTo(200);
        assertThat(admin.send("PUT", shipping, Map.of("shippingStatus", "DELIVERED"), true).statusCode()).isEqualTo(409);
        assertThat(admin.send("PUT", shipping, Map.of("shippingStatus", "SHIPPED"), true).statusCode()).isEqualTo(200);
        assertThat(memberBrowser.send("GET", "/api/orders/" + second.id(), null, false).body()).contains("SHIPPED");
        assertThat(memberBrowser.send("POST", "/api/orders/" + second.id() + "/cancel", null, true).statusCode()).isEqualTo(409);
        assertThat(admin.send("PUT", shipping, Map.of("shippingStatus", "DELIVERED"), true).statusCode()).isEqualTo(200);
    }

    @Test void purchasePersistsSnapshotPaymentStockAndPoints() {
        var member=member(); var product=product(18000,10);
        carts.put(member.id(),new CartService.Change(product.id(),2));
        var order=orders.checkout(member.id(),checkout(0));
        assertThat(order.amountPaid()).isEqualTo(36000);
        assertThat(order.pointsEarned()).isEqualTo(360);
        assertThat(members.byId(member.id()).points()).isEqualTo(360);
        assertThat(products.findById(product.id()).stock()).isEqualTo(8);
        assertThat(carts.list(member.id())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM mall_payments WHERE order_id=?",Integer.class,order.id())).isEqualTo(1);
        products.update(product.id(),new ProductService.Edit("編集된 商品","ファッション",999,"새 설명","👜","",8));
        assertThat(orders.detail(member.id(),order.id()).items().getFirst().unitPrice()).isEqualTo(18000);
        assertThat(orders.detail(member.id(),order.id()).items().getFirst().name()).isEqualTo("테스트 商品");
    }
    @Test void pointsSpendAndCancellationAreIdempotent() {
        var member=member(); var product=product(10000,10);
        carts.put(member.id(),new CartService.Change(product.id(),1));
        var first=orders.checkout(member.id(),checkout(0));
        assertThat(members.byId(member.id()).points()).isEqualTo(100);
        carts.put(member.id(),new CartService.Change(product.id(),1));
        var second=orders.checkout(member.id(),checkout(100));
        assertThat(second.amountPaid()).isEqualTo(9900);
        assertThat(members.byId(member.id()).points()).isEqualTo(99);
        orders.cancel(member.id(),second.id());orders.cancel(member.id(),second.id());
        assertThat(members.byId(member.id()).points()).isEqualTo(100);
        assertThat(products.findById(product.id()).stock()).isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM mall_payments WHERE order_id=? AND kind='REFUND'",Integer.class,second.id())).isEqualTo(1);
        orders.cancel(member.id(),first.id());
        assertThat(members.byId(member.id()).points()).isZero();
        assertThat(products.findById(product.id()).stock()).isEqualTo(10);
    }
    @Test void fullPointsPurchaseAndSpentRewardsCancellation() {
        var member=member();var expensive=product(10000,2);var cheap=product(100,3);
        carts.put(member.id(),new CartService.Change(expensive.id(),1));
        var first=orders.checkout(member.id(),checkout(0));
        carts.put(member.id(),new CartService.Change(cheap.id(),1));
        var second=orders.checkout(member.id(),checkout(100));
        assertThat(second.amountPaid()).isZero();assertThat(second.pointsEarned()).isZero();
        orders.cancel(member.id(),first.id());
        assertThat(members.byId(member.id()).points()).isEqualTo(-100);
        carts.put(member.id(),new CartService.Change(cheap.id(),1));
        assertThatThrownBy(() -> orders.checkout(member.id(),checkout(1))).isInstanceOf(ResponseStatusException.class);
        orders.cancel(member.id(),second.id());
        assertThat(members.byId(member.id()).points()).isZero();
    }
    @Test void insufficientPointsAndStockDoNotMutateOrderState() {
        var member=member();var product=product(1000,1);
        carts.put(member.id(),new CartService.Change(product.id(),1));
        assertThatThrownBy(() -> orders.checkout(member.id(),checkout(1))).isInstanceOf(ResponseStatusException.class);
        assertThat(orders.list(member.id())).isEmpty();
        assertThat(products.findById(product.id()).stock()).isEqualTo(1);
        products.update(product.id(),new ProductService.Edit("商品","暮らし・雑貨",1000,"설명","☕","",0));
        assertThatThrownBy(() -> orders.checkout(member.id(),checkout(0))).isInstanceOf(ResponseStatusException.class);
        assertThat(carts.list(member.id())).hasSize(1);
    }
    @Test void duplicateCheckoutAndForeignOrderAreProtected() {
        var member=member();var stranger=member();var product=product(1000,3);
        carts.put(member.id(),new CartService.Change(product.id(),1));
        var body=checkout(0);var order=orders.checkout(member.id(),body);
        assertThat(orders.checkout(member.id(),body).id()).isEqualTo(order.id());
        assertThat(orders.list(member.id())).hasSize(1);
        assertThat(products.findById(product.id()).stock()).isEqualTo(2);
        assertThatThrownBy(() -> orders.detail(stranger.id(),order.id())).isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        assertThatThrownBy(() -> orders.cancel(stranger.id(),order.id())).isInstanceOf(ResponseStatusException.class);
    }
    @Test void rollbackRestoresStockCartAndOrderAfterPaymentInsertFailure() {
        var member=member();var product=product(12345,2);
        carts.put(member.id(),new CartService.Change(product.id(),1));
        jdbc.execute("ALTER TABLE mall_payments ADD CONSTRAINT test_reject_amount CHECK (amount <> 12345)");
        try {
            assertThatThrownBy(() -> orders.checkout(member.id(),checkout(0))).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThat(orders.list(member.id())).isEmpty();
            assertThat(products.findById(product.id()).stock()).isEqualTo(2);
            assertThat(carts.list(member.id())).hasSize(1);
            assertThat(members.byId(member.id()).points()).isZero();
        } finally { jdbc.execute("ALTER TABLE mall_payments DROP CONSTRAINT test_reject_amount"); }
    }
    @Test void concurrentBuyersCannotOversellLastItem() throws Exception {
        var a=member();var b=member();var product=product(1000,1);
        carts.put(a.id(),new CartService.Change(product.id(),1));carts.put(b.id(),new CartService.Change(product.id(),1));
        var successes=new AtomicInteger();var conflicts=new AtomicInteger();
        var gate=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            List<Future<?>> futures=new ArrayList<>();
            for(var buyer:List.of(a,b)) futures.add(pool.submit(() -> {
                try { gate.await(); orders.checkout(buyer.id(),checkout(0));successes.incrementAndGet(); }
                catch(ResponseStatusException ex) { if(ex.getStatusCode().value()!=409) throw ex;conflicts.incrementAndGet(); }
                catch(InterruptedException ex) { throw new RuntimeException(ex); }
            }));
            gate.countDown();for(var future:futures) future.get(15,TimeUnit.SECONDS);
        }
        assertThat(successes.get()).isEqualTo(1);assertThat(conflicts.get()).isEqualTo(1);
        assertThat(products.findById(product.id()).stock()).isZero();
    }
    @Test void concurrentDuplicateRequestCreatesOnePayment() throws Exception {
        var member=member();var product=product(1000,2);var request=checkout(0);
        carts.put(member.id(),new CartService.Change(product.id(),1));
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(() -> orders.checkout(member.id(),request));var b=pool.submit(() -> orders.checkout(member.id(),request));
            assertThat(a.get(15,TimeUnit.SECONDS).id()).isEqualTo(b.get(15,TimeUnit.SECONDS).id());
        }
        assertThat(orders.list(member.id())).hasSize(1);assertThat(products.findById(product.id()).stock()).isEqualTo(1);
    }
    @Test void wishlistAndVerifiedReviewPermissions() {
        var member=member();var product=product(1000,5);
        carts.wish(member.id(),product.id());carts.wish(member.id(),product.id());assertThat(carts.wishlist(member.id())).hasSize(1);
        assertThatThrownBy(() -> reviews.write(member.id(),product.id(),new ReviewService.Write(5,"좋아요"))).isInstanceOf(ResponseStatusException.class);
        carts.put(member.id(),new CartService.Change(product.id(),1));orders.checkout(member.id(),checkout(0));
        reviews.write(member.id(),product.id(),new ReviewService.Write(5,"좋아요"));
        var review=reviews.list(product.id()).getFirst();
        assertThatThrownBy(() -> reviews.delete(member().id(),review.id())).isInstanceOf(ResponseStatusException.class);
        reviews.delete(member.id(),review.id());assertThat(reviews.list(product.id())).isEmpty();
        products.delete(product.id());assertThat(orders.list(member.id()).getFirst().items()).hasSize(1);
    }
    @Test void httpSessionCsrfAdminAndValidation() throws Exception {
        var guest=new Browser();
        assertThat(guest.send("GET","/api/cart",null,false).statusCode()).isEqualTo(401);
        assertThat(guest.send("POST","/api/auth/register",Map.of("email","x@test.example","password",PASSWORD,"name","お名前"),false).statusCode()).isEqualTo(403);
        assertThat(guest.send("POST","/api/auth/email/send",Map.of("email","x@test.example"),false).statusCode()).isEqualTo(403);
        guest.csrf();
        assertThat(guest.send("POST","/api/auth/register",Map.of("email","bad","password","short","name",""),true).statusCode()).isEqualTo(400);
        String email=UUID.randomUUID()+"@test.example";
        assertThat(guest.send("POST","/api/auth/register",Map.of("email",email,"password",PASSWORD,"name","회원"),true).statusCode()).isEqualTo(400);
        assertThat(guest.send("POST","/api/auth/email/send",Map.of("email",email),true).statusCode()).isEqualTo(200);
        String code=TestSmtpServer.INSTANCE.codeFor(email);
        var anotherBrowser=new Browser();anotherBrowser.csrf();
        assertThat(anotherBrowser.send("POST","/api/auth/email/verify",Map.of("email",email,"code",code),true).statusCode()).isEqualTo(400);
        assertThat(guest.send("POST","/api/auth/email/verify",Map.of("email",email,"code",code),true).statusCode()).isEqualTo(200);
        assertThat(guest.send("POST","/api/auth/register",Map.of("email",email,"password",PASSWORD,"name","회원","role","ADMIN"),true).statusCode()).isEqualTo(200);
        assertThat(guest.login(email,"wrong").statusCode()).isEqualTo(401);guest.csrf();
        assertThat(guest.login(email,PASSWORD).statusCode()).isEqualTo(200);guest.csrf();
        var me=guest.send("GET","/api/auth/me",null,false);
        assertThat(me.body()).contains("MEMBER").doesNotContain("password");
        assertThat(jdbc.queryForObject("SELECT password_hash FROM mall_members WHERE email=?",String.class,email)).startsWith("$2").isNotEqualTo(PASSWORD);
        var edit=Map.of("name","管理商品","category","暮らし・雑貨","price",1000,"description","설명","emoji","☕","imageUrl","","stock",2);
        assertThat(guest.send("POST","/api/admin/products",edit,true).statusCode()).isEqualTo(403);
        assertThat(guest.send("PUT","/api/cart",Map.of("productId",1,"quantity",-1),true).statusCode()).isEqualTo(400);
        assertThat(guest.send("POST","/api/orders/checkout",Map.of("requestKey",UUID.randomUUID().toString(),"pointsUsed",0,"recipient","お名前","phone","010-1234-5678","address","서울시 테스트로","paymentMethod","REAL_CARD"),true).statusCode()).isEqualTo(400);
        var admin=new Browser();admin.csrf();assertThat(admin.login("admin@test.example","TestAdmin!12345").statusCode()).isEqualTo(200);admin.csrf();
        var response=admin.send("POST","/api/admin/products",edit,true);assertThat(response.statusCode()).isEqualTo(200);
        long productId=json.readTree(response.body()).get("id").asLong();
        assertThat(admin.send("PUT","/api/admin/products/"+productId,edit,true).statusCode()).isEqualTo(200);
        assertThat(admin.send("DELETE","/api/admin/products/"+productId,null,true).statusCode()).isEqualTo(200);
        assertThat(guest.send("POST","/api/auth/logout",null,true).statusCode()).isEqualTo(204);
        assertThat(guest.send("GET","/api/auth/me",null,false).statusCode()).isEqualTo(401);
    }
    class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String token;
        void csrf() throws Exception { token=json.readTree(send("GET","/api/auth/csrf",null,false).body()).get("token").asText(); }
        HttpResponse<String> login(String email,String password) throws Exception {
            return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/auth/login"))
                .header("X-CSRF-TOKEN",token).header("Content-Type","application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("email="+URLEncoder.encode(email,java.nio.charset.StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(password,java.nio.charset.StandardCharsets.UTF_8))).build(),HttpResponse.BodyHandlers.ofString());
        }
        HttpResponse<String> send(String method,String path,Object body,boolean csrf) throws Exception {
            var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json");
            if(csrf) builder.header("X-CSRF-TOKEN",token);
            return client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
        }
    }
}
