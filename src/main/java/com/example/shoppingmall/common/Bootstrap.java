package com.example.shoppingmall.common;

import com.example.shoppingmall.auth.MemberService;
import com.example.shoppingmall.auth.MemberRepository;
import com.example.shoppingmall.product.ProductService;
import com.example.shoppingmall.product.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class Bootstrap implements ApplicationRunner {
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final MemberService members;
    private final ProductService products;
    @Value("${mall.admin.email:}") private String adminEmail;
    @Value("${mall.admin.password:}") private String adminPassword;
    public Bootstrap(MemberRepository memberRepository,ProductRepository productRepository,MemberService members,ProductService products) {
        this.memberRepository=memberRepository;this.productRepository=productRepository;this.members=members;this.products=products;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        if(!adminEmail.isBlank()) {
            String email=adminEmail.trim().toLowerCase(java.util.Locale.ROOT);
            var existing=memberRepository.findByEmail(email);
            if(existing.isEmpty()) {
                if(adminPassword.length()<10 || adminPassword.length()>64 || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
                    throw new IllegalStateException("管理者メールアドレスと10～64文字のパスワードを環境変数に設定してください。");
                members.create(new MemberService.Registration(email,adminPassword,"管理者"),"ADMIN");
            } else if(!existing.get().getRole().equals("ADMIN")) {
                throw new IllegalStateException("一般会員を自動で管理者に変更することはできません。別のメールアドレスをご利用ください。");
            }
        }
        if(productRepository.count()==0) {
            products.create(new ProductService.Edit("デイリーセラミックマグ","暮らし・雑貨",1800,"一日のはじまりを彩る、お気に入りのカップ。","☕","",30));
            products.create(new ProductService.Edit("ナチュラルキャンバスバッグ","ファッション",2900,"気軽なお出かけに、毎日持ちたくなるバッグ。","👜","",25));
            products.create(new ProductService.Edit("ワイヤレスヘッドホン","デジタル",8900,"好きな音楽に、じっくり耳を傾ける時間。","🎧","",15));
            products.create(new ProductService.Edit("やわらかコットンブランケット","暮らし・雑貨",3500,"くつろぎの時間に、やさしい肌ざわりを。","🛋️","",20));
            products.create(new ProductService.Edit("クラシックデイリースニーカー","ファッション",5900,"どこへでも心地よく、自分らしい一歩を。","👟","",20));
            products.create(new ProductService.Edit("ミニテーブルライト","暮らし・雑貨",4200,"小さな空間を、やわらかい光で満たします。","💡","",20));
        }
    }
}
