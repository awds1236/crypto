package com.crypto.analysis.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.crypto.analysis.model.CryptoAnalysis;

@Repository
public interface CryptoAnalysisRepository extends JpaRepository<CryptoAnalysis, Long> {
    
    // 특정 마켓의 분석 결과 가져오기
    List<CryptoAnalysis> findByMarketOrderByAnalysisDateDesc(String market);
    
    // 특정 날짜 이후의 분석 결과 가져오기
    List<CryptoAnalysis> findByAnalysisDateAfterOrderByAnalysisDateDesc(LocalDateTime date);
    
    // 특정 추천 유형의 분석 결과 가져오기
    List<CryptoAnalysis> findByRecommendationOrderByAnalysisDateDesc(String recommendation);
    
    // 특정 신뢰도 이상의 분석 결과 가져오기
    List<CryptoAnalysis> findByConfidenceScoreGreaterThanEqualOrderByConfidenceScoreDesc(Double confidenceScore);
    
    // 최근 분석 결과 가져오기
    @Query("SELECT a FROM CryptoAnalysis a WHERE a.market = :market ORDER BY a.analysisDate DESC")
    List<CryptoAnalysis> findLatestAnalysisByMarket(@Param("market") String market, @org.springframework.data.domain.Pageable pageable);
    
    // 오늘의 분석 결과 가져오기
    @Query("SELECT a FROM CryptoAnalysis a WHERE a.analysisDate >= :startDate AND a.analysisDate <= :endDate")
    List<CryptoAnalysis> findAnalysisByDateRange(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);
    
    // 매수 추천 코인만 가져오기
    @Query("SELECT a FROM CryptoAnalysis a WHERE a.recommendation = '매수' AND a.analysisDate = " +
            "(SELECT MAX(b.analysisDate) FROM CryptoAnalysis b WHERE b.market = a.market) " +
            "ORDER BY a.confidenceScore DESC")
    List<CryptoAnalysis> findLatestBuyRecommendations();
    
    // 매도 추천 코인만 가져오기
    @Query("SELECT a FROM CryptoAnalysis a WHERE a.recommendation = '매도' AND a.analysisDate = " +
            "(SELECT MAX(b.analysisDate) FROM CryptoAnalysis b WHERE b.market = a.market) " +
            "ORDER BY a.confidenceScore DESC")
    List<CryptoAnalysis> findLatestSellRecommendations();
    
    // 각 코인별 최신 분석 결과만 가져오기
    @Query("SELECT a FROM CryptoAnalysis a WHERE a.analysisDate = " +
            "(SELECT MAX(b.analysisDate) FROM CryptoAnalysis b WHERE b.market = a.market)")
    List<CryptoAnalysis> findLatestAnalysisForAllCoins();
}