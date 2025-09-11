package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingQuery;
import com.loopers.interfaces.api.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rankings")
public class RankingV1Controller {
    
    private final RankingQuery rankingQuery;

    @Autowired
    public RankingV1Controller(RankingQuery rankingQuery) {
        this.rankingQuery = rankingQuery;
    }

    @GetMapping
    public ApiResponse<RankingQuery.RankingPageResult> getRankings(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        RankingQuery.RankingPageResult result = rankingQuery.getRankings(date, page, size);

        return ApiResponse.success(result);
    }
}
