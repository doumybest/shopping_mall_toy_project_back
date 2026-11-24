package com.example.shoppingmall.order;

import com.example.shoppingmall.auth.*;
import com.example.shoppingmall.cart.CartService;
import com.example.shoppingmall.product.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly=true)
public class OrderService {
    public record Checkout(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{16,64}") String requestKey,
        @Min(0) @Max(1000000000) long pointsUsed,@NotBlank @Size(max=50) String recipient,
        @NotBlank @Pattern(regexp="[0-9+() -]{8,30}") String phone,@NotBlank @Size(min=5,max=300) String address,
        @NotNull @Pattern(regexp="MOCK_CARD|MOCK_PAY") String paymentMethod) {
        public Checkout { if(phone!=null) phone=java.text.Normalizer.normalize(phone,java.text.Normalizer.Form.NFKC).trim(); }
    }
    public record Line(long productId,String name,long unitPrice,int quantity) {}
    public record Order(long id,String status,long subtotal,long pointsUsed,long amountPaid,long pointsEarned,
        String recipient,String phone,String address,String paymentMethod,String createdAt,List<Line> items) {}
    public record PointEntry(long id,long orderId,String kind,long amount,long balanceAfter,String createdAt) {}

    private final OrderRepository repository;
    private final OrderItemRepository items;
    private final PaymentRepository payments;
    private final PointLedgerRepository ledger;
    private final MemberService members;
    private final ProductService products;
    private final CartService carts;

    public OrderService(OrderRepository repository,OrderItemRepository items,PaymentRepository payments,
            PointLedgerRepository ledger,MemberService members,ProductService products,CartService carts) {
        this.repository=repository;this.items=items;this.payments=payments;this.ledger=ledger;
        this.members=members;this.products=products;this.carts=carts;
    }
    private OrderEntity owned(long memberId,long id) {
        return repository.findByIdAndMember_Id(id,memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"注文が見つかりません。"));
    }
    private List<Line> lines(long id) { return items.findLines(List.of(id)).stream().map(OrderLineView::toLine).toList(); }
    public Order detail(long memberId,long id) { return view(owned(memberId,id),lines(id)); }
    public List<Order> list(long memberId) {
        List<OrderEntity> orders=repository.findAllByMember_IdOrderByIdDesc(memberId);
        if(orders.isEmpty()) return List.of();
        // One query for all line snapshots, rather than one additional query per order.
        Map<Long,List<OrderLineView>> grouped=items.findLines(orders.stream().map(OrderEntity::getId).toList())
            .stream().collect(Collectors.groupingBy(OrderLineView::orderId));
        return orders.stream().map(order -> view(order,grouped.getOrDefault(order.getId(),List.of())
            .stream().map(OrderLineView::toLine).toList())).toList();
    }

    @Transactional
    public Order checkout(long memberId,Checkout body) {
        // Serialize this member's mutations, then lock products in ascending ID order.
        MemberEntity member=members.lockEntity(memberId);
        Optional<OrderEntity> existing=repository.findByMember_IdAndRequestKey(memberId,body.requestKey());
        if(existing.isPresent()) {
            OrderEntity old=existing.get();
            if(old.getPointsUsed()!=body.pointsUsed() || !old.getRecipient().equals(body.recipient().trim()) ||
                !old.getPhone().equals(body.phone()) || !old.getAddress().equals(body.address().trim()) || !old.getPaymentMethod().equals(body.paymentMethod()))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"同じ注文リクエストで内容を変更することはできません。");
            return view(old,lines(old.getId()));
        }
        var cart=carts.list(memberId); // Scalar DTOs: no stale ProductEntity is loaded before locking.
        if(cart.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"カートは空です。");
        long subtotal=0;
        List<Line> snapshots=new ArrayList<>();
        List<ProductEntity> locked=new ArrayList<>();
        for(var item:cart) {
            ProductEntity product=products.lockEntity(item.product().id());
            if(product.getActive()!=1 || product.getStock()<item.quantity())
                throw new ResponseStatusException(HttpStatus.CONFLICT,product.getName()+"の在庫が不足しているか、販売が終了しています。");
            subtotal=Math.addExact(subtotal,Math.multiplyExact(product.getPrice(),item.quantity()));
            snapshots.add(new Line(product.getId(),product.getName(),product.getPrice(),item.quantity()));
            locked.add(product);
        }
        if(body.pointsUsed()>Math.max(0,member.getPoints()) || body.pointsUsed()>subtotal)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"利用可能なポイントまたは商品金額を超えています。");
        long paid=subtotal-body.pointsUsed();
        long earned=paid/100;
        OrderEntity order=new OrderEntity();
        order.setMember(member);order.setRequestKey(body.requestKey());order.setStatus("PAID");
        order.setSubtotal(subtotal);order.setPointsUsed(body.pointsUsed());order.setAmountPaid(paid);order.setPointsEarned(earned);
        order.setRecipient(body.recipient().trim());order.setPhone(body.phone());order.setAddress(body.address().trim());order.setPaymentMethod(body.paymentMethod());
        repository.saveAndFlush(order);
        for(int index=0;index<snapshots.size();index++) {
            Line line=snapshots.get(index);
            ProductEntity product=locked.get(index);
            OrderItemEntity item=new OrderItemEntity();
            item.setOrder(order);item.setProduct(product);item.setProductName(line.name());item.setUnitPrice(line.unitPrice());item.setQuantity(line.quantity());
            items.save(item);
            product.setStock(product.getStock()-line.quantity());
        }
        payment(order,"PAYMENT",paid,"mock-payment-"+order.getId());
        long balance=member.getPoints();
        if(body.pointsUsed()>0) { balance-=body.pointsUsed();point(member,order,"USE",-body.pointsUsed(),balance); }
        if(earned>0) { balance+=earned;point(member,order,"EARN",earned,balance); }
        member.setPoints(balance);
        carts.clear(memberId);
        return view(order,snapshots);
    }

    @Transactional
    public Order cancel(long memberId,long id) {
        MemberEntity member=members.lockEntity(memberId);
        OrderEntity order=owned(memberId,id);
        List<Line> snapshots=lines(id);
        if(order.getStatus().equals("CANCELLED")) return view(order,snapshots);
        for(Line line:snapshots) {
            ProductEntity product=products.lockEntity(line.productId());
            product.setStock(product.getStock()+line.quantity());
        }
        order.setStatus("CANCELLED");order.setCancelledAt(LocalDateTime.now());
        payment(order,"REFUND",order.getAmountPaid(),"mock-refund-"+id);
        long balance=member.getPoints();
        if(order.getPointsUsed()>0) { balance+=order.getPointsUsed();point(member,order,"RESTORE",order.getPointsUsed(),balance); }
        // Previously spent rewards may leave a negative balance; future rewards offset it.
        if(order.getPointsEarned()>0) { balance-=order.getPointsEarned();point(member,order,"REVOKE",-order.getPointsEarned(),balance); }
        member.setPoints(balance);
        return view(order,snapshots);
    }
    private void payment(OrderEntity order,String kind,long amount,String reference) {
        PaymentEntity payment=new PaymentEntity();
        payment.setOrder(order);payment.setProvider("MOCK");payment.setKind(kind);payment.setAmount(amount);payment.setReferenceCode(reference);
        payments.saveAndFlush(payment);
    }
    private void point(MemberEntity member,OrderEntity order,String kind,long amount,long balance) {
        PointLedgerEntity entry=new PointLedgerEntity();
        entry.setMember(member);entry.setOrder(order);entry.setKind(kind);entry.setAmount(amount);entry.setBalanceAfter(balance);
        ledger.save(entry);
    }
    public List<PointEntry> points(long memberId) {
        return ledger.findAllByMember_IdOrderByIdDesc(memberId).stream().map(entry -> new PointEntry(
            entry.getId(),entry.getOrder().getId(),entry.getKind(),entry.getAmount(),entry.getBalanceAfter(),entry.getCreatedAt().toString())).toList();
    }
    private static Order view(OrderEntity order,List<Line> lines) {
        return new Order(order.getId(),order.getStatus(),order.getSubtotal(),order.getPointsUsed(),order.getAmountPaid(),
            order.getPointsEarned(),order.getRecipient(),order.getPhone(),order.getAddress(),order.getPaymentMethod(),order.getCreatedAt().toString(),lines);
    }
}
