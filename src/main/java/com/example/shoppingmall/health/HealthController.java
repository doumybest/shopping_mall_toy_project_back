package com.example.shoppingmall.health;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class HealthController {
    @GetMapping("/api/health") Map<String,String> health() { return Map.of("status","UP","paymentMode","MOCK"); }
}
