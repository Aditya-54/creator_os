package com.creatoros.brand;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Lightweight keyword-based brand mention detection, mirroring
 * {@code TopicExtractionService}. Only ever records that a brand name
 * appeared in a title/description - never infers brand preference or intent
 * from that (see docs/architecture.md, section 14 of the product spec).
 */
@Service
public class BrandExtractionService {

    private static final String[] KNOWN_BRANDS = {
            "Nike", "Adidas", "Red Bull", "Samsung", "Apple", "Sony", "Puma", "Gatorade"
    };

    public Set<String> extract(String... texts) {
        String haystack = String.join(" ", texts).toLowerCase();
        Set<String> found = new LinkedHashSet<>();
        for (String brand : KNOWN_BRANDS) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(brand.toLowerCase()) + "\\b");
            if (pattern.matcher(haystack).find()) {
                found.add(brand);
            }
        }
        return found;
    }
}
